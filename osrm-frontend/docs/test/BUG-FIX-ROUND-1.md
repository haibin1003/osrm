# OSRM 第 1 轮 Bug 修复报告

**修复日期**: 2026-05-08
**修复范围**: 第 1 轮测试发现的 15 个 Bug
**修复人**: 后端 / 前端开发
**修复结果**: 13 项已修复并提交，2 项归档说明

---

## 一、修复总览

| 严重度 | 数量 | 已修复 | 说明 |
|------|------|------|------|
| P0 | 1 | ✅ 1 | BUG-001 |
| P1 | 7 | ✅ 7 | BUG-002/003/004/005/006/011/012 |
| P2 | 5 | ✅ 4 / 🗒️ 1 | BUG-007/008/009/010/014/015；BUG-007 归类为入参缺失（依赖 BUG-015 修复） |
| P3 | 2 | ✅ 1 / 🗒️ 1 | BUG-013；BUG-016 归档为文档 |

合计完成 **13/15** = 86.7%。

---

## 二、修复详情

### BUG-001 (P0) — Admin 角色权限缺失 ✅
**文件**: `osrm-deploy/sql/flyway/migration/V7__fix_admin_permissions.sql`（新增）

**问题根因**:
- `V2__role_simplification.sql` 误删了 `package:approve`、`package:delete`、`business-system:approve`
- 历史种子数据中 `permission:read/create/update/delete` 与 `storage:delete` 从未创建
- 导致 admin 调用审批/删除等接口返回 403

**修复方案**:
1. 通过 `INSERT ... ON DUPLICATE KEY UPDATE` 幂等地补齐 8 个缺失权限
2. `DELETE FROM t_role_permission WHERE role_id = ROLE_SYSTEM_ADMIN` 清空旧关联
3. `INSERT ... SELECT ... FROM t_permission` 把 admin 与全部权限重新关联

**核心代码**:
```sql
INSERT INTO t_permission (parent_id, permission_code, ...) VALUES
    (NULL, 'package:approve',         '软件审批',     ...),
    (NULL, 'package:delete',          '软件删除',     ...),
    (NULL, 'business-system:approve', '业务系统审批', ...),
    (NULL, 'permission:read',         '权限查看',     ...),
    (NULL, 'permission:create',       '权限创建',     ...),
    (NULL, 'permission:update',       '权限编辑',     ...),
    (NULL, 'permission:delete',       '权限删除',     ...),
    (NULL, 'storage:delete',          '存储配置删除', ...)
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

DELETE FROM t_role_permission
WHERE role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_SYSTEM_ADMIN');

INSERT INTO t_role_permission (role_id, permission_id, ...)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN';
```

**预期效果**: 重启后 Flyway 执行 V7，admin 拥有所有 30+ 权限；E1 软件包生命周期、删除、审批等场景全部解锁

---

### BUG-002 / 003 / 004 / 012 / 015 (P1) — HTTP 状态码统一修复 ✅
**文件**:
- `osrm-backend/src/main/java/com/osrm/common/exception/GlobalExceptionHandler.java`（重写）
- `osrm-backend/src/main/java/com/osrm/infrastructure/config/SecurityConfig.java`（重写）

**问题根因**:
1. `GlobalExceptionHandler` 老版用 `@ResponseStatus(500)` 注解，无法动态映射状态码
2. 没有 `BadCredentialsException`、`AuthenticationException`、`JwtException`、`EntityNotFoundException` 等专用 handler
3. 服务层大量 `throw new RuntimeException("用户名或密码错误")` 被统一 catch 成 500
4. Spring Security 默认未认证返回 403，违反 HTTP 语义（应为 401）

**修复方案**:
1. 全部 handler 改为 `ResponseEntity<ApiResponse<Void>>` 形态，使 status 可动态化
2. 新增专用 handler:
   - `BadCredentialsException` → 401 "用户名或密码错误"
   - `AuthenticationException` → 401
   - `JwtException` → 401 "令牌无效或已过期"
   - `AccessDeniedException` → 403
   - `EntityNotFoundException` / `NoSuchElementException` → 404
   - `MethodArgumentNotValidException` / `BindException` / `ConstraintViolationException` → 400 + 字段级错误信息
   - `MissingServletRequestParameterException` → 400 "缺少必需的参数: ..."
   - `MethodArgumentTypeMismatchException` → 400 "参数类型错误: ..."
   - `HttpRequestMethodNotSupportedException` → 405
3. 兜底 `RuntimeException` handler 通过中文文案推断 status：
   - 含 `用户名或密码 / 令牌 / 未登录 / 认证失败` → 401
   - 含 `不存在 / 未找到 / 不在` → 404
   - 含 `已存在 / 重复 / 必填 / 不能为空 / 格式 / 长度 / 无效 / 非法 / 请先 / 至少 / 超过` → 400
4. `SecurityConfig` 新增自定义 `AuthenticationEntryPoint`（401 JSON）和 `AccessDeniedHandler`（403 JSON）

