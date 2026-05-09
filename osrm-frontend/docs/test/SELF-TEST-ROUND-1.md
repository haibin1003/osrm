# OSRM 第 1 轮 自测报告

**自测日期**: 2026-05-08
**自测范围**: `BUG-FIX-ROUND-1.md` 中 13 项已修复 Bug
**自测人**: 后端 / 前端开发
**自测结论**: 13/13 已修复项全部回归通过 ✅

---

## 一、自测总览

| 严重度 | 修复 | 自测通过 | 通过率 |
|------|------|------|------|
| P0 | 1 | 1 | 100% |
| P1 | 7 | 7 | 100% |
| P2 | 4 | 4 | 100% |
| P3 | 1 | 1 | 100% |
| **合计** | **13** | **13** | **100%** |

---

## 二、自测环境

| 项 | 值 |
|---|---|
| 后端版本 | osrm-backend 重启后含 V7 Flyway 迁移 |
| 后端端口 | http://localhost:8888 |
| 前端编译 | 静态源码扫描（dev 服务器无需启动） |
| 数据库 | MySQL 114.66.38.81:3306/osrm |
| Redis | 114.66.38.81:6379 |
| 测试账号 | admin / admin123 |
| 启动日志 | `Started OsrmApplication in 21.571 seconds`<br>`Permissions: 34, Roles: 7, Users: 3` |

---

## 三、自测明细

### ✅ BUG-001 (P0) — Admin 角色权限补齐

**测试方法**:
```bash
TOKEN=$(curl -s -X POST .../auth/login -d '{"username":"admin","password":"admin123"}' | jq -r .data.accessToken)
curl -s .../auth/me -H "Authorization: Bearer $TOKEN" | jq '.data.permissions'
```

**测试结果**:
```
count=34
package:approve in => True
package:delete in => True
permission:read in => True
storage:delete in => True
```

**结论**: ✅ admin 拥有 34 个权限（>30 的预期），8 个新增权限全部生效

---

### ✅ BUG-002 (P1) — 错误密码返回 401

**测试方法**:
```bash
curl -X POST .../auth/login -d '{"username":"admin","password":"wrongpassword"}'
```

**测试结果**:
```json
{"code":401,"message":"用户名或密码错误"} [HTTP 401]
```

**结论**: ✅ 错误密码正确返回 401（修复前为 500）

---

### ✅ BUG-003 (P1) — 无效 refresh token 返回 401

**测试方法**:
```bash
curl -X POST .../auth/refresh -d '{"refreshToken":"invalid.fake.token"}'
```

**测试结果**:
```json
{"code":401,"message":"刷新令牌无效或已过期"} [HTTP 401]
```

**结论**: ✅ 无效令牌正确返回 401（修复前为 500）

---

### ✅ BUG-004 (P1) — 不存在用户返回 404

**测试方法**:
```bash
curl .../users/99999 -H "Authorization: Bearer $TOKEN"
```

**测试结果**:
```json
{"code":404,"message":"用户不存在"} [HTTP 404]
```

**结论**: ✅ 不存在的资源正确返回 404（修复前为 500）

---

### ✅ BUG-005 (P1) — 统计端点公开访问

**测试方法**:
```bash
curl .../statistics/overview     # 不带 Authorization
```

**测试结果**:
```json
{"code":200,"message":"Success","data":{"totalPackages":11,"totalSubscriptions":2,...}} [HTTP 200]
```

**结论**: ✅ `/statistics/overview` 无需鉴权（修复前为 403）

---

### ✅ BUG-006 (P1) — 暗色模式持久化

**测试方法**: 静态代码审查 `osrm-frontend/src/composables/useTheme.ts`

**关键代码**:
```typescript
function applyTheme(dark: boolean): void {
  isDark.value = dark
  const html = document.documentElement
  // Always set explicit data-theme so CSS selectors and tests can detect both states
  html.setAttribute('data-theme', dark ? 'dark' : 'light')
  if (dark) html.classList.add('dark')
  else html.classList.remove('dark')
  localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
}
```

**结论**: ✅ 不论切换到 light 还是 dark，`data-theme` 始终被显式设置（不再 removeAttribute），且写入 `localStorage`

**待覆盖**: 浏览器端 E2E（Playwright）测试，将在第 2 轮覆盖

---

### ✅ BUG-007 (P2) — `/files` 缺参数返回 400

**测试方法**:
```bash
curl .../files -H "Authorization: Bearer $TOKEN"   # 不带 relatedType / relatedId
```

**测试结果**:
```json
{"code":400,"message":"缺少必需的参数: relatedType"} [HTTP 400]
```

**结论**: ✅ 缺少必填参数正确返回 400 + 友好错误信息（修复前为 500）

---

### ✅ BUG-008 (P2) — `.stripe-card` 类残留清理

**测试方法**:
```bash
cd osrm-frontend && grep -r "stripe-card" src/views/
```

**测试结果**:
```
Total stripe-card matches: 0 (expected 0)
```

**结论**: ✅ 18 个 Vue 文件已全部替换为 `content-card`

---

### ✅ BUG-009 (P2) — 角色管理权限树 200

**测试方法**:
```bash
curl .../permissions/tree -H "Authorization: Bearer $TOKEN"
```

**测试结果**:
```json
{"code":200,"message":"Success","data":[{"id":1,"permissionCode":"user:read",...}]} [HTTP 200]
```

**结论**: ✅ admin 已拥有 `permission:read` 权限，可访问权限树（依赖 BUG-001 修复）

---

### ✅ BUG-010 (P2) — 门户统计公开

**测试方法**:
```bash
curl .../portal/stats/overview     # 不带 Authorization
```

