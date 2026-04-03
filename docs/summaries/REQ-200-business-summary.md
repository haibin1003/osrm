# 功能总结文档：业务系统管理

**功能编号**: REQ-200
**功能名称**: 业务系统管理
**开发周期**: 2026-03-19 ~ 2026-03-20
**文档日期**: 2026-03-20

---

## 1. 功能概述

管理企业业务系统信息，支持按业务域分类，为软件订购提供业务系统选择。

---

## 2. 实现功能清单

### 2.1 后端功能

| 功能点 | 实现状态 | 说明 |
|--------|----------|------|
| 业务系统CRUD | ✅ | 创建、查询、更新、删除 |
| 业务域管理 | ✅ | 5个业务域：业务/运营/资源/服务/数据 |
| 启用/停用 | ✅ | 控制系统可用状态 |
| 编码唯一性 | ✅ | 系统编码全局唯一 |
| 名称唯一性 | ✅ | 系统名称全局唯一 |
| 删除保护 | ✅ | 启用的系统不能删除 |

### 2.2 前端功能

| 页面 | 实现状态 | 说明 |
|------|----------|------|
| 业务系统列表 | ✅ | 表格展示，支持搜索筛选 |
| 新增业务系统 | ✅ | 表单录入 |
| 编辑业务系统 | ✅ | 修改系统信息 |
| 业务域管理 | ✅ | 独立页面管理业务域 |

### 2.3 接口清单

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| /api/v1/business-systems | GET | business-system:read | 列表查询（分页、搜索） |
| /api/v1/business-systems/{id} | GET | business-system:read | 详情查询 |
| /api/v1/business-systems | POST | business-system:create | 新增业务系统 |
| /api/v1/business-systems/{id} | PUT | business-system:update | 编辑业务系统 |
| /api/v1/business-systems/{id} | DELETE | business-system:approve | 删除业务系统 |
| /api/v1/business-systems/{id}/status | PUT | business-system:update | 启用/停用 |
| /api/v1/business-systems/domains | GET | business-system:read | 获取业务域列表 |

---

## 3. 数据库设计

### 表结构

```sql
CREATE TABLE t_business_system (
    id BIGSERIAL PRIMARY KEY,
    system_code VARCHAR(64) NOT NULL UNIQUE,
    system_name VARCHAR(128) NOT NULL UNIQUE,
    domain VARCHAR(32) NOT NULL, -- BUSINESS/OPERATION/RESOURCE/SERVICE/DATA
    responsible_person VARCHAR(64),
    description VARCHAR(500),
    enabled BOOLEAN DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_business_system_domain ON t_business_system(domain);
CREATE INDEX idx_business_system_enabled ON t_business_system(enabled);
```

---

## 4. 测试报告

### 4.1 单元测试

**测试类**: `BusinessSystemAppServiceTest`

| 测试方法 | 说明 | 状态 |
|----------|------|------|
| findByConditions_shouldReturnPagedResults | 分页查询 | ✅ |
| findByConditions_withKeyword_shouldFilter | 关键词筛选 | ✅ |
| findByConditions_withDomain_shouldFilter | 业务域筛选 | ✅ |
| findById_withExistingId_shouldReturnDTO | 查询详情 | ✅ |
| findById_withNonExistingId_shouldThrowException | 查询不存在 | ✅ |
| create_withValidRequest_shouldCreateSystem | 创建成功 | ✅ |
| create_withDuplicateCode_shouldThrowException | 编码重复校验 | ✅ |
| create_withDuplicateName_shouldThrowException | 名称重复校验 | ✅ |
| update_withValidRequest_shouldUpdateSystem | 更新成功 | ✅ |
| update_withNonExistingId_shouldThrowException | 更新不存在 | ✅ |
| update_withDuplicateName_shouldThrowException | 更新名称重复 | ✅ |
| setEnabled_shouldToggleStatus | 启用/停用 | ✅ |
| setEnabled_withNonExistingId_shouldThrowException | 操作不存在 | ✅ |
| delete_withDisabledSystem_shouldDelete | 删除成功 | ✅ |
| delete_withEnabledSystem_shouldThrowException | 启用状态保护 | ✅ |
| delete_withNonExistingId_shouldThrowException | 删除不存在 | ✅ |
| getAllDomains_shouldReturnAllDomains | 获取业务域 | ✅ |

**测试统计**:
- 总测试数: 17个
- 通过率: 100%
- 代码覆盖率: ~70%

### 4.2 集成测试

**测试类**: `BusinessSystemControllerIntegrationTest`

- API接口连通性测试
- 权限控制测试

---

## 5. 前端页面清单

| 页面 | 路径 | 功能 |
|------|------|------|
| 业务系统列表 | /business/systems | 表格展示，搜索筛选分页 |
| 业务域管理 | /business/domains | 业务域列表展示 |

---

## 6. 业务规则实现

### 6.1 系统编码规则
- 唯一性：全局唯一
- 格式建议：小写字母、数字、连字符、下划线
- 示例：`order-sys`, `payment-service`

### 6.2 删除约束
- 已启用的业务系统不能删除

### 6.3 业务域定义

| 编码 | 名称 | 说明 |
|------|------|------|
| BUSINESS | 业务域 | 核心业务系统 |
| OPERATION | 运营域 | 运营支撑系统 |
| RESOURCE | 资源域 | 资源管理系统 |
| SERVICE | 服务域 | 公共服务系统 |
| DATA | 数据域 | 数据相关系统 |

---

## 7. 已知问题/限制

| 问题 | 说明 | 计划修复版本 |
|------|------|-------------|
| 无 | 功能完整，无已知问题 | - |

---

## 8. 后续迭代计划

| 版本 | 功能 |
|------|------|
| v1.1 | 业务系统与订购记录关联统计 |
| v1.2 | 业务系统负责人通知功能 |

---

## 9. 交付物清单

- [x] 后端代码（Domain/Application/Infrastructure/Interface层）
- [x] 前端页面（列表、业务域管理）
- [x] 数据库表结构
- [x] 单元测试（17个测试用例）
- [x] 集成测试
- [x] API文档

---

**总结**: 业务系统管理模块已完整实现需求，功能简洁实用，测试覆盖良好，可进入维护阶段。
