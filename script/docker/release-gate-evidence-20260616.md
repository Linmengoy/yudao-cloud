# Release gate evidence - 2026-06-16

This file records the repository-side evidence for release gate issues #242, #243, #244, #245, #233, #252, #253, #232, #224, and #221.

## Scope

- #245 Nacos healthcheck: fix the Docker healthcheck endpoint for Nacos 3.0.3 and record runtime probes.
- #244 stable versions: record current Git version, previous stable tag lookup, and running frontend image tags.
- #243 release note database gate: identify the exact SQL files and verification commands for `aigc_release_note` and `system_menu` permissions.
- #242 release ref gate: record the current HEAD and the command set used to prove review/test/build coverage.
- #233 admin build gate: run `pnpm build:test` serially and keep the log path.
- #252 frontend stable tags: record both test and prod running frontend images and reject `latest` as rollback evidence.
- #253 frontend release gate rerun: collect review/test/build/stable evidence and produce the exact `-UseRegistry` publish commands only when all gates pass.
- #232 client pnpm build-script approval: keep `draw2video-client/pnpm-workspace.yaml` allowBuilds in-repo and collect `pnpm test`.
- #224 canvas fallback polling: keep 30s visible-page `syncFromVersion` polling and defer remote snapshots while local operations are pending.
- #221 canvas operation confirmation queue: `submitOperation` returns `{ opId, promise }`; ack/HTTP resolve, rejection/timeout/page switch reject.

## Machine-checkable targets

- Nacos console readiness returns HTTP 200 on `/v3/console/health/readiness` within 20s.
- Nacos service API returns a healthy `system-server` instance for the test namespace.
- Rollback evidence must include a non-`latest` immutable tag; missing `prod-stable-*` tags keep the stable-version gate failed.
- Release-note SQL evidence must include table DDL, `uk_version`, `idx_status_release_date`, and all five permissions: query, create, update, publish, delete.
- `draw2video-admin` build evidence must include command, start time, end time, exit code, and log path.
- `draw2video-client` test evidence must enter Vitest without `ERR_PNPM_IGNORED_BUILDS`.
- Frontend release commands must include target environment, target services, and `-UseRegistry`.

## Current evidence

Current repository HEAD before this evidence update:

```text
49e59605e2c76814296c77e31847e92afd1f95d8
49e59605e2c7
```

Nacos test environment:

```text
yudao-nacos Docker health status: unhealthy
Failing healthcheck command: curl -f http://127.0.0.1:8848/nacos/v1/console/health/readiness
Failing output: HTTP 410 Gone, Nacos says to use GET /v3/console/health/readiness.
Container status: Up 8 hours (unhealthy)
Log summary: Nacos Server API started successfully on 8848; Nacos Console started successfully on 8080.
API probe: http://111.228.39.103:8848/nacos/v1/ns/instance/list?...serviceName=system-server returned one healthy system-server instance.
Console probe: http://111.228.39.103:8080/v3/console/health/readiness returned {"code":0,"message":"success","data":"ok"}.
```

Conclusion for #245: Nacos is available; the unhealthy Docker status is caused by an obsolete Nacos 3.0 healthcheck endpoint. The compose files now use `http://127.0.0.1:8080/v3/console/health/readiness` for both test and prod Nacos containers.

Stable version evidence:

```text
Current HEAD: 49e59605e2c76814296c77e31847e92afd1f95d8
Current short SHA: 49e59605e2c7
prod-stable tag lookup: no prod-stable tag was returned by the local tag query.
test draw2video-client image: 127.0.0.1:3000/root/draw2video-client:latest
test draw2video-admin image: 127.0.0.1:3000/root/draw2video-admin:latest
local test image cache: draw2video-client:f13afed66360, draw2video-client:fa841db5b28b, latest tags.
```

Conclusion for #244/#252: the repository now has a repeatable collection script for both test and prod, but the release gate remains failed until each service has a pullable previous stable tag that is not `latest`.

Release-note database evidence:

