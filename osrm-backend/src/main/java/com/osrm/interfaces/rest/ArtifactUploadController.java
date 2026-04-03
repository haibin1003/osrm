package com.osrm.interfaces.rest;

import com.osrm.application.artifact.dto.ArtifactUploadResult;
import com.osrm.application.artifact.service.ArtifactUploadService;
import com.osrm.common.model.ApiResponse;
import com.osrm.infrastructure.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/artifacts")
public class ArtifactUploadController {

    @Autowired
    private ArtifactUploadService artifactUploadService;

    /**
     * 上传制品文件到指定软件版本
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('package:create')")
    public ApiResponse<ArtifactUploadResult> upload(
            @RequestParam Long packageId,
            @RequestParam Long versionId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser Long userId) {
        ArtifactUploadResult result = artifactUploadService.uploadArtifact(packageId, versionId, file);
        if (!result.isSuccess()) {
            return ApiResponse.error(500, result.getMessage());
        }
        return ApiResponse.success(result);
    }

    /**
     * 下载制品文件
     */
    @GetMapping("/download/{versionId}")
    public ResponseEntity<Resource> download(@PathVariable Long versionId) throws Exception {
        Path filePath = artifactUploadService.getArtifactPath(versionId);
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String filename = URLEncoder.encode(filePath.getFileName().toString(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
