# yudao-module-aigc-gen 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置
4. 当前仓库已落地 `yudao-module-aigc-gen` 的 `api` 与 `server` 两个子模块，根工程已接入，已完成基础生成编排、模型校验、提示词检查、计费冻结、任务创建、渠道适配抽象、Mock 渠道、`gpt-image-2` 渠道、回调记录、同步补偿、资产创建、扣费确认、用量计量、MySQL 建表脚本和 Swagger 分组配置

## 1. 模块定位

`yudao-module-aigc-gen` 是 AIGC 平台的统一生成服务，负责承接用户侧文本、图片、视频、音频、代码、文档、PPT、数字人等生成请求，并统一完成第三方模型调用适配、生成任务提交、第三方任务查询、回调处理、结果下载、结果解析和跨服务编排。

当前实现已落地 `api + server` 双子模块，Server 服务名为 `aigc-gen-server`，默认端口为 `48095`。当前用户端显式入口主要覆盖通用提交、文本生成、文生图、文生视频和结果查询；音频、代码、文档、PPT、数字人等能力属于后续按模型能力继续扩展的规划入口。

该模块是 AIGC 赚钱闭环中用户直接感知最强的服务，但不独立维护模型配置、价格规则、钱包账务、统一任务状态机、文件资产元数据和审核策略。它通过调用 `aigc-model`、`aigc-task`、`aigc-billing`、`aigc-asset`、`aigc-safety` 等模块完成生成链路的前置校验、计费冻结、状态推进、结果入库和安全审核。

`aigc-gen` 的核心抽象是“生成执行与渠道适配”，不是“模型中台”，也不是“任务状态机”。模型中台负责可用模型、渠道、参数和价格；任务服务负责生命周期状态事实源；生成服务负责把业务请求转换为具体渠道调用，并把渠道结果回填到任务和资产。

## 2. 核心职责

### 2.1 负责内容

- 文本生成、对话、摘要、翻译
- 文生图、图生图
- 文生视频、图生视频
- 文本转语音、语音转文本、音乐生成
- 代码生成、代码审查
- 文档、PPT、数字人等扩展生成能力
- 用户端生成入口
- 管理端生成记录查询和运维监控
- 模型调用客户端抽象
- 第三方渠道适配器
- 第三方任务提交
- 第三方任务状态同步
- 第三方回调接收、验签、解析和幂等处理
- 生成结果文件下载
- 调用资产服务创建文件型资产
- 调用任务服务推进任务状态
- 调用计费服务冻结、确认扣费和失败释放
- 调用模型服务做模型校验、参数校验、价格预估和调用计量
- 调用审核服务做提示词安全检查和生成后审核记录创建

### 2.2 不负责内容

- 不维护模型供应商、模型基础配置、参数模板和价格规则
- 不直接保存渠道商密钥明文
- 不作为任务状态事实源
- 不直接修改钱包余额和冻结余额
- 不直接管理充值、支付、退款和账务流水
- 不作为文件资产中心
- 不直接实现底层对象存储
- 不维护敏感词规则和审核策略
- 不承接社区发布、模板市场、创作者收益等后续业务

对应职责归属：

| 能力 | 归属模块 |
| ---- | -------- |
| 模型、渠道、参数、价格、路由、计量 | `yudao-module-aigc-model` |
| 统一任务状态机、任务日志、回调记录、重试补偿 | `yudao-module-aigc-task` |
| 钱包、冻结、扣费、退款、成本、毛利 | `yudao-module-aigc-billing` |
| 图片、视频、音频、文档等文件资产元数据 | `yudao-module-aigc-asset` |
| 敏感词、提示词检查、审核记录、人工审核 | `yudao-module-aigc-safety` |
| 文件上传、对象存储、文件访问 URL | `yudao-module-infra` |

## 3. 模块结构

### 3.1 Maven 结构

```text
yudao-module-aigc-gen
  ├── yudao-module-aigc-gen-api
  └── yudao-module-aigc-gen-server
```

命名规则遵循当前项目规范：

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-gen` |
| 聚合 artifactId | `yudao-module-aigc-gen` |
| API 子模块 artifactId | `yudao-module-aigc-gen-api` |
| Server 子模块 artifactId | `yudao-module-aigc-gen-server` |
| Spring 应用名 | `aigc-gen-server` |

### 3.2 根包名

```text
cn.iocoder.yudao.module.aigc.gen
```

### 3.3 API 模块结构

```text
yudao-module-aigc-gen-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/gen
      ├── api
      │   ├── AigcGenerateApi.java
      │   └── AigcGenerateApi.java
      ├── dto
      │   ├── AigcGenerateSubmitReqDTO.java
      │   ├── AigcGenerateSubmitRespDTO.java
      │   ├── AigcGenerateResultRespDTO.java
      │   ├── AigcGenerateCallbackReqDTO.java
      │   ├── AigcGenerateSyncReqDTO.java
      │   └── AigcGenerateProviderRespDTO.java
      └── enums
          ├── AigcGenerateTypeEnum.java
          ├── AigcGenerateModeEnum.java
          ├── AigcGenerateStatusEnum.java
          ├── AigcGenerateResultTypeEnum.java
          ├── AigcGenerateProviderTaskStatusEnum.java
          ├── AigcGenerateCallbackStatusEnum.java
          ├── AigcGenerateFailReasonEnum.java
          ├── ApiConstants.java
          └── ErrorCodeConstants.java
