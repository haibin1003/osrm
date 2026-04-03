# OSRM 部署架构设计

本文档描述 OSRM（开源软件仓库管理）系统的部署架构设计，包括部署拓扑、容器化方案、Kubernetes部署和监控告警。

---

## 文档信息

- **作者**: OSRM 架构团队
- **创建时间**: 2026-03-17
- **最后更新**: 2026-03-17
- **维护责任人**: 架构师
- **版本**: v1.0

---

## 变更记录

| 时间 | 变更内容 | 处理人 |
|------|----------|--------|
| 2026-03-17 | 初始版本 | 架构师 |
| 2026-03-17 | v1.1 优化版本 | 架构师 |
| | • 添加 Pod Disruption Budget 配置 | |
| | • 添加分布式 MinIO 架构 | |
| | • 添加数据库备份方案 | |
| | • 添加 Secret 管理方案 | |

---

## 1. 部署拓扑架构

### 1.1 生产环境部署拓扑

```
┌─────────────────────────────────────────────────────────────────────┐
│                    生产环境部署拓扑图                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Internet                                                           │
│      │                                                               │
│      ▼                                                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      CDN (静态资源)                          │   │
│  └─────────────────────────┬───────────────────────────────────┘   │
│                            │                                         │
│  ┌─────────────────────────▼───────────────────────────────────┐   │
│  │                      负载均衡层                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │   │
│  │  │   Nginx     │  │   Nginx     │  │    Keepalived       │  │   │
│  │  │   Master    │  │   Backup    │  │    (VIP)            │  │   │
│  │  │  10.0.1.10  │  │  10.0.1.11  │  │    10.0.1.100       │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │   │
│  └─────────────────────────┬───────────────────────────────────┘   │
│                            │                                         │
│  ┌─────────────────────────▼───────────────────────────────────┐   │
│  │                      应用服务层 (K8s)                        │   │
│  │  ┌─────────────────────────────────────────────────────────┐ │   │
│  │  │                  Ingress Controller                      │ │   │
│  │  │              (Nginx Ingress / Traefik)                  │ │   │
│  │  └─────────────────────────┬───────────────────────────────┘ │   │
│  │                            │                                  │   │
│  │  ┌─────────────────────────▼─────────────────────────────┐   │   │
│  │  │                 OSRM 应用 Pod (3 replicas)              │   │   │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐              │   │   │
│  │  │  │  Pod-1   │  │  Pod-2   │  │  Pod-3   │              │   │   │
│  │  │  │ 10.0.2.10│  │ 10.0.2.11│  │ 10.0.2.12│              │   │   │
│  │  │  └──────────┘  └──────────┘  └──────────┘              │   │   │
│  │  └────────────────────────────────────────────────────────┘   │   │
│  │                            │                                  │   │
│  │  ┌─────────────────────────▼─────────────────────────────┐   │   │
│  │  │                 Job Pod (定时任务)                       │   │   │
│  │  │  • 存储健康检查                                          │   │   │
│  │  │  • 数据统计计算                                          │   │   │
│  │  │  • 数据清理任务                                          │   │   │
│  │  └────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────┬───────────────────────────────────┘   │
│                            │                                         │
│  ┌─────────────────────────▼───────────────────────────────────┐   │
│  │                      数据存储层                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │   │
│  │  │ PostgreSQL  │  │    Redis    │  │       MinIO         │  │   │
│  │  │   主节点     │  │   Master    │  │    (分布式存储)      │  │   │
│  │  │  10.0.3.10  │  │  10.0.3.20  │  │    10.0.3.30        │  │   │
│  │  │             │  │             │  │    10.0.3.31        │  │   │
│  │  │  从节点     │  │   Slave     │  │    10.0.3.32        │  │   │
│  │  │  10.0.3.11  │  │  10.0.3.21  │  │                     │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      外部存储 (已纳管)                        │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │   │
│  │  │    Harbor   │  │    Nexus    │  │       NAS           │  │   │
│  │  │   镜像仓库   │  │   组件仓库   │  │    文件存储         │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      监控告警层                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │   │
│  │  │ Prometheus  │  │   Grafana   │  │   Alertmanager      │  │   │
│  │  │   监控采集   │  │   可视化    │  │    告警通知         │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │   │
│  │  │    ELK      │  │    Jaeger   │  │      SkyWalking     │  │   │
│  │  │   日志平台   │  │  链路追踪    │  │     APM监控         │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

网络分区:
• DMZ区: 10.0.1.0/24    (负载均衡)
• 应用区: 10.0.2.0/24   (K8s集群)
• 数据区: 10.0.3.0/24   (数据库/存储)
```

