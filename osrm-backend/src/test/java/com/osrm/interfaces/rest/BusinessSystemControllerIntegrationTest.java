package com.osrm.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrm.application.business.dto.request.CreateBusinessSystemRequest;
import com.osrm.application.business.dto.request.UpdateBusinessSystemRequest;
import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BusinessSystemControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private BusinessSystemRepository businessSystemRepository;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        Permission businessRead = createPermissionIfNotExists("business-system:read", "业务读取");
        Permission businessCreate = createPermissionIfNotExists("business-system:create", "业务创建");
        Permission businessUpdate = createPermissionIfNotExists("business-system:update", "业务更新");
        Permission businessApprove = createPermissionIfNotExists("business-system:approve", "业务审批");
        Permission systemManage = createPermissionIfNotExists("system:manage", "系统管理");

        Role testRole = new Role();
        testRole.setRoleCode("ROLE_TEST_ADMIN");
        testRole.setRoleName("Test Admin");
        Set<Permission> permissions = new HashSet<>(Set.of(businessRead, businessCreate, businessUpdate, businessApprove, systemManage));
        testRole.setPermissions(permissions);
        testRole = roleRepository.save(testRole);

        User testUser = new User();
        testUser.setUsername("testadmin");
        testUser.setPassword(passwordEncoder.encode("test123"));
        testUser.setRealName("Test Admin");
        testUser.setEmail("test@osrm.local");
        testUser.setEnabled(true);
        testUser.setLoginFailCount(0);
        testUser.setRoles(new HashSet<>(Set.of(testRole)));
        userRepository.save(testUser);

        String loginJson = "{\"username\":\"testadmin\",\"password\":\"test123\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginJson))
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("data").get("accessToken").asText();
    }

    @Test
    void list_shouldReturnPagedResult() throws Exception {
        mockMvc.perform(get("/api/v1/business-systems")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void create_shouldCreateSystem() throws Exception {
        CreateBusinessSystemRequest request = new CreateBusinessSystemRequest();
        request.setSystemCode("order-sys");
        request.setSystemName("订单系统");
        request.setDomain(BusinessDomain.BUSINESS);
        request.setResponsiblePerson("张三");
        request.setDescription("订单管理系统");

        mockMvc.perform(post("/api/v1/business-systems")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.systemCode").value("order-sys"))
                .andExpect(jsonPath("$.data.systemName").value("订单系统"))
                .andExpect(jsonPath("$.data.domainName").value("业务域"));
    }

    @Test
    void create_withDuplicateCode_shouldReturnError() throws Exception {
        BusinessSystem existing = new BusinessSystem();
        existing.setSystemCode("dup-sys");
        existing.setSystemName("已有系统");
        existing.setDomain(BusinessDomain.BUSINESS);
        existing.setEnabled(true);
        businessSystemRepository.save(existing);

        CreateBusinessSystemRequest request = new CreateBusinessSystemRequest();
        request.setSystemCode("dup-sys");
        request.setSystemName("新系统");
        request.setDomain(BusinessDomain.BUSINESS);

        mockMvc.perform(post("/api/v1/business-systems")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统编码已存在"));
    }

    @Test
    void create_withoutRequiredFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/business-systems")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_shouldReturnSystem() throws Exception {
        BusinessSystem system = createTestSystem();

        mockMvc.perform(get("/api/v1/business-systems/{id}", system.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.systemCode").value("test-sys"));
    }

    @Test
    void getById_withNonExistingId_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/v1/business-systems/999")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("业务系统不存在"));
    }

    @Test
    void update_shouldUpdateSystem() throws Exception {
        BusinessSystem system = createTestSystem();

        UpdateBusinessSystemRequest request = new UpdateBusinessSystemRequest();
        request.setSystemName("更新后的系统");
        request.setDomain(BusinessDomain.RESOURCE);

        mockMvc.perform(put("/api/v1/business-systems/{id}", system.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.systemName").value("更新后的系统"));
    }

    @Test
    void setEnabled_shouldToggleStatus() throws Exception {
        BusinessSystem system = createTestSystem();

        mockMvc.perform(put("/api/v1/business-systems/{id}/status", system.getId())
                .param("enabled", "false")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void delete_shouldDeleteDisabledSystem() throws Exception {
        BusinessSystem system = createTestSystem();
        system.setEnabled(false);
        businessSystemRepository.save(system);

        mockMvc.perform(delete("/api/v1/business-systems/{id}", system.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void delete_withEnabledSystem_shouldReturnError() throws Exception {
        BusinessSystem system = createTestSystem();

        mockMvc.perform(delete("/api/v1/business-systems/{id}", system.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("已启用的业务系统不能删除，请先停用后再删除"));
    }

    @Test
    void getDomains_shouldReturnAllDomains() throws Exception {
        mockMvc.perform(get("/api/v1/business-systems/domains")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(5)));
    }

    private BusinessSystem createTestSystem() {
        BusinessSystem system = new BusinessSystem();
        system.setSystemCode("test-sys");
        system.setSystemName("测试系统");
        system.setDomain(BusinessDomain.BUSINESS);
        system.setResponsiblePerson("测试人");
        system.setEnabled(true);
        return businessSystemRepository.save(system);
    }

    private Permission createPermissionIfNotExists(String code, String name) {
        return permissionRepository.findByPermissionCode(code)
                .orElseGet(() -> {
                    Permission p = new Permission();
                    p.setPermissionCode(code);
                    p.setPermissionName(name);
                    p.setResourceType(code.contains(":") ? code.split(":")[0] : "system");
                    return permissionRepository.save(p);
                });
    }
}