```text
DDL source: sql/mysql/model/model_db.sql
Menu source: sql/mysql/system/aigc_admin_menu.sql
Table: aigc_release_note
Required indexes: uk_version, idx_status_release_date
Required permissions: aigc:release-note:query, create, update, publish, delete
Verification command: script/release-gate-checks.ps1 -Check release-note-db -Environment test
```

Conclusion for #243: SQL sources and verification commands are present. The gate should remain failed if the target database backup path, sha256, executor, execution window, and verification output are not attached to the deployment issue.

Release ref evidence:

```text
Current ref command: git rev-parse HEAD
Diff command: git log --oneline -12
Status command: git status --short --branch
Repeatable collection command: script/release-gate-checks.ps1 -Check release-ref
```

Conclusion for #242: the current ref can be rechecked by script. The release gate remains failed until review/test/build evidence points at the exact selected release SHA or a dedicated release ref.

Admin build evidence:

```text
Previous successful log already present: tmp/draw2video-admin-build-test-20260616-214248.log
Result in that log: exit code 0; Vite reported "Build successful. Please see dist-test directory".
Current retry log: tmp/draw2video-admin-build-test-20260616-224137.log
Current retry result: tool timeout interrupted the build and left node processes; do not use this retry as pass evidence.
Repeatable command: script/release-gate-checks.ps1 -Check admin-build
```

Conclusion for #233: a prior same-day serial build log is available and passes. The current retry was interrupted by automation timeout, so the previous passing log remains the evidence to attach.

Client test and pnpm approval evidence:

```text
Approval source: yudao-ui/draw2video-client/pnpm-workspace.yaml
Approved build scripts: @swc/core, @parcel/watcher, msw, sharp, unrs-resolver
Repeatable command: script/release-gate-checks.ps1 -Check client-test
Current log: tmp/release-gates/20260616-231316-938-b228ae06/draw2video-client-pnpm-test.log
Current result: 11 test files passed, 33 tests passed, exit 0.
Expected result: Vitest starts and exits through test results, not ERR_PNPM_IGNORED_BUILDS.
```

Conclusion for #232: build-script approval is stored in the repository. Attach the generated `draw2video-client-pnpm-test.log` from `tmp/release-gates/<timestamp>/` to prove the current candidate.

Canvas sync evidence:

```text
Confirmation queue: yudao-ui/draw2video-client/src/features/canvas/use-canvas-operations.ts
Fallback polling: yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx
Polling cadence: 30s while the page is visible and hydrated.
Pending protection: remote snapshots are deferred while pendingOperationCount > 0.
Failure signal: server rejection, retry exhaustion, or page/project switch rejects pending promises.
```

Conclusion for #221/#224: implementation is present in the client and is validated by the TypeScript/Vitest client gate.

Frontend release gate rerun:

```powershell
script/release-gate-checks.ps1 -Check stable-versions
script/release-gate-checks.ps1 -Check client-test
script/release-gate-checks.ps1 -Check admin-build
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target client -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target admin -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target client -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target admin -UseRegistry
```

Current stable-version rerun:

```text
Command: script/release-gate-checks.ps1 -Check stable-versions
Summary: tmp/release-gates/20260616-231612-972-16a930e4/summary.json
Git HEAD: e49ddb5dfd94fe7ebca5f0f7420980dc95557966
test client image: 127.0.0.1:3000/root/draw2video-client:latest
test admin image: 127.0.0.1:3000/root/draw2video-admin:latest
prod client image: 111.228.39.103:3000/root/draw2video-client:prod-f506dd425e34
prod admin image: 111.228.39.103:3000/root/draw2video-admin:prod-f506dd425e34
Gate result: failed, because test frontend images are `latest` and fail immutable rollback tag validation.
```

Conclusion for #253: do not execute the publish commands until the stable-version gate returns non-`latest` rollback tags for both environments.

## Collection script

Use the script below to collect all gates into one timestamped directory:

```powershell
script/release-gate-checks.ps1 -Check all -Environment test
```

The script writes logs under `tmp/release-gates/<timestamp>/` and emits `summary.json`. A non-zero exit means one or more gates are missing evidence or failed.
