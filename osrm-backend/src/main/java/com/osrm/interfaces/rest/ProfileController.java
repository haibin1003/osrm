package com.osrm.interfaces.rest;

import com.osrm.application.user.dto.ProfileDTO;
import com.osrm.application.user.dto.request.ChangePasswordRequest;
import com.osrm.application.user.dto.request.UpdateProfileRequest;
import com.osrm.application.user.service.ProfileAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 个人中心控制器
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileAppService profileAppService;
    private final JwtTokenProvider jwtTokenProvider;

    public ProfileController(ProfileAppService profileAppService,
                             JwtTokenProvider jwtTokenProvider) {
        this.profileAppService = profileAppService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileDTO> getProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        ProfileDTO profile = profileAppService.getProfile(userId);
        return ApiResponse.success(profile);
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ProfileDTO> updateProfile(HttpServletRequest request,
                                               @Valid @RequestBody UpdateProfileRequest updateRequest) {
        Long userId = getCurrentUserId(request);
        ProfileDTO profile = profileAppService.updateProfile(userId, updateRequest);
        return ApiResponse.success(profile);
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(HttpServletRequest request,
                                          @Valid @RequestBody ChangePasswordRequest passwordRequest) {
        Long userId = getCurrentUserId(request);
        profileAppService.changePassword(userId, passwordRequest);
        return ApiResponse.success();
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
