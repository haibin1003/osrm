package com.osrm.interfaces.rest;

import com.osrm.application.catalog.dto.response.ApplicationCatalogDTO;
import com.osrm.application.catalog.dto.response.SystemCatalogDTO;
import com.osrm.application.catalog.service.SystemCatalogAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统目录(内部页)。
 * H5 公开端点见 {@link H5PublicController}。
 */
@RestController
@RequestMapping("/api/v1/system-catalog")
public class SystemCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(SystemCatalogController.class);

    @Autowired
    private SystemCatalogAppService systemCatalogAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('business-system:read')")
    public ApiResponse<PageResult<SystemCatalogDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<SystemCatalogDTO> result = systemCatalogAppService.findByConditions(keyword, status, enabled, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('business-system:read')")
    public ApiResponse<SystemCatalogDTO> getById(@PathVariable Long id) {
        SystemCatalogDTO dto = systemCatalogAppService.findById(id);
        return ApiResponse.success(dto);
    }

    @GetMapping("/{id}/applications")
    @PreAuthorize("hasAuthority('business-system:read')")
    public ApiResponse<List<ApplicationCatalogDTO>> listApplications(@PathVariable Long id) {
        List<ApplicationCatalogDTO> apps = systemCatalogAppService.listEnabledApplicationsBySystem(id);
        return ApiResponse.success(apps);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('business-system:update')")
    public ApiResponse<SystemCatalogDTO> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        logger.info("设置系统目录状态: id={}, enabled={}", id, enabled);
        SystemCatalogDTO dto = systemCatalogAppService.setEnabled(id, enabled);
        return ApiResponse.success(dto);
    }
}
