package com.osrm;

import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.repository.PermissionRepository;
import com.osrm.domain.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class EncodingTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @BeforeEach
    void setUp() {
        // Create a basic permission if not exists for the role
        Permission perm = permissionRepository.findByPermissionCode("user:read").orElseGet(() -> {
            Permission p = new Permission();
            p.setPermissionCode("user:read");
            p.setPermissionName("用户查看");
            p.setResourceType("user");
            p.setAction("read");
            return permissionRepository.save(p);
        });

        // Create ROLE_SYSTEM_ADMIN with Chinese characters
        if (roleRepository.findByRoleCode("ROLE_SYSTEM_ADMIN").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setRoleCode("ROLE_SYSTEM_ADMIN");
            adminRole.setRoleName("系统管理员");
            adminRole.setDescription("系统管理全权权限");
            adminRole.setEnabled(true);
            adminRole.setPermissions(new HashSet<>(Set.of(perm)));
            roleRepository.save(adminRole);
        }
    }

    @Test
    public void testChineseEncoding() {
        List<Role> roles = roleRepository.findAll();
        assertFalse(roles.isEmpty(), "Roles should not be empty");

        for (Role role : roles) {
            System.out.println("Role: " + role.getRoleCode() + " | " + role.getRoleName() + " | " + role.getDescription());
        }

        // Check specific roles
        Role adminRole = roles.stream()
            .filter(r -> "ROLE_SYSTEM_ADMIN".equals(r.getRoleCode()))
            .findFirst()
            .orElse(null);

        assertNotNull(adminRole, "Admin role should exist");
        assertEquals("系统管理员", adminRole.getRoleName(), "Admin role name should be in Chinese");
        assertTrue(adminRole.getDescription().contains("系统管理"), "Description should contain Chinese characters");
    }
}
