package com.osrm.interfaces.rest;

import com.osrm.application.business.dto.request.CreateBusinessSystemRequest;
import com.osrm.application.business.dto.request.UpdateBusinessSystemRequest;
import com.osrm.application.business.dto.response.BusinessSystemDTO;
import com.osrm.application.business.service.BusinessSystemAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business-systems")
public class BusinessSystemController {

    private static final Logger logger = LoggerFactory.getLogger(BusinessSystemController.class);

    @Autowired
    private BusinessSystemAppService businessSystemAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('business-system:read')")
    public ApiResponse<PageResult<BusinessSystemDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<BusinessSystemDTO> result = businessSystemAppService.findByConditions(keyword, domain, enabled, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('business-system:read')")
    public ApiResponse<BusinessSystemDTO> getById(@PathVariable Long id) {
        BusinessSystemDTO dto = businessSystemAppService.findById(id);
        return ApiResponse.success(dto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('business-system:create')")
    public ApiResponse<BusinessSystemDTO> create(
            @Valid @RequestBody CreateBusinessSystemRequest request,
            @CurrentUser Long userId) {
        logger.info("创建业务系统: code={}, name={}", request.getSystemCode(), request.getSystemName());
        BusinessSystemDTO dto = businessSystemAppService.create(request, userId);
        return ApiResponse.success(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('business-system:update')")
    public ApiResponse<BusinessSystemDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBusinessSystemRequest request) {
        logger.info("更新业务系统: id={}", id);
        BusinessSystemDTO dto = businessSystemAppService.update(id, request);
        return ApiResponse.success(dto);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('business-system:update')")
    public ApiResponse<BusinessSystemDTO> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        logger.info("设置业务系统状态: id={}, enabled={}", id, enabled);
        BusinessSystemDTO dto = businessSystemAppService.setEnabled(id, enabled);
        return ApiResponse.success(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('business-system:approve')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        logger.info("删除业务系统: id={}", id);
        businessSystemAppService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/domains")
    @PreAuthorize("hasAuthority('business-system:read')")
    public ApiResponse<List<String>> getDomains() {
        List<String> domains = businessSystemAppService.getAllDomains().stream()
                .map(d -> d.getCode() + ":" + d.getName())
                .toList();
        return ApiResponse.success(domains);
    }
}
