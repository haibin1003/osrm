# 功能需求文档：软件包管理

**功能编号**: REQ-300
**需求编号**: REQ-300 ~ REQ-399
**优先级**: P0 (MVP)
**创建日期**: 2026-03-20
**状态**: 需求设计阶段

---

## 1. 功能概述

### 1.1 背景
开源软件包是 OSRM 平台的核心资源。存储后端管理完成后，需要将实际的软件包纳入管理，实现从"录入→审核→发布→订购→使用"的完整闭环。

### 1.2 目标
- 支持软件包基础信息的录入和维护
- 实现软件包版本的全生命周期管理
- 建立软件包审核发布流程（一级审批）
- 对接存储后端（Harbor/Nexus/NAS）实现制品存储

### 1.3 业务范围
| 在范围内 | 不在范围内（后续版本） |
|---------|---------------------|
| 软件包基础信息 CRUD | 自动从 Harbor/Nexus 同步 |
| 版本管理（增删改查） | SBOM 解析 |
| 状态机管理 | 许可证合规分析 |
| 审核流程（一级审批） | 漏洞扫描集成 |
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
| software_type | VARCHAR(32) | ✅ | - | 软件类型：DOCKER_IMAGE/MAVEN/NPM/PYPI/GENERIC |
| category_id | BIGINT | ❌ | - | 所属分类ID（可选） |
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
| status | VARCHAR(20) | ✅ | - | 状态：DRAFT/PENDING/PUBLISHED/OFFLINE/ARCHIVED |
| created_by | BIGINT | ✅ | - | 创建人 ID |
| created_at | TIMESTAMP | ✅ | - | 创建时间 |
| updated_at | TIMESTAMP | ✅ | - | 更新时间 |
| published_by | BIGINT | ❌ | - | 发布人 ID |
| published_at | TIMESTAMP | ❌ | - | 发布时间 |

#### 3.1.2 业务规则

**package_key 规则** (REQ-301):
- 只允许小写字母、数字、连字符 `-`、下划线 `_`
- 必须唯一，全局不可重复
- 示例：`mysql`, `redis`, `spring-boot`, `nodejs`
- 正则校验：`^[a-z0-9_-]+$`

**名称唯一性**:
- package_name 全局唯一

**软件类型定义** (REQ-302):

| 类型 | 说明 | 存储后端 |
|------|------|----------|
| DOCKER_IMAGE | Docker 镜像 | Harbor |
| HELM_CHART | Helm Chart | Harbor |
| MAVEN | Maven 组件 | Nexus |
| NPM | NPM 包 | Nexus |
| PYPI | PyPI 包 | Nexus |
| GENERIC | 通用文件 | NAS |

### 3.2 软件包版本管理 (REQ-330)

#### 3.2.1 字段定义

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | - | 主键 |
| package_id | BIGINT | ✅ | 关联的软件包 ID |
| version_no | VARCHAR(32) | ✅ | 版本号，如 "8.0.33" |
| status | VARCHAR(20) | ✅ | 版本状态：DRAFT/PUBLISHED/OFFLINE |
| storage_backend_id | BIGINT | ✅ | 存储后端 ID |
| storage_path | VARCHAR(512) | ❌ | 存储路径 |
| artifact_url | VARCHAR(512) | ❌ | 制品下载 URL |
| release_notes | TEXT | ❌ | 发行说明 |
| file_size | BIGINT | ❌ | 文件大小（字节） |
| checksum | VARCHAR(128) | ❌ | 文件校验和 |
| is_latest | BOOLEAN | - | 是否最新版本，默认 false |
| published_by | BIGINT | ❌ | 发布人 ID |
| published_at | TIMESTAMP | ❌ | 发布时间 |
| created_by | BIGINT | ✅ | 创建人 ID |
| created_at | TIMESTAMP | ✅ | 创建时间 |

#### 3.2.2 版本号规范 (REQ-331)

遵循语义化版本控制（SemVer）：
```
主版本.次版本.修订号[-预发布标识]
```

- 有效示例：`1.0.0`, `2.1.3`, `1.0.0-beta`, `2.0.0-rc.1`
- 系统自动校验版本号格式
- 同一软件禁止重复版本号录入

### 3.3 软件包状态机 (REQ-320)

#### 3.3.1 状态定义