```

当前 API 模块以通用 `AigcGenerateApi` 为主，文本、图片、视频等垂直 API 可在对应业务复杂度上升后再拆出。

### 3.4 Server 模块结构

```text
yudao-module-aigc-gen-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/gen
      ├── AigcGenServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── callback
      │   │   ├── providerlog
      │   │   └── record
      │   └── app
      │       └── AigcGenerateAppController.java
      ├── api
      │   └── AigcGenerateApiImpl.java
      ├── framework
      │   ├── client
      │   │   ├── AigcProviderClient.java
      │   │   ├── AigcProviderClientFactory.java
      │   │   ├── MockAigcProviderClient.java
      │   │   ├── GptImageProviderClient.java
      │   │   └── dto
      │   ├── security
      │   └── web
      │       └── config
      │           └── AigcGenWebConfiguration.java
      ├── service
      │   └── record
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcGenerateRecordDO.java
      │   │   ├── AigcGenerateCallbackDO.java
      │   │   └── AigcGenerateProviderLogDO.java
      │   └── mysql
      │       ├── AigcGenerateRecordMapper.java
      │       ├── AigcGenerateCallbackMapper.java
      │       └── AigcGenerateProviderLogMapper.java
      ├── job
      │   └── AigcGenerateSyncJob.java
      └── convert
