# API 设计文档：使用统计看板

**功能编号**: REQ-700
**文档日期**: 2026-03-25
**版本**: v1.0

---

## 1. 接口概览

| 接口 | 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|------|
| 统计概览 | GET | `/api/v1/statistics/overview` | 获取概览卡片数据 | 登录用户 |
| 趋势数据 | GET | `/api/v1/statistics/trend` | 获取订购趋势图数据 | 登录用户 |
| 业务系统分布 | GET | `/api/v1/statistics/business-distribution` | 获取业务系统软件分布 | 软件管理员+ |
| 软件热度排行 | GET | `/api/v1/statistics/popularity` | 获取热门软件排行 | 登录用户 |
| 类型分布 | GET | `/api/v1/statistics/type-distribution` | 获取软件类型分布 | 登录用户 |
| 导出报表 | GET | `/api/v1/statistics/export` | 导出统计报表 | 系统管理员 |

---

## 2. 通用规范

### 2.1 请求头

```
Authorization: Bearer {access_token}
Content-Type: application/json
```

### 2.2 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 2.3 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未授权（Token无效或过期） |
| 403 | 无权限访问 |
| 500 | 服务器内部错误 |

---

## 3. 接口详细定义

### 3.1 GET /api/v1/statistics/overview

**接口说明**: 获取统计概览卡片数据（总软件包数、总订购数、活跃业务系统数、本月新增）

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | - |

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalPackages": 45,
    "totalSubscriptions": 128,
    "activeBusinessSystems": 12,
    "newSubscriptionsThisMonth": 15,
    "trends": {
      "totalPackagesChange": 5.2,
      "totalSubscriptionsChange": 12.8,
      "activeBusinessSystemsChange": 0.0,
      "newSubscriptionsThisMonthChange": -3.2
    }
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalPackages | Integer | 已发布软件包总数 |
| totalSubscriptions | Integer | 累计订购申请总数 |
| activeBusinessSystems | Integer | 已启用业务系统数 |
| newSubscriptionsThisMonth | Integer | 本月新增订购数 |
| trends | Object | 环比变化数据 |
| trends.totalPackagesChange | Float | 总软件包环比变化率（%） |
| trends.totalSubscriptionsChange | Float | 总订购环比变化率（%） |
| trends.activeBusinessSystemsChange | Float | 业务系统环比变化率（%） |
| trends.newSubscriptionsThisMonthChange | Float | 本月新增环比变化率（%） |

---

### 3.2 GET /api/v1/statistics/trend

**接口说明**: 获取订购量趋势图数据

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| days | Integer | 否 | 时间范围天数，默认7，可选：7/30/90/365 |
| startDate | String | 否 | 自定义开始日期（yyyy-MM-dd） |
| endDate | String | 否 | 自定义结束日期（yyyy-MM-dd） |

**参数规则**:
- 当传入 `days` 时，`startDate` 和 `endDate` 无效
- 当传入 `startDate` 时，必须同时传入 `endDate`
- `days` 与日期范围二选一，都为空时默认7天

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "period": "daily",
    "startDate": "2026-03-18",
    "endDate": "2026-03-25",
    "totalDays": 8,
    "data": [
      {
        "date": "2026-03-18",
        "subscriptionCount": 5,
        "approvedCount": 3,
        "rejectedCount": 1,
        "pendingCount": 1
      },
      {
        "date": "2026-03-19",
        "subscriptionCount": 8,
        "approvedCount": 5,
        "rejectedCount": 2,
        "pendingCount": 1
      },
      {
        "date": "2026-03-20",
        "subscriptionCount": 12,
        "approvedCount": 8,
        "rejectedCount": 3,
        "pendingCount": 1
      },
      {
        "date": "2026-03-21",
        "subscriptionCount": 6,
        "approvedCount": 4,
        "rejectedCount": 1,
        "pendingCount": 1
      },
      {
        "date": "2026-03-22",
        "subscriptionCount": 10,
        "approvedCount": 6,
        "rejectedCount": 2,
        "pendingCount": 2
      },
      {
        "date": "2026-03-23",
        "subscriptionCount": 15,
        "approvedCount": 10,
        "rejectedCount": 3,
        "pendingCount": 2
      },
      {
        "date": "2026-03-24",
        "subscriptionCount": 7,
        "approvedCount": 5,
        "rejectedCount": 1,
        "pendingCount": 1
      },
      {
        "date": "2026-03-25",
        "subscriptionCount": 9,
        "approvedCount": 6,
        "rejectedCount": 2,
        "pendingCount": 1
      }
    ],
    "summary": {
      "totalSubscriptionCount": 72,
      "totalApprovedCount": 47,
      "totalRejectedCount": 15,
      "averageDaily": 9.0
    }
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| period | String | 数据聚合周期：daily/weekly/monthly |
| startDate | String | 数据起始日期 |
| endDate | String | 数据结束日期 |
| totalDays | Integer | 总天数 |
| data | Array | 每日统计数据 |
| data[].date | String | 日期 |
| data[].subscriptionCount | Integer | 订购申请数 |
| data[].approvedCount | Integer | 审批通过数 |
| data[].rejectedCount | Integer | 审批拒绝数 |
| data[].pendingCount | Integer | 待审批数 |
| summary | Object | 汇总数据 |
| summary.totalSubscriptionCount | Integer | 总计订购数 |
| summary.totalApprovedCount | Integer | 总计通过数 |
| summary.totalRejectedCount | Integer | 总计拒绝数 |
| summary.averageDaily | Float | 日均订购数 |

