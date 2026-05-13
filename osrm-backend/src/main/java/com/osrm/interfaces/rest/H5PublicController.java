package com.osrm.interfaces.rest;

import com.osrm.application.catalog.dto.response.ApplicationCatalogDTO;
import com.osrm.application.catalog.dto.response.SystemCatalogDTO;
import com.osrm.application.h5.dto.request.H5InventorySubmitRequest;
import com.osrm.application.h5.dto.response.H5SubmitResponse;
import com.osrm.application.h5.service.CaptchaService;
import com.osrm.application.h5.service.H5InventoryAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemApplicationRepository;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * H5 公开端点(无需登录)。
 */
@RestController
@RequestMapping("/api/v1/h5")
public class H5PublicController {

    private static final Logger logger = LoggerFactory.getLogger(H5PublicController.class);

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private H5InventoryAppService h5InventoryAppService;

    @Autowired
    private BusinessSystemRepository businessSystemRepository;

    @Autowired
    private BusinessSystemApplicationRepository businessSystemApplicationRepository;

    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> captcha(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        CaptchaService.CaptchaResult result = captchaService.generate(clientIp);
        return ApiResponse.success(Map.of("key", result.key(), "image", result.image()));
    }

    @GetMapping("/systems")
    public ApiResponse<PageResult<SystemCatalogDTO>> listSystems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "systemName"));
        Page<BusinessSystem> pageResult = businessSystemRepository.findByConditions(keyword, BusinessSystem.SystemStatus.IN_USE, true, pageable);
        List<SystemCatalogDTO> content = pageResult.getContent().stream()
                .map(SystemCatalogDTO::from)
                .collect(Collectors.toList());
        PageResult<SystemCatalogDTO> result = PageResult.of(content, pageResult.getTotalElements(),
                pageResult.getTotalPages(), pageResult.getSize(), pageResult.getNumber() + 1);
        return ApiResponse.success(result);
    }

    @GetMapping("/systems/{id}/applications")
    public ApiResponse<List<ApplicationCatalogDTO>> listApplications(@PathVariable Long id) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new com.osrm.common.exception.BizException("系统不存在"));
        List<ApplicationCatalogDTO> apps = businessSystemApplicationRepository
                .findByBusinessSystemIdAndEnabledTrueOrderByApplicationName(system.getId())
                .stream()
                .map(ApplicationCatalogDTO::from)
                .collect(Collectors.toList());
        return ApiResponse.success(apps);
    }

    @GetMapping("/systems/{id}/suggestion")
    public ApiResponse<Map<String, String>> getSuggestion(@PathVariable Long id) {
        BusinessSystem system = businessSystemRepository.findById(id)
                .orElseThrow(() -> new com.osrm.common.exception.BizException("系统不存在"));
        return ApiResponse.success(Map.of(
                "responsiblePerson", system.getResponsiblePerson() != null ? system.getResponsiblePerson() : "",
                "responsiblePhone", system.getResponsiblePhone() != null ? system.getResponsiblePhone() : "",
                "responsibleDept", system.getResponsibleDept() != null ? system.getResponsibleDept() : ""
        ));
    }

    @PostMapping("/inventory/submit")
    public ApiResponse<H5SubmitResponse> submit(
            @Valid @RequestBody H5InventorySubmitRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        logger.info("H5 提交盘点: systemCatalogId={}, person={}, ip={}",
                request.getSystemCatalogId(), request.getResponsiblePerson(), clientIp);
        H5SubmitResponse response = h5InventoryAppService.submit(request, clientIp);
        return ApiResponse.success(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
