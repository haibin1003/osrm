package com.osrm.domain.subscription.entity;

public enum SubscriptionStatus {

    PENDING("待审批"),
    APPROVED("已批准"),
    REJECTED("已驳回"),
    REVOKED("已撤销");

    private final String name;

    SubscriptionStatus(String name) { this.name = name; }

    public String getName() { return name; }
}