---

### 3.3 GET /api/v1/statistics/business-distribution

**接口说明**: 获取业务系统软件使用分布数据（饼图 + 表格）

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回数量，默认10，最大50 |

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalBusinessSystems": 12,
    "totalPackages": 45,
    "totalSubscriptions": 128,
    "data": [
      {
        "businessSystemId": 1,
        "systemName": "订单管理系统",
        "systemCode": "order-sys",
        "domain": "BUSINESS",
        "packageCount": 15,
        "subscriptionCount": 25,
        "percentage": 19.5
      },
      {
        "businessSystemId": 2,
        "systemName": "用户中心",
        "systemCode": "user-center",
        "domain": "FOUNDATION",
        "packageCount": 12,
        "subscriptionCount": 20,
        "percentage": 15.6
      },
      {
        "businessSystemId": 3,
        "systemName": "支付系统",
        "systemCode": "payment-sys",
        "domain": "BUSINESS",
        "packageCount": 10,
        "subscriptionCount": 18,
        "percentage": 14.1
      }
    ]
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalBusinessSystems | Integer | 业务系统总数 |
| totalPackages | Integer | 软件包总数（引用） |
| totalSubscriptions | Integer | 订购总数（引用） |
| data | Array | 业务系统分布数据 |
| data[].businessSystemId | Long | 业务系统ID |
| data[].systemName | String | 业务系统名称 |
| data[].systemCode | String | 业务系统编码 |
| data[].domain | String | 业务域枚举 |
| data[].packageCount | Integer | 使用的软件包数（去重） |
| data[].subscriptionCount | Integer | 订购次数 |
| data[].percentage | Float | 占比（基于packageCount） |

---

### 3.4 GET /api/v1/statistics/popularity

**接口说明**: 获取软件包热度排行数据

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | Integer | 否 | 返回数量，默认10，最大100 |
| sortBy | String | 否 | 排序维度：subscription_count（默认）/business_system_count |

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sortBy": "subscription_count",
    "total": 10,
    "data": [
      {
        "rank": 1,
        "packageId": 1,
        "packageName": "MySQL",
        "packageKey": "mysql",
        "softwareType": "DOCKER_IMAGE",
        "subscriptionCount": 25,
        "businessSystemCount": 8,
        "trend": "up",
        "change": 2
      },
      {
        "rank": 2,
        "packageId": 5,
        "packageName": "Redis",
        "packageKey": "redis",
        "softwareType": "DOCKER_IMAGE",
        "subscriptionCount": 20,
        "businessSystemCount": 7,
        "trend": "stable",
        "change": 0
      },
      {
        "rank": 3,
        "packageId": 12,
        "packageName": "Spring Boot",
        "packageKey": "spring-boot",
        "softwareType": "MAVEN",
        "subscriptionCount": 18,
        "businessSystemCount": 6,
        "trend": "down",
        "change": -1
      }
    ]
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| sortBy | String | 当前排序维度 |
| total | Integer | 返回数量 |
| data | Array | 排行数据 |
| data[].rank | Integer | 排名 |
| data[].packageId | Long | 软件包ID |
| data[].packageName | String | 软件包名称 |
| data[].packageKey | String | 软件包唯一标识 |
| data[].softwareType | String | 软件类型 |
| data[].subscriptionCount | Integer | 订购次数 |
| data[].businessSystemCount | Integer | 使用业务系统数（去重） |
| data[].trend | String | 趋势：up/down/stable |
| data[].change | Integer | 排名变化（正数上升，负数下降） |

---

### 3.5 GET /api/v1/statistics/type-distribution

