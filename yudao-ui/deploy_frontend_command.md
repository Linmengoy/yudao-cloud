# 前端部署命令

## 访问链路

```text
PC
  -> ucould / Caddy
  -> WireGuard
  -> manman(test) 或 manman2(prod)
  -> draw2video-client:13000 / draw2video-admin:8081 / yudao-gateway:48080
```

当前 prod 的 `ucould:/opt/caddy/Caddyfile` 前端路由：

- `beta.copse.top` 页面反代到 `10.66.0.9:13000`，画布 WebSocket `/aigc/workflow/ws*` 反代到 `10.66.0.9:48080`。
- `admin.copse.top` 页面反代到 `10.66.0.9:8081`，`/admin-api/*` 和 `/infra/ws*` 反代到 `10.66.0.9:48080`。

## 发布命令

全量发布前端服务：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -UseRegistry
```

manman 是测试环境，manman2 是生产环境。`-DeployEnv auto` 会根据 `-Server` 自动选择，manman 默认为 `test`，manman2 默认为 `prod`。两边都推荐使用 Gitea Container Registry 发布，避免上传大 tar 包。

如果只想临时走旧的 tar 包模式，可以去掉 `-UseRegistry`：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -Target admin
```

只发布管理端：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target admin -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target admin -UseRegistry
```

只发布用户端：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target client -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target client -UseRegistry
```

## 发布策略

服务器资源不足，前端镜像必须在本地构建。推荐 Registry 模式：

1. 本地 Docker Desktop 执行 `docker buildx build --load`。
2. 本地按环境生成镜像标签：test 使用 `script/docker/test-image-version`，当前从 `v0.0.1` 开始；prod 使用 `prod-<commit>`，避免 test/prod 共用 `latest` 串环境。
3. 本地把镜像推送到 Gitea Container Registry：test 为 `111.228.39.103:3000/root/draw2video-*:v0.0.1` 这类 tag，prod 为 `111.228.39.103:3000/root/draw2video-*:prod-<commit>`。
4. 脚本同步 `script/docker/docker-compose.frontend.yml` 和目标环境文件到服务器：`/opt/code/.frontend-test.env` 或 `/opt/code/.frontend-prod.env`。
5. 服务器执行 `docker compose --env-file ... pull`。
6. 服务器执行 `docker compose --env-file ... up -d --no-build --force-recreate`。

`--no-build` 是硬约束，避免服务器上误触发前端构建。

旧 tar 包模式只作为临时回退：

1. 本地 Docker Desktop 执行 `docker buildx build --load`。
2. 本地 `docker save` 生成 `draw2video-frontend.tar`、`draw2video-admin.tar` 或 `draw2video-client.tar`。
3. 上传镜像包到目标服务器 `/opt/code`。
4. 上传 `script/docker/docker-compose.frontend.yml` 到目标服务器 `/opt/code/docker-compose.frontend.yml`。
5. 服务器只执行 `docker load` 和 `docker compose up -d --no-build --force-recreate`。

manman 和 manman2 都已配置 Docker 信任 `111.228.39.103:3000` 和 `10.66.0.2:3000` 作为 HTTP registry。由于 manman 与 Gitea Registry 在同一台机器上，脚本默认本机推送到 `111.228.39.103:3000/root`，manman 远端拉取时使用 `127.0.0.1:3000/root`；manman2 远端拉取时使用 `111.228.39.103:3000/root`。

