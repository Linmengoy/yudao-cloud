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

全量发布用户端和管理端：

```powershell
./script/deploy-frontend-images.ps1 -Server manman
```

只发布管理端：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -Target admin
```

只发布用户端：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -Target client
```

## 发布策略

服务器资源不足，前端镜像必须在本地构建：

1. 本地 Docker Desktop 执行 `docker buildx build --load`。
2. 本地 `docker save` 生成 `draw2video-frontend.tar`、`draw2video-admin.tar` 或 `draw2video-client.tar`。
3. 上传镜像包到 `manman:/opt/code`。
4. 上传 `script/docker/docker-compose.frontend.yml` 到 `manman:/opt/code/docker-compose.frontend.yml`。
5. 服务器只执行 `docker load` 和 `docker compose up -d --no-build --force-recreate`。

`--no-build` 是硬约束，避免服务器上误触发前端构建。

## 关键配置

用户端 `draw2video-client`：

- 浏览器 API 基地址保持同源：`NEXT_PUBLIC_API_BASE_URL=""`、`NEXT_PUBLIC_APP_API_PREFIX=/app-api`。
- 浏览器 WebSocket：`NEXT_PUBLIC_WS_BASE_URL=wss://beta.copse.top`。
- 容器内 Next `/app-api` 代理不能访问 `111.228.39.103:48080`，否则可能在容器内绕公网访问自身导致超时。
- 生产容器通过 `NEXT_PUBLIC_GATEWAY_HOST=host.docker.internal`、`NEXT_PUBLIC_GATEWAY_PORT=48080` 访问 manman 宿主机网关，并在 compose 中配置 `extra_hosts: host.docker.internal:host-gateway`。

管理端 `draw2video-admin`：

- 生产构建使用 `.env.prod`，当前 `VITE_BASE_URL=""`、`VITE_API_URL=/admin-api`，由 `admin.copse.top` 同源转发。
- `.env.dev` 用于 `build:dev` 或开发环境，当前指向 `https://admin.copse.top`。

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
