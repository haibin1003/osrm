package com.osrm.application.catalog.dto.response;

import com.osrm.domain.business.entity.BusinessSystemApplication;

/**
 * 业务系统子应用 DTO (原 ApplicationCatalogDTO)。
 */
public class ApplicationCatalogDTO {

    private Long id;
    private String applicationCode;
    private Long businessSystemId;
    private String applicationName;
    private String status;
    private String description;
    private String vendor;
    private String responsiblePerson;
    private String responsiblePhone;
    private Boolean enabled;

    public static ApplicationCatalogDTO from(BusinessSystemApplication entity) {
        if (entity == null) {
            return null;
        }
        ApplicationCatalogDTO dto = new ApplicationCatalogDTO();
        dto.id = entity.getId();
        dto.applicationCode = entity.getApplicationCode();
        dto.businessSystemId = entity.getBusinessSystemId();
        dto.applicationName = entity.getApplicationName();
        dto.status = entity.getStatus() != null ? entity.getStatus().name() : null;
        dto.description = entity.getDescription();
        dto.vendor = entity.getVendor();
        dto.responsiblePerson = entity.getResponsiblePerson();
        dto.responsiblePhone = entity.getResponsiblePhone();
        dto.enabled = entity.getEnabled();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApplicationCode() { return applicationCode; }
    public void setApplicationCode(String applicationCode) { this.applicationCode = applicationCode; }

    public Long getBusinessSystemId() { return businessSystemId; }
    public void setBusinessSystemId(Long businessSystemId) { this.businessSystemId = businessSystemId; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getResponsiblePhone() { return responsiblePhone; }
    public void setResponsiblePhone(String responsiblePhone) { this.responsiblePhone = responsiblePhone; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