```

当前 Server 模块以通用生成编排服务 `AigcGenerateRecordServiceImpl` 为核心，垂直生成类型通过 `generateType`、`generateMode` 和统一 DTO 承载，后续如图片、视频、音频复杂度提升，可再拆分独立 service 与 controller。

## 4. 依赖设计

### 4.1 API 模块依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 4.2 Server 模块依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-env</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-system-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-infra-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-gen-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-model-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-task-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-billing-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-asset-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-safety-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-biz-tenant</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-mybatis</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-rpc</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-job</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-monitor</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 4.3 被依赖关系

| 调用方 | 调用目的 |
| ------ | -------- |
| 用户端前端 | 提交通用生成、文本生成、文生图、文生视频等生成请求 |
| 管理端前端 | 查询生成记录、回调记录、渠道调用日志并手动同步任务 |
| `yudao-module-aigc-task` | 超时补偿时可调用生成服务同步第三方任务状态 |
| 后续 `yudao-module-aigc-workflow` | 工作流节点调用生成能力 |
| 后续 `yudao-module-aigc-template` | 模板生成调用生成能力 |

## 5. 数据库设计

### 5.1 表清单

| 表名 | 说明 |
| ---- | ---- |
| `aigc_gen_record` | 通用生成记录，当前已落地 |
| `aigc_gen_callback` | 第三方回调记录，保存回调原文、验签结果和处理状态，当前已落地 |
| `aigc_gen_provider_log` | 第三方渠道调用日志，记录请求摘要、响应摘要、耗时和错误，当前已落地 |
| `aigc_image` | 图片生成记录，可作为垂直扩展表，当前未落地 |
| `aigc_video` | 视频生成记录，可作为垂直扩展表，当前未落地 |
| `aigc_audio` | 音频生成记录，可作为垂直扩展表，当前未落地 |
| `aigc_text` | 文本生成记录，可作为垂直扩展表，当前未落地 |

第一阶段已落地 `aigc_gen_record`、`aigc_gen_callback`、`aigc_gen_provider_log` 三张通用表，图片、视频、音频、文本垂直表可在对应业务复杂度上升后扩展。

### 5.2 aigc_gen_record

`aigc_gen_record` 用于记录一次生成请求在生成服务内的执行上下文，重点保存模型、渠道、任务、计费、第三方任务、输入输出摘要和错误信息。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `task_id` | bigint | AIGC 统一任务 ID |
| `user_id` | bigint | 用户编号 |
| `generate_no` | varchar | 生成流水号 |
| `generate_type` | varchar | 生成类型，文本、图片、视频、音频、代码、文档等 |
| `generate_mode` | varchar | 生成模式，如文生图、图生图、文生视频、文本生成等 |
| `model_id` | bigint | 模型 ID |
| `model_code` | varchar | 模型编码快照 |
| `provider_id` | bigint | 渠道商 ID |
| `provider_code` | varchar | 渠道编码快照 |
| `provider_task_id` | varchar | 第三方任务编号 |
| `provider_status` | varchar | 第三方任务状态 |
| `status` | varchar | 生成服务内部状态 |
| `input_params` | json | 输入参数快照 |
| `input_summary` | varchar | 输入摘要 |
| `output_text` | text | 文本型输出 |
| `output_data` | json | 结构化输出 |
| `output_urls` | json | 第三方结果 URL 列表 |
| `asset_ids` | json | 资产 ID 列表 |
| `freeze_id` | bigint | 冻结记录 ID |
| `price_amount` | decimal | 用户计费金额快照 |
| `cost_amount` | decimal | 渠道成本金额快照 |
| `submit_time` | datetime | 提交第三方时间 |
| `callback_time` | datetime | 回调时间 |
| `finish_time` | datetime | 完成时间 |
| `fail_reason` | varchar | 失败原因类型 |
| `fail_message` | varchar | 失败信息 |
| `creator` | varchar | 创建者 |
| `create_time` | datetime | 创建时间 |
| `updater` | varchar | 更新者 |
| `update_time` | datetime | 更新时间 |
| `deleted` | bit | 是否删除 |
| `tenant_id` | bigint | 租户编号 |

建议索引：

| 索引 | 字段 | 说明 |
| ---- | ---- | ---- |
| `uk_tenant_generate_no` | `tenant_id, generate_no` | 保证生成流水号唯一 |
| `uk_tenant_task` | `tenant_id, task_id` | 保证任务与生成记录一对一 |
| `idx_tenant_user_type_time` | `tenant_id, user_id, generate_type, create_time` | 用户端按生成类型查询 |
| `idx_tenant_status_time` | `tenant_id, status, create_time` | 管理端查询失败、执行中任务 |
| `idx_tenant_provider_task` | `tenant_id, provider_code, provider_task_id` | 回调和同步时定位记录 |

### 5.3 aigc_gen_callback

`aigc_gen_callback` 用于保存第三方渠道回调原文和处理结果，作为回调幂等、问题排查和补偿依据。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `record_id` | bigint | 生成记录 ID |
| `task_id` | bigint | AIGC 统一任务 ID |
| `provider_code` | varchar | 渠道编码 |
| `provider_task_id` | varchar | 第三方任务编号 |
| `callback_type` | varchar | 回调类型 |
| `callback_no` | varchar | 回调唯一编号或消息 ID |
| `signature_valid` | bit | 签名是否通过 |
| `raw_body` | text | 回调原文 |
| `parsed_data` | json | 解析后的结构化数据 |
| `process_status` | varchar | 处理状态 |
| `process_message` | varchar | 处理说明 |
| `process_time` | datetime | 处理时间 |
| `creator` | varchar | 创建者 |
| `create_time` | datetime | 创建时间 |
| `updater` | varchar | 更新者 |
| `update_time` | datetime | 更新时间 |
| `deleted` | bit | 是否删除 |
| `tenant_id` | bigint | 租户编号 |

建议索引：

| 索引 | 字段 | 说明 |
| ---- | ---- | ---- |
| `uk_tenant_provider_callback` | `tenant_id, provider_code, callback_no` | 回调消息幂等 |
| `idx_tenant_provider_task` | `tenant_id, provider_code, provider_task_id` | 根据第三方任务查回调 |
| `idx_tenant_task` | `tenant_id, task_id` | 根据任务查回调记录 |
| `idx_tenant_status_time` | `tenant_id, process_status, create_time` | 查询待处理和失败回调 |

### 5.4 aigc_gen_provider_log

`aigc_gen_provider_log` 用于记录调用第三方渠道的请求和响应摘要，避免在业务日志中泄漏密钥和完整敏感内容。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `record_id` | bigint | 生成记录 ID |
| `task_id` | bigint | AIGC 统一任务 ID |
| `provider_code` | varchar | 渠道编码 |
| `model_code` | varchar | 模型编码 |
| `api_action` | varchar | 调用动作，如 submit、query、cancel、download |
| `request_id` | varchar | 请求编号 |
| `request_summary` | json | 请求摘要，不能包含密钥 |
| `response_summary` | json | 响应摘要 |
| `success` | bit | 是否成功 |
| `http_status` | int | HTTP 状态码 |
| `error_code` | varchar | 错误码 |
| `error_message` | varchar | 错误信息 |
| `duration_ms` | bigint | 调用耗时 |
| `creator` | varchar | 创建者 |
| `create_time` | datetime | 创建时间 |
| `tenant_id` | bigint | 租户编号 |

建议索引：

| 索引 | 字段 | 说明 |
| ---- | ---- | ---- |
| `idx_tenant_record` | `tenant_id, record_id` | 查询生成记录的渠道调用明细 |
| `idx_tenant_provider_time` | `tenant_id, provider_code, create_time` | 渠道调用统计 |
| `idx_tenant_success_time` | `tenant_id, success, create_time` | 失败调用排查 |

## 6. API 设计

### 6.1 内部 RPC API

`AigcGenerateApi` 是当前已落地的内部 RPC API：

```text
submit(userId, req)
getResult(taskId)
handleCallback(providerCode, callbackData)
syncTask(taskId)
```

以下垂直 API 属于后续规划，可在通用编排稳定后按业务需要拆出：

`AigcTextGenerateApi`：

```text
generateText(userId, req)
chat(userId, req)
summarize(userId, req)
translate(userId, req)
```

`AigcImageGenerateApi`：

```text
textToImage(userId, req)
imageToImage(userId, req)
getImage(id)
```

`AigcVideoGenerateApi`：

```text
textToVideo(userId, req)
imageToVideo(userId, req)
handleCallback(providerCode, callbackData)
syncVideoTask(taskId)
```

`AigcAudioGenerateApi`：

```text
textToSpeech(userId, req)
speechToText(userId, req)
musicGenerate(userId, req)
```

`AigcCodeGenerateApi`：

```text
generateCode(userId, req)
reviewCode(userId, req)
```

`AigcDocumentGenerateApi`：

```text
generateDocument(userId, req)
generatePpt(userId, req)
```

### 6.2 用户端接口

用户端接口由 `controller.app` 提供，服务内路径不手写 `/app-api` 前缀。

| 方法 | 服务内路径 | 说明 |
| ---- | ---------- | ---- |
| POST | `/aigc/gen/submit` | 通用生成任务提交 |
| POST | `/aigc/gen/text/generate` | 文本生成 |
| POST | `/aigc/gen/image/text-to-image` | 文生图 |
| POST | `/aigc/gen/video/text-to-video` | 文生视频 |
| GET | `/aigc/gen/result` | 查询生成结果 |

`chat`、摘要、翻译、图生图、图生视频、音频、代码、文档和 PPT 等独立路径属于后续扩展入口，当前可优先通过通用 `/aigc/gen/submit` 按 `generateType` 与 `generateMode` 承载。

### 6.3 管理端接口

| 方法 | 服务内路径 | 说明 |
| ---- | ---------- | ---- |
| GET | `/aigc/gen/record/page` | 生成记录分页 |
| GET | `/aigc/gen/record/get` | 生成记录详情 |
| GET | `/aigc/gen/callback/page` | 回调记录分页 |
| GET | `/aigc/gen/provider-log/page` | 渠道调用日志分页 |
| POST | `/aigc/gen/record/sync` | 手动同步第三方任务 |

### 6.4 第三方回调接口

第三方渠道回调当前通过内部 RPC `AigcGenerateApi.handleCallback` 承接，后续如需要直接暴露外部 HTTP 回调，可按渠道区分路径，便于单独验签和解析。

| 方法 | 服务内路径 | 说明 |
| ---- | ---------- | ---- |
| POST | `/aigc/gen/callback/{providerCode}` | 通用渠道回调入口 |
| POST | `/aigc/gen/callback/{providerCode}/image` | 图片生成回调入口 |
| POST | `/aigc/gen/callback/{providerCode}/video` | 视频生成回调入口 |
| POST | `/aigc/gen/callback/{providerCode}/audio` | 音频生成回调入口 |

回调接口必须满足：

- 保存回调原文
- 校验渠道签名
- 根据 `providerCode + providerTaskId` 定位生成记录
- 使用 `providerCode + callbackNo` 或 `providerCode + providerTaskId + callbackType` 做幂等
- 不在响应中暴露内部任务、账务、资产细节

## 7. 核心流程

### 7.1 用户提交生成任务

```text
用户端
  -> aigc-gen 接收生成请求
  -> aigc-safety.checkPrompt 检查提示词
  -> aigc-model.validateModel 校验模型可用性
  -> aigc-model.validateParams 校验参数合法性
  -> aigc-model.calculatePrice 计算价格
  -> aigc-billing.freeze 冻结积分
  -> aigc-task.createTask 创建统一任务
  -> aigc-gen 创建 aigc_gen_record
  -> aigc-gen 调用第三方渠道提交任务
  -> aigc-task.updateStatus 推进到 SUBMITTED 或 CALLBACK_WAITING
  -> 返回 taskId、generateNo、状态
