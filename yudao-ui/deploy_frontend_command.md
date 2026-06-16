# 前端部署命令

## 访问链路

```text
PC
  -> ucould / Caddy
  -> manman / draw2video-client:13000 或 draw2video-admin:8081
  -> manman / yudao-gateway:48080
```

当前 `ucould:/opt/caddy/Caddyfile` 中的前端路由：

- `beta.copse.top` 页面反代到 `10.66.0.2:13000`，画布 WebSocket `/aigc/workflow/ws*` 反代到 `10.66.0.2:48080`。
- `admin.copse.top` 页面反代到 `10.66.0.2:8081`，`/admin-api/*` 和 `/infra/ws*` 反代到 `10.66.0.2:48080`。

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

服务器资源不足，前端镜像必须在本地构建：

1. 本地 Docker Desktop 执行 `docker buildx build --load`。
2. 本地 `docker save` 生成 `draw2video-frontend.tar`、`draw2video-admin.tar` 或 `draw2video-client.tar`。
3. 上传镜像包到 `manman:/opt/code`。
4. 上传 `script/docker/docker-compose.frontend.yml` 到 `manman:/opt/code/docker-compose.frontend.yml`。
5. 服务器只执行 `docker load` 和 `docker compose up -d --no-build --force-recreate`。

`--no-build` 是硬约束，避免服务器上误触发前端构建。

Registry 模式的发布链路：

1. 本地 Docker Desktop 执行 `docker buildx build --load`。
2. 本地按环境生成镜像标签：`test-<commit>` 或 `prod-<commit>`，避免 test/prod 共用 `latest` 串环境。
3. 本地把镜像推送到 Gitea Container Registry：`111.228.39.103:3000/root/draw2video-*:<env>-<commit>`。
4. 脚本同步目标环境文件到服务器：`/opt/code/.frontend-test.env` 或 `/opt/code/.frontend-prod.env`。
5. 服务器执行 `docker compose --env-file ... pull`。
6. 服务器执行 `docker compose --env-file ... up -d --no-build --force-recreate`。

manman 和 manman2 都已配置 Docker 信任 `111.228.39.103:3000` 和 `10.66.0.2:3000` 作为 HTTP registry。由于 manman 与 Gitea Registry 在同一台机器上，脚本默认本机推送到 `111.228.39.103:3000/root`，manman 远端拉取时使用 `127.0.0.1:3000/root`；manman2 远端拉取时使用 `111.228.39.103:3000/root`。

如需手动指定远端拉取地址，可以使用：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -UseRegistry -RemoteRegistry 127.0.0.1:3000/root
```

你的本地 Docker Desktop 也需要把 `111.228.39.103:3000` 加到 insecure registries，或后续给 Gitea registry 配置 HTTPS 域名。

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
