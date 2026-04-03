package com.osrm.application.business.dto.response;

import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;

import java.time.LocalDateTime;

public class BusinessSystemDTO {

    private Long id;
    private String systemCode;
    private String systemName;
    private BusinessDomain domain;
    private String domainName;
    private String responsiblePerson;
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
        dto.setDomain(system.getDomain());
        dto.setDomainName(system.getDomain() != null ? system.getDomain().getName() : null);
        dto.setResponsiblePerson(system.getResponsiblePerson());
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

    public BusinessDomain getDomain() { return domain; }
    public void setDomain(BusinessDomain domain) { this.domain = domain; }

    public String getDomainName() { return domainName; }
    public void setDomainName(String domainName) { this.domainName = domainName; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

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