```
                    提交审核 (软件管理员)
    草稿 ───────────────────────────────▶ 待审核
     ▲                                        │
     │              驳回 (系统管理员)         │ 审批通过 (系统管理员)
     └────────────────────────────────────────┤
                                              ▼
                                        ┌───────────┐
                                        │   已发布  │◀────────────┐
                                        └───────────┘             │
                                              │                   │
                                              │ 下架              │ 重新上架
                                              ▼                   │
                                        ┌───────────┐   归档      │
                                        │   已下架  │────────────▶│ 已归档
                                        └───────────┘             │
                                                                    │
                                              驳回 ────────────────┘
```

**状态说明**：

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| DRAFT（草稿） | 编辑中，未提交审核 | 编辑、删除、提交审核、版本管理 |
| PENDING（待审核） | 等待管理员审批 | 取消申请（退回草稿） |
| PUBLISHED（已发布） | 已通过审核，可在门户订购 | 下架、编辑描述信息 |
| OFFLINE（已下架） | 不再提供新订购，已有订购仍可使用 | 重新上架 |
| ARCHIVED（已归档） | 永久下架，不可恢复 | 无 |

#### 3.3.2 状态流转规则

**提交审核** (REQ-321):
- 权限：软件管理员
- 校验：
  - 软件包基础信息完整（名称、key、类型）
  - 至少有一个已发布版本
- 操作：状态变更为 PENDING

**审批通过** (REQ-322):
- 权限：系统管理员
- 操作：状态变更为 PUBLISHED，记录 published_by 和 published_at

**审批驳回** (REQ-323):
- 权限：系统管理员
- 必须填写驳回原因
- 操作：状态退回 DRAFT

**下架** (REQ-324):
- 权限：系统管理员
- 必须填写下架原因
- 操作：状态变更为 OFFLINE
- 影响：已发布的版本变为 OFFLINE，不可新订购，已有订购仍有效

**重新上架** (REQ-325):
- 权限：系统管理员
- 操作：状态变更为 PUBLISHED

### 3.4 版本状态管理 (REQ-335)

版本状态独立于软件包状态：

| 版本状态 | 说明 | 可下载 |
|----------|------|--------|
| DRAFT | 草稿版本 | 否（仅创建者可见） |
| PUBLISHED | 已发布版本 | 是（软件包发布后） |
| OFFLINE | 已下线版本 | 否 |

**版本发布规则**:
- 软件包处于 DRAFT 状态时，可以添加/删除版本
- 提交审核时，必须至少有一个版本
- 软件包发布后，新添加的版本默认为 DRAFT，需单独发布

### 3.5 软件包审核流程 (REQ-360)

#### 3.5.1 发布申请
- 软件管理员提交发布申请
- 必须选择已存在的版本进行发布
- 提交时系统校验：
  - 软件包基础信息完整
  - 至少有一个版本
  - 版本关联了存储后端

#### 3.5.2 审批操作
**审批通过**：
- 状态变为 PUBLISHED
- 记录审批人、审批时间
- 软件包可见于门户

**审批驳回**：
- 状态退回 DRAFT
- 必须填写驳回原因
- 申请人收到通知（站内信）

### 3.6 软件包与存储后端关联

#### 3.6.1 软件类型与存储后端映射

| 软件类型 | 存储目标 | 说明 |
|---------|---------|-----|
| DOCKER_IMAGE | Harbor | Docker 镜像 |
| HELM_CHART | Harbor | Helm Chart |
| MAVEN | Nexus | Maven 组件 |
| NPM | Nexus | NPM 包 |
| PYPI | Nexus | PyPI 包 |
| GENERIC | NAS | 通用文件 |

#### 3.6.2 制品存储逻辑
- 创建版本时选择目标存储后端
- 根据软件类型筛选可用的存储后端
- 上传制品后记录 storage_path 和 artifact_url

---

## 4. 接口需求

### 4.1 软件包管理接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| /api/v1/software-packages | GET | package:read | 列表查询（分页、筛选） |
| /api/v1/software-packages | POST | package:create | 创建软件包 |
| /api/v1/software-packages/{id} | GET | package:read | 详情查询 |
| /api/v1/software-packages/{id} | PUT | package:update | 更新软件包 |
| /api/v1/software-packages/{id} | DELETE | package:delete | 删除软件包 |
| /api/v1/software-packages/{id}/submit | POST | package:update | 提交审核 |
| /api/v1/software-packages/{id}/approve | POST | package:approve | 审批通过 |
| /api/v1/software-packages/{id}/reject | POST | package:approve | 审批驳回 |
| /api/v1/software-packages/{id}/offline | POST | package:approve | 下架 |
| /api/v1/software-packages/{id}/republish | POST | package:approve | 重新上架 |
| /api/v1/software-packages/types | GET | package:read | 获取软件类型列表 |

