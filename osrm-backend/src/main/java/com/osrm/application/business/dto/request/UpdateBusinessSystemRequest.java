package com.osrm.application.business.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateBusinessSystemRequest {

    @NotNull(message = "系统名称不能为空")
    @Size(min = 2, max = 64, message = "系统名称长度在2到64个字符")
    private String systemName;

    @Size(max = 64, message = "一级域长度不能超过64")
    private String domainL1;

    @Size(max = 64, message = "二级域长度不能超过64")
    private String domainL2;

    @Size(max = 64, message = "三级域长度不能超过64")
    private String domainL3;

    @Size(max = 64, message = "负责人长度不能超过64")
    private String responsiblePerson;

    @Size(max = 512, message = "描述长度不能超过512")
    private String description;

    // Getters and Setters

    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    public String getDomainL1() { return domainL1; }
    public void setDomainL1(String domainL1) { this.domainL1 = domainL1; }

    public String getDomainL2() { return domainL2; }
    public void setDomainL2(String domainL2) { this.domainL2 = domainL2; }

    public String getDomainL3() { return domainL3; }
    public void setDomainL3(String domainL3) { this.domainL3 = domainL3; }

    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
