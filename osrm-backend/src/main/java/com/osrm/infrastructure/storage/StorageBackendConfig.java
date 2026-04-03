package com.osrm.infrastructure.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储后端配置解析
 * configJson 格式示例（Nexus）：
 * {
 *   "mavenRepo": "maven-releases",
 *   "npmRepo": "npm-hosted",
 *   "pypiRepo": "pypi-hosted",
 *   "rawRepo": "raw-hosted"
 * }
 * configJson 格式示例（Harbor）：
 * {
 *   "project": "opensource"
 * }
 */
public class StorageBackendConfig {

    private final Map<String, String> config;

    public StorageBackendConfig(String configJson) {
        Map<String, String> parsed;
        if (configJson == null || configJson.isBlank()) {
            parsed = new HashMap<>();
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                parsed = mapper.readValue(configJson,
                        mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
            } catch (JsonProcessingException e) {
                parsed = new HashMap<>();
            }
        }
        this.config = parsed;
    }

    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public String getMavenRepo() { return get("mavenRepo", "maven-releases"); }
    public String getNpmRepo()   { return get("npmRepo", "npm-hosted"); }
    public String getPypiRepo()  { return get("pypiRepo", "pypi-hosted"); }
    public String getRawRepo()   { return get("rawRepo", "raw-hosted"); }
    public String getProject()   { return get("project", "library"); }
}
