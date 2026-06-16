# 部署与环境操作手册

这份文档是之后发布服务时的总入口。先按环境判断，再按发布对象选择命令。

## 先记住结论

- `manman` 是测试环境，日常 devops 自动化优先在这里跑。
- `manman2` 是生产环境，只做人工确认后的 prod 发布。
- 生产域名不要直接改到 `manman2` 公网入口。公网入口仍然是 `ucould` 上的 Caddy，Caddy 通过 WireGuard 访问 `manman2`。
- 后端 test 用 Gitea workflow `yudao-micro-cicd`，runner 是 `manman`。
- 后端 prod 用 Gitea workflow `yudao-micro-cicd-prod`，runner 是 `manman2-prod`。
- 前端不能在服务器构建，必须在本机 Docker Desktop 构建，再推到 Gitea Registry，由服务器拉取并重启容器。
- prod 的 Nacos 地址固定走 manman 的 WireGuard 地址 `10.66.0.2:8848`，namespace 是 `prod`。
- prod Redis 当前不需要密码。

## 环境表

| 环境 | 主机 | 用途 | Runner | Nacos | 日志 |
| --- | --- | --- | --- | --- | --- |
| local | 本机 Windows | 开发、前端镜像构建 | 无 | 按本地配置 | 本机 |
| test | `manman` | 测试、日常 devops 自动化 | `manman` | `nacos:8848` / namespace `dev` | `/opt/data/yudao-logs` |
| prod | `manman2` | 生产、人工发布 | `manman2-prod` | `10.66.0.2:8848` / namespace `prod` | `/opt/data/yudao-logs-prod` |

WireGuard 地址：

| 节点 | 地址 |
| --- | --- |
| `ucould` | `10.66.0.1` |
| `manman` | `10.66.0.2` |
| `manman2` | `10.66.0.9` |

生产访问链路：

```text
用户浏览器
  -> admin.copse.top / beta.copse.top
  -> ucould:Caddy
  -> WireGuard
  -> manman2:8081 / manman2:13000 / manman2:48080
```

## 服务和端口

| 服务 | Compose service | Spring serviceName | test 容器 | prod 容器 | 端口 |
| --- | --- | --- | --- | --- | --- |
| 网关 | `yudao-gateway` | `gateway-server` | `yudao-gateway` | `yudao-gateway-prod` | `48080` |
| 系统 | `yudao-system` | `system-server` | `yudao-system` | `yudao-system-prod` | `48081` |
| 基础设施 | `yudao-infra` | `infra-server` | `yudao-infra` | `yudao-infra-prod` | `48082` |
| 支付 | `yudao-pay` | `pay-server` | `yudao-pay` | `yudao-pay-prod` | `48085` |
| 会员 | `yudao-member` | `member-server` | `yudao-member` | `yudao-member-prod` | `48087` |
| 模型 | `aigc-model` | `aigc-model-server` | `aigc-model` | `aigc-model-prod` | `48090` |
| 任务 | `aigc-task` | `aigc-task-server` | `aigc-task` | `aigc-task-prod` | `48091` |
| 计费 | `aigc-billing` | `aigc-billing-server` | `aigc-billing` | `aigc-billing-prod` | `48092` |
| 素材 | `aigc-asset` | `aigc-asset-server` | `aigc-asset` | `aigc-asset-prod` | `48093` |
| 安全 | `aigc-safety` | `aigc-safety-server` | `aigc-safety` | `aigc-safety-prod` | `48094` |
| 生成 | `aigc-gen` | `aigc-gen-server` | `aigc-gen` | `aigc-gen-prod` | `48095` |
| 工作流 | `aigc-workflow` | `aigc-workflow-server` | `aigc-workflow` | `aigc-workflow-prod` | `48096` |
| 社区 | `aigc-community` | `aigc-community-server` | `aigc-community` | `aigc-community-prod` | `48097` |
| 管理端 | `draw2video-admin` | 无 | `draw2video-admin` | `draw2video-admin` | `8081 -> 80` |
| 用户端 | `draw2video-client` | 无 | `draw2video-client` | `draw2video-client` | `13000 -> 3000` |
| 指南 | `draw2video-guide` | 无 | `draw2video-guide` | `draw2video-guide` | `8082 -> 80` |

## 发布前门禁

prod 发布前必须确认下面每一项。缺任何关键项就不要发布，先补信息或标记失败。

| 门禁 | 怎么确认 |
| --- | --- |
| review 已通过 | Gitea issue / PR 里有明确 review 结论 |
| test 已通过 | 有测试命令和结果，不要只写 `mvn -DskipTests` |
| build 已通过 | test 环境 workflow 或本地构建成功 |
| 必要环境变量齐全 | Nacos prod 配置、Compose env、前端 `.frontend-prod.env` 都存在 |
| 数据库变更可回滚 | 有备份文件、备份 SHA、SQL 版本、回滚说明 |
| 健康检查地址存在 | 至少能查容器运行、Nacos 实例或 `/actuator/health` |
| 回滚版本明确 | 记录当前 commit SHA 和上一个稳定 commit SHA |

