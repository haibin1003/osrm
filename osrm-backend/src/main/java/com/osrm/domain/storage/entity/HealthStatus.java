package com.osrm.domain.storage.entity;

/**
 * 存储后端健康状态枚举
 */
public enum HealthStatus {
    ONLINE("在线", "存储后端连接正常"),
    OFFLINE("离线", "存储后端连接失败"),
    ERROR("异常", "存储后端连接成功但存在异常"),
    UNKNOWN("未知", "尚未进行健康检查");

    private final String displayName;
    private final String description;

    HealthStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
