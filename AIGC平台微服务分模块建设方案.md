# AIGC 平台微服务分模块建设方案

前提：

1. 租户模块会自动注入租户
2. api前缀有外部拦截器添加识别
3. swagger开发后的注释要集成到gateway上去

## 1. 方案定位

本方案基于 AIGC 平台 MVP 赚钱版目标，将系统从“单一独立模块”调整为“微服务分模块建设”。当前 `yudao-module-aigc-model` 已按 `api + server` 结构落地，本文同步补充模型服务的实际开发结果，并作为其他 AIGC 微服务继续建设时的边界参考。

核心目标：

```text
先上线赚钱，后持续扩展
```

第一阶段优先建设：

- 文本生成、对话、摘要、翻译等轻量大模型生成
- 图片生成
- 视频生成
- 音频生成、语音合成等可快速变现能力
- 模型管理
- 任务调度
- 资产管理
- 钱包计费
- 基础审核
- 管理后台

不复用旧 `yudao-module-ai`，新建全新的 AIGC 微服务体系。

## 2. 总体微服务架构

### 2.1 微服务模块清单

第一阶段建议拆成 6 个核心业务微服务：

```text
yudao-module-aigc-model      模型与渠道服务
yudao-module-aigc-task       任务调度服务
yudao-module-aigc-asset      资产中心服务
yudao-module-aigc-billing    计费钱包服务
yudao-module-aigc-gen        生成服务，统一承接大模型生成适配
yudao-module-aigc-safety     审核风控服务
```

后续扩展服务：

```text
yudao-module-aigc-workflow   工作流服务
yudao-module-aigc-template   模板服务
yudao-module-aigc-community  社区服务
yudao-module-aigc-creator    创作者服务
yudao-module-aigc-publish    发布导出服务
```

### 2.2 第一阶段推荐模块

第一阶段为了“快速上线赚钱”，建议只落地以下模块：


| 模块           | 是否第一阶段建设 | 说明                             |
| -------------- | ---------------- | -------------------------------- |
| aigc-model     | 是               | 渠道商、模型、参数模板、价格配置、租户授权、调用计量 |
| aigc-billing   | 是               | 钱包、积分冻结、扣费、退款、成本 |
| aigc-task      | 是               | 统一任务、状态机、回调、日志     |
| aigc-asset     | 是               | 图片、视频、音频、文档等文件资产管理 |
| aigc-gen       | 是               | 文本、图片、视频、音频等大模型生成适配 |
| aigc-safety    | 是               | 敏感词、基础审核                 |
| aigc-workflow  | 否               | 第二阶段建设                     |
| aigc-template  | 否               | 社区或模板市场阶段建设           |
| aigc-community | 否               | 第三阶段建设                     |
| aigc-publish   | 否               | 成片导出阶段建设                 |

## 3. 微服务命名规范

### 3.1 Maven 模块命名

每个 AIGC 微服务按当前项目已有模块规范建设：根目录是 Maven 聚合模块，目录名和聚合 `artifactId` 使用 `yudao-module-{domain}`；子模块使用 `yudao-module-{domain}-api` 和 `yudao-module-{domain}-server`；服务注册名 `spring.application.name` 去掉 `yudao-module-` 前缀，使用 `{domain}-server`。

示例：

```text
yudao-module-aigc-model
  ├── yudao-module-aigc-model-api
  └── yudao-module-aigc-model-server
```

对应关系：

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-model` |
| 聚合 artifactId | `yudao-module-aigc-model` |
| API 子模块 artifactId | `yudao-module-aigc-model-api` |
| Server 子模块 artifactId | `yudao-module-aigc-model-server` |
| Spring 应用名 | `aigc-model-server` |

完整第一阶段目录：

```text
yudao-module-aigc-model
  ├── yudao-module-aigc-model-api
  └── yudao-module-aigc-model-server

yudao-module-aigc-billing
  ├── yudao-module-aigc-billing-api
  └── yudao-module-aigc-billing-server

yudao-module-aigc-task
  ├── yudao-module-aigc-task-api
  └── yudao-module-aigc-task-server

yudao-module-aigc-asset
  ├── yudao-module-aigc-asset-api
  └── yudao-module-aigc-asset-server

yudao-module-aigc-gen
  ├── yudao-module-aigc-gen-api
  └── yudao-module-aigc-gen-server