### 1.2 环境划分

| 环境 | 用途 | 规模 | 高可用 |
|------|------|------|--------|
| **开发环境** | 开发联调 | 1节点 | 否 |
| **测试环境** | 测试验证 | 2节点 | 否 |
| **预发环境** | 上线前验证 | 3节点 | 是 |
| **生产环境** | 正式运行 | 5+节点 | 是 |

---

## 2. 容器化方案

### 2.1 Dockerfile 设计

```dockerfile
# ===========================================
# OSRM 后端服务 Dockerfile
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS base

# 安装必要工具
RUN apk add --no-cache tzdata curl \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 创建应用用户
RUN addgroup -S osrm && adduser -S osrm -G osrm

WORKDIR /app

# ===========================================
# 构建阶段
# ===========================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# 复制 Maven 配置
COPY pom.xml .
COPY osrm-common/pom.xml osrm-common/
COPY osrm-domain/pom.xml osrm-domain/
COPY osrm-application/pom.xml osrm-application/
COPY osrm-infrastructure/pom.xml osrm-infrastructure/
COPY osrm-interfaces/pom.xml osrm-interfaces/
COPY osrm-starter/pom.xml osrm-starter/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -B

# 复制源码
COPY osrm-common/src osrm-common/src
COPY osrm-domain/src osrm-domain/src
COPY osrm-application/src osrm-application/src
COPY osrm-infrastructure/src osrm-infrastructure/src
COPY osrm-interfaces/src osrm-interfaces/src
COPY osrm-starter/src osrm-starter/src

# 构建应用
RUN mvn clean package -DskipTests -B \
    && mkdir -p /build/target \
    && cp osrm-starter/target/*.jar /build/target/app.jar

# ===========================================
# 运行阶段
# ===========================================
FROM base

# 复制构建产物
COPY --from=build /build/target/app.jar app.jar

# 设置权限
RUN chown -R osrm:osrm /app
USER osrm

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM 参数优化
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 2.2 Docker Compose 开发环境

```yaml
version: '3.8'

services:
  # =========================================
  # 后端服务
  # =========================================
  osrm-backend:
    build:
      context: ./osrm-backend
      dockerfile: Dockerfile
    container_name: osrm-backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/osrm
      - SPRING_DATASOURCE_USERNAME=osrm
      - SPRING_DATASOURCE_PASSWORD=osrm123
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - MINIO_ENDPOINT=http://minio:9000
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
      minio:
        condition: service_started
    networks:
      - osrm-network
    restart: unless-stopped

  # =========================================
  # 前端服务
  # =========================================
  osrm-frontend:
    build:
      context: ./osrm-frontend
      dockerfile: Dockerfile
    container_name: osrm-frontend
    ports:
      - "80:80"
    environment:
      - VITE_API_BASE_URL=http://localhost:8080/api/v1
    depends_on:
      - osrm-backend
    networks:
      - osrm-network
    restart: unless-stopped

  # =========================================
  # PostgreSQL 数据库
  # =========================================
  postgres:
    image: postgres:15-alpine
    container_name: osrm-postgres
    environment:
      - POSTGRES_DB=osrm
      - POSTGRES_USER=osrm
      - POSTGRES_PASSWORD=osrm123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U osrm -d osrm"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - osrm-network
    restart: unless-stopped

  # =========================================
  # Redis 缓存
  # =========================================
  redis:
    image: redis:7-alpine
    container_name: osrm-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    networks:
      - osrm-network
    restart: unless-stopped

  # =========================================
  # MinIO 对象存储 (开发环境单节点)
  # 生产环境请使用分布式部署 (见 2.3 节)
  # =========================================
  minio:
    image: minio/minio:RELEASE.2024-03-03T17-50-39Z
    container_name: osrm-minio
    environment:
      - MINIO_ROOT_USER=osrm
      - MINIO_ROOT_PASSWORD=osrm123456
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    networks:
      - osrm-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 30s
      timeout: 20s
      retries: 3

  # =========================================
  # 可选: pgAdmin (数据库管理)
  # =========================================
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: osrm-pgadmin
    environment:
      - PGADMIN_DEFAULT_EMAIL=admin@osrm.local
      - PGADMIN_DEFAULT_PASSWORD=admin123
    ports:
      - "5050:80"
    depends_on:
      - postgres
    networks:
      - osrm-network
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
  minio_data:

