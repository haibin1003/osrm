# 测试规范

本规范定义 OSRM 项目的测试策略，重点覆盖后端 REST 接口集成测试和前端 E2E 场景测试要求。

---

## 文档信息

- **作者**: OSRM 开发团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-20
- **维护责任人**: 技术负责人

---

## 测试策略概述

| 测试类型 | 适用阶段 | 工具 | 覆盖率要求 |
|----------|----------|------|------------|
| REST 接口集成测试 | Phase 4（后端编码完成后） | Spring Boot Test + MockMvc | 接口层 ≥90% |
| E2E 场景测试 | Phase 6（前后端联调） | Playwright | 覆盖所有用户场景 |

---

## REST 接口集成测试（Phase 4）

### 技术栈

- **框架**：Spring Boot `@SpringBootTest` + MockMvc
- **数据清理**：`@Transactional` 回滚（优先）或手动清理
- **执行方式**：`mvn test`

### 必须覆盖的场景

每个 Controller 方法必须至少覆盖以下场景：

| 场景类型 | 说明 | 期望状态码 |
|----------|------|------------|
| 正常路径 | 合法输入，操作成功 | 200 / 201 |
| 参数缺失/非法 | 必填字段为空、格式错误 | 400 |
| 未登录访问 | 无 Token 或 Token 失效 | 401 |
| 权限不足 | 角色无此操作权限 | 403 |
| 资源不存在 | 查询/操作不存在的数据 | 404 |
| 业务规则违反 | 重复创建、状态流转错误等 | 409 / 422 |

### 测试编写要求

1. 测试类和方法顶部标注关联需求编号：`// 关联需求: REQ-XXX`
2. 测试方法命名清晰，描述被测场景：`should{Result}_When{Condition}()`
3. 使用 `@Transactional` 确保测试数据自动回滚
4. 测试间无顺序依赖，可独立运行

### 示例

```java
// 关联需求: REQ-001
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ✅ 正常路径
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreatePackage_WhenInputIsValid() throws Exception {
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test-pkg\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test-pkg"))
            .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // ✅ 未登录返回 401
    @Test
    void shouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test-pkg\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ✅ 权限不足返回 403
    @Test
    @WithMockUser(roles = "GUEST")
    void shouldReturn403_WhenUserLacksPermission() throws Exception {
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test-pkg\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isForbidden());
    }

    // ✅ 参数非法返回 400
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400_WhenNameIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isBadRequest());
    }

    // ✅ 资源不存在返回 404
    @Test
    @WithMockUser(roles = "DEVELOPER")
    void shouldReturn404_WhenPackageNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/packages/99999"))
            .andExpect(status().isNotFound());
    }
}
```

### 覆盖率要求

| 层级 | 要求 |
|------|------|
| 接口层（Controller） | ≥90%（每个方法至少一个测试） |
| 业务层（Service）核心逻辑 | ≥80% |

**覆盖率报告生成**：
```bash
./mvnw test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

---

## E2E 场景测试（Phase 6）

### 技术栈

- **框架**：Playwright（TypeScript）
- **执行方式**：`npx playwright test`

### 必须覆盖的场景

- ✅ 各角色登录/登出流程
- ✅ 每个角色的核心业务场景（按需求验收标准）
- ✅ 无权限操作被正确拦截（前端不显示入口，后端返回 403）
- ✅ 关键表单的异常输入提示
- ✅ 页面无 JS 报错，接口无 500 错误

### 测试编写要求

1. 顶部标注关联需求编号：`// 关联需求: REQ-XXX`
2. 使用 `loginAs(page, role)` 工具函数切换角色
3. 关键步骤截图保存至 `e2e/screenshots/`
4. 测试描述清晰，体现用户视角

### 示例

```typescript
// e2e/package-management.spec.ts
// 关联需求: REQ-001, REQ-002
import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

test.describe('软件包管理', () => {

  test('软件管理员应能成功录入软件包', async ({ page }) => {
    await loginAs(page, 'software-admin');
    await page.goto('/packages/new');
    await page.fill('[name="name"]', 'test-package');
    await page.fill('[name="version"]', '1.0.0');
    await page.click('button[type="submit"]');
    await expect(page.locator('.success-message')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/package-create-success.png' });
  });

  test('访客不应看到录入软件包按钮', async ({ page }) => {
    await loginAs(page, 'guest');
    await page.goto('/packages');
    await expect(page.locator('button:has-text("录入软件包")')).not.toBeVisible();
  });

  test('开发人员应能查看软件包列表', async ({ page }) => {
    await loginAs(page, 'developer');
    await page.goto('/packages');
    await expect(page.locator('.package-list')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/package-list-developer.png' });
  });
});
```

---

## 测试命名规范

### 文件命名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 接口集成测试 | `{Controller}Test.java` | `PackageControllerTest.java` |
| E2E 场景测试 | `{feature}.spec.ts` | `package-management.spec.ts` |

### 方法命名

**Java**：
```java
void should{ExpectedResult}_When{Condition}()

// 示例
void shouldCreatePackage_WhenInputIsValid()
void shouldReturn403_WhenUserLacksPermission()
```

**TypeScript**：
```typescript
'should {expected result} when {condition}'

// 示例
'should show package list when user is developer'
'should hide create button when user is guest'
```

---

## 测试数据管理

### 数据隔离

- 接口集成测试：使用 `@Transactional` 回滚，测试数据不污染数据库
- E2E 测试：使用独立测试环境或每次测试前重置数据

### 测试数据工厂

```java
public class PackageTestFactory {

    public static CreatePackageRequest createValidRequest() {
        return new CreatePackageRequest("test-package", "1.0.0");
    }

    public static CreatePackageRequest createRequestWithEmptyName() {
        return new CreatePackageRequest("", "1.0.0");
    }
}
```

---

## 测试环境配置

### 后端 (Spring Boot)

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

### 前端 E2E (Playwright)

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  outputDir: './e2e/screenshots',
  use: {
    baseURL: 'http://localhost:5173',
    screenshot: 'only-on-failure',
  },
  reporter: [['html', { outputFolder: 'e2e/report' }]],
});
```

---

## 变更记录

| 时间 | 变更内容 | 变更原因 | 影响范围 | 处理人 | 状态 |
|------|----------|----------|----------|--------|------|
| 2026-03-17 | 初始版本（TDD规范） | 建立测试规范 | 全部 | 技术负责人 | 已完成 |
| 2026-03-20 | 改为接口集成测试规范 | 去除TDD强制先写测试要求，改为后端编码后再写接口测试 | 全部 | 技术负责人 | 已完成 |
