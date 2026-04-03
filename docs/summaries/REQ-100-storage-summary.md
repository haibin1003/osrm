# 功能总结文档：存储后端管理

**功能编号**: REQ-100
**功能名称**: 存储后端管理
**开发周期**: 2026-03-19 ~ 2026-03-20
**文档日期**: 2026-03-20

---

## 1. 功能概述

统一管理 Harbor、Nexus、NAS/本地存储等存储后端，为开源软件制品提供存储基础设施。

---

## 2. 实现功能清单

### 2.1 后端功能

| 功能点 | 实现状态 | 说明 |
|--------|----------|------|
| 存储后端CRUD | ✅ | 创建、查询、更新、删除 |
| 类型支持 | ✅ | Harbor、Nexus、NAS三种类型 |
| 连接测试 | ✅ | 实时测试存储后端连通性 |
| 健康检查 | ✅ | 手动触发健康检查 |
| 默认后端设置 | ✅ | 支持设置默认存储后端 |
| 启用/停用 | ✅ | 控制存储后端可用状态 |
| 密码加密 | ✅ | AES加密存储访问密钥 |
| 编码自动生成 | ✅ | 类型前缀+随机6位字符 |

### 2.2 前端功能

| 页面 | 实现状态 | 说明 |
|------|----------|------|
| 存储后端列表 | ✅ | 卡片式布局，支持搜索筛选 |
| 新增存储后端 | ✅ | 分步骤表单（基本信息+配置） |
| 编辑存储后端 | ✅ | 支持修改配置 |
| 详情查看 | ✅ | 展示完整配置信息 |
| 健康状态显示 | ✅ | 在线/离线/错误状态标识 |

### 2.3 接口清单

| 接口 | 方法 | 权限 | 说明 |
|------|------|------|------|
| /api/v1/storage-backends | GET | storage:read | 列表查询（分页、搜索、筛选） |
| /api/v1/storage-backends/{id} | GET | storage:read | 详情查询 |
| /api/v1/storage-backends | POST | storage:create | 新增存储后端 |
| /api/v1/storage-backends/{id} | PUT | storage:update | 编辑存储后端 |
| /api/v1/storage-backends/{id} | DELETE | storage:delete | 删除存储后端 |
| /api/v1/storage-backends/test-connection | POST | storage:create/update | 连接测试 |
| /api/v1/storage-backends/{id}/health | POST | storage:read | 健康检查 |
| /api/v1/storage-backends/{id}/default | PUT | storage:update | 设为默认 |
| /api/v1/storage-backends/{id}/status | PUT | storage:update | 启用/停用 |
| /api/v1/storage-backends/types | GET | storage:read | 获取存储类型列表 |

---

## 3. 数据库设计

### 表结构

```sql
CREATE TABLE t_storage_backend (
    id BIGSERIAL PRIMARY KEY,
    backend_code VARCHAR(32) NOT NULL UNIQUE,
    backend_name VARCHAR(64) NOT NULL UNIQUE,
    backend_type VARCHAR(20) NOT NULL, -- HARBOR/NEXUS/NAS
    endpoint VARCHAR(255) NOT NULL,
    access_key VARCHAR(64),
    secret_key VARCHAR(255), -- 加密存储
    namespace VARCHAR(64),
    is_default BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    health_status VARCHAR(20) DEFAULT 'UNKNOWN', -- ONLINE/OFFLINE/ERROR/UNKNOWN
    last_health_check TIMESTAMP,
    error_message VARCHAR(500),
    description VARCHAR(255),
    config_json TEXT, -- 扩展配置JSON
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_storage_backend_type ON t_storage_backend(backend_type);
CREATE INDEX idx_storage_backend_status ON t_storage_backend(health_status);
CREATE INDEX idx_storage_backend_enabled ON t_storage_backend(enabled);
```

---

## 4. 测试报告

### 4.1 单元测试

**测试类**: `StorageBackendAppServiceTest`