### 4.2 版本管理接口

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| /api/v1/software-packages/{id}/versions | GET | package:read | 版本列表 |
| /api/v1/software-packages/{id}/versions | POST | package:create | 创建版本 |
| /api/v1/software-packages/{id}/versions/{vid} | GET | package:read | 版本详情 |
| /api/v1/software-packages/{id}/versions/{vid} | PUT | package:update | 更新版本 |
| /api/v1/software-packages/{id}/versions/{vid} | DELETE | package:delete | 删除版本 |
| /api/v1/software-packages/{id}/versions/{vid}/publish | POST | package:update | 发布版本 |
| /api/v1/software-packages/{id}/versions/{vid}/offline | POST | package:update | 下线版本 |

---

## 5. 前端页面需求

### 5.1 软件包列表页

**路径**: `/software/packages`

**功能**:
- 卡片式/列表式切换展示
- 搜索：关键词（名称、描述）
- 筛选：分类、状态、软件类型
- 排序：最新/最热/评分
- 分页：每页 12/24/48

**展示字段**:
- Logo、软件名称、简要描述
- 分类标签、当前版本
- 浏览/下载/订购次数
- 状态标签（草稿/待审核/已发布/已下架）

### 5.2 软件包录入页

**路径**: `/software/packages/create`

**表单分组**:
1. **基础信息**: 名称、package_key、类型、分类、描述
2. **链接信息**: 官网、许可证、源码地址
3. **上传 Logo**: 图片上传组件

**校验规则**:
- package_key: 正则 `^[a-z0-9_-]+$`，全局唯一
- 名称: 2-128 字符，全局唯一
- 描述: 最大 5000 字符

### 5.3 软件包详情页

**路径**: `/software/packages/{id}`

**布局**:
- 左侧：Logo、基本信息、操作按钮
- 右侧：Tab 切换（概述/版本/变更日志）

**版本列表**:
- 版本号、状态、发布时间
- 下载/订购按钮
- 标记最新版本

**操作按钮**（根据权限和状态显示）:
- 编辑（软件管理员，DRAFT/PENDING）
- 提交审核（软件管理员，DRAFT）
- 审批通过/驳回（系统管理员，PENDING）
- 下架（系统管理员，PUBLISHED）
- 重新上架（系统管理员，OFFLINE）

### 5.4 版本管理弹窗/页面

**功能**:
- 版本列表展示
- 新增版本表单
- 版本状态管理
- 上传制品文件
- 选择存储后端

---

## 6. 数据库设计

### 6.1 软件包表 (t_software_package)

```sql
CREATE TABLE t_software_package (
    id BIGSERIAL PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL UNIQUE,
    package_key VARCHAR(64) NOT NULL UNIQUE,
    software_type VARCHAR(32) NOT NULL, -- DOCKER_IMAGE/HELM_CHART/MAVEN/NPM/PYPI/GENERIC
    category_id BIGINT,
    description TEXT,
    website_url VARCHAR(256),
    license_type VARCHAR(64),
    license_url VARCHAR(256),
    source_url VARCHAR(256),
    logo_url VARCHAR(256),
    current_version VARCHAR(32),
    view_count INT DEFAULT 0,
    download_count INT DEFAULT 0,
    subscription_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT/PENDING/PUBLISHED/OFFLINE/ARCHIVED
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by BIGINT,
    published_at TIMESTAMP
);

CREATE INDEX idx_package_key ON t_software_package(package_key);
CREATE INDEX idx_package_status ON t_software_package(status);
CREATE INDEX idx_package_type ON t_software_package(software_type);
CREATE INDEX idx_package_category ON t_software_package(category_id);
```

### 6.2 软件版本表 (t_software_version)

```sql
CREATE TABLE t_software_version (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL REFERENCES t_software_package(id) ON DELETE CASCADE,
    version_no VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT/PUBLISHED/OFFLINE
    storage_backend_id BIGINT NOT NULL REFERENCES t_storage_backend(id),
    storage_path VARCHAR(512),
    artifact_url VARCHAR(512),
    release_notes TEXT,
    file_size BIGINT,
    checksum VARCHAR(128),
    is_latest BOOLEAN DEFAULT FALSE,
    published_by BIGINT,
    published_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(package_id, version_no)
);

CREATE INDEX idx_version_package_id ON t_software_version(package_id);
CREATE INDEX idx_version_status ON t_software_version(status);
```

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
| 状态机并发冲突 | 中 | 数据库乐观锁（version字段） |
| 文件上传大小限制 | 中 | 分片上传、进度显示 |

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