networks:
  osrm-network:
    driver: bridge
```

### 2.3 分布式 MinIO 生产部署

生产环境使用分布式 MinIO 部署，推荐最低4节点，支持高可用和数据冗余。

```
┌─────────────────────────────────────────────────────────────────────┐
│                      MinIO 分布式架构 (4节点)                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐      ┌─────────────┐      ┌─────────────┐         │
│  │  MinIO-0    │◄────►│  MinIO-1    │◄────►│  MinIO-2    │         │
│  │  (Zone 1)   │      │  (Zone 1)   │      │  (Zone 1)   │         │
│  │  /data-0    │      │  /data-1    │      │  /data-2    │         │
│  │  /data-3    │      │  /data-4    │      │  /data-5    │         │
│  └──────┬──────┘      └──────┬──────┘      └──────┬──────┘         │
│         │                    │                    │                │
│         └────────────────────┼────────────────────┘                │
│                              │                                      │
│                       ┌──────┴──────┐                              │
│                       │  MinIO-3    │                              │
│                       │  (Zone 1)   │                              │
│                       │  /data-6    │                              │
│                       │  /data-7    │                              │
│                       └─────────────┘                              │
│                                                                      │
│  配置:                                                               │
│  • 节点数: 4 (最低要求，支持2节点故障)                                │
│  • 每节点磁盘: 2+ (本例使用虚拟磁盘)                                  │
│  • 纠删码: EC:2 (可容忍2节点/磁盘故障)                               │
│  • 总容量: 磁盘数 × 单盘容量 × (N/2)                                  │
│  • 可用容量: 约50% (4节点EC:2配置)                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Docker Compose 分布式部署：**

```yaml
# docker-compose.minio-cluster.yml
version: '3.8'

services:
  minio-0:
    image: minio/minio:RELEASE.2024-03-03T17-50-39Z
    hostname: minio-0
    environment:
      MINIO_ROOT_USER: osrm
      MINIO_ROOT_PASSWORD: osrm123456
      MINIO_SERVER_URL: http://minio-0:9000
    volumes:
      - minio_data_0:/data-0
      - minio_data_1:/data-1
    command: server --console-address ":9001" http://minio-{0...3}/data-{0...1}
    ports:
      - "9000:9000"
      - "9001:9001"
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 30s
      timeout: 20s
      retries: 3

  minio-1:
    image: minio/minio:RELEASE.2024-03-03T17-50-39Z
    hostname: minio-1
    environment:
      MINIO_ROOT_USER: osrm
      MINIO_ROOT_PASSWORD: osrm123456
    volumes:
      - minio_data_2:/data-0
      - minio_data_3:/data-1
    command: server --console-address ":9001" http://minio-{0...3}/data-{0...1}

  minio-2:
    image: minio/minio:RELEASE.2024-03-03T17-50-39Z
    hostname: minio-2
    environment:
      MINIO_ROOT_USER: osrm
      MINIO_ROOT_PASSWORD: osrm123456
    volumes:
      - minio_data_4:/data-0
      - minio_data_5:/data-1
    command: server --console-address ":9001" http://minio-{0...3}/data-{0...1}

  minio-3:
    image: minio/minio:RELEASE.2024-03-03T17-50-39Z
    hostname: minio-3
    environment:
      MINIO_ROOT_USER: osrm
      MINIO_ROOT_PASSWORD: osrm123456
    volumes:
      - minio_data_6:/data-0
      - minio_data_7:/data-1
    command: server --console-address ":9001" http://minio-{0...3}/data-{0...1}

  # Nginx 负载均衡
  minio-lb:
    image: nginx:alpine
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - ./nginx-minio.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - minio-0
      - minio-1
      - minio-2
      - minio-3

volumes:
  minio_data_0:
  minio_data_1:
  minio_data_2:
  minio_data_3:
  minio_data_4:
  minio_data_5:
  minio_data_6:
  minio_data_7:
```

**MinIO 集群运维命令：**

