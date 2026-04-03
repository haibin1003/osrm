# 存储后端数据库设计

## 表结构

### t_storage_backend（存储后端表）

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGSERIAL | 是 | 主键，自增 |
| backend_code | VARCHAR(32) | 是 | 后端编码，唯一 |
| backend_name | VARCHAR(64) | 是 | 显示名称，唯一 |
| backend_type | VARCHAR(20) | 是 | 类型：HARBOR/NEXUS/NAS |
| endpoint | VARCHAR(256) | 是 | 服务端点地址 |
| access_key | VARCHAR(128) | 否 | 认证用户名 |
| secret_key | VARCHAR(256) | 否 | 认证密码（加密存储） |
| namespace | VARCHAR(64) | 否 | 默认命名空间/项目 |
| config_json | JSON | 否 | 扩展配置（JSON格式）|
| is_default | BOOLEAN | 是 | 是否默认后端，默认FALSE |
| enabled | BOOLEAN | 是 | 是否启用，默认TRUE |
| health_status | VARCHAR(20) | 是 | 健康状态：ONLINE/OFFLINE/ERROR/UNKNOWN |
| last_health_check | TIMESTAMP | 否 | 最后健康检查时间 |
| error_message | VARCHAR(500) | 否 | 错误信息 |
| description | VARCHAR(512) | 否 | 描述说明 |
| created_by | BIGINT | 否 | 创建人ID |
| created_at | TIMESTAMP | 是 | 创建时间 |
| updated_at | TIMESTAMP | 是 | 更新时间 |

## 索引

```sql
CREATE INDEX idx_storage_backend_type ON t_storage_backend(backend_type);
CREATE INDEX idx_storage_backend_status ON t_storage_backend(health_status);
CREATE INDEX idx_storage_backend_enabled ON t_storage_backend(enabled);
```

## 数据库迁移脚本

```sql
-- 添加新字段
ALTER TABLE t_storage_backend 
ADD COLUMN IF NOT EXISTS config_json JSON,
ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_storage_backend_type ON t_storage_backend(backend_type);
CREATE INDEX IF NOT EXISTS idx_storage_backend_status ON t_storage_backend(health_status);
```

## config_json 结构定义

### Harbor 配置

```json
{
  "protocol": "HTTP",
  "apiVersion": "v2.0",
  "project": "osrm"
}
```

### Nexus 配置

```json
{
  "protocol": "HTTP",
  "mavenRepo": "maven-releases",
  "npmRepo": "npm-hosted",
  "pypiRepo": "pypi-hosted",
  "rawRepo": "raw-hosted"
}
```

### NAS 配置

```json
{
  "capacityLimit": 1073741824,
  "currentUsage": 536870912
}
```
