# 开发流程规范

本规范定义 OSRM 项目的七阶段开发流程，确保代码实现与需求高度一致，所有功能经过充分测试。

---

## 文档信息

- **作者**: OSRM 开发团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-20
- **维护责任人**: 技术负责人

---

## 七阶段开发流程

所有功能开发必须遵循以下七阶段流程：

| 阶段 | 任务 | 输出物 | 检查点 |
|------|------|--------|--------|
| **Phase 1: 需求设计** | 编写/阅读功能需求文档 | `/docs/requirements/features/{feature-id}.md` | 功能描述、业务规则、验收标准 |
| **Phase 2: 技术设计** | API设计 + 数据库设计 + 架构设计 | `/docs/design/api/`, `/docs/design/database/` | 接口定义完整，数据模型合理 |
| **Phase 3: 设计评审** | 对 Phase 2 设计进行评审与优化 | 评审意见 + 更新后的设计文档 | 设计无明显问题，可指导编码 |
| **Phase 4: 后端编码 + 接口测试** | 实现后端代码，完成后写 REST 接口集成测试 | 后端代码 + 测试全部通过 | 接口层覆盖 ≥90%，所有场景覆盖 |
| **Phase 5: 前端 UI 设计 + 编码** | 先出专业 UI 设计稿，确认后开发 | UI 设计方案 + 前端代码 | 设计经确认，角色权限控制正确 |
| **Phase 6: 前后端联调 + 场景测试** | 联调所有接口，Playwright 走完所有场景 | 联调通过 + 测试截图/报告 | 所有页面可用无报错 |
| **Phase 7: 功能总结** | 编写功能总结文档 | `/docs/summaries/{feature-id}-summary.md` | 实现清单、遗留问题 |

---

## 复杂功能的任务拆分

对于较复杂的功能，在进入七阶段流程之前，应先将其拆分为细粒度的子任务，逐步推进开发。

### 何时需要拆分

满足以下任一条件时，应进行任务拆分：
- 功能涉及 3 张以上数据库表
- 包含多个独立子流程（如录入、审核、发布是三个独立流程）
- 前后端改动文件预计超过 10 个
- 开发周期预计超过 2 天

### 拆分方式

**按业务子流程拆分**（推荐）：
```
软件包管理（复杂功能）
├── Task 1: 软件包录入（含 CRUD）
├── Task 2: 审核流程（提交审核 → 审核通过/驳回）
├── Task 3: 发布流程（审核通过 → 发布到存储后端）
└── Task 4: 订购与使用（开发人员侧）
```

**按层次拆分**（后端先行）：
```
用户管理（复杂功能）
├── Task 1: 后端接口 + 数据库（CRUD + 权限）
└── Task 2: 前端页面（列表 + 表单 + 角色权限控制）
```

### 拆分后的执行方式

每个子任务独立走完七阶段流程（或适用的阶段），子任务之间可并行或串行执行：

```
Task 1 → Phase 1~7 ✓
Task 2 → Phase 1~7 ✓（可复用 Task 1 的技术设计）
Task 3 → Phase 1~7 ✓
```

使用 Claude Code 的 **TaskCreate / TaskUpdate / TaskList** 工具管理子任务进度，在会话中保持任务可见性。

---

## Phase 1: 需求设计

### 目标
明确功能需求，确保开发团队对功能有一致理解。

### 输入
- 产品需求文档 (`plans/osrm-requirements.md`)
- 用户反馈
- 业务方需求

### 输出物
`/docs/requirements/features/{feature-id}.md`

文档必须包含：
1. **功能描述** - 清晰描述功能是什么
2. **业务规则** - 功能涉及的业务逻辑和约束
3. **验收标准** - 功能完成的判断标准（可测试）
4. **用户故事** - 从用户角度描述功能价值
5. **界面原型**（如有）- UI/UX 设计稿或草图
6. **变更记录** - 需求变更历史

### 检查点
- [ ] 功能描述清晰完整
- [ ] 业务规则无歧义
- [ ] 验收标准可测试
- [ ] 需求编号唯一（如 REQ-001）

---

## Phase 2: 技术设计

### 目标
设计技术实现方案，识别技术风险。

### 输入
- 功能需求文档
- 现有系统架构
- 技术栈约束

### 输出物

#### 1. 架构设计文档
`/docs/design/architecture/{feature-id}-architecture.md`
- 组件图
- 数据流图
- 与现有系统的集成点
- 技术选型说明

#### 2. API 设计文档
`/docs/design/api/{feature-id}-api.md`
- RESTful API 定义
- 请求/响应示例
- 错误处理

