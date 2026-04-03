# OSRM 开源软件仓库管理系统 - 部署指南

## 环境要求

- **操作系统**: CentOS 9 Stream
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **外部服务** (用户已提供):
  - MySQL 8.0+ (地址: 114.66.38.81:3306)
  - Redis (与 MySQL 同地址)
  - MinIO (地址: 114.66.38.81:9000)

---

## 一、数据库初始化

### 1.1 创建 OSRM 数据库

连接 MySQL 服务器，执行：

```sql
CREATE DATABASE IF NOT EXISTS osrm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 1.2 创建 AI 助手数据库

```sql
CREATE DATABASE IF NOT EXISTS ai_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 1.3 执行 OSRM 数据库脚本

```bash
# OSRM 表结构
mysql -h 114.66.38.81 -u root -p osrm -e "source /path/to/osrm-deploy/sql/01-schema-mysql.sql"

# OSRM 基础数据 (角色、权限、用户、分类等)
mysql -h 114.66.38.81 -u root -p osrm -e "source /path/to/osrm-deploy/sql/02-data-mysql.sql"
```

### 1.4 执行 AI 助手数据库脚本

```bash
# AI 助手表结构
mysql -h 114.66.38.81 -u root -p ai_assistant -e "source /path/to/osrm-deploy/sql/03-ai-assistant-schema.sql"

# AI 助手初始数据 (技能包)
mysql -h 114.66.38.81 -u root -p ai_assistant -e "source /path/to/osrm-deploy/sql/04-ai-assistant-data.sql"
```

### 1.5 数据库初始化清单

#### OSRM 数据库 (osrm)

| 顺序 | 脚本文件 | 说明 | 必须执行 |
|------|----------|------|----------|
| 1 | `01-schema-mysql.sql` | 创建所有表结构 | **是** |
| 2 | `02-data-mysql.sql` | 初始化角色、权限、用户、分类等基础数据 | **是** |

#### AI 助手数据库 (ai_assistant)

| 顺序 | 脚本文件 | 说明 | 必须执行 |
|------|----------|------|----------|
| 1 | `03-ai-assistant-schema.sql` | 创建所有表结构 | **是** |
| 2 | `04-ai-assistant-data.sql` | 初始化技能包数据 | **是** |

### 1.6 初始账号

#### OSRM 系统
- **用户名**: admin
- **密码**: admin123
- **角色**: 系统管理员

#### AI 助手
- AI 助手无需登录，通过 API 访问

---

## 二、服务部署

### 2.1 OSRM 后端部署 (osrm-backend)

#### 构建 JAR 文件

```bash
cd /path/to/osrm-backend
mvn clean package -DskipTests
```

#### 配置 application.yml

创建 `config/application.yml`:

```yaml
server:
  port: 8080

spring:
  application:
    name: osrm-backend

  datasource:
    url: jdbc:mysql://114.66.38.81:3306/osrm?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  flyway:
    enabled: false

  redis:
    host: 114.66.38.81
    port: 6379
    password:
    database: 0

osrm:
  storage:
    type: local
    local:
      path: /data/osrm/artifacts
    minio:
      enabled: true
      endpoint: http://114.66.38.81:9000
      access-key: minioadmin
      secret-key: minioadmin123
      bucket: osrm

  harbor:
    enabled: false
    url: http://114.66.38.81:8080
    username: admin
    password: Harbor12345

  nexus:
    enabled: false
    url: http://114.66.38.81:8081
    username: admin
    password: 14cdf79a-e549-45c5-80de-245395c6c293

logging:
  level:
    root: INFO
    com.osrm: INFO
```

#### Docker 部署

创建 `Dockerfile.backend`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/osrm-backend-*.jar app.jar
COPY config /app/config
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.config.location=/app/config/", "app.jar"]
```

构建和运行:

```bash
docker build -f Dockerfile.backend -t osrm-backend:latest .
docker run -d \
  --name osrm-backend \
  -p 8080:8080 \
  -v /path/to/config:/app/config \
  -v /data/osrm/artifacts:/data/osrm/artifacts \
  osrm-backend:latest
```

---

### 2.2 OSRM 前端部署 (osrm-frontend)

#### 构建前端

```bash
cd /path/to/osrm-frontend
npm install
npm run build
```

#### Docker 部署

创建 `nginx.conf`:

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

创建 `Dockerfile.frontend`:

```dockerfile
FROM nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

构建和运行:

```bash
docker build -f Dockerfile.frontend -t osrm-frontend:latest .
docker run -d \
  --name osrm-frontend \
  -p 80:80 \
  osrm-frontend:latest
```

---

### 2.3 AI 助手部署 (ai-assistant-service)

#### 构建 JAR 文件

```bash
cd /path/to/ai-assistant-service
mvn clean package -DskipTests
```

#### 配置 application.yml

