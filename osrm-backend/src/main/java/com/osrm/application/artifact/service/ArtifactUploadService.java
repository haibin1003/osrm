package com.osrm.application.artifact.service;

import com.osrm.application.artifact.dto.ArtifactUploadResult;
import com.osrm.common.exception.BizException;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
import com.osrm.domain.storage.entity.StorageBackend;
import com.osrm.domain.storage.entity.StorageBackendType;
import com.osrm.domain.storage.repository.StorageBackendRepository;
import com.osrm.infrastructure.storage.HarborArtifactClient;
import com.osrm.infrastructure.storage.NexusArtifactClient;
import com.osrm.infrastructure.storage.StorageBackendConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 制品上传服务
 * 根据版本关联的存储后端类型，路由到 Nexus / Harbor / NAS（本地）
 */
@Service
@Transactional(readOnly = true)
public class ArtifactUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactUploadService.class);
    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB

    private final SoftwarePackageRepository packageRepository;
    private final SoftwareVersionRepository versionRepository;
    private final StorageBackendRepository storageBackendRepository;

    @Value("${osrm.upload.dir:./uploads}")
    private String uploadBaseDir;

    @Autowired
    public ArtifactUploadService(SoftwarePackageRepository packageRepository,
                                  SoftwareVersionRepository versionRepository,
                                  StorageBackendRepository storageBackendRepository) {
        this.packageRepository = packageRepository;
        this.versionRepository = versionRepository;
        this.storageBackendRepository = storageBackendRepository;
    }

    /**
     * 上传制品文件到指定软件版本
     */
    @Transactional
    public ArtifactUploadResult uploadArtifact(Long packageId, Long versionId, MultipartFile file) {
        if (file.isEmpty()) {
            return ArtifactUploadResult.failure("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ArtifactUploadResult.failure("文件大小不能超过500MB");
        }

        SoftwarePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new BizException("软件包不存在"));

        SoftwareVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BizException("版本不存在"));

        if (!version.getSoftwarePackage().getId().equals(packageId)) {
            return ArtifactUploadResult.failure("版本不属于该软件包");
        }

        // 查找存储后端
        StorageBackend backend = null;
        if (version.getStorageBackendId() != null) {
            backend = storageBackendRepository.findById(version.getStorageBackendId()).orElse(null);
        }

        try {
            String artifactUrl;
            String downloadCmd;
            String relativePath;
            long fileSize = file.getSize();

            if (backend != null && backend.getBackendType() == StorageBackendType.NEXUS) {
                artifactUrl = uploadToNexus(backend, pkg, version, file);
                relativePath = artifactUrl;
                downloadCmd = DownloadCommandGenerator.generate(pkg.getSoftwareType(), pkg.getPackageName(), version.getVersionNo(), file.getOriginalFilename());

            } else if (backend != null && backend.getBackendType() == StorageBackendType.HARBOR) {
                artifactUrl = uploadToHarbor(backend, pkg, version, file);
                relativePath = artifactUrl;
                downloadCmd = DownloadCommandGenerator.generate(pkg.getSoftwareType(), pkg.getPackageName(), version.getVersionNo(), file.getOriginalFilename());

            } else {
                // 本地 NAS / 无后端 fallback
                UploadResult local = saveToLocal(pkg, version, file);
                artifactUrl = local.path;
                relativePath = local.path;
                downloadCmd = DownloadCommandGenerator.generate(pkg.getSoftwareType(), pkg.getPackageName(), version.getVersionNo(), file.getOriginalFilename());
            }

            // 计算 checksum（本地保存时已有，远程上传后基于内存计算）
            String md5 = calculateMD5FromBytes(file.getBytes());

            // 更新版本记录
            version.setStoragePath(relativePath);
            version.setArtifactUrl(artifactUrl);
            version.setFileSize(fileSize);
            version.setChecksum(md5);
            versionRepository.save(version);

            logger.info("制品上传成功: package={}, version={}, backend={}, url={}",
                    pkg.getPackageName(), version.getVersionNo(),
                    backend != null ? backend.getBackendType() : "LOCAL", artifactUrl);

            return ArtifactUploadResult.success(relativePath, fileSize, md5, downloadCmd);

        } catch (IOException e) {
            logger.error("文件上传失败", e);
            return ArtifactUploadResult.failure("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("制品上传异常", e);
            return ArtifactUploadResult.failure("上传失败: " + e.getMessage());
        }
    }

    private String uploadToNexus(StorageBackend backend, SoftwarePackage pkg,
                                  SoftwareVersion version, MultipartFile file) throws IOException {
        StorageBackendConfig cfg = new StorageBackendConfig(backend.getConfigJson());
        NexusArtifactClient client = new NexusArtifactClient(
                backend.getEndpoint(), backend.getAccessKey(), backend.getSecretKey(), cfg);

        SoftwareType type = pkg.getSoftwareType();
        switch (type) {
            case MAVEN:
                // groupId 从 namespace 字段取；artifactId 为包名
                String groupId = backend.getNamespace() != null ? backend.getNamespace() : "com.osrm";
                return client.uploadMaven(groupId, pkg.getPackageKey(), version.getVersionNo(), file);
            case NPM:
                return client.uploadNpm(file);
            case PYPI:
                return client.uploadPypi(file);
            default:
                // GENERIC → Raw 仓库
                String path = pkg.getPackageKey() + "/" + version.getVersionNo() + "/" + file.getOriginalFilename();
                return client.uploadRaw(path, file);
        }
    }

    private String uploadToHarbor(StorageBackend backend, SoftwarePackage pkg,
                                   SoftwareVersion version, MultipartFile file) {
        StorageBackendConfig cfg = new StorageBackendConfig(backend.getConfigJson());
        HarborArtifactClient client = new HarborArtifactClient(
                backend.getEndpoint(), backend.getAccessKey(), backend.getSecretKey(), cfg);

        SoftwareType type = pkg.getSoftwareType();
        if (type == SoftwareType.HELM_CHART) {
            // Helm: 返回推送命令作为 URL（实际推送需 CLI）
            String pushCmd = client.generateHelmPushCommand(pkg.getPackageKey(), version.getVersionNo());
            logger.info("生成 Helm 推送命令:\n{}", pushCmd);
            return client.getArtifactUrl(pkg.getPackageKey(), version.getVersionNo());
        } else {
            // DOCKER_IMAGE: 返回推送命令作为日志，URL 指向 Harbor
            String pushCmd = client.generateDockerPushCommand(pkg.getPackageKey(), version.getVersionNo());
            logger.info("生成 Docker 推送命令:\n{}", pushCmd);
            return client.getArtifactUrl(pkg.getPackageKey(), version.getVersionNo());
        }
    }

    private UploadResult saveToLocal(SoftwarePackage pkg, SoftwareVersion version, MultipartFile file) throws IOException {
        String typeDir = pkg.getSoftwareType().name().toLowerCase();
        String dir = typeDir + "/" + pkg.getPackageName() + "/" + version.getVersionNo();
        Path storageDir = Paths.get(uploadBaseDir, dir);
        Files.createDirectories(storageDir);

        String originalName = file.getOriginalFilename();
        String storedName = generateStoredName(originalName);
        Path targetPath = storageDir.resolve(storedName);
        file.transferTo(targetPath.toFile());

        return new UploadResult(dir + "/" + storedName);
    }

    /**
     * 获取上传文件的物理路径（仅适用于本地存储）
     */
    public Path getArtifactPath(Long versionId) {
        SoftwareVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BizException("版本不存在"));

        if (version.getStoragePath() == null) {
            throw new BizException("该版本尚未上传制品文件");
        }

        Path path = Paths.get(uploadBaseDir, version.getStoragePath());
        if (!Files.exists(path)) {
            throw new BizException("制品文件不存在");
        }
        return path;
    }

    private String generateStoredName(String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16) + extension;
    }

    private String calculateMD5FromBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static class UploadResult {
        final String path;
        UploadResult(String path) { this.path = path; }
    }
}
