-- ============================================
-- OSRM 数据库初始化脚本
-- 版本: v1.0
-- 创建时间: 2026-03-18
-- ============================================

-- 创建数据库 (如果执行用户有权限)
-- CREATE DATABASE IF NOT EXISTS osrm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE osrm;

-- ============================================
-- 1. 用户中心模块表结构
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT 'BCrypt加密密码',
    real_name VARCHAR(64) COMMENT '真实姓名',
    email VARCHAR(128) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    locked_until TIMESTAMP COMMENT '锁定截止时间',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    last_login_at TIMESTAMP COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(256) COMMENT '角色描述',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    permission_code VARCHAR(64) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型',
    action VARCHAR(32) NOT NULL COMMENT '操作类型',
    description VARCHAR(256) COMMENT '权限描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id),
    CONSTRAINT fk_role_permission_role_id FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission_id FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS t_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    login_type VARCHAR(20) NOT NULL COMMENT '登录类型 (LOGIN/LOGOUT/REFRESH)',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    login_status VARCHAR(20) NOT NULL COMMENT '登录状态 (SUCCESS/FAIL)',
    fail_reason VARCHAR(256) COMMENT '失败原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_username (username),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ============================================
-- 2. 存储管理模块表结构
-- ============================================

-- 存储后端配置表
CREATE TABLE IF NOT EXISTS t_storage_backend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    backend_code VARCHAR(32) NOT NULL COMMENT '后端编码 (HARBOR/NEXUS/MINIO)',
    backend_name VARCHAR(64) NOT NULL COMMENT '后端名称',
    backend_type VARCHAR(20) NOT NULL COMMENT '后端类型 (DOCKER/MAVEN/NPM/GENERIC)',
    endpoint VARCHAR(256) NOT NULL COMMENT '服务端点 URL',
    access_key VARCHAR(128) COMMENT '访问密钥 (加密存储)',
    secret_key VARCHAR(256) COMMENT '密钥 (加密存储)',
    namespace VARCHAR(64) COMMENT '命名空间/项目',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为默认存储',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    health_status VARCHAR(20) DEFAULT 'UNKNOWN' COMMENT '健康状态 (HEALTHY/UNHEALTHY/UNKNOWN)',
    last_health_check TIMESTAMP COMMENT '最后健康检查时间',
    description VARCHAR(512) COMMENT '描述',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_backend_code (backend_code),
    KEY idx_backend_type (backend_type),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储后端配置表';

-- ============================================
-- 3. 业务系统管理模块表结构
-- ============================================

-- 归属域表
CREATE TABLE IF NOT EXISTS t_domain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    domain_code VARCHAR(32) NOT NULL COMMENT '域编码',
    domain_name VARCHAR(64) NOT NULL COMMENT '域名称',
    parent_id BIGINT COMMENT '父域ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_domain_code (domain_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归属域表';

