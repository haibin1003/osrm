package com.osrm.application.tracking.dto;

/**
 * 图谱边（订阅关系）
 */
public class GraphEdge {
    private String id;
    private String source;  // node id
    private String target;  // node id
    private String versionNumber;
    private String status;

    public GraphEdge() {}

    public GraphEdge(String id, String source, String target, String versionNumber, String status) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.versionNumber = versionNumber;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getVersionNumber() { return versionNumber; }
    public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
