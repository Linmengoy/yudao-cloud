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
./script/deploy-frontend-images.ps1 -Server manman
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman -Target admin
```

```powershell
./script/deploy-frontend-images.ps1 -Server manman -Target client
```

前端发布策略：

- 本地 Docker Desktop 完成 `docker buildx build --load` 和 `docker save`。
- 上传镜像包到 `manman:/opt/code`，服务器只执行 `docker load` 和 `docker compose up -d --no-build --force-recreate`。
- `script/docker/docker-compose.frontend.yml` 会随发布脚本同步到 `manman:/opt/code/docker-compose.frontend.yml`。
- 用户访问链路为 `PC -> ucould(Caddy) -> manman(draw2video-client/admin) -> manman(yudao-gateway)`。
- `draw2video-client` 容器内的 Next `/app-api` 代理通过 `host.docker.internal:48080` 访问 manman 宿主机网关，避免容器内访问 `111.228.39.103:48080` 超时。
