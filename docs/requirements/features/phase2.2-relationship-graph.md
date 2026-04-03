# Phase 2.2 使用追踪：系统-软件关联图

## 文档信息

- **Feature ID**: Phase 2.2
- **功能名称**: 使用追踪 - 全局系统软件关联图
- **创建时间**: 2026-03-25
- **作者**: 开发团队
- **状态**: 设计中

---

## 1. 需求概述

### 1.1 背景

OSRM 平台中，业务系统通过订购方式使用软件包。随着业务增长，需要可视化展示：
- 哪些系统使用了哪些软件
- 软件包被哪些系统依赖
- 系统之间的软件共享关系
- 潜在的影响范围（当一个软件需要升级或存在漏洞时）

### 1.2 目标

提供一个全局的系统-软件关联图（System-Software Relationship Graph），支持：
1. **全局视角**: 展示所有业务系统与软件包的关联网络
2. **系统视角**: 查看单个系统使用的所有软件
3. **软件视角**: 查看使用某个软件的所有系统
4. **影响分析**: 分析软件变更对系统的影响范围

### 1.3 用户场景

| 角色 | 场景 | 价值 |
|------|------|------|
| 系统管理员 | 查看全局依赖关系 | 了解平台整体使用情况 |
| 软件管理员 | 分析软件影响范围 | 评估升级/变更的影响 |
| 安全管理员 | 识别漏洞影响范围 | 快速定位需要修复的系统 |
| 业务负责人 | 查看系统依赖详情 | 了解系统技术栈构成 |

---

## 2. 功能需求

### 2.1 关联图可视化

#### FR-2.1.1 全局关联网络图
- 以力导向图（Force-Directed Graph）展示所有系统和软件的关系
- 节点类型：业务系统（圆形）、软件包（方形）
- 边：订阅关系，可显示版本号
- 支持缩放、拖拽、平移

#### FR-2.1.2 节点交互
- 点击节点：显示详情面板
- 悬停节点：显示基本信息 tooltip
- 双击节点：聚焦该节点及其直接关系
- 右键菜单：跳转到详情页

#### FR-2.1.3 过滤器
- 按业务域过滤（业务域/运营域/数据域等）
- 按软件类型过滤（Docker/Maven/NPM等）
- 按订阅状态过滤（APPROVED/PENDING等）
- 按关键字搜索系统或软件

### 2.2 多维度视图

#### FR-2.2.1 系统视图
- 选择一个业务系统为中心
- 展示该系统使用的所有软件包
- 按软件类型分组展示

#### FR-2.2.2 软件视图
- 选择一个软件包为中心
- 展示使用该软件的所有业务系统
- 显示各系统使用的版本

#### FR-2.2.3 版本视图
- 展示软件各版本被哪些系统使用
- 识别使用旧版本的系统

### 2.3 影响分析

#### FR-2.3.1 软件影响分析
- 选择一个软件包
- 分析该软件被哪些系统使用
- 统计影响范围（系统数量、业务域分布）

#### FR-2.3.2 版本兼容性分析
- 识别使用不同版本的系统
- 高亮版本差异较大的系统

---

## 3. 数据模型

### 3.1 节点类型

```typescript
// 业务系统节点
interface SystemNode {
  id: string;           // 格式: "system:{id}"
  type: 'system';
  systemId: number;
  systemName: string;
  systemCode: string;
  domain: string;
  enabled: boolean;
}

// 软件包节点
interface PackageNode {
  id: string;           // 格式: "package:{id}"
  type: 'package';
  packageId: number;
  packageName: string;
  packageKey: string;
  softwareType: string;
  status: string;
}
```

### 3.2 边类型

```typescript
interface SubscriptionEdge {
  id: string;
  source: string;       // system:{id}
  target: string;       // package:{id}
  versionId?: number;
  versionNumber?: string;
  status: string;
  createdAt: string;
}
```

### 3.3 图谱数据

```typescript
interface RelationshipGraph {
  nodes: (SystemNode | PackageNode)[];
  edges: SubscriptionEdge[];
  metadata: {
    totalSystems: number;
    totalPackages: number;
    totalSubscriptions: number;
  };
}
```

---

## 4. API 设计

### 4.1 获取完整关联图

```
GET /api/v1/tracking/relationship-graph
```

**Query Parameters:**
| 参数 | 类型 | 说明 |
|------|------|------|
| domain | string | 按业务域过滤 |
| softwareType | string | 按软件类型过滤 |
| status | string | 按订阅状态过滤 |

**Response:**
```json
{
  "code": 200,
  "data": {
    "nodes": [
      { "id": "system:1", "type": "system", "systemName": "订单系统", ... },
      { "id": "package:1", "type": "package", "packageName": "MySQL", ... }
    ],
    "edges": [
      { "id": "e1", "source": "system:1", "target": "package:1", "versionNumber": "8.0" }
    ],
    "metadata": {
      "totalSystems": 15,
      "totalPackages": 42,
      "totalSubscriptions": 128
    }
  }
}
```

