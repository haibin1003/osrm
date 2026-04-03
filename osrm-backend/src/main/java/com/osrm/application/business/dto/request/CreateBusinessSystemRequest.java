package com.osrm.application.business.dto.request;

import com.osrm.domain.business.entity.BusinessDomain;
import jakarta.validation.constraints.*;

public class CreateBusinessSystemRequest {

    @NotBlank(message = "系统编码不能为空")
    @Size(min = 1, max = 32, message = "系统编码长度在1到32个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "系统编码只支持字母、数字、下划线和连字符")
    private String systemCode;

    @NotBlank(message = "系统名称不能为空")
    @Size(min = 2, max = 64, message = "系统名称长度在2到64个字符")
    private String systemName;

    @NotNull(message = "所属业务域不能为空")
    private BusinessDomain domain;

    @Size(max = 64, message = "负责人长度不能超过64")
    private String responsiblePerson;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    // Getters and Setters

    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public BusinessDomain getDomain() { return domain; }
    public void setDomain(BusinessDomain domain) { this.domain = domain; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
