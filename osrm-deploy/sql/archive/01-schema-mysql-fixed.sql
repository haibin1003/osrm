-- OSRM Database Schema for MySQL 8.0+
-- Version: 1.1.0
-- 修复版本：解决SQL脚本与JPA实体类字段不一致问题
-- 审查日期: 2026-04-10

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================
-- 1. 用户权限模块
-- =============================================

-- User table
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(20),
    bio TEXT,
    avatar VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMP NULL,
    login_fail_count INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Role table
DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- Permission table
DROP TABLE IF EXISTS t_permission;
CREATE TABLE t_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL UNIQUE,
    permission_name VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- User-Role association table
DROP TABLE IF EXISTS t_user_role;
CREATE TABLE t_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- Role-Permission association table
DROP TABLE IF EXISTS t_role_permission;
CREATE TABLE t_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- Login log table
DROP TABLE IF EXISTS t_login_log;
CREATE TABLE t_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(32) NOT NULL,
    login_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    login_status VARCHAR(20) NOT NULL,
    fail_reason VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =============================================
-- 2. 存储管理模块
-- =============================================

-- Storage backend table
DROP TABLE IF EXISTS t_storage_backend;
CREATE TABLE t_storage_backend (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backend_code VARCHAR(32) NOT NULL UNIQUE,
    backend_name VARCHAR(64) NOT NULL UNIQUE,
    backend_type VARCHAR(20) NOT NULL,
    endpoint VARCHAR(256) NOT NULL,
    access_key VARCHAR(128),
    secret_key VARCHAR(256),
    namespace VARCHAR(64),
    config_json TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',
    last_health_check TIMESTAMP NULL,
    error_message VARCHAR(500),
    description VARCHAR(512),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储后端表';

-- =============================================
-- 3. 业务系统模块（与实体对齐版本）
-- =============================================

-- Domain table（领域表）
-- 注意：实体BusinessSystem使用的是BusinessDomain枚举，但SQL设计了独立的领域表
-- 这里提供两种方案：

-- 方案A：使用独立的Domain表（推荐，符合SQL设计）
DROP TABLE IF EXISTS t_domain;
CREATE TABLE t_domain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain_code VARCHAR(32) NOT NULL UNIQUE,
    domain_name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务领域表';

-- Business system table（修正版 - 与实体对齐）
DROP TABLE IF EXISTS t_business_system;
CREATE TABLE t_business_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_code VARCHAR(32) NOT NULL UNIQUE,
    system_name VARCHAR(64) NOT NULL UNIQUE,
    domain VARCHAR(20) NOT NULL COMMENT '业务领域枚举：FINANCE/OPERATIONS/TECHNOLOGY/MARKETING',
    responsible_person VARCHAR(64) COMMENT '负责人姓名',
    description VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务系统表（与实体对齐版）';

-- =============================================
-- 4. 软件包管理模块
-- =============================================

-- Category table（修正版 - 添加description字段）
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code VARCHAR(32) NOT NULL UNIQUE,
    category_name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    icon VARCHAR(64),
    sort_order INT DEFAULT 0,
    description VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件分类表';

-- Tag table（修正版 - 添加tag_code字段，移除多余字段）
DROP TABLE IF EXISTS t_tag;
CREATE TABLE t_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(32) NOT NULL UNIQUE,
    tag_code VARCHAR(32) NOT NULL UNIQUE,
    description VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- Software package table（修正版 - 调整约束与实体一致）
DROP TABLE IF EXISTS t_software_package;
CREATE TABLE t_software_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL UNIQUE,
    package_key VARCHAR(64) NOT NULL UNIQUE,
    software_type VARCHAR(32) NOT NULL,
    category_id BIGINT,
    description VARCHAR(5000),
    website_url VARCHAR(256),
    license_type VARCHAR(64),
    license_url VARCHAR(256),
    source_url VARCHAR(256),
    logo_url VARCHAR(256),
    current_version VARCHAR(32),
    view_count INT DEFAULT 0,
    download_count INT DEFAULT 0,
    subscription_count INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_by BIGINT,
    published_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_package_category FOREIGN KEY (category_id) REFERENCES t_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件包表';

-- Software package tags association
DROP TABLE IF EXISTS t_software_package_tag;
CREATE TABLE t_software_package_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_package_tag (package_id, tag_id),
    CONSTRAINT fk_package_tag_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE,
    CONSTRAINT fk_package_tag_tag FOREIGN KEY (tag_id) REFERENCES t_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件包标签关联表';

-- Software version table（修正版 - 添加file_size字段，对齐实体）
DROP TABLE IF EXISTS t_software_version;
CREATE TABLE t_software_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    version_no VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    storage_backend_id BIGINT,
    storage_path VARCHAR(512),
    artifact_url VARCHAR(512),
    release_notes TEXT,
    file_size BIGINT COMMENT '文件大小（字节）',
    checksum VARCHAR(128),
    is_latest BOOLEAN DEFAULT FALSE,
    published_by BIGINT,
    published_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_version_package_version_no (package_id, version_no),
    CONSTRAINT fk_version_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件版本表（与实体对齐版）';

-- Software document table
DROP TABLE IF EXISTS t_software_document;
CREATE TABLE t_software_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    doc_type VARCHAR(32) NOT NULL,
    doc_name VARCHAR(128) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(64),
    uploaded_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='软件文档表';

-- =============================================
-- 5. 订阅与下载模块
-- =============================================

-- Subscription table
DROP TABLE IF EXISTS t_subscription;
CREATE TABLE t_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    business_system_id BIGINT,
    usage_scenario VARCHAR(512),
    deploy_environment VARCHAR(32),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_date DATE,
    end_date DATE,
    max_renewal_count INT DEFAULT 3,
    current_renewal_count INT DEFAULT 0,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    reject_reason VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订阅表';

-- Download token table
DROP TABLE IF EXISTS t_download_token;
CREATE TABLE t_download_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_value VARCHAR(256) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    storage_backend_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NULL,
    usage_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='下载令牌表';

-- =============================================
-- 6. 系统管理模块
-- =============================================

-- Audit log table
DROP TABLE IF EXISTS t_audit_log;
CREATE TABLE t_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64),
    operation_detail TEXT,
    user_id BIGINT,
    username VARCHAR(32),
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    operation_result VARCHAR(20),
    fail_reason VARCHAR(512),
    execution_time_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- Approval record table（修正版 - 移除updated_at与实体对齐）
