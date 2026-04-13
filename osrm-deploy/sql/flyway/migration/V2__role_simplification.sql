-- =============================================================================
-- V2: 角色体系简化
-- 变更: 删除ROLE_PACKAGE_MANAGER和ROLE_VISITOR角色，精简权限
-- 作者: Claude
-- 日期: 2026-04-10
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 清理角色权限关联数据（保留开发人员和管理员）
-- -----------------------------------------------------------------------------

-- 删除PACKAGE_MANAGER和VISITOR的角色权限关联
DELETE FROM t_role_permission
WHERE role_id IN (
    SELECT id FROM t_role WHERE role_code IN ('ROLE_PACKAGE_MANAGER', 'ROLE_VISITOR')
);

-- -----------------------------------------------------------------------------
-- 2. 将PACKAGE_MANAGER角色的用户迁移到DEVELOPER角色
-- -----------------------------------------------------------------------------
UPDATE t_user_role
SET role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_DEVELOPER')
WHERE role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_PACKAGE_MANAGER');

-- -----------------------------------------------------------------------------
-- 3. 删除VISITOR角色的用户关联
-- -----------------------------------------------------------------------------
DELETE FROM t_user_role
WHERE role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_VISITOR');

-- -----------------------------------------------------------------------------
-- 4. 删除废弃角色
-- -----------------------------------------------------------------------------
DELETE FROM t_role WHERE role_code = 'ROLE_PACKAGE_MANAGER';
DELETE FROM t_role WHERE role_code = 'ROLE_VISITOR';

-- -----------------------------------------------------------------------------
-- 5. 清理不再需要的权限
-- -----------------------------------------------------------------------------
DELETE FROM t_permission WHERE permission_code IN (
    'package:delete',
    'business-system:approve',
    'package:approve'
);

-- -----------------------------------------------------------------------------
-- 6. 重新定义ROLE_DEVELOPER权限（精简后）
-- -----------------------------------------------------------------------------

DELETE FROM t_role_permission
WHERE role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_DEVELOPER');

INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_DEVELOPER'
AND p.permission_code IN (
    'package:read',
    'subscription:create',
    'subscription:read'
)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- -----------------------------------------------------------------------------
-- 7. 重新定义ROLE_SYSTEM_ADMIN权限（拥有所有权限）
-- -----------------------------------------------------------------------------

DELETE FROM t_role_permission
WHERE role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_SYSTEM_ADMIN');

INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN'
ON DUPLICATE KEY UPDATE updated_at = NOW();

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 变更验证清单:
-- [ ] ROLE_PACKAGE_MANAGER 和 ROLE_VISITOR 角色已删除
-- [ ] 原 PACKAGE_MANAGER 用户已迁移到 DEVELOPER 角色
-- [ ] ROLE_DEVELOPER 权限已重新配置 (package:read, subscription:create, subscription:read)
-- [ ] ROLE_SYSTEM_ADMIN 拥有全部权限
-- [ ] 废弃权限已清理 (package:delete, business-system:approve, package:approve)
-- =============================================================================
