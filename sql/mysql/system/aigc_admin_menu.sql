-- AIGC Admin 菜单配置（模型、生成、任务、安全、资产、工作流）
-- 适用于 MySQL
-- 注意：请确保在 system_db.sql 之后执行，且 billing 菜单已存在（sort=90）

BEGIN;

-- ===================================================================
-- 1. AIGC 模型管理（父级目录，sort=91）
-- ===================================================================

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    'AIGC 模型管理', '', 1, 91, 0,
    '/aigc-model', 'ep:setting', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @aigcModelParentId := LAST_INSERT_ID();

-- ===================================================================
-- AIGC 使用指南（Docusaurus 静态站入口）
-- ===================================================================

SELECT @aigcGuideParentId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `name` = 'AIGC 使用指南'
  AND `type` = 1
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    'AIGC 使用指南', '', 1, 97, 0,
    '/aigc-guide', 'ep:guide', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcGuideParentId IS NULL;

SELECT @aigcGuideParentId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `name` = 'AIGC 使用指南'
  AND `type` = 1
LIMIT 1;

SELECT @aigcGuideMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `permission` = 'aigc:guide:query'
  AND `type` = 2
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    '使用指南', 'aigc:guide:query', 2, 1, @aigcGuideParentId,
    'guide', 'ep:document', 'aigc/guide/index', 'AigcGuide',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcGuideMenuId IS NULL;

SELECT @aigcGuideMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `permission` = 'aigc:guide:query'
  AND `type` = 2
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '使用指南查询', 'aigc:guide:query', 3, 1, @aigcGuideMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'aigc:guide:query'
      AND `type` = 3
);

