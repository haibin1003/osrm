package com.osrm.application.catalog.dto.response;

import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.entity.BusinessSystemApplication;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务系统 DTO (原 SystemCatalogDTO)。
 */
public class SystemCatalogDTO {

    private Long id;
    private String systemCode;
    private String systemName;
    private String systemAlias;
    private String unit;
    private String category;
    private String domainL1;
    private String domainL2;
    private String domainL3;
    private String status;
    private String description;
    private Boolean hasApplications;
    private String vendor;
    private String level;
    private String responsibleDept;
    private String responsiblePerson;
    private String responsiblePhone;
    private LocalDate onlineDate;
    private String buildMode;
    private String tags;
    private Boolean enabled;

    private List<ApplicationCatalogDTO> applications;

    public static SystemCatalogDTO from(BusinessSystem entity) {
        if (entity == null) {
            return null;
        }
        SystemCatalogDTO dto = new SystemCatalogDTO();
        dto.id = entity.getId();
        dto.systemCode = entity.getSystemCode();
        dto.systemName = entity.getSystemName();
        dto.systemAlias = entity.getSystemAlias();
        dto.unit = entity.getUnit();
        dto.category = entity.getCategory();
        dto.domainL1 = entity.getDomainL1();
        dto.domainL2 = entity.getDomainL2();
        dto.domainL3 = entity.getDomainL3();
        dto.status = entity.getStatus() != null ? entity.getStatus().name() : null;
        dto.description = entity.getDescription();
        dto.hasApplications = entity.getHasApplications();
        dto.vendor = entity.getVendor();
        dto.level = entity.getLevel();
        dto.responsibleDept = entity.getResponsibleDept();
        dto.responsiblePerson = entity.getResponsiblePerson();
        dto.responsiblePhone = entity.getResponsiblePhone();
        dto.onlineDate = entity.getOnlineDate();
        dto.buildMode = entity.getBuildMode();
        dto.tags = entity.getTags();
        dto.enabled = entity.getEnabled();
        return dto;
    }

    public static SystemCatalogDTO fromWithApplications(BusinessSystem entity, List<BusinessSystemApplication> apps) {
        SystemCatalogDTO dto = from(entity);
        if (dto != null && apps != null) {
            dto.applications = apps.stream()
                    .map(ApplicationCatalogDTO::from)
                    .collect(Collectors.toList());
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getSystemAlias() { return systemAlias; }
    public void setSystemAlias(String systemAlias) { this.systemAlias = systemAlias; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDomainL1() { return domainL1; }
    public void setDomainL1(String domainL1) { this.domainL1 = domainL1; }

    public String getDomainL2() { return domainL2; }
    public void setDomainL2(String domainL2) { this.domainL2 = domainL2; }

    public String getDomainL3() { return domainL3; }
    public void setDomainL3(String domainL3) { this.domainL3 = domainL3; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getHasApplications() { return hasApplications; }
    public void setHasApplications(Boolean hasApplications) { this.hasApplications = hasApplications; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getResponsibleDept() { return responsibleDept; }
    public void setResponsibleDept(String responsibleDept) { this.responsibleDept = responsibleDept; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getResponsiblePhone() { return responsiblePhone; }
    public void setResponsiblePhone(String responsiblePhone) { this.responsiblePhone = responsiblePhone; }

    public LocalDate getOnlineDate() { return onlineDate; }
    public void setOnlineDate(LocalDate onlineDate) { this.onlineDate = onlineDate; }

    public String getBuildMode() { return buildMode; }
    public void setBuildMode(String buildMode) { this.buildMode = buildMode; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public List<ApplicationCatalogDTO> getApplications() { return applications; }
    public void setApplications(List<ApplicationCatalogDTO> applications) { this.applications = applications; }
}
