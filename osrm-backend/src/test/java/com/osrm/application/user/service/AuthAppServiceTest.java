package com.osrm.application.user.service;

import com.osrm.application.user.dto.request.LoginRequest;
import com.osrm.application.user.dto.response.LoginResponse;
import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.UserRepository;
import com.osrm.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 认证应用服务测试类
 */
@ExtendWith(MockitoExtension.class)
class AuthAppServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthAppService authAppService;

    private User mockUser;
    private Role mockRole;
    private Permission mockPermission;

    @BeforeEach
    void setUp() {
        // Setup mock permission
        mockPermission = new Permission();
        mockPermission.setId(1L);
        mockPermission.setPermissionCode("system:manage");
        mockPermission.setPermissionName("System Management");

        // Setup mock role
        mockRole = new Role();
        mockRole.setId(1L);
        mockRole.setRoleCode("ROLE_SYSTEM_ADMIN");
        mockRole.setRoleName("System Administrator");
        mockRole.setPermissions(new HashSet<>() {{ add(mockPermission); }});

        // Setup mock user
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setPassword("encodedPassword");
        mockUser.setRealName("System Administrator");
        mockUser.setEmail("admin@osrm.local");
        mockUser.setEnabled(true);
        mockUser.setLoginFailCount(0);
        mockUser.setRoles(new HashSet<>() {{ add(mockRole); }});
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("admin123", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any()))
                .thenReturn("mockAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(), any()))
                .thenReturn("mockRefreshToken");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(7200L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        LoginResponse response = authAppService.login(request);

        // Then
        assertNotNull(response);
        assertEquals("mockAccessToken", response.getAccessToken());
        assertEquals("mockRefreshToken", response.getRefreshToken());
        assertEquals(7200L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals("admin", response.getUser().getUsername());

        verify(userRepository).save(mockUser);
        verify(valueOperations).set(anyString(), eq("mockRefreshToken"), any());
    }

    @Test
    void login_withInvalidUsername_shouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("invalid");
        request.setPassword("admin123");

        when(userRepository.findByUsername("invalid")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authAppService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void login_withInvalidPassword_shouldThrowException() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrongpassword");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authAppService.login(request));
        assertEquals("用户名或密码错误", exception.getMessage());
        verify(userRepository).save(mockUser);
    }

    @Test
    void login_withDisabledUser_shouldThrowException() {
        // Given
        mockUser.setEnabled(false);
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authAppService.login(request));
        assertEquals("账户已被禁用", exception.getMessage());
    }

    @Test
    void login_withLockedUser_shouldThrowException() {
        // Given
        mockUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authAppService.login(request));
        assertEquals("账户已被锁定，请15分钟后再试", exception.getMessage());
    }

    @Test
    void getCurrentUser_withValidUserId_shouldReturnUserInfo() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // When
        LoginResponse.UserInfo userInfo = authAppService.getCurrentUser(1L);

        // Then
        assertNotNull(userInfo);
        assertEquals(1L, userInfo.getId());
        assertEquals("admin", userInfo.getUsername());
        assertEquals("System Administrator", userInfo.getRealName());
        assertNotNull(userInfo.getRoles());
        assertNotNull(userInfo.getPermissions());
    }

    @Test
    void getCurrentUser_withInvalidUserId_shouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authAppService.getCurrentUser(999L));
        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void extractRoles_shouldReturnRoleCodes() {
        // Given - user has ROLE_SYSTEM_ADMIN

        // When
        java.lang.reflect.Method method;
        String[] roles;
        try {
            method = AuthAppService.class.getDeclaredMethod("extractRoles", User.class);
            method.setAccessible(true);
            roles = (String[]) method.invoke(authAppService, mockUser);
        } catch (Exception e) {
            fail("Failed to invoke extractRoles method: " + e.getMessage());
            return;
        }

        // Then
        assertNotNull(roles);
        assertEquals(1, roles.length);
        assertEquals("ROLE_SYSTEM_ADMIN", roles[0]);
    }

    @Test
    void extractPermissions_shouldReturnPermissionCodes() {
        // Given - role has system:manage permission

        // When
        java.lang.reflect.Method method;
        String[] permissions;
        try {
            method = AuthAppService.class.getDeclaredMethod("extractPermissions", User.class);
            method.setAccessible(true);
            permissions = (String[]) method.invoke(authAppService, mockUser);
        } catch (Exception e) {
            fail("Failed to invoke extractPermissions method: " + e.getMessage());
            return;
        }

        // Then
        assertNotNull(permissions);
        assertEquals(1, permissions.length);
        assertEquals("system:manage", permissions[0]);
    }
}
