# Phase 2.2 技术设计文档：系统-软件关联图

## 文档信息

- **Feature ID**: Phase 2.2
- **文档类型**: 技术设计
- **创建时间**: 2026-03-25
- **关联需求**: docs/requirements/features/phase2.2-relationship-graph.md

---

## 1. 架构设计

### 1.1 整体架构

```
┌────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                          │
│  ┌──────────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ RelationshipGraph │  │ FilterPanel  │  │ NodeDetailPanel │  │
│  │    (ECharts)     │  │              │  │                │  │
│  └──────────────────┘  └──────────────┘  └────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼ REST API
┌────────────────────────────────────────────────────────────────┐
│                      控制层 (Controller)                       │
│              RelationshipGraphController                       │
│  ├─ GET /api/v1/tracking/relationship-graph                    │
│  ├─ GET /api/v1/tracking/system/{id}/dependencies             │
│  └─ GET /api/v1/tracking/package/{id}/impact                  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                       应用服务层 (Service)                     │
│              RelationshipGraphAppService                       │
│  ├─ buildRelationshipGraph(filters)                            │
│  ├─ getSystemDependencies(systemId)                           │
│  └─ getPackageImpact(packageId)                               │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                      数据访问层 (Repository)                   │
│  SubscriptionRepository / BusinessSystemRepository             │
│  / SoftwarePackageRepository                                   │
└────────────────────────────────────────────────────────────────┘
```

### 1.2 数据流

```
1. 前端请求关联图数据
2. Controller 接收请求，调用 Service
3. Service 查询所有订阅关系（带过滤条件）
4. Service 构建节点和边
5. 返回 GraphDTO 给前端
6. 前端使用 ECharts 渲染力导向图
```

---

## 2. 数据库设计

### 2.1 结论：无需表变更

Phase 2.2 功能完全基于现有表：
- `t_business_system` - 业务系统节点
- `t_software_package` - 软件包节点
- `t_subscription` - 订阅关系边

### 2.2 查询优化

需要在 `t_subscription` 表添加复合索引（可选优化）：

```sql
-- 已存在索引检查
-- 建议添加（如果尚不存在）：
CREATE INDEX idx_subscription_status ON t_subscription(status);
CREATE INDEX idx_subscription_composite ON t_subscription(business_system_id, package_id, status);
```

---

## 3. API 详细设计

### 3.1 DTO 定义

```java
// RelationshipGraphDTO.java
public class RelationshipGraphDTO {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private GraphMetadata metadata;
}

public abstract class GraphNode {
    private String id;
    private String type; // "system" | "package"
    private String name;
}

public class SystemNode extends GraphNode {
    private Long systemId;
    private String systemCode;
    private String domain;
    private Boolean enabled;
}

public class PackageNode extends GraphNode {
    private Long packageId;
    private String packageKey;
    private String softwareType;
    private String status;
}

public class GraphEdge {
    private String id;
    private String source;  // node id
    private String target;  // node id
    private String versionNumber;
    private String status;
}

public class GraphMetadata {
    private Integer totalSystems;
    private Integer totalPackages;
    private Integer totalSubscriptions;
}
```

### 3.2 Controller

```java
@RestController
@RequestMapping("/api/v1/tracking")
public class RelationshipGraphController {

    @GetMapping("/relationship-graph")
    public ApiResponse<RelationshipGraphDTO> getGraph(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String softwareType,
            @RequestParam(required = false) String status) {
        // implementation
    }

    @GetMapping("/system/{systemId}/dependencies")
    public ApiResponse<SystemDependenciesDTO> getSystemDependencies(
            @PathVariable Long systemId) {
        // implementation
    }

    @GetMapping("/package/{packageId}/impact")
    public ApiResponse<PackageImpactDTO> getPackageImpact(
            @PathVariable Long packageId) {
        // implementation
    }
}
```

---

## 4. 前端设计

### 4.1 组件结构

```
src/views/tracking/
├── RelationshipGraph.vue          # 主页面
├── components/
│   ├── GraphVisualization.vue     # ECharts 图表组件
│   ├── FilterPanel.vue            # 过滤器面板
│   ├── NodeDetailPanel.vue        # 节点详情
│   └── StatisticsPanel.vue        # 统计面板
```

