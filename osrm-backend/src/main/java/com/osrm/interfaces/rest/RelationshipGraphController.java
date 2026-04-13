package com.osrm.interfaces.rest;

import com.osrm.application.tracking.dto.RelationshipGraphDTO;
import com.osrm.application.tracking.dto.response.PackageImpactDTO;
import com.osrm.application.tracking.dto.response.SystemDependenciesDTO;
import com.osrm.application.tracking.service.RelationshipGraphAppService;
import com.osrm.common.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 关联图谱控制器
 */
@RestController
@RequestMapping("/api/v1/tracking")
public class RelationshipGraphController {

    private final RelationshipGraphAppService graphAppService;

    @Autowired
    public RelationshipGraphController(RelationshipGraphAppService graphAppService) {
        this.graphAppService = graphAppService;
    }

    /**
     * 获取完整的系统-软件关联图
     */
    @GetMapping("/relationship-graph")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<RelationshipGraphDTO> getRelationshipGraph(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String softwareType,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(graphAppService.buildRelationshipGraph(domain, softwareType, status));
    }

    /**
     * 获取系统依赖详情
     */
    @GetMapping("/system/{systemId}/dependencies")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<SystemDependenciesDTO> getSystemDependencies(@PathVariable Long systemId) {
        return ApiResponse.success(graphAppService.getSystemDependencies(systemId));
    }

    /**
     * 获取软件影响分析
     */
    @GetMapping("/package/{packageId}/impact")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<PackageImpactDTO> getPackageImpact(@PathVariable Long packageId) {
        return ApiResponse.success(graphAppService.getPackageImpact(packageId));
    }
}
