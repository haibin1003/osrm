-- V5: 初始化存量数据中的软件包到 t_software_package 表
-- 根据 V3 迁移脚本中的存量记录，初始化对应的软件包

-- 中间件类
INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'zookeeper', 'zookeeper', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'zookeeper');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'redis', 'redis', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'redis');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'vsearch', 'vsearch', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'vsearch');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'nslb', 'nslb', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'nslb');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'ingress', 'ingress', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'ingress');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'nginx', 'nginx', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'nginx');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'prometheus', 'prometheus', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'prometheus');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'elk', 'elk', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'elk');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'hadoop_hbase', 'hadoop_hbase', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'hadoop_hbase');

INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'Apache', 'apache', 'MIDDLEWARE', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'apache');

-- 运行时类
INSERT INTO t_software_package (package_name, package_key, software_type, status, created_by, created_at, updated_at)
SELECT DISTINCT 'jdk', 'jdk', 'RUNTIME', 'PUBLISHED', 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_software_package WHERE package_key = 'jdk');
