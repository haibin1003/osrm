package com.osrm.application.category.dto.request;

import jakarta.validation.constraints.*;

public class CreateTagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(min = 1, max = 32, message = "标签名称长度在1到32个字符")
    private String tagName;

    @NotBlank(message = "标签编码不能为空")
    @Size(min = 1, max = 32, message = "标签编码长度在1到32个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "标签编码只支持字母、数字、下划线和连字符")
    private String tagCode;

    @Size(max = 128, message = "描述长度不能超过128")
    private String description;

    // Getters and Setters
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
