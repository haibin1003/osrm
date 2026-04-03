package com.osrm.infrastructure.storage.client;

/**
 * Nexus 配置
 */
public class NexusConfig {

    private String host;
    private String protocol = "HTTPS";
    private String username;
    private String password;
    private String mavenRepo;
    private String npmRepo;
    private String pypiRepo;
    private String rawRepo;

    public static NexusConfig fromEndpoint(String endpoint, String username, String password) {
        NexusConfig config = new NexusConfig();
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

    public String getMavenRepo() {
        return mavenRepo;
    }

    public void setMavenRepo(String mavenRepo) {
        this.mavenRepo = mavenRepo;
    }

    public String getNpmRepo() {
        return npmRepo;
    }

    public void setNpmRepo(String npmRepo) {
        this.npmRepo = npmRepo;
    }

    public String getPypiRepo() {
        return pypiRepo;
    }

    public void setPypiRepo(String pypiRepo) {
        this.pypiRepo = pypiRepo;
    }

    public String getRawRepo() {
        return rawRepo;
    }

    public void setRawRepo(String rawRepo) {
        this.rawRepo = rawRepo;
    }
}
