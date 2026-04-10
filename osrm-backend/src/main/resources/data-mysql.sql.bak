-- OSRM 数据库初始化脚本
-- 执行顺序：先执行 schema.sql 创建表结构，再执行此脚本插入初始数据

-- ========================================
-- 1. 权限表 (t_permission)
-- ========================================
INSERT INTO t_permission (id, permission_code, permission_name, resource_type, action, created_at, updated_at) VALUES
(1, 'user:read', '用户查看', 'user', 'read', NOW(), NOW()),
(2, 'user:create', '用户创建', 'user', 'create', NOW(), NOW()),
(3, 'user:update', '用户编辑', 'user', 'update', NOW(), NOW()),
(4, 'user:delete', '用户删除', 'user', 'delete', NOW(), NOW()),
(5, 'role:read', '角色查看', 'role', 'read', NOW(), NOW()),
(6, 'role:create', '角色创建', 'role', 'create', NOW(), NOW()),
(7, 'role:update', '角色编辑', 'role', 'update', NOW(), NOW()),
(8, 'role:delete', '角色删除', 'role', 'delete', NOW(), NOW()),
(9, 'permission:read', '权限查看', 'permission', 'read', NOW(), NOW()),
(10, 'permission:create', '权限创建', 'permission', 'create', NOW(), NOW()),
(11, 'permission:update', '权限编辑', 'permission', 'update', NOW(), NOW()),
(12, 'permission:delete', '权限删除', 'permission', 'delete', NOW(), NOW()),
(13, 'package:read', '软件浏览', 'package', 'read', NOW(), NOW()),
(14, 'package:create', '软件录入', 'package', 'create', NOW(), NOW()),
(15, 'package:update', '软件编辑', 'package', 'update', NOW(), NOW()),
(16, 'package:delete', '软件删除', 'package', 'delete', NOW(), NOW()),
(17, 'package:approve', '软件审批', 'package', 'approve', NOW(), NOW()),
(18, 'subscription:create', '订购申请', 'subscription', 'create', NOW(), NOW()),
(19, 'subscription:read', '订购查看', 'subscription', 'read', NOW(), NOW()),
(20, 'subscription:approve', '订购审批', 'subscription', 'approve', NOW(), NOW()),
(21, 'business-system:create', '业务系统录入', 'business-system', 'create', NOW(), NOW()),
(22, 'business-system:read', '业务系统查看', 'business-system', 'read', NOW(), NOW()),
(23, 'business-system:update', '业务系统编辑', 'business-system', 'update', NOW(), NOW()),
(24, 'business-system:approve', '业务系统审批', 'business-system', 'approve', NOW(), NOW()),
(25, 'storage:read', '存储配置查看', 'storage', 'read', NOW(), NOW()),
(26, 'storage:create', '存储配置创建', 'storage', 'create', NOW(), NOW()),
(27, 'storage:update', '存储配置编辑', 'storage', 'update', NOW(), NOW()),
(28, 'system:manage', '系统管理', 'system', 'manage', NOW(), NOW()),
(29, 'tracking:read', '使用追踪查看', 'tracking', 'read', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ========================================
-- 2. 角色表 (t_role)
-- ========================================
INSERT INTO t_role (id, role_code, role_name, description, enabled, created_at, updated_at) VALUES
(1, 'ROLE_SYSTEM_ADMIN', '系统管理员', '全部权限，包括系统管理', 1, NOW(), NOW()),
(2, 'ROLE_PACKAGE_MANAGER', '软件管理员', '管理软件包、业务系统、存储配置和订购审批', 1, NOW(), NOW()),
(3, 'ROLE_DEVELOPER', '开发人员', '可浏览和订购软件', 1, NOW(), NOW()),
(4, 'ROLE_VISITOR', '访客', '仅可浏览公开信息', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ========================================
-- 3. 角色权限关联表 (t_role_permission)
-- ========================================
-- 系统管理员：所有权限
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20),
(1, 21), (1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27), (1, 28), (1, 29)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 软件管理员：软件包、订购、业务系统、存储相关权限
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(2, 13), (2, 14), (2, 15), (2, 16), (2, 17), -- package
(2, 19), (2, 20), -- subscription
(2, 21), (2, 22), (2, 23), (2, 24), -- business-system
(2, 25) -- storage:read
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 开发人员：浏览和订购
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(3, 13), -- package:read
(3, 18), (3, 19), -- subscription
(3, 22) -- business-system:read
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 访客：仅浏览
INSERT INTO t_role_permission (role_id, permission_id) VALUES
(4, 13) -- package:read
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ========================================
-- 4. 系统管理员用户
-- 密码：admin123 (BCrypt 加密)
-- ========================================
INSERT INTO t_user (id, username, password, real_name, email, enabled, login_fail_count, created_at, updated_at) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'admin@osrm.local', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 管理员用户角色关联
INSERT INTO t_user_role (user_id, role_id) VALUES
(1, 1)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ========================================
-- 5. 开发人员测试账号
-- 密码：dev123 (BCrypt 加密)
-- ========================================
INSERT INTO t_user (id, username, password, real_name, email, enabled, login_fail_count, created_at, updated_at) VALUES
(2, 'dev1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '开发人员', 'dev1@osrm.local', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO t_user_role (user_id, role_id) VALUES
(2, 3)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ========================================
-- 6. 软件分类
-- ========================================
INSERT INTO t_category (id, category_name, description, sort_order, created_at, updated_at) VALUES
(1, '开发工具', '开发框架、库和工具', 1, NOW(), NOW()),
(2, '中间件', '消息队列、缓存、数据库等中间件', 2, NOW(), NOW()),
(3, '运维工具', '监控、日志、CI/CD 等运维相关', 3, NOW(), NOW()),
(4, '数据库', '关系型和非关系型数据库', 4, NOW(), NOW()),
(5, '前端框架', 'UI 框架和前端工具', 5, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ========================================
-- 7. 业务系统样本数据
-- ========================================
INSERT INTO t_business_system (id, system_code, system_name, domain, responsible_person, description, enabled, created_by, created_at, updated_at) VALUES
(1, 'SYS-OA', '办公自动化系统', 'BUSINESS', '张三', '企业内部 OA 系统', 1, 1, NOW(), NOW()),
(2, 'SYS-ERP', 'ERP 系统', 'BUSINESS', '李四', '企业资源规划系统', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ========================================
-- 8. 软件包样本数据
-- ========================================
INSERT INTO t_software_package (id, package_name, package_key, software_type, description, current_version, website_url, license_type, status, created_by, published_by, published_at, view_count, download_count, subscription_count, category_id, created_at, updated_at) VALUES
(1, 'Spring Boot', 'spring-boot', 'MAVEN', 'Spring Boot 是用于构建独立、生产级 Spring 应用程序的框架，内置 Tomcat，无需部署 WAR 文件。', '3.2.0', 'https://spring.io/projects/spring-boot', 'Apache-2.0', 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 10 DAY), 156, 89, 23, 1, NOW(), NOW()),
(2, 'React', 'react', 'NPM', '用于构建用户界面的 JavaScript 库，由 Meta 开源维护，采用声明式、组件化设计。', '18.2.0', 'https://react.dev', 'MIT', 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), 234, 123, 45, 5, NOW(), NOW()),
(3, 'Nginx', 'nginx', 'DOCKER_IMAGE', '高性能 HTTP 服务器和反向代理，也用作邮件代理和通用 TCP/UDP 代理。', '1.25.3', 'https://nginx.org', 'BSD-2-Clause', 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 89, 67, 12, 2, NOW(), NOW()),
(4, 'Vue.js', 'vue', 'NPM', '渐进式 JavaScript 框架，易于上手，可与其他库集成，也可用于构建复杂的单页应用。', '3.4.0', 'https://vuejs.org', 'MIT', 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 312, 198, 67, 5, NOW(), NOW()),
(5, 'Redis', 'redis', 'DOCKER_IMAGE', '开源内存数据结构存储，用作数据库、缓存和消息代理，支持多种数据结构。', '7.2.3', 'https://redis.io', 'BSD-3-Clause', 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), 178, 145, 34, 2, NOW(), NOW()),
(6, 'MyBatis-Plus', 'mybatis-plus', 'MAVEN', 'MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变，简化开发、提高效率。', '3.5.5', 'https://baomidou.com', 'Apache-2.0', 'PUBLISHED', 1, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), 123, 87, 19, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
