package com.osrm.application.h5.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class H5InventorySubmitRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64")
    private String responsiblePerson;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 64, message = "部门长度不能超过64")
    private String department;

    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @NotNull(message = "请选择系统")
    private Long systemCatalogId;

    private Long applicationCatalogId;

    @NotEmpty(message = "至少需要添加一个软件")
    @Valid
    private List<SoftwareEntry> softwareEntries;

    @NotBlank(message = "验证码不能为空")
    private String captchaKey;

    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    // --- Software entry ---
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

    // --- Getters/Setters ---
    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getSystemCatalogId() { return systemCatalogId; }
    public void setSystemCatalogId(Long systemCatalogId) { this.systemCatalogId = systemCatalogId; }

    public Long getApplicationCatalogId() { return applicationCatalogId; }
    public void setApplicationCatalogId(Long applicationCatalogId) { this.applicationCatalogId = applicationCatalogId; }

    public List<SoftwareEntry> getSoftwareEntries() { return softwareEntries; }
    public void setSoftwareEntries(List<SoftwareEntry> softwareEntries) { this.softwareEntries = softwareEntries; }

    public String getCaptchaKey() { return captchaKey; }
    public void setCaptchaKey(String captchaKey) { this.captchaKey = captchaKey; }

    public String getCaptchaCode() { return captchaCode; }
    public void setCaptchaCode(String captchaCode) { this.captchaCode = captchaCode; }
}
