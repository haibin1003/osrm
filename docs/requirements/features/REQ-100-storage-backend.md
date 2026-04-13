# 存储后端管理 (REQ-100~199)

## 文档信息
- **需求编号**: REQ-100 ~ REQ-103
- **模块名称**: 存储后端管理
- **优先级**: P0
- **状态**: 已实现

## 1. 功能概述

存储后端管理允许系统管理员配置和管理多种类型的存储后端，包括 Harbor（Docker镜像仓库）、Nexus（Maven/NPM仓库）和 NAS（文件存储）。

## 2. 功能清单

### REQ-100 存储后端基础管理
- 创建、编辑、删除存储后端配置
- 支持存储后端类型：Harbor、Nexus、NAS
- 存储后端启用/禁用

### REQ-101 存储后端健康检测
- 自动健康状态检测（每5分钟）
- 手动触发健康检测
- 健康状态展示（健康/异常/未知）

### REQ-102 存储后端连接测试
- 保存前验证连接配置
- 测试连接功能
- 显示连接错误信息

### REQ-103 默认存储后端
- 设置默认存储后端
- 创建软件版本时自动选择默认存储

## 3. API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 列表 | GET | /api/v1/storage/backends | 获取存储后端列表 |
| 详情 | GET | /api/v1/storage/backends/{id} | 获取存储后端详情 |
| 创建 | POST | /api/v1/storage/backends | 创建存储后端 |
| 更新 | PUT | /api/v1/storage/backends/{id} | 更新存储后端 |
| 删除 | DELETE | /api/v1/storage/backends/{id} | 删除存储后端 |
| 健康检测 | POST | /api/v1/storage/backends/{id}/health | 触发健康检测 |
| 连接测试 | POST | /api/v1/storage/backends/{id}/test | 测试连接 |

## 4. 前端页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 存储后端列表 | /system/storage | 存储后端列表页 |
| 创建存储 | /system/storage/create | 创建存储后端 |
| 编辑存储 | /system/storage/:id/edit | 编辑存储后端 |
| 存储详情 | /system/storage/:id | 存储详情页 |

## 5. 数据表

- `t_storage_backend` - 存储后端配置表

## 6. 实现情况

| 功能 | 状态 | 说明 |
|------|------|------|
| 基础CRUD | ✅ 已实现 | StorageBackendController |
| 健康检测 | ✅ 已实现 | StorageBackendService |
| 连接测试 | ✅ 已实现 | StorageBackendService |
| 前端页面 | ✅ 已实现 | storage/*.vue |