### 回滚版本从哪里取

当前 commit SHA 取本次准备发布的 Git 版本：

```powershell
git rev-parse --short=12 HEAD
```

如果是从 Gitea Actions 发布，也可以直接使用本次 workflow 页面显示的 commit 短 SHA；两者必须一致。

上一个稳定 commit SHA 优先从上一次成功发布记录或 `prod-stable-*` tag 获取：

```powershell
git fetch gitea --tags
git tag --sort=-creatordate | Select-String '^prod-stable-' | Select-Object -First 5
git rev-parse --short=12 prod-stable-20260616-1600
```

如果没有稳定 tag，就从 Gitea issue 的上一条成功发布写回、`script/docker/community-release-evidence-index.md`、或服务器当前运行镜像 tag 中选择最后一次健康检查通过的 SHA。不要把 `latest` 当作生产回滚版本；找不到时先补发布证据或标记门禁失败。

推荐发布策略：

- 单个后端服务：rolling 思路，单服务构建、重启、健康检查。
- 多个后端服务：按依赖顺序小批量发布，不建议 prod 直接选 `all`。
- 前端：本机构建镜像，Registry 分发，服务器只 pull/up。
- 数据库：先备份，再执行 SQL，再发布依赖该表结构的服务。

## 后端 test 发布

适用场景：日常 devops 自动化、验证新功能、先在 `manman` 跑一遍。

Gitea Web 操作：

1. 打开 `http://111.228.39.103:3000/root/manman/actions`。
2. 选择 `yudao-micro-cicd`。
3. 点击手动运行。
4. `ref` 选 `master-jdk17`。
5. `service` 选择要发布的服务，例如 `yudao-system`。
6. `previous_stable_image_tag` 填上一个稳定 commit 的短 SHA，例如 `5fbe85a739e2`。test 也要求填，是为了保留回滚证据。
7. 等 workflow 成功后看 `docker compose ps` 和日志。

不要在这个 workflow 里选择 `draw2video-admin`、`draw2video-client`、`draw2video-guide`。前端必须用本机脚本发布，避免服务器上触发前端打包。

API 触发示例：

```powershell
curl.exe -sS -u root:root -X POST `
  "http://111.228.39.103:3000/api/v1/repos/root/manman/actions/workflows/yudao-micro-cicd.yml/dispatches" `
  -H "Content-Type: application/json" `
  -d "{\"ref\":\"master-jdk17\",\"inputs\":{\"service\":\"yudao-system\",\"previous_stable_image_tag\":\"5fbe85a739e2\"}}"
```

test 验证命令：

```powershell
ssh manman "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
ssh manman "docker logs --tail=120 yudao-system"
curl.exe -sS "http://111.228.39.103:48080/admin-api/system/auth/login"
```

## 后端 prod 发布

适用场景：人工确认后的生产发布，目标主机是 `manman2`。

prod 发布前先记录：

```text
target environment: prod
service:
current commit:
previous stable commit:
database migration:
nacos dataId:
rollback owner:
maintenance window:
```

Gitea Web 操作：

1. 打开 `http://111.228.39.103:3000/root/manman/actions`。
2. 选择 `yudao-micro-cicd-prod`。
3. 手动运行，`ref` 选 `master-jdk17` 或已推送的稳定 tag。
4. `service` 选择单个服务。
5. `previous_stable_image_tag` 必填，必须是 Git SHA。
6. 等 workflow 成功。
7. 在 `manman2` 验证容器、日志、Nacos 实例和公网域名。

API 触发示例：

```powershell
curl.exe -sS -u root:root -X POST `
  "http://111.228.39.103:3000/api/v1/repos/root/manman/actions/workflows/yudao-micro-cicd-prod.yml/dispatches" `
  -H "Content-Type: application/json" `
  -d "{\"ref\":\"master-jdk17\",\"inputs\":{\"service\":\"yudao-system\",\"previous_stable_image_tag\":\"5fbe85a739e2\"}}"
