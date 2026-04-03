package com.osrm.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * Nexus 制品上传客户端
 * 支持 Maven / NPM / PyPI（multipart POST）和 Raw/Generic（PUT）
 */
public class NexusArtifactClient {

    private static final Logger logger = LoggerFactory.getLogger(NexusArtifactClient.class);

    private final String endpoint;
    private final String username;
    private final String password;
    private final StorageBackendConfig config;
    private final RestTemplate restTemplate;

    public NexusArtifactClient(String endpoint, String username, String password, StorageBackendConfig config) {
        this.endpoint = endpoint.replaceAll("/$", "");
        this.username = username;
        this.password = password;
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 上传 Maven 制品
     *
     * @param groupId    com.example
     * @param artifactId my-lib
     * @param version    1.0.0
     * @param file       jar/pom/war 文件
     * @return 制品 URL
     */
    public String uploadMaven(String groupId, String artifactId, String version, MultipartFile file) throws IOException {
        String repo = config.getMavenRepo();
        String url = endpoint + "/service/rest/v1/components?repository=" + repo;

        String extension = getExtension(file.getOriginalFilename());
        String classifier = "";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("maven2.groupId", groupId);
        body.add("maven2.artifactId", artifactId);
        body.add("maven2.version", version);
        body.add("maven2.asset1", toResource(file));
        body.add("maven2.asset1.extension", extension);
        if (!classifier.isEmpty()) {
            body.add("maven2.asset1.classifier", classifier);
        }

        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            String groupPath = groupId.replace('.', '/');
            String artifactUrl = endpoint + "/repository/" + repo + "/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "." + extension;
            logger.info("Maven 制品上传成功: {}", artifactUrl);
            return artifactUrl;
        }
        throw new RuntimeException("Maven 上传失败，状态码：" + resp.getStatusCode());
    }

    /**
     * 上传 NPM 包
     *
     * @param file .tgz 文件
     * @return 制品 URL
     */
    public String uploadNpm(MultipartFile file) throws IOException {
        String repo = config.getNpmRepo();
        String url = endpoint + "/service/rest/v1/components?repository=" + repo;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("npm.asset", toResource(file));

        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            String artifactUrl = endpoint + "/repository/" + repo + "/" + file.getOriginalFilename();
            logger.info("NPM 包上传成功: {}", artifactUrl);
            return artifactUrl;
        }
        throw new RuntimeException("NPM 上传失败，状态码：" + resp.getStatusCode());
    }

    /**
     * 上传 PyPI 包
     *
     * @param file .whl 或 .tar.gz 文件
     * @return 制品 URL
     */
    public String uploadPypi(MultipartFile file) throws IOException {
        String repo = config.getPypiRepo();
        String url = endpoint + "/service/rest/v1/components?repository=" + repo;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("pypi.asset", toResource(file));

        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            String artifactUrl = endpoint + "/repository/" + repo + "/packages/" + file.getOriginalFilename();
            logger.info("PyPI 包上传成功: {}", artifactUrl);
            return artifactUrl;
        }
        throw new RuntimeException("PyPI 上传失败，状态码：" + resp.getStatusCode());
    }

    /**
     * 上传通用/Raw 文件（HTTP PUT）
     *
     * @param remotePath 仓库内路径，如 myapp/1.0.0/myapp-1.0.0.tar.gz
     * @param file       任意文件
     * @return 制品 URL
     */
    public String uploadRaw(String remotePath, MultipartFile file) throws IOException {
        String repo = config.getRawRepo();
        String url = endpoint + "/repository/" + repo + "/" + remotePath;

        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            logger.info("Raw 文件上传成功: {}", url);
            return url;
        }
        throw new RuntimeException("Raw 上传失败，状态码：" + resp.getStatusCode());
    }

    // ---- helpers ----

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        headers.set("Authorization", "Basic " + auth);
        return headers;
    }

    private ByteArrayResource toResource(MultipartFile file) throws IOException {
        final String filename = file.getOriginalFilename();
        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jar";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
