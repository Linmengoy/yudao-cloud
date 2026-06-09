ALTER TABLE `aigc_canvas_snapshot`
    ADD COLUMN `storage_type` varchar(32) NOT NULL DEFAULT 'INLINE' COMMENT '存储类型 INLINE/OSS/MINIO' AFTER `version`,
    ADD COLUMN `storage_config_id` bigint DEFAULT NULL COMMENT '文件存储配置 ID' AFTER `storage_type`,
    ADD COLUMN `bucket` varchar(128) DEFAULT NULL COMMENT 'Bucket' AFTER `storage_config_id`,
    ADD COLUMN `snapshot_object_key` varchar(1024) DEFAULT NULL COMMENT '快照对象 Key' AFTER `bucket`,
    ADD COLUMN `snapshot_size` bigint DEFAULT NULL COMMENT '快照 JSON 字节数' AFTER `snapshot_object_key`,
    ADD COLUMN `snapshot_hash` varchar(128) DEFAULT NULL COMMENT '快照 Hash' AFTER `snapshot_size`,
    MODIFY COLUMN `nodes_json` json DEFAULT NULL COMMENT '节点 JSON',
    MODIFY COLUMN `edges_json` json DEFAULT NULL COMMENT '连线 JSON';
