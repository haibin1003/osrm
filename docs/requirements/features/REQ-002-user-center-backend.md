# REQ-002: 用户中心后端API需求设计

## 文档信息

| 属性 | 值 |
|------|-----|
| 需求编号 | REQ-002 |
| 需求名称 | 用户中心后端API |
| 所属模块 | 用户中心 |
| 优先级 | P0 |
| 状态 | 设计中 |
| 创建时间 | 2026-03-18 |
| 维护责任人 | 架构师 |

---

## 1. 功能概述

为前端用户中心页面提供完整的RESTful API支持，包括用户管理、角色管理、权限管理、个人中心和系统设置五大功能模块。

---

## 2. 功能需求

### 2.1 用户管理

#### 2.1.1 用户列表查询
- **功能描述**: 分页查询用户列表，支持按用户名、真实姓名、状态筛选
- **请求方法**: GET
- **请求路径**: `/api/v1/users`
- **请求参数**:
  - `page` (int): 页码，默认1
  - `size` (int): 每页条数，默认10
  - `username` (string, optional): 用户名模糊查询
  - `realName` (string, optional): 真实姓名模糊查询
  - `enabled` (boolean, optional): 状态筛选
- **响应数据**:
  ```json
  {
    "code": 200,
    "data": {
      "content": [...],
      "totalElements": 100,
      "totalPages": 10,
      "number": 0,
      "size": 10
    }
  }
  ```

#### 2.1.2 新增用户
- **功能描述**: 创建新用户
- **请求方法**: POST
- **请求路径**: `/api/v1/users`
- **请求体**:
  ```json
  {
    "username": "string",      // 必填，3-20字符
    "realName": "string",      // 必填
    "email": "string",         // 可选，邮箱格式
    "phone": "string",         // 可选，手机号格式
    "password": "string",      // 必填，至少6位
    "roleIds": [1, 2],         // 必填，角色ID列表
    "enabled": true            // 必填，默认true
  }
  ```

#### 2.1.3 编辑用户
- **功能描述**: 修改用户信息
- **请求方法**: PUT
- **请求路径**: `/api/v1/users/{id}`
- **请求体**: 同新增，但不包含password字段

#### 2.1.4 删除用户
- **功能描述**: 删除用户（逻辑删除或物理删除）
- **请求方法**: DELETE
- **请求路径**: `/api/v1/users/{id}`
- **约束**: 不能删除当前登录用户

#### 2.1.5 重置密码
- **功能描述**: 管理员重置用户密码
- **请求方法**: PUT
- **请求路径**: `/api/v1/users/{id}/password`
- **请求体**:
  ```json
  {
    "newPassword": "string"    // 必填，至少6位
  }
  ```

#### 2.1.6 切换用户状态
- **功能描述**: 启用/禁用用户
- **请求方法**: PUT
- **请求路径**: `/api/v1/users/{id}/status`
- **请求体**:
  ```json
  {
    "enabled": true
  }
  ```

### 2.2 角色管理

#### 2.2.1 角色列表查询
- **请求方法**: GET
- **请求路径**: `/api/v1/roles`
- **请求参数**:
  - `page` (int): 页码
  - `size` (int): 每页条数
  - `roleName` (string, optional): 角色名称模糊查询
  - `roleCode` (string, optional): 角色编码模糊查询

#### 2.2.2 新增角色
- **请求方法**: POST
- **请求路径**: `/api/v1/roles`
- **请求体**:
  ```json
  {
    "roleCode": "ROLE_ADMIN",    // 必填，格式ROLE_XXX
    "roleName": "系统管理员",     // 必填
    "description": "string"      // 可选
  }
  ```

#### 2.2.3 编辑角色
- **请求方法**: PUT
- **请求路径**: `/api/v1/roles/{id}`

#### 2.2.4 删除角色
- **请求方法**: DELETE
- **请求路径**: `/api/v1/roles/{id}`
- **约束**: 角色已被用户使用则不能删除

#### 2.2.5 配置角色权限
- **请求方法**: PUT
- **请求路径**: `/api/v1/roles/{id}/permissions`
- **请求体**:
  ```json
  {
    "permissionIds": [1, 2, 3]
  }
  ```

#### 2.2.6 获取角色权限
- **请求方法**: GET
- **请求路径**: `/api/v1/roles/{id}/permissions`

### 2.3 权限管理

#### 2.3.1 权限树查询
- **请求方法**: GET
- **请求路径**: `/api/v1/permissions/tree`
- **响应数据**: 树形结构的权限列表

#### 2.3.2 权限列表查询（平级）
- **请求方法**: GET
- **请求路径**: `/api/v1/permissions`
- **请求参数**:
  - `permissionName` (string, optional)
  - `permissionCode` (string, optional)
  - `resourceType` (string, optional): menu/button/api/data