### 4.2 ECharts 配置

```typescript
const graphOption = {
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      if (params.dataType === 'node') {
        return formatNodeTooltip(params.data);
      }
      return formatEdgeTooltip(params.data);
    }
  },
  series: [{
    type: 'graph',
    layout: 'force',
    data: nodes,
    links: edges,
    roam: true,
    draggable: true,
    force: {
      repulsion: 300,
      edgeLength: 150,
      gravity: 0.1
    },
    emphasis: {
      focus: 'adjacency',
      lineStyle: { width: 4 }
    },
    lineStyle: {
      color: 'source',
      curveness: 0.1
    }
  }]
};
```

### 4.3 节点样式

```typescript
const nodeStyles = {
  system: {
    symbol: 'circle',
    symbolSize: 50,
    itemStyle: { color: '#5470c6' }
  },
  package: {
    symbol: 'rect',
    symbolSize: [60, 40],
    itemStyle: { color: (params: any) => typeColors[params.data.softwareType] }
  }
};

const typeColors: Record<string, string> = {
  'DOCKER_IMAGE': '#91cc75',
  'HELM_CHART': '#fac858',
  'MAVEN': '#ee6666',
  'NPM': '#73c0de',
  'PYPI': '#3ba272',
  'GENERIC': '#9a60b4'
};
```

---

## 5. 性能设计

### 5.1 大数据量处理

| 数据规模 | 策略 |
|----------|------|
| < 500 节点 | 直接渲染 |
| 500-2000 节点 | 聚合展示 + 按需展开 |
| > 2000 节点 | 分页加载 + 聚焦视图 |

### 5.2 优化措施

1. **后端优化**
   - 使用数据库索引
   - 批量查询避免 N+1
   - 缓存热点数据

2. **前端优化**
   - 节点聚合（相同类型的系统聚合为超级节点）
   - 懒加载（只渲染视口内节点）
   - Web Worker 计算布局

### 5.3 缓存策略

```java
@Cacheable(value = "relationshipGraph", key = "#filters.hashCode()")
public RelationshipGraphDTO buildGraph(GraphFilter filters) {
    // implementation
}
```

---

## 6. 安全设计

### 6.1 权限控制

| API | 所需权限 |
|-----|----------|
| GET /relationship-graph | tracking:read |
| GET /system/{id}/dependencies | tracking:read |
| GET /package/{id}/impact | tracking:read |

### 6.2 数据隔离

- 用户只能看到其有权限的业务系统关联
- 敏感软件包信息脱敏展示

---

## 7. 测试策略

### 7.1 测试用例

| 测试类型 | 测试内容 |
|----------|----------|
| 单元测试 | Graph 数据构建逻辑 |
| 集成测试 | 3 个 API 端点 |
| E2E 测试 | 图表渲染、交互、过滤器 |

### 7.2 性能测试

- 1000 节点渲染时间 < 3s
- 节点交互响应时间 < 200ms

---

## 8. 部署计划

### 8.1 实施步骤

1. **Phase 1**: 后端 API 实现 + 集成测试
2. **Phase 2**: 前端基础图表 + 过滤器
3. **Phase 3**: 节点交互 + 详情面板
4. **Phase 4**: E2E 测试 + 性能优化

### 8.2 回滚策略

- 数据库无变更，无需回滚
- 功能开关控制（前端 feature flag）

---

## 9. 风险评估

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 大数据量渲染卡顿 | 中 | 高 | 节点聚合、分页加载 |
| 布局混乱不美观 | 中 | 中 | 力导向参数调优 |
| 浏览器兼容性问题 | 低 | 中 | 使用 ECharts 标准功能 |

---

## 10. 附录

### 10.1 参考资料

- ECharts Graph: https://echarts.apache.org/examples/zh/index.html#chart-type-graph
- Force-Directed Layout: https://github.com/d3/d3-force

### 10.2 设计决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| 图表库 | ECharts | 项目已有依赖，功能丰富 |
| 布局算法 | 力导向 | 自动布局，适合网络图 |
| 节点形状 | 圆形/方形 | 区分系统和软件 |
| 边样式 | 曲线 | 美观，减少重叠 |
