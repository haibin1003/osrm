package com.osrm.application.storage.dto.request;

import com.osrm.domain.storage.entity.StorageBackendType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 连接测试请求
 */
public class TestConnectionRequest {

    @NotNull(message = "后端类型不能为空")
    private StorageBackendType backendType;

    @NotBlank(message = "服务端点不能为空")
    @Size(max = 256, message = "服务端点长度不能超过256")
    private String endpoint;

    @Size(max = 128, message = "访问密钥长度不能超过128")
    private String accessKey;

    @Size(max = 256, message = "密钥长度不能超过256")
    private String secretKey;

    private Object config;

    // Getters and Setters

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

    public Object getConfig() {
        return config;
    }

    public void setConfig(Object config) {
        this.config = config;
    }
}
