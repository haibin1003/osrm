package com.osrm.application.tracking.dto;

/**
 * 图谱节点抽象类
 */
public abstract class GraphNode {
    private String id;
    private String type; // "system" | "package"
    private String name;

    public GraphNode() {}

    public GraphNode(String id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
