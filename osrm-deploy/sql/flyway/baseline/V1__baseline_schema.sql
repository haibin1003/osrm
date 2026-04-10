-- OSRM Database Schema for MySQL 8.0+
-- Version: 2.0.0
-- Generated from JPA Entities - 2026-04-10
-- 此脚本与所有实体类字段一一对应

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 用户权限模块
-- =============================================

-- User 实体映射: com.osrm.domain.user.entity.User
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(32) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(64) COMMENT '真实姓名',
    email VARCHAR(128) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    bio TEXT COMMENT '个人简介',
    avatar VARCHAR(500) COMMENT '头像URL',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    locked_until TIMESTAMP NULL COMMENT '锁定截止时间',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Role 实体映射: com.osrm.domain.user.entity.Role
DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    role_code VARCHAR(32) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(256) COMMENT '描述',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- Permission 实体映射: com.osrm.domain.user.entity.Permission
DROP TABLE IF EXISTS t_permission;
CREATE TABLE t_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    parent_id BIGINT COMMENT '父权限ID',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(20) NOT NULL COMMENT '资源类型',
    action VARCHAR(20) COMMENT '操作类型',
    path VARCHAR(500) COMMENT '资源路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    description VARCHAR(500) COMMENT '描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 用户角色关联表
DROP TABLE IF EXISTS t_user_role;
CREATE TABLE t_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色权限关联表
DROP TABLE IF EXISTS t_role_permission;
CREATE TABLE t_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 登录日志表
DROP TABLE IF EXISTS t_login_log;
CREATE TABLE t_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    login_type VARCHAR(20) NOT NULL COMMENT '登录类型',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '浏览器UA',
    login_status VARCHAR(20) NOT NULL COMMENT '登录状态',
    fail_reason VARCHAR(256) COMMENT '失败原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =============================================
-- 2. 存储管理模块
-- =============================================

-- StorageBackend 实体映射: com.osrm.domain.storage.entity.StorageBackend
DROP TABLE IF EXISTS t_storage_backend;
CREATE TABLE t_storage_backend (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '存储后端ID',
    backend_code VARCHAR(32) NOT NULL UNIQUE COMMENT '后端编码',
    backend_name VARCHAR(64) NOT NULL UNIQUE COMMENT '后端名称',
    backend_type VARCHAR(20) NOT NULL COMMENT '后端类型(HARBOR/NEXUS/MINIO)',
    endpoint VARCHAR(256) NOT NULL COMMENT '服务端点',
    access_key VARCHAR(128) COMMENT '访问密钥',
    secret_key VARCHAR(256) COMMENT '秘密密钥',
    namespace VARCHAR(64) COMMENT '命名空间',
    config_json TEXT COMMENT '扩展配置(JSON)',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否默认',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    health_status VARCHAR(20) DEFAULT 'UNKNOWN' COMMENT '健康状态',
    last_health_check TIMESTAMP NULL COMMENT '最后健康检查时间',
    error_message VARCHAR(500) COMMENT '错误信息',
    description VARCHAR(512) COMMENT '描述',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_backend_type (backend_type),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储后端表';

-- =============================================
-- 3. 业务系统模块
-- =============================================

-- BusinessSystem 实体映射: com.osrm.domain.business.entity.BusinessSystem
DROP TABLE IF EXISTS t_business_system;
CREATE TABLE t_business_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '业务系统ID',
    system_code VARCHAR(32) NOT NULL UNIQUE COMMENT '系统编码',
    system_name VARCHAR(64) NOT NULL UNIQUE COMMENT '系统名称',
    domain VARCHAR(20) NOT NULL COMMENT '业务领域枚举',
    responsible_person VARCHAR(64) COMMENT '负责人',
    description VARCHAR(512) COMMENT '描述',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain (domain),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务系统表';

-- =============================================
-- 4. 分类标签模块
-- =============================================

-- Category 实体映射: com.osrm.domain.category.entity.Category
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    category_code VARCHAR(32) NOT NULL UNIQUE COMMENT '分类编码',
    category_name VARCHAR(64) NOT NULL COMMENT '分类名称',
    parent_id BIGINT COMMENT '父分类ID',
    icon VARCHAR(64) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    description VARCHAR(256) COMMENT '描述',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_enabled (enabled),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES t_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- Tag 实体映射: com.osrm.domain.category.entity.Tag
DROP TABLE IF EXISTS t_tag;
CREATE TABLE t_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    tag_name VARCHAR(32) NOT NULL UNIQUE COMMENT '标签名称',
    tag_code VARCHAR(32) NOT NULL UNIQUE COMMENT '标签编码',
    description VARCHAR(128) COMMENT '描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- =============================================
-- 5. 软件包管理模块
-- =============================================

-- SoftwarePackage 实体映射: com.osrm.domain.software.entity.SoftwarePackage
DROP TABLE IF EXISTS t_software_package;
CREATE TABLE t_software_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '软件包ID',
    package_name VARCHAR(128) NOT NULL UNIQUE COMMENT '软件包名称',
    package_key VARCHAR(64) NOT NULL UNIQUE COMMENT '软件包唯一标识',
    software_type VARCHAR(32) NOT NULL COMMENT '软件类型(DOCKER/MAVEN/NPM等)',
    category_id BIGINT COMMENT '分类ID',
    description VARCHAR(5000) COMMENT '描述',
    website_url VARCHAR(256) COMMENT '官网URL',
    license_type VARCHAR(64) COMMENT '许可证类型',
    license_url VARCHAR(256) COMMENT '许可证URL',
    source_url VARCHAR(256) COMMENT '源码URL',
    logo_url VARCHAR(256) COMMENT 'Logo URL',
    current_version VARCHAR(32) COMMENT '当前版本',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    subscription_count INT DEFAULT 0 COMMENT '订阅次数',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT/PENDING/PUBLISHED/OFFLINE)',
    published_by BIGINT COMMENT '发布人ID',
    published_at TIMESTAMP NULL COMMENT '发布时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_software_type (software_type),
    INDEX idx_package_key (package_key),
    CONSTRAINT fk_package_category FOREIGN KEY (category_id) REFERENCES t_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件包表';

