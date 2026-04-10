package com.osrm.application.inventory.dto.response;

import com.osrm.domain.inventory.entity.InventoryRecord;

import java.time.LocalDateTime;

/**
 * 存量登记DTO
 */
public class InventoryDTO {

    private Long id;
    private String recordNo;
    private Long userId;
    private String userName;
    private Long packageId;
    private String packageName;
    private String versionNo;
    private String softwareType;
    private String responsiblePerson;
    private Long businessSystemId;
    private String businessSystemName;
    private String deployEnvironment;
    private Integer serverCount;
    private String usageScenario;
    private String sourceType;
    private String status;
    private String statusName;
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InventoryDTO from(InventoryRecord record) {
        InventoryDTO dto = new InventoryDTO();
        dto.setId(record.getId());
        dto.setRecordNo(record.getRecordNo());
        dto.setUserId(record.getUserId());
        dto.setPackageId(record.getPackageId());
        dto.setPackageName(record.getPackageName());
        dto.setVersionNo(record.getVersionNo());
        dto.setSoftwareType(record.getSoftwareType());
        dto.setResponsiblePerson(record.getResponsiblePerson());
        dto.setBusinessSystemId(record.getBusinessSystemId());
        dto.setDeployEnvironment(record.getDeployEnvironment());
        dto.setServerCount(record.getServerCount());
        dto.setUsageScenario(record.getUsageScenario());
        dto.setSourceType(record.getSourceType() != null ? record.getSourceType().name() : null);
        dto.setStatus(record.getStatus() != null ? record.getStatus().name() : null);
        dto.setStatusName(record.getStatus() != null ? record.getStatus().getName() : null);
        dto.setApprovedBy(record.getApprovedBy());
        dto.setApprovedAt(record.getApprovedAt());
        dto.setRejectReason(record.getRejectReason());
        dto.setRemarks(record.getRemarks());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }

    public String getSoftwareType() { return softwareType; }
    public void setSoftwareType(String softwareType) { this.softwareType = softwareType; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public Long getBusinessSystemId() { return businessSystemId; }
    public void setBusinessSystemId(Long businessSystemId) { this.businessSystemId = businessSystemId; }

    public String getBusinessSystemName() { return businessSystemName; }
    public void setBusinessSystemName(String businessSystemName) { this.businessSystemName = businessSystemName; }

    public String getDeployEnvironment() { return deployEnvironment; }
    public void setDeployEnvironment(String deployEnvironment) { this.deployEnvironment = deployEnvironment; }

    public Integer getServerCount() { return serverCount; }
    public void setServerCount(Integer serverCount) { this.serverCount = serverCount; }

    public String getUsageScenario() { return usageScenario; }
    public void setUsageScenario(String usageScenario) { this.usageScenario = usageScenario; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