DROP TABLE IF EXISTS t_approval_record;
CREATE TABLE t_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL COMMENT 'SOFTWARE_PACKAGE, SUBSCRIPTION, BUSINESS_SYSTEM',
    target_id BIGINT NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(64),
    action VARCHAR(20) NOT NULL COMMENT 'SUBMIT, APPROVE, REJECT',
    reason VARCHAR(512),
    operator_id BIGINT,
    operator_name VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表（与实体对齐版）';

-- System setting table（修正版 - 添加category字段，与实体对齐）
DROP TABLE IF EXISTS t_system_setting;
CREATE TABLE t_system_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL COMMENT '设置分类',
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表（与实体对齐版）';

-- File upload table
DROP TABLE IF EXISTS t_file_upload;
CREATE TABLE t_file_upload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(256) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(64),
    checksum VARCHAR(128),
    storage_backend_id BIGINT,
    uploaded_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传表';

-- =============================================
-- 7. 索引创建
-- =============================================

-- User indexes
CREATE INDEX idx_user_username ON t_user(username);
CREATE INDEX idx_user_enabled ON t_user(enabled);

-- Role & Permission indexes
CREATE INDEX idx_role_code ON t_role(role_code);
CREATE INDEX idx_permission_code ON t_permission(permission_code);

-- Login log indexes
CREATE INDEX idx_login_log_user_id ON t_login_log(user_id);
CREATE INDEX idx_login_log_created_at ON t_login_log(created_at);

-- Storage backend indexes
CREATE INDEX idx_storage_backend_type ON t_storage_backend(backend_type);
CREATE INDEX idx_storage_backend_enabled ON t_storage_backend(enabled);
CREATE INDEX idx_storage_backend_status ON t_storage_backend(health_status);

-- Category indexes
CREATE INDEX idx_category_code ON t_category(category_code);
CREATE INDEX idx_category_parent ON t_category(parent_id);

-- Software package indexes
CREATE INDEX idx_package_category ON t_software_package(category_id);
CREATE INDEX idx_package_status ON t_software_package(status);
CREATE INDEX idx_package_type ON t_software_package(software_type);
CREATE INDEX idx_package_key ON t_software_package(package_key);

-- Software version indexes
CREATE INDEX idx_version_package ON t_software_version(package_id);
CREATE INDEX idx_version_status ON t_software_version(status);
CREATE INDEX idx_version_is_latest ON t_software_version(package_id, is_latest);

-- Subscription indexes
CREATE INDEX idx_subscription_user ON t_subscription(user_id);
CREATE INDEX idx_subscription_status ON t_subscription(status);
CREATE INDEX idx_subscription_package ON t_subscription(package_id);

-- Download token indexes
CREATE INDEX idx_download_token_user ON t_download_token(user_id);
CREATE INDEX idx_download_token_token ON t_download_token(token_value);
CREATE INDEX idx_download_token_expires ON t_download_token(expires_at);

-- Audit log indexes
CREATE INDEX idx_audit_log_created_at ON t_audit_log(created_at);
CREATE INDEX idx_audit_log_user_id ON t_audit_log(user_id);
CREATE INDEX idx_audit_log_resource ON t_audit_log(resource_type, resource_id);

-- Approval record indexes
CREATE INDEX idx_approval_record_target ON t_approval_record(target_type, target_id);
CREATE INDEX idx_approval_record_applicant ON t_approval_record(applicant_id);

-- System setting indexes
CREATE INDEX idx_system_setting_category ON t_system_setting(category);
CREATE INDEX idx_system_setting_key ON t_system_setting(setting_key);

-- Business system indexes
CREATE INDEX idx_business_system_domain ON t_business_system(domain);
CREATE INDEX idx_business_system_enabled ON t_business_system(enabled);

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 8. 变更说明
-- =============================================
/*
版本 1.1.0 变更记录：

1. t_category: 添加 description 字段
2. t_tag:
   - 添加 tag_code 字段（非NULL, UNIQUE）
   - 移除 tag_color, sort_order, enabled, updated_at（与实体对齐）
3. t_software_package:
   - 修改 software_type 为 NOT NULL
   - 修改 category_id 为 NULL（与实体对齐）
   - 移除 rating_avg（实体无此字段）
4. t_software_version:
   - 移除 version_status（使用status）
   - 移除 is_lts, deprecated_by, deprecated_at, deprecate_reason, changelog（与实体对齐）
   - 添加 file_size 字段（实体有此字段）
5. t_role: 添加 description 字段
6. t_approval_record: 移除 updated_at（与实体对齐）
7. t_system_setting:
   - 添加 category 字段（非NULL）
   - 移除 setting_type（与实体对齐）
8. t_business_system:
   - 将 domain_id 改为 domain VARCHAR(20)
   - 移除 owner_name, owner_email, status, approved_by, approved_at, reject_reason
   - 添加 responsible_person, enabled
   - 与实体类 BusinessSystem.java 对齐

注意：此版本SQL与当前实体类保持一致。
如果业务需要更多字段，需要同时修改实体类和SQL脚本。
*/
