# REQ-001: 用户中心模块需求设计

## 文档信息

- **需求编号**: REQ-001
- **需求名称**: 用户中心
- **模块**: 用户认证与权限管理
- **优先级**: P0 (MVP核心功能)
- **作者**: OSRM 架构团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-17
- **版本**: v1.0

---

## 变更记录

| 时间 | 变更内容 | 变更人 | 版本 |
|------|----------|--------|------|
| 2026-03-17 | 初始版本 | 架构师 | v1.0 |

---

## 1. 概述

### 1.1 功能描述

用户中心模块是 OSRM 系统的基础模块，负责用户的身份认证、会话管理和权限控制。该模块为整个系统提供统一的认证入口，确保只有合法用户才能访问系统资源。

### 1.2 业务价值

- 保障系统安全：通过身份认证防止未授权访问
- 权限分级管理：基于角色的权限控制，满足不同用户群体的需求
- 无状态设计：JWT Token 机制支持水平扩展

### 1.3 涉及角色

| 角色 | 权限范围 | 说明 |
|------|----------|------|
| 访客 (ROLE_VISITOR) | 软件浏览、搜索 | 未登录用户，仅可查看公开信息 |
| 开发人员 (ROLE_DEVELOPER) | + 软件订购、下载 | 企业内部开发人员 |
| 软件管理员 (ROLE_PACKAGE_MANAGER) | + 软件录入、审核 | 负责软件包管理 |
| 系统管理员 (ROLE_SYSTEM_ADMIN) | 全部权限 | 系统运维管理 |

---

## 2. 功能需求

### 2.1 用户登录 (REQ-001-001)

#### 功能描述
用户通过用户名和密码进行身份认证，成功登录后获取访问令牌。

#### 业务流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        用户登录流程                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐         │
│   │ 输入凭证 │───▶│ 校验格式 │───▶│ 验证密码 │───▶│ 生成Token│         │
│   │         │    │         │    │         │    │         │         │
│   │ 用户名  │    │ 非空检查│    │ BCrypt  │    │ Access  │         │
│   │ 密码    │    │ 长度检查│    │ 比对    │    │ Refresh │         │
│   └─────────┘    └────┬────┘    └────┬────┘    └─────────┘         │
│                        │              │                             │
│                        ▼              ▼                             │
│                   ┌─────────┐    ┌─────────┐                       │
│                   │格式错误 │    │密码错误 │                       │
│                   │400错误 │    │401错误 │                       │
│                   └─────────┘    └─────────┘                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 输入

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，3-20字符，字母数字下划线 |
| password | string | 是 | 密码，6-20字符 |

#### 输出

