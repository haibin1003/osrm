package com.osrm.application.storage.dto.request;

import jakarta.validation.constraints.*;

/**
 * 更新存储后端请求
 */
public class UpdateStorageBackendRequest {

    @NotBlank(message = "后端名称不能为空")
    @Size(max = 64, message = "后端名称长度不能超过64")
    private String backendName;

    @NotBlank(message = "服务端点不能为空")
    @Size(max = 256, message = "服务端点长度不能超过256")
    private String endpoint;

    @Size(max = 128, message = "访问密钥长度不能超过128")
    private String accessKey;

    @Size(max = 256, message = "密钥长度不能超过256")
    private String secretKey;

    @Size(max = 64, message = "命名空间长度不能超过64")
    private String namespace;

    private Object config;

    private Boolean isDefault;

    private Boolean enabled;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    // Getters and Setters

    public String getBackendName() {
        return backendName;
    }

    public void setBackendName(String backendName) {
        this.backendName = backendName;
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

    public Object getConfig() {
        return config;
    }

    public void setConfig(Object config) {
        this.config = config;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