```

关键约束：

- 价格计算结果必须快照到任务和生成记录
- 冻结成功后才能提交第三方渠道
- 第三方提交失败时必须标记任务失败并触发冻结释放
- 生成服务不直接修改钱包余额，只调用计费 API

### 7.2 同步生成流程

短文本、摘要、翻译、代码生成等可走同步生成。

```text
aigc-gen 提交第三方同步请求
  -> 第三方返回生成结果
  -> aigc-gen 写入 outputText 或 outputData
  -> aigc-task.markSuccess 回写结果
  -> aigc-billing.confirm 确认扣费
  -> aigc-model.recordUsage 记录模型调用计量
  -> 返回生成结果和任务状态
```

同步生成仍需创建任务和生成记录，保证用户端查询、账务、计量和运营统计口径一致。

### 7.3 异步生成流程

图片、视频、音乐、数字人、PPT、文档等长任务走异步流程。

```text
aigc-gen 提交第三方异步任务
  -> 第三方返回 providerTaskId
  -> aigc-gen 保存 providerTaskId
  -> aigc-task 标记 CALLBACK_WAITING
  -> 第三方回调或定时同步
  -> aigc-gen 解析结果
  -> 文件型结果进入下载和资产创建
  -> 非文件型结果直接回写任务输出
  -> aigc-task 标记 SUCCESS
  -> aigc-billing.confirm 确认扣费
  -> aigc-model.recordUsage 记录模型调用计量
```

### 7.4 文件型结果入库流程

```text
aigc-gen 获取第三方结果 URL
  -> 校验 URL 来源和有效期
  -> 转交 aigc-asset 创建资产
  -> aigc-asset.createAsset 创建资产
  -> aigc-task 回写 outputAssetIds
  -> aigc-safety.createAuditRecord 创建审核记录
  -> aigc-task 标记 SUCCESS
```

当前代码以结果 URL 安全校验和资产创建为主，文件下载转存、生成后审核状态联动可继续在 `aigc-asset` 与 `aigc-safety` 中完善。

### 7.5 回调成功流程

```text
第三方渠道回调 aigc-gen
  -> 保存 aigc_gen_callback
  -> 验签
  -> 幂等校验
  -> 解析 providerTaskId 和结果状态
  -> 查询 aigc_gen_record
  -> 调用 aigc-task.createCallbackRecord
  -> 根据结果创建资产或回写文本输出
  -> aigc-task.markSuccess
  -> aigc-billing.confirm
  -> aigc-model.recordUsage
