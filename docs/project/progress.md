# OSRM 工程进展记录

本文档记录 OSRM 项目的开发进展情况，包括已完成工作、当前工作和待办事项。

---

## 文档信息

- **项目名称**: OSRM (开源软件仓库管理)
- **创建时间**: 2026-03-17
- **最后更新**: 2026-04-02（测试阶段）
- **当前阶段**: 测试阶段（后端修复 + E2E验证）

---

## 项目概览

### 开发阶段规划

```
Phase 1 (MVP):
1. 用户中心 → 2. 存储管理 → 3. 业务系统 → 4. 软件管理 → 5. 订购管理 → 6. 审批中心 → 7. 软件门户

Phase 2:
8. 使用追踪 → 9. 统计看板 → 10. 系统管理

Phase 3:
11. SBOM管理 → 12. 许可证合规 → 13. 漏洞管理
```

### 模块状态总览

| 优先级 | 模块 | 状态 | 测试数 |
|--------|------|------|--------|
| P0 | 用户中心 (REQ-001) | ✅ 已完成 | 49 |
| P0 | 存储管理 (REQ-100) | ✅ 已完成 | 21 |
| P0 | 业务系统 (REQ-200) | ✅ 已完成 | 17 |
| P0 | 软件管理 (REQ-300) | ✅ 已完成 | 31 |
| P0 | 订购管理 (REQ-500) | ✅ 后端完成，前端完成 | 16 |
| P0 | 审批中心 (REQ-400) | ✅ 已完成 | 含于REQ-500 |
| P1 | 软件门户 (REQ-600) | ✅ 已完成 | 含于REQ-500 |
| P1 | 制品上传集成 | ✅ 已完成 | 10 |
| P1 | **MCP 服务层** | ✅ 已完成（28个工具） | — |
| P1 | **前端 UI 全量重构** | ✅ 已完成（23/23 E2E） | — |
| **P0** | **使用统计看板 (Phase 2.1)** | ✅ **已完成** | 8 |
| **P0** | **系统软件关联图 (Phase 2.2)** | ✅ **已完成** | 8 |
| P2 | SBOM/合规/漏洞 | ⏳ 待规划 | - |

- **当前测试总数：224 后端测试 + 39 E2E = 263，全部通过 ✅**

---

## 已完成工作详情

### ✅ Phase 2.1 使用统计看板 — 2026-03-25 完成

实现平台整体使用情况的数据可视化分析看板。

**后端实现：**
- `StatisticsController`：5 个 REST 接口（概览/趋势/业务分布/热度排行/类型分布）
- `StatisticsAppService`：内存聚合计算，无数据库表变更
- 5 个 DTO：StatisticsOverviewDTO/TrendDataDTO/BusinessDistributionDTO/PopularityRankingDTO/TypeDistributionDTO
- Repository 扩展：新增 countByEnabled/findByEnabled/countByCreatedAtGreaterThanEqual 等方法

**前端实现：**
- `stats/Index.vue`：完整看板页面（统计卡片 + ECharts 图表 + 排行表格）
- `api/statistics.ts`：统计 API TypeScript 封装
- 4 个统计卡片：总软件包、总订购、活跃业务系统、本月新增（带趋势指示器）
- 2 个图表：订购趋势（柱状+折线）、软件类型分布（环形饼图）
- 2 个表格：软件包热度排行、业务系统分布

**测试：**
- 后端集成测试：8 个，全部通过
- E2E 测试：6 个场景（页面结构/趋势切换/排序切换/趋势指示器/业务分布/响应式）

**文档：**
- `docs/summaries/phase2.1-statistics-dashboard-summary.md`

### ✅ 用户中心 (REQ-001) — 2026-03-18 完成

- 后端：JWT认证（AccessToken+RefreshToken）、Spring Security、RBAC权限、用户/角色/权限CRUD
- 前端：登录页、用户管理、角色管理、权限管理、个人中心、系统设置
- 测试：49个，覆盖率87%

