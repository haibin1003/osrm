package com.osrm.application.category.dto.response;

import com.osrm.domain.category.entity.Category;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryDTO {

    private Long id;
    private String categoryName;
    private String categoryCode;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private String description;
    private List<CategoryDTO> children;

    public static CategoryDTO from(Category cat) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(cat.getId());
        dto.setCategoryName(cat.getCategoryName());
        dto.setCategoryCode(cat.getCategoryCode());
        dto.setParentId(cat.getParent() != null ? cat.getParent().getId() : null);
        dto.setParentName(cat.getParent() != null ? cat.getParent().getCategoryName() : null);
        dto.setSortOrder(cat.getSortOrder());
        dto.setDescription(cat.getDescription());
        if (cat.getChildren() != null && !cat.getChildren().isEmpty()) {
            dto.setChildren(cat.getChildren().stream().map(CategoryDTO::from).collect(Collectors.toList()));
        }
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<CategoryDTO> getChildren() { return children; }
    public void setChildren(List<CategoryDTO> children) { this.children = children; }
}
