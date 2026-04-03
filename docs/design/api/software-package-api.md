# API设计文档：软件包管理

**功能编号**: REQ-300
**文档日期**: 2026-03-20

---

## 1. 接口概览

| 接口组 | 接口数量 | 说明 |
|--------|----------|------|
| 软件包管理 | 11个 | CRUD + 状态流转 |
| 版本管理 | 7个 | 版本CRUD + 发布/下线 |

---

## 2. 软件包管理接口

### 2.1 列表查询

```
GET /api/v1/software-packages
```

**权限**: package:read

**请求参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 关键词搜索（名称、描述） |
| type | String | 否 | 软件类型：DOCKER_IMAGE/MAVEN/NPM/PYPI/GENERIC |
| status | String | 否 | 状态：DRAFT/PENDING/PUBLISHED/OFFLINE |
| categoryId | Long | 否 | 分类ID |
| sort | String | 否 | 排序：createdAt/viewCount/downloadCount |
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页大小，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "packageName": "MySQL Community Server",
        "packageKey": "mysql",
        "softwareType": "DOCKER_IMAGE",
        "softwareTypeName": "Docker镜像",
        "categoryId": 2,
        "categoryName": "数据库",
        "description": "开源关系型数据库",
        "logoUrl": "https://example.com/mysql-logo.png",
        "currentVersion": "8.0.33",
        "viewCount": 100,
        "downloadCount": 50,
        "subscriptionCount": 10,
        "status": "PUBLISHED",
        "statusName": "已发布",
        "createdBy": 1,
        "createdAt": "2026-03-20T10:00:00",
        "updatedAt": "2026-03-20T10:00:00",
        "publishedBy": 2,
        "publishedAt": "2026-03-20T12:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 1
  }
}
```

### 2.2 详情查询

```
GET /api/v1/software-packages/{id}
```

**权限**: package:read

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "packageName": "MySQL Community Server",
    "packageKey": "mysql",
    "softwareType": "DOCKER_IMAGE",
    "softwareTypeName": "Docker镜像",
    "categoryId": 2,
    "categoryName": "数据库",
    "description": "开源关系型数据库",
    "websiteUrl": "https://www.mysql.com",
    "licenseType": "GPL-2.0",
    "licenseUrl": "https://www.gnu.org/licenses/gpl-2.0.html",
    "sourceUrl": "https://github.com/mysql/mysql-server",
    "logoUrl": "https://example.com/mysql-logo.png",
    "currentVersion": "8.0.33",
    "viewCount": 100,
    "downloadCount": 50,
    "subscriptionCount": 10,
    "status": "PUBLISHED",
    "statusName": "已发布",
    "createdBy": 1,
    "createdAt": "2026-03-20T10:00:00",
    "updatedAt": "2026-03-20T10:00:00",
    "publishedBy": 2,
    "publishedAt": "2026-03-20T12:00:00"
  }
}
```

### 2.3 创建软件包

```
POST /api/v1/software-packages
```

**权限**: package:create

**请求体**:
```json
{
  "packageName": "MySQL Community Server",
  "packageKey": "mysql",
  "softwareType": "DOCKER_IMAGE",
  "categoryId": 2,
  "description": "开源关系型数据库",
  "websiteUrl": "https://www.mysql.com",
  "licenseType": "GPL-2.0",
  "licenseUrl": "https://www.gnu.org/licenses/gpl-2.0.html",
  "sourceUrl": "https://github.com/mysql/mysql-server"
}
```

**字段校验**:

| 字段 | 规则 |
|------|------|
| packageName | 必填，2-128字符，全局唯一 |
| packageKey | 必填，正则`^[a-z0-9_-]+$`，2-64字符，全局唯一 |
| softwareType | 必填，枚举值 |
| description | 可选，最大5000字符 |

**响应**: 返回创建的软件包详情

### 2.4 更新软件包

```
PUT /api/v1/software-packages/{id}
```

**权限**: package:update

**约束**: 仅 DRAFT 状态可编辑基础信息

**请求体**:
```json
{
  "packageName": "MySQL",
  "categoryId": 2,
  "description": "更新后的描述",
  "websiteUrl": "https://www.mysql.com",
  "licenseType": "GPL-2.0",
  "licenseUrl": "https://www.gnu.org/licenses/gpl-2.0.html",
  "sourceUrl": "https://github.com/mysql/mysql-server"
}
```

**响应**: 返回更新后的软件包详情

### 2.5 删除软件包

```
DELETE /api/v1/software-packages/{id}
```

**权限**: package:delete

**约束**: 仅 DRAFT 状态可删除

**响应**:
```json
{
  "code": 200,
  "message": "success"
}
```

### 2.6 提交审核

```
POST /api/v1/software-packages/{id}/submit
```

**权限**: package:update

**约束**:
- 仅 DRAFT 状态可提交
- 必须至少有一个版本

**响应**: 返回更新后的软件包详情

### 2.7 审批通过

```
POST /api/v1/software-packages/{id}/approve
```

**权限**: package:approve

**约束**: 仅 PENDING 状态可审批

**响应**: 返回更新后的软件包详情

### 2.8 审批驳回

```
POST /api/v1/software-packages/{id}/reject
```

**权限**: package:approve

**约束**: 仅 PENDING 状态可驳回

**请求体**:
```json
{
  "reason": "版本号格式不正确，请修正后重新提交"
}
```

**响应**: 返回更新后的软件包详情

### 2.9 下架

```
POST /api/v1/software-packages/{id}/offline
```

