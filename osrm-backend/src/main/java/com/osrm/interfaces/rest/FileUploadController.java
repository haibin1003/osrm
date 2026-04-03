package com.osrm.interfaces.rest;

import com.osrm.application.upload.dto.FileUploadDTO;
import com.osrm.application.upload.service.FileUploadAppService;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    @Autowired
    private FileUploadAppService fileUploadAppService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('package:create')")
    public ApiResponse<FileUploadDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String relatedType,
            @RequestParam(required = false) Long relatedId,
            @CurrentUser Long userId) {
        FileUploadDTO dto = fileUploadAppService.upload(file, relatedType, relatedId, userId);
        return ApiResponse.success(dto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<List<FileUploadDTO>> list(
            @RequestParam String relatedType,
            @RequestParam Long relatedId) {
        return ApiResponse.success(fileUploadAppService.findByRelated(relatedType, relatedId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('package:update')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileUploadAppService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        Path filePath = fileUploadAppService.getFilePath(id);
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
