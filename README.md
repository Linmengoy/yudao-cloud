# Manman AIGC 多模态创作平台

Manman 是基于芋道 Spring Cloud 脚手架二次开发的 AIGC 多模态创作平台。当前项目已经从通用后台脚手架演进为面向「文生图、图生图、文生视频、图生视频、资产管理、画布编排、社区分发、计费与安全审核」的一体化系统。

## 项目定位

- 面向创作者提供 Draw2Video 创作工作台，支持在画布中组织提示词、图片、草图、视频和生成结果。
- 面向运营与管理员提供后台管理，覆盖模型渠道、生成任务、资产库、社区内容、安全审核、计费和系统配置。
- 面向生产交付提供微服务化部署、Gitea Actions 发布流水线、Docker Compose 编排和发布门禁证据。

## 核心能力

- AIGC 生成：文本、图片、视频、音频、文档等生成任务统一提交、回调、同步、重试和状态跟踪。
- 多模型接入：通过模型、供应商、渠道和候选策略管理不同 AIGC 服务商。
- 创作画布：支持图片节点、草图节点、提示词节点、结果节点、视频节点和节点连线编排。
- 资产中心：统一管理上传资产和生成资产，支持对象存储、缩略图、签名访问 URL、下载记录和可见性控制。
- 社区内容：支持作品发布、点赞、评论、分享、关注、作者统计和审核日志。
- 安全与计费：提供提示词安全检查、任务计费冻结/确认/释放、用量记录和失败退款流程。
- 生产发布：提供微服务镜像构建、Compose 部署、健康检查、网关 smoke test、DB 证据和回滚版本校验。

## 技术栈

### 后端

- JDK 17
- Spring Boot 3.4.x
- Spring Cloud Alibaba
- Nacos
- Spring Cloud Gateway
- MyBatis Plus
- Redis / Redisson
- MySQL
- Flowable
- XXL-Job
- Knife4j / Swagger

### 前端

- Draw2Video Client：Next.js、React、TypeScript、Tailwind CSS、XYFlow
- Draw2Video Admin：Vue 3、Vite、TypeScript、Element Plus
- Guide 文档站：Docusaurus

### 部署与基础设施

- Docker / Docker Compose
- Gitea Actions
- 对象存储文件服务
- Nacos 配置中心
- Redis 缓存与分布式锁
- MySQL 业务库

## 主要模块

| 模块 | 说明 |
| --- | --- |
| `yudao-gateway` | 微服务网关 |
| `yudao-server` | 单体启动与管理后台聚合服务 |
| `yudao-module-aigc-gen` | AIGC 生成任务、回调、同步、重试和结果资产化 |
| `yudao-module-aigc-model` | 模型、供应商、渠道、价格和候选策略 |
| `yudao-module-aigc-asset` | AIGC 资产、对象存储、缩略图、签名 URL 和下载 |
| `yudao-module-aigc-task` | AIGC 任务生命周期与统计 |
| `yudao-module-aigc-billing` | 计费冻结、确认、释放和用量记录 |
| `yudao-module-aigc-safety` | 内容安全、提示词审核和策略 |
| `yudao-module-aigc-workflow` | AIGC 工作流编排 |
| `yudao-module-aigc-community` | 社区作品、互动、关注和审核 |
| `yudao-ui/draw2video-client` | 创作者端画布与工作台 |
| `yudao-ui/draw2video-admin` | 管理后台 |
| `script/docker` | 本地与生产 Docker Compose、发布校验脚本 |

## 本地开发

### 后端构建

```bash
mvn clean package -DskipTests
```

构建单个服务示例：

```bash
mvn clean package -pl yudao-module-aigc-gen/yudao-module-aigc-gen-server -am -DskipTests
```

### 前端开发

创作者端：

```bash
cd yudao-ui/draw2video-client
npm install
npm run dev
```

管理后台：

```bash
cd yudao-ui/draw2video-admin
npm install
npm run dev
```

## 发布说明

生产发布流水线位于 `.gitea/workflows/yudao-micro-cicd-prod.yml`。生产发布必须提供上一稳定镜像 tag，并通过以下门禁：

- 回滚版本存在且为 Git SHA tag。
- 社区库发布必须提供 DB 备份、SHA256、执行窗口、验证 SQL、回滚演练和服务健康证据。
- `aigc-community` 发布必须通过服务健康检查和网关 smoke test。
- 发布证据必须记录 run URL、commit SHA、镜像 tag、上一稳定 tag、回滚命令和验证输出。

测试环境镜像版本由 `script/docker/test-image-version` 管理，当前从 `v0.0.1` 开始。

## 数据与文件原则

- 数据库只保存对象存储地址、资产编号、元数据和访问控制信息。
- 生成结果如果是 base64 或第三方 URL，会先转存到对象存储，再作为资产记录保存。
- 前端画布临时数据可以保存在浏览器 IndexedDB，但服务端持久化只记录可恢复的资产引用。

## 许可证

本项目基于原芋道开源项目二次开发。使用时请同时遵守原项目许可证和本仓库后续补充的授权说明。
