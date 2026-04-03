# OSRM MCP 服务架构文档

## 概述

本项目将 OSRM（开源软件仓库管理）系统的 REST API 封装为三个 [MCP（Model Context Protocol）](https://modelcontextprotocol.io) 服务，使 AI 智能体能够以工具调用的方式驱动软件全生命周期管理。

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        AI 智能体（Claude）                        │
│                                                                 │
│   "帮我搜索 nginx 镜像"   "批量通过今天的订购申请"   "创建新包"     │
└────────────────┬────────────────┬────────────────┬─────────────┘
                 │ MCP stdio      │ MCP stdio      │ MCP stdio
     ┌───────────▼──┐  ┌──────────▼──┐  ┌──────────▼──┐
     │ osrm-portal  │  │osrm-workflow│  │  osrm-admin  │
     │  (6 tools)   │  │ (12 tools)  │  │  (10 tools)  │
     │  无需鉴权     │  │  用户级JWT   │  │  管理员JWT    │
     └──────┬───────┘  └──────┬──────┘  └──────┬───────┘
            │                 │                 │
            └─────────────────┴─────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │  @osrm-mcp/shared  │
                    │  ┌─────────────┐  │
                    │  │ api-client  │  │  axios + 拦截器
                    │  │  (axios)    │  │  自动解包 ApiResponse<T>
                    │  └──────┬──────┘  │
                    │  ┌──────▼──────┐  │
                    │  │    auth     │  │  JWT 自动刷新
                    │  │  (jwt mgr)  │  │
                    │  └─────────────┘  │
                    └─────────┬─────────┘
                              │ HTTP / REST
                    ┌─────────▼─────────┐
                    │   OSRM Backend     │
                    │  Spring Boot 3.4   │
                    │  :8080/api/v1      │
                    │                   │
                    │  90 REST 端点       │
                    └───────────────────┘
```

---

## 目录结构

```
osrm-mcp/
├── package.json              # npm workspaces 根配置
├── tsconfig.base.json        # 共享 TypeScript 配置
├── ARCHITECTURE.md           # 本文档
│
├── shared/                   # 共享库 @osrm-mcp/shared
│   ├── package.json
│   ├── tsconfig.json
│   └── src/
│       ├── index.ts          # 公开导出
│       ├── api-client.ts     # axios 封装 + 响应拦截
│       └── auth.ts           # JWT 登录 + 自动刷新
│
└── packages/
    ├── portal/               # osrm-portal MCP Server
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── src/index.ts      # 6 个工具 + 1 个资源
    │
    ├── workflow/             # osrm-workflow MCP Server
    │   ├── package.json
    │   ├── tsconfig.json
    │   └── src/index.ts      # 12 个工具
    │
    └── admin/                # osrm-admin MCP Server
        ├── package.json
        ├── tsconfig.json
        └── src/index.ts      # 10 个工具
```

---

## 三个 MCP 服务详细说明

### 1. osrm-portal — 公开软件门户

**定位**：只读发现服务，无需任何凭据，任何智能体均可挂载。

**工具列表（6个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `search_software` | `GET /portal/software` | 搜索已发布软件包，支持关键词和类型过滤 |
| `get_software_detail` | `GET /portal/software/{id}` + `GET /software-packages/{id}/versions` | 获取软件包详情及所有版本 |
| `get_portal_stats` | `GET /portal/stats` | 门户统计概览（各类型数量） |
| `get_popular_software` | `GET /portal/popular` | 热门软件包（按下载量排序） |
| `list_software_types` | `GET /software-packages/types` | 软件类型枚举列表 |
| `get_download_command` | `GET /portal/software/{id}` + versions | 生成制品下载/使用命令 |

**资源（1个）**：
- `osrm://portal/stats` — 实时门户统计，可由 AI 客户端订阅

**`get_download_command` 生成规则**：

| 类型 | 生成命令格式 |
|------|------------|
| `DOCKER_IMAGE` | `docker pull {storagePath}` |
| `HELM_CHART` | `helm pull <repo>/{name} --version {ver}` |
| `MAVEN` | `<dependency>` XML 片段 |
| `NPM` | `npm install {name}@{ver}` |
| `PYPI` | `pip install {name}=={ver}` |
| `GENERIC` | `curl -O "{artifactUrl}"` 或存储路径 |

---

### 2. osrm-workflow — 业务工作流

**定位**：面向开发者和软件管理员，处理订购申请与软件生命周期管理。

**认证**：启动时读取 `OSRM_USERNAME` / `OSRM_PASSWORD`，自动获取并维护 JWT。

**工具列表（12个）**：

**订购类（3个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `apply_subscription` | `POST /subscriptions` | 提交订购申请（含业务系统和使用场景） |
| `list_my_subscriptions` | `GET /subscriptions/my` | 查询当前用户的订购记录 |
| `get_subscription_token` | `GET /subscriptions/{id}/token` | 获取已批准订购的制品访问令牌 |

**软件管理类（5个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `create_software_package` | `POST /software-packages` | 创建软件包草稿 |
| `add_version` | `POST /software-packages/{id}/versions` | 为软件包添加新版本 |
| `submit_for_review` | `POST /software-packages/{id}/submit` | 提交审核（DRAFT → PENDING） |
| `publish_version` | `POST /software-packages/{id}/versions/{vid}/publish` | 发布版本（DRAFT → PUBLISHED） |
| `offline_version` | `POST /software-packages/{id}/versions/{vid}/offline` | 下线版本 |

**查询类（4个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `list_my_packages` | `GET /software-packages/my` | 查询当前用户管理的软件包 |
| `list_business_systems` | `GET /business-systems` | 获取业务系统列表（用于选择订购关联系统） |
| `list_storage_backends` | `GET /storage-backends` | 获取可用存储后端（用于新建版本） |
| `check_compliance` | `GET /subscriptions/compliance/{systemId}` | 检查业务系统软件使用合规情况 |

---

### 3. osrm-admin — 系统管理

**定位**：面向系统管理员，处理审批、用户管理、存储配置等管理操作。

**认证**：与 workflow 相同机制，需配置管理员级账户。

**工具列表（10个）**：

**审批类（4个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `list_pending_approvals` | `GET /subscriptions/pending` + `GET /software-packages/pending` | 查询所有待审批事项 |
| `approve_item` | `POST /subscriptions/{id}/approve` 或 `/software-packages/{id}/approve` | 批准单个审批项 |
| `reject_item` | `POST /subscriptions/{id}/reject` 或 `/software-packages/{id}/reject` | 拒绝审批项（需填原因） |
| `batch_approve` | `POST /subscriptions/batch-approve` 或 `/software-packages/batch-approve` | 批量批准 |

**用户管理（2个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `list_users` | `GET /users` | 查询用户列表 |
| `create_user` | `POST /users` | 创建用户并分配角色 |

**存储管理（2个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `check_storage_health` | `GET /storage-backends/health` | 检查所有存储后端健康状态 |
| `list_storage_backends_detail` | `GET /storage-backends` | 查询存储后端完整配置 |

**软件管理（2个）**：

| 工具 | 对应 REST | 说明 |
|------|-----------|------|
| `list_all_packages` | `GET /software-packages` | 查询所有软件包（管理员视角，含他人的） |
| `offline_package` | `POST /software-packages/{id}/offline` | 强制下线软件包（需填原因） |

---

## 共享层设计

### api-client.ts

```
createClient(baseUrl, requireAuth)
  │
  ├── axios 实例
  │     timeout: 15s
  │     Content-Type: application/json
  │
  ├── Request 拦截器（requireAuth=true 时启用）
  │     └── 调用 getAccessToken() → 注入 Authorization: Bearer {token}
  │
  └── Response 拦截器
        ├── 成功：解包 ApiResponse<T>.data，业务错误 code≠200 → throw OsrmApiError
        └── 失败：HTTP 错误 → throw OsrmApiError(message, statusCode)
```

### auth.ts

```
getAccessToken()
  │
  ├── token 存在且未过期（留 60s 缓冲）→ 直接返回
  │
  └── token 过期或不存在
        ├── 有 refreshToken → POST /auth/refresh
        │     成功 → 更新内存 token
        │     失败 → 走 login 流程
        └── POST /auth/login（从环境变量读 username/password）
              └── 保存 { accessToken, refreshToken, expiresAt }
```

**Token 仅存内存，进程重启后自动重新登录。**

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | TypeScript 5.7，编译目标 ES2022 |
| 模块系统 | ESM（`"type": "module"`，`moduleResolution: Node16`） |
| MCP SDK | `@modelcontextprotocol/sdk` ^1.12.0 |
| HTTP 客户端 | `axios` ^1.7.9 |
| 参数校验 | `zod` ^3.24.1 |
| 传输方式 | stdio（标准输入输出，Claude Code 本地集成） |
| 包管理 | npm workspaces（monorepo） |

---

## Claude Code 集成配置

配置文件：`C:\Users\51554\claude\.mcp.json`

```json
{
  "mcpServers": {
    "osrm-portal": {
      "command": "node",
      "args": ["C:/Users/51554/claude/osrm-mcp/packages/portal/dist/index.js"],
      "env": {
        "OSRM_BASE_URL": "http://localhost:8080/api/v1"
      }
    },
    "osrm-workflow": {
      "command": "node",
      "args": ["C:/Users/51554/claude/osrm-mcp/packages/workflow/dist/index.js"],
      "env": {
        "OSRM_BASE_URL": "http://localhost:8080/api/v1",
        "OSRM_USERNAME": "admin",
        "OSRM_PASSWORD": "admin123"
      }
    },
    "osrm-admin": {
      "command": "node",
      "args": ["C:/Users/51554/claude/osrm-mcp/packages/admin/dist/index.js"],
      "env": {
        "OSRM_BASE_URL": "http://localhost:8080/api/v1",
        "OSRM_USERNAME": "admin",
        "OSRM_PASSWORD": "admin123"
      }
    }
  }
}
```

---

## 典型使用场景

### 场景 1：发现并获取软件

```
智能体: "帮我找一下有没有 nginx 的 Docker 镜像，并给我拉取命令"

→ search_software(keyword="nginx", type="DOCKER_IMAGE")
→ get_download_command(package_id=3)
→ 返回: docker pull registry.example.com/library/nginx:1.25.3
```

### 场景 2：软件包录入全流程

```
智能体: "帮我录入一个新的 Python 包 requests 2.31.0"

→ list_storage_backends()                              # 选存储后端
→ create_software_package(name="requests", key="requests", type="PYPI")
→ add_version(package_id=X, version_no="2.31.0", storage_backend_id=1)
→ submit_for_review(package_id=X)
→ 返回: 软件包已提交审核，等待管理员审批
```

### 场景 3：批量审批

```
智能体: "把今天所有待审批的订购申请都批了"

→ list_pending_approvals(type="SUBSCRIPTION")
→ batch_approve(type="SUBSCRIPTION", ids=[12, 13, 14, 15])
→ 返回: 已批准 4 个订购申请
```

### 场景 4：合规检查

```
智能体: "检查订单系统用了哪些软件，是否都有审批"

→ list_business_systems(keyword="订单")
→ check_compliance(system_id=5)
→ 返回: { compliant: true/false, software: [...], unapprovedCount: N }
```

---

## 构建命令

```bash
# 构建全部
npm run build --workspaces

# 单独构建
npm run build:portal
npm run build:workflow
npm run build:admin
```

---

## 设计决策说明

**为什么分 3 个 Server 而不是 1 个？**

- **最小权限原则**：portal 无需凭据，可安全挂载到任意环境；workflow/admin 需要账户，按角色隔离
- **工具数量控制**：每个 Server ≤15 个工具，避免 AI 上下文窗口因工具描述过多而膨胀
- **独立部署**：可按需挂载，普通查询场景只需 portal，不暴露管理能力

**为什么用 stdio 而不是 SSE/HTTP？**

- 本地 Claude Code 集成首选 stdio，零额外服务、无需端口、天然进程隔离
- 如需远程部署，SDK 支持直接切换 `SSEServerTransport` 而无需修改工具逻辑