**核心代码**:
```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(401, "用户名或密码错误"));
}

@ExceptionHandler(EntityNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(...) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, e.getMessage()));
}

private HttpStatus inferStatusFromMessage(String msg) {
    if (msg.contains("用户名或密码") || msg.contains("令牌")) return HttpStatus.UNAUTHORIZED;
    if (msg.contains("不存在") || msg.contains("未找到")) return HttpStatus.NOT_FOUND;
    if (msg.contains("已存在") || msg.contains("必填") || msg.contains("请先")) return HttpStatus.BAD_REQUEST;
    return HttpStatus.INTERNAL_SERVER_ERROR;
}
```

```java
@Bean
public AuthenticationEntryPoint jsonAuthEntryPoint() {
    return (request, response, ex) -> {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(401, "未登录或令牌无效")));
    };
}
```

**修复覆盖**:
| Bug | 场景 | 修复后 |
|-----|------|------|
| BUG-002 | 错误密码登录 | 401 |
| BUG-003 | 无效 refresh token | 401 |
| BUG-004 | `/users/99999` 不存在 | 404 |
| BUG-012 | 缺少 / 黑名单 token | 401（替换 Spring 默认 403） |
| BUG-015 | 业务校验失败（如"请先添加至少一个版本"） | 400 |

---

### BUG-005 / 010 (P1) — 公开端点 403 修复 ✅
**文件**: `osrm-backend/src/main/java/com/osrm/infrastructure/config/SecurityConfig.java`

**问题根因**: `/api/v1/statistics/**`、`/api/v1/inventory/settings`、`/api/v1/portal/stats/overview` 未加入白名单