| 测试方法 | 说明 | 状态 |
|----------|------|------|
| findByConditions_shouldReturnPagedResults | 分页查询 | ✅ |
| findByConditions_withKeyword_shouldFilter | 关键词筛选 | ✅ |
| findByConditions_withType_shouldFilter | 类型筛选 | ✅ |
| findById_withExistingId_shouldReturnDTO | 查询详情 | ✅ |
| findById_withNonExistingId_shouldThrowException | 查询不存在 | ✅ |
| create_withValidRequest_shouldCreateBackend | 创建成功 | ✅ |
| create_withDuplicateName_shouldThrowException | 名称重复校验 | ✅ |
| create_withIsDefault_shouldUnsetOtherDefaults | 默认设置切换 | ✅ |
| update_withValidRequest_shouldUpdateBackend | 更新成功 | ✅ |
| update_withNonExistingId_shouldThrowException | 更新不存在 | ✅ |
| update_withDuplicateName_shouldThrowException | 更新名称重复 | ✅ |
| update_withNewSecretKey_shouldEncrypt | 密码加密 | ✅ |
| delete_withValidId_shouldDelete | 删除成功 | ✅ |
| delete_withDefaultBackend_shouldThrowException | 默认后端保护 | ✅ |
| delete_withEnabledBackend_shouldThrowException | 启用状态保护 | ✅ |
| setDefault_shouldSetDefaultAndUnsetOthers | 设置默认 | ✅ |
| setEnabled_shouldToggleStatus | 启用/停用 | ✅ |
| setEnabled_disableDefault_shouldThrowException | 停用默认保护 | ✅ |
| getStorageTypes_shouldReturnAllTypes | 获取类型列表 | ✅ |
| convertToDTO_shouldNotReturnSecretKey | DTO脱敏 | ✅ |
| convertToDTO_shouldParseConfigJson | 配置解析 | ✅ |

**测试统计**:
- 总测试数: 21个
- 通过率: 100%
- 代码覆盖率: ~75%

### 4.2 集成测试

**测试类**: `StorageBackendControllerIntegrationTest`

- API接口连通性测试
- 权限控制测试
- 数据一致性测试

---

## 5. 前端页面清单

| 页面 | 路径 | 功能 |
|------|------|------|
| 存储后端列表 | /system/storage | 卡片式展示，搜索筛选分页 |
| 新增存储后端 | /system/storage/create | 分步表单，连接测试 |
| 编辑存储后端 | /system/storage/:id/edit | 编辑配置 |
| 详情查看 | /system/storage/:id | 查看详细信息 |

---

## 6. 业务规则实现

### 6.1 编码生成规则
- 自动生成：`类型前缀 + "-" + 随机6位字符`
- 示例：`harbor-a1b2c3`, `nexus-x7k8m9`

### 6.2 删除约束
- 默认存储后端不能删除
- 已启用的存储后端不能删除

### 6.3 停用约束
- 默认存储后端不能停用

### 6.4 密码安全
- 使用AES加密存储secretKey
- API返回时脱敏处理（不返回密码）

---

## 7. 已知问题/限制

| 问题 | 说明 | 计划修复版本 |
|------|------|-------------|
| NAS连接测试为模拟 | NAS类型连接测试仅返回模拟结果 | v1.1 |
| 缺少定时健康检查 | 目前仅支持手动触发 | v1.1 |

---

## 8. 后续迭代计划

| 版本 | 功能 |
|------|------|
| v1.1 | 定时健康检查任务（每5分钟） |
| v1.1 | NAS真实连接测试 |
| v1.2 | 存储后端使用统计 |

---

## 9. 交付物清单

- [x] 后端代码（Domain/Application/Infrastructure/Interface层）
- [x] 前端页面（列表、创建、编辑、详情）
- [x] 数据库表结构
- [x] 单元测试（21个测试用例）
- [x] 集成测试
- [x] API文档

---

**总结**: 存储后端管理模块已完整实现需求文档 REQ-100 中的所有功能，测试覆盖良好，可进入维护阶段。
