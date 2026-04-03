package com.osrm.domain.storage.entity;

/**
 * 存储后端类型枚举
 */
public enum StorageBackendType {
    HARBOR("Harbor仓库", "Docker镜像、Helm Chart存储"),
    NEXUS("Nexus仓库", "Maven、NPM、PyPI、Raw文件存储"),
    NAS("NAS/本地存储", "通用文件存储");

    private final String displayName;
    private final String description;

    StorageBackendType(String displayName, String description) {
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
