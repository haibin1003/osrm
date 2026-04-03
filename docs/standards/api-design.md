# RESTful API 设计规范

本规范定义 OSRM 项目的 RESTful API 设计标准，包括 URL 命名、HTTP 方法使用、响应格式和错误处理。

---n
## 文档信息

- **作者**: OSRM 开发团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-17
- **维护责任人**: 技术负责人

---

## 基本原则

1. **资源导向**：API 应该围绕资源设计，而非动作
2. **统一接口**：使用标准的 HTTP 方法和状态码
3. **无状态**：每个请求应该包含所有必要信息
4. **可缓存**：适当使用缓存头

---

## URL 命名规范

### 基本规则

| 规则 | 正确 | 错误 |
|------|------|------|
| 使用小写字母 | `/api/packages` | `/api/Packages` |
| 使用连字符分隔 | `/api/software-packages` | `/api/software_packages` |
| 使用名词复数 | `/api/users` | `/api/user` |
| 不使用动词 | `GET /api/packages` | `GET /api/getPackages` |
| 不使用文件扩展名 | `/api/packages` | `/api/packages.json` |

### URL 结构

```
/api/{version}/{resource}/{id}/{sub-resource}
```

**示例**：
```
/api/v1/packages                    # 软件包列表
/api/v1/packages/123                # 特定软件包
/api/v1/packages/123/versions       # 软件包的版本列表
/api/v1/packages/123/versions/1.0.0 # 特定版本
```

### 嵌套资源

对于关联资源，使用嵌套路径：

```
/api/v1/users/123/orders            # 用户的订单
/api/v1/projects/456/members        # 项目成员
```

---

## HTTP 方法使用

### 方法对照表

| 方法 | 用途 | 幂等性 | 示例 |
|------|------|--------|------|
| **GET** | 获取资源 | 是 | `GET /api/v1/packages` |
| **POST** | 创建资源 | 否 | `POST /api/v1/packages` |
| **PUT** | 完整更新资源 | 是 | `PUT /api/v1/packages/123` |
| **PATCH** | 部分更新资源 | 否 | `PATCH /api/v1/packages/123` |
| **DELETE** | 删除资源 | 是 | `DELETE /api/v1/packages/123` |

### 详细说明

#### GET - 获取资源

```
GET /api/v1/packages              # 获取列表
GET /api/v1/packages/123          # 获取单个资源
GET /api/v1/packages?status=active # 条件查询
```

#### POST - 创建资源

```
POST /api/v1/packages
Content-Type: application/json

{
  "name": "nginx",
  "version": "1.20.0"
}
```

**响应**：201 Created，返回创建的资源

#### PUT - 完整更新

```
PUT /api/v1/packages/123
Content-Type: application/json

{
  "name": "nginx",
  "version": "1.21.0",
  "status": "active"
}
```

**注意**：PUT 要求提供资源的完整表示

#### PATCH - 部分更新

```
PATCH /api/v1/packages/123
Content-Type: application/json

{
  "status": "archived"
}
```

**注意**：PATCH 只更新提供的字段

#### DELETE - 删除资源

```
DELETE /api/v1/packages/123
```

**响应**：204 No Content（成功删除）

---

## 统一响应格式

### 成功响应

#### 单个资源

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "name": "nginx",
    "version": "1.20.0",
    "createdAt": "2026-03-17T10:00:00Z"
  }
}
```

#### 资源列表

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "nginx"
      },
      {
        "id": 2,
        "name": "redis"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "totalPages": 5
    }
  }
}
```

#### 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码（从1开始） |
| size | int | 20 | 每页大小（最大100） |
| sort | string | id,desc | 排序字段和方向 |

**示例**：
```
GET /api/v1/packages?page=1&size=10&sort=createdAt,desc
```

### 错误响应

```json
{
  "code": 400,
  "message": "请求参数错误",
  "errors": [
    {
      "field": "name",
      "message": "名称不能为空"
    },
    {
      "field": "version",
      "message": "版本号格式不正确"
    }
  ]
}
```

---

## HTTP 状态码

### 成功状态码

| 状态码 | 用途 | 说明 |
|--------|------|------|
| 200 OK | 请求成功 | 用于 GET、PUT、PATCH |
| 201 Created | 创建成功 | 用于 POST 创建资源 |
| 204 No Content | 无返回内容 | 用于 DELETE 成功 |

