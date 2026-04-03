package com.osrm.interfaces.rest;

import com.osrm.application.system.dto.SettingDTO;
import com.osrm.application.system.service.SettingAppService;
import com.osrm.common.model.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统设置控制器
 */
@RestController
@RequestMapping("/api/v1/settings")
public class SettingController {

    private final SettingAppService settingAppService;

    public SettingController(SettingAppService settingAppService) {
        this.settingAppService = settingAppService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<Map<String, Map<String, String>>> getAllSettings() {
        Map<String, Map<String, String>> settings = settingAppService.getAllSettings();
        return ApiResponse.success(settings);
    }

    @GetMapping("/{category}")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<Map<String, String>> getSettingsByCategory(@PathVariable String category) {
        Map<String, String> settings = settingAppService.getSettingsByCategory(category);
        return ApiResponse.success(settings);
    }

    @PutMapping("/{category}")
    @PreAuthorize("hasAuthority('system:manage')")
    public ApiResponse<Void> updateSettings(@PathVariable String category,
                                           @RequestBody Map<String, String> settings) {
        settingAppService.updateSettings(category, settings);
        return ApiResponse.success();
    }
}
