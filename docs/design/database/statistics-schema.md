# 数据库设计文档：使用统计看板

**功能编号**: REQ-700
**文档日期**: 2026-03-25

---

## 1. 表结构概览

| 表名 | 说明 | 记录数预估 |
|------|------|-----------|
| t_statistics_daily | 每日统计快照表 | 365条/年（轻量） |
| t_statistics_package_daily | 软件包每日统计表 | 3650条/年（10个热门包） |

**设计原则**: 基于现有表（t_software_package, t_subscriptions, t_business_systems）进行聚合查询，统计表仅用于性能优化（可选）。

---

## 2. 每日统计快照表 (t_statistics_daily)

### 2.1 表定义

```sql
CREATE TABLE t_statistics_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',

    -- 软件包统计
    total_packages INT DEFAULT 0 COMMENT '总软件包数（已发布）',
    new_packages INT DEFAULT 0 COMMENT '新增软件包数',
    packages_by_type JSON COMMENT '各类型数量分布 {"DOCKER_IMAGE": 10, "MAVEN": 5}',

    -- 订购统计
    total_subscriptions INT DEFAULT 0 COMMENT '累计订购总数',
    new_subscriptions INT DEFAULT 0 COMMENT '新增订购数',
    approved_subscriptions INT DEFAULT 0 COMMENT '审批通过数',
    rejected_subscriptions INT DEFAULT 0 COMMENT '审批拒绝数',

    -- 业务系统统计
    active_business_systems INT DEFAULT 0 COMMENT '活跃业务系统数',
    business_systems_with_subscriptions INT DEFAULT 0 COMMENT '有订购的业务系统数',

    -- 元数据
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_stat_date (stat_date)
) COMMENT='每日统计快照表';
```

### 2.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| stat_date | DATE | 统计日期，主键，唯一 |
| total_packages | INT | 截止到该日期的已发布软件包总数 |
| new_packages | INT | 该日期新增发布的软件包数 |
| packages_by_type | JSON | 各软件类型的数量分布，便于饼图展示 |
| total_subscriptions | INT | 截止到该日期的累计订购总数 |
| new_subscriptions | INT | 该日期新增的订购申请数 |
| approved_subscriptions | INT | 该日期审批通过的订购数 |
| rejected_subscriptions | INT | 该日期审批拒绝的订购数 |
| active_business_systems | INT | 已启用的业务系统数量 |
| business_systems_with_subscriptions | INT | 至少有一个订购的业务系统数 |

### 2.3 索引设计

```sql
-- 主键索引（自动创建）
PRIMARY KEY (id)

-- 唯一索引：按日期查询
UNIQUE KEY uk_stat_date (stat_date)

-- 普通索引：按时间范围查询
KEY idx_stat_date_range (stat_date, total_subscriptions)
```

---

## 3. 软件包每日统计表 (t_statistics_package_daily)

### 3.1 表定义

```sql
CREATE TABLE t_statistics_package_daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',
    package_id BIGINT NOT NULL COMMENT '软件包ID',

    -- 软件包信息（冗余，避免JOIN）
    package_name VARCHAR(128) NOT NULL COMMENT '软件包名称',
    package_key VARCHAR(64) NOT NULL COMMENT '软件包唯一标识',
    software_type VARCHAR(32) NOT NULL COMMENT '软件类型',

    -- 统计数据
    new_subscriptions INT DEFAULT 0 COMMENT '新增订购数',
    total_subscriptions INT DEFAULT 0 COMMENT '累计订购数',
    unique_business_systems INT DEFAULT 0 COMMENT '使用此包的业务系统数（去重）',

    -- 排名（每日计算）
    rank_by_subscriptions INT COMMENT '按订购数排名',
    rank_by_business_systems INT COMMENT '按业务系统数排名',

    -- 元数据
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_stat_date_package (stat_date, package_id),
    KEY idx_package_id_date (package_id, stat_date)
) COMMENT='软件包每日统计表（热门包追踪）';
```

### 3.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| stat_date | DATE | 统计日期 |
| package_id | BIGINT | 软件包ID |
| package_name | VARCHAR(128) | 软件包名称（冗余） |
| package_key | VARCHAR(64) | 软件包唯一标识（冗余） |
| software_type | VARCHAR(32) | 软件类型（冗余） |
| new_subscriptions | INT | 该日期新增的订购数 |
| total_subscriptions | INT | 截止到该日期的累计订购数 |
| unique_business_systems | INT | 使用此软件包的业务系统数量（去重） |
| rank_by_subscriptions | INT | 当日按订购数排名 |
| rank_by_business_systems | INT | 当日按业务系统数排名 |

