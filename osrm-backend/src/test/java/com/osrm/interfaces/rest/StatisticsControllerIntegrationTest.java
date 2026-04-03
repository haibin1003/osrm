package com.osrm.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.subscription.dto.request.CreateSubscriptionRequest;
import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import com.osrm.domain.software.entity.SoftwareVersion;
import com.osrm.domain.software.entity.VersionStatus;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.software.repository.SoftwareVersionRepository;
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
 * 统计功能控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StatisticsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private StorageBackendRepository storageBackendRepository;
    @Autowired private BusinessSystemRepository businessSystemRepository;
    @Autowired private SoftwarePackageRepository softwarePackageRepository;
    @Autowired private SoftwareVersionRepository softwareVersionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private Long businessSystemId1;
    private Long businessSystemId2;

    @BeforeEach
    void setUp() throws Exception {
        // Permissions
        Permission statsRead = createPermissionIfNotExists("statistics:read", "查看统计");
        Permission pkgRead = createPermissionIfNotExists("package:read", "读取软件包");
        Permission pkgCreate = createPermissionIfNotExists("package:create", "创建软件包");
        Permission pkgApprove = createPermissionIfNotExists("package:approve", "审批软件包");
        Permission subCreate = createPermissionIfNotExists("subscription:create", "申请订购");
        Permission subApprove = createPermissionIfNotExists("subscription:approve", "审批订购");

        // Admin role
        Role adminRole = new Role();
        adminRole.setRoleCode("ROLE_STATS_ADMIN");
        adminRole.setRoleName("Stats Admin");
        adminRole.setPermissions(new HashSet<>(Set.of(
            statsRead, pkgRead, pkgCreate, pkgApprove, subCreate, subApprove
        )));
        adminRole = roleRepository.save(adminRole);

        // Admin user
        User adminUser = new User();
        adminUser.setUsername("statsadmin");
        adminUser.setPassword(passwordEncoder.encode("test123"));
        adminUser.setRealName("Stats Admin");
        adminUser.setEmail("stats@osrm.local");
        adminUser.setEnabled(true);
        adminUser.setLoginFailCount(0);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(adminUser);

        // Get token
        adminToken = getToken("statsadmin", "test123");

        // Create storage backend
        StorageBackend backend = new StorageBackend();
        backend.setBackendName("测试存储");
        backend.setBackendCode("stats-test-storage");
        backend.setBackendType(StorageBackendType.HARBOR);
        backend.setEndpoint("https://harbor.test.local");
        backend.setEnabled(true);
        backend = storageBackendRepository.save(backend);

        // Create business systems
        BusinessSystem system1 = new BusinessSystem();
        system1.setSystemCode("stats-sys-1");
        system1.setSystemName("统计测试系统1");
        system1.setDomain(BusinessDomain.BUSINESS);
        system1.setEnabled(true);
        system1 = businessSystemRepository.save(system1);
        businessSystemId1 = system1.getId();

        BusinessSystem system2 = new BusinessSystem();
        system2.setSystemCode("stats-sys-2");
        system2.setSystemName("统计测试系统2");
        system2.setDomain(BusinessDomain.OPERATION);
        system2.setEnabled(true);
        system2 = businessSystemRepository.save(system2);
        businessSystemId2 = system2.getId();

        // Create and publish multiple software packages of different types
        createPublishedPackage("docker-app-1", "docker-app-1", SoftwareType.DOCKER_IMAGE, backend.getId());
        createPublishedPackage("docker-app-2", "docker-app-2", SoftwareType.DOCKER_IMAGE, backend.getId());
        createPublishedPackage("helm-app-1", "helm-app-1", SoftwareType.HELM_CHART, backend.getId());
        createPublishedPackage("maven-lib", "maven-lib", SoftwareType.MAVEN, backend.getId());
        createPublishedPackage("npm-package", "npm-package", SoftwareType.NPM, backend.getId());
    }

    private void createPublishedPackage(String name, String key, SoftwareType type, Long backendId) {
        SoftwarePackage pkg = new SoftwarePackage();
        pkg.setPackageName(name);
        pkg.setPackageKey(key);
        pkg.setSoftwareType(type);
        pkg.setStatus(PackageStatus.PUBLISHED);
        pkg.setCreatedBy(1L);
        pkg = softwarePackageRepository.save(pkg);

        SoftwareVersion ver = new SoftwareVersion();
        ver.setSoftwarePackage(pkg);
        ver.setVersionNo("1.0.0");
        ver.setStatus(VersionStatus.PUBLISHED);
        ver.setStorageBackendId(backendId);
        ver.setIsLatest(true);
        ver.setCreatedBy(1L);
        softwareVersionRepository.save(ver);
    }

    @Test
    void getOverview_shouldReturnStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/overview")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalPackages").value(greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.data.totalSubscriptions").value(greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.data.activeBusinessSystems").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.newSubscriptionsThisMonth").value(greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.data.trends").exists())
            .andExpect(jsonPath("$.data.trends.totalPackagesChange").exists())
            .andExpect(jsonPath("$.data.trends.totalSubscriptionsChange").exists());
    }

    @Test
    void getTrend_shouldReturnDailyData() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/trend")
                .header("Authorization", "Bearer " + adminToken)
                .param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.period").value("daily"))
            .andExpect(jsonPath("$.data.totalDays").value(7))
            .andExpect(jsonPath("$.data.startDate").exists())
            .andExpect(jsonPath("$.data.endDate").exists())
            .andExpect(jsonPath("$.data.data").isArray())
            .andExpect(jsonPath("$.data.data.length()").value(7))
            .andExpect(jsonPath("$.data.data[0].date").exists())
            .andExpect(jsonPath("$.data.data[0].subscriptionCount").exists())
            .andExpect(jsonPath("$.data.data[0].approvedCount").exists())
            .andExpect(jsonPath("$.data.data[0].rejectedCount").exists())
            .andExpect(jsonPath("$.data.data[0].pendingCount").exists())
            .andExpect(jsonPath("$.data.summary").exists())
            .andExpect(jsonPath("$.data.summary.totalSubscriptionCount").exists())
            .andExpect(jsonPath("$.data.summary.averageDaily").exists());
    }

    @Test
    void getTrend_withCustomDays_shouldReturnCorrectNumberOfDays() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/trend")
                .header("Authorization", "Bearer " + adminToken)
                .param("days", "14"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalDays").value(14))
            .andExpect(jsonPath("$.data.data.length()").value(14));
    }

    @Test
    void getBusinessDistribution_shouldReturnSystemStats() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/business-distribution")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalBusinessSystems").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.data.totalPackages").value(greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.data.data").isArray())
            .andExpect(jsonPath("$.data.data[0].systemName").exists())
            .andExpect(jsonPath("$.data.data[0].systemCode").exists())
            .andExpect(jsonPath("$.data.data[0].packageCount").exists())
            .andExpect(jsonPath("$.data.data[0].subscriptionCount").exists())
            .andExpect(jsonPath("$.data.data[0].percentage").exists());
    }

    @Test
    void getPopularity_shouldReturnPackageRankings() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/popularity")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "10")
                .param("sortBy", "subscription_count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.sortBy").value("subscription_count"))
            .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(5)))
            .andExpect(jsonPath("$.data.data").isArray())
            .andExpect(jsonPath("$.data.data[0].rank").exists())
            .andExpect(jsonPath("$.data.data[0].packageId").exists())
            .andExpect(jsonPath("$.data.data[0].packageName").exists())
            .andExpect(jsonPath("$.data.data[0].packageKey").exists())
            .andExpect(jsonPath("$.data.data[0].subscriptionCount").exists())
            .andExpect(jsonPath("$.data.data[0].businessSystemCount").exists())
            .andExpect(jsonPath("$.data.data[0].trend").exists());
    }

    @Test
    void getPopularity_sortByBusinessSystem_shouldReturnDifferentOrder() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/popularity")
                .header("Authorization", "Bearer " + adminToken)
                .param("limit", "10")
                .param("sortBy", "business_system_count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.sortBy").value("business_system_count"));
    }

    @Test
    void getTypeDistribution_shouldReturnTypeStats() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/type-distribution")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalPackages").exists())
            .andExpect(jsonPath("$.data.data").isArray());
    }

    @Test
    void fullScenario_withSubscriptions_shouldReflectInStatistics() throws Exception {
        // Create a new package and publish it directly via repository
        Long backendId = storageBackendRepository.findAll().get(0).getId();
        SoftwarePackage pkg = new SoftwarePackage();
        pkg.setPackageName("stats-scenario-app");
        pkg.setPackageKey("stats-scenario-app");
        pkg.setSoftwareType(SoftwareType.DOCKER_IMAGE);
        pkg.setStatus(PackageStatus.PUBLISHED);
        pkg.setCreatedBy(1L);
        pkg = softwarePackageRepository.save(pkg);

        SoftwareVersion ver = new SoftwareVersion();
        ver.setSoftwarePackage(pkg);
        ver.setVersionNo("1.0.0");
        ver.setStatus(VersionStatus.PUBLISHED);
        ver.setStorageBackendId(backendId);
        ver.setIsLatest(true);
        ver.setCreatedBy(1L);
        ver = softwareVersionRepository.save(ver);

        // Apply for subscription
        CreateSubscriptionRequest req = new CreateSubscriptionRequest();
        req.setPackageId(pkg.getId());
        req.setVersionId(ver.getId());
        req.setBusinessSystemId(businessSystemId1);
        req.setUseScene("统计测试场景");

        String applyResponse = mockMvc.perform(post("/api/v1/subscriptions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getContentAsString();
        Long subId = objectMapper.readTree(applyResponse).get("data").get("id").asLong();

        // Approve the subscription
        mockMvc.perform(post("/api/v1/subscriptions/" + subId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // Verify statistics reflect the subscription
        mockMvc.perform(get("/api/v1/statistics/overview")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalSubscriptions").value(greaterThanOrEqualTo(1)));

        // Verify type distribution returns valid structure
        mockMvc.perform(get("/api/v1/statistics/type-distribution")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.data").isArray());
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
