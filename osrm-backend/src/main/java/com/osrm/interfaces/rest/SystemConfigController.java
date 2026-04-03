package com.osrm.interfaces.rest;

import com.osrm.application.config.service.SystemConfigAppService;
import com.osrm.common.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/config")
public class SystemConfigController {

    @Autowired
    private SystemConfigAppService systemConfigAppService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<Map<String, String>> getAll() {
        return ApiResponse.success(systemConfigAppService.getAllConfigs());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<Void> update(@RequestBody Map<String, String> configs) {
        systemConfigAppService.updateConfigs(configs);
        return ApiResponse.success();
    }
}
