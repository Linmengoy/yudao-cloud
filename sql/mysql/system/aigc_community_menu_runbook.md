# AIGC Community Menu Runbook

## Execute

1. Back up `system_menu` and `system_role_menu` from `system_db`.
2. Run `sql/mysql/system/aigc_community_menu.sql`.
3. Grant the inserted menu IDs to the admin role through the role menu UI or `system_role_menu`.
4. Verify:
   - `aigc/community/post/index` exists with permission `aigc:community-post:query`.
   - `aigc/community/comment/index` exists with permission `aigc:community-comment:query`.
   - Button permissions include `aigc:community-post:audit` and `aigc:community-comment:audit`.

## Rollback

Run `sql/mysql/system/aigc_community_menu_rollback.sql`. The rollback removes role bindings first, then removes the four community permissions and the parent `/aigc-community` directory.
