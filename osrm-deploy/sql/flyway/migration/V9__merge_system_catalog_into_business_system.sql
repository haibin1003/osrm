-- =============================================================================
-- V9: 合并系统目录到业务系统，应用目录改为业务系统子应用
-- 背景: SystemCatalog 和 BusinessSystem 是同一概念，需要合并;
--      ApplicationCatalog 改为 BusinessSystemApplication，作为 BusinessSystem 的子系统。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 扩展 t_business_system 表，增加 SystemCatalog 的字段
-- -----------------------------------------------------------------------------
ALTER TABLE t_business_system
    ADD COLUMN system_alias VARCHAR(128) COMMENT '系统别名' AFTER system_name,
    ADD COLUMN unit VARCHAR(64) COMMENT '单位' AFTER system_alias,
    ADD COLUMN category VARCHAR(32) COMMENT '系统分类' AFTER unit,
    ADD COLUMN domain_l1 VARCHAR(64) COMMENT '一级域' AFTER category,
    ADD COLUMN domain_l2 VARCHAR(64) COMMENT '二级域' AFTER domain_l1,
    ADD COLUMN domain_l3 VARCHAR(64) COMMENT '三级域' AFTER domain_l2,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_USE' COMMENT '状态(IN_USE/OFFLINE/OFFLINE_REFERENCE/BUILDING)' AFTER domain_l3,
    ADD COLUMN has_applications TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否包含子系统' AFTER status,
    ADD COLUMN vendor VARCHAR(128) COMMENT '开发厂商' AFTER has_applications,
    ADD COLUMN level VARCHAR(32) COMMENT '系统等级' AFTER vendor,
    ADD COLUMN responsible_dept VARCHAR(64) COMMENT '责任部门' AFTER level,
    ADD COLUMN responsible_phone VARCHAR(20) COMMENT '系统级联系方式' AFTER responsible_person,
    ADD COLUMN online_date DATE COMMENT '上线时间' AFTER responsible_phone,
    ADD COLUMN build_mode VARCHAR(20) COMMENT '建设模式' AFTER online_date,
    ADD COLUMN tags VARCHAR(255) COMMENT '系统标签' AFTER build_mode,
    ADD INDEX idx_status (status),
    ADD INDEX idx_unit (unit),
    ADD INDEX idx_dept (responsible_dept);

-- -----------------------------------------------------------------------------
-- 2. 用 system_catalog 数据更新/填充 business_system
--    策略: 按 system_code 匹配更新，未匹配到的插入新记录
-- -----------------------------------------------------------------------------

-- 2.1 更新已有业务系统(13条)
UPDATE t_business_system bs
JOIN t_system_catalog sc ON bs.system_code = sc.system_code
SET
    bs.system_alias = sc.system_alias,
    bs.unit = sc.unit,
    bs.category = sc.category,
    bs.domain_l1 = sc.domain_l1,
    bs.domain_l2 = sc.domain_l2,
    bs.domain_l3 = sc.domain_l3,
    bs.status = sc.status,
    bs.has_applications = sc.has_applications,
    bs.vendor = sc.vendor,
    bs.level = sc.level,
    bs.responsible_dept = sc.responsible_dept,
    bs.responsible_person = COALESCE(bs.responsible_person, sc.responsible_person),
    bs.responsible_phone = sc.responsible_phone,
    bs.online_date = sc.online_date,
    bs.build_mode = sc.build_mode,
    bs.tags = sc.tags;

-- 2.2 插入 system_catalog 中 business_system 没有的数据(约184条)
-- 先临时删除 system_name 唯一索引，避免名称冲突
-- baseline V1 中 system_name 是 UNIQUE 约束，索引名即列名
ALTER TABLE t_business_system DROP INDEX system_name;

INSERT INTO t_business_system (
    system_code, system_name, system_alias, unit, category,
    domain, domain_l1, domain_l2, domain_l3, status, description,
    has_applications, vendor, level, responsible_dept,
    responsible_person, responsible_phone, online_date, build_mode, tags,
    enabled, created_at, updated_at
)
SELECT
    sc.system_code, sc.system_name, sc.system_alias, sc.unit, sc.category,
    'BUSINESS', sc.domain_l1, sc.domain_l2, sc.domain_l3, sc.status, sc.description,
    sc.has_applications, sc.vendor, sc.level, sc.responsible_dept,
    sc.responsible_person, sc.responsible_phone, sc.online_date, sc.build_mode, sc.tags,
    sc.enabled, sc.created_at, sc.updated_at
