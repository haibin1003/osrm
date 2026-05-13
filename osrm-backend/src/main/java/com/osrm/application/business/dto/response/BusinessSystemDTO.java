package com.osrm.application.business.dto.response;

import com.osrm.domain.business.entity.BusinessSystem;

import java.time.LocalDateTime;

public class BusinessSystemDTO {

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
    private String statusName;
    private String responsiblePerson;
    private String responsibleDept;
    private String description;
    private Boolean enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BusinessSystemDTO from(BusinessSystem system) {
        BusinessSystemDTO dto = new BusinessSystemDTO();
        dto.setId(system.getId());
        dto.setSystemCode(system.getSystemCode());
        dto.setSystemName(system.getSystemName());
        dto.setSystemAlias(system.getSystemAlias());
        dto.setUnit(system.getUnit());
        dto.setCategory(system.getCategory());
        dto.setDomainL1(system.getDomainL1());
        dto.setDomainL2(system.getDomainL2());
        dto.setDomainL3(system.getDomainL3());
        dto.setStatus(system.getStatus() != null ? system.getStatus().name() : null);
        dto.setStatusName(system.getStatus() != null ? system.getStatus().getName() : null);
        dto.setResponsiblePerson(system.getResponsiblePerson());
        dto.setResponsibleDept(system.getResponsibleDept());
        dto.setDescription(system.getDescription());
        dto.setEnabled(system.getEnabled());
        dto.setCreatedBy(system.getCreatedBy());
        dto.setCreatedAt(system.getCreatedAt());
        dto.setUpdatedAt(system.getUpdatedAt());
        return dto;
    }

    // Getters and Setters

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

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getResponsibleDept() { return responsibleDept; }
    public void setResponsibleDept(String responsibleDept) { this.responsibleDept = responsibleDept; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