**成功响应 (HTTP 200):**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "user": {
      "id": 10001,
      "username": "zhangsan",
      "realName": "张三",
      "roles": ["ROLE_DEVELOPER"],
      "permissions": ["package:read", "subscription:create"]
    }
  }
}
```

**失败响应:**
- HTTP 400: 请求参数错误
- HTTP 401: 用户名或密码错误
- HTTP 429: 登录过于频繁（限流）

#### 业务规则

| 规则编号 | 规则描述 | 优先级 |
|----------|----------|--------|
| R1 | 用户名密码必须完全匹配才能登录 | 高 |
| R2 | 密码使用 BCrypt 算法加密存储 | 高 |
| R3 | 用户被禁用时无法登录 | 高 |
| R4 | 登录失败5次后锁定账户15分钟 | 中 |
| R5 | 同一用户最多3个并发会话 | 中 |

#### 验收标准

- [ ] 正确用户名密码可成功登录并返回 Token
- [ ] 错误密码返回 401 错误，提示"用户名或密码错误"
- [ ] 禁用用户登录返回 401 错误，提示"账户已被禁用"
- [ ] 连续5次失败锁定账户15分钟
- [ ] Access Token 有效期2小时，Refresh Token 有效期7天

---

### 2.2 Token 刷新 (REQ-001-002)

#### 功能描述
当 Access Token 即将过期或已过期时，使用 Refresh Token 换取新的访问令牌。

#### 业务流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Token 刷新流程                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐         │
│   │Refresh  │───▶│Token    │───▶│Token    │───▶│生成新   │         │
│   │Token    │    │格式校验 │    │有效性   │    │Token对  │         │
│   │         │    │         │    │检查     │    │         │         │
│   └─────────┘    └────┬────┘    └────┬────┘    └─────────┘         │
│                        │              │                             │
│                        ▼              ▼                             │
│                   ┌─────────┐    ┌─────────┐                       │
│                   │格式错误 │    │Token过期│                       │
│                   │400错误 │    │或无效  │                       │
│                   └─────────┘    │401错误 │                       │
│                                  └─────────┘                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 输入

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 刷新令牌 |

#### 输出

**成功响应 (HTTP 200):**
```json
{
  "code": 200,
  "message": "刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 7200
  }
}
```

**注意**: Refresh Token 轮换策略，每次刷新同时生成新的 Refresh Token，旧的作废。

#### 业务规则

| 规则编号 | 规则描述 | 优先级 |
|----------|----------|--------|
| R1 | Refresh Token 只能使用一次 | 高 |
| R2 | Refresh Token 过期后需重新登录 | 高 |
| R3 | 用户禁用后 Refresh Token 立即失效 | 高 |
| R4 | 刷新时生成新的 Token 对（轮换策略） | 中 |

#### 验收标准

- [ ] 有效 Refresh Token 可成功换取新的 Token 对
- [ ] 旧的 Refresh Token 使用后立即失效
- [ ] 已过期的 Refresh Token 返回 401，要求重新登录
- [ ] 用户禁用后，其 Refresh Token 立即失效

---

### 2.3 用户登出 (REQ-001-003)

#### 功能描述
用户主动退出系统，吊销当前会话的 Token。

#### 业务流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                        用户登出流程                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐                        │
│   │ 登出请求 │───▶│ Token   │───▶│ 吊销    │                        │
│   │ + Token │    │ 解析    │    │ Token   │                        │
│   │         │    │         │    │         │                        │
│   └─────────┘    └─────────┘    └────┬────┘                        │
│                                      │                               │
│                                      ▼                               │
│                               ┌─────────────┐                        │
│                               │ 加入黑名单  │                        │
│                               │ (Redis)     │                        │
│                               │ TTL=剩余时间│                        │
│                               └─────────────┘                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 输入

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| Authorization | Header | 是 | Bearer Token |

#### 输出

**成功响应 (HTTP 200):**
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

#### 业务规则

| 规则编号 | 规则描述 | 优先级 |
|----------|----------|--------|
| R1 | 登出后 Token 立即失效 | 高 |
| R2 | 使用 Redis 黑名单机制实现 | 高 |
| R3 | 黑名单 TTL 设置为 Token 剩余有效期 | 中 |

#### 验收标准

- [ ] 登出后，原 Token 无法访问受保护资源
- [ ] 登出接口可重复调用（幂等性）
- [ ] Redis 中黑名单记录自动过期

---

### 2.4 权限控制 (REQ-001-006)

#### 功能描述
基于 RBAC 模型的权限控制，验证用户是否有权访问特定资源。

#### 权限模型

```
┌─────────────────────────────────────────────────────────────────────┐
│                        RBAC 权限模型                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌──────────────┐         ┌──────────────┐         ┌──────────────┐│
│   │     用户     │◄───────►│     角色     │◄───────►│     权限     ││
│   │    User      │  N:M    │    Role      │  N:M    │  Permission  ││
│   └──────────────┘         └──────────────┘         └──────────────┘│
│                                                                      │
│   预定义角色权限:                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ ROLE_VISITOR:                                               │   │
│   │   • package:read (软件浏览)                                  │   │
│   │                                                             │   │
│   │ ROLE_DEVELOPER:                                             │   │
│   │   • package:read (软件浏览)                                  │   │
│   │   • subscription:create (订购申请)                           │   │
│   │   • subscription:read (查看我的订购)                         │   │
│   │                                                             │   │
│   │ ROLE_PACKAGE_MANAGER:                                       │   │
│   │   • package:* (软件包管理全部权限)                            │   │
│   │   • subscription:approve (订购审批)                          │   │
│   │   • business-system:* (业务系统管理)                         │   │
│   │                                                             │   │
│   │ ROLE_SYSTEM_ADMIN:                                          │   │
│   │   • *:* (全部权限)                                           │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 权限定义

