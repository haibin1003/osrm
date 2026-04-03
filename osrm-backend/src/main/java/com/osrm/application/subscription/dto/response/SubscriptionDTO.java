package com.osrm.application.subscription.dto.response;

import com.osrm.domain.subscription.entity.Subscription;
import com.osrm.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SubscriptionDTO {

    private Long id;
    private String subscriptionNo;
    private Long userId;
    private Long packageId;
    private String packageName;
    private Long versionId;
    private String versionNumber;
    private Long businessSystemId;
    private String systemName;
    private String usageScenario;
    private String deployEnvironment;
    private SubscriptionStatus status;
    private String statusName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxRenewalCount;
    private Integer currentRenewalCount;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubscriptionDTO from(Subscription sub) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.setId(sub.getId());
        dto.setSubscriptionNo(sub.getSubscriptionNo());
        dto.setUserId(sub.getUserId());
        dto.setPackageId(sub.getPackageId());
        dto.setVersionId(sub.getVersionId());
        dto.setBusinessSystemId(sub.getBusinessSystemId());
        dto.setUsageScenario(sub.getUsageScenario());
        dto.setDeployEnvironment(sub.getDeployEnvironment());
        dto.setStatus(sub.getStatus());
        dto.setStatusName(sub.getStatus() != null ? sub.getStatus().getName() : null);
        dto.setStartDate(sub.getStartDate());
        dto.setEndDate(sub.getEndDate());
        dto.setMaxRenewalCount(sub.getMaxRenewalCount());
        dto.setCurrentRenewalCount(sub.getCurrentRenewalCount());
        dto.setApprovedBy(sub.getApprovedBy());
        dto.setApprovedAt(sub.getApprovedAt());
        dto.setRejectReason(sub.getRejectReason());
        dto.setCreatedAt(sub.getCreatedAt());
        dto.setUpdatedAt(sub.getUpdatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubscriptionNo() { return subscriptionNo; }
    public void setSubscriptionNo(String subscriptionNo) { this.subscriptionNo = subscriptionNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public String getVersionNumber() { return versionNumber; }
    public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }

    public Long getBusinessSystemId() { return businessSystemId; }
    public void setBusinessSystemId(Long businessSystemId) { this.businessSystemId = businessSystemId; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getUsageScenario() { return usageScenario; }
    public void setUsageScenario(String usageScenario) { this.usageScenario = usageScenario; }

    public String getDeployEnvironment() { return deployEnvironment; }
    public void setDeployEnvironment(String deployEnvironment) { this.deployEnvironment = deployEnvironment; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getMaxRenewalCount() { return maxRenewalCount; }
    public void setMaxRenewalCount(Integer maxRenewalCount) { this.maxRenewalCount = maxRenewalCount; }

    public Integer getCurrentRenewalCount() { return currentRenewalCount; }
    public void setCurrentRenewalCount(Integer currentRenewalCount) { this.currentRenewalCount = currentRenewalCount; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
