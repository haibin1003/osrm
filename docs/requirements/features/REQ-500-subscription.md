# 订购管理 (REQ-500~550)

## 文档信息
- **需求编号**: REQ-500 ~ REQ-550
- **模块名称**: 订购与下载管理
- **优先级**: P0
- **状态**: 已实现

## 1. 功能概述

订购与下载管理实现软件订购申请、审批流程、下载令牌管理，支持多种获取方式（Docker Pull/Maven/NPM/PyPI）。

## 2. 功能清单

### REQ-500 订购申请
- 用户浏览软件门户
- 申请订购指定版本的软件
- 选择业务系统和使用环境
- 填写使用场景说明

### REQ-501 订购审批流程
- 软件管理员审批订购申请
- 批准或驳回申请
- 驳回时填写原因

### REQ-502 我的订购
- 查看我的订购记录
- 查看订购状态（待审批/已批准/已驳回）
- 订购历史记录

### REQ-503 下载令牌管理
- 审批通过后自动生成下载令牌
- 支持设置令牌有效期
- 支持设置最大下载次数
- 令牌禁用/启用管理

### REQ-504 多种获取方式
- Docker Pull 命令
- Maven coordinates
- NPM install 命令
- PyPI pip install 命令

## 3. API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 申请订购 | POST | /api/v1/subscriptions | 创建订购申请 |
| 我的订购列表 | GET | /api/v1/subscriptions/my | 我的订购列表 |
| 订购详情 | GET | /api/v1/subscriptions/{id} | 订购详情 |
| 待审批列表 | GET | /api/v1/subscriptions/pending | 待审批列表 |
| 审批历史 | GET | /api/v1/subscriptions/history | 审批历史 |
| 批准 | POST | /api/v1/subscriptions/{id}/approve | 批准订购 |
| 驳回 | POST | /api/v1/subscriptions/{id}/reject | 驳回订购 |
| 生成令牌 | POST | /api/v1/subscriptions/{id}/token | 生成下载令牌 |
| 令牌列表 | GET | /api/v1/subscriptions/{id}/tokens | 下载令牌列表 |

## 4. 前端页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 我的订购 | /subscription/my | 我的订购记录 |
| 申请订购 | /subscription/apply | 申请订购页面 |
| 待审批 | /subscription/pending | 待我审批的订购 |
| 审批历史 | /subscription/history | 审批历史记录 |

## 5. 数据表

- `t_subscription` - 订购申请表
- `t_download_token` - 下载令牌表

## 6. 实现情况

| 功能 | 状态 | 说明 |
|------|------|------|
| 订购申请 | ✅ 已实现 | SubscriptionController |
| 审批流程 | ✅ 已实现 | SubscriptionController |
| 我的订购 | ✅ 已实现 | SubscriptionController |
| 下载令牌 | ✅ 已实现 | SubscriptionController |
| 前端页面 | ✅ 已实现 | subscription/*.vue |
