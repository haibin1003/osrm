# 设计评审文档：REQ-700 使用统计看板

**功能编号**: REQ-700
**评审日期**: 2026-03-25
**评审人**: Claude
**状态**: 通过 ✅（有条件）

---

## 1. 评审结论

**评审结果**: 通过，可以进入 Phase 4 编码阶段

**条件**:
1. 一期实现基于现有表的实时聚合查询（不创建统计快照表）
2. 数据量超过 10万条订购记录时，再考虑预计算优化
3. 缓存策略必须实现，避免频繁查询

---

## 2. 设计要点确认

### 2.1 统计维度

| 维度 | 实现方式 | 状态 |
|------|---------|------|
| 时间范围 | 参数控制（days/date range） | ✅ 确认 |
| 软件包热度 | 按订购数 + 业务系统数双维度 | ✅ 确认 |
| 业务系统分布 | 饼图 + TOP 10 表格 | ✅ 确认 |
| 类型分布 | 饼图展示 | ✅ 确认 |
| 趋势图 | 折线图（日/周/月聚合） | ✅ 确认 |

### 2.2 技术方案

| 决策项 | 方案 | 理由 |
|--------|------|------|
| 数据来源 | 实时聚合查询 | 当前数据量小，实时查询更简单 |
| 统计表 | 暂不创建 | 一期先验证需求，二期优化性能 |
| 缓存 | Redis 5-60分钟 | 减少数据库压力 |
| 图表库 | ECharts（已集成） | 与现有前端一致 |

### 2.3 接口设计

| 接口 | 路径 | 状态 |
|------|------|------|
| 概览 | GET /statistics/overview | ✅ 确认 |
| 趋势 | GET /statistics/trend | ✅ 确认 |
| 业务系统分布 | GET /statistics/business-distribution | ✅ 确认 |
| 热度排行 | GET /statistics/popularity | ✅ 确认 |
| 类型分布 | GET /statistics/type-distribution | ✅ 确认 |
| 导出 | GET /statistics/export | ⚠️ 二期实现 |

---

## 3. 实现建议

### 3.1 性能优化

```sql
-- 确保以下索引存在
ALTER TABLE t_subscription ADD KEY idx_created_at (created_at);
ALTER TABLE t_subscription ADD KEY idx_status_created_at (status, created_at);
```

### 3.2 缓存实现

```java
@Cacheable(value = "statistics", key = "'overview'")
public StatisticsOverviewDTO getOverview() { ... }

@Cacheable(value = "statistics", key = "'trend:' + #days")
public TrendDataDTO getTrend(int days) { ... }
```

### 3.3 大数据量处理

当 `t_subscription` 表数据超过 10万条时，考虑：
1. 创建统计快照表
2. 定时任务每日凌晨预计算
3. 趋势查询走统计表

---

## 4. 风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| 数据量大时查询慢 | 中 | 加索引 + 缓存，必要时创建统计表 |
| 统计口径不明确 | 低 | 需求文档已明确各指标计算方式 |
| 图表展示性能 | 低 | 前端分页，后端限制返回数量 |

---

## 5. 下一步行动

进入 Phase 4: 后端编码 + 接口测试

1. 创建 StatisticsController
2. 创建 StatisticsAppService（应用服务层）
3. 创建 StatisticsRepository（数据访问层）
4. 编写集成测试

---

## 6. 变更记录

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-03-25 | v1.0 | 初始评审通过 |
