package com.osrm.interfaces.rest;

import com.osrm.application.user.dto.PermissionDTO;
import com.osrm.application.user.dto.request.CreatePermissionRequest;
import com.osrm.application.user.service.PermissionAppService;
import com.osrm.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionAppService permissionAppService;

    public PermissionController(PermissionAppService permissionAppService) {
        this.permissionAppService = permissionAppService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('permission:read')")
    public ApiResponse<List<PermissionDTO>> getPermissionTree() {
        List<PermissionDTO> tree = permissionAppService.getPermissionTree();
        return ApiResponse.success(tree);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public ApiResponse<List<PermissionDTO>> listPermissions(
            @RequestParam(required = false) String permissionName,
            @RequestParam(required = false) String permissionCode,
            @RequestParam(required = false) String resourceType) {
        List<PermissionDTO> list = permissionAppService.listPermissions(permissionName, permissionCode, resourceType);
        return ApiResponse.success(list);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('permission:create')")
    public ApiResponse<PermissionDTO> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        PermissionDTO permission = permissionAppService.createPermission(request);
        return ApiResponse.success(permission);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ApiResponse<Void> deletePermission(@PathVariable Long id) {
        permissionAppService.deletePermission(id);
        return ApiResponse.success();
    }
}
