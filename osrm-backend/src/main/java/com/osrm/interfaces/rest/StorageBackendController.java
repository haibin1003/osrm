package com.osrm.interfaces.rest;

import com.osrm.application.storage.dto.*;
import com.osrm.application.storage.dto.request.CreateStorageBackendRequest;
import com.osrm.application.storage.dto.request.TestConnectionRequest;
import com.osrm.application.storage.dto.request.UpdateStorageBackendRequest;
import com.osrm.application.storage.service.StorageBackendAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存储后端管理控制器
 */
@RestController
@RequestMapping("/api/v1/storage-backends")
public class StorageBackendController {

    private static final Logger logger = LoggerFactory.getLogger(StorageBackendController.class);

    private final StorageBackendAppService storageBackendAppService;

    public StorageBackendController(StorageBackendAppService storageBackendAppService) {
        this.storageBackendAppService = storageBackendAppService;
    }

    /**
     * 列表查询
     */
    @GetMapping
    @PreAuthorize("hasAuthority('storage:read') or hasAuthority('system:manage')")
    public ApiResponse<PageResult<StorageBackendDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.debug("查询存储后端列表: keyword={}, type={}, status={}, page={}, size={}",
            keyword, type, status, page, size);

        PageResult<StorageBackendDTO> result = storageBackendAppService.findByConditions(
            keyword, type, status, page, size);

        return ApiResponse.success(result);
    }

    /**
     * 详情查询
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('storage:read') or hasAuthority('system:manage')")
    public ApiResponse<StorageBackendDTO> getById(@PathVariable Long id) {
        logger.debug("查询存储后端详情: id={}", id);
        StorageBackendDTO dto = storageBackendAppService.findById(id);
        return ApiResponse.success(dto);
    }

    /**
     * 新增存储后端
     */
    @PostMapping
    @PreAuthorize("hasAuthority('storage:create') or hasAuthority('system:manage')")
    public ApiResponse<StorageBackendDTO> create(
            @Valid @RequestBody CreateStorageBackendRequest request,
            @CurrentUser Long userId) {

        logger.info("创建存储后端: name={}, type={}",
            request.getBackendName(), request.getBackendType());

        StorageBackendDTO dto = storageBackendAppService.create(request, userId);
        return ApiResponse.success(dto);
    }

    /**
     * 编辑存储后端
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('storage:update') or hasAuthority('system:manage')")
    public ApiResponse<StorageBackendDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStorageBackendRequest request) {

        logger.info("更新存储后端: id={}", id);

        StorageBackendDTO dto = storageBackendAppService.update(id, request);
        return ApiResponse.success(dto);
    }

    /**
     * 删除存储后端
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('storage:delete') or hasAuthority('system:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        logger.info("删除存储后端: id={}", id);
        storageBackendAppService.delete(id);
        return ApiResponse.success(null);
    }

    /**
     * 连接测试
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasAuthority('storage:create') or hasAuthority('storage:update') or hasAuthority('system:manage')")
    public ApiResponse<ConnectionTestResultDTO> testConnection(
            @Valid @RequestBody TestConnectionRequest request) {

        logger.debug("测试存储后端连接: type={}, endpoint={}",
            request.getBackendType(), request.getEndpoint());

        ConnectionTestResultDTO result = storageBackendAppService.testConnection(request);
        return ApiResponse.success(result);
    }

    /**
     * 健康检查
     */
    @PostMapping("/{id}/health")
    @PreAuthorize("hasAuthority('storage:read') or hasAuthority('system:manage')")
    public ApiResponse<HealthCheckResultDTO> healthCheck(@PathVariable Long id) {
        logger.debug("执行健康检查: id={}", id);
        HealthCheckResultDTO result = storageBackendAppService.healthCheck(id);
        return ApiResponse.success(result);
    }

    /**
     * 获取存储后端健康状态列表
     */
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('storage:read') or hasAuthority('system:manage')")
    public ApiResponse<List<HealthCheckResultDTO>> getAllHealthStatus() {
        List<HealthCheckResultDTO> result = storageBackendAppService.getAllHealthStatus();
        return ApiResponse.success(result);
    }

    /**
     * 获取存储类型列表
     */
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('storage:read') or hasAuthority('system:manage')")
    public ApiResponse<List<StorageTypeDTO>> getStorageTypes() {
        List<StorageTypeDTO> types = storageBackendAppService.getStorageTypes();
        return ApiResponse.success(types);
    }

    /**
     * 设为默认存储后端
     */
    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('storage:update') or hasAuthority('system:manage')")
    public ApiResponse<StorageBackendDTO> setDefault(@PathVariable Long id) {
        logger.info("设为默认存储后端: id={}", id);
        StorageBackendDTO dto = storageBackendAppService.setDefault(id);
        return ApiResponse.success(dto);
    }

    /**
     * 启用/停用存储后端
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('storage:update') or hasAuthority('system:manage')")
    public ApiResponse<StorageBackendDTO> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        logger.info("设置存储后端状态: id={}, enabled={}", id, enabled);
        StorageBackendDTO dto = storageBackendAppService.setEnabled(id, enabled);
        return ApiResponse.success(dto);
    }
}
