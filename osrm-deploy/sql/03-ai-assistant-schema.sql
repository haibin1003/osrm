-- AI 助手服务数据库结构脚本
-- Version: 1.0.0
-- Date: 2026-03-30
-- Description: AI 助手服务完整表结构

SET NAMES utf8mb4;

-- 1. 目标系统注册表
CREATE TABLE IF NOT EXISTS t_registered_system (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_id       VARCHAR(64)         NOT NULL COMMENT '系统唯一标识',
    system_name     VARCHAR(128)       NOT NULL COMMENT '系统名称',
    icon_url        VARCHAR(512)        COMMENT '系统图标URL',
    mcp_gateway_url VARCHAR(512)       NOT NULL COMMENT 'MCP网关地址',
    auth_type       VARCHAR(32)         NOT NULL DEFAULT 'none' COMMENT '认证类型: none/basic/bearer',
    tool_prefix     VARCHAR(32)         NOT NULL COMMENT '工具名前缀',
    description     TEXT                COMMENT '系统描述',
    is_active       TINYINT            NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_system_id (system_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标系统注册表';

-- 2. 用户会话表
CREATE TABLE IF NOT EXISTS t_session (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    session_id      VARCHAR(64)         NOT NULL COMMENT '会话唯一标识',
    system_id       VARCHAR(64)         NOT NULL COMMENT '目标系统ID',
    user_id         VARCHAR(64)         COMMENT '用户ID',
    username        VARCHAR(128)        COMMENT '用户名',
    user_context    TEXT                COMMENT '用户上下文(JSON)',
    access_token    TEXT                COMMENT '访问令牌(加密)',
    refresh_token   TEXT                COMMENT '刷新令牌(加密)',
    token_expires_at TIMESTAMP          COMMENT '令牌过期时间',
    expires_at      TIMESTAMP          NOT NULL COMMENT '会话过期时间',
    auth_username   VARCHAR(128)        COMMENT 'Basic Auth username',
    auth_password   TEXT                COMMENT 'Basic Auth password (encrypted)',
    is_deleted      TINYINT            NOT NULL DEFAULT 0 COMMENT '是否已删除',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_system_user (system_id, user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

-- 3. 对话表
CREATE TABLE IF NOT EXISTS t_conversation (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    conversation_id VARCHAR(64)         NOT NULL COMMENT '对话唯一标识',
    session_id      VARCHAR(64)         NOT NULL COMMENT '会话ID',
    title           VARCHAR(256)        COMMENT '对话标题',
    message_count   INT                NOT NULL DEFAULT 0 COMMENT '消息数量',
    is_deleted      TINYINT            NOT NULL DEFAULT 0 COMMENT '是否已删除',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_conversation_id (conversation_id),
    INDEX idx_session_id (session_id),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话表';

-- 4. 消息表
CREATE TABLE IF NOT EXISTS t_message (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id      VARCHAR(64)         NOT NULL COMMENT '消息唯一标识',
    conversation_id VARCHAR(64)         NOT NULL COMMENT '对话ID',
    role            VARCHAR(32)         NOT NULL COMMENT '角色: user/assistant/tool',
    content         TEXT                COMMENT '消息内容',
    tool_calls      TEXT                COMMENT '工具调用(JSON)',
    tool_call_id    VARCHAR(64)         COMMENT '工具调用ID',
    tool_name       VARCHAR(128)        COMMENT '工具名称',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_message_id (message_id),
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 5. 技能定义表
CREATE TABLE IF NOT EXISTS t_skill (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    skill_id        VARCHAR(64)         NOT NULL COMMENT '技能唯一标识',
    name            VARCHAR(128)        NOT NULL COMMENT '技能名称',
    description     TEXT                NOT NULL COMMENT '技能描述',
    prompt_template TEXT                NOT NULL COMMENT '提示词模板',
    trigger_keywords TEXT               COMMENT '触发关键词(JSON数组)',
    required_tools  TEXT                COMMENT '所需工具(JSON数组)',
    is_global       TINYINT            NOT NULL DEFAULT 0 COMMENT '是否全局',
    is_active       TINYINT            NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_by      VARCHAR(64)         COMMENT '创建人用户ID',
    system_id       VARCHAR(64)         COMMENT '所属系统ID',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_skill_id (skill_id),
    INDEX idx_is_global (is_global),
    INDEX idx_created_by (created_by),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能定义表';

-- 6. 工具缓存表
CREATE TABLE IF NOT EXISTS t_tool_cache (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_id       VARCHAR(64)         NOT NULL COMMENT '系统ID',
    tool_name       VARCHAR(128)        NOT NULL COMMENT '工具名称',
    description     TEXT                COMMENT '工具描述',
    input_schema    LONGTEXT            NOT NULL COMMENT '输入Schema(JSON)',
    fetched_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '获取时间',

    UNIQUE KEY uk_system_tool (system_id, tool_name),
    INDEX idx_system_id (system_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具缓存表';

-- 7. API Key 配置表
CREATE TABLE IF NOT EXISTS t_api_key_config (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    provider        VARCHAR(32)         NOT NULL COMMENT '提供商标识',
    provider_type   VARCHAR(32)         NOT NULL COMMENT '提供商类型: llm/search',
    api_key         TEXT                COMMENT 'API Key(加密)',
    api_endpoint    VARCHAR(512)        COMMENT 'API 端点(可选覆盖)',
    is_active       TINYINT            NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark          VARCHAR(256)        COMMENT '配置备注',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_provider (provider),
    INDEX idx_provider_type (provider_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API Key配置表';

-- 8. 生成的文档记录表
CREATE TABLE IF NOT EXISTS t_generated_document (
    id                  BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    document_id         VARCHAR(64)         NOT NULL COMMENT '文档唯一标识(UUID)',
    document_type       VARCHAR(32)         NOT NULL COMMENT '文档类型: EXCEL, WORD',
    title               VARCHAR(256)        NOT NULL COMMENT '文档标题',
    file_name           VARCHAR(256)       NOT NULL COMMENT '文件名(带扩展名)',
    file_path           VARCHAR(512)       NOT NULL COMMENT '文件存储路径',
    file_size           BIGINT             COMMENT '文件大小(字节)',
    mime_type           VARCHAR(256)       NOT NULL DEFAULT 'application/octet-stream' COMMENT 'MIME类型',
    created_by          VARCHAR(64)         COMMENT '创建人ID',
    session_id          VARCHAR(64)         COMMENT '会话ID',
    expires_at          TIMESTAMP          NOT NULL COMMENT '过期时间(24小时后)',
    created_at          TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_document_id (document_id),
    INDEX idx_created_by (created_by),
    INDEX idx_session_id (session_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成的文档记录表';

-- 9. 技能包表
CREATE TABLE IF NOT EXISTS t_skill_package (
    id              BIGINT UNSIGNED     PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    skill_id        VARCHAR(64)         NOT NULL COMMENT '技能唯一标识',
    name            VARCHAR(128)        NOT NULL COMMENT '技能名称',
    description     TEXT                COMMENT '技能描述',
    storage_path    VARCHAR(512)        NOT NULL COMMENT '对象存储路径',
    version         VARCHAR(32)         DEFAULT '1.0.0' COMMENT '版本号',
    is_active       TINYINT            NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_by      VARCHAR(64)         COMMENT '创建人',
    created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_skill_id (skill_id),
    INDEX idx_is_active (is_active),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能包表';
