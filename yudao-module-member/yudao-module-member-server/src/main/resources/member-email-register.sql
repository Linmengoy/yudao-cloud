ALTER TABLE `member_user`
    ADD COLUMN `email` varchar(255) DEFAULT NULL COMMENT '邮箱' AFTER `mobile`,
    ADD COLUMN `email_verified` bit(1) NOT NULL DEFAULT b'0' COMMENT '邮箱是否已验证' AFTER `email`,
    ADD COLUMN `email_bind_time` datetime DEFAULT NULL COMMENT '邮箱绑定时间' AFTER `email_verified`;

CREATE UNIQUE INDEX `uk_tenant_email_deleted` ON `member_user` (`tenant_id`, `email`, `deleted`);

CREATE TABLE IF NOT EXISTS `member_email_code` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `email` varchar(255) NOT NULL COMMENT '邮箱',
    `code` varchar(16) NOT NULL COMMENT '验证码',
    `scene` varchar(64) NOT NULL COMMENT '发送场景',
    `used` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否使用',
    `used_time` datetime DEFAULT NULL COMMENT '使用时间',
    `used_ip` varchar(50) DEFAULT NULL COMMENT '使用 IP',
    `create_ip` varchar(50) DEFAULT NULL COMMENT '创建 IP',
    `today_index` int NOT NULL DEFAULT 1 COMMENT '当天第几次发送',
    `expires_time` datetime NOT NULL COMMENT '过期时间',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_email_scene_create_time` (`tenant_id`, `email`, `scene`, `create_time`),
    KEY `idx_tenant_email_scene_code` (`tenant_id`, `email`, `scene`, `code`),
    KEY `idx_tenant_create_ip_create_time` (`tenant_id`, `create_ip`, `create_time`)
) COMMENT='会员邮箱验证码';

INSERT INTO `system_mail_template` (`name`, `code`, `account_id`, `nickname`, `title`, `content`, `params`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
('会员邮箱注册验证码', 'member_email_register_code', 1, 'AIGC 平台', '【${productName}】邮箱注册验证码', '您好，您的邮箱注册验证码为：${code}，${expireMinutes} 分钟内有效。如非本人操作，请忽略本邮件。', '["code","expireMinutes","productName"]', 0, '会员邮箱注册验证码', '1', NOW(), '1', NOW(), b'0'),
('会员邮箱登录验证码', 'member_email_login_code', 1, 'AIGC 平台', '【${productName}】邮箱登录验证码', '您好，您的邮箱登录验证码为：${code}，${expireMinutes} 分钟内有效。如非本人操作，请忽略本邮件。', '["code","expireMinutes","productName"]', 0, '会员邮箱登录验证码', '1', NOW(), '1', NOW(), b'0'),
('会员邮箱找回密码验证码', 'member_email_reset_password_code', 1, 'AIGC 平台', '【${productName}】找回密码验证码', '您好，您的找回密码验证码为：${code}，${expireMinutes} 分钟内有效。如非本人操作，请忽略本邮件。', '["code","expireMinutes","productName"]', 0, '会员邮箱找回密码验证码', '1', NOW(), '1', NOW(), b'0'),
('会员绑定邮箱验证码', 'member_email_bind_code', 1, 'AIGC 平台', '【${productName}】绑定邮箱验证码', '您好，您的绑定邮箱验证码为：${code}，${expireMinutes} 分钟内有效。如非本人操作，请忽略本邮件。', '["code","expireMinutes","productName"]', 0, '会员绑定邮箱验证码', '1', NOW(), '1', NOW(), b'0'),
('会员换绑邮箱验证码', 'member_email_change_code', 1, 'AIGC 平台', '【${productName}】换绑邮箱验证码', '您好，您的换绑邮箱验证码为：${code}，${expireMinutes} 分钟内有效。如非本人操作，请忽略本邮件。', '["code","expireMinutes","productName"]', 0, '会员换绑邮箱验证码', '1', NOW(), '1', NOW(), b'0');
