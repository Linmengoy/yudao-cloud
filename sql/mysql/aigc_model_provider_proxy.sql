ALTER TABLE `aigc_model_provider`
  ADD COLUMN `proxy_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用代理' AFTER `timeout_seconds`,
  ADD COLUMN `proxy_protocol` varchar(32) DEFAULT NULL COMMENT '代理协议' AFTER `proxy_enabled`,
  ADD COLUMN `proxy_host` varchar(255) DEFAULT NULL COMMENT '代理主机' AFTER `proxy_protocol`,
  ADD COLUMN `proxy_port` int DEFAULT NULL COMMENT '代理端口' AFTER `proxy_host`,
  ADD COLUMN `proxy_username` varchar(255) DEFAULT NULL COMMENT '代理用户名' AFTER `proxy_port`,
  ADD COLUMN `proxy_password` varchar(1024) DEFAULT NULL COMMENT '代理密码' AFTER `proxy_username`;