#### 3. 数据库设计文档（如需要）
`/docs/design/database/{feature-id}-db.md`
- 实体关系图
- 表结构设计
- 索引设计
- 迁移脚本

### 检查点
- [ ] 接口定义完整，含请求/响应/错误码
- [ ] 数据模型合理，字段命名规范
- [ ] 技术风险已识别并记录
- [ ] 性能和安全考量已评估

---

## Phase 3: 设计评审

### 目标
对 Phase 2 输出的设计文档进行评审，发现问题并优化，确保设计可指导编码。

### 流程
1. 使用 **architect** 代理对设计文档进行评审
2. 检查 API 设计是否符合 RESTful 规范（见 `api-design.md`）
3. 检查数据库设计是否符合规范（见 `database.md`）
4. 识别设计缺陷、安全隐患、性能风险
5. 根据评审意见更新设计文档

### 评审清单
- [ ] API 路径、方法、状态码符合规范
- [ ] 请求/响应结构合理，字段完整
- [ ] 数据库表结构规范，索引合理
- [ ] 权限控制设计覆盖所有角色
- [ ] 异常场景已在设计中考虑
- [ ] 设计文档已根据评审意见更新

### 输出物
- 评审意见（可记录在设计文档的 `## 评审记录` 节）
- 更新后的设计文档

---

## Phase 4: 后端编码 + 接口测试

### 目标
按照设计实现后端功能，完成后编写 REST 接口集成测试验证正确性。

### 编码要求
- 代码注释标注关联的需求编号：`// 关联需求: REQ-XXX`
- 遵循 `api-design.md`、`database.md` 规范
- 实现所有设计文档中定义的接口

### 接口测试要求

使用 Spring Boot `@SpringBootTest` + MockMvc 对 REST 接口进行集成测试。

**必须覆盖：**
- ✅ 正常场景（成功路径）
- ✅ 异常场景（参数缺失、数据不存在、业务规则违反等）
- ✅ 权限/角色控制（无权限访问返回 403，未登录返回 401）
- ✅ 每个测试用例执行后清理测试数据（用 `@Transactional` 回滚或手动清理）
- ✅ 所有测试可通过 `mvn test` 批量执行，无顺序依赖

**覆盖率要求：** 接口层覆盖 ≥90%（每个 Controller 方法至少一个测试）

**示例**：
```java
// 关联需求: REQ-001
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PackageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreatePackage_WhenInputIsValid() throws Exception {
        mockMvc.perform(post("/api/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test"));
    }

    @Test
    void shouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_WhenUserHasNoPermission() throws Exception {
        // 以访客身份请求
        mockMvc.perform(post("/api/packages")
                .with(user("guest").roles("GUEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400_WhenNameIsEmpty() throws Exception {
        mockMvc.perform(post("/api/packages")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"version\":\"1.0.0\"}"))
            .andExpect(status().isBadRequest());
    }
}
```

### 检查点
- [ ] 所有接口已实现
- [ ] 接口层测试覆盖 ≥90%
- [ ] 正常/异常/权限场景全覆盖
- [ ] `mvn test` 全部通过，无顺序依赖

---

## Phase 5: 前端 UI 设计 + 编码

### 目标
先产出专业 UI 设计方案，确认后再开发前端代码，避免通用模板风格。

### UI 设计先行

1. 使用 `/frontend-design` 技能为每个页面生成专业设计方案
2. 设计要考虑不同角色看到的菜单和内容差异：
   - **访客**：只读，无操作入口
   - **开发人员**：订购、查看，无审核/录入
   - **软件管理员**：录入、审核、发布
   - **系统管理员**：全部功能 + 系统配置
3. 避免通用模板风格，要有视觉特点和设计感
4. 设计经确认后再开始编码

### 开发要求

- 菜单和功能按钮须根据当前用户角色动态显示/隐藏
- 无权限操作不显示入口（非仅后端拦截）
- 遵循 `frontend.md` 规范（Vue 3 + Composition API + Pinia）

**角色权限控制示例**：
```vue
<template>
  <!-- 仅软件管理员和系统管理员可见 -->
  <el-button
    v-if="hasPermission('package:create')"
    @click="handleCreate"
  >
    录入软件包
  </el-button>
</template>
```

### 检查点
- [ ] UI 设计方案已产出并经确认
- [ ] 菜单和按钮根据角色动态显示
- [ ] 无权限入口不可见（前端控制）
- [ ] 页面风格有设计感，非通用模板

---

## Phase 6: 前后端联调 + 场景测试