### 4.2 获取系统关联详情

```
GET /api/v1/tracking/system/{systemId}/dependencies
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "system": { ... },
    "packages": [
      { "packageId": 1, "packageName": "MySQL", "versionNumber": "8.0", ... }
    ],
    "statistics": {
      "totalPackages": 8,
      "byType": { "DOCKER_IMAGE": 3, "MAVEN": 5 }
    }
  }
}
```

### 4.3 获取软件影响分析

```
GET /api/v1/tracking/package/{packageId}/impact
```

**Response:**
```json
{
  "code": 200,
  "data": {
    "package": { ... },
    "affectedSystems": [
      { "systemId": 1, "systemName": "订单系统", "versionNumber": "8.0" }
    ],
    "statistics": {
      "totalSystems": 5,
      "byDomain": { "BUSINESS": 3, "TECHNOLOGY": 2 },
      "byVersion": { "8.0": 3, "5.7": 2 }
    }
  }
}
```

---

## 5. 前端设计

### 5.1 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  [标题] 系统-软件关联图                              [刷新] │
├────────────┬────────────────────────────────────────────────┤
│            │                                                │
│  过滤器    │                                                │
│  ───────── │         关联图可视化区域                        │
│  业务域    │         (力导向图/环形图/矩阵图)               │
│  软件类型  │                                                │
│  订阅状态  │                                                │
│  ───────── │                                                │
│  视图切换  │                                                │
│  [全局]    │                                                │
│  [系统]    │                                                │
│  [软件]    │                                                │
│            │                                                │
├────────────┤                                                │
│  节点详情  │                                                │
│  (选中时)  │                                                │
│            │                                                │
└────────────┴────────────────────────────────────────────────┘
```

### 5.2 可视化方案

使用 **ECharts Graph** 组件，配置：
- layout: 'force'（力导向布局）
- roam: true（支持缩放拖拽）
- draggable: true（节点可拖拽）

### 5.3 颜色方案

| 节点类型 | 颜色 | 说明 |
|----------|------|------|
| 业务系统 | #5470c6 | 蓝色 |
| Docker镜像 | #91cc75 | 绿色 |
| Helm Chart | #fac858 | 黄色 |
| Maven依赖 | #ee6666 | 红色 |
| NPM包 | #73c0de | 青色 |
| PyPI包 | #3ba272 | 深绿 |
| 通用文件 | #9a60b4 | 紫色 |

---

## 6. 实现方案

### 6.1 后端实现

**Controller**: `RelationshipGraphController`
- `GET /api/v1/tracking/relationship-graph`
- `GET /api/v1/tracking/system/{id}/dependencies`
- `GET /api/v1/tracking/package/{id}/impact`

**Service**: `RelationshipGraphAppService`
- 查询所有订阅关系
- 构建节点和边
- 支持过滤器

**Repository 扩展**:
```java
// SubscriptionRepository
List<Subscription> findByStatus(SubscriptionStatus status);
List<Subscription> findByBusinessSystemId(Long systemId);
List<Subscription> findByPackageId(Long packageId);
```

### 6.2 前端实现

**页面**: `views/tracking/RelationshipGraph.vue`
**组件**:
- `GraphVisualization.vue` - ECharts 图表封装
- `FilterPanel.vue` - 过滤器面板
- `NodeDetailPanel.vue` - 节点详情

**API**: `api/tracking.ts`

---

## 7. 验收标准

### 7.1 功能验收

| 验收项 | 标准 |
|--------|------|
| 关联图加载 | 页面加载后 3 秒内显示完整关联图 |
| 节点交互 | 点击/悬停/拖拽操作响应时间 < 200ms |
| 过滤器 | 应用过滤器后 2 秒内更新图表 |
| 大数据量 | 支持 100+ 系统、500+ 软件、1000+ 订阅关系 |

### 7.2 测试覆盖

- 单元测试：Graph 数据构建逻辑
- 集成测试：3 个 API 端点
- E2E 测试：关联图可视化、过滤器、节点交互

---

## 8. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 大数据量性能 | 高 | 分页加载、节点聚合、Web Worker |
| 图表库兼容性 | 中 | 使用成熟的 ECharts 库 |
| 布局混乱 | 中 | 力导向参数调优、支持手动布局 |

---

## 9. 依赖关系

- 依赖 Phase 1：业务系统、软件包、订购管理模块
- 依赖 Phase 2.1：统计功能（可选，用于概览数据）

---

## 10. 变更记录

| 时间 | 变更 | 说明 |
|------|------|------|
| 2026-03-25 | 创建文档 | 初始版本 |