### 3.3 索引设计

```sql
-- 唯一索引：每个软件包每天一条记录
UNIQUE KEY uk_stat_date_package (stat_date, package_id)

-- 查询某软件包的历史趋势
KEY idx_package_id_date (package_id, stat_date)

-- 查询某日的排行榜
KEY idx_stat_date_rank (stat_date, rank_by_subscriptions)
```

---

## 4. 业务系统订购统计视图

### 4.1 视图定义（可选）

```sql
-- 业务系统软件订购统计视图
CREATE VIEW v_business_system_subscription_stats AS
SELECT
    bs.id AS business_system_id,
    bs.system_name,
    bs.system_code,
    bs.domain,
    COUNT(DISTINCT s.software_package_id) AS package_count,
    COUNT(s.id) AS subscription_count,
    SUM(CASE WHEN s.status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
    MAX(s.created_at) AS last_subscription_date
FROM t_business_system bs
LEFT JOIN t_subscription s ON bs.id = s.business_system_id AND s.deleted = 0
WHERE bs.enabled = 1
GROUP BY bs.id, bs.system_name, bs.system_code, bs.domain;
```

---

## 5. 核心查询 SQL

### 5.1 统计概览查询

```sql
-- 统计概览（实时查询）
SELECT
    (SELECT COUNT(*) FROM t_software_package WHERE status = 'PUBLISHED') AS total_packages,
    (SELECT COUNT(*) FROM t_subscription WHERE deleted = 0) AS total_subscriptions,
    (SELECT COUNT(*) FROM t_business_system WHERE enabled = 1) AS active_business_systems,
    (SELECT COUNT(*) FROM t_subscription
     WHERE deleted = 0
     AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')) AS new_subscriptions_this_month;
```

### 5.2 趋势数据查询

```sql
-- 近7天订购趋势
SELECT
    DATE(created_at) AS stat_date,
    COUNT(*) AS subscription_count,
    SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count,
    SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_count
FROM t_subscription
WHERE deleted = 0
  AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY DATE(created_at)
ORDER BY stat_date;
```

### 5.3 软件包热度排行

```sql
-- 按订购数排行 TOP 10
SELECT
    sp.id AS package_id,
    sp.package_name,
    sp.package_key,
    sp.software_type,
    COUNT(s.id) AS subscription_count,
    COUNT(DISTINCT s.business_system_id) AS business_system_count
FROM t_software_package sp
LEFT JOIN t_subscription s ON sp.id = s.software_package_id AND s.deleted = 0
WHERE sp.status = 'PUBLISHED'
GROUP BY sp.id, sp.package_name, sp.package_key, sp.software_type
ORDER BY subscription_count DESC
LIMIT 10;
```

### 5.4 业务系统分布

```sql
-- 业务系统软件分布
SELECT
    bs.id AS business_system_id,
    bs.system_name,
    bs.system_code,
    COUNT(DISTINCT s.software_package_id) AS package_count,
    ROUND(COUNT(DISTINCT s.software_package_id) * 100.0 /
        (SELECT COUNT(DISTINCT software_package_id) FROM t_subscription WHERE deleted = 0), 2
    ) AS percentage
FROM t_business_system bs
LEFT JOIN t_subscription s ON bs.id = s.business_system_id AND s.deleted = 0 AND s.status = 'APPROVED'
WHERE bs.enabled = 1
GROUP BY bs.id, bs.system_name, bs.system_code
ORDER BY package_count DESC;
```

### 5.5 类型分布

```sql
-- 软件类型分布
SELECT
    software_type,
    COUNT(*) AS package_count,
    SUM(subscription_count) AS total_subscriptions
FROM t_software_package sp
LEFT JOIN (
    SELECT software_package_id, COUNT(*) AS subscription_count
    FROM t_subscription
    WHERE deleted = 0
    GROUP BY software_package_id
) s ON sp.id = s.software_package_id
WHERE sp.status = 'PUBLISHED'
GROUP BY software_type;
```

---

## 6. 数据初始化与维护

### 6.1 初始化历史数据

