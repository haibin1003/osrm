package com.osrm.domain.software.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_software_version",
       uniqueConstraints = @UniqueConstraint(columnNames = {"package_id", "version_no"}))
public class SoftwareVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private SoftwarePackage softwarePackage;

    @Column(name = "version_no", nullable = false, length = 32)
    private String versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VersionStatus status = VersionStatus.DRAFT;

    @Column(name = "storage_backend_id", nullable = false)
    private Long storageBackendId;

    @Column(name = "storage_path", length = 512)
    private String storagePath;

    @Column(name = "artifact_url", length = 512)
    private String artifactUrl;

    @Column(name = "release_notes", length = 2000)
    private String releaseNotes;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "is_latest")
    private Boolean isLatest = false;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 状态流转方法
    public void publish(Long publisherId) {
        if (this.status != VersionStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态可以发布");
        }
        this.status = VersionStatus.PUBLISHED;
        this.publishedBy = publisherId;
        this.publishedAt = LocalDateTime.now();
    }

    public void offline() {
        if (this.status != VersionStatus.PUBLISHED) {
            throw new IllegalStateException("只有已发布状态可以下线");
        }
        this.status = VersionStatus.OFFLINE;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SoftwarePackage getSoftwarePackage() { return softwarePackage; }
    public void setSoftwarePackage(SoftwarePackage softwarePackage) { this.softwarePackage = softwarePackage; }
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
    public VersionStatus getStatus() { return status; }
    public void setStatus(VersionStatus status) { this.status = status; }
    public Long getStorageBackendId() { return storageBackendId; }
    public void setStorageBackendId(Long storageBackendId) { this.storageBackendId = storageBackendId; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getArtifactUrl() { return artifactUrl; }
    public void setArtifactUrl(String artifactUrl) { this.artifactUrl = artifactUrl; }
    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }
    public Long getPublishedBy() { return publishedBy; }
    public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
