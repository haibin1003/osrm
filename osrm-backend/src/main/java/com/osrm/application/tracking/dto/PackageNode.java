package com.osrm.application.tracking.dto;

/**
 * 软件包节点
 */
public class PackageNode extends GraphNode {
    private Long packageId;
    private String packageKey;
    private String softwareType;
    private String status;

    public PackageNode() {
        super();
    }

    public PackageNode(String id, String name, Long packageId, String packageKey,
                       String softwareType, String status) {
        super(id, "package", name);
        this.packageId = packageId;
        this.packageKey = packageKey;
        this.softwareType = softwareType;
        this.status = status;
    }

    // Getters and Setters
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getPackageKey() { return packageKey; }
    public void setPackageKey(String packageKey) { this.packageKey = packageKey; }
    public String getSoftwareType() { return softwareType; }
    public void setSoftwareType(String softwareType) { this.softwareType = softwareType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
