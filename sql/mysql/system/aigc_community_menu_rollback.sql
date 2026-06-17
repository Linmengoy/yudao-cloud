-- Roll back AIGC community operation menus and permissions.
-- Execute in system_db only when removing the community admin entry is intended.

BEGIN;

DELETE role_menu
FROM system_role_menu role_menu
JOIN system_menu menu ON menu.id = role_menu.menu_id
WHERE menu.permission IN (
    'aigc:community-post:query',
    'aigc:community-post:audit',
    'aigc:community-comment:query',
    'aigc:community-comment:audit'
)
OR menu.path = '/aigc-community';

DELETE FROM system_menu
WHERE permission IN (
    'aigc:community-post:query',
    'aigc:community-post:audit',
    'aigc:community-comment:query',
    'aigc:community-comment:audit'
)
OR path = '/aigc-community';

COMMIT;