```

回调处理需要保证“至少一次回调”场景下不重复扣费、不重复创建资产、不重复推进终态。

### 7.6 失败退款流程

```text
第三方提交失败、回调失败或同步失败
  -> aigc-gen 记录失败原因
  -> aigc-task.markFailed
  -> aigc-task 或 aigc-billing 进入退款处理
  -> aigc-billing.release 释放冻结积分
  -> aigc-task 标记 REFUNDED
```

失败场景中，生成服务只负责识别失败和上报失败，不直接操作账务表。

### 7.7 超时补偿流程

```text
AigcGenerateSyncJob 扫描长时间 CALLBACK_WAITING 的生成记录
  -> 调用第三方查询任务状态
  -> 成功则走结果入库流程
  -> 失败则走失败退款流程
  -> 仍执行中则更新时间或等待下一轮
  -> 超过最大等待时间则标记失败
```

补偿任务需要与 `aigc-task` 的超时补偿保持幂等，建议由任务服务发现超时后调用 `AigcGenerateApi.syncTask(taskId)`，生成服务也可保留本地扫描作为兜底。

## 8. 状态设计

### 8.1 生成服务内部状态

| 状态 | 说明 |
| ---- | ---- |
| `CREATED` | 已创建生成记录 |
| `SUBMITTING` | 正在提交第三方 |
| `SUBMITTED` | 已提交第三方 |
| `CALLBACK_WAITING` | 等待第三方回调 |
| `SYNCING` | 正在同步第三方状态 |
| `DOWNLOADING` | 正在下载结果文件 |
| `ASSET_CREATING` | 正在创建资产 |
| `SUCCESS` | 生成成功 |
| `FAILED` | 生成失败 |
| `CANCELLED` | 已取消 |

前置校验、计费冻结和审核属于生成链路动作，当前枚举未单独定义 `VALIDATING`、`FROZEN`、`AUDITING` 状态。

### 8.2 与任务状态关系

`aigc-gen` 内部状态用于生成服务排查和补偿，最终用户可见状态以 `aigc-task` 为准。

| gen 状态 | task 状态建议 |
| -------- | ------------- |
| `CREATED` | `CREATED` |
| `SUBMITTING` | `RUNNING` |
| `SUBMITTED` | `SUBMITTED` |
| `CALLBACK_WAITING` | `CALLBACK_WAITING` |
| `SYNCING` | `RUNNING` 或 `CALLBACK_WAITING` |
| `DOWNLOADING` | `DOWNLOADING` |
| `ASSET_CREATING` | `ASSET_CREATING` |
| `SUCCESS` | `SUCCESS` |
| `FAILED` | `FAILED` 或 `REFUNDING` |
| `CANCELLED` | `CANCELLED` |

## 9. 第三方渠道适配设计

### 9.1 客户端抽象

第三方渠道统一抽象为 `AigcProviderClient`，当前接口提供提交、查询和回调验签能力：

```text
AigcProviderClient
  ├── submit(req)
  ├── query(providerTaskId)
  └── verifyCallback(rawBody, signature)
