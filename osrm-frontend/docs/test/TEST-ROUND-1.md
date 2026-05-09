# OSRM 第 1 轮测试报告

**测试日期**: 2026-05-08
**测试范围**: 全量功能测试（后端 API + 前端页面 + UI 检查 + E2E 业务流程 + 边界异常）
**测试环境**: 后端 localhost:8888, 前端 localhost:5173, MySQL/Redis 114.66.38.81

---

## 一、测试概况

| 指标 | 数量 |
|------|------|
| 后端 API 测试点 | ~95 (已执行) |
| 前端页面检查 | 15 个页面 |
| E2E 业务链路 | 4 条 |
| 边界异常测试 | 10 项 |
| 发现 Bug 总数 | **15** |
| P0 阻断性 | 1 |
| P1 严重 | 7 |
| P2 一般 | 5 |
| P3 轻微 | 2 |

---

## 二、Bug 清单

### P0 — 阻断性

| ID | 模块 | 描述 | 现象 |
|----|------|------|------|
| **BUG-001** | 权限系统 | Admin `ROLE_SYSTEM_ADMIN` 角色缺少关键权限 | 从 `/auth/me` 返回的权限列表缺少 `package:approve`、`package:create`、`package:update`、`package:delete`、`permission:read`、`permission:create`、`business-system:read`、`business-system:approve`、`storage:create`、`storage:delete`。导致管理后台多项功能不可用。 |

### P1 — 严重

| ID | 模块 | 描述 | 现象 |
|----|------|------|------|
| **BUG-002** | 认证 | 认证失败返回 HTTP 500 而非 401 | 错误密码登录、不存在用户登录时后端返回 `{code:500, message:"用户名或密码错误"}` 状态码 500，应返回 401 |
| **BUG-003** | 认证 | 无效 Refresh Token 返回 HTTP 500 而非 401 | `/auth/refresh` 传入无效 token 返回 500，应返回 401 |
| **BUG-004** | 全局 | 资源不存在返回 HTTP 500 而非 404 | `/users/99999`、`/software-packages/99999` 等不存在资源返回 500，应返回 404 |
| **BUG-005** | 统计 | `/statistics/*` 公开端点返回 403 | 首页仪表盘所有统计 API 返回 403 Forbidden，导致统计数据卡片和图表无数据 |
| **BUG-006** | 主题 | 暗色模式切换按钮无实际效果 | 点击 header 中 `.theme-toggle` 按钮，`document.documentElement` 未设置 `data-theme` 属性，CSS 变量不生效 |
| **BUG-011** | 安全（XSS） | 后端未对软件包名称/描述进行 HTML 转义/过滤 | 创建软件包时 `packageName="<script>alert('XSS')</script>"`、`description="<img src=x onerror=alert(1)>"` 直接入库。前端 Vue 默认转义未触发执行，但脏数据将影响其它消费方（导出、邮件、SDK），属于存储型 XSS |
| **BUG-012** | 认证 | 无效 / 过期 / 已注销 Token 返回 403 而非 401 | `Bearer expired.token.here`、缺失 Header、黑名单 Token 调用 `/auth/me` 均返回 403。HTTP 语义上未认证应使用 401 |

### P2 — 一般

| ID | 模块 | 描述 | 现象 |
|----|------|------|------|
| **BUG-007** | 存储 | `/files` 端点返回 HTTP 500 | `/api/v1/files` 返回 `{code:500, message:"系统错误，请稍后重试"}` |
| **BUG-008** | UI | `.stripe-card` 类未完全移除 | 9 个文件仍使用 `.stripe-card`：storage/Detail.vue, storage/Create.vue, storage/Edit.vue, system/UserManagement.vue, system/RoleManagement.vue, system/PermissionManagement.vue, subscription/PendingApproval.vue, subscription/ApplySubscription.vue, subscription/ApprovalHistory.vue |
| **BUG-009** | 权限 | 角色管理页权限树加载 403 | `/system/roles` 页面调用 `/permissions/tree` 返回 403（admin 缺少 `permission:read` 权限） |
| **BUG-010** | 存储 | `/stats/overview` 公开端点需要认证 | `/api/v1/portal/stats/overview` 无 auth header 时返回 500，统计端点配置不一致 |
| **BUG-014** | 盘点 | 盘点创建参数冗余且必填校验不一致 | `POST /inventory` 同时要求 `packageName` 与 `packageId` 必填；理论上 `packageId` 可推导出 `packageName`，存在数据冗余风险（前端传错时与后端实体不一致） |
| **BUG-015** | 文件 / 业务异常 | 业务校验错误返回 HTTP 500 | 软件包提交审批前未添加版本时返回 `{status:200, code:500, "请先添加至少一个版本"}`；用户创建密码长度不足返回 `code:500`；此类业务校验应使用 400 |

