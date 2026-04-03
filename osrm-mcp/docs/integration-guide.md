# OSRM MCP 服务接入手册

本手册面向第三方 AI 应用开发者，说明如何将 OSRM MCP 服务接入 Claude Code、Claude Desktop、Cursor 等支持 MCP 协议的 AI 客户端。

---

## 1. 服务概览

### 1.1 两个服务端点

| 端点 | 认证方式 | 适用用户 | 工具数量 |
|------|---------|---------|---------|
| `/portal/mcp` | 无需认证 | 所有人（公开浏览） | 6 个 |
| `/api/mcp` | HTTP Basic Auth（OSRM 账号密码） | 所有已注册用户（含管理员） | 22 个 |

**默认地址：** `http://localhost:3000`（部署后替换为实际公网地址）

**权限说明：** `/api/mcp` 的 22 个工具对所有已登录用户可见，实际能否执行由后端 RBAC 控制。例如审批工具对普通用户返回 403，这是后端正常行为。

### 1.2 能力边界

**Portal 服务**（公开，无需登录）：

| # | 工具 | 说明 |
|---|------|------|
| 1 | `search_software` | 搜索已发布软件包，支持关键词和类型过滤 |
| 2 | `get_software_detail` | 获取软件包详情和所有版本列表 |
| 3 | `get_portal_stats` | 门户统计数据（发布总数、各类型数量） |
| 4 | `get_popular_software` | 热门软件包列表（按下载量） |
| 5 | `list_software_types` | 支持的软件类型枚举 |
| 6 | `get_download_command` | 生成 docker pull / npm install / pip install 等命令 |

**API 服务**（需登录，后端 RBAC 鉴权）：

订购类 3 个 · 软件管理类 5 个 · 查询类 4 个 · 审批类 4 个（管理员）· 用户管理 2 个（管理员）· 存储管理 2 个（管理员）· 软件包管理员视图 2 个

---

## 2. 快速开始

### 2.1 验证服务可用性

```bash
curl http://<your-server>:3000/health
# {"status":"ok","version":"1.0.0","services":["portal","api"]}
```

### 2.2 最简接入（仅 Portal，无需认证）

```json
{
  "mcpServers": {
    "osrm-portal": {
      "type": "http",
      "url": "http://<your-server>:3000/portal/mcp"
    }
  }
}
```

---

## 3. Claude Code 接入

### 3.1 配置文件位置

- **项目级**（仅当前项目生效）：项目根目录 `.mcp.json`
- **全局**（所有项目生效）：`~/.claude/settings.json` 中的 `mcpServers` 字段

### 3.2 完整配置（Portal + API）

```json
{
  "mcpServers": {
    "osrm-portal": {
      "type": "http",
      "url": "http://<your-server>:3000/portal/mcp"
    },
    "osrm-api": {
      "type": "http",
      "url": "http://<your-server>:3000/api/mcp",
      "headers": {
        "Authorization": "Basic <base64(username:password)>"
      }
    }
  }
}
```

### 3.3 生成 Basic Auth 字符串

```bash
# Linux / macOS
echo -n "your_username:your_password" | base64

# Windows PowerShell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("your_username:your_password"))
```

示例：`admin:admin123` → `YWRtaW46YWRtaW4xMjM=`

```json
"Authorization": "Basic YWRtaW46YWRtaW4xMjM="
```

> **注意：** 使用 `echo -n`（不带换行符），直接 `echo` 会导致 Base64 错误。

---

## 4. Claude Desktop 接入

配置文件路径：
- **macOS**：`~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**：`%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "osrm-portal": {
      "type": "http",
      "url": "http://<your-server>:3000/portal/mcp"
    },
    "osrm-api": {
      "type": "http",
      "url": "http://<your-server>:3000/api/mcp",
      "headers": {
        "Authorization": "Basic <base64(username:password)>"
      }
    }
  }
}
```

修改后需**重启 Claude Desktop** 生效。

---

## 5. 通用客户端接入（Cursor / Windsurf / 其他）

任何支持 Streamable HTTP 传输的 MCP 客户端均可接入，关键参数：

- **传输类型**：Streamable HTTP
- **Portal URL**：`http://<your-server>:3000/portal/mcp`
- **API URL**：`http://<your-server>:3000/api/mcp`
- **认证 Header**：`Authorization: Basic <base64(username:password)>`

---

## 6. 工具速查表

### 6.1 Portal 服务（6 个，无需认证）

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `search_software` | 搜索已发布软件包 | 无（可选 `keyword`, `type`, `page`, `size`） |
| `get_software_detail` | 获取软件包详情和版本列表 | `id` |
| `get_portal_stats` | 门户统计概览 | 无 |
| `get_popular_software` | 热门软件包（按下载量） | 无（可选 `limit`） |
| `list_software_types` | 软件类型枚举列表 | 无 |
| `get_download_command` | 生成下载/使用命令 | `package_id`（可选 `version_id`） |