| 权限编码 | 权限名称 | 描述 |
|----------|----------|------|
| package:read | 软件浏览 | 查看软件列表、详情 |
| package:create | 软件录入 | 创建软件包 |
| package:update | 软件编辑 | 修改软件信息 |
| package:delete | 软件删除 | 删除软件包 |
| package:approve | 软件审批 | 审核软件发布 |
| subscription:create | 订购申请 | 申请订购软件 |
| subscription:read | 订购查看 | 查看订购记录 |
| subscription:approve | 订购审批 | 审批订购申请 |
| business-system:create | 业务系统录入 | 创建业务系统 |
| business-system:read | 业务系统查看 | 查看业务系统 |
| business-system:update | 业务系统编辑 | 修改业务系统 |
| system:* | 系统管理 | 用户、角色、配置管理 |

#### 业务规则

| 规则编号 | 规则描述 | 优先级 |
|----------|----------|--------|
| R1 | 每个用户可拥有多个角色 | 高 |
| R2 | 权限检查基于用户拥有的所有角色的并集 | 高 |
| R3 | 无权限访问返回 HTTP 403 | 高 |
| R4 | 支持通配符权限（如 package:*） | 中 |

#### 验收标准

- [ ] 用户访问有权限的资源成功
- [ ] 用户访问无权限的资源返回 403
- [ ] 角色变更后，权限立即生效
- [ ] 支持方法级别的权限注解控制

---

## 3. 数据库设计

### 3.1 ER 图

```
┌─────────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│       t_user        │       │   t_user_role       │       │      t_role         │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ PK  id              │◄──────┤ PK  id              │       │ PK  id              │
│     username        │   1:N │ FK  user_id         │   N:1 │     role_code       │
│     password        │       │ FK  role_id         │──────►│     role_name       │
│     real_name       │       │     created_at      │       │     description     │
│     email           │       └─────────────────────┘       │     enabled         │
│     phone           │                                     └──────────┬──────────┘
│     enabled         │                                                │
│     last_login_at   │                                     ┌──────────┴──────────┐
│     created_at      │                                     │   t_role_permission │
└─────────────────────┘                                     ├─────────────────────┤
                                                            │ PK  id              │
                                                            │ FK  role_id         │
                                                            │ FK  permission_id   │
                                                            └─────────────────────┘
                                                                       │
                                                                       ▼
                                                            ┌─────────────────────┐
                                                            │   t_permission      │
                                                            ├─────────────────────┤
                                                            │ PK  id              │
                                                            │     permission_code │
                                                            │     permission_name │
                                                            │     resource_type   │
                                                            │     action          │
                                                            └─────────────────────┘
```

### 3.2 表结构

#### t_user (用户表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| username | VARCHAR | 32 | 是 | - | 用户名，唯一索引 |
| password | VARCHAR | 128 | 是 | - | BCrypt加密密码 |
| real_name | VARCHAR | 64 | 否 | - | 真实姓名 |
| email | VARCHAR | 128 | 否 | - | 邮箱 |
| phone | VARCHAR | 20 | 否 | - | 手机号 |
| enabled | BOOLEAN | - | 是 | TRUE | 是否启用 |
| locked_until | TIMESTAMP | - | 否 | - | 锁定截止时间 |
| login_fail_count | INT | - | 是 | 0 | 连续登录失败次数 |
| last_login_at | TIMESTAMP | - | 否 | - | 最后登录时间 |
| created_at | TIMESTAMP | - | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引:**
- `idx_username`: username (UNIQUE)
- `idx_enabled`: enabled

#### t_role (角色表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| role_code | VARCHAR | 32 | 是 | - | 角色编码，唯一索引 |
| role_name | VARCHAR | 64 | 是 | - | 角色名称 |
| description | VARCHAR | 256 | 否 | - | 角色描述 |
| enabled | BOOLEAN | - | 是 | TRUE | 是否启用 |
| created_at | TIMESTAMP | - | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | - | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引:**
- `idx_role_code`: role_code (UNIQUE)

**初始数据:**

