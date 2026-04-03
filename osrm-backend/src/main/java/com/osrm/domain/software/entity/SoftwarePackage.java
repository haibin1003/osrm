package com.osrm.domain.software.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "t_software_package")
public class SoftwarePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_name", nullable = false, unique = true, length = 128)
    private String packageName;

    @Column(name = "package_key", nullable = false, unique = true, length = 64)
    private String packageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "software_type", nullable = false, length = 32)
    private SoftwareType softwareType;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "website_url", length = 256)
    private String websiteUrl;

    @Column(name = "license_type", length = 64)
    private String licenseType;

    @Column(name = "license_url", length = 256)
    private String licenseUrl;

    @Column(name = "source_url", length = 256)
    private String sourceUrl;

    @Column(name = "logo_url", length = 256)
    private String logoUrl;

    @Column(name = "current_version", length = 32)
    private String currentVersion;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "subscription_count")
    private Integer subscriptionCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PackageStatus status = PackageStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @OneToMany(mappedBy = "softwarePackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<SoftwareVersion> versions = new ArrayList<>();

    // 状态流转方法
    public void submit() {
        if (!status.canSubmit()) {
            throw new IllegalStateException("当前状态不能提交审核: " + status);
        }
        this.status = PackageStatus.PENDING;
    }

    public void approve(Long approverId) {
        if (!status.canApprove()) {
            throw new IllegalStateException("当前状态不能审批: " + status);
        }
        this.status = PackageStatus.PUBLISHED;
        this.publishedBy = approverId;
        this.publishedAt = LocalDateTime.now();
    }

    public void reject() {
        if (!status.canApprove()) {
            throw new IllegalStateException("当前状态不能驳回: " + status);
        }
        this.status = PackageStatus.DRAFT;
    }

    public void offline() {
        if (!status.canOffline()) {
            throw new IllegalStateException("当前状态不能下架: " + status);
        }
        this.status = PackageStatus.OFFLINE;
    }

    public void republish() {
        if (!status.canRepublish()) {
            throw new IllegalStateException("当前状态不能重新上架: " + status);
        }
        this.status = PackageStatus.PUBLISHED;
    }

    public void archive() {
        this.status = PackageStatus.ARCHIVED;
    }

    // 统计方法
    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public void incrementSubscriptionCount() {
        this.subscriptionCount++;
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
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
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
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<SoftwareVersion> getVersions() { return versions; }
    public void setVersions(List<SoftwareVersion> versions) { this.versions = versions; }
}
