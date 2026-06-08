CREATE TABLE IF NOT EXISTS `aigc_model_proxy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) NOT NULL COMMENT '代理名称',
  `protocol` varchar(32) NOT NULL COMMENT '代理协议',
  `host` varchar(255) NOT NULL COMMENT '代理主机',
  `port` int NOT NULL COMMENT '代理端口',
  `username` varchar(255) DEFAULT NULL COMMENT '代理用户名',
  `password` varchar(1024) DEFAULT NULL COMMENT '代理密码',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_tenant` (`name`, `tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型代理表';

SET @add_proxy_id_sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `aigc_model_provider` ADD COLUMN `proxy_id` bigint DEFAULT NULL COMMENT ''代理 ID'' AFTER `proxy_enabled`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'aigc_model_provider'
    AND COLUMN_NAME = 'proxy_id'
);

PREPARE stmt FROM @add_proxy_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
