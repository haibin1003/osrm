package com.osrm.interfaces.rest;

import com.osrm.application.inventory.dto.request.CreateInventoryRequest;
import com.osrm.application.inventory.dto.request.RejectInventoryRequest;
import com.osrm.application.inventory.dto.response.InventoryDTO;
import com.osrm.application.inventory.service.InventoryAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.common.model.PageResult;
import com.osrm.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @Autowired
    private InventoryAppService inventoryAppService;

    /**
     * 获取存量功能设置（公开接口，用于前端判断是否显示菜单）
     */
    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enableInventoryFeature", inventoryAppService.isInventoryFeatureEnabled());
        return ApiResponse.success(settings);
    }

    /**
     * 获取我的存量登记列表（数据权限：只能查看自己的）
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<PageResult<InventoryDTO>> getMyInventory(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser Long userId) {
        PageResult<InventoryDTO> result = inventoryAppService.getMyInventory(userId, status, page, size);
        return ApiResponse.success(result);
    }

    /**
     * 获取待审批列表（管理员使用）
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('inventory:approve')")
    public ApiResponse<PageResult<InventoryDTO>> getPendingInventory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<InventoryDTO> result = inventoryAppService.getPendingInventory(page, size);
        return ApiResponse.success(result);
    }

    /**
     * 获取所有存量列表（管理员使用）
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('inventory:manage')")
    public ApiResponse<PageResult<InventoryDTO>> getAllInventory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) Long businessSystemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<InventoryDTO> result = inventoryAppService.getAllInventory(
                userId, status, packageName, businessSystemId, page, size);
        return ApiResponse.success(result);
    }

    /**
     * 获取存量登记详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    public ApiResponse<InventoryDTO> getById(
            @PathVariable Long id,
            @CurrentUser Long userId) {
        boolean isAdmin = isCurrentUserAdmin();
        InventoryDTO dto = inventoryAppService.getInventoryById(id, userId, isAdmin);
        return ApiResponse.success(dto);
    }

    /**
     * 创建存量登记
     */
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:create')")
    public ApiResponse<InventoryDTO> create(
            @Valid @RequestBody CreateInventoryRequest request,
            @CurrentUser Long userId) {
        logger.info("创建存量登记: userId={}, packageName={}", userId, request.getPackageName());
        String username = getCurrentUsername();
        InventoryDTO dto = inventoryAppService.createInventory(request, userId, username);
        return ApiResponse.success(dto);
    }

    /**
     * 更新存量登记
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:update')")
    public ApiResponse<InventoryDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateInventoryRequest request,
            @CurrentUser Long userId) {
        logger.info("更新存量登记: id={}, userId={}", id, userId);
        boolean isAdmin = isCurrentUserAdmin();
        InventoryDTO dto = inventoryAppService.updateInventory(id, request, userId, isAdmin);
        return ApiResponse.success(dto);
    }

    /**
     * 批准存量登记
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:approve')")
    public ApiResponse<InventoryDTO> approve(
            @PathVariable Long id,
            @CurrentUser Long userId) {
        logger.info("批准存量登记: id={}, approverId={}", id, userId);
        InventoryDTO dto = inventoryAppService.approveInventory(id, userId);
        return ApiResponse.success(dto);
    }

    /**
     * 驳回存量登记
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('inventory:approve')")
    public ApiResponse<InventoryDTO> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectInventoryRequest request,
            @CurrentUser Long userId) {
        logger.info("驳回存量登记: id={}, approverId={}, reason={}", id, userId, request.getReason());
        InventoryDTO dto = inventoryAppService.rejectInventory(id, request, userId);
        return ApiResponse.success(dto);
    }

    /**
     * 删除存量登记
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:manage')")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @CurrentUser Long userId) {
        logger.info("删除存量登记: id={}, userId={}", id, userId);
        boolean isAdmin = isCurrentUserAdmin();
        inventoryAppService.deleteInventory(id, userId, isAdmin);
        return ApiResponse.success();
    }

    /**
     * 检查当前用户是否是管理员
     */
    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("inventory:manage"));
    }

    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "unknown";
        }
        return authentication.getName();
    }
}
