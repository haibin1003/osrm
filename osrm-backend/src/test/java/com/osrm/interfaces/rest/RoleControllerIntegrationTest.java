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
 * 角色管理控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleControllerIntegrationTest {

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
        Permission roleRead = createPermission("role:read", "读取角色");
        Permission roleCreate = createPermission("role:create", "创建角色");
        Permission roleUpdate = createPermission("role:update", "更新角色");
        Permission roleDelete = createPermission("role:delete", "删除角色");
        Permission permRead = createPermission("permission:read", "读取权限");

        // Create admin role
        Role adminRole = new Role();
        adminRole.setRoleCode("ROLE_ADMIN");
        adminRole.setRoleName("Admin");
        adminRole.setPermissions(new HashSet<>(Set.of(roleRead, roleCreate, roleUpdate, roleDelete, permRead)));
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

        // Login
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
    void listRoles_shouldReturnRoleList() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createRole_withValidData_shouldSucceed() throws Exception {
        String requestBody = "{"
                + "\"roleCode\": \"ROLE_TEST\","
                + "\"roleName\": \"测试角色\","
                + "\"description\": \"用于测试的角色\""
                + "}";

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleCode").value("ROLE_TEST"))
                .andExpect(jsonPath("$.data.roleName").value("测试角色"));
    }

    @Test
    void createRole_withInvalidCode_shouldFail() throws Exception {
        String requestBody = "{"
                + "\"roleCode\": \"INVALID_CODE\","  // 不符合ROLE_XXX格式
                + "\"roleName\": \"测试角色\""
                + "}";

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRole_withDuplicateCode_shouldFail() throws Exception {
        String requestBody = "{"
                + "\"roleCode\": \"ROLE_DUPLICATE\","
                + "\"roleName\": \"测试角色\""
                + "}";

        // First creation
        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Duplicate creation
        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateRole_shouldSucceed() throws Exception {
        // Create role
        String createBody = "{"
                + "\"roleCode\": \"ROLE_UPDATE\","
                + "\"roleName\": \"原名称\","
                + "\"description\": \"原描述\""
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        Long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Update role
        String updateBody = "{"
                + "\"roleName\": \"新名称\","
                + "\"description\": \"新描述\""
                + "}";

        mockMvc.perform(put("/api/v1/roles/" + roleId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleName").value("新名称"));
    }

    @Test
    void deleteRole_shouldSucceed() throws Exception {
        // Create role
        String createBody = "{"
                + "\"roleCode\": \"ROLE_DELETE\","
                + "\"roleName\": \"待删除角色\""
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        Long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Delete role
        mockMvc.perform(delete("/api/v1/roles/" + roleId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void configureRolePermissions_shouldSucceed() throws Exception {
        // Create role
        String createBody = "{"
                + "\"roleCode\": \"ROLE_PERM\","
                + "\"roleName\": \"权限测试角色\""
                + "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andReturn();

        Long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Configure permissions
        String permBody = "{\"permissionIds\": [1, 2, 3]}";

        mockMvc.perform(put("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(permBody))
                .andExpect(status().isOk());

        // Get role permissions
        mockMvc.perform(get("/api/v1/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