| role_code | role_name | description |
|-----------|-----------|-------------|
| ROLE_VISITOR | 访客 | 仅可浏览公开信息 |
| ROLE_DEVELOPER | 开发人员 | 可浏览和订购软件 |
| ROLE_PACKAGE_MANAGER | 软件管理员 | 管理软件包和审批 |
| ROLE_SYSTEM_ADMIN | 系统管理员 | 全部权限 |

#### t_user_role (用户角色关联表)

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| user_id | BIGINT | 是 | 用户ID，外键 |
| role_id | BIGINT | 是 | 角色ID，外键 |
| created_at | TIMESTAMP | 是 | 创建时间 |

**索引:**
- `idx_user_role`: (user_id, role_id) (UNIQUE)

#### t_permission (权限表)

| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| id | BIGINT | - | 是 | 自增 | 主键 |
| permission_code | VARCHAR | 64 | 是 | - | 权限编码，唯一索引 |
| permission_name | VARCHAR | 64 | 是 | - | 权限名称 |
| resource_type | VARCHAR | 32 | 是 | - | 资源类型 |
| action | VARCHAR | 32 | 是 | - | 操作类型 |
| description | VARCHAR | 256 | 否 | - | 权限描述 |
| created_at | TIMESTAMP | - | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引:**
- `idx_permission_code`: permission_code (UNIQUE)

#### t_role_permission (角色权限关联表)

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键，自增 |
| role_id | BIGINT | 是 | 角色ID，外键 |
| permission_id | BIGINT | 是 | 权限ID，外键 |
| created_at | TIMESTAMP | 是 | 创建时间 |

**索引:**
- `idx_role_permission`: (role_id, permission_id) (UNIQUE)

---

## 4. API 接口设计

### 4.1 接口清单

| 接口 | 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|------|
| 用户登录 | POST | /api/v1/auth/login | 公开 | 用户身份认证 |
| Token刷新 | POST | /api/v1/auth/refresh | 公开 | 刷新访问令牌 |
| 用户登出 | POST | /api/v1/auth/logout | 需认证 | 用户退出登录 |
| 获取当前用户 | GET | /api/v1/auth/me | 需认证 | 获取登录用户信息 |
| 修改密码 | PUT | /api/v1/auth/password | 需认证 | 修改当前用户密码 |

### 4.2 接口详情

#### 用户登录

```yaml
接口: POST /api/v1/auth/login
标签: 认证
描述: 用户登录获取访问令牌

请求体:
  content:
    application/json:
      schema:
        type: object
        required: [username, password]
        properties:
          username:
            type: string
            minLength: 3
            maxLength: 20
            pattern: '^[a-zA-Z0-9_]+$'
            description: 用户名
          password:
            type: string
            minLength: 6
            maxLength: 20
            description: 密码

响应:
  200:
    description: 登录成功
    content:
      application/json:
        schema:
          type: object
          properties:
            code:
              type: integer
              example: 200
            message:
              type: string
              example: "登录成功"
            data:
              type: object
              properties:
                accessToken:
                  type: string
                  description: 访问令牌，有效期2小时
                refreshToken:
                  type: string
                  description: 刷新令牌，有效期7天
                tokenType:
                  type: string
                  example: "Bearer"
                expiresIn:
                  type: integer
                  example: 7200
                  description: 过期时间（秒）
                user:
                  $ref: '#/components/schemas/UserInfo'

  400:
    description: 请求参数错误
  401:
    description: 认证失败（用户名或密码错误）
  429:
    description: 登录过于频繁
```

#### Token 刷新

```yaml
接口: POST /api/v1/auth/refresh
标签: 认证
描述: 使用 Refresh Token 换取新的访问令牌

请求体:
  content:
    application/json:
      schema:
        type: object
        required: [refreshToken]
        properties:
          refreshToken:
            type: string
            description: 刷新令牌

响应:
  200:
    description: 刷新成功
    content:
      application/json:
        schema:
          type: object
          properties:
            code:
              type: integer
              example: 200
            message:
              type: string
              example: "刷新成功"
            data:
              type: object
              properties:
                accessToken:
                  type: string
                refreshToken:
                  type: string
                tokenType:
                  type: string
                expiresIn:
                  type: integer

  401:
    description: Refresh Token 无效或已过期
```

