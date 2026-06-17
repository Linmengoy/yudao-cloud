# Release gate evidence - 2026-06-17

This file records the automation-2 fullstack development evidence for issues #258, #251, #245, #233, and #232.

## Scope

- #258 prod SSH and frontend healthcheck channel recovery.
- #251 draw2video-admin `pnpm build:test` evidence for candidate `053d08c897fcc35bffa4d458690e6a0ff6bdca73`.
- #245 test `yudao-nacos` Docker healthcheck recovery for Nacos 3.0.3.
- #233 serial draw2video-admin `build:test` log.
- #232 draw2video-client pnpm build-script approval and `pnpm test` evidence.

## Current ref

```text
Current HEAD at verification time: 053d08c897fcc35bffa4d458690e6a0ff6bdca73
Current short SHA: 053d08c897fc
```

## #258 prod SSH and frontend health

Evidence log:

```text
tmp/manman2-prod-ssh-health-20260617-094428.log
tmp/manman2-prod-compose-default-fix-20260617-094545.log
```

Result:

```text
ssh manman2 "docker version" -> exit 0, Docker Engine 29.1.3.
ssh manman2 "docker compose version" -> exit 0, Docker Compose 2.40.3.
ssh manman2 "curl -fsS -I http://127.0.0.1:8081/" -> HTTP/1.1 200 OK.
ssh manman2 "curl -fsS -I http://127.0.0.1:13000/" -> HTTP/1.1 200 OK.
```

Initial `docker compose ps draw2video-admin draw2video-client` failed because `/opt/code` only had `docker-compose.frontend.yml`, so Docker Compose had no default compose file. The fix copied `docker-compose.frontend.yml` to `/opt/code/compose.yml`; the same command then returned both frontend containers and both HTTP checks passed.

## #251 and #233 admin build evidence

Evidence log:

```text
tmp/draw2video-admin-build-test-20260617-093931.log
```

Result:

```text
pnpm install --frozen-lockfile -> exit 0.
pnpm build:test -> exit 0.
Vite output: Build successful. Please see dist-test directory.
```

PowerShell printed `NativeCommandError` text for pnpm lifecycle output, but `$LASTEXITCODE` for both install and build was `0`; this log is valid pass evidence for the candidate SHA above.

## #245 Nacos healthcheck evidence

Evidence logs:

```text
tmp/test-yudao-nacos-health-20260617-094428.log
tmp/test-yudao-nacos-fix-20260617-094545.log
```

Diagnosis:

```text
docker inspect yudao-nacos before fix -> Status unhealthy, FailingStreak 6759.
Failing healthcheck output -> HTTP 410 from the obsolete /nacos/v1/console/health/readiness endpoint.
Nacos logs -> server API started on 8848 and console started on 8080.
curl http://127.0.0.1:8080/v3/console/health/readiness -> {"code":0,"message":"success","data":"ok"}.
```

Fix:

```text
scp script/docker/docker-compose-micro.yml manman:/opt/code/docker-compose-micro.yml
ssh manman "cd /opt/code && docker compose -f docker-compose-micro.yml up -d --no-deps nacos"
```

Verification after recreate:

```text
docker inspect yudao-nacos -> Status healthy, FailingStreak 0.
Readiness endpoint -> {"code":0,"message":"success","data":"ok"}.
Service instance API -> system-server instance returned healthy=true for namespace dev.
```

The repository compose files use `http://127.0.0.1:8080/v3/console/health/readiness` for Nacos 3.0.3.

## #232 client pnpm approval and test evidence

Approval source:

```text
yudao-ui/draw2video-client/pnpm-workspace.yaml
```

Approved build scripts:

```text
@parcel/watcher
@swc/core
msw
sharp
unrs-resolver
```

Evidence log:

```text
tmp/draw2video-client-pnpm-test-20260617-094349.log
```

Result:

```text
pnpm install --frozen-lockfile -> exit 0.
pnpm test -> exit 0.
Vitest result -> 11 test files passed, 36 tests passed.
No ERR_PNPM_IGNORED_BUILDS failure occurred.
```

## Repository verification

```text
python -m pytest tests/test_community_release_gates.py tests/test_review_ready_contracts.py
33 passed in 0.39s
```

