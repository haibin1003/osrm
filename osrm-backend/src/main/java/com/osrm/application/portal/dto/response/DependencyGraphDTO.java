package com.osrm.application.portal.dto.response;

import java.util.List;

public class DependencyGraphDTO {

    private List<Node> nodes;
    private List<Link> links;

    public static class Node {
        private String id;
        private String name;
        private String type;

        public Node() {}

        public Node(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    public static class Link {
        private String source;
        private String target;
        private String version;

        public Link() {}

        public Link(String source, String target, String version) {
            this.source = source;
            this.target = target;
            this.version = version;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    public List<Node> getNodes() { return nodes; }
    public void setNodes(List<Node> nodes) { this.nodes = nodes; }

    public List<Link> getLinks() { return links; }
    public void setLinks(List<Link> links) { this.links = links; }
}
