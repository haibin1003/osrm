package com.osrm.infrastructure.storage.client;

import com.osrm.application.storage.dto.ConnectionTestResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Harbor 客户端实现
 */
@Component
public class HarborClientImpl implements HarborClient {

    private static final Logger logger = LoggerFactory.getLogger(HarborClientImpl.class);

    private final RestTemplate restTemplate;

    public HarborClientImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ConnectionTestResultDTO testConnection(HarborConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            // 尝试获取系统信息
            Map<String, Object> systemInfo = getSystemInfo(config);
            long responseTime = System.currentTimeMillis() - startTime;

            String version = (String) systemInfo.get("harbor_version");
            String registryUrl = (String) systemInfo.get("registry_url");

            ConnectionTestResultDTO result = ConnectionTestResultDTO.success(
                "连接成功，Harbor 版本: " + version,
                Map.of("version", version, "registryUrl", registryUrl)
            );
            result.setResponseTimeMs(responseTime);
            return result;

        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Harbor 认证失败: {}", config.getHost());
            return ConnectionTestResultDTO.failure("认证失败，请检查用户名和密码");
        } catch (ResourceAccessException e) {
            logger.warn("Harbor 连接失败: {}", config.getHost(), e);
            return ConnectionTestResultDTO.failure("连接失败，请检查网络或地址是否正确");
        } catch (Exception e) {
            logger.error("Harbor 连接测试异常: {}", config.getHost(), e);
            return ConnectionTestResultDTO.failure("连接异常: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSystemInfo(HarborConfig config) {
        String url = config.getBaseUrl() + "/api/v2.0/systeminfo";

        HttpHeaders headers = createAuthHeaders(config.getUsername(), config.getPassword());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, Map.class
        );

        return response.getBody();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listProjects(HarborConfig config) {
        String url = config.getBaseUrl() + "/api/v2.0/projects?page_size=100";

        HttpHeaders headers = createAuthHeaders(config.getUsername(), config.getPassword());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, List.class
        );

        return response.getBody();
    }

    @Override
    public boolean projectExists(HarborConfig config, String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            return true; // 未配置项目，默认存在
        }

        try {
            String url = config.getBaseUrl() + "/api/v2.0/projects?project_name=" + projectName;

            HttpHeaders headers = createAuthHeaders(config.getUsername(), config.getPassword());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class
            );

            List<?> projects = response.getBody();
            return projects != null && !projects.isEmpty();
        } catch (Exception e) {
            logger.warn("检查 Harbor 项目是否存在失败: {}", projectName, e);
            return false;
        }
    }

    private HttpHeaders createAuthHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (username != null && password != null) {
            String auth = username + ":" + password;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
        }

        return headers;
    }
}
