package com.osrm.domain.business.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 业务系统实体
 * 合并原 SystemCatalog 后的统一系统实体
 */
@Entity
@Table(name = "t_business_system")
public class BusinessSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_code", nullable = false, unique = true, length = 32)
    private String systemCode;

    @Column(name = "system_name", nullable = false, unique = true, length = 64)
    private String systemName;

    @Column(name = "system_alias", length = 128)
    private String systemAlias;

    @Column(name = "unit", length = 64)
    private String unit;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "domain_l1", length = 64)
    private String domainL1;

    @Column(name = "domain_l2", length = 64)
    private String domainL2;

    @Column(name = "domain_l3", length = 64)
    private String domainL3;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SystemStatus status = SystemStatus.IN_USE;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "has_applications", nullable = false)
    private Boolean hasApplications = false;

    @Column(name = "vendor", length = 128)
    private String vendor;

    @Column(name = "level", length = 32)
    private String level;

    @Column(name = "responsible_dept", length = 64)
    private String responsibleDept;

    @Column(name = "responsible_person", length = 64)
    private String responsiblePerson;

    @Column(name = "responsible_phone", length = 20)
    private String responsiblePhone;

    @Column(name = "online_date")
    private LocalDate onlineDate;

    @Column(name = "build_mode", length = 20)
    private String buildMode;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SystemStatus {
        IN_USE("在用"),
        OFFLINE("下线"),
        OFFLINE_REFERENCE("下线参考"),
        BUILDING("建设中");

        private final String name;

        SystemStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemAlias() {
        return systemAlias;
    }

    public void setSystemAlias(String systemAlias) {
        this.systemAlias = systemAlias;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDomainL1() {
        return domainL1;
    }

    public void setDomainL1(String domainL1) {
        this.domainL1 = domainL1;
    }

    public String getDomainL2() {
        return domainL2;
    }

    public void setDomainL2(String domainL2) {
        this.domainL2 = domainL2;
    }

    public String getDomainL3() {
        return domainL3;
    }

    public void setDomainL3(String domainL3) {
        this.domainL3 = domainL3;
    }

    public SystemStatus getStatus() {
        return status;
    }

    public void setStatus(SystemStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getHasApplications() {
        return hasApplications;
    }

    public void setHasApplications(Boolean hasApplications) {
        this.hasApplications = hasApplications;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getResponsibleDept() {
        return responsibleDept;
    }

    public void setResponsibleDept(String responsibleDept) {
        this.responsibleDept = responsibleDept;
    }

    public String getResponsiblePerson() {
        return responsiblePerson;
    }

    public void setResponsiblePerson(String responsiblePerson) {
        this.responsiblePerson = responsiblePerson;
    }

    public String getResponsiblePhone() {
        return responsiblePhone;
    }

    public void setResponsiblePhone(String responsiblePhone) {
        this.responsiblePhone = responsiblePhone;
    }

    public LocalDate getOnlineDate() {
        return onlineDate;
    }

    public void setOnlineDate(LocalDate onlineDate) {
        this.onlineDate = onlineDate;
    }

    public String getBuildMode() {
        return buildMode;
    }

    public void setBuildMode(String buildMode) {
        this.buildMode = buildMode;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
