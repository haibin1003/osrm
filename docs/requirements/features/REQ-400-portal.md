# 软件门户 (REQ-400~430)

## 文档信息
- **需求编号**: REQ-400 ~ REQ-430
- **模块名称**: 软件门户
- **优先级**: P0
- **状态**: 已实现

## 1. 功能概述

软件门户为所有用户提供公开的软件浏览功能，无需登录即可查看已发布的软件信息。

## 2. 功能清单

### REQ-400 软件浏览
- 公开访问软件列表
- 按关键字搜索
- 按类型筛选
- 分页展示

### REQ-401 软件详情
- 软件基本信息展示
- 版本列表
- 依赖关系图
- 安全报告

### REQ-402 热门软件推荐
- 按下载量排序
- 按浏览量排序
- 首页展示热门软件

### REQ-403 统计概览
- 软件总数
- 已发布数量
- 订购次数
- 近7天趋势

## 3. API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 软件列表 | GET | /api/v1/portal/software | 公开软件列表 |
| 软件详情 | GET | /api/v1/portal/software/{id} | 软件详情 |
| 热门软件 | GET | /api/v1/portal/popular | 热门推荐 |
| 统计概览 | GET | /api/v1/portal/stats/overview | 统计概览 |
| 趋势数据 | GET | /api/v1/portal/stats/trend | 趋势数据 |
| 依赖关系 | GET | /api/v1/portal/software/{id}/dependencies | 依赖图 |
| 安全报告 | GET | /api/v1/portal/software/{id}/security | 安全报告 |
| 版本列表 | GET | /api/v1/portal/software/{id}/versions | 版本列表 |

## 4. 前端页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 软件门户 | /browse | 公开软件列表页 |
| 软件详情 | /browse/software/:id | 软件详情页 |

## 5. 实现情况

| 功能 | 状态 | 说明 |
|------|------|------|
| 软件浏览 | ✅ 已实现 | PortalController |
| 软件详情 | ✅ 已实现 | PortalController |
| 热门推荐 | ✅ 已实现 | PortalController |
| 统计概览 | ✅ 已实现 | PortalController |
| 前端页面 | ✅ 已实现 | portal/*.vue |
