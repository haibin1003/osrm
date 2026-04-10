-- OSRM Database Initialization Script for MySQL
-- Version: 2.0.0
-- 与实体类字段完全匹配

SET NAMES utf8mb4;

-- 1. 初始角色
INSERT INTO t_role (role_code, role_name, description, enabled, created_at, updated_at) VALUES
    ('ROLE_VISITOR', '访客', '仅可浏览公开信息', true, NOW(), NOW()),
    ('ROLE_DEVELOPER', '开发人员', '可浏览和订购软件', true, NOW(), NOW()),
    ('ROLE_PACKAGE_MANAGER', '软件管理员', '管理软件包、业务系统、存储配置和订购审批', true, NOW(), NOW()),
    ('ROLE_SYSTEM_ADMIN', '系统管理员', '全部权限，包括系统管理', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    description = VALUES(description),
    updated_at = NOW();

-- 2. 初始权限
INSERT INTO t_permission (parent_id, permission_code, permission_name, resource_type, action, path, icon, sort_order, description, created_at, updated_at) VALUES
    -- 用户管理权限
    (NULL, 'user:read', '用户查看', 'user', 'read', '/users', 'User', 1, '查看用户列表、详情', NOW(), NOW()),
    (NULL, 'user:create', '用户创建', 'user', 'create', '/users/create', 'UserPlus', 2, '创建用户', NOW(), NOW()),
    (NULL, 'user:update', '用户编辑', 'user', 'update', '/users/update', 'UserEdit', 3, '修改用户信息', NOW(), NOW()),
    (NULL, 'user:delete', '用户删除', 'user', 'delete', '/users/delete', 'UserX', 4, '删除用户', NOW(), NOW()),
    -- 角色管理权限
    (NULL, 'role:read', '角色查看', 'role', 'read', '/roles', 'Shield', 5, '查看角色列表、详情', NOW(), NOW()),
    (NULL, 'role:create', '角色创建', 'role', 'create', '/roles/create', 'ShieldPlus', 6, '创建角色', NOW(), NOW()),
    (NULL, 'role:update', '角色编辑', 'role', 'update', '/roles/update', 'ShieldEdit', 7, '修改角色信息', NOW(), NOW()),
    (NULL, 'role:delete', '角色删除', 'role', 'delete', '/roles/delete', 'ShieldX', 8, '删除角色', NOW(), NOW()),
    -- 软件包权限
    (NULL, 'package:read', '软件浏览', 'package', 'read', '/packages', 'Package', 9, '查看软件列表、详情', NOW(), NOW()),
    (NULL, 'package:create', '软件录入', 'package', 'create', '/packages/create', 'PackagePlus', 10, '创建软件包', NOW(), NOW()),
    (NULL, 'package:update', '软件编辑', 'package', 'update', '/packages/update', 'PackageEdit', 11, '修改软件信息', NOW(), NOW()),
    (NULL, 'package:delete', '软件删除', 'package', 'delete', '/packages/delete', 'PackageX', 12, '删除软件包', NOW(), NOW()),
    (NULL, 'package:approve', '软件审批', 'package', 'approve', '/packages/approve', 'CheckCircle', 13, '审核软件发布', NOW(), NOW()),
    -- 订购权限
    (NULL, 'subscription:create', '订购申请', 'subscription', 'create', '/subscriptions/create', 'ShoppingCart', 14, '申请订购软件', NOW(), NOW()),
    (NULL, 'subscription:read', '订购查看', 'subscription', 'read', '/subscriptions', 'List', 15, '查看订购记录', NOW(), NOW()),
    (NULL, 'subscription:approve', '订购审批', 'subscription', 'approve', '/subscriptions/approve', 'Check', 16, '审批订购申请', NOW(), NOW()),
    -- 业务系统权限
    (NULL, 'business-system:create', '业务系统录入', 'business-system', 'create', '/business-systems/create', 'Building', 17, '创建业务系统', NOW(), NOW()),
    (NULL, 'business-system:read', '业务系统查看', 'business-system', 'read', '/business-systems', 'Building2', 18, '查看业务系统', NOW(), NOW()),
    (NULL, 'business-system:update', '业务系统编辑', 'business-system', 'update', '/business-systems/update', 'BuildingEdit', 19, '修改业务系统', NOW(), NOW()),
    (NULL, 'business-system:approve', '业务系统审批', 'business-system', 'approve', '/business-systems/approve', 'CheckBuilding', 20, '审批业务系统', NOW(), NOW()),
    -- 存储管理权限
    (NULL, 'storage:read', '存储配置查看', 'storage', 'read', '/storage', 'HardDrive', 21, '查看存储配置', NOW(), NOW()),
    (NULL, 'storage:create', '存储配置创建', 'storage', 'create', '/storage/create', 'HardDrivePlus', 22, '创建存储配置', NOW(), NOW()),
    (NULL, 'storage:update', '存储配置编辑', 'storage', 'update', '/storage/update', 'HardDriveEdit', 23, '修改存储配置', NOW(), NOW()),
    -- 系统管理权限
    (NULL, 'system:manage', '系统管理', 'system', 'manage', '/system', 'Settings', 24, '系统管理权限', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    updated_at = NOW();

-- 3. 角色权限关联
-- ROLE_DEVELOPER 权限
INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER'
AND p.permission_code IN ('package:read', 'subscription:create', 'subscription:read')
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ROLE_PACKAGE_MANAGER 权限
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

-- ROLE_SYSTEM_ADMIN 权限(全部权限)
INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 4. 默认管理员用户 (密码: admin123)
-- BCrypt hash: $2a$10$XB08J2X16dv3xRAB2SHBwOH/lsXG9ZpgDmWyWrZINZtTwOsWKY0mq
INSERT INTO t_user (username, password, real_name, email, enabled, login_fail_count, created_at, updated_at) VALUES
    ('admin', '$2a$10$XB08J2X16dv3xRAB2SHBwOH/lsXG9ZpgDmWyWrZINZtTwOsWKY0mq', '系统管理员', 'admin@osrm.local', true, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    real_name = VALUES(real_name),
    enabled = VALUES(enabled),
    updated_at = NOW();

-- 5. 管理员角色关联
INSERT INTO t_user_role (user_id, role_id, created_at, updated_at)
SELECT u.id, r.id, NOW(), NOW()
FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- 6. 初始分类
INSERT INTO t_category (category_code, category_name, parent_id, icon, sort_order, description, enabled, created_at, updated_at) VALUES
    ('DEV_TOOLS', '开发工具', NULL, 'Code', 1, '开发工具类软件', true, NOW(), NOW()),
    ('DATABASE', '数据库', NULL, 'Database', 2, '数据库软件', true, NOW(), NOW()),
    ('MIDDLEWARE', '中间件', NULL, 'Layers', 3, '中间件软件', true, NOW(), NOW()),
    ('OPS_TOOLS', '运维工具', NULL, 'Wrench', 4, '运维工具类软件', true, NOW(), NOW()),
    ('SECURITY', '安全工具', NULL, 'Shield', 5, '安全工具类软件', true, NOW(), NOW()),
    ('AI_ML', 'AI/机器学习', NULL, 'Brain', 6, 'AI和机器学习工具', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    updated_at = NOW();

-- 7. 初始标签
INSERT INTO t_tag (tag_name, tag_code, description, created_at) VALUES
    ('开源', 'open-source', '开源软件标签', NOW()),
    ('商业', 'commercial', '商业软件标签', NOW()),
    ('免费', 'free', '免费软件标签', NOW()),
    ('企业级', 'enterprise', '企业级软件标签', NOW()),
    ('热门', 'popular', '热门软件标签', NOW())
ON DUPLICATE KEY UPDATE
    tag_code = VALUES(tag_code),
    description = VALUES(description);

-- 8. 系统设置
INSERT INTO t_system_setting (category, setting_key, setting_value, description, created_at, updated_at) VALUES
    ('GENERAL', 'SITE_NAME', 'OSRM 开源软件仓库管理系统', '站点名称', NOW(), NOW()),
    ('GENERAL', 'SITE_LOGO', '/logo.png', '站点Logo路径', NOW(), NOW()),
    ('UPLOAD', 'MAX_UPLOAD_SIZE', '524288000', '最大上传文件大小(字节)', NOW(), NOW()),
    ('SECURITY', 'TOKEN_EXPIRE_HOURS', '24', '下载令牌过期时间(小时)', NOW(), NOW()),
    ('SECURITY', 'ENABLE_REGISTRATION', 'false', '是否开放注册', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    updated_at = NOW();

-- 9. 系统配置
INSERT INTO t_system_config (config_key, config_value, description, updated_at) VALUES
    ('default_storage_backend', '1', '默认存储后端ID', NOW()),
    ('max_file_upload_size', '524288000', '最大文件上传大小(字节)', NOW()),
    ('session_timeout', '7200', '会话超时时间(秒)', NOW())
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    updated_at = NOW();
