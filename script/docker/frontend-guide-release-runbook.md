# draw2video-guide release runbook

This runbook closes the `/guide/` release gate for the frontend guide site.

## Build and deploy

PowerShell:

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target guide
```

Bash:

```bash
script/deploy-frontend-images.sh --server manman --deploy-env test --target guide
```

The test image tag is read from `script/docker/test-image-version`, currently `v0.0.1`.

## Runtime contract

- Compose file: `script/docker/docker-compose.frontend.yml`
- Service: `draw2video-guide`
- Image: `draw2video-guide:${FRONTEND_IMAGE_TAG}`
- Container: `draw2video-guide`
- Local port: `8082:80`
- Public path: `/guide/`
- Container health: `http://127.0.0.1/health`

## Smoke checks

Run on the target host after deploy:

```bash
cd /opt/code
docker compose -f docker-compose.frontend.yml ps draw2video-guide
curl -fsS http://127.0.0.1:8082/health
curl -fsS http://127.0.0.1:8082/guide/
```

The upstream proxy should route public `/guide/` traffic to `manman:8082`.

## Rollback

Use the previous stable test image version recorded by the release operator:

```bash
cd /opt/code
FRONTEND_IMAGE_TAG=<previous-test-version> docker compose -f docker-compose.frontend.yml up -d --no-build --force-recreate draw2video-guide
docker compose -f docker-compose.frontend.yml ps draw2video-guide
curl -fsS http://127.0.0.1:8082/health
```