-- 业务系统表
CREATE TABLE IF NOT EXISTS t_business_system (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_code VARCHAR(32) NOT NULL COMMENT '系统编码',
    system_name VARCHAR(128) NOT NULL COMMENT '系统名称',
    domain_id BIGINT COMMENT '归属域ID',
    owner_name VARCHAR(64) COMMENT '负责人姓名',
    owner_email VARCHAR(128) COMMENT '负责人邮箱',
    description VARCHAR(512) COMMENT '系统描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 (PENDING/APPROVED/REJECTED/DISABLED)',
    approved_by BIGINT COMMENT '审批人ID',
    approved_at TIMESTAMP COMMENT '审批时间',
    reject_reason VARCHAR(256) COMMENT '驳回原因',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_code (system_code),
    KEY idx_domain_id (domain_id),
    KEY idx_status (status),
    KEY idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务系统表';

-- ============================================
-- 4. 软件管理模块表结构
-- ============================================

-- 软件分类表
CREATE TABLE IF NOT EXISTS t_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category_code VARCHAR(32) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    parent_id BIGINT COMMENT '父分类ID',
    icon VARCHAR(64) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_category_code (category_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件分类表';

-- 软件包表
CREATE TABLE IF NOT EXISTS t_software_package (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    package_name VARCHAR(128) NOT NULL COMMENT '软件名称',
    package_key VARCHAR(64) NOT NULL COMMENT '软件标识 (如 org.springframework.boot)',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    description TEXT COMMENT '软件描述',
    website_url VARCHAR(256) COMMENT '官网URL',
    license_type VARCHAR(64) COMMENT '许可证类型',
    license_url VARCHAR(256) COMMENT '许可证URL',
    source_url VARCHAR(256) COMMENT '源码地址',
    logo_url VARCHAR(256) COMMENT 'Logo URL',
    current_version VARCHAR(32) COMMENT '当前版本',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    subscription_count INT DEFAULT 0 COMMENT '订购次数',
    rating_avg DECIMAL(2,1) DEFAULT 5.0 COMMENT '平均评分',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 (DRAFT/PENDING/PUBLISHED/REJECTED/DEPRECATED/ARCHIVED)',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_package_key (package_key),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件包表';

-- 软件版本表
CREATE TABLE IF NOT EXISTS t_software_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    version_no VARCHAR(32) NOT NULL COMMENT '版本号',
    version_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态 (DRAFT/PENDING/PUBLISHED/REJECTED/DEPRECATED/ARCHIVED)',
    storage_backend_id BIGINT NOT NULL COMMENT '存储后端ID',
    storage_path VARCHAR(512) COMMENT '存储路径',
    artifact_url VARCHAR(512) COMMENT '制品URL',
    release_notes TEXT COMMENT '发布说明',
    changelog TEXT COMMENT '变更日志',
    is_latest BOOLEAN DEFAULT FALSE COMMENT '是否为最新版本',
    is_lts BOOLEAN DEFAULT FALSE COMMENT '是否为长期支持版本',
    published_by BIGINT COMMENT '发布人ID',
    published_at TIMESTAMP COMMENT '发布时间',
    deprecated_by BIGINT COMMENT '下架人ID',
    deprecated_at TIMESTAMP COMMENT '下架时间',
    deprecate_reason VARCHAR(256) COMMENT '下架原因',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_package_version (package_id, version_no),
    KEY idx_package_id (package_id),
    KEY idx_version_status (version_status),
    KEY idx_storage_backend_id (storage_backend_id),
    CONSTRAINT fk_version_package_id FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件版本表';

-- 软件文档表
CREATE TABLE IF NOT EXISTS t_software_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    doc_type VARCHAR(32) NOT NULL COMMENT '文档类型 (TECHNICAL/SECURITY/LICENSE/OTHER)',
    doc_name VARCHAR(128) NOT NULL COMMENT '文档名称',
    file_url VARCHAR(512) NOT NULL COMMENT '文件URL',
    file_size BIGINT COMMENT '文件大小(字节)',
    mime_type VARCHAR(64) COMMENT 'MIME类型',
    uploaded_by BIGINT COMMENT '上传人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_package_id (package_id),
    KEY idx_doc_type (doc_type),
    CONSTRAINT fk_doc_package_id FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件文档表';

-- ============================================
-- 5. 订购管理模块表结构
-- ============================================

-- 订购记录表
CREATE TABLE IF NOT EXISTS t_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    subscription_no VARCHAR(32) NOT NULL COMMENT '订购编号',
    user_id BIGINT NOT NULL COMMENT '订购用户ID',
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    version_id BIGINT NOT NULL COMMENT '版本ID',
    business_system_id BIGINT COMMENT '业务系统ID',
    usage_scenario VARCHAR(512) COMMENT '使用场景描述',
    deploy_environment VARCHAR(32) COMMENT '部署环境 (DEV/TEST/STAGING/PROD)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 (PENDING/APPROVED/REJECTED/ACTIVE/EXPIRED/CANCELLED)',
    start_date DATE COMMENT '生效日期',
    end_date DATE COMMENT '到期日期',
    max_renewal_count INT DEFAULT 3 COMMENT '最大续期次数',
    current_renewal_count INT DEFAULT 0 COMMENT '当前续期次数',
    approved_by BIGINT COMMENT '审批人ID',
    approved_at TIMESTAMP COMMENT '审批时间',
    reject_reason VARCHAR(256) COMMENT '驳回原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_subscription_no (subscription_no),
    KEY idx_user_id (user_id),
    KEY idx_package_id (package_id),
    KEY idx_status (status),
    KEY idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订购记录表';

-- 下载令牌表
CREATE TABLE IF NOT EXISTS t_download_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    token_value VARCHAR(256) NOT NULL COMMENT '令牌值',
    subscription_id BIGINT NOT NULL COMMENT '订购ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    version_id BIGINT NOT NULL COMMENT '版本ID',
    storage_backend_id BIGINT NOT NULL COMMENT '存储后端ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 (ACTIVE/REVOKED/EXPIRED)',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    last_used_at TIMESTAMP COMMENT '最后使用时间',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_token_value (token_value),
    KEY idx_subscription_id (subscription_id),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载令牌表';

-- ============================================
-- 6. 审计日志表
-- ============================================

-- 操作审计日志表
CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(64) NOT NULL COMMENT '资源类型',
    resource_id VARCHAR(64) COMMENT '资源ID',
    operation_detail TEXT COMMENT '操作详情 (JSON)',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(32) COMMENT '操作用户名',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '用户代理',
    operation_result VARCHAR(20) COMMENT '操作结果 (SUCCESS/FAIL)',
    fail_reason VARCHAR(512) COMMENT '失败原因',
    execution_time_ms INT COMMENT '执行时间(毫秒)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_resource (resource_type, resource_id),
    KEY idx_operation_type (operation_type),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

-- ============================================
-- 7. 初始化数据
-- ============================================

-- 插入初始角色
INSERT INTO t_role (role_code, role_name, description) VALUES
('ROLE_VISITOR', '访客', '仅可浏览公开信息'),
('ROLE_DEVELOPER', '开发人员', '可浏览和订购软件'),
('ROLE_PACKAGE_MANAGER', '软件管理员', '管理软件包、业务系统、存储配置和订购审批'),
('ROLE_SYSTEM_ADMIN', '系统管理员', '全部权限，包括系统管理')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- 插入初始权限
INSERT INTO t_permission (permission_code, permission_name, resource_type, action) VALUES
-- 用户管理权限
('user:read', '用户查看', 'user', 'read'),
('user:create', '用户创建', 'user', 'create'),
('user:update', '用户编辑', 'user', 'update'),
('user:delete', '用户删除', 'user', 'delete'),
-- 角色管理权限
('role:read', '角色查看', 'role', 'read'),
('role:create', '角色创建', 'role', 'create'),
('role:update', '角色编辑', 'role', 'update'),
('role:delete', '角色删除', 'role', 'delete'),
-- 权限管理权限
('permission:read', '权限查看', 'permission', 'read'),
('permission:create', '权限创建', 'permission', 'create'),
('permission:update', '权限编辑', 'permission', 'update'),
('permission:delete', '权限删除', 'permission', 'delete'),
-- 软件包权限
('package:read', '软件浏览', 'package', 'read'),
('package:create', '软件录入', 'package', 'create'),
('package:update', '软件编辑', 'package', 'update'),
('package:delete', '软件删除', 'package', 'delete'),
('package:approve', '软件审批', 'package', 'approve'),
-- 订购权限
('subscription:create', '订购申请', 'subscription', 'create'),
('subscription:read', '订购查看', 'subscription', 'read'),
('subscription:approve', '订购审批', 'subscription', 'approve'),
-- 业务系统权限
('business-system:create', '业务系统录入', 'business-system', 'create'),
('business-system:read', '业务系统查看', 'business-system', 'read'),
('business-system:update', '业务系统编辑', 'business-system', 'update'),
('business-system:approve', '业务系统审批', 'business-system', 'approve'),
-- 存储管理权限
('storage:read', '存储配置查看', 'storage', 'read'),
('storage:create', '存储配置创建', 'storage', 'create'),
('storage:update', '存储配置编辑', 'storage', 'update'),
-- 系统管理权限
('system:manage', '系统管理', 'system', 'manage')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 关联角色权限
-- ROLE_DEVELOPER: package:read, subscription:create, subscription:read
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER' AND p.permission_code IN ('package:read', 'subscription:create', 'subscription:read')
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ROLE_PACKAGE_MANAGER: 软件包全部权限 + 订购审批 + 业务系统全部权限 + 存储查看
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_PACKAGE_MANAGER'
AND p.permission_code IN (
    'package:read', 'package:create', 'package:update', 'package:delete', 'package:approve',
    'subscription:read', 'subscription:approve',
    'business-system:create', 'business-system:read', 'business-system:update', 'business-system:approve',
    'storage:read'
)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ROLE_SYSTEM_ADMIN: 全部权限
INSERT INTO t_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 插入默认管理员用户 (密码: admin123)
-- BCrypt hash for 'admin123': $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO
INSERT INTO t_user (username, password, real_name, email, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'admin@osrm.local', true)
ON DUPLICATE KEY UPDATE password = VALUES(password);

-- 关联管理员角色
INSERT INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- 插入示例分类数据
INSERT INTO t_category (category_code, category_name, sort_order) VALUES
('DEV_TOOLS', '开发工具', 1),
('DATABASE', '数据库', 2),
('MIDDLEWARE', '中间件', 3),
('OPS_TOOLS', '运维工具', 4),
('SECURITY', '安全工具', 5),
('AI_ML', 'AI/机器学习', 6)
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

-- 插入示例归属域数据
INSERT INTO t_domain (domain_code, domain_name, sort_order) VALUES
('ECOMMERCE', '电商域', 1),
('PAYMENT', '支付域', 2),
('LOGISTICS', '物流域', 3),
('USER_CENTER', '用户中心', 4),
('DATA_PLATFORM', '数据平台', 5)
ON DUPLICATE KEY UPDATE domain_name = VALUES(domain_name);

-- ============================================
-- 8. 注释说明
-- ============================================

-- 初始账号信息:
-- 用户名: admin
-- 密码: admin123
-- 角色: 系统管理员

-- 初始角色说明:
-- ROLE_VISITOR: 访客，仅可浏览
-- ROLE_DEVELOPER: 开发人员，可浏览和订购
-- ROLE_PACKAGE_MANAGER: 软件管理员，管理功能
-- ROLE_SYSTEM_ADMIN: 系统管理员，全部权限
