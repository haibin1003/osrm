package com.osrm.application.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建存量登记请求
 */
public class CreateInventoryRequest {

    @NotBlank(message = "软件名称不能为空")
    @Size(max = 128, message = "软件名称长度不能超过128")
    private String packageName;

    private Long packageId;

    @Size(max = 32, message = "版本号长度不能超过32")
    private String versionNo;

    @Size(max = 32, message = "软件类型长度不能超过32")
    private String softwareType;

    @Size(max = 64, message = "负责人长度不能超过64")
    private String responsiblePerson;

    private Long businessSystemId;

    @Size(max = 32, message = "部署环境长度不能超过32")
    private String deployEnvironment;

    private Integer serverCount = 1;

    @Size(max = 512, message = "使用场景描述长度不能超过512")
    private String usageScenario;

    private String remarks;

    // Getters and Setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