### 客户端错误状态码

| 状态码 | 用途 | 说明 |
|--------|------|------|
| 400 Bad Request | 请求参数错误 | 参数校验失败 |
| 401 Unauthorized | 未认证 | 缺少或无效的认证信息 |
| 403 Forbidden | 无权限 | 已认证但无权访问 |
| 404 Not Found | 资源不存在 | 请求的资源不存在 |
| 409 Conflict | 资源冲突 | 如重复创建已存在的资源 |
| 422 Unprocessable Entity | 业务逻辑错误 | 请求语义正确但无法执行 |
| 429 Too Many Requests | 请求过于频繁 | 限流触发 |

### 服务器错误状态码

| 状态码 | 用途 | 说明 |
|--------|------|------|
| 500 Internal Server Error | 服务器内部错误 | 未知错误 |
| 503 Service Unavailable | 服务不可用 | 如服务维护中 |

---

## 错误码定义

### 错误码格式

```
{HTTP状态码}{模块代码}{错误序号}
```

**模块代码**：
| 模块 | 代码 |
|------|------|
| 通用 | 00 |
| 用户认证 | 01 |
| 软件包管理 | 02 |
| 审核流程 | 03 |
| 订购管理 | 04 |
| 系统管理 | 05 |

### 错误码列表

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 40000001 | 请求参数错误 | 400 |
| 40000002 | JSON解析错误 | 400 |
| 40001001 | 用户名已存在 | 400 |
| 40002001 | 软件包名称不能为空 | 400 |
| 40002002 | 版本号格式不正确 | 400 |
| 40101001 | 用户名或密码错误 | 401 |
| 40101002 | Token已过期 | 401 |
| 40300001 | 无权限访问 | 403 |
| 40402001 | 软件包不存在 | 404 |
| 40902001 | 软件包已存在 | 409 |
| 50000001 | 服务器内部错误 | 500 |

---

## API 版本控制

### 版本策略

使用 URL 路径进行版本控制：

```
/api/v1/packages
/api/v2/packages
```

### 版本升级原则

1. **向后兼容的变更**：不需要升级版本号
   - 添加新的可选参数
   - 添加新的响应字段
   - 添加新的 API 端点

2. **不兼容的变更**：需要升级版本号
   - 删除或重命名字段
   - 修改字段类型
   - 修改 API 行为
   - 删除 API 端点

### 版本弃用

```
GET /api/v1/packages
Deprecation: true
Sunset: Sat, 01 Jan 2027 00:00:00 GMT
```

---

## 认证与安全

### 认证方式

使用 JWT (JSON Web Token) 进行认证：

```
Authorization: Bearer {jwt_token}
```

### 安全要求

1. **HTTPS**：所有 API 必须使用 HTTPS
2. **输入验证**：所有输入参数必须经过验证
3. **防 SQL 注入**：使用参数化查询
4. **防 XSS**：对输出进行编码
5. **速率限制**：每个 IP/用户限制请求频率

---

## 请求/响应示例

### 完整的 CRUD 示例

#### 创建资源

**请求**：
```http
POST /api/v1/packages HTTP/1.1
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "nginx",
  "version": "1.20.0",
  "description": "Web server",
  "sourceUrl": "https://nginx.org"
}
```

**响应**：
```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/v1/packages/123

{
  "code": 201,
  "message": "创建成功",
  "data": {
    "id": 123,
    "name": "nginx",
    "version": "1.20.0",
    "description": "Web server",
    "sourceUrl": "https://nginx.org",
    "status": "PENDING_REVIEW",
    "createdAt": "2026-03-17T10:00:00Z",
    "createdBy": "user123"
  }
}
```

#### 查询资源列表

**请求**：
```http
GET /api/v1/packages?status=ACTIVE&page=1&size=10 HTTP/1.1
Authorization: Bearer {token}
```

**响应**：
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "nginx",
        "version": "1.20.0",
        "status": "ACTIVE"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 10,
      "total": 50,
      "totalPages": 5
    }
  }
}
```

---

## 变更记录

| 时间 | 变更内容 | 变更原因 | 影响范围 | 处理人 | 状态 |
|------|----------|----------|----------|--------|------|
| 2026-03-17 | 初始版本 | 建立 API 规范 | 全部 | 技术负责人 | 已完成 |