```

不同渠道通过 `providerCode` 注册到 `AigcProviderClientFactory`，生成服务根据模型服务返回的渠道配置选择客户端。

当前已实现 `mock` 与 `gpt-image-2` 两个客户端。`cancel`、独立回调解析、结果下载等能力属于后续渠道增强项，可在真实渠道接入时扩展。

### 9.2 渠道适配原则

- 请求参数在进入客户端前先转换为平台统一 DTO
- 渠道客户端只关心第三方协议，不处理钱包、任务、资产等业务逻辑
- 渠道密钥只从模型服务或安全配置中读取，不能写入日志、回调记录或异常堆栈
- 渠道响应统一转换为平台结果对象
- 第三方错误码需要映射为平台失败原因
- 不同渠道的超时、重试、限流策略可独立配置

### 9.3 支持能力矩阵

| 能力 | 同步 | 异步 | 文件型结果 | 第一阶段建议 |
| ---- | ---- | ---- | ---------- | ------------ |
| 文本生成 | 是 | 可选 | 否 | 优先支持 |
| 对话 | 是 | 可选 | 否 | 优先支持 |
| 摘要翻译 | 是 | 否 | 否 | 优先支持 |
| 文生图 | 可选 | 是 | 是 | 优先支持 |
| 图生图 | 可选 | 是 | 是 | 优先支持 |
| 文生视频 | 否 | 是 | 是 | 优先支持 |
| 图生视频 | 否 | 是 | 是 | 优先支持 |
| 文本转语音 | 可选 | 是 | 是 | 可选支持 |
| 语音转文本 | 是 | 可选 | 否 | 可选支持 |
| 代码生成 | 是 | 可选 | 否 | 可选支持 |
| 文档/PPT | 否 | 是 | 是 | 后续支持 |

## 10. 计费与幂等设计

### 10.1 计费原则

- 所有收费生成都必须先调用模型服务计算价格
- 所有收费生成都必须先冻结积分再提交第三方
- 第三方提交失败、回调失败、超时失败需要释放冻结积分
- 生成成功后才能确认扣费
- 成本记录需要在渠道调用成功后根据模型、渠道和第三方用量写入
- 生成服务不直接写钱包、冻结、流水和成本表

### 10.2 幂等键设计

| 场景 | 幂等键 | 说明 |
| ---- | ------ | ---- |
| 用户提交 | `tenant_id + user_id + client_request_id` | 防止前端重复提交 |
| 任务创建 | `tenant_id + task_id` | 防止重复创建生成记录 |
| 第三方回调 | `tenant_id + provider_code + callback_no` | 防止重复处理回调消息 |
| 第三方任务 | `tenant_id + provider_code + provider_task_id` | 定位第三方任务 |
| 资产创建 | `tenant_id + task_id + asset_type` | 防止重复创建资产 |
| 扣费确认 | `freeze_id` | 防止重复扣费 |

## 11. 安全设计

### 11.1 提示词安全

生成任务创建前必须调用 `aigc-safety.checkPrompt`。命中高风险敏感词时直接拒绝创建任务；命中中低风险时可根据策略进入人工审核或继续生成并记录风险。

### 11.2 回调安全

- 每个渠道独立实现验签逻辑
- 回调原文只保存业务必要内容
- 回调处理失败不能返回内部堆栈
- 回调接口需要限流和异常监控
- 对无法验签的回调只记录，不推进任务状态

### 11.3 文件安全

- 第三方结果 URL 下载前需要校验协议、域名和文件大小
- 禁止下载内网地址和本机地址
- 下载文件需要限制超时、大小和 MIME 类型
- 文件入库后以平台文件 URL 为准，不直接长期暴露第三方临时 URL

### 11.4 日志安全

- 不记录渠道密钥、Authorization、签名密钥和完整鉴权头
- 不记录用户完整隐私输入，长文本只保存摘要或脱敏内容
- 错误日志不输出第三方密钥和内部配置
- 管理端展示错误信息时区分用户可见原因和内部排查信息

## 12. OpenAPI 与网关接入

`aigc-gen-server` 当前通过 `AigcGenWebConfiguration` 注册独立 OpenAPI 分组 `aigc-gen`，Gateway Knife4j 聚合配置按网关侧统一接入。

当前分组：

| 分组 | 路径 | 说明 |
| ---- | ---- | ---- |
| `aigc-gen` | `/aigc/gen/**`、`/rpc-api/aigc/gen/**` | 生成服务用户端、管理端和内部 RPC 接口 |

网关路由需要保证：

- 用户端只开放 app 生成和查询接口
- 管理端接口需要登录和权限校验
- 第三方回调如开放外部 HTTP 入口，需要按渠道路径开放，并限制请求体大小
- RPC 接口仅供服务间调用

## 13. 第一阶段落地范围

### 13.1 必须落地

- `api + server` Maven 模块
- `AigcGenerateApi` 通用内部 API
- 文本生成用户端接口
- 图片生成用户端接口
- 视频生成用户端接口
- 通用生成记录表
- 回调记录表
- 渠道调用日志表
- 模型校验和价格预估接入
- 提示词安全检查接入
- 钱包冻结、确认扣费、失败释放接入
- 统一任务创建和状态推进接入
- 文件型结果资产创建接入
- 第三方回调验签和幂等
- 超时同步补偿任务
- 管理端生成记录和回调记录查询
- OpenAPI 分组

### 13.2 可以延后

- 音乐生成
- 数字人生成
- 文档和 PPT 生成
- 多渠道智能路由
- 渠道自动熔断和降级
- 高级成本毛利分析
- 复杂工作流编排
- 模板市场和社区发布
- 生成结果深度机审

## 14. 验收标准

### 14.1 功能验收

- 用户可以提交文本生成任务并获得文本结果
- 用户可以提交图片生成任务并在完成后得到资产 ID 和访问 URL
- 用户可以提交视频生成任务并通过回调或同步补偿完成状态推进
- 提示词命中敏感词时能阻断或记录审核风险
- 余额不足时不能提交收费生成任务
- 第三方失败时任务失败并释放冻结积分
- 第三方重复回调不会重复创建资产和重复扣费
- 管理端可以查询生成记录、回调记录和渠道调用日志

### 14.2 技术验收

- 根工程 `pom.xml` 接入 `yudao-module-aigc-gen`
- `application.yaml` 中 `spring.application.name` 为 `aigc-gen-server`
- Controller 不手写 `/admin-api`、`/app-api`
- API 模块不依赖 Server 模块
- Server 模块只通过 API 调用其他 AIGC 服务
- 数据表均包含租户隔离和项目标准审计字段
- 回调、扣费、资产创建具备幂等保护
- 日志不输出渠道密钥和敏感鉴权信息
- Swagger 分组已注册，Gateway Knife4j 聚合由网关侧统一接入

## 15. 与已落地模块的协作清单

| 协作模块 | 调用接口方向 | 关键数据 |
| -------- | ------------ | -------- |
| `aigc-model` | `aigc-gen -> aigc-model` | 模型、渠道、参数模板、价格、用量 |
| `aigc-task` | `aigc-gen -> aigc-task` | 任务 ID、任务状态、输出文本、输出资产、回调记录 |
| `aigc-billing` | `aigc-gen -> aigc-billing` | 冻结记录、确认扣费、释放冻结、成本记录 |
| `aigc-asset` | `aigc-gen -> aigc-asset` | 文件 URL、资产类型、来源任务、模型和渠道快照 |
| `aigc-safety` | `aigc-gen -> aigc-safety` | 提示词、场景、审核对象、审核结果 |
| `infra` | `aigc-gen -> infra` 或 `aigc-asset -> infra` | 文件上传、文件访问 URL、存储配置 |

## 16. 开发顺序建议

1. 创建 Maven 聚合模块、API 模块和 Server 模块
2. 接入根工程、Nacos、Gateway、Knife4j 和基础启动配置
3. 定义 API、DTO、枚举和错误码
4. 创建通用生成记录、回调记录和渠道调用日志表
5. 实现通用生成编排服务
6. 接入安全检查、模型校验、价格计算和计费冻结
7. 实现文本同步生成链路
8. 实现图片异步生成链路和回调处理
9. 实现视频异步生成链路和同步补偿
10. 接入资产创建、任务成功回写和扣费确认
11. 实现管理端查询、补偿任务和监控指标
12. 补充单元测试、集成测试和异常场景验证

## 17. 当前实现同步说明

当前代码已按本文档完成 `yudao-module-aigc-gen` 基础可运行闭环，核心文件如下：

| 类型 | 文件 | 说明 |
| ---- | ---- | ---- |
| API | `AigcGenerateApi` | 提交生成、查询结果、处理回调、同步任务 |
| DTO | `AigcGenerateSubmitReqDTO` | 支持生成类型、生成模式、提示词、同步标识、输出字段和计费快照 |
| 枚举 | `AigcGenerateStatusEnum` | 生成服务内部状态 |
| Server | `AigcGenServerApplication` | 独立微服务启动类 |
| 编排 | `AigcGenerateRecordServiceImpl` | 生成主链路编排服务 |
| 渠道 | `AigcProviderClient` | 第三方渠道适配接口 |
| 渠道 | `MockAigcProviderClient` | 可编译、可联调的 Mock 渠道实现 |
| 渠道 | `GptImageProviderClient` | `gpt-image-2` 图片生成渠道实现 |
| 补偿 | `AigcGenerateSyncJob` | 超时生成任务同步补偿任务 |
| 安全 | `AigcGenerateFileSecurityUtils` | 第三方结果 URL 安全校验 |
| SQL | `schema/mysql.sql` | `aigc_gen_record`、`aigc_gen_callback`、`aigc_gen_provider_log` |

当前实现的三张核心表统一使用 `aigc_gen_` 前缀：

| 实际表名 | 说明 |
| -------- | ---- |
| `aigc_gen_record` | 通用生成记录 |
| `aigc_gen_callback` | 第三方回调记录 |
| `aigc_gen_provider_log` | 第三方渠道调用日志 |

## 18. 详细接口约定

### 18.1 submit

`submit` 是内部 RPC 和用户端生成入口的核心方法，用于提交一次生成请求。

请求字段：

| 字段 | 必填 | 说明 |
| ---- | ---- | ---- |
| `userId` | 是 | 用户编号，用户端由登录上下文注入 |
| `clientRequestId` | 否 | 客户端请求编号，用于防重复提交 |
| `generateType` | 是 | 生成类型，如 `TEXT`、`IMAGE`、`VIDEO` |
| `generateMode` | 是 | 模型能力和生成模式，如 `TEXT_GENERATE`、`TEXT_TO_IMAGE` |
| `modelId` | 是 | 模型编号 |
| `providerId` | 否 | 渠道商编号，优先以模型服务返回的渠道为准 |
| `prompt` | 否 | 提示词，非空时调用审核服务检查 |
| `inputParams` | 否 | 输入参数 JSON |
| `sync` | 否 | 是否同步生成，文本类默认可同步 |

响应字段：

| 字段 | 说明 |
| ---- | ---- |
| `id` | 生成记录编号 |
| `taskId` | 统一任务编号 |
| `generateNo` | 生成流水号 |
| `status` | 生成状态 |

主要错误码：

| 错误码 | 场景 |
| ------ | ---- |
| `GENERATE_PROMPT_NOT_PASS` | 提示词审核不通过 |
| `GENERATE_PROVIDER_RESULT_INVALID` | 第三方结果 URL 不安全或结果不正确 |
| 上游错误码 | 模型不可用、余额不足、任务创建失败等由对应服务返回 |

### 18.2 handleCallback

`handleCallback` 用于接收第三方渠道回调，当前实现已支持回调保存、签名校验、任务回调记录、成功状态推进、失败退款。

请求字段：

| 字段 | 必填 | 说明 |
| ---- | ---- | ---- |
| `providerCode` | 是 | 渠道编码 |
| `providerTaskId` | 是 | 第三方任务编号 |
| `callbackNo` | 否 | 回调消息唯一编号 |
| `callbackType` | 否 | 回调类型 |
| `rawBody` | 否 | 回调原文 |
| `signature` | 否 | 渠道签名 |
| `resultStatus` | 否 | 回调结果状态，支持 `SUCCESS`、`FAILED` |
| `outputText` | 否 | 文本输出 |
| `outputData` | 否 | 结构化输出 |
| `outputUrls` | 否 | 文件型结果 URL JSON |
| `failReason` | 否 | 失败原因 |

幂等规则：

- 优先使用 `tenant_id + provider_code + callback_no` 防重复回调
- 通过 `provider_code + provider_task_id` 定位生成记录
- 已处理过的 `callbackNo` 直接返回成功，不重复扣费、不重复创建资产

## 19. 状态流转矩阵

| 当前状态 | 允许流转到 | 触发场景 |
| -------- | ---------- | -------- |
| `CREATED` | `SUBMITTING`、`FAILED` | 创建生成记录后提交渠道，或前置校验失败 |
| `SUBMITTING` | `SUBMITTED`、`CALLBACK_WAITING`、`SUCCESS`、`FAILED` | 第三方提交完成 |
| `SUBMITTED` | `CALLBACK_WAITING`、`SUCCESS`、`FAILED` | 渠道同步查询或回调 |
| `CALLBACK_WAITING` | `SYNCING`、`SUCCESS`、`FAILED` | 补偿同步、回调成功、回调失败 |
| `SYNCING` | `CALLBACK_WAITING`、`SUCCESS`、`FAILED` | 第三方任务查询结果 |
| `DOWNLOADING` | `ASSET_CREATING`、`FAILED` | 文件下载成功或失败 |
| `ASSET_CREATING` | `SUCCESS`、`FAILED` | 资产创建成功或失败 |
| `SUCCESS` | 不允许流转 | 终态 |
| `FAILED` | 不允许流转 | 终态 |
| `CANCELLED` | 不允许流转 | 终态 |

并发控制要求：

- 状态更新应优先使用 `id + status` 条件更新，避免重复回调导致终态被覆盖
- `SUCCESS`、`FAILED`、`CANCELLED` 是终态，不允许被普通同步任务覆盖
- 账务确认、资产创建、任务成功回写必须具备幂等键

## 20. 异常补偿策略

| 异常场景 | 处理策略 |
| -------- | -------- |
| 提示词审核失败 | 不创建生成记录或创建失败记录，不冻结积分 |
| 模型校验失败 | 不冻结积分，直接返回模型服务错误 |
| 价格计算失败 | 不冻结积分，直接返回模型服务错误 |
| 冻结失败 | 不创建第三方任务，直接返回计费服务错误 |
| 任务创建失败 | 调用计费释放冻结，记录失败原因 |
| 第三方提交失败 | 标记生成失败、任务失败、释放冻结 |
| 第三方已提交但本地状态更新失败 | 通过 `providerTaskId` 和补偿任务恢复状态 |
| 回调重复 | 基于 `callbackNo` 幂等返回，不重复创建资产和扣费 |
| 回调验签失败 | 保存回调记录但不推进任务状态 |
| 文件 URL 不安全 | 阻断资产创建，标记任务失败并释放冻结 |
| 资产创建失败 | 任务失败，释放冻结，等待人工或补偿处理 |
| 扣费确认失败 | 保留成功生成记录和任务状态，进入账务补偿和告警 |

## 21. 测试清单

| 测试类型 | 用例 |
| -------- | ---- |
| 编译测试 | `mvn -pl yudao-module-aigc-gen/yudao-module-aigc-gen-server -am -DskipTests compile` 必须通过 |
| 提交幂等 | 相同 `clientRequestId` 重复提交只生成一条记录 |
| 提示词审核 | 敏感词命中时不冻结、不提交第三方 |
| 模型校验 | 不可用模型返回错误 |
| 计费冻结 | 余额不足时不创建第三方任务 |
| 同步生成 | 文本生成可直接进入成功并确认扣费 |
| 异步生成 | 图片、视频提交后进入 `CALLBACK_WAITING` |
| 回调幂等 | 相同 `callbackNo` 重复回调不重复扣费 |
| 回调验签 | 签名错误只保存记录，不推进任务 |
| 资产安全 | 内网 URL、localhost URL 被拒绝 |
| 失败退款 | 第三方失败时释放冻结并标记任务退款完成 |
| 补偿任务 | 超时等待任务可被 `AigcGenerateSyncJob` 同步推进 |

## 22. 监控指标

| 指标 | 说明 |
| ---- | ---- |
| `aigc_gen_submit_total` | 生成提交总数 |
| `aigc_gen_submit_success_total` | 提交第三方成功数 |
| `aigc_gen_submit_failed_total` | 提交第三方失败数 |
| `aigc_gen_callback_total` | 回调总数 |
| `aigc_gen_callback_invalid_total` | 验签失败回调数 |
| `aigc_gen_success_total` | 生成成功数 |
| `aigc_gen_failed_total` | 生成失败数 |
| `aigc_gen_timeout_total` | 超时补偿任务数 |
| `aigc_gen_asset_create_failed_total` | 资产创建失败数 |
| `aigc_gen_billing_confirm_failed_total` | 扣费确认失败数 |
| `aigc_gen_provider_duration_ms` | 第三方渠道调用耗时 |

告警建议：

- 回调验签失败数短时间突增时告警
- 渠道失败率超过阈值时告警
- 等待回调任务超过最大等待时间时告警
- 扣费确认失败、资产创建失败必须告警
