-- AIGC community operation menus and permissions.
-- Execute in system_db after the base AIGC admin menu migration.

BEGIN;

SELECT @aigcCommunityParentId := id
FROM system_menu
WHERE deleted = b'0'
  AND name = 'AIGC 社区管理'
  AND path = '/aigc-community'
  AND type = 1
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
WHERE deleted = b'0'
  AND name = 'AIGC 社区管理'
  AND path = '/aigc-community'
  AND type = 1
LIMIT 1;

SELECT @communityPostMenuId := id
FROM system_menu
WHERE deleted = b'0'
  AND permission = 'aigc:community-post:query'
  AND type = 2
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '社区作品管理', 'aigc:community-post:query', 2, 1, @aigcCommunityParentId,
    'post', 'ep:finished', 'aigc/community/post/index', 'AigcCommunityPost',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @communityPostMenuId IS NULL;

SELECT @communityPostMenuId := id
FROM system_menu
WHERE deleted = b'0'
  AND permission = 'aigc:community-post:query'
  AND type = 2
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT * FROM (
    SELECT '作品查询' AS name, 'aigc:community-post:query' AS permission, 3 AS type, 1 AS sort, @communityPostMenuId AS parent_id,
           '' AS path, '' AS icon, NULL AS component, NULL AS component_name,
           0 AS status, b'1' AS visible, b'1' AS keep_alive, b'1' AS always_show,
           'admin' AS creator, NOW() AS create_time, 'admin' AS updater, NOW() AS update_time, b'0' AS deleted
    UNION ALL
    SELECT '作品审核', 'aigc:community-post:audit', 3, 2, @communityPostMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
) AS menu_rows
WHERE @communityPostMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE deleted = b'0'
      AND permission = menu_rows.permission
      AND type = menu_rows.type
  );

SELECT @communityCommentMenuId := id
FROM system_menu
WHERE deleted = b'0'
  AND permission = 'aigc:community-comment:query'
  AND type = 2
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT
    '社区评论管理', 'aigc:community-comment:query', 2, 2, @aigcCommunityParentId,
    'comment', 'ep:chat-dot-round', 'aigc/community/comment/index', 'AigcCommunityComment',
    0, b'1', b'1', b'1',
    'admin', NOW(), 'admin', NOW(), b'0'
WHERE @communityCommentMenuId IS NULL;

SELECT @communityCommentMenuId := id
FROM system_menu
WHERE deleted = b'0'
  AND permission = 'aigc:community-comment:query'
  AND type = 2
LIMIT 1;

INSERT INTO system_menu (
    name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
)
SELECT * FROM (
    SELECT '评论查询' AS name, 'aigc:community-comment:query' AS permission, 3 AS type, 1 AS sort, @communityCommentMenuId AS parent_id,
           '' AS path, '' AS icon, NULL AS component, NULL AS component_name,
           0 AS status, b'1' AS visible, b'1' AS keep_alive, b'1' AS always_show,
           'admin' AS creator, NOW() AS create_time, 'admin' AS updater, NOW() AS update_time, b'0' AS deleted
    UNION ALL
    SELECT '评论审核', 'aigc:community-comment:audit', 3, 2, @communityCommentMenuId, '', '', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
) AS menu_rows
WHERE @communityCommentMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE deleted = b'0'
      AND permission = menu_rows.permission
      AND type = menu_rows.type
  );

COMMIT;
