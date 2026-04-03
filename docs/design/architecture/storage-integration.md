# 存储后端集成架构设计

## 组件架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Storage Backend Module                    │
├─────────────────────────────────────────────────────────────┤
│  Controller Layer                                           │
│  ├── StorageBackendController (REST API)                    │
│  └── StorageBackendHealthController (健康检查)               │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                          │
│  ├── StorageBackendAppService (业务逻辑)                     │
│  └── StorageHealthCheckService (健康检查任务)                │
├─────────────────────────────────────────────────────────────┤
│  Domain Layer                                               │
│  ├── StorageBackend (实体)                                  │
│  ├── StorageBackendRepository (仓储接口)                     │
│  └── StorageBackendFactory (工厂)                           │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                       │
│  ├── HarborClient (Harbor API客户端)                        │
│  ├── NexusClient (Nexus API客户端)                          │
│  ├── NasStorageClient (NAS存储客户端)                        │
│  └── StorageBackendRepositoryImpl (仓储实现)                 │
└─────────────────────────────────────────────────────────────┘
```

## 客户端设计

### HarborClient

```java
public interface HarborClient {
    // 连接测试
    ConnectionTestResult testConnection(HarborConfig config);
    
    // 获取系统信息
    HarborSystemInfo getSystemInfo();
    
    // 获取项目列表
    List<HarborProject> listProjects();
    
    // 检查项目是否存在
    boolean projectExists(String projectName);
}
```

### NexusClient

```java
public interface NexusClient {
    // 连接测试
    ConnectionTestResult testConnection(NexusConfig config);
    
    // 获取状态
    NexusStatus getStatus();
    
    // 获取仓库列表
    List<NexusRepository> listRepositories();
    
    // 检查仓库是否存在
    boolean repositoryExists(String repoName);
}
```

## 健康检查任务

使用 Spring @Scheduled 实现定时健康检查：

```java
@Scheduled(fixedRate = 300000) // 每5分钟
public void checkAllBackendsHealth() {
    // 1. 查询所有启用的存储后端
    // 2. 逐个执行健康检查
    // 3. 更新健康状态
    // 4. 状态变化时触发告警
}
```

## 密码加密

使用 AES 加密存储密码：

```java
@Component
public class PasswordEncryptor {
    @Value("${osrm.encryption.key}")
    private String encryptionKey;
    
    public String encrypt(String plainPassword);
    public String decrypt(String encryptedPassword);
}
```

## 错误处理

### Harbor 错误映射

| Harbor 错误 | 业务错误 |
|-------------|----------|
| 401 Unauthorized | 认证失败，请检查用户名密码 |
| 403 Forbidden | 权限不足 |
| 404 Not Found | 项目不存在 |
| 连接超时 | 网络连接失败 |

### Nexus 错误映射

| Nexus 错误 | 业务错误 |
|------------|----------|
| 401 Unauthorized | 认证失败 |
| 仓库不存在 | 配置的仓库不存在 |
