package com.osrm.application.category.dto.request;

import jakarta.validation.constraints.*;

public class CreateCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 64, message = "分类名称长度在1到64个字符")
    private String categoryName;

    @NotBlank(message = "分类编码不能为空")
    @Size(min = 1, max = 32, message = "分类编码长度在1到32个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "分类编码只支持字母、数字、下划线和连字符")
    private String categoryCode;

    private Long parentId;

    private Integer sortOrder;

    @Size(max = 256, message = "描述长度不能超过256")
    private String description;

    // Getters and Setters
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
