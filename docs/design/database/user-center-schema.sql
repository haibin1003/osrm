-- 用户中心数据库设计
-- 执行此脚本前请确保已执行基础表结构

-- ============================================
-- 1. 扩展用户表
-- ============================================
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS avatar VARCHAR(500);
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT FALSE;

-- ============================================
-- 2. 角色表
-- ============================================
CREATE TABLE IF NOT EXISTS t_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_role IS '角色表';
COMMENT ON COLUMN t_role.role_code IS '角色编码，如ROLE_ADMIN';
COMMENT ON COLUMN t_role.role_name IS '角色名称';

-- ============================================
-- 3. 权限表
-- ============================================
CREATE TABLE IF NOT EXISTS t_permission (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES t_permission(id),
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) NOT NULL, -- menu/button/api/data
    action VARCHAR(20), -- read/create/update/delete/export/import
    path VARCHAR(500),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE t_permission IS '权限表';
COMMENT ON COLUMN t_permission.resource_type IS '资源类型：menu菜单、button按钮、api接口、data数据';
COMMENT ON COLUMN t_permission.action IS '操作类型：read查看、create新增、update编辑、delete删除、export导出、import导入';

-- ============================================
-- 4. 角色权限关联表
-- ============================================
CREATE TABLE IF NOT EXISTS t_role_permission (
    role_id BIGINT NOT NULL REFERENCES t_role(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES t_permission(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

COMMENT ON TABLE t_role_permission IS '角色权限关联表';

-- ============================================
-- 5. 用户角色关联表（用于替换t_user的roles字段）
-- ============================================
CREATE TABLE IF NOT EXISTS t_user_role (
    user_id BIGINT NOT NULL REFERENCES t_user(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES t_role(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

COMMENT ON TABLE t_user_role IS '用户角色关联表';

-- ============================================
-- 6. 系统设置表
-- ============================================
CREATE TABLE IF NOT EXISTS t_system_setting (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (category, setting_key)
);

COMMENT ON TABLE t_system_setting IS '系统设置表';
COMMENT ON COLUMN t_system_setting.category IS '设置分类：basic基本、security安全、notification通知、storage存储、log日志';

-- ============================================
-- 7. 初始化数据
-- ============================================

-- 初始化角色
INSERT INTO t_role (role_code, role_name, description) VALUES
('ROLE_ADMIN', '系统管理员', '拥有系统所有权限'),
('ROLE_SOFTWARE_ADMIN', '软件管理员', '管理软件包和版本'),
('ROLE_DEVELOPER', '开发人员', '可以查看和订购软件'),
('ROLE_GUEST', '访客', '只读访问权限')
ON CONFLICT (role_code) DO NOTHING;

-- 初始化权限（树形结构）
-- 系统管理
INSERT INTO t_permission (id, permission_code, permission_name, resource_type, sort_order) VALUES
(1, 'system', '系统管理', 'menu', 1)
ON CONFLICT (permission_code) DO NOTHING;

-- 用户管理
INSERT INTO t_permission (id, parent_id, permission_code, permission_name, resource_type, path, icon, sort_order) VALUES
(11, 1, 'system:user', '用户管理', 'menu', '/system/users', 'User', 1),
(111, 11, 'user:read', '查看用户', 'button', 'read', null, 1),
(112, 11, 'user:create', '新增用户', 'button', 'create', null, 2),
(113, 11, 'user:update', '编辑用户', 'button', 'update', null, 3),
(114, 11, 'user:delete', '删除用户', 'button', 'delete', null, 4)
ON CONFLICT (permission_code) DO NOTHING;

-- 角色管理
INSERT INTO t_permission (id, parent_id, permission_code, permission_name, resource_type, path, icon, sort_order) VALUES
(12, 1, 'system:role', '角色管理', 'menu', '/system/roles', 'UserFilled', 2),
(121, 12, 'role:read', '查看角色', 'button', 'read', null, 1),
(122, 12, 'role:create', '新增角色', 'button', 'create', null, 2),
(123, 12, 'role:update', '编辑角色', 'button', 'update', null, 3),
(124, 12, 'role:delete', '删除角色', 'button', 'delete', null, 4)
ON CONFLICT (permission_code) DO NOTHING;

-- 权限管理
INSERT INTO t_permission (id, parent_id, permission_code, permission_name, resource_type, path, icon, sort_order) VALUES
(13, 1, 'system:permission', '权限管理', 'menu', '/system/permissions', 'Key', 3),
(131, 13, 'permission:read', '查看权限', 'button', 'read', null, 1),
(132, 13, 'permission:create', '新增权限', 'button', 'create', null, 2),
(133, 13, 'permission:update', '编辑权限', 'button', 'update', null, 3),
(134, 13, 'permission:delete', '删除权限', 'button', 'delete', null, 4)
ON CONFLICT (permission_code) DO NOTHING;

-- 软件管理
INSERT INTO t_permission (id, permission_code, permission_name, resource_type, path, icon, sort_order) VALUES
(2, 'software', '软件管理', 'menu', '/software', 'Box', 2)
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO t_permission (id, parent_id, permission_code, permission_name, resource_type, sort_order) VALUES
(21, 2, 'software:read', '查看软件', 'button', 1),
(22, 2, 'software:create', '新增软件', 'button', 2),
(23, 2, 'software:update', '编辑软件', 'button', 3),
(24, 2, 'software:delete', '删除软件', 'button', 4)
ON CONFLICT (permission_code) DO NOTHING;

-- 给角色分配权限
-- 系统管理员拥有所有权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r, t_permission p WHERE r.role_code = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- 软件管理员
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SOFTWARE_ADMIN'
AND p.permission_code IN ('software:read', 'software:create', 'software:update', 'software:delete')
ON CONFLICT DO NOTHING;

-- 给admin用户分配系统管理员角色
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- ============================================
-- 8. 创建索引
-- ============================================
CREATE INDEX IF NOT EXISTS idx_permission_parent ON t_permission(parent_id);
CREATE INDEX IF NOT EXISTS idx_permission_resource_type ON t_permission(resource_type);
CREATE INDEX IF NOT EXISTS idx_role_permission_role ON t_role_permission(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permission_permission ON t_role_permission(permission_id);
CREATE INDEX IF NOT EXISTS idx_user_role_user ON t_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role ON t_user_role(role_id);
CREATE INDEX IF NOT EXISTS idx_system_setting_category ON t_system_setting(category);