FROM t_system_catalog sc
LEFT JOIN t_business_system bs ON sc.system_code = bs.system_code
WHERE bs.id IS NULL;

-- 处理重复的 system_name：为重复名称添加序号后缀
UPDATE t_business_system bs1
JOIN (
    SELECT id, system_name,
           ROW_NUMBER() OVER (PARTITION BY system_name ORDER BY id) AS rn
    FROM t_business_system
) bs2 ON bs1.id = bs2.id
SET bs1.system_name = CONCAT(bs1.system_name, '_', bs2.rn - 1)
WHERE bs2.rn > 1;

-- 恢复 system_name 唯一索引
ALTER TABLE t_business_system ADD UNIQUE INDEX system_name (system_name);

-- -----------------------------------------------------------------------------
-- 3. 创建业务系统子应用表 t_business_system_application
-- -----------------------------------------------------------------------------
CREATE TABLE t_business_system_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    application_code VARCHAR(64) NOT NULL UNIQUE COMMENT '子系统编码',
    business_system_id BIGINT NOT NULL COMMENT '关联业务系统ID',
    application_name VARCHAR(128) NOT NULL COMMENT '子系统名称',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_USE' COMMENT '状态',
    description TEXT COMMENT '子系统描述',
    vendor VARCHAR(128) COMMENT '开发厂商',
    responsible_person VARCHAR(64) COMMENT '责任人',
    responsible_phone VARCHAR(20) COMMENT '联系电话',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_business_system_id (business_system_id),
    INDEX idx_status (status),
    INDEX idx_enabled (enabled),
    CONSTRAINT fk_app_business_system FOREIGN KEY (business_system_id)
        REFERENCES t_business_system(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务系统子应用';

-- -----------------------------------------------------------------------------
-- 4. 迁移 application_catalog 数据到 business_system_application
-- -----------------------------------------------------------------------------
INSERT INTO t_business_system_application (
    application_code, business_system_id, application_name, status,
    description, vendor, responsible_person, responsible_phone, enabled,
    created_at, updated_at
)
SELECT
    ac.application_code, bs.id, ac.application_name, ac.status,
    ac.description, ac.vendor, ac.responsible_person, ac.responsible_phone, ac.enabled,
    ac.created_at, ac.updated_at
FROM t_application_catalog ac
JOIN t_business_system bs ON ac.system_code = bs.system_code;

-- -----------------------------------------------------------------------------
-- 5. 更新存量记录: 把 system_catalog_id 映射到 business_system_id
-- -----------------------------------------------------------------------------
UPDATE t_inventory_record ir
JOIN t_system_catalog sc ON ir.system_catalog_id = sc.id
JOIN t_business_system bs ON sc.system_code = bs.system_code
SET ir.business_system_id = bs.id
WHERE ir.system_catalog_id IS NOT NULL;

-- 更新存量记录的 application_catalog_id 为新的 business_system_application.id
UPDATE t_inventory_record ir
JOIN t_application_catalog ac ON ir.application_catalog_id = ac.id
JOIN t_business_system_application bsa ON ac.application_code = bsa.application_code
SET ir.application_catalog_id = bsa.id
WHERE ir.application_catalog_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 6. 更新权限: system-catalog:* → business-system:*
--    策略: business-system:* 权限在 V1.1 已存在。
--    system-catalog 权限在 V8 中新增，目标角色已拥有对应 business-system 权限，
--    直接删除 system-catalog 的角色权限关联和权限记录即可。
-- -----------------------------------------------------------------------------

-- 6.1 删除引用 system-catalog 权限的角色权限关联
DELETE rp FROM t_role_permission rp
JOIN t_permission p ON rp.permission_id = p.id
WHERE p.permission_code LIKE 'system-catalog:%';

-- 6.2 删除 system-catalog 权限
DELETE FROM t_permission WHERE permission_code LIKE 'system-catalog:%';

-- -----------------------------------------------------------------------------
-- 7. 删除旧的 catalog 相关外键约束(避免后续删除表时报错)
--    V8 创建了此约束，直接删除
-- -----------------------------------------------------------------------------
ALTER TABLE t_application_catalog DROP FOREIGN KEY fk_app_system;

SET FOREIGN_KEY_CHECKS = 1;