yudao-module-aigc-safety
  ├── yudao-module-aigc-safety-api
  └── yudao-module-aigc-safety-server
```

第一阶段 Spring 应用名统一为：

| Maven Server artifactId | spring.application.name |
| ----------------------- | ----------------------- |
| `yudao-module-aigc-model-server` | `aigc-model-server` |
| `yudao-module-aigc-billing-server` | `aigc-billing-server` |
| `yudao-module-aigc-task-server` | `aigc-task-server` |
| `yudao-module-aigc-asset-server` | `aigc-asset-server` |
| `yudao-module-aigc-gen-server` | `aigc-gen-server` |
| `yudao-module-aigc-safety-server` | `aigc-safety-server` |

后续扩展模块也按同一规则命名，例如 `yudao-module-aigc-workflow-server` 对应 `aigc-workflow-server`，`yudao-module-aigc-template-server` 对应 `aigc-template-server`。

### 3.2 包名规范


| 微服务       | 根包名                                 |
| ------------ | -------------------------------------- |
| aigc-model   | `cn.iocoder.yudao.module.aigc.model`   |
| aigc-billing | `cn.iocoder.yudao.module.aigc.billing` |
| aigc-task    | `cn.iocoder.yudao.module.aigc.task`    |
| aigc-asset   | `cn.iocoder.yudao.module.aigc.asset`   |
| aigc-gen     | `cn.iocoder.yudao.module.aigc.gen`     |
| aigc-safety  | `cn.iocoder.yudao.module.aigc.safety`  |

### 3.3 URL 前缀规范

说明：代码中的 Controller 路径通常不直接写 `/admin-api`、`/app-api`，由网关或外部拦截器统一识别和补充前缀。以下表格中的用户端 URL 是对外访问口径，服务内实际路径以 Controller 为准。


| 微服务       | 管理端 URL      | 用户端 URL             |
| ------------ | --------------- | ---------------------- |
| aigc-model   | `/aigc/model`   | `/app-api/aigc/model`  |
| aigc-billing | `/aigc/billing` | `/app-api/aigc/wallet` |
| aigc-task    | `/aigc/task`    | `/app-api/aigc/task`   |
| aigc-asset   | `/aigc/asset`   | `/app-api/aigc/asset`  |
| aigc-gen     | `/aigc/gen`     | `/app-api/aigc/gen`    |
| aigc-safety  | `/aigc/safety`  | 暂不开放或仅内部调用   |

当前 `aigc-model` 已实现的服务内路径包括：

| 类型 | 服务内路径 | 说明 |
| ---- | ---------- | ---- |
| 管理端 | `/aigc/model` | 模型管理 |
| 管理端 | `/aigc/model/provider` | 渠道商管理 |
| 管理端 | `/aigc/model/param` | 参数模板管理 |
| 管理端 | `/aigc/model/price` | 价格规则管理 |
| 管理端 | `/aigc/model/route` | 路由规则管理 |
| 管理端 | `/aigc/model/tenant` | 租户模型授权管理 |
| 用户端 | `/aigc/model/get` | 获取当前租户可见模型详情 |
| 用户端 | `/aigc/model/list` | 获取当前租户可用模型列表 |
| 用户端 | `/aigc/model/price/calculate` | 价格预估 |
| 用户端 | `/aigc/model/param/list` | 获取模型参数模板 |

### 3.4 表名前缀规范

所有 AIGC 微服务表统一使用 `aigc_` 前缀。


| 微服务       | 表名前缀                                      |
| ------------ | --------------------------------------------- |
| aigc-model   | `aigc_model_`                                 |
| aigc-billing | `aigc_wallet`、`aigc_billing_`、`aigc_quota_` |
| aigc-task    | `aigc_task_`                                  |
| aigc-asset   | `aigc_asset_`                                 |
| aigc-gen     | `aigc_image`、`aigc_video`                    |
| aigc-safety  | `aigc_audit_`、`aigc_sensitive_`              |

### 3.5 根工程接入规范

新增 AIGC 微服务时，需要同时接入根工程和本模块聚合工程：

| 位置 | 配置要求 |
| ---- | -------- |
| 根工程 `pom.xml` | 在 `<modules>` 中加入 `yudao-module-aigc-xxx` |
| 聚合模块 `pom.xml` | `artifactId` 等于目录名，`packaging` 为 `pom` |
| 聚合模块 `modules` | 至少包含 `yudao-module-aigc-xxx-api`、`yudao-module-aigc-xxx-server` |
| API 模块 | 只放跨服务 API、DTO、枚举、错误码，不依赖 server |
| Server 模块 | 放 Controller、Service、DAL、Convert、配置、SQL、测试 |
| `application.yaml` | `spring.application.name` 使用 `aigc-xxx-server` |

例如后续创建 `aigc-task` 时，根工程加入 `yudao-module-aigc-task`，聚合模块下创建 `yudao-module-aigc-task-api` 和 `yudao-module-aigc-task-server`，服务注册名为 `aigc-task-server`。

## 4. 服务职责边界

## 4.1 模型与渠道服务：aigc-model

### 4.1.1 服务定位

负责所有模型供应商、模型配置、模型能力、参数模板、价格配置、租户模型授权、基础路由配置和模型调用计量。

这是平台的模型中台，不负责实际生成任务执行。

### 4.1.2 核心能力

- 渠道商管理
- 模型管理
- 模型能力管理
- 模型参数模板
- 模型价格配置
- 模型启停
- 模型展示排序
- 模型可用性校验
- 模型价格计算
- 租户模型授权
- 租户维度可见性和默认模型配置
- 基础模型路由规则
- 模型调用计量日志记录

### 4.1.3 核心表


| 表名                      | 说明             |
| ------------------------- | ---------------- |
| aigc_model_provider       | 模型渠道商       |
| aigc_model                | 模型配置         |
| aigc_model_capability     | 模型能力         |
| aigc_model_param_template | 模型参数模板     |
| aigc_model_price          | 模型价格配置     |
| aigc_model_route          | 模型路由规则     |
| aigc_model_tenant         | 租户模型授权     |
| aigc_model_usage_log      | 模型调用计量日志 |

### 4.1.4 API 暴露

`aigc-model-api` 对其他服务暴露 RPC API：

```text
AigcModelApi
  ├── validateModel(modelId, capability)
  ├── getModel(modelId)
  ├── getProvider(providerId)
  ├── getParamTemplates(modelId, capability)
  ├── validateParams(reqDTO)
  ├── calculatePrice(reqDTO)
  ├── listAvailableModels(type, capability)
  └── recordUsage(reqDTO)
