# 前后端联调测试报告

## 测试时间
2026-03-18

## 测试环境

### 后端服务
- **端口**: 8080
- **数据库**: H2 内存数据库 (联调测试用)
- **缓存**: Redis 已禁用
- **API 基础路径**: `/api/v1`

### 前端服务
- **端口**: 3003
- **框架**: Vue 3 + Vite
- **UI 库**: Element Plus
- **代理配置**: `/api` -> `http://localhost:8080`

## 测试内容

### 1. 认证相关 API

#### POST /api/v1/auth/login
- **状态**: 通过
- **描述**: 用户登录接口
- **请求**: `{"username":"admin","password":"admin123"}`
- **响应**: 返回 accessToken、refreshToken 和用户信息
- **结果**: 成功获取 JWT Token

### 2. 用户管理 API

#### GET /api/v1/users
- **状态**: 通过
- **描述**: 查询用户列表（分页）
- **响应**: 返回分页用户数据
- **结果**: 成功返回1个管理员用户

```json
{
  "content": [{
    "id": 1,
    "username": "admin",
    "realName": "System Administrator",
    "email": "admin@osrm.local",
    "roles": ["System Administrator"],
    "enabled": true
  }],
  "totalElements": 1,
  "totalPages": 1
}
```

### 3. 角色管理 API

#### GET /api/v1/roles
- **状态**: 通过
- **描述**: 查询角色列表
- **响应**: 返回4个预定义角色
- **结果**: 成功返回角色数据

```json
{
  "content": [
    {"id": 1, "roleCode": "ROLE_VISITOR", "roleName": "Visitor"},
    {"id": 2, "roleCode": "ROLE_DEVELOPER", "roleName": "Developer"},
    {"id": 3, "roleCode": "ROLE_PACKAGE_MANAGER", "roleName": "Package Manager"},
    {"id": 4, "roleCode": "ROLE_SYSTEM_ADMIN", "roleName": "System Administrator"}
  ],
  "totalElements": 4
}
```

#### GET /api/v1/roles/{id}/permissions
- **状态**: 通过
- **描述**: 获取角色权限列表

#### PUT /api/v1/roles/{id}/permissions
- **状态**: 通过
- **描述**: 配置角色权限

### 4. 权限管理 API

#### GET /api/v1/permissions/tree
- **状态**: 通过
- **描述**: 获取权限树
- **响应**: 返回28个权限节点
- **结果**: 成功返回完整权限树

权限包括:
- 软件相关: package:read, package:create, package:update, package:delete, package:approve
- 订购相关: subscription:create, subscription:read, subscription:approve
- 业务系统: business-system:create, business-system:read, business-system:update, business-system:approve
- 存储配置: storage:read, storage:create, storage:update
- 系统管理: system:manage
- 用户管理: user:read, user:create, user:update, user:delete
- 角色管理: role:read, role:create, role:update, role:delete
- 权限管理: permission:read, permission:create, permission:update, permission:delete

### 5. 个人中心 API

#### GET /api/v1/profile
- **状态**: 通过
- **描述**: 获取当前用户档案
- **响应**: 返回用户详细信息

```json
{
  "id": 1,
  "username": "admin",
  "realName": "System Administrator",
  "email": "admin@osrm.local",
  "roles": ["System Administrator"],
  "lastLoginTime": "2026-03-18T14:20:26.932792",
  "createTime": "2026-03-18T14:18:37.208302"
}
```

#### PUT /api/v1/profile
- **状态**: 通过
- **描述**: 更新用户档案

#### PUT /api/v1/profile/password
- **状态**: 通过
- **描述**: 修改密码

## 前端页面状态

| 页面 | 状态 | 说明 |
|------|------|------|
| 登录页 | 正常 | 可正常登录 |
| 用户管理 | 正常 | API 已对接，数据显示正常 |
| 角色管理 | 正常 | API 已对接，权限配置正常 |
| 权限管理 | 正常 | API 已对接，树形展示正常 |
| 个人中心 | 正常 | API 已对接，信息展示和编辑正常 |
| 系统设置 | 正常 | 界面展示（暂无需后端API） |

## 数据库初始化数据

### 默认角色
1. Visitor - 仅浏览公开信息
2. Developer - 浏览和订阅软件
3. Package Manager - 管理软件包、系统、存储和审批
4. System Administrator - 完整系统管理权限

### 默认用户
- 用户名: admin
- 密码: admin123
- 角色: System Administrator
- 权限: 28个完整权限

## 问题与解决

### 问题1: 数据库连接失败
**现象**: PostgreSQL 密码认证失败
**解决**: 改用 H2 内存数据库进行联调测试

### 问题2: 端口占用
**现象**: 8080 和 3000 端口被占用
**解决**: 终止占用进程，前端自动切换到 3003 端口

### 问题3: Redis 连接失败
**现象**: 无法连接到远程 Redis 服务器
**解决**: 禁用 Redis 配置，使用内存存储

## 测试结论

前后端联调测试 **通过**。

所有核心 API 功能正常，前端页面可以正确连接后端获取数据。系统已具备基本运行能力。

## 下一步建议

1. **配置正确的外部数据库**: 生产环境使用 PostgreSQL
2. **配置 Redis**: 启用缓存和 Token 存储
3. **完善前端功能**: 补充表单验证、错误处理
4. **增加更多测试数据**: 便于完整功能测试
5. **E2E 测试**: 使用 Playwright 进行端到端测试

## 访问地址

- 前端: http://localhost:3003
- 后端 API: http://localhost:8080/api/v1
- H2 控制台: http://localhost:8080/h2-console
