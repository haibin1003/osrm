package com.osrm.interfaces.rest;

import com.osrm.application.portal.dto.response.*;
import com.osrm.application.portal.service.PortalAppService;
import com.osrm.application.software.dto.response.SoftwareVersionDTO;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.domain.software.entity.SoftwareType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {

    private final PortalAppService portalAppService;

    @Autowired
    public PortalController(PortalAppService portalAppService) {
        this.portalAppService = portalAppService;
    }

    @GetMapping("/software")
    public ApiResponse<PageResult<PortalPackageDTO>> listPublished(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        SoftwareType typeEnum = type != null && !type.isEmpty() ? SoftwareType.valueOf(type) : null;
        return ApiResponse.success(portalAppService.findPublishedPackages(keyword, typeEnum, page, size));
    }

    @GetMapping("/software/{id}")
    public ApiResponse<PortalPackageDTO> getPackageDetail(@PathVariable Long id) {
        return ApiResponse.success(portalAppService.findPackageById(id));
    }

    @GetMapping("/stats")
    public ApiResponse<PortalStatsDTO> getStats() {
        return ApiResponse.success(portalAppService.getStats());
    }

    @GetMapping("/popular")
    public ApiResponse<List<PortalPackageDTO>> getPopular(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(portalAppService.findPopular(limit));
    }

    @GetMapping("/stats/overview")
    public ApiResponse<PortalStatsOverviewDTO> getStatsOverview() {
        return ApiResponse.success(portalAppService.getStatsOverview());
    }

    @GetMapping("/stats/trend")
    public ApiResponse<List<StatsTrendDTO>> getStatsTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(portalAppService.getStatsTrend(days));
    }

    @GetMapping("/software/{id}/dependencies")
    public ApiResponse<DependencyGraphDTO> getDependencies(@PathVariable Long id) {
        return ApiResponse.success(portalAppService.getDependencies(id));
    }

    @GetMapping("/software/{id}/security")
    public ApiResponse<SecurityReportDTO> getSecurityReport(@PathVariable Long id) {
        return ApiResponse.success(portalAppService.getSecurityReport(id));
    }

    @GetMapping("/software/{id}/versions")
    public ApiResponse<List<SoftwareVersionDTO>> getPackageVersions(@PathVariable Long id) {
        return ApiResponse.success(portalAppService.getPackageVersions(id));
    }
}
