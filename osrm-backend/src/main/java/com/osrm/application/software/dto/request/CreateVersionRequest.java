package com.osrm.application.software.dto.request;

import jakarta.validation.constraints.*;

public class CreateVersionRequest {

    @NotBlank(message = "版本号不能为空")
    @Size(min = 1, max = 32, message = "版本号长度在1到32个字符")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+([a-zA-Z0-9._-]*)$", message = "版本号格式应为 SemVer (如 1.0.0)")
    private String versionNo;

    @NotNull(message = "存储后端不能为空")
    private Long storageBackendId;

    @Size(max = 512, message = "存储路径长度不能超过512")
    private String storagePath;

    @Size(max = 512, message = "制品URL长度不能超过512")
    private String artifactUrl;

    @Size(max = 2000, message = "发行说明长度不能超过2000")
    private String releaseNotes;

    private Long fileSize;

    @Size(max = 128, message = "校验和长度不能超过128")
    private String checksum;

    // Getters and Setters
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
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
}