```bash
# 查看集群状态
mc alias set osrm http://localhost:9000 osrm osrm123456
mc admin info osrm

# 查看纠删码配置
mc admin heal osrm

# 扩展集群 (增加节点)
# 停止所有节点 -> 修改启动命令包含新节点 -> 重新启动

# 备份数据
mc mirror osrm/my-bucket /backup/minio-data

# 监控指标
mc admin prometheus generate osrm
```

---

## 3. Kubernetes 部署

### 3.1 Namespace 设计

```yaml
# =========================================
# 命名空间划分
# =========================================
apiVersion: v1
kind: Namespace
metadata:
  name: osrm-prod
  labels:
    app: osrm
    env: production
---
apiVersion: v1
kind: Namespace
metadata:
  name: osrm-dev
  labels:
    app: osrm
    env: development
```

### 3.2 ConfigMap 配置

```yaml
# =========================================
# 应用配置
# =========================================
apiVersion: v1
kind: ConfigMap
metadata:
  name: osrm-config
  namespace: osrm-prod
data:
  application.yml: |
    server:
      port: 8080

    spring:
      profiles:
        active: prod

      datasource:
        url: jdbc:postgresql://postgres-service:5432/osrm
        username: ${DB_USERNAME}
        password: ${DB_PASSWORD}
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5

      redis:
        host: redis-service
        port: 6379
        password: ${REDIS_PASSWORD}
        lettuce:
          pool:
            max-active: 8
            max-idle: 8

      jpa:
        hibernate:
          ddl-auto: validate
        show-sql: false

      flyway:
        enabled: true
        locations: classpath:db/migration

    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration: 7200000
      refresh-token-expiration: 604800000

    storage:
      minio:
        endpoint: http://minio-service:9000
        access-key: ${MINIO_ACCESS_KEY}
        secret-key: ${MINIO_SECRET_KEY}
        bucket: osrm

    logging:
      level:
        com.osrm: INFO
```

### 3.3 Secret 密钥管理

```yaml
# =========================================
# 敏感配置（实际使用需要加密）
# =========================================
apiVersion: v1
kind: Secret
metadata:
  name: osrm-secret
  namespace: osrm-prod
type: Opaque
data:
  # 使用 base64 编码
  DB_USERNAME: b3NybQ==
  DB_PASSWORD: b3NybTEyMw==
  REDIS_PASSWORD: cmVkaXMxMjM=
  JWT_SECRET: eW91ci0yNTYtYml0LXNlY3JldC1rZXktaGVyZQ==
  MINIO_ACCESS_KEY: b3NybQ==
  MINIO_SECRET_KEY: b3NybTEyMzQ1Ng==
```

### 3.4 Deployment 部署

```yaml
# =========================================
# 后端服务 Deployment
# =========================================
apiVersion: apps/v1
kind: Deployment
metadata:
  name: osrm-backend
  namespace: osrm-prod
  labels:
    app: osrm-backend
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: osrm-backend
  template:
    metadata:
      labels:
        app: osrm-backend
    spec:
      containers:
        - name: backend
          image: osrm/backend:v1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
          envFrom:
            - secretRef:
                name: osrm-secret
          volumeMounts:
            - name: config
              mountPath: /app/config
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
      volumes:
        - name: config
          configMap:
            name: osrm-config
---
# =========================================
# Service 服务暴露
# =========================================
apiVersion: v1
kind: Service
metadata:
  name: osrm-backend-service
  namespace: osrm-prod
spec:
  type: ClusterIP
  selector:
    app: osrm-backend
  ports:
    - port: 8080
      targetPort: 8080
      name: http
```

### 3.5 Ingress 配置

```yaml
# =========================================
# Ingress 路由配置
# =========================================
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: osrm-ingress
  namespace: osrm-prod
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10g"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "300"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - osrm.company.com
      secretName: osrm-tls-secret
  rules:
    - host: osrm.company.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: osrm-backend-service
                port:
                  number: 8080
          - path: /
            pathType: Prefix
            backend:
              service:
                name: osrm-frontend-service
                port:
                  number: 80
```

### 3.6 HPA 自动扩缩容

```yaml
# =========================================
# Horizontal Pod Autoscaler
# =========================================
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: osrm-backend-hpa
  namespace: osrm-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: osrm-backend
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 4
          periodSeconds: 15
      selectPolicy: Max
```

### 3.7 Pod Disruption Budget (PDB)

PDB 确保在集群维护、节点升级或缩容时，关键服务的最小可用副本数。

