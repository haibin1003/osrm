# OSRM 部署文档

> 适用于 CentOS 9 + Docker 环境，外部 MySQL/Redis。

## 一、环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 需包含 Docker Compose v2 |
| MySQL | 8.0+ | 已运行于 `114.66.38.81:3306` |
| Redis | 6.0+ | 已运行于 `114.66.38.81:6379` |

## 二、快速部署

### 2.1 克隆项目

```bash
cd /opt
git clone git@github.com:haibin1003/osrm.git
cd osrm
```

### 2.2 配置环境变量（可选）

```bash
# 生产环境建议修改以下变量，不设置则使用默认值
export JWT_SECRET="your-256-bit-secret-key-change-in-production"
export MYSQL_USERNAME="osrm"
export MYSQL_PASSWORD="your-db-password"
export REDIS_PASSWORD="your-redis-password"
```

### 2.3 构建并启动

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

首次构建约 3-5 分钟（下载依赖 + Maven 编译 + npm 构建）。启动后 Flyway 自动执行数据库迁移。

### 2.4 验证

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 登录测试
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

浏览器访问 `http://<服务器IP>` 即可打开系统。

## 三、目录结构

```
/opt/osrm/
├── docker/
│   ├── docker-compose.yml      # 编排文件
│   ├── Dockerfile.backend      # 后端构建（Maven 多阶段）
│   ├── Dockerfile.frontend     # 前端构建（Node + Nginx 多阶段）
│   ├── nginx.conf              # 前端 Nginx 配置
│   └── application-docker.yml  # Docker 专用配置（备用）
├── osrm-backend/               # Spring Boot 后端
├── osrm-frontend/              # Vue 3 前端
├── osrm-deploy/                # Flyway 数据库迁移脚本
│   └── sql/flyway/
│       ├── baseline/           # V1 基线
│       └── migration/          # V2 ~ V12 增量迁移
└── DEPLOY.md                   # 本文档
```

## 四、配置说明

### 4.1 数据库

MySQL 连接配置在 `osrm-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://114.66.38.81:3306/osrm?useSSL=false&...
    username: root
    password: root123
```

通过环境变量可覆盖用户名和密码：`MYSQL_USERNAME`、`MYSQL_PASSWORD`。

数据库迁移由 **Flyway** 管理，首次启动自动执行 `osrm-deploy/sql/flyway/` 下全部迁移脚本（V1 ~ V12），无需手动执行 SQL。

### 4.2 Redis

```yaml
spring:
  data:
    redis:
      host: 114.66.38.81
      port: 6379
      password: redis123
```

通过 `REDIS_PASSWORD` 环境变量可覆盖密码。

### 4.3 JWT 密钥

生产环境务必修改：

```bash
export JWT_SECRET="至少256位的随机字符串"
```

### 4.4 服务端口

| 服务 | 容器端口 | 宿主机端口 |
|------|----------|------------|
| 后端 | 8080 | 8080 |
| 前端 | 80 | 80 |

修改宿主机端口：编辑 `docker/docker-compose.yml` 中的 `ports` 映射。

## 五、运维命令

### 查看日志

```bash
# 后端
docker logs osrm-backend --tail 100 -f

# 前端
docker logs osrm-frontend --tail 50 -f
```

### 重启服务

```bash
docker compose -f docker/docker-compose.yml restart
```

### 更新部署

```bash
git pull
docker compose -f docker/docker-compose.yml up -d --build
```

### 停止服务

```bash
docker compose -f docker/docker-compose.yml down
```

## 六、数据库备份

```bash
# 备份数据库
mysqldump -h 114.66.38.81 -u root -p osrm > osrm_backup_$(date +%Y%m%d).sql

# 恢复
mysql -h 114.66.38.81 -u root -p osrm < osrm_backup_20260101.sql
```

## 七、常见问题

### 构建失败

确保 Docker 版本 20.10+ 且磁盘空间充足（构建需要约 2GB 临时空间）。

### 数据库连接失败

```bash
# 检查容器内到 MySQL 的网络连通性
docker exec osrm-backend wget -q -O- 114.66.38.81:3306 || echo "无法连接 MySQL"

# 检查防火墙
firewall-cmd --list-ports
```

### Flyway 迁移失败

如果迁移中途失败，可能需要手动修复后再重启：

```sql
-- 查看当前迁移状态
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- 删除失败的迁移记录后重启应用
DELETE FROM flyway_schema_history WHERE success = 0;
```

### 前端页面 404

检查 Nginx 是否正确代理 API 请求。确认 `docker/nginx.conf` 中 `/api/` 指向 `http://osrm-backend:8080/api/`。

## 八、初始账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 系统管理员 |
