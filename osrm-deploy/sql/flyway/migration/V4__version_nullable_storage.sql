-- V4: Make storage_backend_id nullable for SoftwareVersion
-- This allows versions to exist without uploaded artifacts

ALTER TABLE t_software_version MODIFY COLUMN storage_backend_id BIGINT NULL;