#### 2.3.3 新增权限
- **请求方法**: POST
- **请求路径**: `/api/v1/permissions`
- **请求体**:
  ```json
  {
    "parentId": null,             // 可选，上级权限ID
    "permissionName": "用户管理",  // 必填
    "permissionCode": "user:read", // 必填，格式如xxx:xxx
    "resourceType": "button",     // 必填：menu/button/api/data
    "action": "read",             // 可选：read/create/update/delete/export/import
    "path": "/system/users",      // 可选
    "icon": "User",               // 可选
    "sortOrder": 1,               // 可选，默认0
    "description": "string"       // 可选
  }
  ```

#### 2.3.4 编辑权限
- **请求方法**: PUT
- **请求路径**: `/api/v1/permissions/{id}`

#### 2.3.5 删除权限
- **请求方法**: DELETE
- **请求路径**: `/api/v1/permissions/{id}`
- **约束**: 有子权限时不能删除

### 2.4 个人中心

#### 2.4.1 获取个人信息
- **请求方法**: GET
- **请求路径**: `/api/v1/profile`
- **说明**: 使用当前登录用户ID

#### 2.4.2 更新个人信息
- **请求方法**: PUT
- **请求路径**: `/api/v1/profile`
- **请求体**:
  ```json
  {
    "realName": "string",
    "email": "string",
    "phone": "string",
    "bio": "string"
  }
  ```

#### 2.4.3 修改密码
- **请求方法**: PUT
- **请求路径**: `/api/v1/profile/password`
- **请求体**:
  ```json
  {
    "currentPassword": "string",  // 必填
    "newPassword": "string"       // 必填，至少6位
  }
  ```

### 2.5 系统设置

#### 2.5.1 获取系统设置
- **请求方法**: GET
- **请求路径**: `/api/v1/settings`
- **响应数据**: 按分类返回配置项

#### 2.5.2 更新系统设置
- **请求方法**: PUT
- **请求路径**: `/api/v1/settings`
- **请求体**:
  ```json
  {
    "category": "basic",
    "settings": {
      "key": "value"
    }
  }
  ```

#### 2.5.3 按分类获取设置
- **请求方法**: GET
- **请求路径**: `/api/v1/settings/{category}`

---

## 3. 数据库设计

### 3.1 用户表 t_user
```sql
-- 已有，补充字段
ALTER TABLE t_user ADD COLUMN phone VARCHAR(20);
ALTER TABLE t_user ADD COLUMN bio TEXT;
ALTER TABLE t_user ADD COLUMN avatar VARCHAR(500);
ALTER TABLE t_user ADD COLUMN deleted BOOLEAN DEFAULT FALSE;
```

### 3.2 角色表 t_role
```sql
CREATE TABLE t_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.3 权限表 t_permission
```sql
CREATE TABLE t_permission (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES t_permission(id),
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) NOT NULL, -- menu/button/api/data
    action VARCHAR(20), -- read/create/update/delete/export/import
    path VARCHAR(500),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.4 角色权限关联表 t_role_permission
```sql
CREATE TABLE t_role_permission (
    role_id BIGINT REFERENCES t_role(id),
    permission_id BIGINT REFERENCES t_permission(id),
    PRIMARY KEY (role_id, permission_id)
);
```

### 3.5 用户角色关联表 t_user_role（替换原有user_roles字段）
```sql
CREATE TABLE t_user_role (
    user_id BIGINT REFERENCES t_user(id),
    role_id BIGINT REFERENCES t_role(id),
    PRIMARY KEY (user_id, role_id)
);
```

### 3.6 系统设置表 t_system_setting
```sql
CREATE TABLE t_system_setting (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (category, setting_key)
);
```

---

## 4. 权限控制

| API | 所需权限 |
|-----|----------|
| 用户管理 | `user:read`, `user:create`, `user:update`, `user:delete` |
| 角色管理 | `role:read`, `role:create`, `role:update`, `role:delete` |
| 权限管理 | `permission:read`, `permission:create`, `permission:update`, `permission:delete` |
| 个人中心 | 登录即可 |
| 系统设置 | `system:settings` |

---

## 5. 验收标准

1. **功能完整性**: 所有API功能正常，返回数据格式正确
2. **权限控制**: 未授权访问返回403，未登录返回401
3. **数据校验**: 参数校验失败返回400，业务错误返回相应错误码
4. **测试覆盖**: 单元测试覆盖率≥80%，集成测试覆盖所有API
5. **性能**: 列表查询响应时间<200ms（单表1000条数据）

---

## 6. 变更记录

| 时间 | 变更内容 | 变更人 | 状态 |
|------|----------|--------|------|
| 2026-03-18 | 创建需求文档 | 架构师 | 已完成 |
