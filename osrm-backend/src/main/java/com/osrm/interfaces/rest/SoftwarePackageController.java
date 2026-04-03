package com.osrm.interfaces.rest;

import com.osrm.application.software.dto.request.CreatePackageRequest;
import com.osrm.application.software.dto.request.CreateVersionRequest;
import com.osrm.application.software.dto.request.UpdatePackageRequest;
import com.osrm.application.software.dto.response.SoftwarePackageDTO;
import com.osrm.application.software.dto.response.SoftwareVersionDTO;
import com.osrm.application.software.service.SoftwarePackageAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/software-packages")
public class SoftwarePackageController {

    @Autowired
    private SoftwarePackageAppService softwarePackageAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<PageResult<SoftwarePackageDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(softwarePackageAppService.findByConditions(keyword, type, status, categoryId, sort, page, size));
    }

    /**
     * 获取当前用户创建的软件包列表
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<PageResult<SoftwarePackageDTO>> myPackages(
            @CurrentUser Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(softwarePackageAppService.findByUserId(userId, keyword, type, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<SoftwarePackageDTO> getById(@PathVariable Long id) {
        return ApiResponse.success(softwarePackageAppService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('package:create')")
    public ApiResponse<SoftwarePackageDTO> create(@Valid @RequestBody CreatePackageRequest request, @CurrentUser Long userId) {
        return ApiResponse.success(softwarePackageAppService.create(request, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('package:update')")
    public ApiResponse<SoftwarePackageDTO> update(@PathVariable Long id, @Valid @RequestBody UpdatePackageRequest request) {
        return ApiResponse.success(softwarePackageAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('package:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        softwarePackageAppService.delete(id);
        return ApiResponse.success();
    }

    // ============ 状态流转 ============

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('package:update')")
    public ApiResponse<SoftwarePackageDTO> submit(@PathVariable Long id, @CurrentUser Long userId) {
        return ApiResponse.success(softwarePackageAppService.submit(id, userId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('package:approve')")
    public ApiResponse<SoftwarePackageDTO> approve(@PathVariable Long id, @CurrentUser Long userId) {
        return ApiResponse.success(softwarePackageAppService.approve(id, userId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('package:approve')")
    public ApiResponse<SoftwarePackageDTO> reject(@PathVariable Long id, @CurrentUser Long userId) {
        return ApiResponse.success(softwarePackageAppService.reject(id, userId));
    }

    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAuthority('package:approve')")
    public ApiResponse<SoftwarePackageDTO> offline(@PathVariable Long id, @RequestParam String reason) {
        return ApiResponse.success(softwarePackageAppService.offline(id, reason));
    }

    @PostMapping("/{id}/republish")
    @PreAuthorize("hasAuthority('package:approve')")
    public ApiResponse<SoftwarePackageDTO> republish(@PathVariable Long id) {
        return ApiResponse.success(softwarePackageAppService.republish(id));
    }

    // ============ 版本管理 ============

    @GetMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<List<SoftwareVersionDTO>> getVersions(@PathVariable Long id) {
        return ApiResponse.success(softwarePackageAppService.getVersions(id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("hasAuthority('package:create')")
    public ApiResponse<SoftwareVersionDTO> createVersion(@PathVariable Long id, @Valid @RequestBody CreateVersionRequest request, @CurrentUser Long userId) {
        return ApiResponse.success(softwarePackageAppService.createVersion(id, request, userId));
    }

    @PostMapping("/{id}/versions/{versionId}/publish")
    @PreAuthorize("hasAuthority('package:update')")
    public ApiResponse<SoftwareVersionDTO> publishVersion(@PathVariable Long id, @PathVariable Long versionId, @CurrentUser Long userId) {
        return ApiResponse.success(softwarePackageAppService.publishVersion(id, versionId, userId));
    }

    @PostMapping("/{id}/versions/{versionId}/offline")
    @PreAuthorize("hasAuthority('package:update')")
    public ApiResponse<SoftwareVersionDTO> offlineVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return ApiResponse.success(softwarePackageAppService.offlineVersion(id, versionId));
    }

    @DeleteMapping("/{id}/versions/{versionId}")
    @PreAuthorize("hasAuthority('package:delete')")
    public ApiResponse<Void> deleteVersion(@PathVariable Long id, @PathVariable Long versionId) {
        softwarePackageAppService.deleteVersion(id, versionId);
        return ApiResponse.success();
    }

    // ============ 其他 ============

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('package:read')")
    public ApiResponse<List<Map<String, String>>> getTypes() {
        return ApiResponse.success(softwarePackageAppService.getSoftwareTypes());
    }

    /**
     * 获取待审批的软件包列表
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('package:approve')")
    public ApiResponse<PageResult<SoftwarePackageDTO>> pendingApproval(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(softwarePackageAppService.findPendingPackages(page, size));
    }
}
