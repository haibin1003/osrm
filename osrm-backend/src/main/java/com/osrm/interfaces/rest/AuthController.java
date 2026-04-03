package com.osrm.interfaces.rest;

import com.osrm.application.user.dto.request.LoginRequest;
import com.osrm.application.user.dto.request.RefreshTokenRequest;
import com.osrm.application.user.dto.response.LoginResponse;
import com.osrm.application.user.service.AuthAppService;
import com.osrm.common.model.ApiResponse;
import com.osrm.infrastructure.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthAppService authAppService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthAppService authAppService,
                          JwtTokenProvider jwtTokenProvider) {
        this.authAppService = authAppService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authAppService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authAppService.refreshToken(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        authAppService.logout(token);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LoginResponse.UserInfo> getCurrentUser(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        LoginResponse.UserInfo userInfo = authAppService.getCurrentUser(userId);
        return ApiResponse.success(userInfo);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("无法获取认证令牌");
    }
}
