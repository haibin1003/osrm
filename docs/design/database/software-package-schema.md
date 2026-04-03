# 数据库设计文档：软件包管理

**功能编号**: REQ-300
**文档日期**: 2026-03-20

---

## 1. 表结构概览

| 表名 | 说明 | 记录数预估 |
|------|------|-----------|
| t_software_package | 软件包主表 | 1000+ |
| t_software_version | 软件版本表 | 5000+ |

---

## 2. 软件包表 (t_software_package)

### 2.1 表定义

```sql
CREATE TABLE t_software_package (
    id BIGSERIAL PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL,
    package_key VARCHAR(64) NOT NULL,
    software_type VARCHAR(32) NOT NULL,
    category_id BIGINT,
    description TEXT,
    website_url VARCHAR(256),
    license_type VARCHAR(64),
    license_url VARCHAR(256),
    source_url VARCHAR(256),
    logo_url VARCHAR(256),
    current_version VARCHAR(32),
    view_count INT DEFAULT 0,
    download_count INT DEFAULT 0,
    subscription_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by BIGINT,
    published_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_package_key UNIQUE (package_key),
    CONSTRAINT uk_package_name UNIQUE (package_name)
);

COMMENT ON TABLE t_software_package IS '软件包主表';
COMMENT ON COLUMN t_software_package.id IS '主键ID';
COMMENT ON COLUMN t_software_package.package_name IS '软件包显示名称';
COMMENT ON COLUMN t_software_package.package_key IS '唯一标识，用于URL';
COMMENT ON COLUMN t_software_package.software_type IS '软件类型：DOCKER_IMAGE/HELM_CHART/MAVEN/NPM/PYPI/GENERIC';
COMMENT ON COLUMN t_software_package.category_id IS '分类ID（可选）';
COMMENT ON COLUMN t_software_package.description IS '详细描述，支持Markdown';
COMMENT ON COLUMN t_software_package.website_url IS '官网链接';
COMMENT ON COLUMN t_software_package.license_type IS '许可证类型';
COMMENT ON COLUMN t_software_package.license_url IS '许可证链接';
COMMENT ON COLUMN t_software_package.source_url IS '源码仓库链接';
COMMENT ON COLUMN t_software_package.logo_url IS 'Logo图片URL';
COMMENT ON COLUMN t_software_package.current_version IS '当前最新版本（冗余字段）';
COMMENT ON COLUMN t_software_package.view_count IS '浏览次数';
COMMENT ON COLUMN t_software_package.download_count IS '下载次数';
COMMENT ON COLUMN t_software_package.subscription_count IS '订购次数';
COMMENT ON COLUMN t_software_package.status IS '状态：DRAFT/PENDING/PUBLISHED/OFFLINE/ARCHIVED';
COMMENT ON COLUMN t_software_package.created_by IS '创建人ID';
COMMENT ON COLUMN t_software_package.created_at IS '创建时间';
COMMENT ON COLUMN t_software_package.updated_at IS '更新时间';
COMMENT ON COLUMN t_software_package.published_by IS '发布人ID';
COMMENT ON COLUMN t_software_package.published_at IS '发布时间';
COMMENT ON COLUMN t_software_package.version IS '乐观锁版本号';
```

### 2.2 索引

```sql
-- 唯一索引
CREATE UNIQUE INDEX uk_package_key ON t_software_package(package_key);
CREATE UNIQUE INDEX uk_package_name ON t_software_package(package_name);

-- 查询索引
CREATE INDEX idx_package_status ON t_software_package(status);
CREATE INDEX idx_package_type ON t_software_package(software_type);
CREATE INDEX idx_package_category ON t_software_package(category_id);
CREATE INDEX idx_package_created_at ON t_software_package(created_at DESC);

-- 统计索引
CREATE INDEX idx_package_view_count ON t_software_package(view_count DESC);
CREATE INDEX idx_package_download_count ON t_software_package(download_count DESC);
```

### 2.3 枚举值定义

**software_type**:
- `DOCKER_IMAGE`: Docker镜像
- `HELM_CHART`: Helm Chart
- `MAVEN`: Maven组件
- `NPM`: NPM包
- `PYPI`: PyPI包
- `GENERIC`: 通用文件

**status**:
- `DRAFT`: 草稿
- `PENDING`: 待审核
- `PUBLISHED`: 已发布
- `OFFLINE`: 已下架
- `ARCHIVED`: 已归档

---

## 3. 软件版本表 (t_software_version)

### 3.1 表定义