-- SoftwareVersion 实体映射: com.osrm.domain.software.entity.SoftwareVersion
DROP TABLE IF EXISTS t_software_version;
CREATE TABLE t_software_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本ID',
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    version_no VARCHAR(32) NOT NULL COMMENT '版本号',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    storage_backend_id BIGINT COMMENT '存储后端ID',
    storage_path VARCHAR(512) COMMENT '存储路径',
    artifact_url VARCHAR(512) COMMENT '制品URL',
    release_notes TEXT COMMENT '发布说明',
    file_size BIGINT COMMENT '文件大小(字节)',
    checksum VARCHAR(128) COMMENT '校验和',
    is_latest BOOLEAN DEFAULT FALSE COMMENT '是否最新版本',
    published_by BIGINT COMMENT '发布人ID',
    published_at TIMESTAMP NULL COMMENT '发布时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_version (package_id, version_no),
    INDEX idx_package_id (package_id),
    INDEX idx_status (status),
    INDEX idx_is_latest (package_id, is_latest),
    CONSTRAINT fk_version_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件版本表';

-- 软件包标签关联表
DROP TABLE IF EXISTS t_software_package_tag;
CREATE TABLE t_software_package_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_tag (package_id, tag_id),
    CONSTRAINT fk_spt_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_spt_tag FOREIGN KEY (tag_id) REFERENCES t_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件包标签关联表';

-- =============================================
-- 6. 订阅与下载模块
-- =============================================

-- Subscription 实体映射: com.osrm.domain.subscription.entity.Subscription
DROP TABLE IF EXISTS t_subscription;
CREATE TABLE t_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订阅ID',
    subscription_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订阅编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    package_id BIGINT NOT NULL COMMENT '软件包ID',
    version_id BIGINT NOT NULL COMMENT '版本ID',
    business_system_id BIGINT COMMENT '业务系统ID',
    usage_scenario VARCHAR(512) COMMENT '使用场景',
    deploy_environment VARCHAR(32) COMMENT '部署环境',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    max_renewal_count INT DEFAULT 3 COMMENT '最大续订次数',
    current_renewal_count INT DEFAULT 0 COMMENT '当前续订次数',
    approved_by BIGINT COMMENT '审批人ID',
    approved_at TIMESTAMP NULL COMMENT '审批时间',
    reject_reason VARCHAR(256) COMMENT '驳回原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅表';

