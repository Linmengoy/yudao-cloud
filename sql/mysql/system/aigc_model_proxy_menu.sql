-- AIGC 模型代理管理菜单
-- 适用于已部署环境：请在 system_db 执行。

BEGIN;

SET @aigcModelParentId := (
    SELECT `id` FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `type` = 1
      AND `path` = '/aigc-model'
    ORDER BY `id` DESC
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    '代理管理', 'aigc:model:proxy:query', 2, 3, @aigcModelParentId,
    'proxy', 'ep:link', 'aigc/model/proxy/index', 'AigcModelProxy',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcModelParentId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = @aigcModelParentId
      AND `permission` = 'aigc:model:proxy:query'
  );

SET @modelProxyMenuId := (
    SELECT `id` FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = @aigcModelParentId
      AND `permission` = 'aigc:model:proxy:query'
    ORDER BY `id` DESC
    LIMIT 1
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT * FROM (
    SELECT '代理查询' AS `name`, 'aigc:model:proxy:query' AS `permission`, 3 AS `type`, 1 AS `sort`, @modelProxyMenuId AS `parent_id`,
           '' AS `path`, '' AS `icon`, NULL AS `component`, NULL AS `component_name`,
           0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`,
           'admin' AS `creator`, NOW() AS `create_time`, 'admin' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
    UNION ALL
    SELECT '代理新增', 'aigc:model:proxy:create', 3, 2, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
    UNION ALL
    SELECT '代理修改', 'aigc:model:proxy:update', 3, 3, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
    UNION ALL
    SELECT '代理删除', 'aigc:model:proxy:delete', 3, 4, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
) AS menu_rows
WHERE @modelProxyMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = @modelProxyMenuId
      AND `permission` = menu_rows.`permission`
  );

COMMIT;
