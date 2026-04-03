package com.osrm.application.statistics.dto.response;

import java.util.List;

/**
 * 业务系统分布响应DTO
 */
public class BusinessDistributionDTO {

    private Integer totalBusinessSystems;
    private Integer totalPackages;
    private Integer totalSubscriptions;
    private List<BusinessSystemStats> data;

    public static class BusinessSystemStats {
        private Long businessSystemId;
        private String systemName;
        private String systemCode;
        private String domain;
        private Integer packageCount;
        private Integer subscriptionCount;
        private Double percentage;

        public BusinessSystemStats() {}

        public BusinessSystemStats(Long businessSystemId, String systemName, String systemCode,
                                   String domain, Integer packageCount, Integer subscriptionCount, Double percentage) {
            this.businessSystemId = businessSystemId;
            this.systemName = systemName;
            this.systemCode = systemCode;
            this.domain = domain;
            this.packageCount = packageCount;
            this.subscriptionCount = subscriptionCount;
            this.percentage = percentage;
        }

        // Getters and Setters
        public Long getBusinessSystemId() {
            return businessSystemId;
        }

        public void setBusinessSystemId(Long businessSystemId) {
            this.businessSystemId = businessSystemId;
        }

        public String getSystemName() {
            return systemName;
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        public String getSystemCode() {
            return systemCode;
        }

        public void setSystemCode(String systemCode) {
            this.systemCode = systemCode;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public Integer getPackageCount() {
            return packageCount;
        }

        public void setPackageCount(Integer packageCount) {
            this.packageCount = packageCount;
        }

        public Integer getSubscriptionCount() {
            return subscriptionCount;
        }

        public void setSubscriptionCount(Integer subscriptionCount) {
            this.subscriptionCount = subscriptionCount;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }

    // Getters and Setters
    public Integer getTotalBusinessSystems() {
        return totalBusinessSystems;
    }

    public void setTotalBusinessSystems(Integer totalBusinessSystems) {
        this.totalBusinessSystems = totalBusinessSystems;
    }

    public Integer getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(Integer totalPackages) {
        this.totalPackages = totalPackages;
    }

    public Integer getTotalSubscriptions() {
        return totalSubscriptions;
    }

    public void setTotalSubscriptions(Integer totalSubscriptions) {
        this.totalSubscriptions = totalSubscriptions;
    }

    public List<BusinessSystemStats> getData() {
        return data;
    }

    public void setData(List<BusinessSystemStats> data) {
        this.data = data;
    }
}