### 6.2 API 服务（22 个，需 Basic Auth）

**订购类（3 个）**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `apply_subscription` | 提交软件订购申请 | `package_id`, `version_id`, `system_id`, `use_scene` |
| `list_my_subscriptions` | 查询我的订购记录 | 无（可选 `status`, `page`, `size`） |
| `get_subscription_token` | 获取已批准订购的访问令牌 | `subscription_id` |

**软件管理类（5 个）**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `create_software_package` | 创建新软件包（草稿） | `package_name`, `package_key`, `software_type` |
| `add_version` | 添加新版本 | `package_id`, `version_no`, `storage_backend_id` |
| `submit_for_review` | 提交审核 | `package_id` |
| `publish_version` | 发布版本 | `package_id`, `version_id` |
| `offline_version` | 下线版本 | `package_id`, `version_id` |

**查询类（4 个）**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `list_my_packages` | 我管理的软件包列表 | 无（可选 `status`, `software_type`） |
| `list_business_systems` | 业务系统列表 | 无（可选 `keyword`） |
| `list_storage_backends` | 可用存储后端列表 | 无 |
| `check_compliance` | 业务系统软件合规检查 | `system_id` |

**审批类（4 个）【管理员】**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `list_pending_approvals` | 待审批事项列表 | 无（可选 `type`） |
| `approve_item` | 批准单个审批项 | `type`, `id` |
| `reject_item` | 拒绝单个审批项 | `type`, `id`, `reason` |
| `batch_approve` | 批量批准 | `type`, `ids` |

**用户管理（2 个）【管理员】**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `list_users` | 用户列表 | 无（可选 `keyword`） |
| `create_user` | 创建新用户 | `username`, `password`, `role` |

**存储管理（2 个）【管理员】**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `check_storage_health` | 存储后端健康检查 | 无 |
| `list_storage_backends_detail` | 存储后端完整配置 | 无 |

**软件包管理员视图（2 个）【管理员】**

| 工具名 | 说明 | 必填参数 |
|--------|------|---------|
| `list_all_packages` | 所有软件包（管理员视图） | 无（可选 `status`, `software_type`, `keyword`） |
| `offline_package` | 强制下线软件包 | `package_id`, `reason` |

---

## 7. 典型场景示例

### 场景一：发现并获取下载命令

```
用户：帮我找一个 Python 数据处理相关的包，给出安装命令

→ search_software(keyword="数据处理", type="PYPI")
  找到 pandas-utils (id: 42)

→ get_download_command(package_id=42)
  返回：pip install pandas-utils==2.1.0
```

### 场景二：申请订购软件

```
用户：为"风控系统"申请使用 Redis 镜像 v7.0，缓存用途

→ search_software(keyword="redis", type="DOCKER_IMAGE")
  找到 redis-official (id: 15, versionId: 67)

→ list_business_systems(keyword="风控")
  找到"风控系统"(id: 8)

→ apply_subscription(package_id=15, version_id=67, system_id=8,
    use_scene="风控系统需要 Redis 作为会话缓存和限流计数器")
  申请提交成功，等待审批
```

### 场景三：管理员处理审批

```
用户：查看待审批申请，批准合规的，拒绝不合规的

→ list_pending_approvals(type="SUBSCRIPTION")
  返回 3 条记录（id: 201, 202, 203）

→ batch_approve(type="SUBSCRIPTION", ids=[201, 202], comment="审核通过")

→ reject_item(type="SUBSCRIPTION", id=203,
    reason="申请版本已下线，请改用最新版本")
```

### 场景四：发布新软件包

```
用户：发布一个 log-analyzer Python 工具包，版本 1.0.0

→ list_storage_backends()
  找到 PyPI 存储后端 (id: 3)

→ create_software_package(package_name="Log Analyzer",
    package_key="log-analyzer", software_type="PYPI")
  创建成功，packageId: 88

→ add_version(package_id=88, version_no="1.0.0", storage_backend_id=3)
  versionId: 156

  [用户手动上传制品到 Nexus]

→ submit_for_review(package_id=88)
  已提交，等待管理员审核
```

---

## 8. 常见问题

**Q：`/api/mcp` 返回 401？**

检查 Basic Auth 编码。必须用 `echo -n`（不含换行），否则 Base64 结果不同。

**Q：工具调用返回 `API错误 [403]`？**

当前账号无权执行该操作。审批、用户管理等工具需要管理员角色，后端统一鉴权。

**Q：工具调用返回 `API错误 [401]`？**

OSRM 账号密码错误，或账号已被禁用，请联系管理员。

**Q：如何确认工具已加载？**

Claude Code 中输入 `/mcp` 查看已加载的 MCP 服务器和工具列表。