**接口说明**: 获取软件类型分布数据

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | - |

#### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalPackages": 45,
    "data": [
      {
        "softwareType": "DOCKER_IMAGE",
        "typeName": "Docker镜像",
        "packageCount": 18,
        "subscriptionCount": 68,
        "percentage": 40.0,
        "color": "#5470c6"
      },
      {
        "softwareType": "MAVEN",
        "typeName": "Maven依赖",
        "packageCount": 12,
        "subscriptionCount": 32,
        "percentage": 26.7,
        "color": "#91cc75"
      },
      {
        "softwareType": "NPM",
        "typeName": "NPM包",
        "packageCount": 8,
        "subscriptionCount": 18,
        "percentage": 17.8,
        "color": "#fac858"
      },
      {
        "softwareType": "GENERIC",
        "typeName": "通用文件",
        "packageCount": 5,
        "subscriptionCount": 8,
        "percentage": 11.1,
        "color": "#ee6666"
      },
      {
        "softwareType": "PYPI",
        "typeName": "PyPI包",
        "packageCount": 2,
        "subscriptionCount": 2,
        "percentage": 4.4,
        "color": "#73c0de"
      }
    ]
  }
}
```

#### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| totalPackages | Integer | 软件包总数 |
| data | Array | 类型分布数据 |
| data[].softwareType | String | 软件类型编码 |
| data[].typeName | String | 软件类型显示名 |
| data[].packageCount | Integer | 该类型软件包数 |
| data[].subscriptionCount | Integer | 该类型订购总数 |
| data[].percentage | Float | 占比（%） |
| data[].color | String | 图表颜色（前端预设） |

---

### 3.6 GET /api/v1/statistics/export

**接口说明**: 导出统计报表（Excel格式）

#### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | String | 否 | 报表类型：summary/trend/distribution，默认summary |
| format | String | 否 | 导出格式：xlsx/csv，默认xlsx |
| startDate | String | 否 | 开始日期（趋势报表必填） |
| endDate | String | 否 | 结束日期（趋势报表必填） |

#### 响应

**成功**: 返回文件流（Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet）

**响应头**:
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="statistics_20260325.xlsx"
```

#### 错误响应

```json
{
  "code": 403,
  "message": "无权导出报表，需要系统管理员权限"
}
```

---

## 4. 前端组件对接

### 4.1 ECharts 配置示例

#### 趋势图（Line Chart）

```javascript
// 趋势图配置
const trendOption = {
  tooltip: { trigger: 'axis' },
  legend: { data: ['订购申请', '审批通过', '审批拒绝'] },
  xAxis: {
    type: 'category',
    data: response.data.data.map(item => item.date)
  },
  yAxis: [{ type: 'value', name: '数量' }],
  series: [
    {
      name: '订购申请',
      type: 'line',
      data: response.data.data.map(item => item.subscriptionCount),
      smooth: true
    },
    {
      name: '审批通过',
      type: 'line',
      data: response.data.data.map(item => item.approvedCount),
      smooth: true
    },
    {
      name: '审批拒绝',
      type: 'line',
      data: response.data.data.map(item => item.rejectedCount),
      smooth: true
    }
  ]
};
```

#### 饼图（Pie Chart）

```javascript
// 类型分布饼图
const pieOption = {
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { orient: 'vertical', left: 'left' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      data: response.data.data.map(item => ({
        value: item.packageCount,
        name: item.typeName
      }))
    }
  ]
};
```

#### 横向柱状图（Bar Chart - Horizontal）

```javascript
// 热度排行柱状图
const barOption = {
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    data: response.data.data.map(item => item.packageName).reverse()
  },
  series: [
    {
      type: 'bar',
      data: response.data.data.map(item => item.subscriptionCount).reverse(),
      itemStyle: { color: '#5470c6' }
    }
  ]
};
```

---

## 5. 缓存策略

### 5.1 服务端缓存

| 接口 | 缓存时长 | 缓存Key |
|------|---------|---------|
| /statistics/overview | 5分钟 | `stats:overview` |
| /statistics/trend | 1小时 | `stats:trend:{days}` |
| /statistics/popularity | 30分钟 | `stats:popularity:{sortBy}` |
| /statistics/type-distribution | 1小时 | `stats:type:dist` |
| /statistics/business-distribution | 30分钟 | `stats:biz:dist` |

### 5.2 缓存刷新

- 订购状态变更时，清除相关缓存
- 软件包发布/下架时，清除全部统计缓存
- 定时任务每小时重建缓存

---

## 6. 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2026-03-25 | v1.0 | 初始版本 | Claude |
