-- V1__init_schema.sql
-- OSRM Database Initial Schema
-- Generated from entity classes

-- =============================================
-- USER MODULE
-- =============================================

-- User table
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
    locked_until DATETIME,
    login_fail_count INT NOT NULL DEFAULT 0,
    last_login_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_user_username (username),
    INDEX idx_user_email (email),
    INDEX idx_user_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Role table
CREATE TABLE t_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_role_role_code (role_code),
    INDEX idx_role_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Permission table
CREATE TABLE t_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    action VARCHAR(20),
    path VARCHAR(500),
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_permission_parent_id (parent_id),
    INDEX idx_permission_resource_type (resource_type),
    FOREIGN KEY (parent_id) REFERENCES t_permission(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User-Role mapping table (Many-to-Many)
CREATE TABLE t_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Role-Permission mapping table (Many-to-Many)
CREATE TABLE t_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- SOFTWARE MODULE
-- =============================================

-- Software Type (reference table)
CREATE TABLE t_software_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(32) NOT NULL UNIQUE,
    type_name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Software Package table
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
    created_by BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    published_by BIGINT,
    published_at DATETIME,
    version BIGINT DEFAULT 0,
    INDEX idx_package_package_key (package_key),
    INDEX idx_package_software_type (software_type),
    INDEX idx_package_category_id (category_id),
    INDEX idx_package_status (status),
    INDEX idx_package_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Software Version table
CREATE TABLE t_software_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    version_no VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    storage_backend_id BIGINT NOT NULL,
    storage_path VARCHAR(512),
    artifact_url VARCHAR(512),
    release_notes VARCHAR(2000),
    file_size BIGINT,
    checksum VARCHAR(128),
    is_latest BOOLEAN DEFAULT FALSE,
    published_by BIGINT,
    published_at DATETIME,
    created_by BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_package_version (package_id, version_no),
    INDEX idx_version_package_id (package_id),
    INDEX idx_version_status (status),
    INDEX idx_version_is_latest (is_latest),
    FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- SUBSCRIPTION MODULE
-- =============================================

-- Subscription table
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
    approved_at DATETIME,
    reject_reason VARCHAR(256),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_subscription_subscription_no (subscription_no),
    INDEX idx_subscription_user_id (user_id),
    INDEX idx_subscription_package_id (package_id),
    INDEX idx_subscription_status (status),
    INDEX idx_subscription_business_system_id (business_system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Download Token table
CREATE TABLE t_download_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    expire_at DATETIME NOT NULL,
    max_downloads INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME,
    INDEX idx_download_token_subscription_id (subscription_id),
    INDEX idx_download_token_token (token),
    INDEX idx_download_token_expire_at (expire_at),
    FOREIGN KEY (subscription_id) REFERENCES t_subscription(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- STORAGE MODULE
-- =============================================

-- Storage Backend table
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
    health_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    last_health_check DATETIME,
    error_message VARCHAR(500),
    description VARCHAR(512),
    created_by BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_storage_backend_code (backend_code),
    INDEX idx_storage_backend_type (backend_type),
    INDEX idx_storage_backend_health_status (health_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- BUSINESS MODULE
-- =============================================

-- Business System table
CREATE TABLE t_business_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    system_code VARCHAR(32) NOT NULL UNIQUE,
    system_name VARCHAR(64) NOT NULL UNIQUE,
    domain VARCHAR(20) NOT NULL,
    responsible_person VARCHAR(64),
    description VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_business_system_code (system_code),
    INDEX idx_business_system_domain (domain),
    INDEX idx_business_system_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- CATEGORY MODULE
-- =============================================

-- Category table (self-referencing for tree structure)
CREATE TABLE t_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(64) NOT NULL,
    category_code VARCHAR(32) NOT NULL UNIQUE,
    parent_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    description VARCHAR(256),
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_category_code (category_code),
    INDEX idx_category_parent_id (parent_id),
    INDEX idx_category_sort_order (sort_order),
    FOREIGN KEY (parent_id) REFERENCES t_category(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tag table
CREATE TABLE t_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(32) NOT NULL UNIQUE,
    tag_code VARCHAR(32) NOT NULL UNIQUE,
    description VARCHAR(128),
    created_at DATETIME,
    UNIQUE KEY uk_tag_name (tag_name),
    UNIQUE KEY uk_tag_code (tag_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- APPROVAL MODULE
-- =============================================

-- Approval Record table
CREATE TABLE t_approval_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(64),
    action VARCHAR(20) NOT NULL,
    reason VARCHAR(512),
    operator_id BIGINT,
    operator_name VARCHAR(64),
    created_at DATETIME,
    INDEX idx_approval_target (target_type, target_id),
    INDEX idx_approval_applicant_id (applicant_id),
    INDEX idx_approval_action (action),
    INDEX idx_approval_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- INVENTORY MODULE
-- =============================================

-- Inventory Record table
CREATE TABLE t_inventory_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_no VARCHAR(32) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    package_id BIGINT,
    package_name VARCHAR(128) NOT NULL,
    version_no VARCHAR(32),
    software_type VARCHAR(32),
    responsible_person VARCHAR(64) NOT NULL,
    business_system_id BIGINT,
    deploy_environment VARCHAR(32),
    server_count INT DEFAULT 1,
    usage_scenario VARCHAR(512),
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at DATETIME,
    reject_reason VARCHAR(256),
    remarks TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_inventory_record_no (record_no),
    INDEX idx_inventory_user_id (user_id),
    INDEX idx_inventory_package_id (package_id),
    INDEX idx_inventory_status (status),
    INDEX idx_inventory_business_system_id (business_system_id),
    INDEX idx_inventory_source_type (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- CONFIG MODULE
-- =============================================

-- System Config table
CREATE TABLE t_system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(64) NOT NULL UNIQUE,
    config_value VARCHAR(512),
    description VARCHAR(256),
    updated_at DATETIME,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- SYSTEM MODULE
-- =============================================

-- System Setting table
CREATE TABLE t_system_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    description VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_system_setting_category (category),
    INDEX idx_system_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- UPLOAD MODULE
-- =============================================

-- File Upload table
CREATE TABLE t_file_upload (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(256) NOT NULL,
    stored_name VARCHAR(128) NOT NULL UNIQUE,
    file_path VARCHAR(512) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(128),
    md5_hash VARCHAR(32),
    related_type VARCHAR(32),
    related_id BIGINT,
    uploaded_by BIGINT,
    created_at DATETIME,
    UNIQUE KEY uk_stored_name (stored_name),
    INDEX idx_file_upload_content_type (content_type),
    INDEX idx_file_upload_related (related_type, related_id),
    INDEX idx_file_upload_uploaded_by (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- INITIAL DATA
-- =============================================

-- Insert default admin role
INSERT INTO t_role (role_code, role_name, description, enabled, created_at, updated_at)
VALUES ('ROLE_SYSTEM_ADMIN', 'System Administrator', 'System administrator with full permissions', TRUE, NOW(), NOW());

-- Insert default developer role
INSERT INTO t_role (role_code, role_name, description, enabled, created_at, updated_at)
VALUES ('ROLE_DEVELOPER', 'Developer', 'Regular developer user', TRUE, NOW(), NOW());

-- Insert default software types
INSERT INTO t_software_type (type_code, type_name, description, created_at, updated_at)
VALUES
    ('DOCKER_IMAGE', 'Docker镜像', 'Docker container image', NOW(), NOW()),
    ('HELM_CHART', 'Helm Chart', 'Kubernetes Helm chart', NOW(), NOW()),
    ('MAVEN', 'Maven包', 'Maven artifact', NOW(), NOW()),
    ('NPM', 'NPM包', 'NPM package', NOW(), NOW()),
    ('PYPI', 'PyPI包', 'Python package', NOW(), NOW()),
    ('GENERIC', '通用文件', 'Generic file artifact', NOW(), NOW());
