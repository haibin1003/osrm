package com.osrm.domain.software.entity;

/**
 * 软件包状态枚举
 */
public enum PackageStatus {
    DRAFT("草稿"),
    PENDING("待审核"),
    PUBLISHED("已发布"),
    OFFLINE("已下架"),
    ARCHIVED("已归档");

    private final String name;

    PackageStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * 是否可以提交审核
     */
    public boolean canSubmit() {
        return this == DRAFT;
    }

    /**
     * 是否可以审批
     */
    public boolean canApprove() {
        return this == PENDING;
    }

    /**
     * 是否可以下架
     */
    public boolean canOffline() {
        return this == PUBLISHED;
    }

    /**
     * 是否可以重新上架
     */
    public boolean canRepublish() {
        return this == OFFLINE;
    }

    /**
     * 是否可以编辑基础信息
     */
    public boolean canEdit() {
        return this == DRAFT || this == PENDING;
    }

    /**
     * 是否可以删除
     */
    public boolean canDelete() {
        return this == DRAFT;
    }
}
