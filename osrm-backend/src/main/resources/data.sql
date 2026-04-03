-- OSRM Database Initialization Script for H2/PostgreSQL
-- Version: 1.0

-- Insert initial roles (H2 compatible - using MERGE)
MERGE INTO t_role (role_code, role_name, description, enabled, created_at, updated_at)
KEY(role_code)
VALUES
    ('ROLE_VISITOR', '访客', '仅可浏览公开信息', true, NOW(), NOW()),
    ('ROLE_DEVELOPER', '开发人员', '可浏览和订购软件', true, NOW(), NOW()),
    ('ROLE_PACKAGE_MANAGER', '软件管理员', '管理软件包、业务系统、存储配置和订购审批', true, NOW(), NOW()),
    ('ROLE_SYSTEM_ADMIN', '系统管理员', '全部权限，包括系统管理', true, NOW(), NOW());

-- Insert initial permissions (H2 compatible)
MERGE INTO t_permission (permission_code, permission_name, resource_type, action, description, created_at)
KEY(permission_code)
VALUES
    -- 用户管理权限
    ('user:read', '用户查看', 'user', 'read', '查看用户列表、详情', NOW()),
    ('user:create', '用户创建', 'user', 'create', '创建用户', NOW()),
    ('user:update', '用户编辑', 'user', 'update', '修改用户信息', NOW()),
    ('user:delete', '用户删除', 'user', 'delete', '删除用户', NOW()),
    -- 角色管理权限
    ('role:read', '角色查看', 'role', 'read', '查看角色列表、详情', NOW()),
    ('role:create', '角色创建', 'role', 'create', '创建角色', NOW()),
    ('role:update', '角色编辑', 'role', 'update', '修改角色信息', NOW()),
    ('role:delete', '角色删除', 'role', 'delete', '删除角色', NOW()),
    -- 权限管理权限
    ('permission:read', '权限查看', 'permission', 'read', '查看权限列表', NOW()),
    ('permission:create', '权限创建', 'permission', 'create', '创建权限', NOW()),
    ('permission:update', '权限编辑', 'permission', 'update', '修改权限', NOW()),
    ('permission:delete', '权限删除', 'permission', 'delete', '删除权限', NOW()),
    -- 软件包权限
    ('package:read', '软件浏览', 'package', 'read', '查看软件列表、详情', NOW()),
    ('package:create', '软件录入', 'package', 'create', '创建软件包', NOW()),
    ('package:update', '软件编辑', 'package', 'update', '修改软件信息', NOW()),
    ('package:delete', '软件删除', 'package', 'delete', '删除软件包', NOW()),
    ('package:approve', '软件审批', 'package', 'approve', '审核软件发布', NOW()),
    -- 订购权限
    ('subscription:create', '订购申请', 'subscription', 'create', '申请订购软件', NOW()),
    ('subscription:read', '订购查看', 'subscription', 'read', '查看订购记录', NOW()),
    ('subscription:approve', '订购审批', 'subscription', 'approve', '审批订购申请', NOW()),
    -- 业务系统权限
    ('business-system:create', '业务系统录入', 'business-system', 'create', '创建业务系统', NOW()),
    ('business-system:read', '业务系统查看', 'business-system', 'read', '查看业务系统', NOW()),
    ('business-system:update', '业务系统编辑', 'business-system', 'update', '修改业务系统', NOW()),
    ('business-system:approve', '业务系统审批', 'business-system', 'approve', '审批业务系统', NOW()),
    -- 存储管理权限
    ('storage:read', '存储配置查看', 'storage', 'read', '查看存储配置', NOW()),
    ('storage:create', '存储配置创建', 'storage', 'create', '创建存储配置', NOW()),
    ('storage:update', '存储配置编辑', 'storage', 'update', '修改存储配置', NOW()),
    -- 系统管理权限
    ('system:manage', '系统管理', 'system', 'manage', '系统管理权限', NOW());

-- Associate ROLE_DEVELOPER permissions (H2 compatible - delete then insert)
DELETE FROM t_role_permission WHERE role_id IN (SELECT id FROM t_role WHERE role_code = 'ROLE_DEVELOPER');
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER'
AND p.permission_code IN ('package:read', 'subscription:create', 'subscription:read');

-- Associate ROLE_PACKAGE_MANAGER permissions
DELETE FROM t_role_permission WHERE role_id IN (SELECT id FROM t_role WHERE role_code = 'ROLE_PACKAGE_MANAGER');
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_PACKAGE_MANAGER'
AND p.permission_code IN (
    'package:read', 'package:create', 'package:update', 'package:delete', 'package:approve',
    'subscription:read', 'subscription:approve',
    'business-system:create', 'business-system:read', 'business-system:update', 'business-system:approve',
    'storage:read', 'storage:create', 'storage:update'
);

-- Associate ROLE_SYSTEM_ADMIN permissions (all permissions)
DELETE FROM t_role_permission WHERE role_id IN (SELECT id FROM t_role WHERE role_code = 'ROLE_SYSTEM_ADMIN');
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN';

-- Insert default admin user (password: admin123) - H2 compatible using MERGE
-- BCrypt hash: $2a$10$XB08J2X16dv3xRAB2SHBwOH/lsXG9ZpgDmWyWrZINZtTwOsWKY0mq
MERGE INTO t_user (username, password, real_name, email, enabled, login_fail_count, created_at, updated_at)
KEY(username)
VALUES ('admin', '$2a$10$XB08J2X16dv3xRAB2SHBwOH/lsXG9ZpgDmWyWrZINZtTwOsWKY0mq', '系统管理员', 'admin@osrm.local', true, 0, NOW(), NOW());

-- Associate admin user with ROLE_SYSTEM_ADMIN (H2 compatible)
DELETE FROM t_user_role WHERE user_id IN (SELECT id FROM t_user WHERE username = 'admin');
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id
FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_SYSTEM_ADMIN';

-- Insert sample categories (H2 compatible)
MERGE INTO t_category (category_code, category_name, sort_order, enabled, created_at, updated_at)
KEY(category_code)
VALUES
    ('DEV_TOOLS', '开发工具', 1, true, NOW(), NOW()),
    ('DATABASE', '数据库', 2, true, NOW(), NOW()),
    ('MIDDLEWARE', '中间件', 3, true, NOW(), NOW()),
    ('OPS_TOOLS', '运维工具', 4, true, NOW(), NOW()),
    ('SECURITY', '安全工具', 5, true, NOW(), NOW()),
    ('AI_ML', 'AI/机器学习', 6, true, NOW(), NOW());

-- Insert sample domains (H2 compatible)
MERGE INTO t_domain (domain_code, domain_name, sort_order, enabled, created_at, updated_at)
KEY(domain_code)
VALUES
    ('ECOMMERCE', '电商域', 1, true, NOW(), NOW()),
    ('PAYMENT', '支付域', 2, true, NOW(), NOW()),
    ('LOGISTICS', '物流域', 3, true, NOW(), NOW()),
    ('USER_CENTER', '用户中心', 4, true, NOW(), NOW()),
    ('DATA_PLATFORM', '数据平台', 5, true, NOW(), NOW());
