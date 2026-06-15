# community_db release runbook

## Scope

Apply `community_db.sql` to `community_db` for `aigc-community-server`.

## Roles and window

- Approval owner: release manager
- Executor: DBA or release engineer
- Verifier: backend owner
- Suggested window: low traffic window with no community writes
- Failure path: stop application rollout, preserve logs and SQL output, then restore from backup or apply rollback SQL below

## Preflight

```bash
mysql -h <mysql-host> -uroot -p -e "CREATE DATABASE IF NOT EXISTS community_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h <mysql-host> -uroot -p -e "SELECT @@version, @@character_set_server, @@collation_server;"
mysql -h <mysql-host> -uroot -p community_db -e "SHOW TABLES LIKE 'aigc_community_%'; SHOW TABLES LIKE 'aigc_guide_content';"
```

## Backup

```bash
mkdir -p /opt/data/mysql-backup/community
mysqldump -h <mysql-host> -uroot -p \
  --single-transaction --routines --triggers --events \
  community_db > /opt/data/mysql-backup/community/community_db_$(date +%Y%m%d%H%M%S).sql
```

Record the backup file path, SHA256, executor, start time, and end time in the release issue.

## Execute

```bash
mysql -h <mysql-host> -uroot -p community_db < yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db.sql
```

## Verify

```sql
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'community_db'
  AND TABLE_NAME IN (
    'aigc_community_post',
    'aigc_community_post_like',
    'aigc_community_comment',
    'aigc_community_share_log',
    'aigc_community_follow',
    'aigc_community_author_stats',
    'aigc_community_audit_log',
    'aigc_guide_content'
  )
ORDER BY TABLE_NAME;

SHOW INDEX FROM aigc_community_post;
SHOW INDEX FROM aigc_community_post_like;
SHOW INDEX FROM aigc_community_follow;
SHOW INDEX FROM aigc_guide_content;
```

Expected result: all eight tables exist, collations are `utf8mb4`, and indexes from `community_db.sql` are present.

## Rollback

If the migration fails before data is written, drop the new tables in reverse dependency order:

```sql
DROP TABLE IF EXISTS aigc_guide_content;
DROP TABLE IF EXISTS aigc_community_audit_log;
DROP TABLE IF EXISTS aigc_community_author_stats;
DROP TABLE IF EXISTS aigc_community_follow;
DROP TABLE IF EXISTS aigc_community_share_log;
DROP TABLE IF EXISTS aigc_community_comment;
DROP TABLE IF EXISTS aigc_community_post_like;
DROP TABLE IF EXISTS aigc_community_post;
```

If community writes already happened, restore from the backup instead of dropping tables:

```bash
mysql -h <mysql-host> -uroot -p -e "DROP DATABASE IF EXISTS community_db; CREATE DATABASE community_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -h <mysql-host> -uroot -p community_db < /opt/data/mysql-backup/community/<backup-file>.sql
```

## Release record

Write the following back to the release issue:

- Approval owner, executor, verifier
- Execution window
- Backup file and checksum
- SQL commit SHA
- Verification SQL output summary
- Rollback decision or "rollback not required"
