package com.osrm.infrastructure.config;

import com.osrm.domain.business.entity.BusinessDomain;
import com.osrm.domain.business.entity.BusinessSystem;
import com.osrm.domain.business.repository.BusinessSystemRepository;
import com.osrm.domain.software.entity.PackageStatus;
import com.osrm.domain.software.entity.SoftwarePackage;
import com.osrm.domain.software.entity.SoftwareType;
import com.osrm.domain.software.repository.SoftwarePackageRepository;
import com.osrm.domain.user.entity.Permission;
import com.osrm.domain.user.entity.Role;
import com.osrm.domain.user.entity.User;
import com.osrm.domain.user.repository.PermissionRepository;
import com.osrm.domain.user.repository.RoleRepository;
import com.osrm.domain.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据初始化器
 * 优先使用 SQL 脚本初始化（data.sql），本类仅在数据不完整时补充初始化
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SoftwarePackageRepository softwarePackageRepository;
    private final BusinessSystemRepository businessSystemRepository;

    public DataInitializer(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           PasswordEncoder passwordEncoder,
                           SoftwarePackageRepository softwarePackageRepository,
                           BusinessSystemRepository businessSystemRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.softwarePackageRepository = softwarePackageRepository;
        this.businessSystemRepository = businessSystemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // MySQL 环境：数据初始化通过 data-mysql.sql 脚本执行
        // 代码中仅做基本检查和日志输出

        long permissionCount = permissionRepository.count();
        long roleCount = roleRepository.count();
        long userCount = userRepository.count();

        System.out.println("===============================================");
        System.out.println("OSRM Database Status (MySQL Mode)");
        System.out.println("===============================================");
        System.out.println("Permissions: " + permissionCount);
        System.out.println("Roles: " + roleCount);
        System.out.println("Users: " + userCount);

        if (permissionCount == 0 || roleCount == 0 || userCount == 0) {
            System.out.println("WARNING: Data appears to be missing!");
            System.out.println("Please run data-mysql.sql to initialize the database.");
        } else {
            System.out.println("Data check passed.");
        }
        System.out.println("===============================================");
    }

    private Map<String, Permission> ensurePermissionsInitialized() {
        Map<String, Permission> permissions = new HashMap<>();
        permissionRepository.findAll().forEach(p -> permissions.put(p.getPermissionCode(), p));

        if (permissions.isEmpty()) {
            System.out.println("Initializing permissions...");

            // 用户管理权限
            permissions.put("user:read", createPermission("user:read", "用户查看", "user", "read"));
            permissions.put("user:create", createPermission("user:create", "用户创建", "user", "create"));
            permissions.put("user:update", createPermission("user:update", "用户编辑", "user", "update"));
            permissions.put("user:delete", createPermission("user:delete", "用户删除", "user", "delete"));

            // 角色管理权限
            permissions.put("role:read", createPermission("role:read", "角色查看", "role", "read"));
            permissions.put("role:create", createPermission("role:create", "角色创建", "role", "create"));
            permissions.put("role:update", createPermission("role:update", "角色编辑", "role", "update"));
            permissions.put("role:delete", createPermission("role:delete", "角色删除", "role", "delete"));

            // 权限管理权限
            permissions.put("permission:read", createPermission("permission:read", "权限查看", "permission", "read"));
            permissions.put("permission:create", createPermission("permission:create", "权限创建", "permission", "create"));
            permissions.put("permission:update", createPermission("permission:update", "权限编辑", "permission", "update"));
            permissions.put("permission:delete", createPermission("permission:delete", "权限删除", "permission", "delete"));

            // 软件包权限
            permissions.put("package:read", createPermission("package:read", "软件浏览", "package", "read"));
            permissions.put("package:create", createPermission("package:create", "软件录入", "package", "create"));
            permissions.put("package:update", createPermission("package:update", "软件编辑", "package", "update"));
            permissions.put("package:delete", createPermission("package:delete", "软件删除", "package", "delete"));
            permissions.put("package:approve", createPermission("package:approve", "软件审批", "package", "approve"));

            // 订购权限
            permissions.put("subscription:create", createPermission("subscription:create", "订购申请", "subscription", "create"));
            permissions.put("subscription:read", createPermission("subscription:read", "订购查看", "subscription", "read"));
            permissions.put("subscription:approve", createPermission("subscription:approve", "订购审批", "subscription", "approve"));

            // 业务系统权限
            permissions.put("business-system:create", createPermission("business-system:create", "业务系统录入", "business-system", "create"));
            permissions.put("business-system:read", createPermission("business-system:read", "业务系统查看", "business-system", "read"));
            permissions.put("business-system:update", createPermission("business-system:update", "业务系统编辑", "business-system", "update"));
            permissions.put("business-system:approve", createPermission("business-system:approve", "业务系统审批", "business-system", "approve"));

            // 存储管理权限
            permissions.put("storage:read", createPermission("storage:read", "存储配置查看", "storage", "read"));
            permissions.put("storage:create", createPermission("storage:create", "存储配置创建", "storage", "create"));
            permissions.put("storage:update", createPermission("storage:update", "存储配置编辑", "storage", "update"));

            // 系统管理权限
            permissions.put("system:manage", createPermission("system:manage", "系统管理", "system", "manage"));

            // 使用追踪权限
            permissions.put("tracking:read", createPermission("tracking:read", "使用追踪查看", "tracking", "read"));

            System.out.println("Permissions initialized: " + permissions.size());
        } else {
            System.out.println("Permissions already initialized: " + permissions.size());

            // 检查并添加可能缺失的使用追踪权限
            if (!permissions.containsKey("tracking:read")) {
                System.out.println("Adding missing tracking:read permission...");
                Permission trackingPerm = createPermission("tracking:read", "使用追踪查看", "tracking", "read");
                permissions.put("tracking:read", trackingPerm);

                // 为系统管理员角色添加此权限
                Role adminRole = roleRepository.findByRoleCode("ROLE_SYSTEM_ADMIN").orElse(null);
                if (adminRole != null) {
                    adminRole.getPermissions().add(trackingPerm);
                    roleRepository.save(adminRole);
                    System.out.println("Added tracking:read permission to ROLE_SYSTEM_ADMIN");
                }
            }
        }

        return permissions;
    }

    private Permission createPermission(String code, String name, String resourceType, String action) {
        Permission permission = new Permission();
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setResourceType(resourceType);
        permission.setAction(action);
        return permissionRepository.save(permission);
    }

    private void ensureRolesInitialized(Map<String, Permission> permissions) {
        // 检查是否需要重新关联权限
        Role adminRole = roleRepository.findByRoleCode("ROLE_SYSTEM_ADMIN").orElse(null);

        if (adminRole == null) {
            System.out.println("Initializing roles...");

            // 系统管理员角色
            adminRole = createRole("ROLE_SYSTEM_ADMIN", "系统管理员", "全部权限，包括系统管理");
            adminRole.setPermissions(new HashSet<>(permissions.values()));
            roleRepository.save(adminRole);

            // 软件管理员角色
            Role packageManagerRole = createRole("ROLE_PACKAGE_MANAGER", "软件管理员", "管理软件包、业务系统、存储配置和订购审批");
            Set<Permission> packageManagerPerms = new HashSet<>();
            packageManagerPerms.add(permissions.get("package:read"));
            packageManagerPerms.add(permissions.get("package:create"));
            packageManagerPerms.add(permissions.get("package:update"));
            packageManagerPerms.add(permissions.get("package:delete"));
            packageManagerPerms.add(permissions.get("package:approve"));
            packageManagerPerms.add(permissions.get("subscription:read"));
            packageManagerPerms.add(permissions.get("subscription:approve"));
            packageManagerPerms.add(permissions.get("business-system:create"));
            packageManagerPerms.add(permissions.get("business-system:read"));
            packageManagerPerms.add(permissions.get("business-system:update"));
            packageManagerPerms.add(permissions.get("business-system:approve"));
            packageManagerPerms.add(permissions.get("storage:read"));
            packageManagerRole.setPermissions(packageManagerPerms);
            roleRepository.save(packageManagerRole);

            // 开发人员角色
            Role developerRole = createRole("ROLE_DEVELOPER", "开发人员", "可浏览和订购软件");
            Set<Permission> developerPerms = new HashSet<>();
            developerPerms.add(permissions.get("package:read"));
            developerPerms.add(permissions.get("subscription:create"));
            developerPerms.add(permissions.get("subscription:read"));
            developerPerms.add(permissions.get("business-system:read"));
            developerRole.setPermissions(developerPerms);
            roleRepository.save(developerRole);

            // 访客角色
            Role visitorRole = createRole("ROLE_VISITOR", "访客", "仅可浏览公开信息");
            Set<Permission> visitorPerms = new HashSet<>();
            visitorPerms.add(permissions.get("package:read"));
            visitorRole.setPermissions(visitorPerms);
            roleRepository.save(visitorRole);

            System.out.println("Roles initialized");
        } else if (adminRole.getPermissions() == null || adminRole.getPermissions().isEmpty()) {
            System.out.println("Roles exist but missing permissions, re-associating...");

            // 重新关联所有角色的权限
            roleRepository.findAll().forEach(role -> {
                Set<Permission> perms = new HashSet<>();
                switch (role.getRoleCode()) {
                    case "ROLE_SYSTEM_ADMIN":
                        perms.addAll(permissions.values());
                        break;
                    case "ROLE_PACKAGE_MANAGER":
                        perms.add(permissions.get("package:read"));
                        perms.add(permissions.get("package:create"));
                        perms.add(permissions.get("package:update"));
                        perms.add(permissions.get("package:delete"));
                        perms.add(permissions.get("package:approve"));
                        perms.add(permissions.get("subscription:read"));
                        perms.add(permissions.get("subscription:approve"));
                        perms.add(permissions.get("business-system:create"));
                        perms.add(permissions.get("business-system:read"));
                        perms.add(permissions.get("business-system:update"));
                        perms.add(permissions.get("business-system:approve"));
                        perms.add(permissions.get("storage:read"));
                        break;
                    case "ROLE_DEVELOPER":
                        perms.add(permissions.get("package:read"));
                        perms.add(permissions.get("subscription:create"));
                        perms.add(permissions.get("subscription:read"));
                        perms.add(permissions.get("business-system:read"));
                        break;
                    case "ROLE_VISITOR":
                        perms.add(permissions.get("package:read"));
                        break;
                }
                role.setPermissions(perms);
                roleRepository.save(role);
            });

            System.out.println("Role permissions re-associated");
        } else {
            System.out.println("Roles already initialized with permissions");
        }
    }

    private void ensureSampleDataInitialized() {
        // 获取 admin 用户 ID
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) return;
        Long adminId = admin.getId();

        // 初始化开发人员账号
        if (userRepository.findByUsername("dev1").isEmpty()) {
            Role devRole = roleRepository.findByRoleCode("ROLE_DEVELOPER").orElse(null);
            if (devRole != null) {
                User dev = new User();
                dev.setUsername("dev1");
                dev.setPassword(passwordEncoder.encode("dev123"));
                dev.setRealName("开发人员");
                dev.setEmail("dev1@osrm.local");
                dev.setEnabled(true);
                dev.setLoginFailCount(0);
                dev.setRoles(new HashSet<>(Collections.singletonList(devRole)));
                userRepository.save(dev);
                System.out.println("Developer user 'dev1' initialized");
            }
        }

        // 初始化业务系统样本数据
        if (businessSystemRepository.count() == 0) {
            BusinessSystem bs = new BusinessSystem();
            bs.setSystemCode("SYS-OA");
            bs.setSystemName("办公自动化系统");
            bs.setDomain(BusinessDomain.BUSINESS);
            bs.setResponsiblePerson("张三");
            bs.setDescription("企业内部 OA 系统");
            bs.setEnabled(true);
            bs.setCreatedBy(adminId);
            businessSystemRepository.save(bs);

            BusinessSystem bs2 = new BusinessSystem();
            bs2.setSystemCode("SYS-ERP");
            bs2.setSystemName("ERP 系统");
            bs2.setDomain(BusinessDomain.BUSINESS);
            bs2.setResponsiblePerson("李四");
            bs2.setDescription("企业资源规划系统");
            bs2.setEnabled(true);
            bs2.setCreatedBy(adminId);
            businessSystemRepository.save(bs2);
            System.out.println("Business systems initialized");
        }

        // 初始化软件包样本数据
        if (softwarePackageRepository.count() == 0) {
            createSamplePackage(adminId, "Spring Boot", "spring-boot", SoftwareType.MAVEN,
                    "Spring Boot 是用于构建独立、生产级 Spring 应用程序的框架，内置 Tomcat，无需部署 WAR 文件。",
                    "3.2.0", "https://spring.io/projects/spring-boot", "Apache-2.0");

            createSamplePackage(adminId, "React", "react", SoftwareType.NPM,
                    "用于构建用户界面的 JavaScript 库，由 Meta 开源维护，采用声明式、组件化设计。",
                    "18.2.0", "https://react.dev", "MIT");

            createSamplePackage(adminId, "Nginx", "nginx", SoftwareType.DOCKER_IMAGE,
                    "高性能 HTTP 服务器和反向代理，也用作邮件代理和通用 TCP/UDP 代理。",
                    "1.25.3", "https://nginx.org", "BSD-2-Clause");

            createSamplePackage(adminId, "Vue.js", "vue", SoftwareType.NPM,
                    "渐进式 JavaScript 框架，易于上手，可与其他库集成，也可用于构建复杂的单页应用。",
                    "3.4.0", "https://vuejs.org", "MIT");

            createSamplePackage(adminId, "Redis", "redis", SoftwareType.DOCKER_IMAGE,
                    "开源内存数据结构存储，用作数据库、缓存和消息代理，支持多种数据结构。",
                    "7.2.3", "https://redis.io", "BSD-3-Clause");

            createSamplePackage(adminId, "MyBatis-Plus", "mybatis-plus", SoftwareType.MAVEN,
                    "MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变，简化开发、提高效率。",
                    "3.5.5", "https://baomidou.com", "Apache-2.0");

            System.out.println("Sample software packages initialized");
        }
    }

    private void createSamplePackage(Long adminId, String name, String key, SoftwareType type,
                                      String description, String version, String website, String license) {
        SoftwarePackage pkg = new SoftwarePackage();
        pkg.setPackageName(name);
        pkg.setPackageKey(key);
        pkg.setSoftwareType(type);
        pkg.setDescription(description);
        pkg.setCurrentVersion(version);
        pkg.setWebsiteUrl(website);
        pkg.setLicenseType(license);
        pkg.setCreatedBy(adminId);
        pkg.setStatus(PackageStatus.PUBLISHED);
        pkg.setPublishedBy(adminId);
        pkg.setPublishedAt(LocalDateTime.now().minusDays((long)(Math.random() * 30)));
        pkg.setViewCount((int)(Math.random() * 200) + 10);
        pkg.setDownloadCount((int)(Math.random() * 100) + 5);
        pkg.setSubscriptionCount((int)(Math.random() * 50) + 1);
        // 设置默认分类ID为1（开发工具）
        pkg.setCategoryId(1L);
        softwarePackageRepository.save(pkg);
    }

    private Role createRole(String code, String name, String description) {
        Role role = new Role();
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setDescription(description);
        role.setEnabled(true);
        return roleRepository.save(role);
    }

    private void ensureAdminUserInitialized() {
        Optional<User> adminUser = userRepository.findByUsername("admin");

        if (adminUser.isEmpty()) {
            System.out.println("Initializing admin user...");

            Role adminRole = roleRepository.findByRoleCode("ROLE_SYSTEM_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_SYSTEM_ADMIN not found"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setEmail("admin@osrm.local");
            admin.setEnabled(true);
            admin.setLoginFailCount(0);
            admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
            userRepository.save(admin);

            System.out.println("Admin user initialized");
        } else {
            User admin = adminUser.get();
            // 确保管理员有角色
            if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
                System.out.println("Admin user missing roles, fixing...");
                Role adminRole = roleRepository.findByRoleCode("ROLE_SYSTEM_ADMIN")
                        .orElseThrow(() -> new RuntimeException("ROLE_SYSTEM_ADMIN not found"));
                admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
                userRepository.save(admin);
            }
            System.out.println("Admin user already initialized");
        }
    }
}
