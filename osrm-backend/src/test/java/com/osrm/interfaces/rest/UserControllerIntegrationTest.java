package com.osrm.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.user.dto.request.LoginRequest;
import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.PermissionRepository;
import com.osrm.domain.user.repository.RoleRepository;
import com.osrm.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户管理控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create permissions
        Permission userRead = createPermission("user:read", "读取用户");
        Permission userCreate = createPermission("user:create", "创建用户");
        Permission userUpdate = createPermission("user:update", "更新用户");
        Permission userDelete = createPermission("user:delete", "删除用户");
        Permission roleRead = createPermission("role:read", "读取角色");

        // Create admin role
        Role adminRole = new Role();
        adminRole.setRoleCode("ROLE_ADMIN");
        adminRole.setRoleName("Admin");
        adminRole.setPermissions(new HashSet<>(Set.of(userRead, userCreate, userUpdate, userDelete, roleRead)));
        adminRole = roleRepository.save(adminRole);

        // Create admin user
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setRealName("Admin User");
        adminUser.setEmail("admin@osrm.local");
        adminUser.setEnabled(true);
        adminUser.setLoginFailCount(0);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(adminUser);

        // Login to get token
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(responseContent)
                .path("data").path("accessToken").asText();
    }

    private Permission createPermission(String code, String name) {
        Permission p = new Permission();
        p.setPermissionCode(code);
        p.setPermissionName(name);
        p.setResourceType("API");
        return permissionRepository.save(p);
    }

    @Test
    void listUsers_withValidToken_shouldReturnUserList() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    void listUsers_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_withValidData_shouldReturnCreatedUser() throws Exception {
        String requestBody = "{"
                + "\"username\": \"testuser\","
                + "\"realName\": \"测试用户\","
                + "\"email\": \"test@example.com\","
                + "\"phone\": \"13800138000\","
                + "\"password\": \"password123\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.realName").value("测试用户"));
    }

    @Test
    void createUser_withInvalidData_shouldReturnBadRequest() throws Exception {
        String requestBody = "{"
                + "\"username\": \"ab\","  // too short
                + "\"realName\": \"\","
                + "\"password\": \"123\","  // too short
                + "\"roleIds\": []"
                + "}";

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_withDuplicateUsername_shouldReturnError() throws Exception {
        // First create a user
        String requestBody = "{"
                + "\"username\": \"duplicateuser\","
                + "\"realName\": \"测试用户\","
                + "\"password\": \"password123\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Try to create again with same username
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateUser_withValidData_shouldReturnUpdatedUser() throws Exception {
        // First create a user
        String createBody = "{"
                + "\"username\": \"updateuser\","
                + "\"realName\": \"原名称\","
                + "\"password\": \"password123\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseContent).path("data").path("id").asLong();

        // Update the user
        String updateBody = "{"
                + "\"realName\": \"新名称\","
                + "\"email\": \"new@example.com\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        mockMvc.perform(put("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realName").value("新名称"))
                .andExpect(jsonPath("$.data.email").value("new@example.com"));
    }

    @Test
    void updateUserStatus_shouldToggleEnabled() throws Exception {
        // Create a user
        String createBody = "{"
                + "\"username\": \"statususer\","
                + "\"realName\": \"状态测试\","
                + "\"password\": \"password123\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseContent).path("data").path("id").asLong();

        // Disable user
        mockMvc.perform(put("/api/v1/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void resetPassword_shouldUpdatePassword() throws Exception {
        // Create a user
        String createBody = "{"
                + "\"username\": \"pwduser\","
                + "\"realName\": \"密码测试\","
                + "\"password\": \"password123\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseContent).path("data").path("id").asLong();

        // Reset password
        mockMvc.perform(put("/api/v1/users/" + userId + "/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"newpassword456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void deleteUser_shouldRemoveUser() throws Exception {
        // Create a user
        String createBody = "{"
                + "\"username\": \"deleteuser\","
                + "\"realName\": \"删除测试\","
                + "\"password\": \"password123\","
                + "\"roleIds\": [1],"
                + "\"enabled\": true"
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Long userId = objectMapper.readTree(responseContent).path("data").path("id").asLong();

        // Delete user
        mockMvc.perform(delete("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));

        // Verify user is deleted
        mockMvc.perform(get("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteCurrentUser_shouldReturnError() throws Exception {
        // Try to delete admin (current user)
        mockMvc.perform(delete("/api/v1/users/1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError());
    }
}