### P3 — 轻微

| ID | 模块 | 描述 | 现象 |
|----|------|------|------|
| **BUG-013** | 角色 | `roleCode` 校验规则与错误信息不匹配 | 提示 "角色编码格式必须为 ROLE_XXX，全大写"，但实际正则为 `^ROLE_[A-Z_]+$`，不允许数字。`ROLE_E2E` 等含数字代码会被拒绝。文案应明确说明 |
| **BUG-016** | 文档 | 部分接口字段命名前后端不一致 | `Subscription` 前端 DTO 使用 `useScene`，后端实体保存到 `usageScenario`；`User` 创建使用 `roleIds`（前端文档显示 `roleId`） |

---

## 三、通过测试的模块

### 后端 API

| 模块 | 通过 | 主要端点 |
|------|------|---------|
| 认证 (Auth) | 7/10 | login(正常), refresh(正常), logout, /me (已登录/未登录/黑名单) |
| 用户 (User) | 4/12 | 列表(200), 详情(200), 开发者403正确, 创建 |
| 角色 (Role) | 5/7 | 列表, 详情, 角色权限, 权限树, 创建（无数字 code） |
| 软件包 (Software) | 9/19 | 详情, 版本列表, 软件类型, 创建, 提交审批, 我的软件包, 添加版本 |
| 业务系统 (Business) | 2/7 | 列表, 业务域 |
| 存储 (Storage) | 2/11 | 列表, 存储类型 |
| 订购 (Subscription) | 6/7 | 我的订购, 申请, 审批通过, 拒绝, 待审批, Token 获取 |
| 盘点 (Inventory) | 5/10 | 设置(公开), 我的盘点, 创建, 审批通过 |
| 门户 (Portal) | 4/10 | 统计, 热门, 概览, 趋势 |
| 审批 (Approval) | 1/2 | 审批历史 |
| 关系图谱 (Tracking) | 1/3 | 全局图谱 |
| 设置 (Settings/Config) | 2/5 | 全部设置, 全部配置 |
| 分类 (Categories) | 1/5 | 分类树 |
| 标签 (Tags) | 1/4 | 标签列表 |

### 前端页面

| 页面 | 路由 | 状态 | 备注 |
|------|------|------|------|
| 落地页 | `/` / `/landing` | ✅ | 无错误，浅色 Hero 正常 |
| 登录页 | `/login` | ✅ | 表单、跳转正常 |
| 首页仪表盘 | `/home` | ⚠️ | 统计 API 403 导致图表无数据 |
| 软件门户 | `/browse` | ✅ | 11 个软件包，搜索/过滤/分页正常 |
| 软件详情 | `/browse/software/:id` | ✅ | Hero、版本列表正常；XSS 内容被 Vue 自动转义 |
| 软件管理 | `/software/packages` | ✅ | 列表正常 |
| 业务系统 | `/business/list` | ✅ | 列表正常 |
| 我的订购 | `/subscription/my` | ✅ | 列表正常 |
| 关系图谱 | `/tracking/relationship-graph` | ✅ | ECharts 正常渲染 |
| 用户管理 | `/system/users` | ✅ | 列表正常 |
| 角色管理 | `/system/roles` | ⚠️ | 权限树 403 错误 |
| 存储管理 | `/storage/list` | ✅ | 列表正常 |
| 盘点管理 | `/inventory/my` | ✅ | 列表正常 |
| 个人信息 | `/profile` | ✅ | 无错误 |
| 系统设置 | `/settings` | ✅ | 无错误 |

---

## 四、UI 专项检查

