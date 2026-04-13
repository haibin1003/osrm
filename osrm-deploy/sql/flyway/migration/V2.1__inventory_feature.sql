-- =============================================================================
-- V2.1: 存量登记功能表结构
-- 变更描述: 支持企业登记存量系统已使用的开源软件，包括登记表、权限和角色关联
-- 作者: Claude
-- 日期: 2026-04-10
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 存量登记表
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_inventory_record;

CREATE TABLE t_inventory_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    record_no VARCHAR(32) NOT NULL UNIQUE COMMENT '登记编号',
    user_id BIGINT NOT NULL COMMENT '登记人ID',
    package_id BIGINT COMMENT '关联软件包ID（库内软件，可为空）',
    package_name VARCHAR(128) NOT NULL COMMENT '软件名称',
    version_no VARCHAR(32) COMMENT '版本号',
    software_type VARCHAR(32) COMMENT '软件类型',
    responsible_person VARCHAR(64) NOT NULL COMMENT '负责人姓名',
    business_system_id BIGINT COMMENT '业务系统ID',
    deploy_environment VARCHAR(32) COMMENT '部署环境(PRODUCTION/TEST/DEVELOPMENT)',
    server_count INT DEFAULT 1 COMMENT '服务器数量',
    usage_scenario VARCHAR(512) COMMENT '使用场景描述',
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型(MANUAL-手动/IMPORT-导入)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING-待审批/APPROVED-已批准/REJECTED-已驳回)',
    approved_by BIGINT COMMENT '审批人ID',
    approved_at TIMESTAMP NULL COMMENT '审批时间',
    reject_reason VARCHAR(256) COMMENT '驳回原因',
    remarks TEXT COMMENT '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_package_id (package_id),
    INDEX idx_business_system (business_system_id),
    INDEX idx_created_at (created_at),
    INDEX idx_record_no (record_no),
    CONSTRAINT fk_inventory_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_inventory_package FOREIGN KEY (package_id) REFERENCES t_software_package(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_system FOREIGN KEY (business_system_id) REFERENCES t_business_system(id) ON DELETE SET NULL,
    CONSTRAINT fk_inventory_approver FOREIGN KEY (approved_by) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存量登记表';

-- -----------------------------------------------------------------------------
-- 2. 新增存量登记相关权限
-- -----------------------------------------------------------------------------

INSERT INTO t_permission (parent_id, permission_code, permission_name, resource_type, action, path, icon, sort_order, description, created_at, updated_at) VALUES
    (NULL, 'inventory:create', '存量登记', 'inventory', 'create', '/inventory/create', 'Edit', 30, '登记存量软件使用', NOW(), NOW()),
    (NULL, 'inventory:read', '存量查看', 'inventory', 'read', '/inventory', 'List', 31, '查看存量登记记录', NOW(), NOW()),
    (NULL, 'inventory:update', '存量编辑', 'inventory', 'update', '/inventory/update', 'Edit', 32, '编辑存量登记', NOW(), NOW()),
    (NULL, 'inventory:approve', '存量审批', 'inventory', 'approve', '/inventory/approve', 'Check', 33, '审批存量登记', NOW(), NOW()),
    (NULL, 'inventory:manage', '存量管理', 'inventory', 'manage', '/inventory/manage', 'Setting', 34, '存量功能开关管理', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 3. 为开发人员分配存量登记权限
-- -----------------------------------------------------------------------------

INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER'
AND p.permission_code IN ('inventory:create', 'inventory:read', 'inventory:update')
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 4. 为管理员分配全部存量权限
-- -----------------------------------------------------------------------------

INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN'
AND p.permission_code LIKE 'inventory:%'
ON DUPLICATE KEY UPDATE updated_at = NOW();

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 变更验证清单:
-- [ ] t_inventory_record 表已创建
-- [ ] inventory 相关权限已插入 (inventory:create, inventory:read, inventory:update, inventory:approve, inventory:manage)
-- [ ] 开发人员角色已分配存量登记权限
-- [ ] 管理员角色已分配全部存量权限
-- =============================================================================
