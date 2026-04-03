package com.osrm.application.software.dto.request;

import jakarta.validation.constraints.*;

public class UpdatePackageRequest {

    @NotBlank(message = "软件包名称不能为空")
    @Size(min = 2, max = 128, message = "软件包名称长度在2到128个字符")
    private String packageName;

    private Long categoryId;

    @Size(max = 5000, message = "描述长度不能超过5000")
    private String description;

    @Size(max = 256, message = "官网链接长度不能超过256")
    private String websiteUrl;

    @Size(max = 64, message = "许可证类型长度不能超过64")
    private String licenseType;

    @Size(max = 256, message = "许可证链接长度不能超过256")
    private String licenseUrl;

    @Size(max = 256, message = "源码链接长度不能超过256")
    private String sourceUrl;

    // Getters and Setters
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
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
}