```

当前 RPC 已基于 Feign 暴露，统一返回 `CommonResult`，主要路径包括 `/validate-model`、`/get-provider`、`/get-model`、`/list-available-models`、`/get-param-templates`、`/validate-params`、`/calculate-price`、`/record-usage`。

### 4.1.5 依赖关系

依赖：

- system-api：用户或管理员信息，非强依赖
- tenant 相关能力：复用项目租户插件和租户上下文

被依赖：

- aigc-gen
- aigc-task
- aigc-billing

当前实现说明：`aigc-model` 已完成 Maven 聚合模块、API DTO/枚举/RPC、Server Controller/Service/DAL、MySQL 建表 SQL、用户端接口、管理端接口、租户授权、调用计量和基础单元测试。当前测试覆盖 5 个测试类、20 个用例，后续建议继续补充管理端 Controller、租户授权、路由、渠道商、计量和密钥脱敏测试。

### 4.1.6 多租户规则

`aigc-model` 当前按“平台模型 + 租户授权/覆盖”思路建设：

- 平台维护渠道商、模型、能力、参数模板、基础价格和路由规则。
- 租户通过 `aigc_model_tenant` 获得模型授权，并可控制启用、用户端可见、默认模型、排序、并发和日限额。
- 用户端模型列表必须基于当前租户上下文查询，不能直接暴露全局 `public_visible` 模型。
- 价格计算优先使用租户价格，没有租户价格时再使用平台默认价格。
- RPC 调用需要透传租户上下文，保证 `validateModel`、`listAvailableModels`、`calculatePrice` 的结果都受租户隔离约束。

### 4.1.7 调用计量边界

`aigc-model` 的 `recordUsage` 只负责记录模型调用计量日志，不负责冻结、扣费、退款和钱包余额变更。

调用计量记录内容包括任务 ID、用户 ID、模型 ID、渠道商 ID、能力、请求编号、第三方任务编号、token 用量、成本价、销售价、调用状态、耗时、原始 usage 和错误信息。后续 `aigc-billing`、运营统计、成本分析可以基于该表做对账和报表，但资金动作仍归 `aigc-billing`。

## 4.2 计费钱包服务：aigc-billing

### 4.2.1 服务定位

负责用户钱包、积分、冻结、扣费、退款、成本、毛利。

这是商业化闭环的核心服务。

### 4.2.2 核心能力

- 用户钱包初始化
- 查询余额
- 手动赠送积分
- 充值入账
- 任务积分冻结
- 任务成功确认扣费
- 任务失败释放冻结
- 计费流水
- 成本记录
- 毛利统计

### 4.2.3 核心表


| 表名                | 说明                       |
| ------------------- | -------------------------- |
| aigc_wallet         | 用户钱包                   |
| aigc_quota_freeze   | 积分冻结记录               |
| aigc_billing_record | 计费流水                   |
| aigc_cost_record    | 成本记录                   |
| aigc_recharge_order | 充值订单，后续接支付时建设 |

### 4.2.4 API 暴露

`aigc-billing-api` 暴露：

```text
AigcBillingApi
  ├── getOrCreateWallet(userId)
  ├── getWallet(userId)
  ├── freeze(userId, bizType, bizId, amount)
  ├── confirmFreeze(freezeId)
  ├── releaseFreeze(freezeId)
  ├── createCostRecord(taskId, modelId, costAmount, saleAmount)
  └── calculateGrossProfit(taskId)
