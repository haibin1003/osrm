package com.osrm.application.statistics.dto.response;

import java.util.List;

/**
 * 软件类型分布响应DTO
 */
public class TypeDistributionDTO {

    private Integer totalPackages;
    private List<TypeStats> data;

    public static class TypeStats {
        private String softwareType;
        private String typeName;
        private Integer packageCount;
        private Integer subscriptionCount;
        private Double percentage;
        private String color;

        public TypeStats() {}

        public TypeStats(String softwareType, String typeName, Integer packageCount,
                        Integer subscriptionCount, Double percentage, String color) {
            this.softwareType = softwareType;
            this.typeName = typeName;
            this.packageCount = packageCount;
            this.subscriptionCount = subscriptionCount;
            this.percentage = percentage;
            this.color = color;
        }

        // Getters and Setters
        public String getSoftwareType() {
            return softwareType;
        }

        public void setSoftwareType(String softwareType) {
            this.softwareType = softwareType;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
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

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    // Getters and Setters
    public Integer getTotalPackages() {
        return totalPackages;
    }

    public void setTotalPackages(Integer totalPackages) {
        this.totalPackages = totalPackages;
    }

    public List<TypeStats> getData() {
        return data;
    }

    public void setData(List<TypeStats> data) {
        this.data = data;
    }
}
