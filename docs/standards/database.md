# 数据库规范

本规范定义 OSRM 项目的数据库设计标准，包括命名规范、字段设计、索引策略和迁移管理。

---

## 文档信息

- **作者**: OSRM 开发团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-17
- **维护责任人**: 技术负责人

---

## 命名规范

### 表名

| 规则 | 示例 | 说明 |
|------|------|------|
| 使用 `t_` 前缀 | `t_software_package` | 区分表和其他数据库对象 |
| 使用小写下划线 | `t_user_role` | 提高可读性 |
| 使用名词单数 | `t_user` | 语义清晰 |
| 避免保留字 | 避免 `t_order` | 防止冲突 |

**常用表名示例**：
```sql
t_user                    -- 用户表
t_software_package        -- 软件包表
t_package_version         -- 软件包版本表
t_review_record           -- 审核记录表
t_subscription            -- 订购记录表
t_system_config           -- 系统配置表
```

### 字段名

| 规则 | 示例 | 说明 |
|------|------|------|
| 使用小写下划线 | `created_at` | 统一风格 |
| 避免使用数据类型名 | 避免 `int_value` | 防止混淆 |
| 布尔字段使用 is/has 前缀 | `is_active`, `has_permission` | 语义清晰 |
| 时间字段使用后缀 | `_at` 表示时间点, `_time` 表示时长 | 区分时间类型 |
| 外键字段使用 `_id` 后缀 | `user_id`, `package_id` | 表明关联 |

**常用字段名**：
```sql
id                    -- 主键
created_at            -- 创建时间
updated_at            -- 更新时间
created_by            -- 创建人
updated_by            -- 更新人
is_deleted            -- 逻辑删除标记（软删除）
version               -- 乐观锁版本号
```

### 索引名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 普通索引 | `idx_{表名}_{字段名}` | `idx_package_name` |
| 唯一索引 | `uk_{表名}_{字段名}` | `uk_package_code` |
| 复合索引 | `idx_{表名}_{字段1}_{字段2}` | `idx_package_status_created_at` |

### 约束名

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 主键 | `pk_{表名}` | `pk_software_package` |
| 外键 | `fk_{表名}_{关联表名}` | `fk_package_user` |
| 唯一约束 | `uk_{表名}_{字段名}` | `uk_user_username` |
| 检查约束 | `ck_{表名}_{规则名}` | `ck_package_status` |

---

## 表结构设计

### 必备字段

每个表必须包含以下字段：

```sql
CREATE TABLE t_example (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    created_at      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by      VARCHAR(64)         COMMENT '创建人',
    updated_by      VARCHAR(64)         COMMENT '更新人',
    is_deleted      TINYINT             NOT NULL DEFAULT 0 COMMENT '逻辑删除标记: 0-未删除, 1-已删除',
    version         INT                 NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',

    -- 业务字段...

    INDEX idx_created_at (created_at),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表示例';
```

### 字段类型选择

| 数据类型 | 适用场景 | 示例 |
|----------|----------|------|
| `BIGINT UNSIGNED` | 主键ID、大整数 | `id`, `user_count` |
| `INT` | 普通整数、状态码 | `status`, `sort_order` |
| `TINYINT` | 布尔值、小范围枚举 | `is_active`, `type` (1-5) |
| `VARCHAR(n)` | 变长字符串 | `username` (≤64), `email` (≤128) |
| `CHAR(n)` | 定长字符串 | `country_code` (2) |
| `TEXT` | 长文本 | `description`, `content` |
| `DECIMAL(p,s)` | 精确小数（金额） | `price DECIMAL(10,2)` |
| `TIMESTAMP` | 日期时间 | `created_at` |
| `DATE` | 仅日期 | `birth_date` |
| `JSON` | 结构化数据 | `config`, `metadata` |

### 字段设计原则

1. **NOT NULL 优先**：尽量使用 NOT NULL，使用默认值替代 NULL
2. **合理长度**：VARCHAR 长度根据实际需求设置，不宜过大
3. **避免 TEXT/BLOB**：除非必要，否则不使用大字段
4. **枚举使用 TINYINT**：不用 ENUM 类型，用 TINYINT + 代码常量

---

## 索引设计

### 索引原则

1. **选择性原则**：高选择性字段适合建索引（如用户名），低选择性不适合（如性别）
2. **最左前缀**：复合索引按查询条件顺序设计
3. **避免冗余**：不创建重复索引
4. **控制数量**：单表索引不超过 5 个

### 必须创建索引的场景

- 主键自动创建唯一索引
- 外键字段
- 经常用于 WHERE 条件的字段
- 经常用于 ORDER BY 的字段
- 经常用于 JOIN 的字段

