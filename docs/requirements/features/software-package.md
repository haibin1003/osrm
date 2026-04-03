# 功能需求文档：软件包管理

**功能编号**: FEAT-003
**需求编号**: REQ-300 ~ REQ-399
**优先级**: P0 (MVP)
**创建日期**: 2026-03-19
**状态**: 需求设计阶段

---

## 1. 功能概述

### 1.1 背景
开源软件包是 OSRM 平台的核心资源。存储后端管理完成后，需要将实际的软件包纳入管理，实现从"录入→审核→发布→订购→使用"的完整闭环。

### 1.2 目标
- 支持软件包基础信息的录入和维护
- 实现软件包版本的全生命周期管理
- 建立软件包审核发布流程
- 对接存储后端（Harbor/Nexus/NAS）实现制品存储

### 1.3 业务范围
| 在范围内 | 不在范围内（后续版本） |
|---------|---------------------|
| 软件包基础信息 CRUD | 自动从 Harbor/Nexus 同步 |
| 版本管理（增删改查） | SBOM 解析 |
| 状态机管理 | 许可证合规分析 |
| 审核流程 | 漏洞扫描集成 |
| 分类标签管理 | 智能推荐 |

---

## 2. 用户角色与权限

| 角色 | 权限范围 |
|------|---------|
| **访客** | 查看已发布的软件包列表和详情 |
| **开发人员** | 查看已发布软件包，申请订购 |
| **软件管理员** | 软件包录入、编辑、版本管理、提交发布申请 |
| **系统管理员** | 审批发布、下架软件包、全部管理权限 |

**权限矩阵**:

| 操作 | 访客 | 开发人员 | 软件管理员 | 系统管理员 |
|------|-----|---------|-----------|-----------|
| 查看列表 | ✅ | ✅ | ✅ | ✅ |
| 查看详情 | ✅ | ✅ | ✅ | ✅ |
| 创建软件包 | ❌ | ❌ | ✅ | ✅ |
| 编辑软件包 | ❌ | ❌ | ✅ | ✅ |
| 版本管理 | ❌ | ❌ | ✅ | ✅ |
| 提交审核 | ❌ | ❌ | ✅ | ✅ |
| 审批发布 | ❌ | ❌ | ❌ | ✅ |
| 下架软件包 | ❌ | ❌ | ❌ | ✅ |

---

## 3. 功能需求

### 3.1 软件包基础信息 (REQ-300)

#### 3.1.1 字段定义

| 字段名 | 类型 | 必填 | 长度限制 | 说明 |
|--------|------|------|---------|------|
| id | BIGINT | - | - | 主键，自增 |
| package_name | VARCHAR(128) | ✅ | 2-128 | 软件包显示名称，如 "MySQL Community Server" |
| package_key | VARCHAR(64) | ✅ | 2-64 | 唯一标识，如 "mysql"，用于 URL |
| category_id | BIGINT | ✅ | - | 所属分类（数据库/中间件/开发工具等） |
| description | TEXT | ❌ | 最大 5000 | 详细描述，支持 Markdown |
| website_url | VARCHAR(256) | ❌ | - | 官网链接 |
| license_type | VARCHAR(64) | ❌ | - | 许可证类型：MIT/Apache-2.0/GPL等 |
| license_url | VARCHAR(256) | ❌ | - | 许可证链接 |
| source_url | VARCHAR(256) | ❌ | - | 源码仓库链接 |
| logo_url | VARCHAR(256) | ❌ | - | Logo 图片 URL |
| current_version | VARCHAR(32) | ❌ | - | 当前最新版本（冗余字段） |
| view_count | INT | - | - | 浏览次数，默认 0 |
| download_count | INT | - | - | 下载次数，默认 0 |
| subscription_count | INT | - | - | 订购次数，默认 0 |
| rating_avg | DECIMAL(2,1) | - | - | 平均评分 0-5，默认 5.0 |
| status | VARCHAR(20) | ✅ | - | 状态：DRAFT/PENDING/PUBLISHED/OFFLINE |
| created_by | BIGINT | ✅ | - | 创建人 ID |
| created_at | TIMESTAMP | ✅ | - | 创建时间 |
| updated_at | TIMESTAMP | ✅ | - | 更新时间 |

#### 3.1.2 业务规则

**package_key 规则** (REQ-301):
- 只允许小写字母、数字、连字符 `-`、下划线 `_`
- 必须唯一，全局不可重复
- 示例：`mysql`, `redis`, `spring-boot`, `nodejs`

**名称唯一性**:
- package_name 在同一分类下不能重复

### 3.2 软件包版本管理 (REQ-330)

