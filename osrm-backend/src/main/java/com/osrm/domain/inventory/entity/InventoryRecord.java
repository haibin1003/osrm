package com.osrm.domain.inventory.entity;

import com.osrm.domain.business.entity.BusinessSystem;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 存量登记表实体
 * 用于登记存量业务系统已使用的开源软件
 */
@Entity
@Table(name = "t_inventory_record")
public class InventoryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_no", nullable = false, unique = true, length = 32)
    private String recordNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "package_name", nullable = false, length = 128)
    private String packageName;

    @Column(name = "version_no", length = 32)
    private String versionNo;

    @Column(name = "software_type", length = 32)
    private String softwareType;

    @Column(name = "responsible_person", nullable = false, length = 64)
    private String responsiblePerson;

    @Column(name = "business_system_id")
    private Long businessSystemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_system_id", insertable = false, updatable = false)
    private BusinessSystem businessSystem;

    @Column(name = "deploy_environment", length = 32)
    private String deployEnvironment;

    @Column(name = "server_count")
    private Integer serverCount = 1;

    @Column(name = "usage_scenario", length = 512)
    private String usageScenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType = SourceType.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InventoryStatus status = InventoryStatus.PENDING;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reject_reason", length = 256)
    private String rejectReason;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 来源类型枚举
    public enum SourceType {
        MANUAL("手动登记"),
        IMPORT("批量导入");

        private final String name;

        SourceType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // 状态枚举
    public enum InventoryStatus {
        PENDING("待审批"),
        APPROVED("已批准"),
        REJECTED("已驳回");

        private final String name;

        InventoryStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // 业务方法：批准
    public void approve(Long approverId) {
        this.status = InventoryStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = null;
    }

    // 业务方法：驳回
    public void reject(Long approverId, String reason) {
        this.status = InventoryStatus.REJECTED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = reason;
    }

    // 业务方法：是否可以编辑
    public boolean canEdit() {
        return this.status == InventoryStatus.PENDING;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getSoftwareType() {
        return softwareType;
    }

    public void setSoftwareType(String softwareType) {
        this.softwareType = softwareType;
    }

    public String getResponsiblePerson() {
        return responsiblePerson;
    }

    public void setResponsiblePerson(String responsiblePerson) {
        this.responsiblePerson = responsiblePerson;
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

    public String getDeployEnvironment() {
        return deployEnvironment;
    }

    public void setDeployEnvironment(String deployEnvironment) {
        this.deployEnvironment = deployEnvironment;
    }

    public Integer getServerCount() {
        return serverCount;
    }

    public void setServerCount(Integer serverCount) {
        this.serverCount = serverCount;
    }

    public String getUsageScenario() {
        return usageScenario;
    }

    public void setUsageScenario(String usageScenario) {
        this.usageScenario = usageScenario;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
}