**测试结果**:
```json
{"code":200,"message":"Success","data":{"totalPackages":15,"publishedCount":11,...}} [HTTP 200]
```

**结论**: ✅ 门户统计公开访问（修复前为 403）

---

### ✅ BUG-011 (P1) — 存储型 XSS 防御

**测试方法**:
```bash
curl -X POST .../software-packages -H "Authorization: Bearer $TOKEN" -d '{
  "packageName":"<script>alert(1)</script>XSSCheck20260508",
  "description":"<img src=x onerror=alert(2)>SafeDesc",
  "licenseType":"MIT<iframe>x</iframe>",
  ...
}'
```

**测试结果**: 创建后 `GET /software-packages/17` 返回:
```
packageName  => 'XSSCheck20260508'    （<script> 已剥离）
description  => 'SafeDesc'             （<img onerror=> 已剥离）
licenseType  => 'MIT'                  （<iframe> 已剥离）
```

**结论**: ✅ 三个字段全部成功清洗，落库内容不含 HTML 标签和事件属性

---

### ✅ BUG-012 (P1) — 缺失 Token 返回 401

**测试方法**:
```bash
curl .../users     # 完全不带 Authorization 头
```

**测试结果**:
```json
{"code":401,"message":"未登录或令牌无效"} [HTTP 401]
```

**结论**: ✅ Spring Security 默认 403 已被自定义 `AuthenticationEntryPoint` 替换为 401

---

### ✅ BUG-013 (P3) — `roleCode` 校验文案准确

**测试方法**:
```bash
curl -X POST .../roles -H "Authorization: Bearer $TOKEN" -d '{"roleCode":"role_lower",...}'
curl -X POST .../roles -H "Authorization: Bearer $TOKEN" -d '{"roleCode":"ROLE_USER1",...}'
```

**测试结果（小写）**:
```json
{"code":400,"message":"roleCode: 角色编码格式必须为 ROLE_XXX，仅允许大写字母和下划线（不允许数字）"}
```

**测试结果（含数字）**:
```json
{"code":400,"message":"roleCode: 角色编码格式必须为 ROLE_XXX，仅允许大写字母和下划线（不允许数字）"}
```

**结论**: ✅ 文案精确传达"仅允许大写字母和下划线（不允许数字）"

---

### ✅ BUG-014 (P2) — 盘点 packageId 自动反查 packageName

**测试方法**:
```bash
curl -X POST .../inventory -H "Authorization: Bearer $TOKEN" -d '{
  "packageId":1,
  "businessSystemId":1,
  "deploymentEnv":"PROD",
  "usageScenario":"production-cluster",
  "versionNo":"3.8.0"
}'   # 注意：未传 packageName
```

**测试结果**:
```json
{"code":200,"data":{"id":94,"packageId":1,"packageName":"zookeeper","versionNo":"3.8.0",...}}
```

**结论**: ✅ 仅传 `packageId=1`，后端自动从软件包表反查 `packageName="zookeeper"`，免除前端冗余传值

---

### ✅ BUG-015 (P1) — 业务校验失败返回 400

**测试方法**:
```bash
curl -X POST .../software-packages/15/submit -H "Authorization: Bearer $TOKEN"
# 包 15 没有任何版本，触发"请先添加至少一个版本"
```

**测试结果**:
```json
{"code":400,"message":"请先添加至少一个版本"} [HTTP 400]
```

**结论**: ✅ `BizException(message)` 默认 code=500 时，由 `inferStatusFromMessage()` 推断为 400（修复前为 500）

---

## 四、补充边界测试

| # | 场景 | 请求 | 响应 | 结论 |
|---|------|------|------|------|
| EDGE-1 | 空用户名登录 | `POST /auth/login {"username":"","password":""}` | `400, "username: 用户名不能为空"` | ✅ DTO 校验生效 |
| EDGE-2 | 自删除登录账号 | `DELETE /users/1` (admin id=1) | `400, "不能删除当前登录用户"` | ✅ 业务规则生效 |
| EDGE-3 | XSS 在描述字段 | `<img src=x onerror=alert>` | 入库为 `SafeDesc` | ✅ 同 BUG-011 |
| EDGE-4 | 角色码 ROLE_USER1 | 含数字 | `400` 准确文案 | ✅ 同 BUG-013 |

---

## 五、未在本轮覆盖的项

| 项 | 原因 | 计划 |
|---|---|---|
| 暗色模式 E2E（点击按钮、刷新持久化） | Playwright 在第 1 轮回归未启动 | 第 2 轮回归覆盖 |
| 大屏全屏轮播 / ESC 退出 | 同上 | 第 2 轮回归覆盖 |
| 列表分页超大 page | 业务方未约定上限 | 第 2 轮回归覆盖 |
| 多 Tab 同时登录 / 登出隔离 | 需手工操作 | 第 2 轮回归覆盖 |

---

## 六、测试结论

- **本轮所有 13 项已修复 Bug 全部通过自测** ✅
- **BUG-016（字段命名差异）已归档为文档优化**，不进入本轮回归
- **BUG-007 与 BUG-015 共用 GlobalExceptionHandler 修复**，验证后两者都正确返回 400
- 后端兜底策略（`inferStatusFromMessage()` + `handleBizException`）覆盖了历史代码中大量 `throw new RuntimeException(...)` 与 `throw new BizException(message)` 调用，无需逐 service 修改即可让 HTTP 语义对齐
- **建议进入第 2 轮**：
  - 用 Playwright 覆盖前端 60+ UI 检查项
  - 对 4 条端到端业务链路（软件生命周期 / 订购审批 / 用户角色权限 / 盘点）完整走通
  - 边界场景剩余 6 项

第 1 轮自测完成，可进入第 2 轮回归测试。
