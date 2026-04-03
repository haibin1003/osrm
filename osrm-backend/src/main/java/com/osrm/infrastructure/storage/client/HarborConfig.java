package com.osrm.infrastructure.storage.client;

/**
 * Harbor 配置
 */
public class HarborConfig {

    private String host;
    private String protocol = "HTTPS";
    private String username;
    private String password;
    private String project;
    private String apiVersion = "v2.0";

    public static HarborConfig fromEndpoint(String endpoint, String username, String password) {
        HarborConfig config = new HarborConfig();
        // 解析 endpoint，可能包含协议前缀
        if (endpoint.startsWith("http://")) {
            config.protocol = "HTTP";
            config.host = endpoint.substring(7);
        } else if (endpoint.startsWith("https://")) {
            config.protocol = "HTTPS";
            config.host = endpoint.substring(8);
        } else {
            config.host = endpoint;
        }
        config.username = username;
        config.password = password;
        return config;
    }

    public String getBaseUrl() {
        return protocol.toLowerCase() + "://" + host;
    }

    // Getters and Setters

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
