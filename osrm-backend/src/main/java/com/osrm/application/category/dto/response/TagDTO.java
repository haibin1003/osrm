package com.osrm.application.category.dto.response;

import com.osrm.domain.category.entity.Tag;

import java.time.LocalDateTime;

public class TagDTO {

    private Long id;
    private String tagName;
    private String tagCode;
    private String description;
    private LocalDateTime createdAt;

    public static TagDTO from(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setTagName(tag.getTagName());
        dto.setTagCode(tag.getTagCode());
        dto.setDescription(tag.getDescription());
        dto.setCreatedAt(tag.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
