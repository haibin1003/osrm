-- OSRM Database Initialization Script for MySQL
-- Version: 1.0.0
-- 使用 MySQL 兼容语法

SET NAMES utf8mb4;

-- Insert initial roles
INSERT INTO t_role (role_code, role_name, description, enabled, created_at, updated_at) VALUES
    ('ROLE_VISITOR', '访客', '仅可浏览公开信息', true, NOW(), NOW()),
    ('ROLE_DEVELOPER', '开发人员', '可浏览和订购软件', true, NOW(), NOW()),
    ('ROLE_PACKAGE_MANAGER', '软件管理员', '管理软件包、业务系统、存储配置和订购审批', true, NOW(), NOW()),
    ('ROLE_SYSTEM_ADMIN', '系统管理员', '全部权限，包括系统管理', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    updated_at = NOW();

-- Insert initial permissions
INSERT INTO t_permission (permission_code, permission_name, resource_type, action, description, created_at, updated_at) VALUES
    -- 用户管理权限
    ('user:read', '用户查看', 'user', 'read', '查看用户列表、详情', NOW(), NOW()),
    ('user:create', '用户创建', 'user', 'create', '创建用户', NOW(), NOW()),
    ('user:update', '用户编辑', 'user', 'update', '修改用户信息', NOW(), NOW()),
    ('user:delete', '用户删除', 'user', 'delete', '删除用户', NOW(), NOW()),
    -- 角色管理权限
    ('role:read', '角色查看', 'role', 'read', '查看角色列表、详情', NOW(), NOW()),
    ('role:create', '角色创建', 'role', 'create', '创建角色', NOW(), NOW()),
    ('role:update', '角色编辑', 'role', 'update', '修改角色信息', NOW(), NOW()),
    ('role:delete', '角色删除', 'role', 'delete', '删除角色', NOW(), NOW()),
    -- 权限管理权限
    ('permission:read', '权限查看', 'permission', 'read', '查看权限列表', NOW(), NOW()),
    ('permission:create', '权限创建', 'permission', 'create', '创建权限', NOW(), NOW()),
    ('permission:update', '权限编辑', 'permission', 'update', '修改权限', NOW(), NOW()),
    ('permission:delete', '权限删除', 'permission', 'delete', '删除权限', NOW(), NOW()),
    -- 软件包权限
    ('package:read', '软件浏览', 'package', 'read', '查看软件列表、详情', NOW(), NOW()),
    ('package:create', '软件录入', 'package', 'create', '创建软件包', NOW(), NOW()),
    ('package:update', '软件编辑', 'package', 'update', '修改软件信息', NOW(), NOW()),
    ('package:delete', '软件删除', 'package', 'delete', '删除软件包', NOW(), NOW()),
    ('package:approve', '软件审批', 'package', 'approve', '审核软件发布', NOW(), NOW()),
    -- 订购权限
    ('subscription:create', '订购申请', 'subscription', 'create', '申请订购软件', NOW(), NOW()),
    ('subscription:read', '订购查看', 'subscription', 'read', '查看订购记录', NOW(), NOW()),
    ('subscription:approve', '订购审批', 'subscription', 'approve', '审批订购申请', NOW(), NOW()),
    -- 业务系统权限
    ('business-system:create', '业务系统录入', 'business-system', 'create', '创建业务系统', NOW(), NOW()),
    ('business-system:read', '业务系统查看', 'business-system', 'read', '查看业务系统', NOW(), NOW()),
    ('business-system:update', '业务系统编辑', 'business-system', 'update', '修改业务系统', NOW(), NOW()),
    ('business-system:approve', '业务系统审批', 'business-system', 'approve', '审批业务系统', NOW(), NOW()),
    -- 存储管理权限
    ('storage:read', '存储配置查看', 'storage', 'read', '查看存储配置', NOW(), NOW()),
    ('storage:create', '存储配置创建', 'storage', 'create', '创建存储配置', NOW(), NOW()),
    ('storage:update', '存储配置编辑', 'storage', 'update', '修改存储配置', NOW(), NOW()),
    -- 系统管理权限
    ('system:manage', '系统管理', 'system', 'manage', '系统管理权限', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    updated_at = NOW();

-- Associate ROLE_DEVELOPER permissions
INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER'
AND p.permission_code IN ('package:read', 'subscription:create', 'subscription:read')
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Associate ROLE_PACKAGE_MANAGER permissions
INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_PACKAGE_MANAGER'
AND p.permission_code IN (
    'package:read', 'package:create', 'package:update', 'package:delete', 'package:approve',
    'subscription:read', 'subscription:approve',
    'business-system:create', 'business-system:read', 'business-system:update', 'business-system:approve',
    'storage:read', 'storage:create', 'storage:update'
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Associate ROLE_SYSTEM_ADMIN permissions (all permissions)
INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Insert default admin user (password: admin123)
-- BCrypt hash: $2a$10$XB08J2X16dv3xRAB2SHBwOH/lsXG9ZpgDmWyWrZINZtTwOsWKY0mq
INSERT INTO t_user (username, password, real_name, email, enabled, login_fail_count, created_at, updated_at) VALUES
    ('admin', '$2a$10$XB08J2X16dv3xRAB2SHBwOH/lsXG9ZpgDmWyWrZINZtTwOsWKY0mq', '系统管理员', 'admin@osrm.local', true, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    real_name = VALUES(real_name),
    enabled = VALUES(enabled),
    updated_at = NOW();

-- Associate admin user with ROLE_SYSTEM_ADMIN
INSERT INTO t_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, NOW(), NOW()
FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Insert sample categories
INSERT INTO t_category (category_code, category_name, sort_order, enabled, created_at, updated_at) VALUES
    ('DEV_TOOLS', '开发工具', 1, true, NOW(), NOW()),
    ('DATABASE', '数据库', 2, true, NOW(), NOW()),
    ('MIDDLEWARE', '中间件', 3, true, NOW(), NOW()),
    ('OPS_TOOLS', '运维工具', 4, true, NOW(), NOW()),
    ('SECURITY', '安全工具', 5, true, NOW(), NOW()),
    ('AI_ML', 'AI/机器学习', 6, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    updated_at = NOW();

-- Insert sample domains
INSERT INTO t_domain (domain_code, domain_name, sort_order, enabled, created_at, updated_at) VALUES
    ('ECOMMERCE', '电商域', 1, true, NOW(), NOW()),
    ('PAYMENT', '支付域', 2, true, NOW(), NOW()),
    ('LOGISTICS', '物流域', 3, true, NOW(), NOW()),
    ('USER_CENTER', '用户中心', 4, true, NOW(), NOW()),
    ('DATA_PLATFORM', '数据平台', 5, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    domain_name = VALUES(domain_name),
    updated_at = NOW();

-- Insert sample tags
INSERT INTO t_tag (tag_name, tag_color, sort_order, enabled, created_at, updated_at) VALUES
    ('开源', '#22c55e', 1, true, NOW(), NOW()),
    ('商业', '#f59e0b', 2, true, NOW(), NOW()),
    ('免费', '#3b82f6', 3, true, NOW(), NOW()),
    ('企业级', '#8b5cf6', 4, true, NOW(), NOW()),
    ('热门', '#ef4444', 5, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    tag_color = VALUES(tag_color),
    updated_at = NOW();

-- Insert system settings
INSERT INTO t_system_setting (setting_key, setting_value, setting_type, description, created_at, updated_at) VALUES
    ('SITE_NAME', 'OSRM 开源软件仓库管理系统', 'STRING', '站点名称', NOW(), NOW()),
    ('SITE_LOGO', '/logo.png', 'STRING', '站点Logo路径', NOW(), NOW()),
    ('MAX_UPLOAD_SIZE', '524288000', 'NUMBER', '最大上传文件大小(字节)', NOW(), NOW()),
    ('TOKEN_EXPIRE_HOURS', '24', 'NUMBER', '下载令牌过期时间(小时)', NOW(), NOW()),
    ('ENABLE_REGISTRATION', 'false', 'BOOLEAN', '是否开放注册', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    updated_at = NOW();
