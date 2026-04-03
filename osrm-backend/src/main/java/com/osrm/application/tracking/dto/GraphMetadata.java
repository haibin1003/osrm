package com.osrm.application.tracking.dto;

/**
 * 图谱元数据
 */
public class GraphMetadata {
    private Integer totalSystems;
    private Integer totalPackages;
    private Integer totalSubscriptions;

    public GraphMetadata() {}

    public GraphMetadata(Integer totalSystems, Integer totalPackages, Integer totalSubscriptions) {
        this.totalSystems = totalSystems;
        this.totalPackages = totalPackages;
        this.totalSubscriptions = totalSubscriptions;
    }

    // Getters and Setters
    public Integer getTotalSystems() { return totalSystems; }
    public void setTotalSystems(Integer totalSystems) { this.totalSystems = totalSystems; }
    public Integer getTotalPackages() { return totalPackages; }
    public void setTotalPackages(Integer totalPackages) { this.totalPackages = totalPackages; }
    public Integer getTotalSubscriptions() { return totalSubscriptions; }
    public void setTotalSubscriptions(Integer totalSubscriptions) { this.totalSubscriptions = totalSubscriptions; }
}
