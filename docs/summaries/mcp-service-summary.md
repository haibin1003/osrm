# MCP 服务层总结

**功能ID**: MCP 服务层（跨模块工具集成）
**完成日期**: 2026-03-22
**开发阶段**: Phase 1 增强（全功能）

---

## 一、实现清单

### 工程结构

| 包 | 路径 | 用途 | 工具数 |
|----|------|------|--------|
| `@osrm-mcp/shared` | `packages/shared/` | Axios 工厂 + per-user JWT 缓存 | — |
| `osrm-portal` | `packages/portal/` | 公开软件浏览工具 | 6 |
| `osrm-api` | `packages/api/` | 认证管理工具（订购/软件/审批/用户/存储） | 22 |
| `osrm-gateway` | `packages/gateway/` | 单端口 HTTP 网关（路由 + Basic Auth） | — |
| `osrm-workflow` | `packages/workflow/` | stdio 模式入口（保留） | — |
| `osrm-admin` | `packages/admin/` | stdio 模式入口（保留） | — |

---

## 二、技术架构

### 传输协议

- **协议**: Streamable HTTP（支持公网部署）
- **部署**: 单端口 `:3000`，路径路由
- **鉴权**: Portal 公开无认证，API 端点 HTTP Basic Auth
- **后端**: 对 OSRM 后端零侵入，权限由后端 RBAC 统一控制

### 端点设计

```
GET  /health       → 公开健康检查
POST /portal/mcp   → 无需认证，6个公开工具
POST /api/mcp      → Basic Auth，22个工具，后端 RBAC 鉴权
```

### per-user JWT 缓存

```typescript
// Map<"user@url", TokenPair>
const tokenCache = new Map<string, TokenPair>();
// 自动处理 Token 续期，客户端无感知
```

---

## 三、工具清单

### Portal 端（6个工具）- 公开访问

| 工具名 | 功能 |
|--------|------|
| `listSoftwarePackages` | 列出所有已发布的软件包 |
| `getSoftwarePackageDetail` | 获取软件包详情 |
| `searchSoftwarePackages` | 搜索软件包 |
| `listSoftwareTypes` | 列出支持的软件类型 |
| `listBusinessDomains` | 列出业务域 |
| `getSoftwarePackageVersions` | 获取软件包版本列表 |

### API 端（22个工具）- 需 Basic Auth

| 类别 | 工具名 | 功能 |
|------|--------|------|
| **订购** | `createSubscription` | 申请订购软件包 |
| | `listMySubscriptions` | 查看我的订购 |
| | `getSubscriptionToken` | 获取订购 Token |
| **审批** | `listPendingApprovals` | 列出待审批申请 |
| | `approveSubscription` | 通过订购申请 |
| | `rejectSubscription` | 拒绝订购申请 |
| **软件包** | `createSoftwarePackage` | 创建软件包 |
| | `listSoftwarePackages` | 列出软件包 |
| | `submitSoftwarePackage` | 提交软件包审核 |
| | `publishSoftwarePackage` | 发布软件包 |
| | `offlineSoftwarePackage` | 下架软件包 |
| | `addSoftwareVersion` | 添加软件版本 |
| **存储** | `listStorageBackends` | 列出存储后端 |
| | `testStorageBackend` | 测试存储连接 |
| **业务系统** | `listBusinessSystems` | 列出业务系统 |
| **用户** | `getCurrentUser` | 获取当前用户信息 |
| | `listUsers` | 列出用户 |
| **统计** | `getPortalStats` | 获取门户统计 |
| | `getSubscriptionTrend` | 获取订购趋势 |
| **制品** | `uploadArtifact` | 上传制品 |
| | `getArtifactDownloadCommand` | 获取下载命令 |

---

## 四、接入方式

### MCP 配置文件（`.mcp.json`）

```json
{
  "mcpServers": {
    "osrm-portal": {
      "type": "http",
      "url": "http://localhost:3000/portal/mcp"
    },
    "osrm-api": {
      "type": "http",
      "url": "http://localhost:3000/api/mcp",
      "headers": {
        "Authorization": "Basic YWRtaW46YWRtaW4xMjM="
      }
    }
  }
}
```

### 启动网关

```bash
cd osrm-mcp
PORT=3000 OSRM_BASE_URL=http://localhost:8080/api/v1 \
  node packages/gateway/dist/index.js
```

---

## 五、核心设计决策

1. **无状态设计**: 每请求创建新 Server+Transport 实例
2. **统一鉴权**: 管理员和普通用户统一走 Basic Auth，权限由后端 RBAC 控制
3. **Token 缓存**: per-user JWT 缓存实现无感续期
4. **类型规范**: `AxiosInstance` 从 `axios` 直接导入，不从 `@osrm-mcp/shared` 导入

---

## 六、文档

- `osrm-mcp/docs/architecture.md` — 架构设计、调用链路图、鉴权时序图
- `osrm-mcp/docs/integration-guide.md` — 第三方接入手册

---

## 七、遗留问题

- 无状态设计带来内存开销，高频调用时可考虑实例池化
- 当前 Basic Auth 明文传输，生产环境需配合 HTTPS
