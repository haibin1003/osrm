-- =============================================================================
-- V7: 修复 BUG-001 - 补齐 ROLE_SYSTEM_ADMIN 缺失的 6 个权限
-- 日期: 2026-05-08
-- 背景: 测试发现 admin 缺少 package:approve, package:delete, business-system:approve
--       以及 permission:read/create/update/delete 和 storage:delete
--       其中 V2 误删了 package:approve、package:delete、business-system:approve
--       permission:* 和 storage:delete 从未在种子数据中创建
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 补齐缺失的权限定义（幂等：ON DUPLICATE KEY UPDATE）
-- -----------------------------------------------------------------------------

INSERT INTO t_permission (parent_id, permission_code, permission_name, resource_type, action, path, icon, sort_order, description, created_at, updated_at) VALUES
    -- 重建 V2 误删的软件包审批与删除权限
    (NULL, 'package:approve',         '软件审批',     'package',         'approve', '/packages/approve',         'CheckCircle',  13, '审核软件发布',           NOW(), NOW()),
    (NULL, 'package:delete',          '软件删除',     'package',         'delete',  '/packages/delete',          'PackageX',     12, '删除软件包',             NOW(), NOW()),
    -- 重建 V2 误删的业务系统审批权限
    (NULL, 'business-system:approve', '业务系统审批', 'business-system', 'approve', '/business-systems/approve', 'CheckBuilding', 20, '审批业务系统',           NOW(), NOW()),
    -- 新增权限管理权限（前端有此页面）
    (NULL, 'permission:read',         '权限查看',     'permission',      'read',    '/permissions',              'Lock',         40, '查看权限列表与权限树',   NOW(), NOW()),
    (NULL, 'permission:create',       '权限创建',     'permission',      'create',  '/permissions/create',       'LockPlus',     41, '创建权限',               NOW(), NOW()),
    (NULL, 'permission:update',       '权限编辑',     'permission',      'update',  '/permissions/update',       'LockEdit',     42, '编辑权限',               NOW(), NOW()),
    (NULL, 'permission:delete',       '权限删除',     'permission',      'delete',  '/permissions/delete',       'LockX',        43, '删除权限',               NOW(), NOW()),
    -- 新增存储删除权限
    (NULL, 'storage:delete',          '存储配置删除', 'storage',         'delete',  '/storage/delete',           'HardDriveX',   24, '删除存储配置',           NOW(), NOW())
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    description     = VALUES(description),
    updated_at      = NOW();

-- -----------------------------------------------------------------------------
-- 2. 重新关联 ROLE_SYSTEM_ADMIN 与全部权限（幂等）
-- -----------------------------------------------------------------------------

DELETE FROM t_role_permission
WHERE role_id = (SELECT id FROM t_role WHERE role_code = 'ROLE_SYSTEM_ADMIN');

INSERT INTO t_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM t_role r, t_permission p
WHERE r.role_code = 'ROLE_SYSTEM_ADMIN';

-- -----------------------------------------------------------------------------
-- 3. ROLE_PACKAGE_MANAGER 在 V2 中已删除，此处无需处理
-- -----------------------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 验证清单:
-- [ ] t_permission 中存在 package:approve / package:delete / business-system:approve
-- [ ] t_permission 中存在 permission:read/create/update/delete
-- [ ] t_permission 中存在 storage:delete
-- [ ] ROLE_SYSTEM_ADMIN 拥有 t_permission 表中的所有权限
-- =============================================================================
