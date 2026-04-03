package com.osrm.application.tracking.dto;

import java.util.List;

/**
 * 系统-软件关联图谱 DTO
 */
public class RelationshipGraphDTO {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private GraphMetadata metadata;

    public RelationshipGraphDTO() {}

    public RelationshipGraphDTO(List<GraphNode> nodes, List<GraphEdge> edges, GraphMetadata metadata) {
        this.nodes = nodes;
        this.edges = edges;
        this.metadata = metadata;
    }

    // Getters and Setters
    public List<GraphNode> getNodes() { return nodes; }
    public void setNodes(List<GraphNode> nodes) { this.nodes = nodes; }
    public List<GraphEdge> getEdges() { return edges; }
    public void setEdges(List<GraphEdge> edges) { this.edges = edges; }
    public GraphMetadata getMetadata() { return metadata; }
    public void setMetadata(GraphMetadata metadata) { this.metadata = metadata; }
}
