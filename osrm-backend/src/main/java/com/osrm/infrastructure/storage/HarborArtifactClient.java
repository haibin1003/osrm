package com.osrm.infrastructure.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Harbor 制品客户端
 * Docker / Helm 制品不直接走 HTTP 上传，而是生成 docker push / helm push 命令供客户端执行。
 * 同时提供验证 Harbor 项目是否存在的能力。
 */
public class HarborArtifactClient {

    private static final Logger logger = LoggerFactory.getLogger(HarborArtifactClient.class);

    private final String endpoint;
    private final String username;
    private final String password;
    private final StorageBackendConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HarborArtifactClient(String endpoint, String username, String password, StorageBackendConfig config) {
        this.endpoint = endpoint.replaceAll("/$", "");
        this.username = username;
        this.password = password;
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 生成 Docker 镜像推送命令
     *
     * @param imageName 镜像名，如 nginx
     * @param tag       镜像标签，如 1.25.0
     * @return 推送命令字符串（多行）
     */
    public String generateDockerPushCommand(String imageName, String tag) {
        String project = config.getProject();
        // 提取 host（去掉 http:// 或 https://）
        String host = endpoint.replaceFirst("https?://", "");
        String fullImage = host + "/" + project + "/" + imageName + ":" + tag;

        return String.join("\n",
                "# 1. 登录 Harbor",
                "docker login " + host + " -u " + username + " -p <password>",
                "",
                "# 2. 为本地镜像打标签",
                "docker tag " + imageName + ":" + tag + " " + fullImage,
                "",
                "# 3. 推送镜像",
                "docker push " + fullImage
        );
    }

    /**
     * 生成 Helm Chart 推送命令（使用 helm cm-push 或 OCI registry）
     *
     * @param chartName    chart 名称
     * @param chartVersion chart 版本
     * @return 推送命令字符串（多行）
     */
    public String generateHelmPushCommand(String chartName, String chartVersion) {
        String project = config.getProject();
        String host = endpoint.replaceFirst("https?://", "");
        String ociUrl = "oci://" + host + "/" + project;

        return String.join("\n",
                "# 使用 OCI 协议推送 Helm Chart（需要 Helm 3.8+）",
                "helm registry login " + host + " --username " + username + " --password <password>",
                "",
                "# 推送已打包的 chart",
                "helm push " + chartName + "-" + chartVersion + ".tgz " + ociUrl,
                "",
                "# 拉取示例",
                "helm pull " + ociUrl + "/" + chartName + " --version " + chartVersion
        );
    }

    /**
     * 获取制品 URL（Harbor Web UI 地址）
     *
     * @param imageName 镜像/Chart 名称
     * @param tag       标签/版本
     * @return Harbor UI 中的完整地址
     */
    public String getArtifactUrl(String imageName, String tag) {
        String project = config.getProject();
        return endpoint + "/harbor/projects/" + project + "/repositories/" + imageName + "/artifacts/" + tag;
    }

    /**
     * 验证 Harbor 项目是否存在
     *
     * @param projectName 项目名称
     * @return true 如果存在
     */
    public boolean projectExists(String projectName) {
        try {
            String url = endpoint + "/api/v2.0/projects/" + projectName;
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("检查 Harbor 项目失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查连接是否正常（调用 Harbor 系统信息接口）
     */
    public boolean checkConnection() {
        try {
            String url = endpoint + "/api/v2.0/systeminfo";
            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> req = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Harbor 连接检查失败: {}", e.getMessage());
            return false;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        headers.set("Authorization", "Basic " + auth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