### 目标
验证前后端集成正确性，通过 Playwright 场景测试覆盖所有用户流程。

### 联调步骤
1. 启动前后端服务
2. 逐一验证前端页面的接口调用是否正常
3. 验证各角色登录后的菜单和功能是否符合权限设计
4. 修复联调发现的问题

### Playwright 场景测试

**必须覆盖：**
- ✅ 各角色登录/登出流程
- ✅ 每个角色的核心业务场景（按需求验收标准）
- ✅ 无权限操作被正确拦截（前端不显示，后端返回 403）
- ✅ 关键表单的异常输入处理
- ✅ 页面无 JS 报错，接口无 500 错误

**示例**：
```typescript
// e2e/package-management.spec.ts
// 关联需求: REQ-001
test.describe('软件包录入（软件管理员）', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'software-admin');
  });

  test('应能成功录入新软件包', async ({ page }) => {
    await page.goto('/packages/new');
    await page.fill('[name="name"]', 'test-package');
    await page.fill('[name="version"]', '1.0.0');
    await page.click('button[type="submit"]');
    await expect(page.locator('.success-message')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/package-create-success.png' });
  });

  test('访客不应看到录入入口', async ({ page }) => {
    await loginAs(page, 'guest');
    await page.goto('/packages');
    await expect(page.locator('button:has-text("录入软件包")')).not.toBeVisible();
  });
});
```

**输出物**：
- 联调通过记录
- Playwright 测试报告 + 截图

### 测试完成后的产物清理

**每次 Phase 6 验证完毕后，必须清理以下临时产物：**

```bash
# Playwright 产物
rm -rf e2e/screenshots/
rm -rf e2e/report/
rm -rf playwright-report/
rm -rf test-results/

# 前端构建产物
rm -rf frontend/dist/
rm -rf frontend/node_modules/.cache/

# 后端编译产物
./mvnw clean   # 清理 target/ 目录
```

**清理原则：**
- 截图、视频、HTML 报告等测试临时文件不提交到代码仓库
- 编译产物（`target/`、`dist/`）不提交，避免 AI 在下次开发时扫描大量无关文件
- `.gitignore` 中需包含以下条目，防止误提交：

```gitignore
# 测试产物
e2e/screenshots/
e2e/report/
playwright-report/
test-results/

# 编译产物
target/
frontend/dist/
```

### 检查点
- [ ] 所有前端页面接口联调通过
- [ ] 各角色权限场景测试通过
- [ ] Playwright 所有场景测试通过
- [ ] 页面无 JS 报错，接口无 500 错误
- [ ] 测试截图/报告等临时产物已清理
- [ ] 编译产物（target/、dist/）已清理

---

## Phase 7: 功能总结

### 目标
记录功能实现情况，为后续维护提供参考。

### 输出物
`/docs/summaries/{feature-id}-summary.md`

文档必须包含：
1. **功能概述** - 简要描述实现的功能
2. **实现清单** - 已实现的功能点
3. **技术要点** - 关键技术决策
4. **遗留问题** - 已知问题和待办事项
5. **变更记录** - 开发过程中的需求变更

### 检查点
- [ ] 实现清单与需求一致
- [ ] 遗留问题已记录并创建跟踪任务

---

## 需求与代码一致性

### 追溯关系

```
需求文档 (REQ-XXX)
    ↓
设计文档 (引用 REQ-XXX)
    ↓
接口测试代码 (// 关联需求: REQ-XXX)
    ↓
实现代码 (// 关联需求: REQ-XXX)
    ↓
E2E 场景测试 (// 关联需求: REQ-XXX)
    ↓
功能总结 (引用 REQ-XXX)
```

### 一致性检查机制

| 不一致场景 | 处理流程 |
|-----------|----------|
| 需求变更导致代码不一致 | 创建变更任务，按流程更新代码和测试 |
| 编码实现与需求不符 | 回退修改代码，或发起需求变更评审 |
| 测试覆盖与需求不符 | 补充测试用例，确保覆盖需求场景 |
| 设计文档与实现不符 | 以实际实现为准更新设计文档，或重构代码 |

---

## 变更记录

| 时间 | 变更内容 | 变更原因 | 影响范围 | 处理人 | 状态 |
|------|----------|----------|----------|--------|------|
| 2026-03-17 | 初始版本（五阶段流程） | 建立开发规范 | 全部 | 技术负责人 | 已完成 |
| 2026-03-20 | 升级为七阶段流程 | 优化开发体验，增加设计评审、UI设计、联调测试环节 | 全部 | 技术负责人 | 已完成 |
