package com.osrm.domain.software.entity;

/**
 * 软件类型枚举
 */
public enum SoftwareType {
    DOCKER_IMAGE("Docker镜像", "HARBOR"),
    HELM_CHART("Helm Chart", "HARBOR"),
    MAVEN("Maven组件", "NEXUS"),
    NPM("NPM包", "NEXUS"),
    PYPI("PyPI包", "NEXUS"),
    GENERIC("通用文件", "NAS"),
    MIDDLEWARE("中间件", "NAS"),
    RUNTIME("运行时", "NAS"),
    MESSAGE_QUEUE("消息队列", "NAS");

    private final String name;
    private final String storageType;

    SoftwareType(String name, String storageType) {
        this.name = name;
        this.storageType = storageType;
    }

    public String getName() {
        return name;
    }

    public String getStorageType() {
        return storageType;
    }
}
