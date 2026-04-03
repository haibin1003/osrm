package com.osrm.application.statistics.dto.response;

import java.util.Map;

/**
 * 统计概览响应DTO
 */
public class StatisticsOverviewDTO {

    private Integer totalPackages;
    private Integer totalSubscriptions;
    private Integer activeBusinessSystems;
    private Integer newSubscriptionsThisMonth;
    private Map<String, Double> trends;

    // Getters and Setters
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

    public Integer getActiveBusinessSystems() {
        return activeBusinessSystems;
    }

    public void setActiveBusinessSystems(Integer activeBusinessSystems) {
        this.activeBusinessSystems = activeBusinessSystems;
    }

    public Integer getNewSubscriptionsThisMonth() {
        return newSubscriptionsThisMonth;
    }

    public void setNewSubscriptionsThisMonth(Integer newSubscriptionsThisMonth) {
        this.newSubscriptionsThisMonth = newSubscriptionsThisMonth;
    }

    public Map<String, Double> getTrends() {
        return trends;
    }

    public void setTrends(Map<String, Double> trends) {
        this.trends = trends;
    }
}