```

### 4.2.5 并发要求

冻结积分必须使用条件更新：

```sql
UPDATE aigc_wallet
SET balance = balance - #{amount},
    frozen_balance = frozen_balance + #{amount}
WHERE user_id = #{userId}
  AND balance >= #{amount}
  AND deleted = 0
```

### 4.2.6 依赖关系

依赖：

- system-api
- pay-api，后续充值接入时使用

被依赖：

- aigc-task
- aigc-gen

## 4.3 任务调度服务：aigc-task

### 4.3.1 服务定位

统一管理所有大模型生成相关任务，包括文本生成、图片生成、视频生成、音频生成、数字人生成、代码生成、PPT/文档生成、审核和文件处理等同步或异步任务。

任务服务不直接调用第三方模型，模型调用由 `aigc-gen` 负责。

### 4.3.2 核心能力

- 创建任务
- 任务状态机
- 任务队列
- 任务日志
- 回调幂等
- 任务重试
- 任务取消
- 任务失败退款联动
- 任务进度查询
- 管理后台任务监控
- 文本、图片、视频、音频、代码、文档、数字人等生成任务的统一状态抽象

### 4.3.3 核心表


| 表名               | 说明               |
| ------------------ | ------------------ |
| aigc_task          | 任务主表           |
| aigc_task_log      | 任务日志           |
| aigc_task_callback | 任务回调记录       |
| aigc_task_retry    | 任务重试记录，可选 |

### 4.3.4 API 暴露

`aigc-task-api` 暴露：

```text
AigcTaskApi
  ├── createTask(userId, taskType, modelId, requestParams, freezeAmount)
  ├── markQueued(taskId)
  ├── markRunning(taskId)
  ├── markCallbackWaiting(taskId, externalTaskId)
  ├── markDownloading(taskId)
  ├── markAuditing(taskId)
  ├── markSuccess(taskId, outputAssetId / outputText / outputData)
  ├── markFailed(taskId, failReason)
  ├── markRefunded(taskId)
  ├── getTask(taskId)
  └── createCallbackRecord(externalTaskId, callbackType, callbackData)
```

### 4.3.5 依赖关系

依赖：

- aigc-billing-api
- aigc-model-api

被依赖：

- aigc-gen
- aigc-asset

## 4.4 资产中心服务：aigc-asset

### 4.4.1 服务定位

统一管理用户生成和上传的文件型资产，包括图片、视频、音频、文档、PPT、字幕、封面、数字人视频等。

第一阶段优先管理图片、视频和音频等可直接消费的文件型资产；文本、代码、摘要、翻译等非文件型结果可直接保存在任务输出或后续知识库/文档服务中，不强制进入资产中心。

### 4.4.2 核心能力

- 图片资产入库
- 视频资产入库
- 音频资产入库
- 文档/PPT 资产入库
- 缩略图/封面管理
- 资产列表
- 资产详情
- 下载计数
- 删除资产
- 审核状态
- 资产可见性

### 4.4.3 核心表


| 表名                | 说明                   |
| ------------------- | ---------------------- |
| aigc_asset          | 资产主表               |
| aigc_asset_relation | 资产关系，第二阶段建设 |
| aigc_asset_version  | 资产版本，第二阶段建设 |

### 4.4.4 API 暴露

`aigc-asset-api` 暴露：

```text
AigcAssetApi
  ├── createAsset(userId, taskId, assetType, fileUrl, metadata)
  ├── createImageAsset(userId, taskId, fileUrl, metadata)
  ├── createVideoAsset(userId, taskId, fileUrl, coverUrl, metadata)
  ├── createAudioAsset(userId, taskId, fileUrl, metadata)
  ├── createDocumentAsset(userId, taskId, fileUrl, metadata)
  ├── getAsset(assetId)
  ├── getUserAssets(userId, type)
  ├── increaseDownloadCount(assetId)
  └── updateAuditStatus(assetId, auditStatus)