```

prod 验证命令：

```powershell
ssh manman2 "cd /opt/deploy/yudao-micro && docker compose -f docker-compose-micro.yml ps"
ssh manman2 "docker logs --tail=160 yudao-system-prod"
curl.exe -k -sS -I "https://admin.copse.top/"
curl.exe -k -sS -I "https://beta.copse.top/"
curl.exe -k -sS "https://admin.copse.top/admin-api/system/auth/login"
```

Nacos 实例验证：

```powershell
curl.exe -sS "http://111.228.39.103:8848/nacos/v1/ns/instance/list?namespaceId=prod&groupName=DEFAULT_GROUP&serviceName=system-server"
```

当前限制：`aigc-community` 已使用 `aigc-community:${MICRO_IMAGE_TAG}`，前端已使用 `prod-<commit>` / `test-<commit>`。但多数后端服务的 Compose image 仍是 `latest`，所以这些服务的 `previous_stable_image_tag` 目前主要是发布证据；真正快速回滚建议用稳定 Git tag 重新触发 workflow，或后续把所有后端 image 改成 `${MICRO_IMAGE_TAG:-latest}`。

## 前端发布

前端发布必须在本机执行，因为服务器磁盘 IO 不适合打包。

全量发布 test：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -UseRegistry
```

全量发布 prod：

```powershell
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -UseRegistry
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

只发布指南：

```powershell
./script/deploy-frontend-images.ps1 -Server manman -DeployEnv test -Target guide -UseRegistry
./script/deploy-frontend-images.ps1 -Server manman2 -DeployEnv prod -Target guide -UseRegistry
```

不要用 Gitea 后端 workflow 发布前端，即使 test workflow 的选项里还能看到 `draw2video-*`。

脚本会做这些事：

1. 本机 Docker Desktop 构建镜像。
2. 镜像 tag 使用 `test-<commit>` 或 `prod-<commit>`。
3. 推送到 Gitea Registry `111.228.39.103:3000/root`。
4. 同步 `/opt/code/.frontend-test.env` 或 `/opt/code/.frontend-prod.env`。
5. 目标服务器执行 `docker compose pull` 和 `docker compose up -d --no-build --force-recreate`。

前端 prod 验证：

```powershell
ssh manman2 "cd /opt/code && docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml ps"
curl.exe -k -sS -I "https://admin.copse.top/"
curl.exe -k -sS -I "https://beta.copse.top/"
curl.exe -k -sS "https://beta.copse.top/app-api/member/auth/email-login"
```

前端回滚到旧镜像 tag：

```powershell
ssh manman2 "cd /opt/code && FRONTEND_IMAGE_TAG=prod-<old-commit> FRONTEND_IMAGE_REGISTRY_PREFIX=111.228.39.103:3000/root/ docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml pull draw2video-client && FRONTEND_IMAGE_TAG=prod-<old-commit> FRONTEND_IMAGE_REGISTRY_PREFIX=111.228.39.103:3000/root/ docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml up -d --no-build --force-recreate draw2video-client"
```

## Nacos 配置发布

prod 后端启动时会使用：

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_CLOUD_NACOS_SERVER_ADDR=10.66.0.2:8848
SPRING_CLOUD_NACOS_CONFIG_NAMESPACE=prod
SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE=prod
```

仓库中的 Nacos 配置目录：

| 目录 | dataId |
| --- | --- |
| `script/nacos/gateway` | `gateway-server-<env>.yaml` |
| `script/nacos/system` | `system-server-<env>.yaml` |
| `script/nacos/infra` | `infra-server-<env>.yaml` |
| `script/nacos/member` | `member-server-<env>.yaml` |
| `script/nacos/pay` | `pay-server-<env>.yaml` |
| `script/nacos/community` | `aigc-community-server-<env>.yaml` |

发布单个 prod 配置示例：

```powershell
curl.exe -sS -X POST "http://111.228.39.103:8848/nacos/v1/cs/configs" `
  --data-urlencode "dataId=system-server-prod.yaml" `
  --data-urlencode "group=DEFAULT_GROUP" `
  --data-urlencode "tenant=prod" `
  --data-urlencode "type=yaml" `
  --data-urlencode "content@script/nacos/system/system-server-prod.yaml"
```

发布配置后，重启对应服务：

```powershell
ssh manman2 "cd /opt/deploy/yudao-micro && docker compose -f docker-compose-micro.yml up -d --no-deps --force-recreate yudao-system"
```

注意：

- prod 配置里 `xxl-job-admin` 当前统一指向 `http://10.66.0.2:8080/xxl-job-admin`。
- prod Redis 不配置密码。
- 修改 Nacos 配置前先保存原内容，避免无回滚版本。
- 如果某个 AIGC 模块没有 `script/nacos/<module>` 文件，先看模块内 `src/main/resources/application-prod.yaml`；需要动态配置时再补入 `script/nacos` 并发布到 Nacos。

## 数据库变更

数据库变更不能和服务发布混在一起糊过去，必须单独记录。

prod 备份示例：

```powershell
ssh manman2 'ts=$(date +%Y%m%d-%H%M%S); mkdir -p /opt/data/mysql-backup; docker exec yudao-mysql-prod sh -c "mysqldump -uroot -p123456 --single-transaction --routines --triggers --databases ruoyi-vue-pro xxl_job community_db" > /opt/data/mysql-backup/prod-${ts}.sql; sha256sum /opt/data/mysql-backup/prod-${ts}.sql'
```

