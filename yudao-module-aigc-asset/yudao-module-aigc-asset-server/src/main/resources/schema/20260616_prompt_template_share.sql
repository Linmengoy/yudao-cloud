ALTER TABLE `aigc_prompt_template`
  ADD COLUMN `model_code` varchar(128) DEFAULT NULL COMMENT '适配模型编码' AFTER `category`,
  ADD COLUMN `model_name` varchar(128) DEFAULT NULL COMMENT '适配模型名称' AFTER `model_code`,
  ADD COLUMN `model_params` text DEFAULT NULL COMMENT '生成参数 JSON' AFTER `model_name`;