```

### 4.4.5 文件存储

`aigc-asset` 依赖 `infra-api` 的 `FileApi`。

外部模型返回的图片、视频、音频、文档、PPT 等文件必须下载后上传到平台文件服务，避免第三方 URL 过期。纯文本、代码、JSON 等非文件型结果不强制入资产库。

### 4.4.6 依赖关系

依赖：

- infra-api
- system-api

被依赖：

- aigc-gen
- aigc-task
- 后续 community、workflow、template

## 4.5 生成服务：aigc-gen

### 4.5.1 服务定位

负责统一大模型生成入口和第三方模型调用适配，覆盖文本、图片、视频、音频、数字人、代码、文档、PPT 等生成能力。

这是第一阶段用户直接感知最强的服务。

### 4.5.2 核心能力

- 文本生成、对话、摘要、翻译
- 文生图、图生图
- 文生视频、图生视频
- 文本转语音、语音转文本、音乐生成
- 数字人视频生成
- 代码生成、代码审查
- 文档/PPT 生成
- 模型调用客户端
- 第三方任务提交
- 第三方任务查询
- 第三方回调处理
- 结果文件下载
- 调用资产服务入库
- 调用任务服务推进状态

### 4.5.3 核心表


| 表名 | 说明 |
| ---- | ---- |
| aigc_generate_record | 通用生成记录，推荐第一阶段优先采用 |
| aigc_image | 图片生成记录，可作为垂直扩展表 |
| aigc_video | 视频生成记录，可作为垂直扩展表 |
| aigc_audio | 音频生成记录，可作为垂直扩展表 |
| aigc_text | 文本生成记录，可作为垂直扩展表 |

### 4.5.4 API 暴露

`aigc-gen-api` 暴露给内部使用：

```text
AigcGenerateApi
  ├── submit(userId, req)
  ├── getResult(taskId)
  ├── handleCallback(providerCode, callbackData)
  └── syncTask(taskId)

AigcTextGenerateApi
  ├── generateText(userId, req)
  ├── chat(userId, req)
  ├── summarize(userId, req)
  └── translate(userId, req)

AigcImageGenerateApi
  ├── textToImage(userId, req)
  ├── imageToImage(userId, req)
  └── getImage(id)

AigcVideoGenerateApi
  ├── textToVideo(userId, req)
  ├── imageToVideo(userId, req)
  ├── handleCallback(providerCode, callbackData)
  └── syncVideoTask(taskId)

AigcAudioGenerateApi
  ├── textToSpeech(userId, req)
  ├── speechToText(userId, req)
  └── musicGenerate(userId, req)

AigcCodeGenerateApi
  ├── generateCode(userId, req)
  └── reviewCode(userId, req)
