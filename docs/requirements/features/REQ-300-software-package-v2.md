# 开源软件管理 (REQ-300~350)

## 文档信息
- **需求编号**: REQ-300 ~ REQ-350
- **模块名称**: 开源软件管理
- **优先级**: P0
- **状态**: 已实现

## 1. 功能概述

开源软件管理实现软件包的录入、版本管理、发布审核全流程管理。

## 2. 功能清单

### REQ-300 软件包录入
- 创建软件包基本信息
- 选择软件类型（Docker/Maven/NPM/PyPI/Helm/通用）
- 关联分类和标签
- 填写软件描述、官网、许可证信息

### REQ-301 版本管理
- 为软件包添加新版本
- 上传制品文件（连接存储后端）
- 填写版本发布说明
- 设置版本状态（草稿/已发布/已下线）

### REQ-302 软件包审核发布
- 提交发布申请
- 系统管理员审批
- 审批通过后状态变为"已发布"

### REQ-303 软件包下架
- 已发布软件可以申请下架
- 下架后不再展示但保留历史数据

### REQ-304 分类管理
- 创建软件分类（树形结构）
- 支持多级分类
- 分类排序

### REQ-305 标签管理
- 创建软件标签
- 为软件包打标签
- 按标签筛选

## 3. API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 软件包列表 | GET | /api/v1/packages | 软件包列表 |
| 软件包详情 | GET | /api/v1/packages/{id} | 软件包详情 |
| 创建软件包 | POST | /api/v1/packages | 创建软件包 |
| 更新软件包 | PUT | /api/v1/packages/{id} | 更新软件包 |
| 删除软件包 | DELETE | /api/v1/packages/{id} | 删除软件包 |
| 版本列表 | GET | /api/v1/packages/{id}/versions | 版本列表 |
| 添加版本 | POST | /api/v1/packages/{id}/versions | 添加版本 |
| 版本详情 | GET | /api/v1/versions/{id} | 版本详情 |
| 更新版本 | PUT | /api/v1/versions/{id} | 更新版本 |
| 发布申请 | POST | /api/v1/packages/{id}/publish | 提交发布申请 |
| 审批发布 | POST | /api/v1/packages/{id}/approve | 审批通过 |
| 下架申请 | POST | /api/v1/packages/{id}/unpublish | 申请下架 |
| 分类列表 | GET | /api/v1/categories | 分类列表 |
| 创建分类 | POST | /api/v1/categories | 创建分类 |
| 标签列表 | GET | /api/v1/tags | 标签列表 |
| 创建标签 | POST | /api/v1/tags | 创建标签 |

## 4. 前端页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 软件包列表 | /software/packages | 软件包列表 |
| 创建软件包 | /software/packages/create | 创建软件包 |
| 版本管理 | /software/versions | 版本管理页面 |
| 分类管理 | /software/categories | 分类管理 |
| 标签管理 | /software/tags | 标签管理 |

## 5. 数据表

- `t_software_package` - 软件包表
- `t_software_version` - 软件版本表
- `t_category` - 分类表
- `t_tag` - 标签表
- `t_software_type` - 软件类型表

## 6. 实现情况

| 功能 | 状态 | 说明 |
|------|------|------|
| 软件包CRUD | ✅ 已实现 | SoftwarePackageController |
| 版本管理 | ✅ 已实现 | SoftwarePackageController |
| 分类管理 | ✅ 已实现 | CategoryController |
| 标签管理 | ✅ 已实现 | TagController |
| 发布审批 | ✅ 已实现 | ApprovalController |
| 前端页面 | ✅ 已实现 | software/*.vue |
