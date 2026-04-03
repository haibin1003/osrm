# REQ-300 软件包管理模块开发总结

**功能ID**: REQ-300
**完成日期**: 2026-03-21
**开发阶段**: Phase 1–7（完整）

---

## 一、实现清单

### 后端实现

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| 软件类型枚举 | `domain/software/entity/SoftwareType.java` | 6种类型：DOCKER_IMAGE/HELM_CHART/MAVEN/NPM/PYPI/GENERIC，含存储后端映射 |
| 包状态枚举 | `domain/software/entity/PackageStatus.java` | 5态：DRAFT/PENDING/PUBLISHED/OFFLINE/ARCHIVED，含状态转换方法 |
| 版本状态枚举 | `domain/software/entity/VersionStatus.java` | 3态：DRAFT/PUBLISHED/OFFLINE |
| 软件包实体 | `domain/software/entity/SoftwarePackage.java` | 含乐观锁、状态机方法、packageKey唯一标识 |
| 版本实体 | `domain/software/entity/SoftwareVersion.java` | 含 versionNo、storageBackendId、storagePath、isLatest |
| 包仓库 | `domain/software/repository/SoftwarePackageRepository.java` | findByConditions、countBy系列方法 |
| 版本仓库 | `domain/software/repository/SoftwareVersionRepository.java` | findBySoftwarePackageId系列方法 |
| 应用服务 | `application/software/service/SoftwarePackageAppService.java` | CRUD + 状态流转 + 版本管理 |
| 控制器 | `interfaces/rest/SoftwarePackageController.java` | 18个REST端点 |
| 数据库迁移 | `resources/db/migration/V3__create_software_tables.sql` | t_software_package + t_software_version |

### 状态机

```
DRAFT → PENDING → PUBLISHED → OFFLINE → PUBLISHED（重新上架）
  ↑        ↓
  └─── REJECTED
```

- DRAFT：可编辑、可添加版本、可删除
- PENDING：待审批，不可编辑
- PUBLISHED：已上架，可下架
- OFFLINE：已下架，可重新上架

### REST API（18个端点）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | /api/v1/software-packages | package:read |
| POST | /api/v1/software-packages | package:create |
| GET | /api/v1/software-packages/{id} | package:read |
| PUT | /api/v1/software-packages/{id} | package:update |
| DELETE | /api/v1/software-packages/{id} | package:delete |
| POST | /api/v1/software-packages/{id}/submit | package:update |
| POST | /api/v1/software-packages/{id}/approve | package:approve |
| POST | /api/v1/software-packages/{id}/reject | package:approve |
| POST | /api/v1/software-packages/{id}/offline | package:approve |
| POST | /api/v1/software-packages/{id}/republish | package:approve |
| GET | /api/v1/software-packages/types | package:read |
| GET | /api/v1/software-packages/{id}/versions | package:read |
| POST | /api/v1/software-packages/{id}/versions | package:create |
| POST | /api/v1/software-packages/{id}/versions/{vid}/publish | package:update |
| POST | /api/v1/software-packages/{id}/versions/{vid}/offline | package:update |
| DELETE | /api/v1/software-packages/{id}/versions/{vid} | package:delete |

### 前端实现

| 组件 | 文件路径 | 说明 |
|------|----------|------|
| 类型定义 | `src/types/software.ts` | SoftwarePackage/SoftwareVersion/PackageForm/VersionForm |
| API层 | `src/api/software.ts` | 完整REST调用封装 |
| 软件包列表页 | `src/views/software/Packages.vue` | 搜索、分页、状态流转操作、版本管理内嵌弹窗 |
| 门户详情页 | `src/views/portal/SoftwareDetail.vue` | 修复字段名与状态值 |
| 首页 | `src/views/home/Index.vue` | 修复displayName→packageName |
| 订购申请 | `src/views/subscription/ApplySubscription.vue` | 修复版本显示字段 |

---

## 二、测试覆盖

| 测试类型 | 测试文件 | 用例数 |
|---------|---------|--------|
| 单元测试 | SoftwarePackageAppServiceTest | 22个 |
| 集成测试 | SoftwarePackageControllerIntegrationTest | 9个 |
| 总计 | | 31个 |

测试场景覆盖：
- CRUD操作正常/异常路径
- 完整状态机流转（DRAFT→PENDING→PUBLISHED→OFFLINE→PUBLISHED）
- 审批拒绝流程
- 版本管理（添加、获取）
- 权限控制（无Token返回4xx）
- 重复包名/包标识验证

---

## 三、关键设计决策

1. **packageKey**：独立的英文唯一标识字段，与packageName分离，支持中文包名
2. **乐观锁**：使用`@Version`防止并发状态转换冲突
3. **IllegalStateException → BizException**：服务层统一捕获域对象抛出的非法状态异常并转换
4. **isLatest标记**：版本发布时自动更新最新版本标记，保持唯一性
5. **存储后端关联**：版本创建时必须指定storageBackendId，明确制品存储位置

---

## 四、遗留问题

1. 前端版本管理弹窗中的"版本发布/下线"操作尚未实现（按钮未添加）
2. 制品上传功能依赖文件系统路径，生产环境需要对接真实存储后端
3. packageKey自动生成（从packageName转换）的功能可作为UX优化添加
4. 软件包编辑（PUT接口）对应的前端编辑弹窗未实现
