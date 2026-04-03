package com.osrm.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.storage.dto.request.CreateStorageBackendRequest;
import com.osrm.application.storage.dto.request.UpdateStorageBackendRequest;
import com.osrm.domain.storage.entity.StorageBackend;
import com.osrm.domain.storage.entity.StorageBackendType;
import com.osrm.domain.storage.repository.StorageBackendRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 存储后端控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StorageBackendControllerIntegrationTest {

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

    @Autowired
    private StorageBackendRepository storageBackendRepository;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Setup permissions
        Permission storageRead = createPermissionIfNotExists("storage:read", "读取存储");
        Permission storageCreate = createPermissionIfNotExists("storage:create", "创建存储");
        Permission storageUpdate = createPermissionIfNotExists("storage:update", "更新存储");
        Permission storageDelete = createPermissionIfNotExists("storage:delete", "删除存储");
        Permission systemManage = createPermissionIfNotExists("system:manage", "系统管理");

        // Setup role
        Role testRole = new Role();
        testRole.setRoleCode("ROLE_TEST_ADMIN");
        testRole.setRoleName("Test Admin");
        Set<Permission> permissions = new HashSet<>();
        permissions.add(storageRead);
        permissions.add(storageCreate);
        permissions.add(storageUpdate);
        permissions.add(storageDelete);
        permissions.add(systemManage);
        testRole.setPermissions(permissions);
        testRole = roleRepository.save(testRole);

        // Setup user
        testUser = new User();
        testUser.setUsername("testadmin");
        testUser.setPassword(passwordEncoder.encode("test123"));
        testUser.setRealName("Test Admin");
        testUser.setEmail("test@osrm.local");
        testUser.setEnabled(true);
        testUser.setLoginFailCount(0);
        testUser.setRoles(new HashSet<>(Set.of(testRole)));
        testUser = userRepository.save(testUser);

        // Login to get token
        String loginJson = "{\"username\":\"testadmin\",\"password\":\"test123\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(response).get("data").get("accessToken").asText();
    }

    // ============ GET /api/v1/storage-backends ============

    @Test
    void list_shouldReturnEmptyListInitially() throws Exception {
        mockMvc.perform(get("/api/v1/storage-backends")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ============ POST /api/v1/storage-backends ============

    @Test
    void create_shouldCreateBackend() throws Exception {
        CreateStorageBackendRequest request = new CreateStorageBackendRequest();
        request.setBackendName("测试Harbor");
        request.setBackendType(StorageBackendType.HARBOR);
        request.setEndpoint("https://harbor.test.com");
        request.setAccessKey("admin");
        request.setSecretKey("password");
        request.setNamespace("library");
        request.setDescription("测试用Harbor");

        mockMvc.perform(post("/api/v1/storage-backends")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.backendCode").value(startsWith("harbor-")))
            .andExpect(jsonPath("$.data.backendName").value("测试Harbor"))
            .andExpect(jsonPath("$.data.backendType").value("HARBOR"));
    }

    @Test
    void create_withDuplicateName_shouldReturnError() throws Exception {
        // Create first
        StorageBackend backend = new StorageBackend();
        backend.setBackendCode("harbor-existing");
        backend.setBackendName("已有Harbor");
        backend.setBackendType(StorageBackendType.HARBOR);
        backend.setEndpoint("https://harbor.example.com");
        backend.setEnabled(true);
        storageBackendRepository.save(backend);

        // Try to create with same name
        CreateStorageBackendRequest request = new CreateStorageBackendRequest();
        request.setBackendName("已有Harbor");
        request.setBackendType(StorageBackendType.HARBOR);
        request.setEndpoint("https://harbor-new.example.com");

        mockMvc.perform(post("/api/v1/storage-backends")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("后端名称已存在"));
    }

    @Test
    void create_withoutRequiredFields_shouldReturnValidationError() throws Exception {
        CreateStorageBackendRequest request = new CreateStorageBackendRequest();

        mockMvc.perform(post("/api/v1/storage-backends")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ============ GET /api/v1/storage-backends/{id} ============

    @Test
    void getById_shouldReturnBackend() throws Exception {
        StorageBackend backend = createTestBackend();

        mockMvc.perform(get("/api/v1/storage-backends/{id}", backend.getId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.backendCode").value("harbor-get"))
            .andExpect(jsonPath("$.data.secretKey").doesNotExist());
    }

    @Test
    void getById_withNonExistingId_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/v1/storage-backends/999")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("存储后端不存在"));
    }

    // ============ PUT /api/v1/storage-backends/{id} ============

    @Test
    void update_shouldUpdateBackend() throws Exception {
        StorageBackend backend = createTestBackend();

        UpdateStorageBackendRequest request = new UpdateStorageBackendRequest();
        request.setBackendName("更新后的Harbor");
        request.setEndpoint("https://harbor-updated.example.com");
        request.setDescription("更新后的描述");

        mockMvc.perform(put("/api/v1/storage-backends/{id}", backend.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.backendName").value("更新后的Harbor"));
    }

    // ============ DELETE /api/v1/storage-backends/{id} ============

    @Test
    void delete_shouldDeleteBackend() throws Exception {
        StorageBackend backend = createTestBackend();
        backend.setEnabled(false);
        storageBackendRepository.save(backend);

        mockMvc.perform(delete("/api/v1/storage-backends/{id}", backend.getId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_withDefaultBackend_shouldReturnError() throws Exception {
        StorageBackend backend = createTestBackend();
        backend.setIsDefault(true);
        storageBackendRepository.save(backend);

        mockMvc.perform(delete("/api/v1/storage-backends/{id}", backend.getId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("默认存储后端不能删除，请先设置其他后端为默认"));
    }

    // ============ PUT /api/v1/storage-backends/{id}/default ============

    @Test
    void setDefault_shouldSetDefault() throws Exception {
        StorageBackend backend = createTestBackend();

        mockMvc.perform(put("/api/v1/storage-backends/{id}/default", backend.getId())
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    // ============ PUT /api/v1/storage-backends/{id}/status ============

    @Test
    void setEnabled_shouldToggleStatus() throws Exception {
        StorageBackend backend = createTestBackend();

        mockMvc.perform(put("/api/v1/storage-backends/{id}/status", backend.getId())
                .param("enabled", "false")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.enabled").value(false));
    }

    // ============ GET /api/v1/storage-backends/types ============

    @Test
    void getStorageTypes_shouldReturnAllTypes() throws Exception {
        mockMvc.perform(get("/api/v1/storage-backends/types")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data", hasSize(3)))
            .andExpect(jsonPath("$..code", hasItem("HARBOR")))
            .andExpect(jsonPath("$..code", hasItem("NEXUS")))
            .andExpect(jsonPath("$..code", hasItem("NAS")));
    }

    // ============ Helper methods ============

    private StorageBackend createTestBackend() {
        StorageBackend backend = new StorageBackend();
        backend.setBackendCode("harbor-get");
        backend.setBackendName("测试查询Harbor");
        backend.setBackendType(StorageBackendType.HARBOR);
        backend.setEndpoint("https://harbor-get.example.com");
        backend.setNamespace("library");
        backend.setEnabled(true);
        backend.setIsDefault(false);
        return storageBackendRepository.save(backend);
    }

    private Permission createPermissionIfNotExists(String code, String name) {
        return permissionRepository.findByPermissionCode(code)
            .orElseGet(() -> {
                Permission p = new Permission();
                p.setPermissionCode(code);
                p.setPermissionName(name);
                // Extract resource type from permission code (e.g., "storage:read" -> "storage")
                String resourceType = code.contains(":") ? code.split(":")[0] : "system";
                p.setResourceType(resourceType);
                return permissionRepository.save(p);
            });
    }
}
