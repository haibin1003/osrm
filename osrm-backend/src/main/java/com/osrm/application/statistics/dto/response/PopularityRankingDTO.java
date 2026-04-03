package com.osrm.application.statistics.dto.response;

import java.util.List;

/**
 * 软件包热度排行响应DTO
 */
public class PopularityRankingDTO {

    private String sortBy;
    private Integer total;
    private List<PackagePopularity> data;

    public static class PackagePopularity {
        private Integer rank;
        private Long packageId;
        private String packageName;
        private String packageKey;
        private String softwareType;
        private Integer subscriptionCount;
        private Integer businessSystemCount;
        private String trend;
        private Integer change;

        public PackagePopularity() {}

        public PackagePopularity(Integer rank, Long packageId, String packageName, String packageKey,
                                 String softwareType, Integer subscriptionCount, Integer businessSystemCount,
                                 String trend, Integer change) {
            this.rank = rank;
            this.packageId = packageId;
            this.packageName = packageName;
            this.packageKey = packageKey;
            this.softwareType = softwareType;
            this.subscriptionCount = subscriptionCount;
            this.businessSystemCount = businessSystemCount;
            this.trend = trend;
            this.change = change;
        }

        // Getters and Setters
        public Integer getRank() {
            return rank;
        }

        public void setRank(Integer rank) {
            this.rank = rank;
        }

        public Long getPackageId() {
            return packageId;
        }

        public void setPackageId(Long packageId) {
            this.packageId = packageId;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageKey() {
            return packageKey;
        }

        public void setPackageKey(String packageKey) {
            this.packageKey = packageKey;
        }

        public String getSoftwareType() {
            return softwareType;
        }

        public void setSoftwareType(String softwareType) {
            this.softwareType = softwareType;
        }

        public Integer getSubscriptionCount() {
            return subscriptionCount;
        }

        public void setSubscriptionCount(Integer subscriptionCount) {
            this.subscriptionCount = subscriptionCount;
        }

        public Integer getBusinessSystemCount() {
            return businessSystemCount;
        }

        public void setBusinessSystemCount(Integer businessSystemCount) {
            this.businessSystemCount = businessSystemCount;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }

        public Integer getChange() {
            return change;
        }

        public void setChange(Integer change) {
            this.change = change;
        }
    }

    // Getters and Setters
    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<PackagePopularity> getData() {
        return data;
    }

    public void setData(List<PackagePopularity> data) {
        this.data = data;
    }
}