```yaml
# =========================================
# Pod Disruption Budget - 后端服务
# =========================================
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: osrm-backend-pdb
  namespace: osrm-prod
spec:
  minAvailable: 2          # 最少保持2个Pod可用
  selector:
    matchLabels:
      app: osrm-backend

---
# =========================================
# Pod Disruption Budget - 前端服务
# =========================================
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: osrm-frontend-pdb
  namespace: osrm-prod
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: osrm-frontend

---
# =========================================
# Pod Disruption Budget - 数据库 (使用标签选择器)
# =========================================
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: osrm-postgres-pdb
  namespace: osrm-prod
spec:
  minAvailable: 1          # 单节点PostgreSQL保持1个可用
  selector:
    matchLabels:
      app: postgres
      cluster: osrm-postgres

---
# =========================================
# Pod Disruption Budget - MinIO (分布式模式)
# =========================================
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: osrm-minio-pdb
  namespace: osrm-prod
spec:
  minAvailable: 3          # 4节点MinIO至少3个可用
  selector:
    matchLabels:
      app: minio
```

**PDB 配置说明：**

| 服务 | minAvailable | maxUnavailable | 说明 |
|------|-------------|----------------|------|
| osrm-backend | 2 | - | 保证服务高可用，至少2副本 |
| osrm-frontend | 2 | - | 保证前端服务可用 |
| PostgreSQL | 1 | - | 单节点模式，不允许同时中断 |
| Redis | 2 | - | 3主3从集群，保证多数派 |
| MinIO | 3 | - | 4节点分布式，最多允许1节点中断 |

**使用建议：**
- 关键业务服务：配置 `minAvailable: 2` 或更多
- 单点服务（如PostgreSQL主库）：配置 `minAvailable: 1`，配合亲和性避免单点故障
- 使用 `kubectl drain` 时，PDB 会阻止驱逐导致可用副本低于阈值的Pod

### 3.8 数据库备份方案

#### 3.8.1 备份策略设计

| 备份类型 | 频率 | 保留期 | 存储位置 | 说明 |
|----------|------|--------|----------|------|
| 全量备份 | 每日 02:00 | 30天 | MinIO + 异地 | pg_dump 逻辑备份 |
| 增量备份(WAL) | 实时归档 | 7天 | MinIO | PostgreSQL PITR |
| 对象存储备份 | 每周 | 90天 | 异地存储 | MinIO Bucket Mirror |
| 配置文件备份 | 每次变更 | 永久 | Git + 对象存储 | K8s ConfigMap/Secret |

#### 3.8.2 PostgreSQL 备份配置

**1. 启用 WAL 归档 (PostgreSQL 配置)**

```yaml
# postgres-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
  namespace: osrm-prod
data:
  postgresql.conf: |
    # WAL 归档配置
    wal_level = replica
    archive_mode = on
    archive_command = 'envdir /etc/wal-g/env wal-g wal-push %p'
    archive_timeout = 60

    # 基础备份配置
    max_wal_senders = 3
    max_replication_slots = 3

    # 性能优化
    wal_buffers = 16MB
    checkpoint_completion_target = 0.9
```

**2. Wal-g 备份工具部署**

```yaml
# postgres-backup-cronjob.yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-daily-backup
  namespace: osrm-prod
spec:
  schedule: "0 2 * * *"  # 每天凌晨2点
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: ghcr.io/wal-g/wal-g:latest
            env:
            - name: PGHOST
              value: postgres-service
            - name: PGUSER
              valueFrom:
                secretKeyRef:
                  name: postgres-secrets
                  key: username
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secrets
                  key: password
            - name: WALG_S3_PREFIX
              value: s3://osrm-backups/postgresql
            - name: AWS_ACCESS_KEY_ID
              valueFrom:
                secretKeyRef:
                  name: minio-secrets
                  key: access-key
            - name: AWS_SECRET_ACCESS_KEY
              valueFrom:
                secretKeyRef:
                  name: minio-secrets
                  key: secret-key
            - name: AWS_ENDPOINT
              value: http://minio-service:9000
            - name: AWS_S3_FORCE_PATH_STYLE
              value: "true"
            command:
            - /bin/sh
            - -c
            - |
              echo "Starting PostgreSQL backup..."
              wal-g backup-push /var/lib/postgresql/data
              echo "Backup completed"

              # 清理过期备份 (保留30天)
              wal-g delete retain 30 --confirm
              echo "Old backups cleaned"
            volumeMounts:
            - name: postgres-data
              mountPath: /var/lib/postgresql/data
              readOnly: true
          volumes:
          - name: postgres-data
            persistentVolumeClaim:
              claimName: postgres-pvc
          restartPolicy: OnFailure
```

