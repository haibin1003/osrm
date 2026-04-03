package com.osrm.infrastructure.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT Token 提供者测试类
 * 测试 Token 生成、验证、解析等功能
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "test-secret-key-for-jwt-signing-at-least-256-bits-long-for-testing");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 7200000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void generateAccessToken_shouldCreateValidToken() {
        // Given
        Long userId = 1L;
        String username = "admin";
        String[] roles = {"ROLE_SYSTEM_ADMIN"};
        String[] permissions = {"system:manage"};

        // When
        String token = jwtTokenProvider.generateAccessToken(userId, username, roles, permissions);

        // Then
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateRefreshToken_shouldCreateValidToken() {
        // Given
        Long userId = 1L;
        String username = "admin";

        // When
        String token = jwtTokenProvider.generateRefreshToken(userId, username);

        // Then
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        // Given
        Long userId = 1L;
        String token = jwtTokenProvider.generateAccessToken(userId, "admin",
                new String[]{"ROLE_SYSTEM_ADMIN"}, new String[]{"system:manage"});

        // When
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void getUsernameFromToken_shouldReturnCorrectUsername() {
        // Given
        String username = "admin";
        String token = jwtTokenProvider.generateAccessToken(1L, username,
                new String[]{"ROLE_SYSTEM_ADMIN"}, new String[]{"system:manage"});

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void getRolesFromToken_shouldReturnCorrectRoles() {
        // Given
        String[] roles = {"ROLE_SYSTEM_ADMIN", "ROLE_USER"};
        String token = jwtTokenProvider.generateAccessToken(1L, "admin",
                roles, new String[]{"system:manage"});

        // When
        String[] extractedRoles = jwtTokenProvider.getRolesFromToken(token);

        // Then
        assertArrayEquals(roles, extractedRoles);
    }

    @Test
    void getPermissionsFromToken_shouldReturnCorrectPermissions() {
        // Given
        String[] permissions = {"system:manage", "package:read"};
        String token = jwtTokenProvider.generateAccessToken(1L, "admin",
                new String[]{"ROLE_SYSTEM_ADMIN"}, permissions);

        // When
        String[] extractedPermissions = jwtTokenProvider.getPermissionsFromToken(token);

        // Then
        assertArrayEquals(permissions, extractedPermissions);
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(1L, "admin",
                new String[]{"ROLE_SYSTEM_ADMIN"}, new String[]{"system:manage"});

        // When & Then
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    void validateToken_withTamperedToken_shouldReturnFalse() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(1L, "admin",
                new String[]{"ROLE_SYSTEM_ADMIN"}, new String[]{"system:manage"});
        String tamperedToken = token + "tampered";

        // When & Then
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    void getExpirationDate_shouldReturnFutureDate() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(1L, "admin",
                new String[]{"ROLE_SYSTEM_ADMIN"}, new String[]{"system:manage"});

        // When
        Date expirationDate = jwtTokenProvider.getExpirationDate(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void parseToken_shouldReturnAllClaims() {
        // Given
        Long userId = 1L;
        String username = "admin";
        String[] roles = {"ROLE_SYSTEM_ADMIN"};
        String[] permissions = {"system:manage"};
        String token = jwtTokenProvider.generateAccessToken(userId, username, roles, permissions);

        // When
        Claims claims = jwtTokenProvider.parseToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(userId, claims.get("userId", Long.class));
        assertEquals(username, claims.getSubject());
        assertEquals("access", claims.get("type"));
    }

    @Test
    void getAccessTokenExpiration_shouldReturnInSeconds() {
        // When
        long expiration = jwtTokenProvider.getAccessTokenExpiration();

        // Then
        assertEquals(7200, expiration); // 7200000ms / 1000 = 7200s
    }
}
