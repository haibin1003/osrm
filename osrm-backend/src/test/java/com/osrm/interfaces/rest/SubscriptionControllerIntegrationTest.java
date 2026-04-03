package com.osrm.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.software.dto.request.CreatePackageRequest;
import com.osrm.application.software.dto.request.CreateVersionRequest;
import com.osrm.application.subscription.dto.request.CreateSubscriptionRequest;
import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
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
 * 订购管理控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubscriptionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private StorageBackendRepository storageBackendRepository;
    @Autowired private BusinessSystemRepository businessSystemRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String userToken;
    private String adminToken;
    private Long publishedPackageId;
    private Long publishedVersionId;
    private Long businessSystemId;

    @BeforeEach
    void setUp() throws Exception {
        // Permissions
        Permission subRead = createPermissionIfNotExists("subscription:read", "读取订购");
        Permission subCreate = createPermissionIfNotExists("subscription:create", "申请订购");
        Permission subApprove = createPermissionIfNotExists("subscription:approve", "审批订购");
        Permission pkgRead = createPermissionIfNotExists("package:read", "读取软件包");
        Permission pkgCreate = createPermissionIfNotExists("package:create", "创建软件包");
        Permission pkgApprove = createPermissionIfNotExists("package:approve", "审批软件包");

        // Developer role (can apply)
        Role devRole = new Role();
        devRole.setRoleCode("ROLE_DEV_TEST");
        devRole.setRoleName("Developer Test");
        devRole.setPermissions(new HashSet<>(Set.of(subRead, subCreate, pkgRead)));
        devRole = roleRepository.save(devRole);

        // Admin role (can approve everything)
        Role adminRole = new Role();
        adminRole.setRoleCode("ROLE_ADMIN_TEST");
        adminRole.setRoleName("Admin Test");
        adminRole.setPermissions(new HashSet<>(Set.of(subRead, subCreate, subApprove, pkgRead, pkgCreate, pkgApprove)));
        adminRole = roleRepository.save(adminRole);

        // Developer user
        User devUser = new User();
        devUser.setUsername("devuser");
        devUser.setPassword(passwordEncoder.encode("test123"));
        devUser.setRealName("Developer");
        devUser.setEmail("dev@osrm.local");
        devUser.setEnabled(true);
        devUser.setLoginFailCount(0);
        devUser.setRoles(new HashSet<>(Set.of(devRole)));
        userRepository.save(devUser);

        // Admin user
        User adminUser = new User();
        adminUser.setUsername("adminuser");
        adminUser.setPassword(passwordEncoder.encode("test123"));
        adminUser.setRealName("Admin");
        adminUser.setEmail("admin@osrm.local");
        adminUser.setEnabled(true);
        adminUser.setLoginFailCount(0);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(adminUser);

        // Get tokens
        userToken = getToken("devuser", "test123");
        adminToken = getToken("adminuser", "test123");

        // Create storage backend
        StorageBackend backend = new StorageBackend();
        backend.setBackendName("测试Harbor");
        backend.setBackendCode("sub-test-harbor");
        backend.setBackendType(StorageBackendType.HARBOR);
        backend.setEndpoint("https://harbor.test.local");
        backend.setEnabled(true);
        backend = storageBackendRepository.save(backend);

        // Create and publish a software package
        CreatePackageRequest pkgReq = new CreatePackageRequest();
        pkgReq.setPackageName("sub-test-app");
        pkgReq.setPackageKey("sub-test-app");
        pkgReq.setSoftwareType(SoftwareType.DOCKER_IMAGE);

        String pkgResponse = mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pkgReq)))
            .andReturn().getResponse().getContentAsString();
        publishedPackageId = objectMapper.readTree(pkgResponse).get("data").get("id").asLong();

        // Add version
        CreateVersionRequest verReq = new CreateVersionRequest();
        verReq.setVersionNo("1.0.0");
        verReq.setStorageBackendId(backend.getId());
        String verResponse = mockMvc.perform(post("/api/v1/software-packages/" + publishedPackageId + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verReq)))
            .andReturn().getResponse().getContentAsString();
        publishedVersionId = objectMapper.readTree(verResponse).get("data").get("id").asLong();

        // Submit and approve to PUBLISHED
        mockMvc.perform(post("/api/v1/software-packages/" + publishedPackageId + "/submit")
                .header("Authorization", "Bearer " + adminToken))
            .andReturn();
        mockMvc.perform(post("/api/v1/software-packages/" + publishedPackageId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andReturn();

        // Create business system
        BusinessSystem system = new BusinessSystem();
        system.setSystemCode("sys-sub-test");
        system.setSystemName("订购测试系统");
        system.setDomain(BusinessDomain.BUSINESS);
        system.setEnabled(true);
        system = businessSystemRepository.save(system);
        businessSystemId = system.getId();
    }

    @Test
    void mySubscriptions_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/my")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void apply_shouldCreatePendingSubscription() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(publishedPackageId);
        req.setVersionId(publishedVersionId);
        req.setBusinessSystemId(businessSystemId);
        req.setUseScene("测试使用场景");

        mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.packageId").value(publishedPackageId));
    }

    @Test
    void apply_nonExistPackage_shouldFail() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(999999L);
        req.setBusinessSystemId(businessSystemId);

        mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(not(200)));
    }

    @Test
    void fullLifecycle_applyApprove() throws Exception {
        // Apply
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(publishedPackageId);
        req.setVersionId(publishedVersionId);
        req.setBusinessSystemId(businessSystemId);
        req.setUseScene("生产使用");

        String applyResponse = mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        Long subId = objectMapper.readTree(applyResponse).get("data").get("id").asLong();

        // View pending list
        mockMvc.perform(get("/api/v1/subscriptions/pending")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));

        // Approve
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // Get download token
        mockMvc.perform(get("/api/v1/subscriptions/" + subId + "/token")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void fullLifecycle_applyReject() throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(publishedPackageId);
        req.setVersionId(publishedVersionId);
        req.setBusinessSystemId(businessSystemId);

        String applyResponse = mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();
        Long subId = objectMapper.readTree(applyResponse).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/reject")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void pendingList_withoutApprovePermission_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/pending")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().is4xxClientError());
    }

    private String getToken(String username, String password) throws Exception {
        String loginJson = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("accessToken").asText();
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
