# 用户认证 (REQ-001)

## 文档信息
- **需求编号**: REQ-001
- **模块名称**: 用户认证
- **优先级**: P0
- **状态**: 已实现

## 1. 功能概述

用户认证提供安全的身份验证和会话管理功能，是所有功能的前置依赖。

## 2. 功能清单

### REQ-001-001 登录方式
- 用户名/密码登录
- JWT Token 认证

### REQ-001-002 密码策略
| 策略项 | 规则 |
|--------|------|
| 最小长度 | 8位 |
| 复杂度 | 必须包含字母和数字 |
| 密码过期 | 90天（可选） |
| 锁定策略 | 5次失败后锁定 |

### REQ-001-003 会话管理
| 配置项 | 规则 |
|--------|------|
| Access Token 有效期 | 2小时 |
| Refresh Token 有效期 | 7天 |
| 超时自动登出 | 30分钟无操作 |

### REQ-001-004 登出与Token吊销
- 用户主动登出
- Token 刷新机制

## 3. API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录 | POST | /api/v1/auth/login | 用户登录 |
| 登出 | POST | /api/v1/auth/logout | 用户登出 |
| 刷新Token | POST | /api/v1/auth/refresh | 刷新Token |
| 当前用户 | GET | /api/v1/auth/me | 获取当前用户信息 |

## 4. 前端页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录页 | /login | 用户登录 |
| 首页 | /home | 登录后首页 |

## 5. 数据表

- `t_user` - 用户表
- `t_role` - 角色表
- `t_permission` - 权限表
- `t_user_role` - 用户角色关联表
- `t_role_permission` - 角色权限关联表

## 6. 权限体系

### 简化后的角色（V2版本）
| 角色 | 编码 | 权限范围 |
|------|------|----------|
| 开发人员 | ROLE_DEVELOPER | package:read, subscription:*, inventory:*, business-system:read |
| 系统管理员 | ROLE_SYSTEM_ADMIN | 全部权限 |

### 原有角色（已废弃）
- ROLE_PACKAGE_MANAGER - 已废弃
- ROLE_VISITOR - 已废弃

## 7. 实现情况

| 功能 | 状态 | 说明 |
|------|------|------|
| 登录认证 | ✅ 已实现 | AuthController |
| Token管理 | ✅ 已实现 | JwtTokenProvider |
| 权限控制 | ✅ 已实现 | @PreAuthorize |
| 前端登录 | ✅ 已实现 | Login.vue |
| 简化角色 | ✅ 已实现 | V2角色重构 |
