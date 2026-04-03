package com.osrm.application.tracking.dto;

/**
 * 业务系统节点
 */
public class SystemNode extends GraphNode {
    private Long systemId;
    private String systemCode;
    private String domain;
    private Boolean enabled;

    public SystemNode() {
        super();
    }

    public SystemNode(String id, String name, Long systemId, String systemCode,
                      String domain, Boolean enabled) {
        super(id, "system", name);
        this.systemId = systemId;
        this.systemCode = systemCode;
        this.domain = domain;
        this.enabled = enabled;
    }

    // Getters and Setters
    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