**权限**: package:approve

**约束**: 仅 PUBLISHED 状态可下架

**请求体**:
```json
{
  "reason": "发现安全漏洞，暂停使用"
}
```

**响应**: 返回更新后的软件包详情

### 2.10 重新上架

```
POST /api/v1/software-packages/{id}/republish
```

**权限**: package:approve

**约束**: 仅 OFFLINE 状态可重新上架

**响应**: 返回更新后的软件包详情

### 2.11 获取软件类型列表

```
GET /api/v1/software-packages/types
```

**权限**: package:read

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "code": "DOCKER_IMAGE", "name": "Docker镜像", "storageType": "HARBOR" },
    { "code": "HELM_CHART", "name": "Helm Chart", "storageType": "HARBOR" },
    { "code": "MAVEN", "name": "Maven组件", "storageType": "NEXUS" },
    { "code": "NPM", "name": "NPM包", "storageType": "NEXUS" },
    { "code": "PYPI", "name": "PyPI包", "storageType": "NEXUS" },
    { "code": "GENERIC", "name": "通用文件", "storageType": "NAS" }
  ]
}
```

---

## 3. 版本管理接口

### 3.1 版本列表

```
GET /api/v1/software-packages/{id}/versions
```

**权限**: package:read

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "versionNo": "8.0.33",
      "status": "PUBLISHED",
      "storageBackendId": 1,
      "storageBackendName": "生产Harbor",
      "storagePath": "library/mysql:8.0.33",
      "artifactUrl": "https://harbor.example.com/v2/library/mysql/manifests/8.0.33",
      "releaseNotes": "修复了多个安全漏洞",
      "fileSize": 0,
      "isLatest": true,
      "publishedBy": 2,
      "publishedAt": "2026-03-20T12:00:00",
      "createdAt": "2026-03-20T10:00:00"
    }
  ]
}
```

### 3.2 创建版本

```
POST /api/v1/software-packages/{id}/versions
```

**权限**: package:create

**请求体**:
```json
{
  "versionNo": "8.0.34",
  "storageBackendId": 1,
  "storagePath": "library/mysql:8.0.34",
  "artifactUrl": "https://harbor.example.com/v2/library/mysql/manifests/8.0.34",
  "releaseNotes": "新特性：性能提升20%",
  "fileSize": 0,
  "checksum": "sha256:abcdef123456"
}
```

**字段校验**:

| 字段 | 规则 |
|------|------|
| versionNo | 必填，符合SemVer规范，同一软件包内唯一 |
| storageBackendId | 必填，必须存在且启用的存储后端 |
| storagePath | 可选，存储路径 |
| artifactUrl | 可选，制品URL |

**响应**: 返回创建的版本详情

### 3.3 版本详情

```
GET /api/v1/software-packages/{id}/versions/{vid}
```

**权限**: package:read

**响应**: 返回版本详情

### 3.4 更新版本

```
PUT /api/v1/software-packages/{id}/versions/{vid}
```

**权限**: package:update

**约束**: 仅 DRAFT 状态版本可编辑

**请求体**:
```json
{
  "releaseNotes": "更新后的发行说明",
  "storagePath": "library/mysql:8.0.34",
  "artifactUrl": "https://harbor.example.com/v2/library/mysql/manifests/8.0.34"
}
```

**响应**: 返回更新后的版本详情

### 3.5 删除版本

```
DELETE /api/v1/software-packages/{id}/versions/{vid}
```

**权限**: package:delete

**约束**: 仅 DRAFT 状态版本可删除

**响应**:
```json
{
  "code": 200,
  "message": "success"
}
```

### 3.6 发布版本

```
POST /api/v1/software-packages/{id}/versions/{vid}/publish
```

**权限**: package:update

**约束**:
- 仅 DRAFT 状态版本可发布
- 软件包状态必须为 PUBLISHED

**响应**: 返回更新后的版本详情

### 3.7 下线版本

```
POST /api/v1/software-packages/{id}/versions/{vid}/offline
```

**权限**: package:update

**约束**: 仅 PUBLISHED 状态版本可下线

**响应**: 返回更新后的版本详情

---

## 4. 错误码

| 错误码 | 说明 | HTTP状态 |
|--------|------|----------|
| 400 | 请求参数错误 | 400 |
| 400001 | package_key格式不正确 | 400 |
| 400002 | package_key已存在 | 400 |
| 400003 | package_name已存在 | 400 |
| 400004 | 版本号格式不正确 | 400 |
| 400005 | 版本号已存在 | 400 |
| 400006 | 状态流转不允许 | 400 |
| 404 | 资源不存在 | 404 |
| 404001 | 软件包不存在 | 404 |
| 404002 | 版本不存在 | 404 |
| 409 | 资源冲突 | 409 |
| 409001 | 软件包当前状态不允许此操作 | 409 |
| 403 | 权限不足 | 403 |

---

## 5. 状态机流转

### 5.1 软件包状态流转

```
DRAFT → PENDING: 提交审核 (submit)
PENDING → PUBLISHED: 审批通过 (approve)
PENDING → DRAFT: 审批驳回 (reject)
PUBLISHED → OFFLINE: 下架 (offline)
OFFLINE → PUBLISHED: 重新上架 (republish)
```

### 5.2 版本状态流转

```
DRAFT → PUBLISHED: 发布版本 (publish) [软件包必须已发布]
PUBLISHED → OFFLINE: 下线版本 (offline)
```

---

**文档版本**: v1.0
**最后更新**: 2026-03-20
