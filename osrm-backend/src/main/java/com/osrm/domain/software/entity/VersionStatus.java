package com.osrm.domain.software.entity;

/**
 * 版本状态枚举
 */
public enum VersionStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布"),
    OFFLINE("已下线");

    private final String name;

    VersionStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
