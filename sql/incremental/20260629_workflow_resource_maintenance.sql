-- Workflow resource maintenance fields and indexes.
-- Safe to run repeatedly on MySQL 8.x.

SET @schema_name = DATABASE();

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'cache' AND COLUMN_NAME = 'health_status') = 0,
  'ALTER TABLE `cache` ADD COLUMN `health_status` enum(''HEALTHY'',''UNHEALTHY'',''UNKNOWN'') DEFAULT ''UNKNOWN'' COMMENT ''Health status'' AFTER `password`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'cache' AND COLUMN_NAME = 'last_health_check') = 0,
  'ALTER TABLE `cache` ADD COLUMN `last_health_check` datetime DEFAULT NULL COMMENT ''Last health check time'' AFTER `health_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'cache' AND COLUMN_NAME = 'last_check_message') = 0,
  'ALTER TABLE `cache` ADD COLUMN `last_check_message` varchar(500) DEFAULT NULL COMMENT ''Last health check message'' AFTER `last_health_check`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'cache' AND INDEX_NAME = 'idx_cache_tenant_name') = 0,
  'CREATE INDEX `idx_cache_tenant_name` ON `cache` (`tenant_id`, `name`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'cache' AND INDEX_NAME = 'idx_cache_tenant_type_enabled') = 0,
  'CREATE INDEX `idx_cache_tenant_type_enabled` ON `cache` (`tenant_id`, `type`, `enabled`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'cache' AND INDEX_NAME = 'idx_cache_tenant_health') = 0,
  'CREATE INDEX `idx_cache_tenant_health` ON `cache` (`tenant_id`, `health_status`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'datasource' AND COLUMN_NAME = 'health_status') = 0,
  'ALTER TABLE `datasource` ADD COLUMN `health_status` enum(''HEALTHY'',''UNHEALTHY'',''UNKNOWN'') DEFAULT ''UNKNOWN'' COMMENT ''Health status'' AFTER `password`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'datasource' AND COLUMN_NAME = 'last_health_check') = 0,
  'ALTER TABLE `datasource` ADD COLUMN `last_health_check` datetime DEFAULT NULL COMMENT ''Last health check time'' AFTER `health_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'datasource' AND COLUMN_NAME = 'last_check_message') = 0,
  'ALTER TABLE `datasource` ADD COLUMN `last_check_message` varchar(500) DEFAULT NULL COMMENT ''Last health check message'' AFTER `last_health_check`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'datasource' AND INDEX_NAME = 'idx_datasource_tenant_name') = 0,
  'CREATE INDEX `idx_datasource_tenant_name` ON `datasource` (`tenant_id`, `name`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'datasource' AND INDEX_NAME = 'idx_datasource_tenant_type_enabled') = 0,
  'CREATE INDEX `idx_datasource_tenant_type_enabled` ON `datasource` (`tenant_id`, `type`, `enabled`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'datasource' AND INDEX_NAME = 'idx_datasource_tenant_health') = 0,
  'CREATE INDEX `idx_datasource_tenant_health` ON `datasource` (`tenant_id`, `health_status`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mq' AND COLUMN_NAME = 'health_status') = 0,
  'ALTER TABLE `mq` ADD COLUMN `health_status` enum(''HEALTHY'',''UNHEALTHY'',''UNKNOWN'') DEFAULT ''UNKNOWN'' COMMENT ''Health status'' AFTER `config`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mq' AND COLUMN_NAME = 'last_health_check') = 0,
  'ALTER TABLE `mq` ADD COLUMN `last_health_check` datetime DEFAULT NULL COMMENT ''Last health check time'' AFTER `health_status`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mq' AND COLUMN_NAME = 'last_check_message') = 0,
  'ALTER TABLE `mq` ADD COLUMN `last_check_message` varchar(500) DEFAULT NULL COMMENT ''Last health check message'' AFTER `last_health_check`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mq' AND INDEX_NAME = 'idx_mq_tenant_name') = 0,
  'CREATE INDEX `idx_mq_tenant_name` ON `mq` (`tenant_id`, `name`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mq' AND INDEX_NAME = 'idx_mq_tenant_type_enabled') = 0,
  'CREATE INDEX `idx_mq_tenant_type_enabled` ON `mq` (`tenant_id`, `type`, `enabled`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mq' AND INDEX_NAME = 'idx_mq_tenant_health') = 0,
  'CREATE INDEX `idx_mq_tenant_health` ON `mq` (`tenant_id`, `health_status`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