如需手动指定远端拉取地址，可以使用：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -UseRegistry -RemoteRegistry 127.0.0.1:3000/root
```

你的本地 Docker Desktop 也需要把 `111.228.39.103:3000` 加到 insecure registries，或后续给 Gitea registry 配置 HTTPS 域名。

## 发布门禁证据

每次前端发布前先记录：

```text
target environment: test 或 prod
target services: draw2video-client / draw2video-admin / draw2video-guide
current commit: <git rev-parse --short=12 HEAD>
current image tag: v0.0.1 或 prod-<current-commit>
previous stable image tag: v0.0.1 或 prod-<old-commit>
release evidence file: tmp/frontend-release-<env>-<yyyymmdd-hhmmss>.log
rollback owner:
```

`previous stable image tag` 不能写 `latest`。优先从上一条成功发布 issue 写回、Gitea Registry 中仍可拉取的镜像 tag、或服务器当前运行镜像获取：

```powershell
ssh manman "docker inspect draw2video-client --format '{{.Config.Image}}'"
ssh manman "docker inspect draw2video-admin --format '{{.Config.Image}}'"
ssh manman2 "docker inspect draw2video-client --format '{{.Config.Image}}'"
ssh manman2 "docker inspect draw2video-admin --format '{{.Config.Image}}'"
```

找不到上一稳定 tag 时，发布门禁失败，先补发布记录或人工确认可回滚镜像。

## 依赖构建脚本审批

`draw2video-client/pnpm-workspace.yaml` 已固化 `@swc/core`、`@parcel/watcher` 等依赖的 build scripts 审批。干净依赖安装后必须能直接进入 Vitest：

```powershell
cd yudao-ui/draw2video-client
pnpm install --frozen-lockfile
pnpm test *> ../../tmp/draw2video-client-pnpm-test-$(Get-Date -Format yyyyMMdd-HHmmss).log
```

如果仍出现 `ERR_PNPM_IGNORED_BUILDS`，不要发布；把完整日志写入发布证据并标记门禁失败。

`draw2video-admin` 同样固化了 pnpm build scripts 审批，发布前串行执行：

```powershell
cd yudao-ui/draw2video-admin
pnpm install --frozen-lockfile
pnpm build:test *> ../../tmp/draw2video-admin-build-test-$(Get-Date -Format yyyyMMdd-HHmmss).log
```

## 健康检查

`script/docker/docker-compose.frontend.yml`、`script/docker/docker-compose-micro.yml` 和 `script/docker/docker-compose-micro-prod.yml` 都为 `draw2video-client` / `draw2video-admin` 配置了容器级 healthcheck。发布后必须保存下面命令输出：

```powershell
ssh manman "cd /opt/code && docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml ps draw2video-client draw2video-admin"
ssh manman "curl -fsS -I http://127.0.0.1:13000/"
ssh manman "curl -fsS -I http://127.0.0.1:8081/"
ssh manman2 "cd /opt/code && docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml ps draw2video-client draw2video-admin"
ssh manman2 "curl -fsS -I http://127.0.0.1:13000/"
ssh manman2 "curl -fsS -I http://127.0.0.1:8081/"
```

期望：`docker compose ps` 显示 `healthy`，HTTP 首页返回 2xx/3xx/4xx 均视为容器可服务，连接失败或超时视为失败。公网 prod 还需要执行：

```powershell
curl.exe -k -sS -I "https://admin.copse.top/"
curl.exe -k -sS -I "https://beta.copse.top/"
```

失败时归档 `docker logs --tail=200 draw2video-client`、`docker logs --tail=200 draw2video-admin`、探活输出，并按下面回滚命令恢复。

## 回滚命令

test 回滚：

```powershell
ssh manman "cd /opt/code && FRONTEND_IMAGE_TAG=<old-test-version> FRONTEND_IMAGE_REGISTRY_PREFIX=127.0.0.1:3000/root/ docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml pull draw2video-client draw2video-admin && FRONTEND_IMAGE_TAG=<old-test-version> FRONTEND_IMAGE_REGISTRY_PREFIX=127.0.0.1:3000/root/ docker compose --env-file .frontend-test.env -f docker-compose.frontend.yml up -d --no-build --force-recreate draw2video-client draw2video-admin"
```

prod 回滚：

```powershell
ssh manman2 "cd /opt/code && FRONTEND_IMAGE_TAG=prod-<old-commit> FRONTEND_IMAGE_REGISTRY_PREFIX=111.228.39.103:3000/root/ docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml pull draw2video-client draw2video-admin && FRONTEND_IMAGE_TAG=prod-<old-commit> FRONTEND_IMAGE_REGISTRY_PREFIX=111.228.39.103:3000/root/ docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml up -d --no-build --force-recreate draw2video-client draw2video-admin"
```

回滚后重复健康检查并把 `previous stable image tag`、回滚命令、`docker compose ps` 和 HTTP 探活结果写回工单。

## 关键配置

用户端 `draw2video-client`：

- 浏览器 API 基地址保持同源：`NEXT_PUBLIC_API_BASE_URL=""`、`NEXT_PUBLIC_APP_API_PREFIX=/app-api`。
- 浏览器 WebSocket：prod 默认 `NEXT_PUBLIC_WS_BASE_URL=wss://beta.copse.top`，test 默认留空并按当前页面域名推导。
- 容器内 Next `/app-api` 代理通过运行期变量 `APP_GATEWAY_HOST`、`APP_GATEWAY_PORT` 指向当前机器网关，不能写死 `111.228.39.103:48080`，否则 manman2 prod 会回连 manman test。
- 容器通过 `host.docker.internal:48080` 访问当前宿主机网关，并在 compose 中配置 `extra_hosts: host.docker.internal:host-gateway`。

管理端 `draw2video-admin`：

- prod 构建使用 `.env.prod`，test 构建使用 `.env.test`，都保持 `VITE_BASE_URL=""`、`VITE_API_URL=/admin-api`，由当前域名同源转发。
- admin 容器内 nginx 的 `/admin-api/` 代理通过运行期变量 `ADMIN_GATEWAY_HOST`、`ADMIN_GATEWAY_PORT` 指向当前机器网关。
- `.env.dev` 用于 `build:dev` 或开发环境。

## 常见问题

如果 `https://beta.copse.top/app-api/member/auth/email-login` 返回 `500` 且 `draw2video-client` 日志出现：

```text
fetch failed
ConnectTimeoutError: attempted address: 111.228.39.103:48080
```

说明错误发生在 `draw2video-client` 的 Next `/app-api` 代理层，不是会员登录业务接口本身。检查 `manman:/opt/code/docker-compose.frontend.yml` 是否包含：

```yaml
NEXT_PUBLIC_GATEWAY_HOST: host.docker.internal
NEXT_PUBLIC_GATEWAY_PORT: 48080
extra_hosts:
  - "host.docker.internal:host-gateway"
```

然后重新发布或执行：

```bash
cd /opt/code
docker compose -f docker-compose.frontend.yml up -d --no-build --force-recreate draw2video-client
```