**3. 实时 WAL 归档 Sidecar**

```yaml
# 在 PostgreSQL StatefulSet 中添加 wal-g sidecar
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: osrm-prod
spec:
  serviceName: postgres
  replicas: 1
  template:
    spec:
      containers:
      # PostgreSQL 主容器
      - name: postgres
        image: postgres:15-alpine
        # ... 其他配置

      # WAL-G 归档 Sidecar
      - name: wal-g-archiver
        image: ghcr.io/wal-g/wal-g:latest
        env:
        - name: WALG_S3_PREFIX
          value: s3://osrm-backups/postgresql/wal
        - name: AWS_ACCESS_KEY_ID
          valueFrom:
            secretKeyRef:
              name: minio-secrets
              key: access-key
        # ... 其他环境变量同上
        command:
        - /bin/sh
        - -c
        - |
          # 持续归档 WAL 文件
          wal-g daemon
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
          readOnly: true
        - name: wal-g-env
          mountPath: /etc/wal-g/env
```

#### 3.8.3 备份验证与恢复

**备份验证 Job：**

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: backup-verify
  namespace: osrm-prod
spec:
  template:
    spec:
      containers:
      - name: verify
        image: ghcr.io/wal-g/wal-g:latest
        env:
        # ... 环境变量同上
        command:
        - /bin/sh
        - -c
        - |
          echo "Listing available backups..."
          wal-g backup-list

          # 下载最新备份并验证完整性
          LATEST_BACKUP=$(wal-g backup-list | tail -1 | awk '{print $1}')
          echo "Latest backup: $LATEST_BACKUP"

          # 验证备份完整性
          wal-g backup-fetch /tmp/verify-backup "$LATEST_BACKUP"
          pg_verifybackup /tmp/verify-backup
          echo "Backup verification passed"
      restartPolicy: Never
```

**恢复流程：**

```bash
#!/bin/bash
# restore.sh - 数据库恢复脚本

# 1. 停止应用服务
kubectl scale deployment osrm-backend --replicas=0 -n osrm-prod

# 2. 恢复数据
kubectl run restore-job --rm -i --restart=Never \
  --image=ghcr.io/wal-g/wal-g:latest \
  --env="WALG_S3_PREFIX=s3://osrm-backups/postgresql" \
  -- \
  sh -c "wal-g backup-fetch /restore LATEST && echo 'Restore completed'"

# 3. 验证数据一致性
# 4. 重启 PostgreSQL
# 5. 启动应用服务
kubectl scale deployment osrm-backend --replicas=3 -n osrm-prod
```

#### 3.8.4 备份监控告警

```yaml
# 备份监控 PrometheusRule
groups:
- name: backup
  rules:
  - alert: BackupJobFailed
    expr: kube_job_status_failed{job_name=~"postgres-daily-backup-.*"} == 1
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "PostgreSQL backup job failed"

  - alert: BackupNotRun
    expr: time() - kube_job_status_completion_time{job_name=~"postgres-daily-backup-.*"} > 90000
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "PostgreSQL backup not run for over 25 hours"

  - alert: WALArchiveLag
    expr: pg_stat_archiver_last_archived_time - pg_stat_archiver_last_failed_time > 300
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "WAL archiving is lagging"
```

---

## 4. 监控告警

### 4.1 Prometheus 监控配置

```yaml
# =========================================
# ServiceMonitor 配置
# =========================================
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: osrm-metrics
  namespace: osrm-prod
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      app: osrm-backend
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
      scrapeTimeout: 10s
```

### 4.2 告警规则

```yaml
# =========================================
# PrometheusRule 告警规则
# =========================================
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: osrm-alerts
  namespace: osrm-prod
