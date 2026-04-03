package com.osrm.application.portal.dto.response;

import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;

import java.time.LocalDateTime;

public class PortalPackageDTO {

    private Long id;
    private String packageName;
    private String displayName;
    private SoftwareType softwareType;
    private String softwareTypeName;
    private String description;
    private PackageStatus status;
    private int versionCount;
    private LocalDateTime createdAt;

    public static PortalPackageDTO from(SoftwarePackage pkg) {
        PortalPackageDTO dto = new PortalPackageDTO();
        dto.setId(pkg.getId());
        dto.setPackageName(pkg.getPackageName());
        dto.setDisplayName(pkg.getPackageName());
        dto.setSoftwareType(pkg.getSoftwareType());
        dto.setSoftwareTypeName(pkg.getSoftwareType().getName());
        dto.setDescription(pkg.getDescription());
        dto.setStatus(pkg.getStatus());
        dto.setVersionCount(pkg.getVersions() != null ? pkg.getVersions().size() : 0);
        dto.setCreatedAt(pkg.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public SoftwareType getSoftwareType() { return softwareType; }
    public void setSoftwareType(SoftwareType softwareType) { this.softwareType = softwareType; }
    public String getSoftwareTypeName() { return softwareTypeName; }
    public void setSoftwareTypeName(String softwareTypeName) { this.softwareTypeName = softwareTypeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PackageStatus getStatus() { return status; }
    public void setStatus(PackageStatus status) { this.status = status; }
    public int getVersionCount() { return versionCount; }
    public void setVersionCount(int versionCount) { this.versionCount = versionCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