#### 用户登出

```yaml
接口: POST /api/v1/auth/logout
标签: 认证
描述: 用户退出登录，吊销当前令牌
安全:
  - BearerAuth: []

响应:
  200:
    description: 登出成功

  401:
    description: 未认证或 Token 已失效
```

#### 获取当前用户信息

```yaml
接口: GET /api/v1/auth/me
标签: 认证
描述: 获取当前登录用户的信息
安全:
  - BearerAuth: []

响应:
  200:
    description: 获取成功
    content:
      application/json:
        schema:
          type: object
          properties:
            code:
              type: integer
              example: 200
            data:
              $ref: '#/components/schemas/UserInfo'

  401:
    description: 未认证
```

### 4.3 数据模型

```yaml
components:
  schemas:
    UserInfo:
      type: object
      properties:
        id:
          type: integer
          description: 用户ID
        username:
          type: string
          description: 用户名
        realName:
          type: string
          description: 真实姓名
        email:
          type: string
          description: 邮箱
        roles:
          type: array
          items:
            type: string
          description: 角色列表
        permissions:
          type: array
          items:
            type: string
          description: 权限列表

  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
```

---

## 5. 前端设计

### 5.1 页面清单

| 页面 | 路径 | 权限 | 说明 |
|------|------|------|------|
| 登录页 | /login | 公开 | 用户登录入口 |

### 5.2 登录页设计

#### 页面布局

```
┌─────────────────────────────────────────────────────────────────────┐
│                           登录页 (Login)                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                                                              │   │
│  │                    OSRM 开源软件仓库                          │   │
│  │                                                              │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │                                                       │  │   │
│  │  │  用户名                                               │  │   │
│  │  │  ┌─────────────────────────────────────────────────┐  │  │   │
│  │  │  │                                                   │  │  │   │
│  │  │  └─────────────────────────────────────────────────┘  │  │   │
│  │  │                                                       │  │   │
│  │  │  密码                                                 │  │   │
│  │  │  ┌─────────────────────────────────────────────────┐  │  │   │
│  │  │  │                                           [👁️]   │  │  │   │
│  │  │  └─────────────────────────────────────────────────┘  │  │   │
│  │  │                                                       │  │   │
│  │  │  [ ] 记住我 (7天内自动登录)                           │  │   │
│  │  │                                                       │  │   │
│  │  │  ┌─────────────────────────────────────────────────┐  │  │   │
│  │  │  │                      登 录                       │  │  │   │
│  │  │  └─────────────────────────────────────────────────┘  │  │   │
│  │  │                                                       │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  │                                                              │   │
│  │              © 2026 OSRM 开源软件仓库管理系统                │   │
│  │                                                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 交互说明

| 元素 | 交互 | 说明 |
|------|------|------|
| 用户名输入框 | 失去焦点校验 | 校验长度3-20字符，格式符合要求 |
| 密码输入框 | 点击眼睛图标切换显示 | 支持明文/密文切换 |
| 记住我复选框 | 勾选后存储Refresh Token | 使用localStorage存储 |
| 登录按钮 | 点击触发登录 | 校验通过后调用登录API |
| 登录失败 | 显示错误提示 | 提示"用户名或密码错误" |

#### 状态管理

```typescript
// Pinia Store 定义
interface AuthState {
  // 状态
  accessToken: string | null;
  refreshToken: string | null;
  user: UserInfo | null;
  isAuthenticated: boolean;

  // 计算属性
  hasPermission: (permission: string) => boolean;
  hasRole: (role: string) => boolean;
}

