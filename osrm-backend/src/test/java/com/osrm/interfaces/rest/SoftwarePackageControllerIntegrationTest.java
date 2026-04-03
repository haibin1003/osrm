package com.osrm.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.software.dto.request.CreatePackageRequest;
import com.osrm.application.software.dto.request.CreateVersionRequest;
import com.osrm.domain.software.entity.SoftwareType;
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
 * 软件包管理控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SoftwarePackageControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private StorageBackendRepository storageBackendRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private Long storageBackendId;

    @BeforeEach
    void setUp() throws Exception {
        // Setup permissions
        Permission pkgRead = createPermissionIfNotExists("package:read", "读取软件包");
        Permission pkgCreate = createPermissionIfNotExists("package:create", "创建软件包");
        Permission pkgUpdate = createPermissionIfNotExists("package:update", "更新软件包");
        Permission pkgDelete = createPermissionIfNotExists("package:delete", "删除软件包");
        Permission pkgApprove = createPermissionIfNotExists("package:approve", "审批软件包");

        Role testRole = new Role();
        testRole.setRoleCode("ROLE_PKG_TEST");
        testRole.setRoleName("Package Test Role");
        testRole.setPermissions(new HashSet<>(Set.of(pkgRead, pkgCreate, pkgUpdate, pkgDelete, pkgApprove)));
        testRole = roleRepository.save(testRole);

        User testUser = new User();
        testUser.setUsername("pkgtest");
        testUser.setPassword(passwordEncoder.encode("test123"));
        testUser.setRealName("Package Tester");
        testUser.setEmail("pkgtest@osrm.local");
        testUser.setEnabled(true);
        testUser.setLoginFailCount(0);
        testUser.setRoles(new HashSet<>(Set.of(testRole)));
        userRepository.save(testUser);

        String loginJson = "{\"username\":\"pkgtest\",\"password\":\"test123\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("data").get("accessToken").asText();

        // Create a storage backend for version tests
        StorageBackend backend = new StorageBackend();
        backend.setBackendName("测试Harbor");
        backend.setBackendCode("test-harbor");
        backend.setBackendType(StorageBackendType.HARBOR);
        backend.setEndpoint("https://harbor.test.local");
        backend.setEnabled(true);
        backend = storageBackendRepository.save(backend);
        storageBackendId = backend.getId();
    }

    // ============ GET /api/v1/software-packages ============

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        mockMvc.perform(get("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void list_withoutAuth_shouldReturn401Or403() throws Exception {
        mockMvc.perform(get("/api/v1/software-packages"))
            .andExpect(status().is4xxClientError());
    }

    // ============ POST /api/v1/software-packages ============

    @Test
    void create_shouldCreate() throws Exception {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setPackageName("test-docker");
        req.setPackageKey("test-docker");
        req.setSoftwareType(SoftwareType.DOCKER_IMAGE);
        req.setDescription("测试Docker镜像");

        mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.packageName").value("test-docker"))
            .andExpect(jsonPath("$.data.packageKey").value("test-docker"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void create_duplicateName_shouldReturn400() throws Exception {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setPackageName("dup-pkg");
        req.setPackageKey("dup-pkg");
        req.setSoftwareType(SoftwareType.MAVEN);

        // First creation
        mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());

        // Duplicate
        mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(not(200)));
    }

    // ============ Full lifecycle test ============

    @Test
    void fullLifecycle_createSubmitApprove() throws Exception {
        // 1. Create package
        CreatePackageRequest createReq = new CreatePackageRequest();
        createReq.setPackageName("lifecycle-app");
        createReq.setPackageKey("lifecycle-app");
        createReq.setSoftwareType(SoftwareType.DOCKER_IMAGE);

        String createResponse = mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andReturn().getResponse().getContentAsString();

        Long pkgId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // 2. Submit without version should fail
        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/submit")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(not(200)));

        // 3. Add version
        CreateVersionRequest versionReq = new CreateVersionRequest();
        versionReq.setVersionNo("1.0.0");
        versionReq.setStorageBackendId(storageBackendId);
        versionReq.setReleaseNotes("首个版本");

        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/versions")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(versionReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.versionNo").value("1.0.0"));

        // 4. Submit
        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/submit")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 5. Cannot submit again
        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/submit")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(not(200)));

        // 6. Approve
        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/approve")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 7. Offline
        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/offline")
                .param("reason", "测试下架")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("OFFLINE"));

        // 8. Republish
        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/republish")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    void reject_shouldReturnToDraft() throws Exception {
        // Create and add version
        CreatePackageRequest createReq = new CreatePackageRequest();
        createReq.setPackageName("reject-test");
        createReq.setPackageKey("reject-test");
        createReq.setSoftwareType(SoftwareType.NPM);

        String createResponse = mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andReturn().getResponse().getContentAsString();
        Long pkgId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        CreateVersionRequest vReq = new CreateVersionRequest();
        vReq.setVersionNo("1.0.0");
        vReq.setStorageBackendId(storageBackendId);

        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/versions")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vReq)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/submit")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/reject")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void delete_draftPackage_shouldDelete() throws Exception {
        CreatePackageRequest req = new CreatePackageRequest();
        req.setPackageName("delete-me");
        req.setPackageKey("delete-me");
        req.setSoftwareType(SoftwareType.GENERIC);

        String response = mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();
        Long pkgId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/v1/software-packages/" + pkgId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // Should not be found anymore
        mockMvc.perform(get("/api/v1/software-packages/" + pkgId)
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void getSoftwareTypes_shouldReturnAll() throws Exception {
        mockMvc.perform(get("/api/v1/software-packages/types")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(6)));
    }

    @Test
    void getVersions_shouldReturnVersionList() throws Exception {
        CreatePackageRequest createReq = new CreatePackageRequest();
        createReq.setPackageName("ver-test-pkg");
        createReq.setPackageKey("ver-test-pkg");
        createReq.setSoftwareType(SoftwareType.MAVEN);

        String createResponse = mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
            .andReturn().getResponse().getContentAsString();
        Long pkgId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        CreateVersionRequest vReq = new CreateVersionRequest();
        vReq.setVersionNo("2.0.0");
        vReq.setStorageBackendId(storageBackendId);

        mockMvc.perform(post("/api/v1/software-packages/" + pkgId + "/versions")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(vReq)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/software-packages/" + pkgId + "/versions")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].versionNo").value("2.0.0"));
    }

    private Permission createPermissionIfNotExists(String code, String name) {
        return permissionRepository.findByPermissionCode(code).orElseGet(() -> {
            Permission p = new Permission();
            p.setPermissionCode(code);
            p.setPermissionName(name);
            p.setResourceType("API");
            return permissionRepository.save(p);
        });
    }
}
