package com.osrm.application.portal.dto.response;

public class PortalStatsOverviewDTO {

    private long totalPackages;
    private long publishedCount;
    private long pendingCount;
    private long draftCount;
    private long totalSubscriptions;

    public long getTotalPackages() { return totalPackages; }
    public void setTotalPackages(long totalPackages) { this.totalPackages = totalPackages; }

    public long getPublishedCount() { return publishedCount; }
    public void setPublishedCount(long publishedCount) { this.publishedCount = publishedCount; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

    public long getDraftCount() { return draftCount; }
    public void setDraftCount(long draftCount) { this.draftCount = draftCount; }

    public long getTotalSubscriptions() { return totalSubscriptions; }
    public void setTotalSubscriptions(long totalSubscriptions) { this.totalSubscriptions = totalSubscriptions; }
}
