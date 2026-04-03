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
 * Nexus 客户端实现
 */
@Component
public class NexusClientImpl implements NexusClient {

    private static final Logger logger = LoggerFactory.getLogger(NexusClientImpl.class);

    private final RestTemplate restTemplate;

    public NexusClientImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ConnectionTestResultDTO testConnection(NexusConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            // Use /service/rest/v1/status which is public and returns 200 when healthy
            getStatus(config);
            long responseTime = System.currentTimeMillis() - startTime;

            ConnectionTestResultDTO result = ConnectionTestResultDTO.success(
                "连接成功，Nexus 服务运行正常",
                Map.of("status", "ONLINE")
            );
            result.setResponseTimeMs(responseTime);
            return result;

        } catch (HttpClientErrorException.Unauthorized e) {
            logger.warn("Nexus 认证失败: {}", config.getHost());
            return ConnectionTestResultDTO.failure("认证失败，请检查用户名和密码");
        } catch (ResourceAccessException e) {
            logger.warn("Nexus 连接失败: {}", config.getHost(), e);
            return ConnectionTestResultDTO.failure("连接失败，请检查网络或地址是否正确");
        } catch (Exception e) {
            logger.error("Nexus 连接测试异常: {}", config.getHost(), e);
            return ConnectionTestResultDTO.failure("连接异常: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatus(NexusConfig config) {
        // /service/rest/v1/status returns 200 with empty body when healthy
        String url = config.getBaseUrl() + "/service/rest/v1/status";

        HttpHeaders headers = createAuthHeaders(config.getUsername(), config.getPassword());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return Map.of("status", "ONLINE");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getStatusCheck(NexusConfig config) {
        String url = config.getBaseUrl() + "/service/rest/v1/status/check";

        HttpHeaders headers = createAuthHeaders(config.getUsername(), config.getPassword());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, Map.class
        );

        Map<String, Object> body = response.getBody();
        return body != null ? body : Map.of("status", "ONLINE");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listRepositories(NexusConfig config) {
        String url = config.getBaseUrl() + "/service/rest/v1/repositories";

        HttpHeaders headers = createAuthHeaders(config.getUsername(), config.getPassword());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, List.class
        );

        return response.getBody();
    }

    @Override
    public boolean repositoryExists(NexusConfig config, String repoName) {
        if (repoName == null || repoName.isEmpty()) {
            return true; // 未配置仓库，默认存在
        }

        try {
            List<Map<String, Object>> repos = listRepositories(config);
            if (repos == null) {
                return false;
            }

            return repos.stream()
                .anyMatch(repo -> repoName.equals(repo.get("name")));
        } catch (Exception e) {
            logger.warn("检查 Nexus 仓库是否存在失败: {}", repoName, e);
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