-- 模型列表
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '模型列表', 'aigc:model:query', 2, 1, @aigcModelParentId,
    'model', 'ep:monitor', 'aigc/model/model/index', 'AigcModel',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelListMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('模型查询', 'aigc:model:query', 3, 1, @modelListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('模型新增', 'aigc:model:create', 3, 2, @modelListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('模型修改', 'aigc:model:update', 3, 3, @modelListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('模型删除', 'aigc:model:delete', 3, 4, @modelListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 模型渠道商
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '模型渠道商', 'aigc:model:provider:query', 2, 2, @aigcModelParentId,
    'provider', 'ep:connection', 'aigc/model/provider/index', 'AigcModelProvider',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelProviderMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('渠道商查询', 'aigc:model:provider:query', 3, 1, @modelProviderMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('渠道商新增', 'aigc:model:provider:create', 3, 2, @modelProviderMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('渠道商修改', 'aigc:model:provider:update', 3, 3, @modelProviderMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('渠道商删除', 'aigc:model:provider:delete', 3, 4, @modelProviderMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 模型渠道实现
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '渠道实现', 'aigc:model:channel:query', 2, 3, @aigcModelParentId,
    'channel', 'ep:share', 'aigc/model/channel/index', 'AigcModelChannel',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelChannelMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('渠道实现查询', 'aigc:model:channel:query', 3, 1, @modelChannelMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('渠道实现新增', 'aigc:model:channel:create', 3, 2, @modelChannelMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('渠道实现修改', 'aigc:model:channel:update', 3, 3, @modelChannelMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('渠道实现删除', 'aigc:model:channel:delete', 3, 4, @modelChannelMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 代理管理
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '代理管理', 'aigc:model:proxy:query', 2, 4, @aigcModelParentId,
    'proxy', 'ep:link', 'aigc/model/proxy/index', 'AigcModelProxy',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelProxyMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('代理查询', 'aigc:model:proxy:query', 3, 1, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('代理新增', 'aigc:model:proxy:create', 3, 2, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('代理修改', 'aigc:model:proxy:update', 3, 3, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('代理删除', 'aigc:model:proxy:delete', 3, 4, @modelProxyMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 模型参数模板
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '模型参数模板', 'aigc:model:param:query', 2, 5, @aigcModelParentId,
    'param', 'ep:document', 'aigc/model/param/index', 'AigcModelParam',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelParamMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('参数模板查询', 'aigc:model:param:query', 3, 1, @modelParamMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('参数模板新增', 'aigc:model:param:create', 3, 2, @modelParamMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('参数模板修改', 'aigc:model:param:update', 3, 3, @modelParamMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('参数模板删除', 'aigc:model:param:delete', 3, 4, @modelParamMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 模型价格规则
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '模型价格规则', 'aigc:model:price:query', 2, 6, @aigcModelParentId,
    'price', 'ep:coin', 'aigc/model/price/index', 'AigcModelPrice',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelPriceMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('价格规则查询', 'aigc:model:price:query', 3, 1, @modelPriceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('价格规则新增', 'aigc:model:price:create', 3, 2, @modelPriceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('价格规则修改', 'aigc:model:price:update', 3, 3, @modelPriceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('价格规则删除', 'aigc:model:price:delete', 3, 4, @modelPriceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 模型路由规则
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '模型路由规则', 'aigc:model:route:query', 2, 7, @aigcModelParentId,
    'route', 'ep:share', 'aigc/model/route/index', 'AigcModelRoute',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelRouteMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('路由规则查询', 'aigc:model:route:query', 3, 1, @modelRouteMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('路由规则新增', 'aigc:model:route:create', 3, 2, @modelRouteMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('路由规则修改', 'aigc:model:route:update', 3, 3, @modelRouteMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('路由规则删除', 'aigc:model:route:delete', 3, 4, @modelRouteMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 租户模型授权
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '租户模型授权', 'aigc:model:tenant:query', 2, 8, @aigcModelParentId,
    'tenant', 'ep:user', 'aigc/model/tenant/index', 'AigcModelTenant',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @modelTenantMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('授权查询', 'aigc:model:tenant:query', 3, 1, @modelTenantMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('授权新增', 'aigc:model:tenant:create', 3, 2, @modelTenantMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('授权修改', 'aigc:model:tenant:update', 3, 3, @modelTenantMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('授权删除', 'aigc:model:tenant:delete', 3, 4, @modelTenantMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 调用计量
SELECT @modelUsageMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `permission` = 'aigc:model:usage:query'
  AND `type` = 2
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    '调用计量', 'aigc:model:usage:query', 2, 9, @aigcModelParentId,
    'usage', 'ep:data-analysis', 'aigc/model/usage/index', 'AigcModelUsage',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @modelUsageMenuId IS NULL;

SELECT @modelUsageMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `permission` = 'aigc:model:usage:query'
  AND `type` = 2
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT '调用计量查询', 'aigc:model:usage:query', 3, 1, @modelUsageMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'aigc:model:usage:query'
      AND `type` = 3
);

-- ===================================================================
-- 2. AIGC 生成管理（父级目录，sort=92）
-- ===================================================================

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    'AIGC 生成管理', '', 1, 92, 0,
    '/aigc-gen', 'ep:magic-stick', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @aigcGenParentId := LAST_INSERT_ID();

-- 生成记录
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '生成记录', 'aigc:gen:query', 2, 1, @aigcGenParentId,
    'record', 'ep:list', 'aigc/gen/record/index', 'AigcGenRecord',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @genRecordMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('生成记录查询', 'aigc:gen:query', 3, 1, @genRecordMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('同步第三方任务', 'aigc:gen:update', 3, 2, @genRecordMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 生成回调
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '生成回调', 'aigc:gen:query', 2, 2, @aigcGenParentId,
    'callback', 'ep:refresh', 'aigc/gen/callback/index', 'AigcGenCallback',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @genCallbackMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('生成回调查询', 'aigc:gen:query', 3, 1, @genCallbackMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 渠道调用日志
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '渠道调用日志', 'aigc:gen:query', 2, 3, @aigcGenParentId,
    'provider-log', 'ep:document-copy', 'aigc/gen/provider-log/index', 'AigcGenProviderLog',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @genProviderLogMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('渠道调用日志查询', 'aigc:gen:query', 3, 1, @genProviderLogMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- ===================================================================
-- 3. AIGC 任务管理（父级目录，sort=93）
-- ===================================================================

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    'AIGC 任务管理', '', 1, 93, 0,
    '/aigc-task', 'ep:list', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @aigcTaskParentId := LAST_INSERT_ID();

-- 任务列表
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '任务列表', 'aigc:task:query', 2, 1, @aigcTaskParentId,
    'task', 'ep:tickets', 'aigc/task/index', 'AigcTask',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @taskListMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('任务查询', 'aigc:task:query', 3, 1, @taskListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('取消任务', 'aigc:task:cancel', 3, 2, @taskListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('人工标记失败', 'aigc:task:update', 3, 3, @taskListMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 任务统计
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '任务统计', 'aigc:task:query', 2, 2, @aigcTaskParentId,
    'statistics', 'ep:data-analysis', 'aigc/task/statistics/index', 'AigcTaskStatistics',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @taskStatisticsMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('任务统计查询', 'aigc:task:query', 3, 1, @taskStatisticsMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 任务日志
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '任务日志', 'aigc:task:log:query', 2, 3, @aigcTaskParentId,
    'log', 'ep:document', 'aigc/task/log/index', 'AigcTaskLog',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @taskLogMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('任务日志查询', 'aigc:task:log:query', 3, 1, @taskLogMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 任务回调
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '任务回调', 'aigc:task:callback:query', 2, 4, @aigcTaskParentId,
    'callback', 'ep:refresh', 'aigc/task/callback/index', 'AigcTaskCallback',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @taskCallbackMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('任务回调查询', 'aigc:task:callback:query', 3, 1, @taskCallbackMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('回调重放', 'aigc:task:callback:replay', 3, 2, @taskCallbackMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 任务重试
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '任务重试', 'aigc:task:retry:query', 2, 5, @aigcTaskParentId,
    'retry', 'ep:refresh-right', 'aigc/task/retry/index', 'AigcTaskRetry',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @taskRetryMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('重试记录查询', 'aigc:task:retry:query', 3, 1, @taskRetryMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('取消重试', 'aigc:task:retry:update', 3, 2, @taskRetryMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('手动触发重试', 'aigc:task:retry', 3, 3, @taskRetryMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- ===================================================================
-- 4. AIGC 安全管理（父级目录，sort=94）
-- ===================================================================

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    'AIGC 安全管理', '', 1, 94, 0,
    '/aigc-safety', 'ep:shield', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @aigcSafetyParentId := LAST_INSERT_ID();

-- 审核记录
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '审核记录', 'aigc:safety-audit-record:query', 2, 1, @aigcSafetyParentId,
    'audit-record', 'ep:finished', 'aigc/safety/audit-record/index', 'AigcAuditRecord',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @safetyAuditMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('审核记录查询', 'aigc:safety-audit-record:query', 3, 1, @safetyAuditMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('人工审核', 'aigc:safety-audit-record:audit', 3, 2, @safetyAuditMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 敏感词
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '敏感词', 'aigc:safety-sensitive-word:query', 2, 2, @aigcSafetyParentId,
    'sensitive-word', 'ep:warn-triangle-filled', 'aigc/safety/sensitive-word/index', 'AigcSensitiveWord',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @safetySensitiveMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('敏感词查询', 'aigc:safety-sensitive-word:query', 3, 1, @safetySensitiveMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('敏感词新增', 'aigc:safety-sensitive-word:create', 3, 2, @safetySensitiveMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('敏感词修改', 'aigc:safety-sensitive-word:update', 3, 3, @safetySensitiveMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('敏感词删除', 'aigc:safety-sensitive-word:delete', 3, 4, @safetySensitiveMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- ===================================================================
-- 5. AIGC 资产管理（父级目录，sort=95）
-- 注意：前端暂无对应页面，仅创建目录 + 按钮权限用于接口鉴权
-- ===================================================================

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    'AIGC 资产管理', '', 1, 95, 0,
    '/aigc-asset', 'ep:picture', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @aigcAssetParentId := LAST_INSERT_ID();

-- 资产列表（暂无前端组件，仅用于角色授权）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '资产列表', 'aigc:asset:query', 2, 1, @aigcAssetParentId,
    'asset', 'ep:picture', 'aigc/asset/index', 'AigcAsset',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @assetMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('资产查询', 'aigc:asset:query', 3, 1, @assetMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('资产新增', 'aigc:asset:create', 3, 2, @assetMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('资产修改', 'aigc:asset:update', 3, 3, @assetMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('资产删除', 'aigc:asset:delete', 3, 4, @assetMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('资产审核', 'aigc:asset:audit', 3, 5, @assetMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('资产导出', 'aigc:asset:export', 3, 6, @assetMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 下载日志（暂无前端组件，仅用于角色授权）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '下载日志', 'aigc:asset:query', 2, 2, @aigcAssetParentId,
    'download-log', 'ep:download', 'aigc/asset/download-log/index', 'AigcAssetDownloadLog',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @assetDownloadLogMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('下载日志查询', 'aigc:asset:query', 3, 1, @assetDownloadLogMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 提示词模板
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '提示词模板', 'aigc:asset:query', 2, 3, @aigcAssetParentId,
    'prompt-template', 'ep:collection', 'aigc/asset/prompt-template/index', 'AigcPromptTemplate',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @promptTemplateMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('提示词模板查询', 'aigc:asset:query', 3, 1, @promptTemplateMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('提示词模板导入', 'aigc:asset:create', 3, 2, @promptTemplateMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- ===================================================================
-- 6. AIGC 工作流管理（父级目录，sort=96）
-- 注意：前端暂无对应页面，仅创建目录 + 按钮权限用于接口鉴权
-- ===================================================================

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    'AIGC 工作流管理', '', 1, 96, 0,
    '/aigc-workflow', 'ep:connection', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @aigcWorkflowParentId := LAST_INSERT_ID();

-- 工作流定义（暂无前端组件，仅用于角色授权）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '工作流定义', 'aigc:workflow:query', 2, 1, @aigcWorkflowParentId,
    'definition', 'ep:setting', 'aigc/workflow/definition/index', 'AigcWorkflowDefinition',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @workflowDefMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('工作流定义查询', 'aigc:workflow:query', 3, 1, @workflowDefMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工作流创建', 'aigc:workflow:create', 3, 2, @workflowDefMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工作流修改', 'aigc:workflow:update', 3, 3, @workflowDefMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('工作流删除', 'aigc:workflow:delete', 3, 4, @workflowDefMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

-- 工作流实例（暂无前端组件，仅用于角色授权）
INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '工作流实例', 'aigc:workflow:query', 2, 2, @aigcWorkflowParentId,
    'instance', 'ep:list', 'aigc/workflow/instance/index', 'AigcWorkflowInstance',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
);

SET @workflowInstanceMenuId := LAST_INSERT_ID();

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES
('工作流实例查询', 'aigc:workflow:query', 3, 1, @workflowInstanceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('重试节点', 'aigc:workflow:update', 3, 2, @workflowInstanceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
('取消实例', 'aigc:workflow:update', 3, 3, @workflowInstanceMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0');

COMMIT;

-- ===================================================================
-- AIGC 生成记录详情（隐藏菜单/路由）
-- ===================================================================

SELECT @genRecordMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `component` = 'aigc/gen/record/index'
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '生成记录详情', 'aigc:gen:query', 2, 99, @genRecordMenuId,
    'detail/:id', '', 'aigc/gen/record/detail', 'AigcGenerateRecordDetail',
    0, b'0', b'0', b'0',
    'admin', NOW(), 'admin', NOW(), b'0'
);

-- ===================================================================
-- AIGC 任务详情（隐藏菜单/路由）
-- ===================================================================

SELECT @taskListMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `component` = 'aigc/task/index'
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
) VALUES (
    '任务详情', 'aigc:task:query', 2, 99, @taskListMenuId,
    'detail/:id', '', 'aigc/task/detail', 'AigcTaskDetail',
    0, b'0', b'0', b'0',
    'admin', NOW(), 'admin', NOW(), b'0'
);



-- 当前准备提交版本：AIGC Billing 管理端菜单权限
-- type: 1=目录，2=菜单，3=按钮
-- status: 0=开启

-- 1. AIGC 计费管理目录
SELECT @billingParentId := id
FROM system_menu
WHERE name = 'AIGC 计费管理'
  AND type = 1
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    'AIGC 计费管理', '', 1, 90, 0,
    '/aigc-billing', 'ep:coin', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @billingParentId IS NULL;

SELECT @billingParentId := id
FROM system_menu
WHERE name = 'AIGC 计费管理'
  AND type = 1
  AND deleted = b'0'
LIMIT 1;


-- 2. 钱包管理
SELECT @walletMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:wallet:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '钱包管理', 'aigc:billing:wallet:query', 2, 10, @billingParentId,
    'wallet', 'ep:wallet', 'aigc/billing/wallet/index', 'AigcBillingWallet',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @walletMenuId IS NULL;

SELECT @walletMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:wallet:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '钱包查询', 'aigc:billing:wallet:query', 3, 1, @walletMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:wallet:query' AND type = 3 AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '手动调整积分', 'aigc:billing:wallet:update', 3, 2, @walletMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:wallet:update' AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '运营赠送积分', 'aigc:billing:wallet:gift', 3, 3, @walletMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:wallet:gift' AND deleted = b'0');


-- 3. 充值订单
SELECT @rechargeMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:recharge:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '充值订单', 'aigc:billing:recharge:query', 2, 20, @billingParentId,
    'recharge', 'ep:money', 'aigc/billing/recharge/index', 'AigcBillingRecharge',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @rechargeMenuId IS NULL;

SELECT @rechargeMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:recharge:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '充值订单查询', 'aigc:billing:recharge:query', 3, 1, @rechargeMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:recharge:query' AND type = 3 AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '手工充值', 'aigc:billing:recharge:create', 3, 2, @rechargeMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:recharge:create' AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '关闭充值订单', 'aigc:billing:recharge:update', 3, 3, @rechargeMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:recharge:update' AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '充值订单导出', 'aigc:billing:recharge:export', 3, 4, @rechargeMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:recharge:export' AND deleted = b'0');


-- 4. 计费流水
SELECT @recordMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:record:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '计费流水', 'aigc:billing:record:query', 2, 30, @billingParentId,
    'record', 'ep:tickets', 'aigc/billing/record/index', 'AigcBillingRecord',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @recordMenuId IS NULL;

SELECT @recordMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:record:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '计费流水查询', 'aigc:billing:record:query', 3, 1, @recordMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:record:query' AND type = 3 AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '计费流水导出', 'aigc:billing:record:export', 3, 2, @recordMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:record:export' AND deleted = b'0');


-- 5. 成本记录
SELECT @costMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:cost:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '成本记录', 'aigc:billing:cost:query', 2, 40, @billingParentId,
    'cost', 'ep:data-analysis', 'aigc/billing/cost/index', 'AigcBillingCost',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @costMenuId IS NULL;

SELECT @costMenuId := id
FROM system_menu
WHERE permission = 'aigc:billing:cost:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '成本记录查询', 'aigc:billing:cost:query', 3, 1, @costMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:cost:query' AND type = 3 AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '成本记录导出', 'aigc:billing:cost:export', 3, 2, @costMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:billing:cost:export' AND deleted = b'0');

-- AIGC Community management menus
SELECT @aigcCommunityParentId := id
FROM system_menu
WHERE name = 'AIGC 社区管理'
  AND path = '/aigc-community'
  AND type = 1
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    'AIGC 社区管理', '', 1, 98, 0,
    '/aigc-community', 'ep:collection', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcCommunityParentId IS NULL;

SELECT @aigcCommunityParentId := id
FROM system_menu
WHERE name = 'AIGC 社区管理'
  AND path = '/aigc-community'
  AND type = 1
  AND deleted = b'0'
LIMIT 1;

SELECT @communityPostMenuId := id
FROM system_menu
WHERE permission = 'aigc:community-post:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '作品审核', 'aigc:community-post:query', 2, 1, @aigcCommunityParentId,
    'post', 'ep:finished', 'aigc/community/post/index', 'AigcCommunityPost',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @communityPostMenuId IS NULL;

SELECT @communityPostMenuId := id
FROM system_menu
WHERE permission = 'aigc:community-post:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '作品查询', 'aigc:community-post:query', 3, 1, @communityPostMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:community-post:query' AND type = 3 AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '作品审核处理', 'aigc:community-post:audit', 3, 2, @communityPostMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:community-post:audit' AND deleted = b'0');

SELECT @communityCommentMenuId := id
FROM system_menu
WHERE permission = 'aigc:community-comment:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '评论管理', 'aigc:community-comment:query', 2, 2, @aigcCommunityParentId,
    'comment', 'ep:chat-dot-round', 'aigc/community/comment/index', 'AigcCommunityComment',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @communityCommentMenuId IS NULL;

SELECT @communityCommentMenuId := id
FROM system_menu
WHERE permission = 'aigc:community-comment:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '评论查询', 'aigc:community-comment:query', 3, 1, @communityCommentMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:community-comment:query' AND type = 3 AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT '评论审核处理', 'aigc:community-comment:audit', 3, 2, @communityCommentMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:community-comment:audit' AND deleted = b'0');

-- AIGC Guide content management button permissions
SELECT @aigcGuideMenuId := id
FROM system_menu
WHERE permission = 'aigc:guide:query'
  AND type = 2
  AND deleted = b'0'
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT 'Guide create', 'aigc:guide:create', 3, 2, @aigcGuideMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcGuideMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:guide:create' AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT 'Guide update', 'aigc:guide:update', 3, 3, @aigcGuideMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcGuideMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:guide:update' AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT 'Guide delete', 'aigc:guide:delete', 3, 4, @aigcGuideMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcGuideMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:guide:delete' AND deleted = b'0');

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT 'Guide publish', 'aigc:guide:publish', 3, 5, @aigcGuideMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcGuideMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'aigc:guide:publish' AND deleted = b'0');