```

### 4.5.5 用户接口

用户端可以直接请求 `aigc-gen`：

```text
/app-api/aigc/gen/image/text-to-image
/app-api/aigc/gen/image/image-to-image
/app-api/aigc/gen/video/text-to-video
/app-api/aigc/gen/video/image-to-video
/app-api/aigc/gen/text/generate
/app-api/aigc/gen/text/chat
/app-api/aigc/gen/audio/text-to-speech
/app-api/aigc/gen/code/generate
/app-api/aigc/gen/document/generate
```

### 4.5.6 依赖关系

依赖：

- aigc-model-api
- aigc-task-api
- aigc-billing-api
- aigc-asset-api
- aigc-safety-api
- infra-api

被依赖：

- 用户端前端
- 管理端前端

## 4.6 审核风控服务：aigc-safety

### 4.6.1 服务定位

第一阶段提供轻量审核能力，主要做提示词敏感词和审核记录。

### 4.6.2 核心能力

- 敏感词管理
- 提示词检测
- 审核记录
- 人工审核通过
- 人工审核拒绝
- 资产审核状态同步

### 4.6.3 核心表


| 表名                | 说明     |
| ------------------- | -------- |
| aigc_sensitive_word | 敏感词   |
| aigc_audit_record   | 审核记录 |

### 4.6.4 API 暴露

`aigc-safety-api` 暴露：

```text
AigcSafetyApi
  ├── checkPrompt(prompt, scene)
  ├── createAuditRecord(objectType, objectId, content)
  ├── markPass(auditId)
  └── markReject(auditId, reason)
```

### 4.6.5 依赖关系

依赖：

- aigc-asset-api，可选，用于同步资产审核状态

被依赖：

- aigc-gen
- aigc-asset

## 5. 服务依赖关系

### 5.1 调用关系图

```text
用户端 / 管理端
      ↓
  aigc-gen
      ↓
  ├── aigc-model
  ├── aigc-task
  ├── aigc-billing
  ├── aigc-asset
  └── aigc-safety

  aigc-task
      ↓
  ├── aigc-billing
  └── aigc-model

  aigc-model
      ↓
  └── system / tenant 上下文

  aigc-asset
      ↓
  └── infra-api FileApi
```

### 5.2 核心链路

```text
aigc-gen 接收用户请求
  ↓
aigc-model 校验模型并计算价格
  ↓
aigc-safety 检查提示词
  ↓
aigc-task 创建任务
  ↓
aigc-billing 冻结积分
  ↓
aigc-gen 调用第三方模型
  ↓
aigc-model 记录模型调用计量
  ↓
aigc-gen 判断结果类型
  ↓
文件型结果下载并调用 aigc-asset 上传创建资产
  ↓
非文件型结果回写任务 outputText / outputData
  ↓
aigc-task 标记成功
  ↓
aigc-billing 确认扣费
```

失败链路：

```text
aigc-gen 捕获异常
  ↓
aigc-model 记录失败调用计量
  ↓
aigc-task 标记失败
  ↓
aigc-billing 释放冻结积分
  ↓
