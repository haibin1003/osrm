package com.osrm.domain.storage.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 存储后端实体
 */
@Entity
@Table(name = "t_storage_backend")
public class StorageBackend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backend_code", nullable = false, unique = true, length = 32)
    private String backendCode;

    @Column(name = "backend_name", nullable = false, unique = true, length = 64)
    private String backendName;

    @Enumerated(EnumType.STRING)
    @Column(name = "backend_type", nullable = false, length = 20)
    private StorageBackendType backendType;

    @Column(name = "endpoint", nullable = false, length = 256)
    private String endpoint;

    @Column(name = "access_key", length = 128)
    private String accessKey;

    @Column(name = "secret_key", length = 256)
    private String secretKey;

    @Column(name = "namespace", length = 64)
    private String namespace;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 领域方法

    /**
     * 更新健康状态
     */
    public void updateHealthStatus(HealthStatus status, String errorMessage) {
        this.healthStatus = status;
        setErrorMessage(errorMessage);
        this.lastHealthCheck = LocalDateTime.now();
    }

    /**
     * 标记为在线
     */
    public void markOnline() {
        this.healthStatus = HealthStatus.ONLINE;
        this.errorMessage = null;
        this.lastHealthCheck = LocalDateTime.now();
    }

    /**
     * 标记为离线
     */
    public void markOffline(String errorMessage) {
        this.healthStatus = HealthStatus.OFFLINE;
        setErrorMessage(errorMessage);
        this.lastHealthCheck = LocalDateTime.now();
    }

    /**
     * 标记为异常
     */
    public void markError(String errorMessage) {
        this.healthStatus = HealthStatus.ERROR;
        setErrorMessage(errorMessage);
        this.lastHealthCheck = LocalDateTime.now();
    }

    /**
     * 设置为默认后端
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * 取消默认设置
     */
    public void unsetDefault() {
        this.isDefault = false;
    }

    /**
     * 启用
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.enabled = false;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBackendCode() {
        return backendCode;
    }

    public void setBackendCode(String backendCode) {
        this.backendCode = backendCode;
    }

    public String getBackendName() {
        return backendName;
    }

    public void setBackendName(String backendName) {
        this.backendName = backendName;
    }

    public StorageBackendType getBackendType() {
        return backendType;
    }

    public void setBackendType(StorageBackendType backendType) {
        this.backendType = backendType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }

    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        // 截断错误信息以适应数据库字段长度限制
        if (errorMessage != null && errorMessage.length() > 500) {
            this.errorMessage = errorMessage.substring(0, 497) + "...";
        } else {
            this.errorMessage = errorMessage;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
