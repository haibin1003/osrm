# OSRM MCP 服务架构设计文档

## 1. 系统概述

OSRM MCP（Model Context Protocol）服务将 OSRM 开源软件仓库管理平台的核心能力封装为标准化 MCP 工具，供 AI 智能体（Claude Code、Claude Desktop、Cursor 等）直接调用。

**核心目标：**
- 无需人工操作 Web 界面，AI 可通过自然语言完成软件发现、订购、审批全流程
- 单端口公网部署，所有用户（含管理员）均通过 Basic Auth 以自身身份接入，JWT 生命周期透明管理
- 对 OSRM 后端系统**零侵入**：仅通过标准 HTTP REST API 调用，不修改任何业务代码

---

## 2. 整体架构

### 2.1 部署拓扑

```
┌─────────────────────────────────────────────────────────────────┐
│                    AI 客户端层                                    │
│                                                                  │
│  Claude Code          Claude Desktop       Cursor / Windsurf     │
│  .mcp.json (url)      claude_desktop_config  任意 MCP 客户端    │
└──────────────┬──────────────────────────────────────────────────┘
               │  Streamable HTTP (MCP 协议)
               ▼
┌─────────────────────────────────────────────────────────────────┐
│                  OSRM MCP Gateway  :3000                         │
│                                                                  │
│  GET  /health      → 健康检查（公开）                             │
│  POST /portal/mcp  → Portal 服务（无需认证，公开浏览）            │
│  POST /api/mcp     → API 服务（HTTP Basic Auth：用户名/密码）     │
└──────────────────────────────┬──────────────────────────────────┘
                               │  REST API (HTTP/JSON)
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│              OSRM Backend  :8080/api/v1                          │
│                                                                  │
│  Spring Boot 3.4 / Java 17 / PostgreSQL                          │
│  Harbor (Docker/Helm) · Nexus (Maven/NPM/PyPI) · NAS (Generic)  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 鉴权设计

| 端点 | MCP 层鉴权 | OSRM 身份来源 | 说明 |
|------|-----------|--------------|------|
| `/portal/mcp` | 无 | 匿名 | 调用公开门户接口 |
| `/api/mcp` | HTTP Basic Auth | 用户提供的账号密码 | 网关提取后调 `/auth/login`，管理员/普通用户均走此端点，后端 RBAC 控制权限 |

**设计原则：** MCP 层不做业务权限判断。用户以自身 OSRM 账号接入，后端返回 403 表示无权限，符合最小职责原则。

### 2.3 代码结构

```
osrm-mcp/
├── package.json               # npm workspaces 根配置
├── tsconfig.base.json         # TypeScript 公共配置
├── shared/src/
│   ├── auth.ts                # per-user JWT 缓存，自动续期
│   ├── api-client.ts          # Axios 工厂 + 响应拦截器
│   └── index.ts
├── packages/
│   ├── portal/src/
│   │   ├── server.ts          # buildPortalServer(api) — 6 个公开工具
│   │   └── index.ts           # stdio 模式（向下兼容本地开发）
│   ├── api/src/
│   │   └── server.ts          # buildApiServer(api) — 22 个认证工具
│   ├── workflow/src/          # 保留 stdio 模式入口
│   ├── admin/src/             # 保留 stdio 模式入口
│   └── gateway/src/
│       └── index.ts           # 单端口 HTTP 网关
└── docs/
    ├── architecture.md
    └── integration-guide.md
```

---

## 3. 三层调用链路

```
AI 客户端
    │
    │  POST /api/mcp
    │  Authorization: Basic base64(username:password)
    │
    ▼
Gateway (Node.js HTTP)
    │
    ├── parseBasicAuth() → { username, password }
    ├── createClientForUser(BASE_URL, username, password)
    │   └── Axios 实例，request 拦截器自动注入 Bearer token
    ├── buildApiServer(userApi) — 创建含 22 个工具的 MCP Server
    ├── new StreamableHTTPServerTransport({ sessionIdGenerator: undefined })
    ├── server.connect(transport)
    │
    │  transport.handleRequest(req, res)
    │  → MCP 框架解析 JSON-RPC → 路由到对应 tool handler
    │
    ▼
Tool Handler（如 list_pending_approvals）
    │
    │  userApi.get('/subscriptions/pending')
    │
    ▼
Axios request 拦截器
    │  getTokenForUser(baseUrl, username, password)
    │  → 缓存命中：直接返回 accessToken
    │  → 缓存未命中：POST /auth/login → 缓存 TokenPair
    │
    ▼
OSRM Backend REST API
    │  后端依据 JWT 中的角色做 RBAC 鉴权
    │  普通用户调管理接口 → 403（由后端统一处理）
    │
    ▼
响应拦截器 → 解包 ApiResponse<T> → tool handler 格式化 → JSON-RPC 响应
    │
    ▼
