# aigc-community release gates

This document is the release evidence template for issues #144, #145, #146, #147, and #148. Fill it during the
release window and paste the completed record back to the release issue.

## Release targets

- API latency target: health and gateway smoke p95 under 500 ms.
- Frontend target: mobile LCP under 2.5 s, INP under 200 ms, CLS under 0.1 for `/guide/` and community pages.
- Availability target: 99.5% monthly SLO for the deployed community path.

## Pre-release evidence for #124-#128

| Issue | Required evidence | Status | Link or command output |
| --- | --- | --- | --- |
| #124 | review result, test result, release decision | pending | |
| #125 | review result, test result, release decision | pending | |
| #126 | review result, test result, release decision | pending | |
| #127 | review result, test result, release decision | pending | |
| #128 | review result, test result, release decision | pending | |

Release is blocked if any row lacks both review and test evidence. When all rows are complete, copy this table to the
release issue with the commit SHA and workflow run URL.

## CI/CD evidence for #145

Record these fields from `.gitea/workflows/yudao-micro-cicd.yml`:

```text
workflow run url:
service input:
runner:
commit sha:
docker version:
docker compose version:
maven command:
image build command:
compose up command:
compose ps summary:
failure logs path:
executor:
executed at:
```

For `aigc-community`, the workflow must run Maven package, image build, `docker compose up`, `docker compose ps`, and
tail service logs on failure.

## Immutable version evidence for #146

Use the 12-character commit SHA from the workflow as `MICRO_IMAGE_TAG` and `FRONTEND_IMAGE_TAG`. Do not use `latest` as
the only production release record.

```text
current version tag:
current commit sha:
previous stable tag:
rollback target tag:
image list:
rollback command:
```

Rollback command:

```bash
MICRO_IMAGE_TAG=<previous-stable-sha> FRONTEND_IMAGE_TAG=<previous-stable-sha> \
  docker compose -f script/docker/docker-compose-micro.yml up -d --no-build --force-recreate aigc-community
```

Local development may omit the tag and fall back to `latest`, but production evidence must record a commit SHA tag.

## Database migration record for #147

Use `yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db_release_runbook.md`.

Required release issue fields:

```text
backup file:
backup sha256:
sql commit sha:
execution window:
executor:
verifier:
verification SQL summary:
service health result:
rollback owner:
rollback decision:
```

Validation must confirm `aigc_guide_content` and all `aigc_community_*` tables use `utf8mb4` collation and have the
indexes defined by `community_db.sql`.

## Smoke test checklist for #148

Record the test account, executor, execution time, screenshots, and log links.

| Area | Check | Expected result | Evidence |
| --- | --- | --- | --- |
| Service health | `curl -fsS http://<host>:48097/actuator/health` | UP or equivalent healthy status | |
| Gateway admin route | `/admin-api/aigc/community/**` and `/admin-api/aigc/guide/**` | routed to `aigc-community-server` | |
| Gateway app route | `/app-api/aigc/community/**` and `/app-api/aigc/guide/**` | routed to `aigc-community-server` | |
| User community list | open community feed | list renders successfully | |
| User detail | open a community post | detail renders successfully | |
| User like | like then unlike a post | counts and state update correctly | |
| User comment | create a comment | comment appears and can be traced | |
| User share | trigger share logging | share count or log updates | |
| User follow | follow then unfollow creator | follow state updates correctly | |
| Creator profile | open creator profile | posts and stats render | |
| Admin list | open community admin list | posts are visible | |
| Admin audit approve | approve a pending post | app visibility matches audit result | |
| Admin audit reject | reject a pending post | app visibility matches audit result | |
| Admin hide/show | hide then show a post | app visibility changes accordingly | |
| Admin delete | delete a test post | post disappears or is marked deleted | |

If any smoke step fails, record the failed API, request payload, response, screenshot or log link, suspected owner, and
rollback decision before marking the release as ready.
