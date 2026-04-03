# 存储后端管理 API 设计

## 基础路径

`/api/v1/storage-backends`

## 接口清单

### 1. 列表查询

**GET** `/api/v1/storage-backends`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 否 | 名称模糊搜索 |
| type | String | 否 | 类型筛选：HARBOR/NEXUS/NAS |
| status | String | 否 | 状态筛选：ONLINE/OFFLINE/ERROR |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 页大小，默认10 |

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "backendCode": "harbor-001",
        "backendName": "Harbor主仓库",
        "backendType": "HARBOR",
        "endpoint": "114.66.38.81:8080",
        "namespace": "osrm",
        "healthStatus": "ONLINE",
        "lastHealthCheck": "2026-03-19T10:30:00",
        "enabled": true,
        "isDefault": true,
        "description": "主Harbor仓库",
        "createdAt": "2026-03-19T08:00:00",
        "updatedAt": "2026-03-19T10:30:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "size": 10,
    "number": 1
  }
}
```

### 2. 详情查询

**GET** `/api/v1/storage-backends/{id}`

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "backendCode": "harbor-001",
    "backendName": "Harbor主仓库",
    "backendType": "HARBOR",
    "endpoint": "114.66.38.81:8080",
    "accessKey": "admin",
    "secretKey": null,
    "namespace": "osrm",
    "config": {
      "protocol": "HTTP",
      "apiVersion": "v2.0",
      "project": "osrm"
    },
    "healthStatus": "ONLINE",
    "lastHealthCheck": "2026-03-19T10:30:00",
    "errorMessage": null,
    "enabled": true,
    "isDefault": true,
    "description": "主Harbor仓库",
    "createdBy": 1,
    "createdAt": "2026-03-19T08:00:00",
    "updatedAt": "2026-03-19T10:30:00"
  }
}
```

**注意**：secretKey 在响应中返回 null，不暴露明文密码

### 3. 新增存储后端

**POST** `/api/v1/storage-backends`

#### 请求体

```json
{
  "backendCode": "harbor-001",
  "backendName": "Harbor主仓库",
  "backendType": "HARBOR",
  "endpoint": "114.66.38.81:8080",
  "accessKey": "admin",
  "secretKey": "Harbor12345",
  "namespace": "osrm",
  "config": {
    "protocol": "HTTP",
    "apiVersion": "v2.0",
    "project": "osrm"
  },
  "isDefault": false,
  "enabled": true,
  "description": "主Harbor仓库"
}
```

#### 字段验证规则

- backendCode: 必填，唯一，长度1-32，只允许字母、数字、中划线、下划线
- backendName: 必填，唯一，长度1-64
- backendType: 必填，只能是 HARBOR/NEXUS/NAS
- endpoint: 必填，长度1-256
- accessKey: Harbor/Nexus 必填
- secretKey: Harbor/Nexus 必填

### 4. 编辑存储后端

**PUT** `/api/v1/storage-backends/{id}`

#### 请求体

```json
{
  "backendName": "Harbor主仓库-新名称",
  "endpoint": "114.66.38.81:8080",
  "accessKey": "admin",
  "secretKey": "Harbor12345",
  "namespace": "osrm",
  "config": {
    "protocol": "HTTP",
    "apiVersion": "v2.0",
    "project": "osrm"
  },
  "isDefault": true,
  "enabled": true,
  "description": "更新描述"
}
```

**注意**：backendCode 和 backendType 不可修改

### 5. 删除存储后端

**DELETE** `/api/v1/storage-backends/{id}`

#### 业务规则

- 有关联软件包时禁止删除，返回 409 Conflict
- 是默认后端时禁止删除，需先设置其他后端为默认

### 6. 连接测试

**POST** `/api/v1/storage-backends/test-connection`

#### 请求体

```json
{
  "backendType": "HARBOR",
  "endpoint": "114.66.38.81:8080",
  "accessKey": "admin",
  "secretKey": "Harbor12345",
  "config": {
    "protocol": "HTTP"
  }
}
```

#### 响应示例（成功）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "message": "连接成功",
    "serverInfo": {
      "version": "v2.12.2",
      "registryUrl": "hb.harbor.sdit.site:8080"
    }
  }
}
```

#### 响应示例（失败）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": false,
    "message": "连接失败：认证信息错误",
    "serverInfo": null
  }
}
```

### 7. 健康检查

**POST** `/api/v1/storage-backends/{id}/health`

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "healthStatus": "ONLINE",
    "lastHealthCheck": "2026-03-19T10:30:00",
    "errorMessage": null,
    "responseTimeMs": 150
  }
}
```

### 8. 获取类型枚举

**GET** `/api/v1/storage-backends/types`

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "code": "HARBOR", "name": "Harbor仓库" },
    { "code": "NEXUS", "name": "Nexus仓库" },
    { "code": "NAS", "name": "NAS/本地存储" }
  ]
}
```

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 存储后端不存在 |
| 409 | 资源冲突（名称已存在、有关联软件包等） |
| 500 | 服务器内部错误 |
