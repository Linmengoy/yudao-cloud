-- AIGC Text 系统提示词管理菜单

SELECT @aigcPromptParentId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `name` = 'AIGC 提示词管理'
  AND `type` = 1
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    'AIGC 提示词管理', '', 1, 99, 0,
    '/aigc-prompt', 'ep:edit-pen', NULL, NULL,
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @aigcPromptParentId IS NULL;

SELECT @aigcPromptParentId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `name` = 'AIGC 提示词管理'
  AND `type` = 1
LIMIT 1;

SELECT @textSystemPromptMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `component` = 'aigc/prompt/text-system/index'
  AND `type` = 2
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT
    'Text 系统提示词', 'aigc:prompt:text-system:query', 2, 1, @aigcPromptParentId,
    'text-system', 'ep:document', 'aigc/prompt/text-system/index', 'AigcTextSystemPrompt',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @textSystemPromptMenuId IS NULL;

SELECT @textSystemPromptMenuId := `id`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND `component` = 'aigc/prompt/text-system/index'
  AND `type` = 2
LIMIT 1;

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 'Text 系统提示词查询', 'aigc:prompt:text-system:query', 3, 1, @textSystemPromptMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = @textSystemPromptMenuId
      AND `permission` = 'aigc:prompt:text-system:query'
);

INSERT INTO `system_menu` (
    `name`, `permission`, `type`, `sort`, `parent_id`,
    `path`, `icon`, `component`, `component_name`,
    `status`, `visible`, `keep_alive`, `always_show`,
    `creator`, `create_time`, `updater`, `update_time`, `deleted`
)
SELECT 'Text 系统提示词保存', 'aigc:prompt:text-system:update', 3, 2, @textSystemPromptMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `parent_id` = @textSystemPromptMenuId
      AND `permission` = 'aigc:prompt:text-system:update'
);