AI 客户端接收工具调用结果
```

---

## 4. 鉴权时序图

### 4.1 Portal（无需认证）

```
AI Client           Gateway         OSRM Backend
    │                  │                 │
    │─ POST /portal/mcp ►               │
    │                  │─ GET /portal/.. ►│
    │                  │◄── 200 OK ───────│
    │◄── 工具结果 ───────│                 │
```

### 4.2 API（Basic Auth，首次调用）

```
AI Client              Gateway              OSRM Backend
    │                     │                     │
    │  Authorization:      │                     │
    │  Basic base64(u:p)   │                     │
    │─ POST /api/mcp ─────►│                     │
    │                     │ parseBasicAuth()     │
    │                     │ createClientForUser()│
    │                     │                     │
    │                     │─ POST /auth/login ──►│
    │                     │◄── {accessToken,...} ─│
    │                     │ tokenCache.set(key,p)│
    │                     │                     │
    │                     │─ GET /subscriptions ►│
    │                     │◄── 200 data ──────────│
    │◄── 工具结果 ──────────│                     │
```

### 4.3 API（token 有效，后续调用）

```
AI Client              Gateway              OSRM Backend
    │                     │                     │
    │─ POST /api/mcp ─────►│                     │
    │                     │ tokenCache.get(key)  │
    │                     │ → token 有效，直接用  │
    │                     │─ GET /users ────────►│
    │                     │◄── 200 data ──────────│
    │◄── 工具结果 ──────────│                     │
```

---

## 5. Per-User Token 缓存机制

```typescript
// shared/src/auth.ts
const tokenCache = new Map<string, TokenPair>();
// key: "username@baseUrl"

async function getTokenForUser(baseUrl, username, password) {
  const cached = tokenCache.get(`${username}@${baseUrl}`);

  if (cached && Date.now() < cached.expiresAt - 60_000)
    return cached.accessToken;           // 直接复用

  if (cached?.refreshToken) {
    try {
      const pair = await POST('/auth/refresh', { refreshToken });
      tokenCache.set(key, pair);
      return pair.accessToken;           // refresh 成功
    } catch { /* 续期失败，继续 login */ }
  }

  const pair = await POST('/auth/login', { username, password });
  tokenCache.set(key, pair);
  return pair.accessToken;              // 重新登录
}
```

- 进程内缓存，每个用户独立，互不干扰
- 距过期 60 秒前自动尝试 refresh，失败则重新 login
- 缓存 key 为 `username@baseUrl`，支持多后端场景

---

## 6. 无状态设计说明

Gateway 对每个 HTTP 请求：
1. 创建新的 `StreamableHTTPServerTransport({ sessionIdGenerator: undefined })`
2. 创建新的 `Server` 实例（`buildPortalServer` / `buildApiServer`）
3. `server.connect(transport)` → 处理请求 → 响应结束后销毁

**适用原因：** 所有 28 个工具均为请求-响应模式，无需跨请求共享状态。无状态设计使 Gateway 天然支持水平扩展和容器化部署。

---

## 7. 技术选型说明

| 方案 | 说明 | 结论 |
|------|------|------|
| **Streamable HTTP** | SDK 内置，单 HTTP 端点处理 MCP 请求/响应流 | ✅ 选用：支持公网，无连接状态 |
| stdio | 子进程通信 | 保留为本地开发模式（index.ts） |
| SSE | SDK 已废弃 | ❌ 不用 |
| 多端口（portal/workflow/admin） | 每服务独立端口 | ❌ 运维复杂 |
| **单端口路径路由** | /portal/mcp · /api/mcp | ✅ 选用：一个端口，TLS 友好 |
| 三端点（portal/workflow/admin） | 按角色分端点 | ❌ 用户需多套配置 |
| **两端点（portal/api）** | 按认证方式分 | ✅ 选用：配置简洁，后端统一 RBAC |

---

## 8. 部署说明

### 环境变量

| 变量 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| `OSRM_BASE_URL` | 否 | `http://localhost:8080/api/v1` | OSRM 后端地址 |
| `PORT` | 否 | `3000` | 网关监听端口 |

> `/api/mcp` 端点不需要服务端内置凭据，所有用户以自身账号密码通过 Basic Auth 接入。

### 启动命令

```bash
# 构建
npm run build

# 启动（开发环境）
PORT=3000 OSRM_BASE_URL=http://localhost:8080/api/v1 node packages/gateway/dist/index.js
```

### 生产环境（Nginx + TLS）

```nginx
server {
    listen 443 ssl;
    server_name mcp.example.com;
    ssl_certificate     /etc/ssl/certs/mcp.crt;
    ssl_certificate_key /etc/ssl/private/mcp.key;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_buffering off;        # 支持 Streamable HTTP 流式响应
        proxy_read_timeout 300s;
    }
}
```
