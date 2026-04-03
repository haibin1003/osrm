package com.osrm.application.upload.dto;

import java.time.LocalDateTime;

public class FileUploadDTO {

    private Long id;
    private String originalName;
    private Long fileSize;
    private String contentType;
    private String relatedType;
    private Long relatedId;
    private LocalDateTime createdAt;

    public static FileUploadDTO from(com.osrm.domain.upload.entity.FileUpload f) {
        FileUploadDTO dto = new FileUploadDTO();
        dto.setId(f.getId());
        dto.setOriginalName(f.getOriginalName());
        dto.setFileSize(f.getFileSize());
        dto.setContentType(f.getContentType());
        dto.setRelatedType(f.getRelatedType());
        dto.setRelatedId(f.getRelatedId());
        dto.setCreatedAt(f.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getRelatedType() { return relatedType; }
    public void setRelatedType(String relatedType) { this.relatedType = relatedType; }
    public Long getRelatedId() { return relatedId; }
    public void setRelatedId(Long relatedId) { this.relatedId = relatedId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
