package com.osrm.domain.business.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 业务系统子应用实体
 * 原 ApplicationCatalog 迁移而来，作为 BusinessSystem 的子系统。
 */
@Entity
@Table(name = "t_business_system_application")
public class BusinessSystemApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_code", nullable = false, unique = true, length = 64)
    private String applicationCode;

    @Column(name = "business_system_id", nullable = false)
    private Long businessSystemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_system_id", insertable = false, updatable = false)
    private BusinessSystem businessSystem;

    @Column(name = "application_name", nullable = false, length = 128)
    private String applicationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.IN_USE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "vendor", length = 128)
    private String vendor;

    @Column(name = "responsible_person", length = 64)
    private String responsiblePerson;

    @Column(name = "responsible_phone", length = 20)
    private String responsiblePhone;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ApplicationStatus {
        IN_USE("在用"),
        OFFLINE("下线"),
        BUILDING("建设中");

        private final String name;

        ApplicationStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public Long getBusinessSystemId() {
        return businessSystemId;
    }

    public void setBusinessSystemId(Long businessSystemId) {
        this.businessSystemId = businessSystemId;
    }

    public BusinessSystem getBusinessSystem() {
        return businessSystem;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