#### 3.2.1 字段定义

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | - | 主键 |
| package_id | BIGINT | ✅ | 关联的软件包 ID |
| version_no | VARCHAR(32) | ✅ | 版本号，如 "8.0.33" |
| version_status | VARCHAR(20) | ✅ | 版本状态：DRAFT/PENDING/PUBLISHED/OFFLINE |
| storage_backend_id | BIGINT | ✅ | 存储后端 ID |
| storage_path | VARCHAR(512) | ❌ | 存储路径 |
| artifact_url | VARCHAR(512) | ❌ | 制品下载 URL |
| release_notes | TEXT | ❌ | 发行说明 |
| changelog | TEXT | ❌ | 变更日志 |
| is_latest | BOOLEAN | - | 是否最新版本，默认 false |
| is_lts | BOOLEAN | - | 是否 LTS 版本，默认 false |
| published_by | BIGINT | ❌ | 发布人 ID |
| published_at | TIMESTAMP | ❌ | 发布时间 |
| deprecated_by | BIGINT | ❌ | 下架人 ID |
| deprecated_at | TIMESTAMP | ❌ | 下架时间 |
| deprecate_reason | VARCHAR(256) | ❌ | 下架原因 |
| created_by | BIGINT | ✅ | 创建人 ID |
| created_at | TIMESTAMP | ✅ | 创建时间 |
| updated_at | TIMESTAMP | ✅ | 更新时间 |

#### 3.2.2 版本号规范 (REQ-331)

遵循语义化版本控制（SemVer）：
```
主版本.次版本.修订号[-预发布标识]
```

- 有效示例：`1.0.0`, `2.1.3`, `1.0.0-beta`, `2.0.0-rc.1`
- 系统自动校验版本号格式
- 同一软件禁止重复版本号录入
- 版本比较支持：系统能正确比较 `1.0.0` < `1.0.1` < `1.1.0` < `2.0.0`

#### 3.2.3 版本状态机 (REQ-320)

```
                    提交审核
    草稿 ───────────────────────▶ 待审核
     ▲                              │
     │         保存草稿             │ 审批通过
     └──────────────────────────────┤
                                    ▼
                              ┌───────────┐
                              │   已发布  │◀────────────────┐
                              └───────────┘                 │
                                    │                       │
                                    │ 下架                  │ 重新上架
                                    ▼                       │
                              ┌───────────┐   归档          │
                              │   已下架  │────────────────▶│ 已归档
                              └───────────┘                 │
                                                            │
                                    驳回 ───────────────────┘
                                    (从待审核)
```

**状态说明**：

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| DRAFT（草稿） | 编辑中，未提交审核 | 编辑、删除、提交审核 |
| PENDING（待审核） | 等待管理员审批 | 取消申请（退回草稿） |
| PUBLISHED（已发布） | 已通过审核，可在门户订购 | 下架、编辑描述信息 |
| OFFLINE（已下架） | 不再提供新订购，已有订购仍可使用 | 重新上架 |

### 3.3 软件包审核流程 (REQ-360)

#### 3.3.1 发布申请
- 软件管理员提交发布申请
- 必须选择已存在的版本进行发布
- 提交时系统校验：
  - 软件包基础信息完整
  - 至少有一个版本
  - 版本关联了存储后端

#### 3.3.2 审批操作
**审批通过**：
- 状态变为 PUBLISHED
- 记录审批人、审批时间
- 记录 published_by, published_at

**审批驳回**：
- 状态退回 DRAFT
- 必须填写驳回原因
- 申请人收到通知（站内信）

**下架操作**：
- 已发布的软件可以下架
- 下架后不可新订购，已有订购仍有效
- 必须填写下架原因

### 3.4 软件包与存储后端关联

#### 3.4.1 软件类型与存储后端映射

| 软件类型 | 存储目标 | 说明 |
|---------|---------|-----|
| DOCKER_IMAGE | Harbor | Docker 镜像 |
| HELM_CHART | Harbor | Helm Chart |
| MAVEN | Nexus | Maven 组件 |
| NPM | Nexus | NPM 包 |
| PYPI | Nexus | PyPI 包 |
| GENERIC | NAS | 通用文件 |

#### 3.4.2 制品存储逻辑
- 创建版本时选择目标存储后端
- 根据软件类型筛选可用的存储后端
- 上传制品后记录 storage_path 和 artifact_url

### 3.5 分类管理 (关联需求)

软件包必须归属一个分类，分类已在 `t_category` 表中定义：
- 开发工具
- 数据库
- 中间件
- 运维工具
- 安全工具
- AI/ML

---

## 4. 接口需求

