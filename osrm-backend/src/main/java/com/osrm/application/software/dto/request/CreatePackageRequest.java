package com.osrm.application.software.dto.request;

import com.osrm.domain.software.entity.SoftwareType;
import jakarta.validation.constraints.*;

public class CreatePackageRequest {

    @NotBlank(message = "软件包名称不能为空")
    @Size(min = 2, max = 128, message = "软件包名称长度在2到128个字符")
    private String packageName;

    @NotBlank(message = "软件包标识不能为空")
    @Size(min = 2, max = 64, message = "软件包标识长度在2到64个字符")
    @Pattern(regexp = "^[a-z0-9_-]+$", message = "软件包标识只支持小写字母、数字、下划线和连字符")
    private String packageKey;

    @NotNull(message = "软件类型不能为空")
    private SoftwareType softwareType;

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
}
