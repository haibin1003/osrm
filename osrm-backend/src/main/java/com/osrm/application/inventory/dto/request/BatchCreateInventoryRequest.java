package com.osrm.application.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BatchCreateInventoryRequest {

    @NotNull(message = "请选择系统")
    private Long businessSystemId;

    private Long businessSystemApplicationId;

    @NotEmpty(message = "至少需要添加一个软件")
    @Valid
    private List<SoftwareEntry> softwareEntries;

    public Long getBusinessSystemId() { return businessSystemId; }
    public void setBusinessSystemId(Long businessSystemId) { this.businessSystemId = businessSystemId; }

    public Long getBusinessSystemApplicationId() { return businessSystemApplicationId; }
    public void setBusinessSystemApplicationId(Long businessSystemApplicationId) { this.businessSystemApplicationId = businessSystemApplicationId; }

    public List<SoftwareEntry> getSoftwareEntries() { return softwareEntries; }
    public void setSoftwareEntries(List<SoftwareEntry> softwareEntries) { this.softwareEntries = softwareEntries; }

    public static class SoftwareEntry {

        private Long packageId;

        @Size(max = 128, message = "软件名称长度不能超过128")
        private String packageName;

        @Size(max = 32, message = "版本号长度不能超过32")
        private String versionNo;

        @Size(max = 32, message = "软件类型长度不能超过32")
        private String softwareType;

        @Size(max = 32, message = "部署环境长度不能超过32")
        private String deployEnvironment;

        private Integer serverCount = 1;

        @Size(max = 512, message = "使用场景描述长度不能超过512")
        private String usageScenario;

        private String remarks;

        public Long getPackageId() { return packageId; }
        public void setPackageId(Long packageId) { this.packageId = packageId; }

        public String getPackageName() { return packageName; }
        public void setPackageName(String packageName) { this.packageName = packageName; }

        public String getVersionNo() { return versionNo; }
        public void setVersionNo(String versionNo) { this.versionNo = versionNo; }

        public String getSoftwareType() { return softwareType; }
        public void setSoftwareType(String softwareType) { this.softwareType = softwareType; }

        public String getDeployEnvironment() { return deployEnvironment; }
        public void setDeployEnvironment(String deployEnvironment) { this.deployEnvironment = deployEnvironment; }

        public Integer getServerCount() { return serverCount; }
        public void setServerCount(Integer serverCount) { this.serverCount = serverCount; }

        public String getUsageScenario() { return usageScenario; }
        public void setUsageScenario(String usageScenario) { this.usageScenario = usageScenario; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}