spec:
  groups:
    - name: osrm
      rules:
        # 服务不可用
        - alert: OSRMServiceDown
          expr: up{job="osrm-backend"} == 0
          for: 1m
          labels:
            severity: critical
          annotations:
            summary: "OSRM 服务不可用"
            description: "{{ $labels.instance }} 服务已宕机超过1分钟"

        # 高错误率
        - alert: OSRMHighErrorRate
          expr: rate(http_requests_total{job="osrm-backend",status=~"5.."}[5m]) > 0.1
          for: 2m
          labels:
            severity: warning
          annotations:
            summary: "OSRM 错误率过高"
            description: "5xx错误率超过10%"

        # 响应时间过长
        - alert: OSRMSlowResponse
          expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "OSRM 响应时间过长"
            description: "95%请求响应时间超过2秒"

        # 磁盘空间不足
        - alert: OSRMLowDiskSpace
          expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "磁盘空间不足"
            description: "{{ $labels.device }} 可用空间小于10%"

        # 内存使用率过高
        - alert: OSRMHighMemoryUsage
          expr: (container_memory_working_set_bytes / container_spec_memory_limit_bytes) > 0.85
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "OSRM 内存使用率过高"
            description: "Pod {{ $labels.pod }} 内存使用率超过85%"
```

---

## 5. CI/CD 流水线

### 5.1 GitLab CI 配置

```yaml
# =========================================
# .gitlab-ci.yml
# =========================================
stages:
  - build
  - test
  - package
  - deploy

variables:
  DOCKER_REGISTRY: registry.company.com
  IMAGE_NAME: $DOCKER_REGISTRY/osrm/backend

# 构建阶段
build:
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn clean compile -DskipTests
  cache:
    paths:
      - .m2/repository
  only:
    - merge_requests
    - main

# 测试阶段
test:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  services:
    - postgres:15
    - redis:7
  variables:
    POSTGRES_DB: osrm_test
    POSTGRES_USER: test
    POSTGRES_PASSWORD: test
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/osrm_test
  script:
    - mvn test
    - mvn jacoco:report
  artifacts:
    reports:
      junit: target/surefire-reports/*.xml
      coverage_report:
        coverage_format: jacoco
        path: target/site/jacoco/jacoco.xml
    paths:
      - target/site/jacoco
  coverage: '/Total.*?([0-9]{1,3})%/'
  only:
    - merge_requests
    - main

# 打包阶段
package:
  stage: package
  image: docker:24
  services:
    - docker:24-dind
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $DOCKER_REGISTRY
    - docker build -t $IMAGE_NAME:$CI_COMMIT_SHA -t $IMAGE_NAME:latest .
    - docker push $IMAGE_NAME:$CI_COMMIT_SHA
    - docker push $IMAGE_NAME:latest
  only:
    - main

# 部署到开发环境
deploy-dev:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context dev
    - kubectl set image deployment/osrm-backend backend=$IMAGE_NAME:$CI_COMMIT_SHA -n osrm-dev
    - kubectl rollout status deployment/osrm-backend -n osrm-dev
  environment:
    name: development
    url: https://osrm-dev.company.com
  only:
    - main

# 部署到生产环境（手动触发）
deploy-prod:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context prod
    - kubectl set image deployment/osrm-backend backend=$IMAGE_NAME:$CI_COMMIT_SHA -n osrm-prod
    - kubectl rollout status deployment/osrm-backend -n osrm-prod
  environment:
    name: production
    url: https://osrm.company.com
  when: manual
  only:
    - tags
```

---

## 6. 与需求文档的追溯关系

| 部署架构章节 | 关联需求文档 | 追溯说明 |
|-------------|-------------|---------|
| 1. 部署拓扑 | 4.3 可用性要求 | 满足99.9%可用性 |
| 2. 容器化 | 5.3 部署 | Docker/K8s部署方案 |
| 3. K8s部署 | 4.4 扩展性要求 | 水平扩展能力 |
| 4. 监控告警 | 4.2 安全要求 | 运维安全监控 |
| 5. CI/CD | 开发流程规范 | 自动化部署流程 |

---

## 附录

### A. 端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| 后端API | 8080 | Spring Boot 服务 |
| 前端 | 80/443 | Nginx 服务 |
| PostgreSQL | 5432 | 数据库 |
| Redis | 6379 | 缓存 |
| MinIO | 9000/9001 | 对象存储 |
| Prometheus | 9090 | 监控 |
| Grafana | 3000 | 可视化 |

### B. 资源需求估算

| 环境 | CPU | 内存 | 存储 | 说明 |
|------|-----|------|------|------|
| 开发 | 4核 | 8GB | 100GB | 单节点 |
| 测试 | 8核 | 16GB | 200GB | 双节点 |
| 生产 | 16核+ | 64GB+ | 1TB+ | 高可用集群 |
