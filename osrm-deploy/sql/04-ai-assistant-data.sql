-- AI 助手服务初始化数据
-- Version: 1.0.0
-- Date: 2026-03-30
-- Description: AI 助手技能包初始数据

SET NAMES utf8mb4;

-- 初始化技能包数据
INSERT INTO t_skill_package (skill_id, name, description, storage_path, version, is_active, created_by)
VALUES
    ('skill-b69dbfb1', 'Open Source Reviewer',
     'Professional open source software security and compliance review assistant. Automatically activates when user asks to review a software package or asks about software security and license compliance.',
     'skills/skill-b69dbfb1', '1.0.0', 1, 'system'),
    ('skill-4d9dac1a', 'License Compliance Checker',
     'License compliance verification assistant. Automatically activates when user asks about license compatibility, commercial use permissions, or wants to check if a software can be used in commercial projects.',
     'skills/skill-4d9dac1a', '1.0.0', 1, 'system'),
    ('skill-sec-scan1', 'Security Vulnerability Scanner',
     'Scans software packages for known security vulnerabilities and provides remediation recommendations.',
     'skills/skill-sec-scan1', '1.0.0', 1, 'system')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    storage_path = VALUES(storage_path);