interface AuthActions {
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
  fetchCurrentUser: () => Promise<void>;
}
```

---

## 6. 测试用例

### 6.1 登录功能测试

| 用例编号 | 测试场景 | 输入 | 预期结果 |
|----------|----------|------|----------|
| TC-001 | 正常登录 | 正确用户名密码 | 登录成功，返回Token |
| TC-002 | 密码错误 | 错误密码 | 返回401，提示用户名或密码错误 |
| TC-003 | 用户不存在 | 不存在的用户名 | 返回401，提示用户名或密码错误 |
| TC-004 | 用户被禁用 | 已禁用用户的正确密码 | 返回401，提示账户已被禁用 |
| TC-005 | 密码暴力破解 | 连续5次错误密码 | 第5次后账户锁定15分钟 |
| TC-006 | 并发会话限制 | 第4次登录 | 踢出最早会话或禁止登录 |
| TC-007 | 参数校验 | 用户名2字符 | 返回400，提示用户名长度3-20 |

### 6.2 Token 刷新测试

| 用例编号 | 测试场景 | 输入 | 预期结果 |
|----------|----------|------|----------|
| TC-008 | 正常刷新 | 有效Refresh Token | 返回新Token对 |
| TC-009 | Token轮换 | 使用已刷新的旧Token | 返回401，要求重新登录 |
| TC-010 | Token过期 | 过期Refresh Token | 返回401，要求重新登录 |
| TC-011 | 用户禁用后刷新 | 用户禁用后的Token | 返回401，要求重新登录 |

### 6.3 权限控制测试

| 用例编号 | 测试场景 | 输入 | 预期结果 |
|----------|----------|------|----------|
| TC-012 | 有权限访问 | 有package:read的Token | 访问成功 |
| TC-013 | 无权限访问 | 无package:create的Token | 返回403 |
| TC-014 | 通配符权限 | 有package:*的Token访问package:create | 访问成功 |
| TC-015 | 角色变更 | 变更角色后访问 | 新权限立即生效 |

---

## 7. 与需求文档的追溯关系

| 本文档章节 | 关联需求文档 | 追溯说明 |
|------------|--------------|----------|
| 2.1 用户登录 | REQ-001-001 | 完整实现需求 |
| 2.2 Token刷新 | REQ-001-002 | 完整实现需求 |
| 2.3 用户登出 | REQ-001-003 | 完整实现需求 |
| 2.4 权限控制 | REQ-001-006 | 完整实现需求 |

---

## 8. 依赖与风险

### 8.1 依赖项

| 依赖项 | 类型 | 说明 |
|--------|------|------|
| Redis | 基础设施 | Token黑名单、登录失败计数 |
| PostgreSQL | 数据库 | 用户数据存储 |

### 8.2 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Token被截获 | 高 | HTTPS传输、Token有效期短、支持吊销 |
| 暴力破解 | 中 | 登录限流、账户锁定机制 |
| 会话劫持 | 中 | Token绑定设备指纹（可选） |

---

## 9. 附录

### 9.1 初始化数据

```sql
-- 插入初始角色
INSERT INTO t_role (role_code, role_name, description) VALUES
('ROLE_VISITOR', '访客', '仅可浏览公开信息'),
('ROLE_DEVELOPER', '开发人员', '可浏览和订购软件'),
('ROLE_PACKAGE_MANAGER', '软件管理员', '管理软件包和审批'),
('ROLE_SYSTEM_ADMIN', '系统管理员', '全部权限');

-- 插入初始权限
INSERT INTO t_permission (permission_code, permission_name, resource_type, action) VALUES
('package:read', '软件浏览', 'package', 'read'),
('package:create', '软件录入', 'package', 'create'),
('package:update', '软件编辑', 'package', 'update'),
('package:delete', '软件删除', 'package', 'delete'),
('package:approve', '软件审批', 'package', 'approve'),
('subscription:create', '订购申请', 'subscription', 'create'),
('subscription:read', '订购查看', 'subscription', 'read'),
('subscription:approve', '订购审批', 'subscription', 'approve'),
('system:manage', '系统管理', 'system', 'manage');

-- 关联角色权限 (示例)
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER' AND p.permission_code IN ('package:read', 'subscription:create', 'subscription:read');

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO t_user (username, password, real_name, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', true);

-- 关联管理员角色
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_SYSTEM_ADMIN';
```

### 9.2 相关文档索引

| 文档 | 路径 | 说明 |
|------|------|------|
| API 规范 | `docs/standards/api-design.md` | RESTful API 设计规范 |
| 数据库规范 | `docs/standards/database.md` | 数据库设计规范 |
| 前端规范 | `docs/standards/frontend.md` | Vue 前端开发规范 |
| 测试规范 | `docs/standards/testing.md` | TDD 测试规范 |
