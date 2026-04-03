# 用户中心API设计文档

## 概述

本文档定义用户中心模块的RESTful API规范。

---

## API列表

### 用户管理

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/v1/users | 查询用户列表 | user:read |
| POST | /api/v1/users | 新增用户 | user:create |
| PUT | /api/v1/users/{id} | 编辑用户 | user:update |
| DELETE | /api/v1/users/{id} | 删除用户 | user:delete |
| PUT | /api/v1/users/{id}/password | 重置密码 | user:update |
| PUT | /api/v1/users/{id}/status | 切换状态 | user:update |

### 角色管理

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/v1/roles | 查询角色列表 | role:read |
| POST | /api/v1/roles | 新增角色 | role:create |
| PUT | /api/v1/roles/{id} | 编辑角色 | role:update |
| DELETE | /api/v1/roles/{id} | 删除角色 | role:delete |
| GET | /api/v1/roles/{id}/permissions | 获取角色权限 | role:read |
| PUT | /api/v1/roles/{id}/permissions | 配置权限 | role:update |

### 权限管理

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/v1/permissions | 查询权限列表 | permission:read |
| GET | /api/v1/permissions/tree | 获取权限树 | permission:read |
| POST | /api/v1/permissions | 新增权限 | permission:create |
| PUT | /api/v1/permissions/{id} | 编辑权限 | permission:update |
| DELETE | /api/v1/permissions/{id} | 删除权限 | permission:delete |

### 个人中心

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/v1/profile | 获取个人信息 | 登录用户 |
| PUT | /api/v1/profile | 更新个人信息 | 登录用户 |
| PUT | /api/v1/profile/password | 修改密码 | 登录用户 |

### 系统设置

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | /api/v1/settings | 获取所有设置 | system:read |
| GET | /api/v1/settings/{category} | 获取分类设置 | system:read |
| PUT | /api/v1/settings | 更新设置 | system:update |

---

## DTO定义

### UserDTO
```java
public class UserDTO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private List<String> roles;
    private Boolean enabled;
    private String lastLoginTime;
}
```

### CreateUserRequest
```java
public class CreateUserRequest {
    @NotBlank @Size(min=3, max=20)
    private String username;
    @NotBlank
    private String realName;
    @Email
    private String email;
    @Pattern(regexp="^1[3-9]\\d{9}$")
    private String phone;
    @NotBlank @Size(min=6)
    private String password;
    @NotEmpty
    private List<Long> roleIds;
    @NotNull
    private Boolean enabled;
}
```

### RoleDTO
```java
public class RoleDTO {
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer permissionCount;
    private String createTime;
}
```

### PermissionDTO
```java
public class PermissionDTO {
    private Long id;
    private Long parentId;
    private String permissionCode;
    private String permissionName;
    private String resourceType;
    private String action;
    private String path;
    private String icon;
    private Integer sortOrder;
    private List<PermissionDTO> children;
}
```

---

## 错误码

| 错误码 | 描述 |
|--------|------|
| 1001 | 用户不存在 |
| 1002 | 用户名已存在 |
| 1003 | 不能删除当前登录用户 |
| 1004 | 原密码错误 |
| 2001 | 角色不存在 |
| 2002 | 角色编码已存在 |
| 2003 | 角色已被使用，不能删除 |
| 3001 | 权限不存在 |
| 3002 | 权限编码已存在 |
| 3003 | 有子权限，不能删除 |
