infra-server

本地构建

```
mvn -pl yudao-module-infra/yudao-module-infra-server -am package -DskipTests
```

```
java -jar .\yudao-module-infra\yudao-module-infra-server\target\yudao-module-infra-server.jar
```

system-server

本地构建

```
mvn -pl yudao-module-system/yudao-module-system-server -am package -DskipTests
```

```
java -jar .\yudao-module-system\yudao-module-system-server\target\yudao-module-system-server.jar
```

gateway-server

本地构建

```
mvn -pl yudao-gateway -am package -DskipTests
```

```java
java -jar .\yudao-gateway\target\yudao-gateway.jar
```

admin创建

```
npm run dev-server
```

member-server

```
mvn -pl yudao-module-member/yudao-module-member-server -am package -DskipTests
```

```
java -jar .\yudao-module-member\yudao-module-member-server\target\yudao-module-member-server.jar
```

pay-server

```
mvn -pl yudao-module-pay/yudao-module-pay-server -am package -DskipTests
```

```
java -jar .\yudao-module-pay\yudao-module-pay-server\target\yudao-module-pay-server.jar
```

aigc-asset-server

```
mvn -pl yudao-module-aigc-asset/yudao-module-aigc-asset-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-asset\yudao-module-aigc-asset-server\target\yudao-module-aigc-asset-server.jar
```

aigc-task-server

```
mvn -pl yudao-module-aigc-task/yudao-module-aigc-task-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-task\yudao-module-aigc-task-server\target\yudao-module-aigc-task-server.jar
```

aigc-billing-server

```
mvn -pl yudao-module-aigc-billing/yudao-module-aigc-billing-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-billing\yudao-module-aigc-billing-server\target\yudao-module-aigc-billing-server.jar
```

aigc-model-server

```
mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-model\yudao-module-aigc-model-server\target\yudao-module-aigc-model-server.jar
```

aigc-safety-server

```
mvn -pl yudao-module-aigc-safety/yudao-module-aigc-safety-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-safety\yudao-module-aigc-safety-server\target\yudao-module-aigc-safety-server.jar
```

aigc-gen-server

```
mvn -pl yudao-module-aigc-gen/yudao-module-aigc-gen-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-gen\yudao-module-aigc-gen-server\target\yudao-module-aigc-gen-server.jar
```

aigc-workflow-server

```
mvn -pl yudao-module-aigc-workflow/yudao-module-aigc-workflow-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-workflow\yudao-module-aigc-workflow-server\target\yudao-module-aigc-workflow-server.jar
```



前端

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -UseRegistry
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -UseRegistry
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target admin -UseRegistry
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target admin -UseRegistry
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target client -UseRegistry
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target client -UseRegistry
```

前端发布策略：

- manman 是测试环境，manman2 是生产环境。
- 前端必须在本机 Docker Desktop 构建，服务器不做前端打包。
- Registry 模式：本机执行 `docker buildx build --load`，按 `test-<commit>` 或 `prod-<commit>` 生成镜像标签，推送到 Gitea Container Registry `111.228.39.103:3000/root/draw2video-*:<env>-<commit>`，服务器执行 `docker compose --env-file .frontend-<env>.env pull` 和 `docker compose --env-file .frontend-<env>.env up -d --no-build --force-recreate`。
- 脚本会同步 `/opt/code/.frontend-test.env` 或 `/opt/code/.frontend-prod.env`，用于注入当前环境的网关、API、WebSocket、租户和终端变量。
- manman 与 Gitea Registry 在同一台机器上，脚本默认本机推送到 `111.228.39.103:3000/root`，manman 远端拉取使用 `127.0.0.1:3000/root`。
- manman2 远端拉取使用 `111.228.39.103:3000/root`。
- 如需手动指定 manman 的远端拉取地址：`./script/deploy-frontend-images.ps1 -Server manman -UseRegistry -RemoteRegistry 127.0.0.1:3000/root`。
- `script/docker/docker-compose.frontend.yml` 会随发布脚本同步到目标服务器的 `/opt/code/docker-compose.frontend.yml`。
- 旧 tar 包模式仅作为临时回退：去掉 `-UseRegistry` 后，脚本会 `docker save` 并上传镜像包到服务器，再执行 `docker load`。
- 用户访问链路为 `PC -> ucould(Caddy) -> manman(draw2video-client/admin) -> manman(yudao-gateway)`。
- `draw2video-client` 容器内的 Next `/app-api` 代理通过 `APP_GATEWAY_HOST=host.docker.internal`、`APP_GATEWAY_PORT=48080` 访问当前宿主机网关，避免 manman2 prod 回连 manman test。