-- DownloadToken 实体映射: com.osrm.domain.subscription.entity.DownloadToken
DROP TABLE IF EXISTS t_download_token;
CREATE TABLE t_download_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '令牌ID',
    subscription_id BIGINT NOT NULL COMMENT '订阅ID',
    token VARCHAR(64) NOT NULL UNIQUE COMMENT '令牌值',
    expire_at TIMESTAMP NOT NULL COMMENT '过期时间',
    max_downloads INT NOT NULL COMMENT '最大下载次数',
    used_count INT NOT NULL DEFAULT 0 COMMENT '已使用次数',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_token (token),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载令牌表';

-- =============================================
-- 7. 审批与日志模块
-- =============================================

-- ApprovalRecord 实体映射: com.osrm.domain.approval.entity.ApprovalRecord
DROP TABLE IF EXISTS t_approval_record;
CREATE TABLE t_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    target_type VARCHAR(32) NOT NULL COMMENT '目标类型(SOFTWARE_PACKAGE/SUBSCRIPTION)',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    target_name VARCHAR(128) NOT NULL COMMENT '目标名称',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(64) COMMENT '申请人名称',
    action VARCHAR(20) NOT NULL COMMENT '操作(SUBMIT/APPROVE/REJECT)',
    reason VARCHAR(512) COMMENT '原因',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(64) COMMENT '操作人名称',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target (target_type, target_id),
    INDEX idx_applicant_id (applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- 审计日志表
DROP TABLE IF EXISTS t_audit_log;
CREATE TABLE t_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(64) NOT NULL COMMENT '资源类型',
    resource_id VARCHAR(64) COMMENT '资源ID',
    operation_detail TEXT COMMENT '操作详情',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(32) COMMENT '用户名',
    ip_address VARCHAR(64) COMMENT 'IP地址',
    user_agent VARCHAR(512) COMMENT '浏览器UA',
    operation_result VARCHAR(20) COMMENT '操作结果',
    fail_reason VARCHAR(512) COMMENT '失败原因',
    execution_time_ms INT COMMENT '执行时间(毫秒)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- =============================================
-- 8. 系统配置模块
-- =============================================

-- SystemSetting 实体映射: com.osrm.domain.system.entity.SystemSetting
DROP TABLE IF EXISTS t_system_setting;
CREATE TABLE t_system_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
    setting_value TEXT COMMENT '设置值',
    description VARCHAR(500) COMMENT '描述',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表';

-- SystemConfig 实体映射: com.osrm.domain.config.entity.SystemConfig
DROP TABLE IF EXISTS t_system_config;
CREATE TABLE t_system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(64) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(512) COMMENT '配置值',
    description VARCHAR(256) COMMENT '描述',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- =============================================
-- 9. 文件上传模块
-- =============================================

-- FileUpload 实体映射: com.osrm.domain.upload.entity.FileUpload
DROP TABLE IF EXISTS t_file_upload;
CREATE TABLE t_file_upload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件ID',
    original_name VARCHAR(256) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(128) NOT NULL UNIQUE COMMENT '存储文件名',
    file_path VARCHAR(512) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    content_type VARCHAR(128) COMMENT '内容类型',
    md5_hash VARCHAR(32) COMMENT 'MD5哈希',
    related_type VARCHAR(32) COMMENT '关联类型(ARTIFACT/ATTACHMENT)',
    related_id BIGINT COMMENT '关联ID',
    uploaded_by BIGINT COMMENT '上传人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_related (related_type, related_id),
    INDEX idx_uploaded_by (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传表';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 实体与表对应关系
-- =============================================
/*
| 实体类 | 表名 | 状态 |
|--------|------|------|
| User | t_user | ✅ |
| Role | t_role | ✅ |
| Permission | t_permission | ✅ |
| StorageBackend | t_storage_backend | ✅ |
| BusinessSystem | t_business_system | ✅ |
| Category | t_category | ✅ |
| Tag | t_tag | ✅ |
| SoftwarePackage | t_software_package | ✅ |
| SoftwareVersion | t_software_version | ✅ |
| Subscription | t_subscription | ✅ |
| DownloadToken | t_download_token | ✅ |
| ApprovalRecord | t_approval_record | ✅ |
| SystemSetting | t_system_setting | ✅ |
| SystemConfig | t_system_config | ✅ |
| FileUpload | t_file_upload | ✅ |

关联表:
| t_user_role | 用户角色关联 | ✅ |
| t_role_permission | 角色权限关联 | ✅ |
| t_software_package_tag | 软件包标签关联 | ✅ |

日志表:
| t_login_log | 登录日志 | ✅ |
| t_audit_log | 审计日志 | ✅ |
*/
