package com.osrm.application.software.dto.response;

import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;

import java.time.LocalDateTime;

public class SoftwarePackageDTO {

    private Long id;
    private String packageName;
    private String packageKey;
    private SoftwareType softwareType;
    private String softwareTypeName;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String websiteUrl;
    private String licenseType;
    private String licenseUrl;
    private String sourceUrl;
    private String logoUrl;
    private String currentVersion;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer subscriptionCount;
    private PackageStatus status;
    private String statusName;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private int versionCount;

    public static SoftwarePackageDTO from(SoftwarePackage pkg) {
        SoftwarePackageDTO dto = new SoftwarePackageDTO();
        dto.setId(pkg.getId());
        dto.setPackageName(pkg.getPackageName());
        dto.setPackageKey(pkg.getPackageKey());
        dto.setSoftwareType(pkg.getSoftwareType());
        dto.setSoftwareTypeName(pkg.getSoftwareType() != null ? pkg.getSoftwareType().getName() : null);
        dto.setCategoryId(pkg.getCategoryId());
        dto.setDescription(pkg.getDescription());
        dto.setWebsiteUrl(pkg.getWebsiteUrl());
        dto.setLicenseType(pkg.getLicenseType());
        dto.setLicenseUrl(pkg.getLicenseUrl());
        dto.setSourceUrl(pkg.getSourceUrl());
        dto.setLogoUrl(pkg.getLogoUrl());
        dto.setCurrentVersion(pkg.getCurrentVersion());
        dto.setViewCount(pkg.getViewCount());
        dto.setDownloadCount(pkg.getDownloadCount());
        dto.setSubscriptionCount(pkg.getSubscriptionCount());
        dto.setStatus(pkg.getStatus());
        dto.setStatusName(pkg.getStatus() != null ? pkg.getStatus().getName() : null);
        dto.setCreatedBy(pkg.getCreatedBy());
        dto.setCreatedAt(pkg.getCreatedAt());
        dto.setUpdatedAt(pkg.getUpdatedAt());
        dto.setPublishedBy(pkg.getPublishedBy());
        dto.setPublishedAt(pkg.getPublishedAt());
        dto.setVersionCount(pkg.getVersions() != null ? pkg.getVersions().size() : 0);
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getPackageKey() { return packageKey; }
    public void setPackageKey(String packageKey) { this.packageKey = packageKey; }
    public SoftwareType getSoftwareType() { return softwareType; }
    public void setSoftwareType(SoftwareType softwareType) { this.softwareType = softwareType; }
    public String getSoftwareTypeName() { return softwareTypeName; }
    public void setSoftwareTypeName(String softwareTypeName) { this.softwareTypeName = softwareTypeName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    public Integer getSubscriptionCount() { return subscriptionCount; }
    public void setSubscriptionCount(Integer subscriptionCount) { this.subscriptionCount = subscriptionCount; }
    public PackageStatus getStatus() { return status; }
    public void setStatus(PackageStatus status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public int getVersionCount() { return versionCount; }
    public void setVersionCount(int versionCount) { this.versionCount = versionCount; }
}