```sql
CREATE TABLE t_software_version (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL,
    version_no VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    storage_backend_id BIGINT NOT NULL,
    storage_path VARCHAR(512),
    artifact_url VARCHAR(512),
    release_notes TEXT,
    file_size BIGINT,
    checksum VARCHAR(128),
    is_latest BOOLEAN DEFAULT FALSE,
    published_by BIGINT,
    published_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_version_package_version_no UNIQUE (package_id, version_no),
    CONSTRAINT fk_version_package FOREIGN KEY (package_id)
        REFERENCES t_software_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_version_storage_backend FOREIGN KEY (storage_backend_id)
        REFERENCES t_storage_backend(id)
);

COMMENT ON TABLE t_software_version IS '软件版本表';
COMMENT ON COLUMN t_software_version.id IS '主键ID';
COMMENT ON COLUMN t_software_version.package_id IS '关联的软件包ID';
COMMENT ON COLUMN t_software_version.version_no IS '版本号，如8.0.33';
COMMENT ON COLUMN t_software_version.status IS '版本状态：DRAFT/PUBLISHED/OFFLINE';
COMMENT ON COLUMN t_software_version.storage_backend_id IS '存储后端ID';
COMMENT ON COLUMN t_software_version.storage_path IS '存储路径';
COMMENT ON COLUMN t_software_version.artifact_url IS '制品下载URL';
COMMENT ON COLUMN t_software_version.release_notes IS '发行说明';
COMMENT ON COLUMN t_software_version.file_size IS '文件大小（字节）';
COMMENT ON COLUMN t_software_version.checksum IS '文件校验和';
COMMENT ON COLUMN t_software_version.is_latest IS '是否最新版本';
COMMENT ON COLUMN t_software_version.published_by IS '发布人ID';
COMMENT ON COLUMN t_software_version.published_at IS '发布时间';
COMMENT ON COLUMN t_software_version.created_by IS '创建人ID';
COMMENT ON COLUMN t_software_version.created_at IS '创建时间';
COMMENT ON COLUMN t_software_version.updated_at IS '更新时间';
```

### 3.2 索引

```sql
-- 唯一索引
CREATE UNIQUE INDEX uk_version_package_version_no ON t_software_version(package_id, version_no);

-- 查询索引
CREATE INDEX idx_version_package_id ON t_software_version(package_id);
CREATE INDEX idx_version_status ON t_software_version(status);
CREATE INDEX idx_version_storage_backend ON t_software_version(storage_backend_id);
CREATE INDEX idx_version_is_latest ON t_software_version(package_id, is_latest) WHERE is_latest = TRUE;
```

### 3.3 枚举值定义

**status**:
- `DRAFT`: 草稿
- `PUBLISHED`: 已发布
- `OFFLINE`: 已下线

---

## 4. 关联关系

```
t_software_package ||--o{ t_software_version : has
t_software_package }o--|| t_category : belongs_to
t_software_version }o--|| t_storage_backend : stored_in
t_software_package }o--|| t_user : created_by
t_software_package }o--|| t_user : published_by
t_software_version }o--|| t_user : created_by
t_software_version }o--|| t_user : published_by
```

---

## 5. 数据迁移脚本

```sql
-- 创建软件包表
CREATE TABLE IF NOT EXISTS t_software_package (
    id BIGSERIAL PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL,
    package_key VARCHAR(64) NOT NULL,
    software_type VARCHAR(32) NOT NULL,
    category_id BIGINT,
    description TEXT,
    website_url VARCHAR(256),
    license_type VARCHAR(64),
    license_url VARCHAR(256),
    source_url VARCHAR(256),
    logo_url VARCHAR(256),
    current_version VARCHAR(32),
    view_count INT DEFAULT 0,
    download_count INT DEFAULT 0,
    subscription_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by BIGINT,
    published_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_package_key UNIQUE (package_key),
    CONSTRAINT uk_package_name UNIQUE (package_name)
);

-- 创建软件版本表
CREATE TABLE IF NOT EXISTS t_software_version (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL,
    version_no VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    storage_backend_id BIGINT NOT NULL,
    storage_path VARCHAR(512),
    artifact_url VARCHAR(512),
    release_notes TEXT,
    file_size BIGINT,
    checksum VARCHAR(128),
    is_latest BOOLEAN DEFAULT FALSE,
    published_by BIGINT,
    published_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_version_package_version_no UNIQUE (package_id, version_no),
    CONSTRAINT fk_version_package FOREIGN KEY (package_id)
        REFERENCES t_software_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_version_storage_backend FOREIGN KEY (storage_backend_id)
        REFERENCES t_storage_backend(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_package_status ON t_software_package(status);
CREATE INDEX IF NOT EXISTS idx_package_type ON t_software_package(software_type);
CREATE INDEX IF NOT EXISTS idx_package_category ON t_software_package(category_id);
CREATE INDEX IF NOT EXISTS idx_version_package_id ON t_software_version(package_id);
CREATE INDEX IF NOT EXISTS idx_version_status ON t_software_version(status);
CREATE INDEX IF NOT EXISTS idx_version_storage_backend ON t_software_version(storage_backend_id);
CREATE INDEX IF NOT EXISTS idx_version_is_latest ON t_software_version(package_id, is_latest) WHERE is_latest = TRUE;
```

---

## 6. 性能优化建议

### 6.1 查询优化

1. **列表查询**: 使用复合索引 `(status, created_at DESC)`
2. **类型筛选**: 使用索引 `(software_type, status)`
3. **全文搜索**: 后续考虑引入 Elasticsearch 实现全文检索

### 6.2 统计优化

1. **计数统计**: 使用 Redis 缓存热门软件包的计数
2. **排行榜**: 每日定时任务生成热门榜单缓存

---

**文档版本**: v1.0
**最后更新**: 2026-03-20
