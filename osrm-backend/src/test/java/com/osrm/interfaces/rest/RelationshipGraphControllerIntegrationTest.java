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
import com.osrm.domain.subscription.entity.SubscriptionStatus;
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
 * 关联图谱控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelationshipGraphControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private StorageBackendRepository storageBackendRepository;
    @Autowired private BusinessSystemRepository businessSystemRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long systemId1;
    private Long systemId2;
    private Long packageId1;
    private Long packageId2;
    private Long versionId1;
    private Long versionId2;

    @BeforeEach
    void setUp() throws Exception {
        // Permissions
        Permission trackingRead = createPermissionIfNotExists("tracking:read", "查看关联图");
        Permission pkgRead = createPermissionIfNotExists("package:read", "读取软件包");
        Permission pkgCreate = createPermissionIfNotExists("package:create", "创建软件包");
        Permission pkgApprove = createPermissionIfNotExists("package:approve", "审批软件包");
        Permission subCreate = createPermissionIfNotExists("subscription:create", "申请订购");
        Permission subApprove = createPermissionIfNotExists("subscription:approve", "审批订购");

        // Admin role
        Role adminRole = new Role();
        adminRole.setRoleCode("ROLE_TRACKING_ADMIN");
        adminRole.setRoleName("Tracking Admin");
        adminRole.setPermissions(new HashSet<>(Set.of(
            trackingRead, pkgRead, pkgCreate, pkgApprove, subCreate, subApprove
        )));
        adminRole = roleRepository.save(adminRole);

        // Admin user
        User adminUser = new User();
        adminUser.setUsername("trackingadmin");
        adminUser.setPassword(passwordEncoder.encode("test123"));
        adminUser.setRealName("Tracking Admin");
        adminUser.setEmail("tracking@osrm.local");
        adminUser.setEnabled(true);
        adminUser.setLoginFailCount(0);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(adminUser);

        // Get token
        adminToken = getToken("trackingadmin", "test123");

        // Create storage backend
        StorageBackend backend = new StorageBackend();
        backend.setBackendName("测试存储");
        backend.setBackendCode("tracking-test-storage");
        backend.setBackendType(StorageBackendType.HARBOR);
        backend.setEndpoint("https://harbor.test.local");
        backend.setEnabled(true);
        backend = storageBackendRepository.save(backend);

        // Create business systems
        BusinessSystem system1 = new BusinessSystem();
        system1.setSystemCode("track-sys-1");
        system1.setSystemName("追踪测试系统1");
        system1.setDomain(BusinessDomain.BUSINESS);
        system1.setEnabled(true);
        system1 = businessSystemRepository.save(system1);
        systemId1 = system1.getId();

        BusinessSystem system2 = new BusinessSystem();
        system2.setSystemCode("track-sys-2");
        system2.setSystemName("追踪测试系统2");
        system2.setDomain(BusinessDomain.OPERATION);
        system2.setEnabled(true);
        system2 = businessSystemRepository.save(system2);
        systemId2 = system2.getId();

        // Create and publish software packages
        Long[] pkg1 = createAndPublishPackage("tracking-mysql", "tracking-mysql", SoftwareType.DOCKER_IMAGE, backend.getId());
        packageId1 = pkg1[0];
        versionId1 = pkg1[1];
        Long[] pkg2 = createAndPublishPackage("tracking-redis", "tracking-redis", SoftwareType.DOCKER_IMAGE, backend.getId());
        packageId2 = pkg2[0];
        versionId2 = pkg2[1];

        // Create approved subscriptions to establish relationships
        createApprovedSubscription(packageId1, versionId1, systemId1, "8.0");
        createApprovedSubscription(packageId2, versionId2, systemId1, "7.0");
        createApprovedSubscription(packageId1, versionId1, systemId2, "8.0");
    }

    @Test
    void getRelationshipGraph_shouldReturnGraphData() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/relationship-graph")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodes").isArray())
            .andExpect(jsonPath("$.data.nodes.length()").value(greaterThanOrEqualTo(4)))
            .andExpect(jsonPath("$.data.edges").isArray())
            .andExpect(jsonPath("$.data.edges.length()").value(greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.data.metadata").exists())
            .andExpect(jsonPath("$.data.metadata.totalSystems").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.metadata.totalPackages").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.metadata.totalSubscriptions").value(greaterThanOrEqualTo(3)));
    }

    @Test
    void getRelationshipGraph_withDomainFilter_shouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/relationship-graph")
                .header("Authorization", "Bearer " + adminToken)
                .param("domain", "BUSINESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodes").isArray());
    }

    @Test
    void getRelationshipGraph_withSoftwareTypeFilter_shouldFilterResults() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/relationship-graph")
                .header("Authorization", "Bearer " + adminToken)
                .param("softwareType", "DOCKER_IMAGE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.nodes").isArray());
    }

    @Test
    void getSystemDependencies_shouldReturnSystemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/system/{systemId}/dependencies", systemId1)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.system").exists())
            .andExpect(jsonPath("$.data.system.systemId").value(systemId1))
            .andExpect(jsonPath("$.data.packages").isArray())
            .andExpect(jsonPath("$.data.packages.length()").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.statistics").exists())
            .andExpect(jsonPath("$.data.statistics.totalPackages").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.statistics.byType").exists());
    }

    @Test
    void getSystemDependencies_nonexistentSystem_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/system/{systemId}/dependencies", 999999)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.system").isEmpty());
    }

    @Test
    void getPackageImpact_shouldReturnImpactAnalysis() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/package/{packageId}/impact", packageId1)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.packageInfo").exists())
            .andExpect(jsonPath("$.data.packageInfo.packageId").value(packageId1))
            .andExpect(jsonPath("$.data.affectedSystems").isArray())
            .andExpect(jsonPath("$.data.affectedSystems.length()").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.statistics").exists())
            .andExpect(jsonPath("$.data.statistics.totalSystems").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.statistics.byDomain").exists())
            .andExpect(jsonPath("$.data.statistics.byVersion").exists());
    }

    @Test
    void getPackageImpact_nonexistentPackage_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/package/{packageId}/impact", 999999)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.packageInfo").isEmpty());
    }

    @Test
    void getRelationshipGraph_withoutAuth_shouldReturn401Or403() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/relationship-graph"))
            .andExpect(status().is4xxClientError());
    }

    // Helper methods

    private Long[] createAndPublishPackage(String name, String key, SoftwareType type, Long backendId) throws Exception {
        CreatePackageRequest pkgReq = new CreatePackageRequest();
        pkgReq.setPackageName(name);
        pkgReq.setPackageKey(key);
        pkgReq.setSoftwareType(type);

        String pkgResponse = mockMvc.perform(post("/api/v1/software-packages")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pkgReq)))
            .andReturn().getResponse().getContentAsString();
        Long packageId = objectMapper.readTree(pkgResponse).get("data").get("id").asLong();

        // Add version
        CreateVersionRequest verReq = new CreateVersionRequest();
        verReq.setVersionNo("1.0.0");
        verReq.setStorageBackendId(backendId);
        String verResponse = mockMvc.perform(post("/api/v1/software-packages/" + packageId + "/versions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verReq)))
            .andReturn().getResponse().getContentAsString();
        Long versionId = objectMapper.readTree(verResponse).get("data").get("id").asLong();

        // Submit and approve
        mockMvc.perform(post("/api/v1/software-packages/" + packageId + "/submit")
                .header("Authorization", "Bearer " + adminToken))
            .andReturn();
        mockMvc.perform(post("/api/v1/software-packages/" + packageId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andReturn();

        return new Long[]{packageId, versionId};
    }

    private void createApprovedSubscription(Long packageId, Long versionId, Long systemId, String version) throws Exception {
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(packageId);
        req.setVersionId(versionId);
        req.setBusinessSystemId(systemId);
        req.setUseScene("测试使用场景");

        String response = mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();
        Long subId = objectMapper.readTree(response).get("data").get("id").asLong();

        // Approve the subscription
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andReturn();
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
