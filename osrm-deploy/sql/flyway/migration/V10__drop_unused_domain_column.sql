-- =============================================================================
-- V10: 删除 t_business_system 表中不再使用的 domain 列
-- 背景: V9 迁移将 domain(单值枚举) 替换为 domain_l1/l2/l3(三级域)，
--       但遗留了旧 domain 列，导致新插入时因 NOT NULL 无默认值而失败。
-- =============================================================================

SET NAMES utf8mb4;

ALTER TABLE t_business_system DROP COLUMN domain;
