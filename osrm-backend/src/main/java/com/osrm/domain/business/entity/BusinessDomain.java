package com.osrm.domain.business.entity;

/**
 * 业务域枚举
 */
public enum BusinessDomain {

    BUSINESS("业务域", "B"),
    OPERATION("运营域", "O"),
    RESOURCE("资源域", "M"),
    SERVICE("服务域", "S"),
    DATA("数据域", "D");

    private final String name;
    private final String code;

    BusinessDomain(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
