package com.osrm.application.statistics.dto.response;

import java.util.List;

/**
 * 趋势数据响应DTO
 */
public class TrendDataDTO {

    private String period;
    private String startDate;
    private String endDate;
    private Integer totalDays;
    private List<DailyData> data;
    private Summary summary;

    public static class DailyData {
        private String date;
        private Integer subscriptionCount;
        private Integer approvedCount;
        private Integer rejectedCount;
        private Integer pendingCount;

        public DailyData() {}

        public DailyData(String date, Integer subscriptionCount, Integer approvedCount, Integer rejectedCount, Integer pendingCount) {
            this.date = date;
            this.subscriptionCount = subscriptionCount;
            this.approvedCount = approvedCount;
            this.rejectedCount = rejectedCount;
            this.pendingCount = pendingCount;
        }

        // Getters and Setters
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getSubscriptionCount() {
            return subscriptionCount;
        }

        public void setSubscriptionCount(Integer subscriptionCount) {
            this.subscriptionCount = subscriptionCount;
        }

        public Integer getApprovedCount() {
            return approvedCount;
        }

        public void setApprovedCount(Integer approvedCount) {
            this.approvedCount = approvedCount;
        }

        public Integer getRejectedCount() {
            return rejectedCount;
        }

        public void setRejectedCount(Integer rejectedCount) {
            this.rejectedCount = rejectedCount;
        }

        public Integer getPendingCount() {
            return pendingCount;
        }

        public void setPendingCount(Integer pendingCount) {
            this.pendingCount = pendingCount;
        }
    }

    public static class Summary {
        private Integer totalSubscriptionCount;
        private Integer totalApprovedCount;
        private Integer totalRejectedCount;
        private Double averageDaily;

        // Getters and Setters
        public Integer getTotalSubscriptionCount() {
            return totalSubscriptionCount;
        }

        public void setTotalSubscriptionCount(Integer totalSubscriptionCount) {
            this.totalSubscriptionCount = totalSubscriptionCount;
        }

        public Integer getTotalApprovedCount() {
            return totalApprovedCount;
        }

        public void setTotalApprovedCount(Integer totalApprovedCount) {
            this.totalApprovedCount = totalApprovedCount;
        }

        public Integer getTotalRejectedCount() {
            return totalRejectedCount;
        }

        public void setTotalRejectedCount(Integer totalRejectedCount) {
            this.totalRejectedCount = totalRejectedCount;
        }

        public Double getAverageDaily() {
            return averageDaily;
        }

        public void setAverageDaily(Double averageDaily) {
            this.averageDaily = averageDaily;
        }
    }

    // Getters and Setters
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }

    public List<DailyData> getData() {
        return data;
    }

    public void setData(List<DailyData> data) {
        this.data = data;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }
}
