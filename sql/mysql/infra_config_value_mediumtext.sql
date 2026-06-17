ALTER TABLE `infra_config`
  MODIFY COLUMN `value` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数键值';
