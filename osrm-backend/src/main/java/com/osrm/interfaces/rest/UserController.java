package com.osrm.interfaces.rest;

import com.osrm.application.user.dto.UserDTO;
import com.osrm.application.user.dto.request.CreateUserRequest;
import com.osrm.application.user.dto.request.ResetPasswordRequest;
import com.osrm.application.user.dto.request.UpdateStatusRequest;
import com.osrm.application.user.dto.request.UpdateUserRequest;
import com.osrm.application.user.service.UserAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserAppService userAppService;
    private final JwtTokenProvider jwtTokenProvider;

    public UserController(UserAppService userAppService,
                          JwtTokenProvider jwtTokenProvider) {
        this.userAppService = userAppService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 查询用户列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<Page<UserDTO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> result = userAppService.listUsers(username, realName, enabled, pageable);
        return ApiResponse.success(result);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userAppService.getUserById(id);
        return ApiResponse.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ApiResponse<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO user = userAppService.createUser(request);
        return ApiResponse.success(user);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ApiResponse<UserDTO> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody UpdateUserRequest request) {
        UserDTO user = userAppService.updateUser(id, request);
        return ApiResponse.success(user);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ApiResponse<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        userAppService.deleteUser(id, currentUserId);
        return ApiResponse.success();
    }

    /**
     * 重置密码
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('user:update')")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        userAppService.resetPassword(id, request.getNewPassword());
        return ApiResponse.success();
    }

    /**
     * 切换用户状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:update')")
    public ApiResponse<UserDTO> toggleStatus(@PathVariable Long id,
                                             @Valid @RequestBody UpdateStatusRequest request) {
        UserDTO user = userAppService.toggleStatus(id, request.getEnabled());
        return ApiResponse.success(user);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return jwtTokenProvider.getUserIdFromToken(token);
        }
        throw new RuntimeException("无法获取当前用户");
    }
}
