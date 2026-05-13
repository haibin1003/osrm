-- =============================================================================
-- V11: 重命名 t_inventory_record 表中 application_catalog_id 列为 business_system_application_id
-- 背景: V9 迁移更新了 application_catalog_id 的值指向 business_system_application.id，
--       但没有重命名列，导致 JPA 实体映射失败。
-- =============================================================================

SET NAMES utf8mb4;

ALTER TABLE t_inventory_record CHANGE COLUMN application_catalog_id business_system_application_id BIGINT COMMENT '关联业务系统子应用ID';