aigc-task 标记已退款
```

## 6. 数据一致性方案

### 6.1 不使用强分布式事务

第一阶段不建议引入 Seata 或复杂分布式事务。

采用：

- 本地事务
- 状态机
- 幂等键
- 补偿任务
- 定时扫描异常任务

### 6.2 幂等要求


| 场景     | 幂等方式                            |
| -------- | ----------------------------------- |
| 任务创建 | taskNo 唯一                         |
| 积分冻结 | freezeNo 唯一，bizType + bizId 唯一 |
| 成功扣费 | taskId + CONSUME 唯一               |
| 失败退款 | taskId + REFUND 唯一                |
| 回调处理 | externalTaskId + callbackType 唯一  |
| 资产入库 | taskId + assetType 唯一             |
| 模型调用计量 | requestNo 或 taskId + modelId + capability |

### 6.3 补偿任务

每个关键服务需要定时补偿：


| 服务         | 补偿任务                                   |
| ------------ | ------------------------------------------ |
| aigc-task    | 扫描长时间 RUNNING / CALLBACK_WAITING 任务 |
| aigc-gen     | 轮询视频、音频、数字人、文档等异步渠道外部任务状态 |
| aigc-billing | 扫描超时冻结未释放记录                     |
| aigc-asset   | 检查任务成功但资产未入库记录               |
| aigc-model   | 检查调用计量、价格配置、租户授权异常数据   |

## 7. 数据库拆分建议

### 7.1 第一阶段推荐

第一阶段可以先使用同一个业务数据库，但表按服务边界拆分。

优点：

- 开发快
- 运维简单
- 方便联查排查问题
- 不影响代码上的微服务边界

### 7.2 后续演进

业务量上来后再拆库：


| 微服务       | 数据库          |
| ------------ | --------------- |
| aigc-model   | aigc_model_db   |
| aigc-task    | aigc_task_db    |
| aigc-billing | aigc_billing_db |
| aigc-asset   | aigc_asset_db   |
| aigc-gen     | aigc_gen_db     |
| aigc-safety  | aigc_safety_db  |

拆库后禁止跨库 JOIN，通过 RPC API 查询。

## 8. 网关与注册配置

### 8.1 Nacos 服务名

Nacos 注册名使用 `spring.application.name`，按项目现有规范不带 `yudao-module-` 前缀：

```text
aigc-model-server
aigc-billing-server
aigc-task-server
aigc-asset-server
aigc-gen-server
aigc-safety-server
```

当前 `aigc-model` 的实际 `spring.application.name` 为 `aigc-model-server`，端口为 `48090`。其他服务建设时也应按同一规则设置，避免 Feign 服务名与 Nacos 注册名不一致。

### 8.2 网关路由

用户端主要开放：

```text
/app-api/aigc/gen/**
/app-api/aigc/task/**
/app-api/aigc/asset/**
/app-api/aigc/wallet/**
/app-api/aigc/model/**
```

管理端开放：

```text
/admin-api/aigc/model/**
/admin-api/aigc/task/**
/admin-api/aigc/asset/**
/admin-api/aigc/billing/**
/admin-api/aigc/safety/**
/admin-api/aigc/gen/**
```

## 9. 建设顺序

### 9.1 第一阶段：最小可赚钱闭环

优先顺序：

```text
1. aigc-model
2. aigc-billing
3. aigc-task
4. aigc-asset
5. aigc-safety
6. aigc-gen 文本生成和对话
7. aigc-gen 图片生成
8. aigc-gen 视频/音频等异步生成
9. 管理后台监控
```

### 9.2 为什么这个顺序

- 没有模型服务，生成服务无法选择模型和价格。
- 没有计费服务，不能上线赚钱。
- 没有任务服务，异步生成不可控。
- 没有资产服务，生成结果无法沉淀。
- 没有审核服务，内容风险不可控。
- 文本生成和对话先做，链路最短，可快速验证模型、任务和计费闭环。
- 图片生成成本低、用户感知强，适合验证资产入库和审核链路。
- 视频、音频、数字人、PPT 等异步生成后做，客单价高但链路更长，需要依赖任务补偿和资产能力。

### 9.3 每个阶段验收

#### aigc-model 验收

- 可以新增渠道商。
- 可以新增文本模型。
- 可以新增图片模型。
- 可以新增视频模型。
- 可以新增音频、代码、文档等模型。
- 可以配置模型价格。
- 可以配置参数模板。
- 可以上下线模型。
- 可以配置租户模型授权、启停、可见性和默认模型。
- 可以配置基础模型路由规则。
- 用户端只展示当前租户启用且公开的模型。
- RPC 可以完成模型校验、参数校验、价格计算、渠道查询和调用计量记录。
- 调用计量可以写入 `aigc_model_usage_log`，供后续成本、统计和审计使用。
- 当前已有 20 个自动化测试用例通过，后续补齐管理端、租户、路由、计量和密钥脱敏测试。

#### aigc-billing 验收

- 用户钱包可初始化。
- 可用余额、冻结余额准确。
- 冻结积分不会超扣。
- 成功扣费幂等。
- 失败退款幂等。
- 计费流水完整。

#### aigc-task 验收

- 任务可创建。
- 文本、图片、视频、音频等任务类型可统一创建。
- 状态流转正确。
- 日志完整。
- 回调幂等。
- 失败任务可重试。
- 任务可按用户分页查询。

#### aigc-asset 验收

- 图片资产可入库。
- 视频资产可入库。
- 音频、文档、PPT 等文件型资产可入库。
- 用户只能看自己的资产。
- 下载次数可统计。
- 资产可删除。

#### aigc-gen 验收

- 文本生成、对话、摘要、翻译可生成。
- 文生图、图生图可生成。
- 文生视频、图生视频可生成。
- 文本转语音、代码生成、文档生成等能力可按模型能力逐步接入。
- 成功扣费。
- 失败退款。
- 文件型生成结果进入资产中心，非文件型结果回写任务结果。

## 10. 服务间 API 依赖矩阵


| 调用方       | 被调用方     | 用途                             |
| ------------ | ------------ | -------------------------------- |
| aigc-gen     | aigc-model   | 校验模型、获取渠道配置、获取价格、记录调用计量 |
| aigc-gen     | aigc-billing | 冻结、扣费、退款                 |
| aigc-gen     | aigc-task    | 创建任务、更新状态               |
| aigc-gen     | aigc-asset   | 文件型结果创建资产               |
| aigc-gen     | aigc-safety  | 提示词审核                       |
| aigc-task    | aigc-billing | 异常任务退款补偿                 |
| aigc-task    | aigc-model   | 任务展示模型信息、补充调用计量或统计口径 |
| aigc-asset   | infra-api    | 文件上传                         |
| aigc-billing | pay-api      | 后续充值支付                     |

## 11. 代码规范

### 11.1 类名前缀

所有类统一使用服务前缀，避免和旧 AI 模块冲突。

示例：

```text
AigcModelDO
AigcTaskDO
AigcAssetDO
AigcWalletDO
AigcImageDO
AigcVideoDO
```

### 11.2 错误码段

AIGC 微服务统一使用：

```text
1-041-000-000 ~ 1-041-999-999
```

建议按服务切分：


| 服务         | 错误码段      |
| ------------ | ------------- |
| aigc-model   | 1-041-000-000 |
| aigc-billing | 1-041-100-000 |
| aigc-task    | 1-041-200-000 |
| aigc-asset   | 1-041-300-000 |
| aigc-gen     | 1-041-400-000 |
| aigc-safety  | 1-041-500-000 |

### 11.3 枚举位置

通用枚举放在各服务 API 模块中：

```text
yudao-module-aigc-task-api/src/main/java/.../enums
```

服务内部枚举放在 server 模块中。

### 11.4 DTO 位置

跨服务 RPC DTO 放 API 模块：

```text
api/dto
```

Controller VO 放 server 模块：

```text
controller/admin/xxx/vo
controller/app/xxx/vo
```

### 11.5 数据对象

DO 只放 server 模块：

```text
dal/dataobject
```

## 12. 后续扩展微服务

### 12.1 工作流服务：aigc-workflow

建设时机：第二阶段。

职责：

- 工作流模板
- 节点编排
- 节点执行
- 人工确认
- 局部重跑

依赖：

- aigc-task
- aigc-gen
- aigc-asset
- aigc-billing

### 12.2 模板服务：aigc-template

建设时机：第二或第三阶段。

职责：

- 提示词模板
- 图片模板
- 视频模板
- 参数模板
- 一键同款模板

### 12.3 社区服务：aigc-community

建设时机：第三阶段。

职责：

- 作品发布
- 点赞
- 收藏
- 评论
- 关注
- 标签
- 话题
- 榜单
- 举报

依赖：

- aigc-asset
- aigc-safety
- aigc-template

### 12.4 创作者服务：aigc-creator

建设时机：第四阶段。

职责：

- 创作者主页
- 模板收益
- 作品复用收益
- 创作者激励

依赖：

- aigc-community
- aigc-billing
- aigc-template

## 13. MVP 最小部署组合

第一阶段最小部署服务：

```text
yudao-gateway
system-server
infra-server
aigc-model-server
aigc-billing-server
aigc-task-server
aigc-asset-server
aigc-safety-server
aigc-gen-server
```

说明：部署组合这里填写服务注册名，即各模块的 `spring.application.name`。对应 Maven 模块仍然是 `yudao-module-system-server`、`yudao-module-infra-server`、`yudao-module-aigc-xxx-server`。

如需充值支付，再增加：

```text
pay-server
```

## 14. 最终建议

你要做的是商业化 AIGC 平台，不建议用一个大模块把所有东西都塞进去。

推荐微服务边界：

```text
模型归 model
钱归 billing
任务归 task
文件归 asset
生成归 gen
审核归 safety
```

第一阶段只保证一条赚钱链路跑通：

```text
用户选择模型
  ↓
模型服务按租户授权校验模型、参数和价格
  ↓
提交文本、图片、视频、音频、代码、文档等生成任务
  ↓
冻结积分
  ↓
生成任务执行
  ↓
记录模型调用计量
  ↓
文件型结果入资产，非文件型结果回写任务结果
  ↓
成功扣费 / 失败退款
  ↓
后台统计成本和毛利
```

后续再扩展：

```text
工作流 → 模板 → 社区 → 创作者激励 → 平台生态
```

这样既能快速上线赚钱，也能避免旧 AI 模块的历史包袱，并且后续可以自然演进成覆盖文本、图片、视频、音频、代码、文档、数字人和工作流的完整 AIGC 多模态创作平台。
