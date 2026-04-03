package com.osrm.interfaces.rest;

import com.osrm.application.user.dto.RoleDTO;
import com.osrm.application.user.dto.request.ConfigureRolePermissionsRequest;
import com.osrm.application.user.dto.request.CreateRoleRequest;
import com.osrm.application.user.dto.request.UpdateRoleRequest;
import com.osrm.application.user.service.RoleAppService;
import com.osrm.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleAppService roleAppService;

    public RoleController(RoleAppService roleAppService) {
        this.roleAppService = roleAppService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<Page<RoleDTO>> listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RoleDTO> result = roleAppService.listRoles(roleName, roleCode, pageable);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<RoleDTO> getRoleById(@PathVariable Long id) {
        RoleDTO role = roleAppService.getRoleById(id);
        return ApiResponse.success(role);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ApiResponse<RoleDTO> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDTO role = roleAppService.createRole(request);
        return ApiResponse.success(role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ApiResponse<RoleDTO> updateRole(@PathVariable Long id,
                                           @Valid @RequestBody UpdateRoleRequest request) {
        RoleDTO role = roleAppService.updateRole(id, request);
        return ApiResponse.success(role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleAppService.deleteRole(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<Long>> getRolePermissions(@PathVariable Long id) {
        List<Long> permissionIds = roleAppService.getRolePermissionIds(id);
        return ApiResponse.success(permissionIds);
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:update')")
    public ApiResponse<Void> configurePermissions(@PathVariable Long id,
                                                  @Valid @RequestBody ConfigureRolePermissionsRequest request) {
        roleAppService.configurePermissions(id, request.getPermissionIds());
        return ApiResponse.success();
    }
}
