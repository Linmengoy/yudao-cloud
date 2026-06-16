# Frontend release gate evidence - 2026-06-16

This file records the repository-side evidence for issues #232, #233, #234, #235, and #236. Runtime release evidence still needs to be attached to each actual deployment issue after running the commands below.

## Gate assumptions

- Target audience: internal release owner.
- Deployment cadence: manual release after review.
- Budget impact: no new service or managed dependency.
- Machine-checkable targets: frontend healthcheck resolves within 5s, release smoke commands return a shell exit code, and the release gate requires a recorded rollback image tag before prod.

## Uncommitted change ownership

| File | Diff summary | Owner issue | Release inclusion | Review note |
| --- | --- | --- | --- | --- |
| `script/caddy/Caddyfile` | switches prod upstreams from public `111.228.39.103` to WireGuard `10.66.0.9` | #236 | include only with prod Caddy approval | Needs Caddy validate/reload evidence before prod. |
| `script/d.md` | points operators to `script/deployment-runbook.md` | #236 | include | Documentation only. |
| `script/deploy-frontend-images.sh` | supports `manman2`, optional SSH key, registry pull deploy and no-proxy target resolution | #236 | include with release script review | Bash parity for the PowerShell release path. |
| `script/docker/docker-compose.frontend.yml` | registry-prefix support for guide plus admin/client healthchecks | #235 | include | Required for container-level frontend health gate. |
| `script/docker/docker-compose-micro.yml` | adds admin/client container healthchecks to test micro compose | #235 | include | Keeps standalone and micro compose behavior aligned. |
| `script/docker/docker-compose-micro-prod.yml` | adds admin/client container healthchecks to prod micro compose | #235 | include | Keeps prod compose health gate explicit. |
| `yudao-ui/deploy_frontend_command.md` | documents registry release, rollback tags, health probes and pnpm evidence | #232, #233, #234, #235 | include | Primary operator runbook for frontend release. |
| `script/deployment-runbook.md` | adds frontend healthcheck and previous stable tag gate | #234, #235 | include | Top-level runbook cross-reference. |
| `yudao-ui/draw2video-client/pnpm-workspace.yaml` | approves `@swc/core` and `@parcel/watcher` build scripts as booleans | #232 | include | Prevents `ERR_PNPM_IGNORED_BUILDS` from blocking Vitest in clean installs. |
| `yudao-ui/draw2video-admin/pnpm-workspace.yaml` | approves admin build script dependencies as booleans | #233 | include | Prevents ignored build scripts during `pnpm build:test`. |

No change in this table should be discarded automatically. If a release excludes a row, leave the file in the working tree and record the exclusion reason in the deployment issue.

## Previous stable image tag gate

Before release, record current and previous image tags for both frontend apps:

```powershell
git rev-parse --short=12 HEAD
ssh manman "docker inspect draw2video-client --format '{{.Config.Image}}'"
ssh manman "docker inspect draw2video-admin --format '{{.Config.Image}}'"
ssh manman2 "docker inspect draw2video-client --format '{{.Config.Image}}'"
ssh manman2 "docker inspect draw2video-admin --format '{{.Config.Image}}'"
```

Accept only `test-<commit>` or `prod-<commit>` style rollback tags that are still pullable from the registry. `latest` is not acceptable rollback evidence.

## Build and test evidence commands

Run these commands serially and attach logs to the deployment issue:

```powershell
cd yudao-ui/draw2video-client
pnpm install --frozen-lockfile
pnpm test *> ../../tmp/draw2video-client-pnpm-test-$(Get-Date -Format yyyyMMdd-HHmmss).log

cd ../draw2video-admin
pnpm install --frozen-lockfile
pnpm build:test *> ../../tmp/draw2video-admin-build-test-$(Get-Date -Format yyyyMMdd-HHmmss).log
```

## Current run evidence - 2026-06-16 21:42 +08:00

Current repository commit before this run: `6723999f6a55`.

`draw2video-client` dependency build-script gate:

- Approval config: `yudao-ui/draw2video-client/pnpm-workspace.yaml` allows `@swc/core` and `@parcel/watcher`.
- Command: `pnpm test`
- Log: `tmp/draw2video-client-pnpm-test-20260616-214234.log`
- Result: exit code `0`; Vitest reported 10 test files and 32 tests passed.

`draw2video-admin` build gate:

- Approval config: `yudao-ui/draw2video-admin/pnpm-workspace.yaml` allows admin build dependencies.
- Command: `pnpm build:test`
- Log: `tmp/draw2video-admin-build-test-20260616-214248.log`
- Result: exit code `0`; Vite reported `Build successful. Please see dist-test directory`.

Previous stable image tag inspection for #234:

- `manman` currently runs `127.0.0.1:3000/root/draw2video-client:latest` and `127.0.0.1:3000/root/draw2video-admin:latest`; `latest` is not acceptable rollback evidence.
- `manman` local image cache has client SHA-like tags `f13afed66360` and `fa841db5b28b`, but no matching admin SHA-like tag was present in the inspected list.
- Local Docker has both `111.228.39.103:3000/root/draw2video-client:66abc256c73a` and `111.228.39.103:3000/root/draw2video-admin:66abc256c73a`, and `66abc256c73a` is a real Git commit. However, `docker manifest inspect` against the registry returned `no such manifest` for both tags, so these tags are not accepted as pullable rollback targets.
- `manman2` SSH timed out during banner exchange, so production running images could not be verified in this run.

Conclusion for #234: the repository runbook is in place, but a pullable previous stable tag for both `draw2video-client` and `draw2video-admin` could not be confirmed. The release gate must remain failed until the previous stable images are retagged/pushed or production/test running images expose a valid non-`latest` tag.

## Health evidence commands

```powershell
ssh manman "cd /opt/code && docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml ps draw2video-client draw2video-admin"
ssh manman "curl -fsS -I http://127.0.0.1:13000/"
ssh manman "curl -fsS -I http://127.0.0.1:8081/"
ssh manman2 "cd /opt/code && docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml ps draw2video-client draw2video-admin"
ssh manman2 "curl -fsS -I http://127.0.0.1:13000/"
ssh manman2 "curl -fsS -I http://127.0.0.1:8081/"
```

Expected result: `docker compose ps` reports healthy containers, and HTTP commands exit 0. Any timeout, connection error, or missing `previous stable image tag` fails the release gate.