| 检查项 | 结果 |
|--------|------|
| 紫色残留 (`#635bff/#a259ff/#7c6fff`) | ✅ 无残留 |
| 渐变色 (`linear-gradient`) | ⚠️ 1 处（storage/Index.vue 非紫色渐变） |
| `.stripe-card` 残留 | ❌ 9 个文件 |
| 按钮无 translateY | ✅ |
| 字体 weight ≥ 400 | ✅ |
| 侧边栏样式 | ✅ 白色背景+蓝色高亮 |
| 响应式 768px | ✅ 侧边栏折叠正常 |
| 暗色模式切换 | ❌ 按钮存在但无效果 |

---

## 五、E2E 业务流程测试

| # | 流程 | 步骤 | 结果 |
|---|------|------|------|
| **E1** | 软件包生命周期 | 创建 → 添加版本 → 提交审批 → 审批通过 → 下线 → 重新发布 | ⚠️ 部分通过：创建/版本/提交 ✅；审批通过/下线/重新发布 ❌（被 BUG-001 阻断，admin 缺 `package:approve` 权限） |
| **E2** | 订购-审批 | 申请订购 → 审批通过 → 获取 Token / 申请 → 审批拒绝 | ✅ 全部通过：subscriptionNo 正确生成、状态流转正常、Token 携带过期时间和下载次数 |
| **E3** | 用户-角色-权限 | 创建角色 → 配置权限 → 创建用户 → 登录验证 | ✅ 全部通过：新用户正确继承角色权限（user:read, user:create, user:update），可访问对应资源 |
| **E4** | 盘点流程 | 创建盘点 → 审批通过 / 审批拒绝 | ✅ 通过：盘点单号 INV-YYYYMMDD-XXX 生成正常，状态从 PENDING → APPROVED |

---

## 六、边界与异常测试

| # | 场景 | 结果 | 备注 |
|---|------|------|------|
| **X1** | Token 过期/无效 | ⚠️ | 返回 403 而非 401（BUG-012） |
| **X2** | 网络断开 | — | 未单独测试（前端有 axios 拦截器） |
| **X3** | 超长输入 (10000 字符) | ✅ | 后端正确返回 400 "软件包名称长度在2到128个字符" |
| **X4** | XSS 注入 (`<script>...`) | ⚠️ | 后端未过滤入库（BUG-011），前端 Vue 自动转义未执行脚本，但数据脏 |
| **X5** | SQL 注入 (登录) | ✅ | 用户名格式校验拦截 "用户名只能包含字母、数字和下划线" |
| **X5b** | SQL 注入 (搜索) | ✅ | 参数化查询安全，无数据泄漏 |
| **X6** | 大分页 (page=99999) | ✅ | 返回空 content 数组 |
| **X7** | 删除不存在的资源 | ⚠️ | 返回 403（admin 权限缺失，BUG-001），实际场景应返回 404 |
| **X8** | 大文件元数据 | ⚠️ | 返回 500，应返回 400 友好错误（BUG-015） |
| **X9** | 特殊字符 (emoji/中文) | ✅ | 用户名 emoji 拒绝、realName emoji ✅、描述特殊字符 ✅ |
| **X10** | 多 Tab 登录 | ✅ | 多 Token 并存正常，单端登出不影响其他 Token，黑名单生效 |

---

## 七、测试结论

第 1 轮测试覆盖：
- 后端 15/20 个 Controller 的关键端点（~95 测试点）
- 前端全部 15 个核心页面
- 4 条 E2E 业务链路
- 10 项边界异常测试

发现 **15 个 Bug**：
- 1 个 P0（Admin 权限缺失阻断管理类业务）
- 7 个 P1（HTTP 状态码错误、统计 403、暗色模式失效、存储型 XSS）
- 5 个 P2（UI 残留、参数冗余、业务异常 500）
- 2 个 P3（文案不准确、字段命名不一致）

**修复建议优先级**：
1. **BUG-001**（Admin 权限）— 解锁后才能完整测试软件包生命周期、删除、审批等
2. **BUG-005 / BUG-010**（统计端点 403）— 修复后首页仪表盘可用
3. **BUG-002 / BUG-003 / BUG-004 / BUG-012 / BUG-015**（HTTP 状态码批量修复）— 统一异常处理器
4. **BUG-011**（XSS 入库）— 增加 HTML sanitizer / 黑名单字段过滤
5. **BUG-006**（暗色模式）— 完善 ThemeToggle 实现
6. **BUG-007 / BUG-008 / BUG-009**（次要修复）

第 1 轮测试完成。等待修复后进入第 2 轮回归测试。