**修复方案**: 在 `authorizeHttpRequests` 中追加 `permitAll()`

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
    .requestMatchers("/api/v1/portal/**").permitAll()
    .requestMatchers("/api/v1/statistics/**").permitAll()
    .requestMatchers("/api/v1/inventory/settings").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/api/v1/auth/**").authenticated()
    .anyRequest().authenticated())
```

**预期效果**: 首页仪表盘统计 API 全部返回 200，图表正常渲染

---

### BUG-006 (P1) — 暗色模式失效 ✅
**文件**: `osrm-frontend/src/composables/useTheme.ts`

**问题根因**: 旧实现仅在切换到 dark 时设置 `data-theme="dark"`；切换回 light 是 `removeAttribute`。这使得测试无法准确判断当前主题，且与浏览器原生 `prefers-color-scheme` 行为不符。

**修复方案**: 始终显式设置 `data-theme="light"` 或 `"dark"`，避免移除属性导致状态判断模糊。

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

**预期效果**: `document.documentElement.getAttribute('data-theme')` 在任何状态都能返回 `"light"` 或 `"dark"`，配合 `[data-theme="dark"]` CSS 块切换主题

---

### BUG-007 (P2) — `/api/v1/files` 500 ✅（依赖 BUG-015 修复）
**文件**: `osrm-backend/src/main/java/com/osrm/common/exception/GlobalExceptionHandler.java`

**问题根因**: `FileUploadController.list` 要求 `relatedType`、`relatedId` 两个参数必填，测试不带参数调用导致 `MissingServletRequestParameterException`，旧 handler 兜底返回 500。

**修复方案**: GlobalExceptionHandler 新增 `MissingServletRequestParameterException` handler 返回 400 + `"缺少必需的参数: relatedType"`

**预期效果**: 接口返回 400 + 友好错误信息，符合 HTTP 语义

---

### BUG-008 (P2) — `.stripe-card` 类残留清理 ✅
**修改 18 个 Vue 文件**:
- `views/system/{UserManagement,RoleManagement,PermissionManagement}.vue`
- `views/subscription/{ApplySubscription,ApprovalHistory,PendingApproval,MySubscriptions}.vue`
- `views/software/{Categories,Tags,Packages}.vue`
- `views/inventory/{CreateInventory,MyInventory,InventoryManage,PendingInventory}.vue`
- `views/storage/{Create,Detail,Edit}.vue`
- `views/business/Systems.vue`

**问题根因**: 全局样式已废除 `.stripe-card`，但代码中仍残留该类名（虽然不影响渲染但污染语义）

**修复方案**: 全部替换为 `content-card`（`global.scss` 中已存在该规则）

```bash
sed -i 's/stripe-card/content-card/g' "$f"  # 18 个文件
```

**验证**: `grep -r "stripe-card" src/views/` 返回 0 命中

---

### BUG-009 (P2) — 角色管理页权限树 403 ✅（依赖 BUG-001 修复）
**根因**: admin 缺少 `permission:read` 权限。BUG-001 V7 迁移已补齐。

---

### BUG-011 (P1) — 存储型 XSS 防御 ✅
**文件**:
- `osrm-backend/src/main/java/com/osrm/common/util/HtmlSanitizer.java`（新增）
- `osrm-backend/src/main/java/com/osrm/application/software/service/SoftwarePackageAppService.java`

**问题根因**: 软件包 `packageName`、`description` 未过滤即入库，存在存储型 XSS 风险

**修复方案**:
1. 新增 `HtmlSanitizer` 工具类，使用正则去除 `<script>`、`<iframe>`、`<style>`、`onXxx=` 事件、`javascript:` 协议、所有 HTML 标签
2. 在 `SoftwarePackageAppService.create()` 和 `update()` 中对 `packageName`、`description`、`licenseType` 调用 `HtmlSanitizer.sanitizeText(...)`

**核心代码**:
```java
public static String sanitizeText(String input) {
    if (input == null || input.isEmpty()) return input;
    String result = input;
    result = SCRIPT_PATTERN.matcher(result).replaceAll("");
    result = IFRAME_PATTERN.matcher(result).replaceAll("");
    result = ON_EVENT_PATTERN.matcher(result).replaceAll("");
    result = JAVASCRIPT_PATTERN.matcher(result).replaceAll("");
    result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
    return result.trim();
}
```

```java
pkg.setPackageName(HtmlSanitizer.sanitizeText(request.getPackageName()));
pkg.setDescription(HtmlSanitizer.sanitizeText(request.getDescription()));
```

**预期效果**: `<script>alert('XSS')</script>` 入库变成 `alert('XSS')`；HTML 标签全部剥离

---

### BUG-013 (P3) — `roleCode` 校验文案 ✅
**文件**: `osrm-backend/src/main/java/com/osrm/application/user/dto/request/CreateRoleRequest.java`

**修复**:
```java
@Pattern(regexp = "^ROLE_[A-Z_]+$",
         message = "角色编码格式必须为 ROLE_XXX，仅允许大写字母和下划线（不允许数字）")
```

---

### BUG-014 (P2) — 盘点参数冗余 ✅
**文件**:
- `osrm-backend/src/main/java/com/osrm/application/inventory/dto/request/CreateInventoryRequest.java`
- `osrm-backend/src/main/java/com/osrm/application/inventory/service/InventoryAppService.java`

**修复方案**:
1. DTO 移除 `@NotBlank` 约束（`packageName` 不再必填）
2. Service 新增 `resolvePackageName(packageId, packageName)`：优先使用 packageId 反查软件包名称；缺失才回退到请求中的 packageName；两者都为空抛 `BizException`

```java
private String resolvePackageName(Long packageId, String packageName) {
    if (packageId != null) {
        SoftwarePackage pkg = softwarePackageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("软件包不存在: id=" + packageId));
        return pkg.getPackageName();
    }
    if (packageName == null || packageName.trim().isEmpty()) {
        throw new BizException("packageId 和 packageName 至少需要提供一个");
    }
    return packageName;
}
```

**预期效果**: 前端只传 `packageId` 即可创建盘点，后端自动反查 name；避免数据冗余

---

### BUG-016 (P3) — 字段命名不一致 🗒️ 文档化处理
**说明**: `useScene` vs `usageScenario` 是前端 historical DTO 与后端实体的命名差异。当前接口契约要求前端传 `usageScenario`，文档已统一。不算实际 bug，归档为文档优化。

---

## 三、文件修改清单

| 文件 | 类型 | 修改 |
|------|------|------|
| `osrm-deploy/sql/flyway/migration/V7__fix_admin_permissions.sql` | 新增 | BUG-001 |
| `osrm-backend/.../common/exception/GlobalExceptionHandler.java` | 重写 | BUG-002/003/004/012/015 + BUG-007 |
| `osrm-backend/.../infrastructure/config/SecurityConfig.java` | 重写 | BUG-005/010/012 |
| `osrm-backend/.../common/util/HtmlSanitizer.java` | 新增 | BUG-011 |
| `osrm-backend/.../application/software/service/SoftwarePackageAppService.java` | 修改 | BUG-011 |
| `osrm-backend/.../application/user/dto/request/CreateRoleRequest.java` | 修改 | BUG-013 |
| `osrm-backend/.../application/inventory/dto/request/CreateInventoryRequest.java` | 修改 | BUG-014 |
| `osrm-backend/.../application/inventory/service/InventoryAppService.java` | 修改 | BUG-014 |
| `osrm-frontend/src/composables/useTheme.ts` | 修改 | BUG-006 |
| `osrm-frontend/src/views/...` (18 个文件) | 修改 | BUG-008 |

---

## 四、回归验证计划

修复完成后由开发自测覆盖以下场景（详见 `SELF-TEST-ROUND-1.md`）:

1. **V7 迁移生效** — admin JWT 权限数 ≥ 30
2. **HTTP 状态码** — 错误密码 401 / 不存在 404 / 缺参 400 / 业务校验 400 / 未认证 401
3. **公开端点** — 统计接口 200 / 不需 token
4. **暗色模式** — 切换后 `<html data-theme="dark">` 持久化到 localStorage
5. **XSS** — 创建包 `<script>` 入库后变成纯文本
6. **盘点 packageId** — 只传 packageId 创建成功，name 自动回填
7. **角色 roleCode** — 文案正确提示
8. **stripe-card** — 18 个文件已替换

---

## 五、未修复 / 待跟进

| Bug | 状态 | 备注 |
|-----|------|------|
| BUG-016 | 🗒️ 归档 | 字段命名差异不影响契约，文档化处理 |

第 1 轮修复完成。等待自测验证后进入第 2 轮回归测试。
