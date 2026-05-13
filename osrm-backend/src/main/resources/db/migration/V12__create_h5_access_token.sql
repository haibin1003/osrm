-- =============================================================================
-- V12: 创建 H5 访问令牌表，支持链接有效期管理和访问追踪
-- =============================================================================

SET NAMES utf8mb4;

CREATE TABLE t_h5_access_token (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    token           VARCHAR(64)     NOT NULL                 COMMENT '令牌值（UUID32）',
    expire_at       DATETIME        NOT NULL                 COMMENT '过期时间',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否启用（0=作废 1=有效）',
    created_by      BIGINT          DEFAULT NULL             COMMENT '创建人ID',
    created_by_name VARCHAR(64)     DEFAULT NULL             COMMENT '创建人姓名',
    remark          VARCHAR(200)    DEFAULT NULL             COMMENT '备注说明',
    last_access_at  DATETIME        DEFAULT NULL             COMMENT '最近访问时间',
    access_count    BIGINT          NOT NULL DEFAULT 0       COMMENT '累计访问次数',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE INDEX idx_token (token),
    INDEX idx_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='H5访问令牌表';