创建 `config/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: ai-assistant-service

  datasource:
    url: jdbc:mysql://114.66.38.81:3306/ai_assistant?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  flyway:
    enabled: false

  jackson:
    serialization:
      write-dates-as-timestamps: false
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

ai-assistant:
  encryption:
    key: dev-encryption-key-32-characters-long

  session:
    default-expire-hours: 24
    cleanup-interval-ms: 3600000

  chat:
    max-tool-rounds: 10
    max-history-messages: 20

  llm:
    default-provider: deepseek
    deepseek:
      model: deepseek-chat
      timeout-ms: 120000
      api-url: https://api.deepseek.com/v1/chat/completions

  search:
    default-provider: serper
    serper:
      api-url: https://google.serper.dev/search
      timeout-ms: 30000

  browser:
    enabled: false

  skill:
    minio:
      enabled: false

  document:
    storage:
      path: /data/ai-assistant/documents
    minio:
      enabled: true
    expire:
      hours: 24

  minio:
    enabled: true
    endpoint: http://114.66.38.81:9000
    access-key: minioadmin
    secret-key: minioadmin123
    bucket: ai-assistant

  mcp:
    timeout-ms: 30000
    retry-count: 2

logging:
  level:
    root: INFO
    com.ai.assistant: INFO
```

#### Docker 部署

创建 `Dockerfile.ai-assistant`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/ai-assistant-service-*.jar app.jar
COPY config /app/config
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Dspring.config.location=/app/config/", "app.jar"]
```

构建和运行:

```bash
docker build -f Dockerfile.ai-assistant -t ai-assistant-service:latest .
docker run -d \
  --name ai-assistant-service \
  -p 8081:8081 \
  -v /path/to/config:/app/config \
  -v /data/ai-assistant/documents:/data/ai-assistant/documents \
  ai-assistant-service:latest
```

#### AI 助手 API 端点

- **基础 URL**: `http://localhost:8081`
- **健康检查**: `GET /health`
- **会话管理**: `POST /api/v1/chat/sessions`
- **发送消息**: `POST /api/v1/chat/sessions/{sessionId}/messages`
- **生成文档**: `POST /api/v1/documents/generate`
- **获取文档**: `GET /api/v1/documents/{documentId}`

---

## 三、使用 Docker Compose 部署 (推荐)

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  osrm-backend:
    build:
      context: ./osrm-backend
      dockerfile: Dockerfile.backend
    container_name: osrm-backend
    ports:
      - "8080:8080"
    volumes:
      - ./osrm-backend/config:/app/config
      - osrm-data:/data/osrm
    restart: unless-stopped

  osrm-frontend:
    build:
      context: ./osrm-frontend
      dockerfile: Dockerfile.frontend
    container_name: osrm-frontend
    ports:
      - "80:80"
    depends_on:
      - osrm-backend
    restart: unless-stopped

  ai-assistant-service:
    build:
      context: ./ai-assistant-service
      dockerfile: Dockerfile.ai-assistant
    container_name: ai-assistant-service
    ports:
      - "8081:8081"
    volumes:
      - ./ai-assistant-service/config:/app/config
      - ai-assistant-data:/data/ai-assistant
    depends_on:
      - osrm-backend
    restart: unless-stopped

volumes:
  osrm-data:
  ai-assistant-data:
```

部署:

```bash
docker-compose up -d
```

---

## 四、验证部署

### 4.1 检查服务状态

```bash
# 检查容器状态
docker ps

# 检查 OSRM 后端日志
docker logs osrm-backend --tail 100

# 检查 OSRM 前端日志
docker logs osrm-frontend --tail 50

# 检查 AI 助手日志
docker logs ai-assistant-service --tail 100
```

### 4.2 OSRM API 健康检查

```bash
curl http://localhost:8080/api/v1/health
```

预期响应:

```json
{"code":200,"message":"success","data":"OK"}
```

### 4.3 AI 助手健康检查

```bash
curl http://localhost:8081/health
```

### 4.4 访问系统

| 服务 | 地址 |
|------|------|
| OSRM 前端 | `http://<服务器IP>` |
| OSRM 后端 API | `http://localhost:8080/api/v1` |
| AI 助手 API | `http://localhost:8081` |

#### OSRM 初始账号
- **用户名**: admin
- **密码**: admin123

---

## 五、常见问题

### 5.1 数据库连接失败

```bash
# 检查 MySQL 连接
mysql -h 114.66.38.81 -u root -p -e "SELECT 1"

# 检查防火墙
firewall-cmd --list-all
```

### 5.2 前端访问 API 404

检查 Nginx 配置是否正确代理 `/api/` 到 `http://localhost:8080`

### 5.3 AI 助手文档生成失败

确保 MinIO 配置正确，且 AI 助手有 `/data/ai-assistant/documents` 目录写入权限

---

## 六、数据备份

### 备份 OSRM 数据库

```bash
mysqldump -h 114.66.38.81 -u root -p osrm > osrm_backup_$(date +%Y%m%d).sql
```

### 备份 AI 助手数据库

```bash
mysqldump -h 114.66.38.81 -u root -p ai_assistant > ai_assistant_backup_$(date +%Y%m%d).sql
```

### 备份 MinIO 文件

```bash
mc mirror minio/ai-assistant /backup/ai-assistant/
mc mirror minio/osrm /backup/osrm/
```
