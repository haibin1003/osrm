package com.osrm.application.software.dto.response;

import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.entity.VersionStatus;

import java.time.LocalDateTime;

public class SoftwareVersionDTO {

    private Long id;
    private Long packageId;
    private String versionNo;
    private VersionStatus status;
    private String statusName;
    private Long storageBackendId;
    private String storageBackendName;
    private String storagePath;
    private String artifactUrl;
    private String releaseNotes;
    private Long fileSize;
    private String checksum;
    private Boolean isLatest;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public static SoftwareVersionDTO from(SoftwareVersion ver) {
        SoftwareVersionDTO dto = new SoftwareVersionDTO();
        dto.setId(ver.getId());
        dto.setPackageId(ver.getSoftwarePackage() != null ? ver.getSoftwarePackage().getId() : null);
        dto.setVersionNo(ver.getVersionNo());
        dto.setStatus(ver.getStatus());
        dto.setStatusName(ver.getStatus() != null ? ver.getStatus().getName() : null);
        dto.setStorageBackendId(ver.getStorageBackendId());
        dto.setStoragePath(ver.getStoragePath());
        dto.setArtifactUrl(ver.getArtifactUrl());
        dto.setReleaseNotes(ver.getReleaseNotes());
        dto.setFileSize(ver.getFileSize());
        dto.setChecksum(ver.getChecksum());
        dto.setIsLatest(ver.getIsLatest());
        dto.setPublishedBy(ver.getPublishedBy());
        dto.setPublishedAt(ver.getPublishedAt());
        dto.setCreatedAt(ver.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
    public VersionStatus getStatus() { return status; }
    public void setStatus(VersionStatus status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Long getStorageBackendId() { return storageBackendId; }
    public void setStorageBackendId(Long storageBackendId) { this.storageBackendId = storageBackendId; }
    public String getStorageBackendName() { return storageBackendName; }
    public void setStorageBackendName(String storageBackendName) { this.storageBackendName = storageBackendName; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