```sql
-- 初始化每日快照（历史数据回刷）
INSERT INTO t_statistics_daily (
    stat_date,
    total_packages,
    new_packages,
    total_subscriptions,
    new_subscriptions,
    approved_subscriptions,
    active_business_systems
)
SELECT
    DATE(created_at) AS stat_date,
    (SELECT COUNT(*) FROM t_software_package
     WHERE status = 'PUBLISHED' AND DATE(created_at) <= DATE(s.created_at)) AS total_packages,
    COUNT(DISTINCT CASE WHEN DATE(sp.created_at) = DATE(s.created_at) THEN sp.id END) AS new_packages,
    (SELECT COUNT(*) FROM t_subscription
     WHERE deleted = 0 AND DATE(created_at) <= DATE(s.created_at)) AS total_subscriptions,
    COUNT(*) AS new_subscriptions,
    SUM(CASE WHEN s.status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_subscriptions,
    (SELECT COUNT(*) FROM t_business_system WHERE enabled = 1) AS active_business_systems
FROM t_subscription s
LEFT JOIN t_software_package sp ON s.software_package_id = sp.id
WHERE s.deleted = 0
GROUP BY DATE(s.created_at)
ON DUPLICATE KEY UPDATE
    total_packages = VALUES(total_packages),
    new_packages = VALUES(new_packages),
    total_subscriptions = VALUES(total_subscriptions),
    new_subscriptions = VALUES(new_subscriptions),
    approved_subscriptions = VALUES(approved_subscriptions);
```

### 6.2 定时任务（可选）

```sql
-- 每日凌晨计算前一天的统计数据
-- 可由应用层定时任务执行，或数据库事件调度器

-- MySQL Event Scheduler 示例
DELIMITER $$

CREATE EVENT IF NOT EXISTS evt_calc_daily_statistics
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE, '02:00:00')
DO
BEGIN
    -- 插入或更新昨日统计
    INSERT INTO t_statistics_daily (
        stat_date, total_packages, new_packages,
        total_subscriptions, new_subscriptions, approved_subscriptions,
        active_business_systems
    )
    SELECT
        DATE_SUB(CURDATE(), INTERVAL 1 DAY),
        (SELECT COUNT(*) FROM t_software_package WHERE status = 'PUBLISHED'),
        (SELECT COUNT(*) FROM t_software_package
         WHERE status = 'PUBLISHED' AND DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
        (SELECT COUNT(*) FROM t_subscription WHERE deleted = 0),
        (SELECT COUNT(*) FROM t_subscription
         WHERE deleted = 0 AND DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
        (SELECT COUNT(*) FROM t_subscription
         WHERE deleted = 0 AND status = 'APPROVED'
         AND DATE(updated_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
        (SELECT COUNT(*) FROM t_business_system WHERE enabled = 1)
    ON DUPLICATE KEY UPDATE
        total_packages = VALUES(total_packages),
        new_packages = VALUES(new_packages),
        total_subscriptions = VALUES(total_subscriptions),
        new_subscriptions = VALUES(new_subscriptions),
        approved_subscriptions = VALUES(approved_subscriptions),
        active_business_systems = VALUES(active_business_systems);
END$$

DELIMITER ;

-- 启用事件调度器
SET GLOBAL event_scheduler = ON;
```

---

## 7. 性能优化建议

### 7.1 索引优化

确保以下索引已存在：

```sql
-- 订购表时间索引（用于趋势查询）
ALTER TABLE t_subscription ADD KEY idx_created_at (created_at);
ALTER TABLE t_subscription ADD KEY idx_status_created_at (status, created_at);

-- 订购表业务系统索引（用于分布查询）
ALTER TABLE t_subscription ADD KEY idx_business_system (business_system_id, deleted);
```

### 7.2 查询优化

1. **趋势查询**: 大数据量时改用统计快照表，避免实时聚合
2. **排行查询**: 限制时间范围，或使用预计算表
3. **缓存策略**:
   - 概览数据：Redis缓存5分钟
   - 趋势数据：缓存1小时
   - 排行榜：缓存30分钟

### 7.3 分页策略

对于排行榜等数据，采用应用层分页：

```java
// 示例：先查询ID列表，再查详情
List<Long> topPackageIds = statisticsRepository.findTopPackageIds(limit);
List<PackageStats> details = packageRepository.findStatsByIds(topPackageIds);
```

---

## 8. 数据归档

### 8.1 归档策略

- 统计快照表数据量小（365条/年），无需归档
- 软件包每日统计表可保留最近2年数据
- 历史数据可导出为 CSV 后删除

### 8.2 清理脚本

```sql
-- 清理2年前的详细统计数据
DELETE FROM t_statistics_package_daily
WHERE stat_date < DATE_SUB(CURDATE(), INTERVAL 2 YEAR);
```

---

## 9. 附录

### 9.1 相关表

| 表名 | 关系 | 说明 |
|------|------|------|
| t_software_package | 引用 | 软件包基础信息 |
| t_subscription | 引用 | 订购数据 |
| t_business_system | 引用 | 业务系统信息 |

### 9.2 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2026-03-25 | v1.0 | 初始版本 | Claude |
