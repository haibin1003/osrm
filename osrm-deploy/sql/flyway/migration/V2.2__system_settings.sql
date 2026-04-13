-- =============================================================================
-- V2.2: 系统设置扩展 - 存量登记功能开关
-- 功能: 管理员可控制存量登记功能的开启/关闭
-- 作者: Claude
-- 日期: 2026-04-10
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1. 存量登记功能开关设置
-- -----------------------------------------------------------------------------

INSERT INTO t_system_setting (category, setting_key, setting_value, description, created_at, updated_at) VALUES
    ('INVENTORY', 'ENABLE_INVENTORY_FEATURE', 'true', '是否启用存量登记功能(true-启用/false-关闭)', NOW(), NOW()),
    ('INVENTORY', 'INVENTORY_REQUIRE_APPROVAL', 'true', '存量登记是否需要审批(true-需要/false-直接生效)', NOW(), NOW()),
    ('INVENTORY', 'INVENTORY_ALLOW_EDIT_PENDING', 'true', '待审批状态是否允许编辑(true-允许/false-不允许)', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 2. 更新系统版本标记
-- -----------------------------------------------------------------------------

INSERT INTO t_system_config (config_key, config_value, description, updated_at) VALUES
    ('schema_version', '2.2', '数据库架构版本', NOW())
ON DUPLICATE KEY UPDATE
    config_value = '2.2',
    updated_at = NOW();
