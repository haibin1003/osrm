-- OSRM Database Schema for PostgreSQL
-- Version: 1.0

-- Create tables if not exist

-- User table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(20),
    bio TEXT,
    avatar VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMP,
    login_fail_count INTEGER NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Role table
CREATE TABLE IF NOT EXISTS t_role (
    id BIGSERIAL PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Permission table
CREATE TABLE IF NOT EXISTS t_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(64) NOT NULL UNIQUE,
    permission_name VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    description VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User-Role association table
CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE
);

-- Role-Permission association table
CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES t_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES t_permission(id) ON DELETE CASCADE
);

-- Login log table
CREATE TABLE IF NOT EXISTS t_login_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(32) NOT NULL,
    login_type VARCHAR(20) NOT NULL,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    login_status VARCHAR(20) NOT NULL,
    fail_reason VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Storage backend table
CREATE TABLE IF NOT EXISTS t_storage_backend (
    id BIGSERIAL PRIMARY KEY,
    backend_code VARCHAR(32) NOT NULL UNIQUE,
    backend_name VARCHAR(64) NOT NULL,
    backend_type VARCHAR(20) NOT NULL,
    endpoint VARCHAR(256) NOT NULL,
    access_key VARCHAR(128),
    secret_key VARCHAR(256),
    namespace VARCHAR(64),
    config_json TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',
    last_health_check TIMESTAMP,
    error_message VARCHAR(500),
    description VARCHAR(512),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Domain table
CREATE TABLE IF NOT EXISTS t_domain (
    id BIGSERIAL PRIMARY KEY,
    domain_code VARCHAR(32) NOT NULL UNIQUE,
    domain_name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    sort_order INTEGER DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Business system table
CREATE TABLE IF NOT EXISTS t_business_system (
    id BIGSERIAL PRIMARY KEY,
    system_code VARCHAR(32) NOT NULL UNIQUE,
    system_name VARCHAR(128) NOT NULL,
    domain_id BIGINT,
    owner_name VARCHAR(64),
    owner_email VARCHAR(128),
    description VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT,
    approved_at TIMESTAMP,
    reject_reason VARCHAR(256),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Category table
CREATE TABLE IF NOT EXISTS t_category (
    id BIGSERIAL PRIMARY KEY,
    category_code VARCHAR(32) NOT NULL UNIQUE,
    category_name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    icon VARCHAR(64),
    sort_order INTEGER DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Software package table
CREATE TABLE IF NOT EXISTS t_software_package (
    id BIGSERIAL PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL,
    package_key VARCHAR(64) NOT NULL UNIQUE,
    category_id BIGINT NOT NULL,
    description TEXT,
    website_url VARCHAR(256),
    license_type VARCHAR(64),
    license_url VARCHAR(256),
    source_url VARCHAR(256),
    logo_url VARCHAR(256),
    current_version VARCHAR(32),
    view_count INTEGER DEFAULT 0,
    download_count INTEGER DEFAULT 0,
    subscription_count INTEGER DEFAULT 0,
    rating_avg DECIMAL(2,1) DEFAULT 5.0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Software version table
CREATE TABLE IF NOT EXISTS t_software_version (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL,
    version_no VARCHAR(32) NOT NULL,
    version_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    storage_backend_id BIGINT NOT NULL,
    storage_path VARCHAR(512),
    artifact_url VARCHAR(512),
    release_notes TEXT,
    changelog TEXT,
    is_latest BOOLEAN DEFAULT FALSE,
    is_lts BOOLEAN DEFAULT FALSE,
    published_by BIGINT,
    published_at TIMESTAMP,
    deprecated_by BIGINT,
    deprecated_at TIMESTAMP,
    deprecate_reason VARCHAR(256),
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (package_id, version_no),
    CONSTRAINT fk_version_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
);

-- Software document table
CREATE TABLE IF NOT EXISTS t_software_document (
    id BIGSERIAL PRIMARY KEY,
    package_id BIGINT NOT NULL,
    doc_type VARCHAR(32) NOT NULL,
    doc_name VARCHAR(128) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(64),
    uploaded_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE CASCADE
);

-- Subscription table
CREATE TABLE IF NOT EXISTS t_subscription (
    id BIGSERIAL PRIMARY KEY,
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
    max_renewal_count INTEGER DEFAULT 3,
    current_renewal_count INTEGER DEFAULT 0,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    reject_reason VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Download token table
CREATE TABLE IF NOT EXISTS t_download_token (
    id BIGSERIAL PRIMARY KEY,
    token_value VARCHAR(256) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    storage_backend_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit log table
CREATE TABLE IF NOT EXISTS t_audit_log (
    id BIGSERIAL PRIMARY KEY,
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
    execution_time_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_user_username ON t_user(username);
CREATE INDEX IF NOT EXISTS idx_user_enabled ON t_user(enabled);
CREATE INDEX IF NOT EXISTS idx_role_code ON t_role(role_code);
CREATE INDEX IF NOT EXISTS idx_permission_code ON t_permission(permission_code);
CREATE INDEX IF NOT EXISTS idx_login_log_user_id ON t_login_log(user_id);
CREATE INDEX IF NOT EXISTS idx_login_log_created_at ON t_login_log(created_at);
CREATE INDEX IF NOT EXISTS idx_package_category ON t_software_package(category_id);
CREATE INDEX IF NOT EXISTS idx_package_status ON t_software_package(status);
CREATE INDEX IF NOT EXISTS idx_subscription_user ON t_subscription(user_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON t_subscription(status);
CREATE INDEX IF NOT EXISTS idx_download_token_user ON t_download_token(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON t_audit_log(created_at);

-- Storage backend indexes
CREATE INDEX IF NOT EXISTS idx_storage_backend_type ON t_storage_backend(backend_type);
CREATE INDEX IF NOT EXISTS idx_storage_backend_status ON t_storage_backend(health_status);
CREATE INDEX IF NOT EXISTS idx_storage_backend_enabled ON t_storage_backend(enabled);