### 4.1 软件包管理接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| /api/v1/packages | GET | 任意登录用户 | 列表查询（分页、筛选） |
| /api/v1/packages | POST | software:create | 创建软件包 |
| /api/v1/packages/{id} | GET | 任意登录用户 | 详情查询 |
| /api/v1/packages/{id} | PUT | software:update | 更新软件包 |
| /api/v1/packages/{id} | DELETE | software:delete | 删除软件包 |
| /api/v1/packages/{id}/submit | POST | software:submit | 提交审核 |
| /api/v1/packages/{id}/approve | POST | software:approve | 审批通过 |
| /api/v1/packages/{id}/reject | POST | software:approve | 审批驳回 |
| /api/v1/packages/{id}/offline | POST | software:offline | 下架 |

### 4.2 版本管理接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| /api/v1/packages/{id}/versions | GET | 任意登录用户 | 版本列表 |
| /api/v1/packages/{id}/versions | POST | software:create | 创建版本 |
| /api/v1/packages/{id}/versions/{vid} | GET | 任意登录用户 | 版本详情 |
| /api/v1/packages/{id}/versions/{vid} | PUT | software:update | 更新版本 |
| /api/v1/packages/{id}/versions/{vid} | DELETE | software:delete | 删除版本 |
| /api/v1/packages/{id}/versions/{vid}/publish | POST | software:submit | 发布版本 |

---

## 5. 前端页面需求

### 5.1 软件包列表页

**路径**: `/software/packages`

**功能**:
- 卡片式/列表式切换展示
- 搜索：关键词（名称、描述）
- 筛选：分类、状态、排序（最新/最热/评分）
- 分页：每页 12/24/48

**展示字段**:
- Logo、软件名称、简要描述
- 分类标签、当前版本
- 浏览/下载/订购次数
- 状态标签（草稿/待审核/已发布）

### 5.2 软件包录入页

**路径**: `/software/packages/create`

**表单分组**:
1. **基础信息**: 名称、package_key、分类、描述
2. **链接信息**: 官网、许可证、源码地址
3. **上传 Logo**: 图片上传组件

**校验规则**:
- package_key: 正则 `^[a-z0-9_-]+$`
- 名称: 2-128 字符
- 描述: 最大 5000 字符

### 5.3 软件包详情页

**路径**: `/software/packages/{id}`

**布局**:
- 左侧：Logo、基本信息、操作按钮
- 右侧：Tab 切换（概述/版本/文档）

**版本列表**:
- 版本号、状态、发布时间
- 下载/订购按钮
- 标记最新/LTS

### 5.4 版本管理弹窗/页面

**功能**:
- 版本列表展示
- 新增版本表单
- 版本状态管理
- 上传制品文件

---

## 6. 数据库设计

基于现有 `schema.sql` 中的表结构：

```sql
-- 已存在的表（保持不变）
-- t_software_package - 软件包主表
-- t_software_version - 软件版本表
-- t_category - 分类表
```

**索引需求**:
- idx_package_key (唯一)
- idx_package_status
- idx_package_category
- idx_version_package_id_version_no (唯一)
- idx_version_status

---

## 7. 验收标准

### 7.1 功能验收

| 验收项 | 验收标准 | 优先级 |
|--------|---------|--------|
| 软件包 CRUD | 能正常创建、查询、更新、删除软件包 | P0 |
| package_key 唯一性 | 重复 key 创建时返回明确错误 | P0 |
| 版本管理 | 能添加多个版本，版本号唯一校验 | P0 |
| 状态机 | 状态流转符合设计，操作权限控制正确 | P0 |
| 审核流程 | 提交→审批通过/驳回→发布，流程完整 | P0 |
| 存储后端关联 | 版本能选择存储后端，数据保存正确 | P0 |

### 7.2 性能验收

- 列表查询响应时间 < 500ms（1000条数据）
- 详情查询响应时间 < 200ms
- 支持并发创建（10并发无冲突）

### 7.3 测试覆盖率

- 单元测试覆盖率 ≥ 80%
- 核心业务逻辑 ≥ 90%

---

## 8. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 版本号比较逻辑复杂 | 中 | 使用成熟版本库（如 semver4j）|
| 文件上传大小限制 | 中 | 分片上传、进度显示 |
| 状态机并发冲突 | 低 | 数据库乐观锁 |

---

## 9. 后续迭代计划

| 版本 | 功能 |
|------|------|
| v1.1 | 自动从 Harbor/Nexus 同步软件包 |
| v1.2 | SBOM 解析与依赖分析 |
| v1.3 | 许可证合规检查 |

---

**文档审批**:
**审批人**:
**审批日期**:
**状态**: 待审批
