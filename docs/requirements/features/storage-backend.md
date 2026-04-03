# 存储后端管理功能需求文档

## 功能概述

统一管理 Harbor、Nexus、NAS/本地存储等存储后端，为开源软件制品提供存储基础设施。

## 需求编号

REQ-100 ~ REQ-199

## 功能目标

1. 纳管 Harbor、Nexus、NAS/本地存储等多种存储后端
2. 提供连接测试和健康检查能力
3. 支持存储后端的状态监控
4. 为软件包管理提供存储目标选择

## 详细需求

### REQ-100: 存储后端基础信息

#### 字段定义

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 主键，自增 |
| name | String(64) | 是 | 存储后端显示名称，唯一 |
| type | Enum | 是 | 类型：HARBOR/NEXUS/NAS |
| status | Enum | 是 | 状态：ONLINE/OFFLINE/ERROR |
| description | String(255) | 否 | 描述说明 |
| config | JSON | 是 | 类型特定的配置信息 |
| lastCheckTime | DateTime | 否 | 最后健康检查时间 |
| errorMessage | String(500) | 否 | 错误信息（离线时） |
| createdAt | DateTime | 是 | 创建时间 |
| updatedAt | DateTime | 是 | 更新时间 |
| createdBy | String(64) | 是 | 创建人 |

### REQ-101: Harbor 存储配置

#### 配置字段

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| host | String(255) | 是 | Harbor 地址，如 114.66.38.81:8080 |
| protocol | Enum | 是 | HTTP/HTTPS，默认 HTTPS |
| username | String(64) | 是 | 认证用户名 |
| password | String(255) | 是 | 认证密码（加密存储） |
| project | String(64) | 否 | 默认项目/组织名称 |
| apiVersion | String(16) | 否 | API 版本，默认 v2.0 |

#### 连接测试验证项

1. API 连通性（/api/v2.0/systeminfo）
2. 认证有效性
3. 指定项目是否存在（如果配置了）

### REQ-102: Nexus 存储配置

#### 配置字段

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| host | String(255) | 是 | Nexus 地址，如 114.66.38.81:8081 |
| protocol | Enum | 是 | HTTP/HTTPS，默认 HTTPS |
| username | String(64) | 是 | 认证用户名 |
| password | String(255) | 是 | 认证密码（加密存储） |
| mavenRepo | String(64) | 否 | Maven hosted 仓库名称 |
| npmRepo | String(64) | 否 | NPM hosted 仓库名称 |
| pypiRepo | String(64) | 否 | PyPI hosted 仓库名称 |
| rawRepo | String(64) | 否 | Raw hosted 仓库名称 |

#### 连接测试验证项

1. API 连通性（/service/rest/v1/status/check）
2. 认证有效性
3. 各类型仓库是否存在（如果配置了）

### REQ-103: NAS/本地存储配置

#### 配置字段

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| path | String(500) | 是 | 挂载点或本地路径 |
| capacityLimit | Long | 否 | 容量限制（MB） |
| currentUsage | Long | 否 | 当前使用量（MB） |

#### 连接测试验证项

1. 路径可访问性
2. 读写权限
3. 剩余空间检查

### REQ-104: 连接测试功能

#### 功能描述

- 新增/编辑存储后端时，可执行实时连接测试
- 测试失败返回具体错误信息
- 测试通过返回存储后端基本信息

### REQ-105: 健康检查机制

#### 自动健康检测

- 检测周期：每5分钟
- 检测内容：API 连通性 + 认证有效性
- 状态更新：ONLINE / OFFLINE / ERROR

#### 健康状态定义

| 状态 | 说明 | 处理建议 |
|------|------|----------|
| ONLINE | 连接正常 | 可正常使用 |
| OFFLINE | 连接失败 | 检查网络或配置 |
| ERROR | 连接成功但异常 | 查看错误信息 |

### REQ-106: 存储后端列表查询

#### 查询条件

- 名称模糊搜索
- 类型筛选（HARBOR/NEXUS/NAS）
- 状态筛选

### REQ-107: 存储后端删除约束

- 有关联软件包时禁止删除
- 删除需二次确认

## 接口清单

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/storage-backends | GET | 列表查询 |
| /api/v1/storage-backends/{id} | GET | 详情查询 |
| /api/v1/storage-backends | POST | 新增存储后端 |
| /api/v1/storage-backends/{id} | PUT | 编辑存储后端 |
| /api/v1/storage-backends/{id} | DELETE | 删除存储后端 |
| /api/v1/storage-backends/test-connection | POST | 连接测试 |
| /api/v1/storage-backends/{id}/health | GET | 健康检查 |

## 验收标准

- [ ] 支持 Harbor 存储纳管，能成功连接并获取系统信息
- [ ] 支持 Nexus 存储纳管，能成功连接并获取状态
- [ ] 连接测试功能正常，能正确返回成功/失败结果
- [ ] 健康检查任务每5分钟执行，状态更新准确
- [ ] 存储后端列表支持分页、搜索、筛选
- [ ] 删除时有关联软件包给出明确提示
- [ ] 密码加密存储，不在接口中返回明文
