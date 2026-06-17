# Fullstack development evidence - 2026-06-17 automation-2

This file records the development and release-gate evidence for issues #275, #248, #247, #241, and #224.

## Scope

- #275: test `draw2video-admin` previous stable image tag was changed from `latest` to a pullable immutable tag.
- #248: client marketing homepage now reuses the `static/www-home` visual direction and includes a public community section.
- #247: generation video outputs are covered by OSS/file-service archiving tests and existing asset creation persists only URL strings plus asset IDs.
- #241: release gate evidence for `aigc-workflow` and `draw2video-client` rollback versions.
- #224: canvas fallback polling is present and covered by existing collaboration contracts.

## Current ref

```text
Current HEAD before commit: 96d366aa2e66
```

## #275 test admin stable tag

The previous test container was running:

```text
127.0.0.1:3000/root/draw2video-admin:latest
```

The current healthy image digest was retagged and pushed:

```text
111.228.39.103:3000/root/draw2video-admin:test-96d366aa2e66
digest: sha256:9e5af7535172be256bc451502daa00f051b2aebff5416c53cc05c93d7d017fc6
docker pull result: Status: Image is up to date
```

`docker manifest inspect` against this Gitea registry returned `no such manifest`, but `docker push` wrote the digest and `docker pull` from the registry succeeded. Pullability was accepted as stronger runtime evidence for this registry.

The test `draw2video-admin` service was recreated with the explicit tag:

```text
IMAGE                                                         STATUS
111.228.39.103:3000/root/draw2video-admin:test-96d366aa2e66   Up 8 minutes (healthy)

docker inspect draw2video-admin --format '{{.Config.Image}}'
111.228.39.103:3000/root/draw2video-admin:test-96d366aa2e66

curl -fsS -I http://127.0.0.1:8081/
HTTP/1.1 200 OK
```

## #241 workflow and client rollback evidence

Production client currently has an immutable image tag and HTTP health:

```text
111.228.39.103:3000/root/draw2video-client:prod-f506dd425e34
HTTP/1.1 200 OK at http://127.0.0.1:13000/
```

Production workflow is still running `latest`:

```text
aigc-workflow:latest
curl http://127.0.0.1:48096/actuator/health -> {"code":401,"msg":"账号未登录","data":null}
```

Release gate result for #241: failed until `aigc-workflow` is rebuilt, tagged, pushed, and deployed with an immutable rollback target. Do not use `latest` as rollback evidence.

## #248 homepage implementation

Implemented in:

```text
yudao-ui/draw2video-client/src/app/(marketing)/page.tsx
yudao-ui/draw2video-client/public/www-home/assets/images/*.webp
```

The homepage now uses the `www-home` hero and inspiration images, keeps the existing start-create auth flow, and loads hot public community posts through the cached community API.

Browser verification against `http://localhost:3000/`:

```text
Hero image loaded: /www-home/assets/images/hero-cinema.webp, natural size 1915x821.
Inspiration images loaded: portrait, bamboo, robots, wave.
Visible headings include: 把灵感，变成会发光的影像。 / 灵感创作 / 公开作品.
Community section renders a public works placeholder when the backend is unavailable.
```

## #247 video output persistence

The existing implementation already archives provider output URLs before persistence and creates generated assets:

```text
AigcGenerateRecordServiceImpl#createAssetsIfNecessary
AigcGenerateRecordServiceImpl#createAsset
AigcMediaArchiveService#archiveOutputUrls
```

Added a focused unit test for `data:video/mp4` output URLs to prove generated video bytes are uploaded through `FileApi` and the persisted `output_urls` value no longer contains inline video media.

## #224 canvas fallback polling

Current implementation:

```text
yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx
```

The canvas runs a 30-second visible-page `syncFromVersion` fallback poll, skips polling while pending operations exist, defers snapshot hydration until pending operations clear, and syncs on visibility return. Existing review contracts cover this behavior.

## Verification

```text
python -m pytest tests/test_generation_persistence_contracts.py tests/test_review_ready_contracts.py tests/test_community_release_gates.py
37 passed in 0.55s

pnpm test
11 test files passed, 36 tests passed

pnpm exec tsc --noEmit
passed

cmd /c "mvn -pl yudao-module-aigc-gen/yudao-module-aigc-gen-server -am -Dtest=AigcMediaArchiveServiceTest -Dsurefire.failIfNoSpecifiedTests=false test"
AigcMediaArchiveServiceTest: 6 tests passed
```