从 `manman` 复制数据库到 `manman2` 时，不要经过本机中转，走 WireGuard 内网直连：

```powershell
ssh manman "docker exec yudao-mysql sh -c 'mysqldump -uroot -p123456 --single-transaction --routines --triggers --databases community_db' | ssh manman2 'docker exec -i yudao-mysql-prod mysql -uroot -p123456'"
```

执行 SQL 后至少验证：

```powershell
ssh manman2 "docker exec yudao-mysql-prod mysql -uroot -p123456 -e 'SHOW DATABASES;'"
ssh manman2 "docker exec yudao-mysql-prod mysql -uroot -p123456 -e 'SELECT COUNT(*) FROM community_db.aigc_community_post;'"
```

## Caddy / 域名切换

DNS 不需要直接指向 `manman2`。域名继续到 `ucould`，只改 `ucould:/opt/caddy/Caddyfile` 的 upstream。

当前 prod upstream：

```text
beta.copse.top page      -> 10.66.0.9:13000
beta.copse.top ws        -> 10.66.0.9:48080
admin.copse.top page     -> 10.66.0.9:8081
admin.copse.top admin-api -> 10.66.0.9:48080
admin.copse.top infra ws -> 10.66.0.9:48080
```

Caddy 修改后验证和重载：

```powershell
ssh ucould "docker exec caddy-proxy caddy validate --config /etc/caddy/Caddyfile"
ssh ucould "docker exec caddy-proxy caddy reload --config /etc/caddy/Caddyfile"
```

公网验证：

```powershell
curl.exe -k -sS -I "https://beta.copse.top/"
curl.exe -k -sS -I "https://admin.copse.top/"
curl.exe -k -sS "https://beta.copse.top/app-api/member/auth/email-login"
curl.exe -k -sS "https://admin.copse.top/admin-api/system/auth/login"
```

## 回滚

后端 prod 回滚短期做法：

1. 找到上一个稳定 commit SHA。
2. 如果有稳定 tag，直接用该 tag 触发 `yudao-micro-cicd-prod`。
3. 如果没有稳定 tag，先补 tag 并推到 Gitea。
4. 重新发布同一个 service。
5. 验证容器、Nacos 实例和业务接口。

创建稳定 tag 示例：

```powershell
git tag prod-stable-20260616-1600 <stable-commit-sha>
git push gitea prod-stable-20260616-1600
```

`aigc-community` 已支持镜像 tag 回滚：

```powershell
ssh manman2 "cd /opt/deploy/yudao-micro && MICRO_IMAGE_TAG=<old-commit> docker compose -f docker-compose-micro.yml up -d --no-build --no-deps --force-recreate aigc-community"
```

前端回滚见“前端发布”章节的旧 tag 命令。

数据库回滚：

- 如果还没有写入新数据，优先恢复发布前备份。
- 如果已经产生新数据，不要直接覆盖，先评估数据差异并写补偿 SQL。
- 回滚数据库后必须重启依赖新表结构的服务。

## 常见故障定位

服务不可用：

```powershell
ssh manman2 "cd /opt/deploy/yudao-micro && docker compose -f docker-compose-micro.yml ps"
ssh manman2 "ls -lah /opt/data/yudao-logs-prod"
ssh manman2 "docker logs --tail=200 yudao-gateway-prod"
```

Nacos 注册异常：

```powershell
curl.exe -sS "http://111.228.39.103:8848/nacos/v1/ns/instance/list?namespaceId=prod&groupName=DEFAULT_GROUP&serviceName=infra-server"
```

Redis AUTH 错误：

```powershell
ssh manman2 "docker exec yudao-redis-prod redis-cli ping"
ssh manman2 "docker exec yudao-redis-prod redis-cli CONFIG GET requirepass"
```

前端打到错误环境：

```powershell
ssh manman2 "cd /opt/code && cat .frontend-prod.env"
ssh manman2 "cd /opt/code && docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml ps"
ssh ucould "grep -n '10.66.0.9' /opt/caddy/Caddyfile"
```

workflow 一直排队：

- test 检查 runner `manman` 是否在线。
- prod 检查 runner `manman2-prod` 是否在线。
- runner 所在机器必须能执行 `docker version`、`docker compose version`、`mvn -version`。

## 发布结果写回模板

每次发布完成后，把下面内容写回对应 Gitea issue：

```text
发布结论:
目标环境:
发布版本:
发布策略:
构建结果:
测试结果:
部署步骤:
健康检查结果:
监控/日志入口:
回滚方案:
风险项:
下一步建议:
```

标签规则：

- 发布成功且健康检查通过：改为 `devops:done` 并关闭工单。
- 发布失败：改为 `devops:failed`，并创建上线问题工单，标记 `po:pending`。
