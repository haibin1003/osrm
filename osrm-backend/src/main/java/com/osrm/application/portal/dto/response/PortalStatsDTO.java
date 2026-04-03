package com.osrm.application.portal.dto.response;

public class PortalStatsDTO {

    private long totalPackages;
    private long dockerCount;
    private long helmCount;
    private long mavenCount;
    private long npmCount;
    private long pypiCount;
    private long genericCount;
    private long publishedCount;
    private long pendingCount;

    public long getTotalPackages() { return totalPackages; }
    public void setTotalPackages(long totalPackages) { this.totalPackages = totalPackages; }
    public long getDockerCount() { return dockerCount; }
    public void setDockerCount(long dockerCount) { this.dockerCount = dockerCount; }
    public long getHelmCount() { return helmCount; }
    public void setHelmCount(long helmCount) { this.helmCount = helmCount; }
    public long getMavenCount() { return mavenCount; }
    public void setMavenCount(long mavenCount) { this.mavenCount = mavenCount; }
    public long getNpmCount() { return npmCount; }
    public void setNpmCount(long npmCount) { this.npmCount = npmCount; }
    public long getPypiCount() { return pypiCount; }
    public void setPypiCount(long pypiCount) { this.pypiCount = pypiCount; }
    public long getGenericCount() { return genericCount; }
    public void setGenericCount(long genericCount) { this.genericCount = genericCount; }
    public long getPublishedCount() { return publishedCount; }
    public void setPublishedCount(long publishedCount) { this.publishedCount = publishedCount; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
}