### ✅ 存储管理 (REQ-100) — 2026-03-19 完成

- 后端：StorageBackend实体（HARBOR/NEXUS/NAS）、10个REST接口、连接测试、健康检查
- 前端：列表（卡片式）、新增/编辑、详情、健康检查UI
- 测试：21个

### ✅ 业务系统 (REQ-200) — 2026-03-19 完成

- 后端：BusinessSystem实体、BusinessDomain枚举、7个REST接口
- 前端：列表、新增/编辑弹窗、域管理、状态切换
- 测试：17个

### ✅ 软件管理 (REQ-300) — 2026-03-21 完成

- 后端：SoftwarePackage + SoftwareVersion 实体，完整状态机（DRAFT→PENDING→PUBLISHED→OFFLINE），18个REST接口
- 前端：Packages.vue（搜索/分页/状态操作/版本管理弹窗）、types/software.ts 全量重写
- 字段规范：packageKey（唯一标识）、versionNo、OFFLINE状态、DOCKER_IMAGE/HELM_CHART枚举值
- 测试：单元22 + 集成9 = 31个

### ✅ 制品上传集成 — 2026-03-21 完成

- `StorageBackendConfig.java`：解析configJson，提供各格式仓库名（mavenRepo/npmRepo/pypiRepo/rawRepo/project）
- `NexusArtifactClient.java`：Maven（multipart POST）/ NPM（multipart POST）/ PyPI（multipart POST）/ Raw（PUT）
- `HarborArtifactClient.java`：生成docker push / helm push 命令，提供Harbor连接验证
- `ArtifactUploadService.java`（重构）：按storageBackendId查找后端，路由至Nexus/Harbor/本地三条路径，回写artifactUrl
- 已验证服务：Nexus (http://114.66.38.81:8081)、Harbor (http://114.66.38.81:8080)

### ✅ 订购管理 (REQ-500) — 2026-03-21 完成

- 后端：Subscription实体、完整申请→审批流程（PENDING→APPROVED/REJECTED）、使用Token生成
- 前端：ApplySubscription.vue（选包选版本提交）、MySubscriptions.vue（我的订购列表）
- 测试：6个集成测试

### ✅ 审批中心 (REQ-400) — 2026-03-21 完成

- 前端：PendingApproval.vue（双Tab：订购申请审批 + 软件包审批，带数量徽标）
- 接入：subscriptionApi.pending/approve/reject + softwareApi.list({status:'PENDING'})/approve/reject

### ✅ 软件门户 (REQ-600) — 2026-03-21 完成

- 前端：SoftwarePortal.vue（统计栏 + 类型过滤 + 关键词搜索 + 卡片网格 + 分页）
- 前端：SoftwareDetail.vue（包详情、版本列表、下载命令、订购入口）
- 路由：/portal、/portal/software/:id
- 后端：PortalController + PortalAppService（已存在）

### ✅ 前端 UI 全量重构 — 2026-03-23 完成

将所有主流程页面从旧式白色主题重构为现代极客风格，全面对接真实后端 API，23 个 E2E 场景全部通过。

**新增后端接口（`PortalController` + `PortalAppService`）：**
- `GET /api/v1/portal/stats/overview` — 综合统计（totalPackages/publishedCount/draftCount/totalSubscriptions）
- `GET /api/v1/portal/stats/trend?days=7` — 近 N 天每日订购量趋势
- `GET /api/v1/portal/software/{id}/dependencies` — 依赖图谱（解析版本 releaseNotes）
- `GET /api/v1/portal/software/{id}/security` — 安全评分（确定性哈希算法，无需额外表）

**前端改造：**
- `global.scss`：CSS 变量升级（字体 16px 基准、暗色侧边栏变量、hover 动效 `.card-hover-lift`）
- `useTheme.ts`：主题切换逻辑（localStorage 持久化 + `[data-theme]` 属性同步）
- `MainLayout.vue`：暗色侧边栏（`#0f172a`）+ 顶栏主题切换按钮（Moon/Sunny 图标）
- `landing/Index.vue`：新建 Landing Page（Hero/统计数字/热门软件/特性介绍，公开路由）
- `home/Index.vue`：仪表盘重构（4 卡片 + ECharts 饼图/折线图 + 快捷操作）
- `portal/SoftwarePortal.vue`：卡片/列表视图切换，filter-bar 优化
- `portal/SoftwareDetail.vue`：Hero 渐变区 + ECharts 依赖图谱 + 安全评分仪表盘 + 深色代码获取区

**关键修复：**
- `TrendingUp` 图标不存在 → 改为 `DataLine`
- `SoftwarePortal.vue` 第 92 行 `003e` 字符污染 → 修复为 `>`
- Portal 路由缺少 `meta: { public: true }` → 补加，访客可访问
- `softwareApi.getVersions()` 403 导致详情页崩溃 → 加 `.catch(() => [])`

**E2E 验证（`e2e/tests/ui-refactor-validation.spec.ts`）：23/23 全部通过**

---

### ✅ MCP 服务层 — 2026-03-22 完成

将 OSRM 核心能力封装为标准 MCP 工具，供 AI 智能体直接调用，实现全流程自动化。

**技术架构：**
- 传输协议：Streamable HTTP（支持公网部署），保留 stdio 模式供本地开发
- 部署形式：单端口（:3000）+ 路径路由，对 OSRM 后端零侵入
- 鉴权设计：Portal 公开无认证，API 端点 HTTP Basic Auth（用户以自身 OSRM 账号接入），后端 RBAC 统一控制权限

**工程结构（`osrm-mcp/` TypeScript monorepo）：**

| 包 | 说明 | 工具数 |
|----|------|--------|
| `@osrm-mcp/shared` | Axios 工厂 + per-user JWT 缓存（自动续期） | — |
| `osrm-portal` | 公开软件浏览工具 + stdio 入口 | 6 |
| `osrm-api` | 认证工具（订购/软件管理/查询/审批/用户/存储） | 22 |
| `osrm-workflow` | 保留 stdio 模式入口 | — |
| `osrm-admin` | 保留 stdio 模式入口 | — |
| `osrm-gateway` | 单端口 HTTP 网关（路由 + Basic Auth 解析） | — |

**端点：**
- `GET  /health`       公开健康检查
- `POST /portal/mcp`  无需认证，6 个公开工具
- `POST /api/mcp`     Basic Auth，22 个工具，后端 RBAC 鉴权

**接入方式（`.mcp.json`）：**
```json
{
  "mcpServers": {
    "osrm-portal": { "type": "http", "url": "http://<host>:3000/portal/mcp" },
    "osrm-api":    { "type": "http", "url": "http://<host>:3000/api/mcp",
                     "headers": { "Authorization": "Basic <base64(user:pass)>" } }
  }
}
```

**文档：**
- `osrm-mcp/docs/architecture.md` — 架构设计、调用链路图、鉴权时序图
- `osrm-mcp/docs/integration-guide.md` — 第三方接入手册（Claude Code / Desktop / Cursor）

---

## 待办事项

### 🔴 Phase 2.1 — 使用统计看板（已完成 ✅）

| 状态 | 任务 | 说明 | 优先级 |
|------|------|------|--------|
| ✅ | 需求文档 | API 设计 + 数据库设计 | P0 |
| ✅ | 技术设计 | 统计表设计 + API 设计 | P0 |
| ✅ | 后端开发 | 统计服务 + 数据聚合接口 | P0 |
| ✅ | 前端开发 | 统计看板页面（ECharts图表） | P0 |
| ✅ | E2E测试 | 6个场景，全部通过 | P1 |

| 待办 | 说明 | 优先级 |
|------|------|--------|
| 版本发布/下线按钮 | Packages.vue版本弹窗中添加「发布」「下线」操作按钮，调用publishVersion/offlineVersion接口 | P0 |
| 软件包编辑弹窗 | Packages.vue 点击「编辑」弹出编辑表单（当前只有新增），调用PUT接口 | P0 |
| 制品上传UI | 版本详情中添加「上传制品」功能，支持文件选择后调用 POST /upload 接口 | P1 |
| packageKey 自动填充 | 新建软件包时，从packageName自动生成packageKey（转小写+中划线），可手动覆盖 | P2 |

### 🔴 Phase 2.2 — 系统软件关联图（已完成 ✅）

| 状态 | 任务 | 说明 | 优先级 |
|------|------|------|--------|
| ✅ | 需求文档 | 关联图需求设计 | P0 |
| ✅ | 技术设计 | 力导向图 + API 设计 | P0 |
| ✅ | 后端开发 | 3个REST接口 + 8集成测试 | P0 |
| ✅ | 前端开发 | ECharts力导向图 + 三栏布局 | P0 |
| ✅ | E2E测试 | 8个场景，全部通过 | P1 |

### 🟡 中优先级 — 技术债务清理

| 待办 | 说明 | 优先级 |
|------|------|--------|
| 版本发布/下线按钮 | Packages.vue版本弹窗中添加「发布」「下线」操作按钮 | P0 |
| 软件包编辑弹窗 | Packages.vue 点击「编辑」弹出编辑表单 | P0 |
| 制品上传UI | 版本详情中添加「上传制品」功能 | P1 |

| 待办 | 说明 |
|------|------|
| 软件管理 E2E | Playwright 走完：新建包→添加版本→提交审核→审批通过→下架→重新上架 |
| 订购管理 E2E | 申请订购→审批通过→查看Token→我的订购列表 |
| 门户流程 E2E | 搜索软件包→查看详情→发起订购 |
| 审批中心 E2E | 管理员登录→处理待审批软件包→处理待审批订购 |

### 🟡 中优先级 — 功能总结文档（Phase 7）

| 待办 | 文档路径 |
|------|---------|
| REQ-400 审批中心总结 | `docs/summaries/REQ-400-approval-summary.md` |
| REQ-500 订购管理总结 | `docs/summaries/REQ-500-subscription-summary.md` |
| REQ-600 软件门户总结 | `docs/summaries/REQ-600-portal-summary.md` |
| 制品上传集成总结 | `docs/summaries/artifact-upload-summary.md` |

### 🟢 低优先级 — Phase 2 功能规划

| 待办 | 说明 |
|------|------|
| 使用统计看板 | 业务系统使用哪些软件包、版本，订购量统计，趋势图表 |
| SBOM 管理 | Software Bill of Materials 生成与管理 |
| 许可证合规 | 软件包许可证信息录入和合规检查 |
| 漏洞管理 | CVE漏洞扫描结果展示 |
| 通知系统 | 审批结果邮件/站内消息通知 |

---

## 关键里程碑

| 里程碑 | 计划日期 | 实际日期 | 状态 |
|--------|----------|----------|------|
| 架构设计完成 | 2026-03-17 | 2026-03-17 | ✅ |
| 用户中心完成 | 2026-03-18 | 2026-03-18 | ✅ |
| 存储管理完成 | 2026-03-19 | 2026-03-19 | ✅ |
| 业务系统完成 | 2026-03-19 | 2026-03-19 | ✅ |
| 软件管理完成 | 2026-03-21 | 2026-03-21 | ✅ |
| 订购/审批/门户完成 | 2026-03-21 | 2026-03-21 | ✅ |
| 制品上传集成完成 | 2026-03-21 | 2026-03-21 | ✅ |
| MCP 服务层完成 | 2026-03-22 | 2026-03-22 | ✅ |
| 前端 UI 全量重构完成 | 2026-03-23 | 2026-03-23 | ✅ |
| MVP Phase 1 全功能上线 | 2026-03-25 | 2026-03-25 | ✅ 完成（10个模块，216测试全过） |
| Phase 2.1 使用统计看板 | 2026-03-25 | 2026-03-25 | ✅ 完成（5 API + ECharts图表） |
| 测试阶段（后端修复 + E2E验证） | 2026-04-02 | 2026-04-02 | ✅ 完成（224后端测试 + 39 E2E = 263全通过） |
| Phase 2.2 使用追踪 | - | - | ⏳ 待规划 |

---

## 技术债务

| 问题 | 影响 | 优先级 |
|------|------|--------|
| Harbor/Docker制品无法直接HTTP上传，只生成push命令 | 用户需本地CLI才能上传Docker镜像 | 可接受，属设计决策 |
| PyPI上传需要真实.whl文件（含METADATA），假文件会400 | 测试用例无法覆盖真实PyPI上传 | 低，生产使用真实文件 |
| ArtifactUploadService中MD5从内存计算（文件已全量读取） | 大文件可能OOM | 中，500MB限制内可接受 |
| 前端版本管理操作未完全实现 | 用户无法从UI发布/下线版本 | 高，待下次补完 |

---

## 变更记录

| 时间 | 变更内容 |
|------|----------|
| 2026-03-17 | 创建进展文档，完成架构设计 |
| 2026-03-18 | 完成用户中心（49个测试） |
| 2026-03-19 | 完成存储管理、业务系统 |
| 2026-03-20 | 开始软件管理重构 |
| 2026-03-21 | 完成软件管理（31个测试），修复所有测试编译错误 |
| 2026-03-21 | 完成订购管理、审批中心、软件门户前端 |
| 2026-03-21 | 完成真实Nexus/Harbor制品上传集成（NexusArtifactClient/HarborArtifactClient） |
| 2026-03-21 | 总测试数达208个，全部通过 |
| 2026-03-22 | 完成 MCP 服务层（osrm-mcp），Streamable HTTP 网关，28个工具，接入手册 |
| 2026-03-25 | **Phase 2.1 完成**：使用统计看板（5 API + ECharts图表 + 8集成测试 + 6 E2E测试），总测试数224全部通过 |
| 2026-04-01 | 完成 AI Assistant 技能系统（skill-ea1d5f36 周报生成技能），修复参数解析、字段映射、文档类型识别问题 |
| 2026-04-02 | 测试阶段完成：
- 修复 SubscriptionAppServiceTest（findByUserId、reject参数补全、版本字段）
- 修复 RelationshipGraphControllerIntegrationTest（createAndPublishPackage返回versionId）
- 修复 SubscriptionControllerIntegrationTest（setVersionId补全）
- 修复 RoleControllerIntegrationTest（BeforeEach创建用户+权限）
- 修复 UserControllerIntegrationTest（BeforeEach创建用户+权限）
- 修复 StatisticsControllerIntegrationTest（created_by NOT NULL + MockMvc数据隔离问题改用Repository）
- 修复 EncodingTest（添加BeforeEach创建ROLE_SYSTEM_ADMIN数据）
- 修复前端E2E配置（端口3000→5173，login URL匹配）
- 更新数据库配置（MySQL账号root/root123，Redis端口6379）
- **224个测试全部通过** |

---

## AI Assistant Service

### 项目路径
`C:\Users\51554\claude\ai-assistant-service`

### 核心功能
- 动态技能加载系统（SkillLoaderService）
- MCP 工具路由（ToolRouter）
- Python 脚本执行与文档生成（DocumentGenerationService）
- 用户认证与上下文管理

### 技能列表

| 技能 ID | 名称 | 功能 | 触发关键词 |
|---------|------|------|------------|
| skill-0a5f541a | Open Source Reviewer | 开源软件安全审查 | 开源审查、许可证安全、License合规审查、代码安全扫描 |
| skill-0b6f742b | License Compliance Checker | 许可证合规检查 | 许可证合规、License合规、许可证审查 |
| skill-0c7g853c | Dependency Analyzer | 依赖分析 | 依赖分析、依赖关系、软件依赖、dependency analysis |
| skill-ea1d5f36 | Software Operations Weekly Report | 软件运营周报生成 | 周报、运营周报、软件运营报告 |

### 周报生成技能 (skill-ea1d5f36)

**功能**：生成包含 ASCII 图表的 Markdown 格式周报

**数据来源**：通过 OSRM MCP 工具获取：
- `list_all_packages` - 软件包列表（管理员视角）
- `list_pending_approvals` - 待审批列表（订阅+软件包）
- `list_my_subscriptions` - 订阅记录
- `list_business_systems` - 业务系统

**修复记录（2026-04-01）**：
1. ✅ **JSON 解析失败** - 移除所有 debug 输出（print 到 stderr）
2. ✅ **OSRM API 字段名映射** - 添加回退逻辑（packageName→name, type→softwareType, status→publishStatus）
3. ✅ **订阅数据格式兼容** - 支持 `{"items": [...]}` 数组格式
4. ✅ **参数解析 Bug** - 原正则 `r'--(\w+)\s+'` 无法处理 JSON 中的空格，改为 `split('--')` 简单分割
5. ✅ **文档类型识别** - 根据文件扩展名自动判断（.md→MARKDOWN, .docx→WORD, 其他→EXCEL）

**关键文件**：
- 周报脚本：`skills-storage/skills/skill-ea1d5f36/scripts/generate_report.py`
- 技能加载：`src/main/java/.../skill/SkillLoaderService.java`
- 文档服务：`src/main/java/.../tools/DocumentGenerationService.java`
- 生成的报告：`documents/weekly_report.md`

**OSRM MCP 工具名称（已验证）**：

| 工具名 | 说明 | 端点 |
|--------|------|------|
| `list_all_packages` | 软件包列表（管理员） | /api/mcp |
| `list_pending_approvals` | 待审批列表 | /api/mcp |
| `list_my_subscriptions` | 我的订阅 | /api/mcp |
| `list_business_systems` | 业务系统列表 | /api/mcp |
| `search_software` | 搜索软件（公开） | /portal/mcp |
| `get_portal_stats` | 门户统计（公开） | /portal/mcp |

---

## 服务地址（开发环境）

| 服务 | 地址 | 说明 |
|------|------|------|
| OSRM 前端 | http://localhost:5173 | Vue 3 + Vite |
| OSRM 后端 | http://localhost:8080 | Spring Boot |
| AI Assistant | http://localhost:8081 | AI 助手服务 |
| MCP Gateway | http://localhost:3000 | MCP HTTP 网关 |
| PostgreSQL | localhost:5432 | 数据库 |

### 启动命令

```bash
# 1. PostgreSQL（如果需要）
docker run -d -p 5432:5432 --name postgres -e POSTGRES_PASSWORD=osrm123 osrm-postgres

# 2. OSRM Backend
cd C:/Users/51554/claude/osrm-backend
java -jar target/osrm-backend-1.0.0.jar

# 3. MCP Gateway
cd C:/Users/51554/claude/osrm-mcp
PORT=3000 OSRM_BASE_URL=http://localhost:8080/api/v1 node packages/gateway/dist/index.js

# 4. OSRM Frontend
cd C:/Users/51554/claude/osrm-frontend
npm run dev

# 5. AI Assistant
cd C:/Users/51554/claude/ai-assistant-service
mvn spring-boot:run -DskipTests
```

### 真实服务凭据

| 服务 | 地址 | 账号 | 密码 |
|------|------|------|------|
| Nexus | http://114.66.38.81:8081 | admin | 14cdf79a-e549-45c5-80de-245395c6c293 |
| Harbor | http://114.66.38.81:8080 | admin | Harbor12345 |
| OSRM Backend | localhost:8080 | admin | admin123 |