### 复合索引设计

```sql
-- 查询条件: WHERE status = ? AND created_at > ? ORDER BY created_at
-- 复合索引字段顺序：等值条件在前，范围条件在后，排序字段最后
CREATE INDEX idx_status_created_at
ON t_software_package(status, created_at);
```

### 索引示例

```sql
-- 用户表索引
CREATE INDEX idx_username ON t_user(username);
CREATE INDEX idx_email ON t_user(email);
CREATE UNIQUE INDEX uk_username ON t_user(username) WHERE is_deleted = 0;

-- 软件包表索引
CREATE INDEX idx_name ON t_software_package(name);
CREATE INDEX idx_status_created_at ON t_software_package(status, created_at);
CREATE INDEX idx_created_by ON t_software_package(created_by);
```

---

## 迁移管理

### 使用 Flyway

项目使用 Flyway 进行数据库版本管理。

### 迁移脚本命名规范

```
V{版本号}__{描述}.sql
```

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 版本迁移 | `V{数字}__{描述}.sql` | `V1__init_schema.sql` |
| 重复迁移 | `R__{描述}.sql` | `R__create_report_view.sql` |
| 撤销迁移 | `U{数字}__{描述}.sql` | `U2__drop_user_table.sql` |

### 版本号规则

- 使用数字，如 `1`, `2`, `2.1`
- 每提交一个迁移文件，版本号递增
- 版本号必须唯一，不能重复

### 迁移脚本示例

```sql
-- V1__init_schema.sql
-- 创建用户表
CREATE TABLE t_user (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(64)         NOT NULL,
    email           VARCHAR(128)        NOT NULL,
    password_hash   VARCHAR(255)        NOT NULL,
    status          TINYINT             NOT NULL DEFAULT 1 COMMENT '1-启用, 0-禁用',
    created_at      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT             NOT NULL DEFAULT 0,
    version         INT                 NOT NULL DEFAULT 1,

    UNIQUE KEY uk_username (username),
    INDEX idx_email (email),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建软件包表
CREATE TABLE t_software_package (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(128)        NOT NULL COMMENT '软件包名称',
    code            VARCHAR(64)         NOT NULL COMMENT '软件包编码',
    description     TEXT                COMMENT '描述',
    source_url      VARCHAR(512)        COMMENT '源码地址',
    status          TINYINT             NOT NULL DEFAULT 0 COMMENT '0-草稿, 1-审核中, 2-已发布, 3-已归档',
    created_by      BIGINT UNSIGNED     NOT NULL,
    created_at      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted      TINYINT             NOT NULL DEFAULT 0,
    version         INT                 NOT NULL DEFAULT 1,

    UNIQUE KEY uk_code (code),
    INDEX idx_name (name),
    INDEX idx_status_created_at (status, created_at),
    INDEX idx_created_by (created_by),
    CONSTRAINT fk_package_created_by FOREIGN KEY (created_by) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='软件包表';
```

### 迁移脚本原则

1. **原子性**：每个脚本应该原子执行，要么全部成功，要么全部失败
2. **幂等性**：脚本可以重复执行而不产生错误（使用 `IF NOT EXISTS`）
3. **向前兼容**：只添加修改，不删除已有结构（除非特别设计）
4. **禁止修改已执行脚本**：已执行的迁移脚本不可修改

### 迁移命令

```bash
# 查看当前版本
./mvnw flyway:info

# 执行迁移
./mvnw flyway:migrate

# 撤销上次迁移（谨慎使用）
./mvnw flyway:undo

# 修复元数据表
./mvnw flyway:repair
```

---

## 软删除实现

### 方案

使用 `is_deleted` 字段标记删除状态，而不是物理删除。

### 查询过滤

应用程序层自动添加删除过滤：

```java
// MyBatis-Plus 配置
@Configuration
public class MybatisConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // 自动添加 is_deleted = 0 条件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}

// Entity 基类
@MappedSuperclass
public abstract class BaseEntity {
    @TableLogic
    private Integer isDeleted;
}
```

---

## 乐观锁实现

### 方案

使用 `version` 字段实现乐观锁，防止并发更新冲突。

```java
@Entity
public class SoftwarePackage extends BaseEntity {
    @Version
    private Integer version;

    // ...
}
```

---

## 变更记录

| 时间 | 变更内容 | 变更原因 | 影响范围 | 处理人 | 状态 |
|------|----------|----------|----------|--------|------|
| 2026-03-17 | 初始版本 | 建立数据库规范 | 全部 | 技术负责人 | 已完成 |

