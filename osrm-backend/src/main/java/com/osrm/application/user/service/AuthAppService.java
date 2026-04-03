package com.osrm.application.user.service;

import com.osrm.application.user.dto.request.LoginRequest;
import com.osrm.application.user.dto.request.RefreshTokenRequest;
import com.osrm.application.user.dto.response.LoginResponse;
import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.UserRepository;
import com.osrm.infrastructure.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthAppService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    public AuthAppService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // Check if user is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("账户已被禁用");
        }

        // Check if user is locked
        if (user.isLocked()) {
            throw new RuntimeException("账户已被锁定，请15分钟后再试");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.recordLoginFailure();
            userRepository.save(user);
            throw new RuntimeException("用户名或密码错误");
        }

        // Record login success
        user.recordLoginSuccess();
        userRepository.save(user);

        // Generate tokens
        String[] roles = extractRoles(user);
        String[] permissions = extractPermissions(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername());

        // Store refresh token in Redis for rotation
        storeRefreshToken(user.getId(), refreshToken);

        // Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiration());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setRoles(List.of(roles));
        userInfo.setPermissions(List.of(permissions));
        response.setUser(userInfo);

        return response;
    }

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        Claims claims = jwtTokenProvider.parseToken(request.getRefreshToken());
        String tokenType = claims.get("type", String.class);

        if (!"refresh".equals(tokenType)) {
            throw new RuntimeException("无效的令牌类型");
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();

        // Check if token is in Redis (rotation check) - skip if Redis unavailable
        try {
            String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
            if (storedToken == null || !storedToken.equals(request.getRefreshToken())) {
                // Token reuse detected - revoke all tokens for this user
                revokeAllUserTokens(userId);
                throw new RuntimeException("令牌已被使用，请重新登录");
            }
        } catch (Exception e) {
            // If Redis is unavailable, skip rotation check for development
            System.out.println("Warning: Redis unavailable, skipping token rotation check");
        }

        // Get user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!user.getEnabled()) {
            throw new RuntimeException("账户已被禁用");
        }

        // Generate new token pair (rotation)
        String[] roles = extractRoles(user);
        String[] permissions = extractPermissions(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), roles, permissions);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername());

        // Replace old refresh token with new one
        storeRefreshToken(userId, newRefreshToken);

        // Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpiration());

        return response;
    }

    public void logout(String accessToken) {
        try {
            // Add access token to blacklist
            long expiration = jwtTokenProvider.getExpirationDate(accessToken).getTime();
            long now = System.currentTimeMillis();
            long ttl = expiration - now;

            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        TOKEN_BLACKLIST_PREFIX + accessToken,
                        "logout",
                        Duration.ofMillis(ttl)
                );
            }

            // Remove refresh token from Redis
            Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        } catch (Exception e) {
            // Log but don't fail logout if Redis is unavailable
            System.out.println("Warning: Failed to clear tokens from Redis: " + e.getMessage());
        }
    }

    public LoginResponse.UserInfo getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setRoles(List.of(extractRoles(user)));
        userInfo.setPermissions(List.of(extractPermissions(user)));

        return userInfo;
    }

    private String[] extractRoles(User user) {
        return user.getRoles().stream()
                .map(Role::getRoleCode)
                .toArray(String[]::new);
    }

    private String[] extractPermissions(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getPermissionCode)
                .distinct()
                .toArray(String[]::new);
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        try {
            long ttl = 7 * 24 * 60 * 60; // 7 days in seconds
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + userId,
                    refreshToken,
                    Duration.ofSeconds(ttl)
            );
        } catch (Exception e) {
            // Log but don't fail login if Redis is unavailable
            System.out.println("Warning: Failed to store refresh token in Redis: " + e.getMessage());
        }
    }

    private void revokeAllUserTokens(Long userId) {
        try {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
        } catch (Exception e) {
            System.out.println("Warning: Failed to revoke tokens from Redis: " + e.getMessage());
        }
    }
}
