# yudao-module-aigc-task 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置

## 1. 模块定位

`yudao-module-aigc-task` 是 AIGC 平台的统一任务调度与状态机服务，负责承接所有大模型相关生成任务的生命周期管理，包括文本生成、图片生成、视频生成、音频生成、数字人生成、代码生成、PPT/文档生成、工作流编排任务、审核、下载、入库、失败补偿等异步任务。

该模块不直接调用第三方模型，不保存生成正文或文件，不执行钱包扣费，不维护模型配置，只负责任务创建、任务状态流转、任务日志、回调幂等、重试补偿、任务查询和跨服务编排状态记录。

`aigc-task` 是 AIGC 赚钱闭环中的“流程账本”和“状态事实源”，需要保证任务状态可靠、幂等、可追踪、可补偿。

## 2. 核心职责

### 2.1 负责内容

- 统一任务创建
- 任务编号生成
- 任务状态机管理
- 任务进度记录
- 任务日志记录
- 第三方回调记录
- 回调幂等控制
- 任务重试记录
- 任务失败原因记录
- 任务取消记录
- 任务退款状态记录
- 长时间异常任务扫描
- 任务用户端查询
- 任务管理端监控
- 文本、图片、视频、音频、代码、文档、数字人等生成任务的统一抽象
- 与计费、生成、资产、模型服务的状态协作

### 2.2 不负责内容

- 不直接调用第三方模型 API
- 不直接下载第三方生成文件
- 不上传图片、视频、音频、文档等文件
- 不保存大模型生成正文内容
- 不保存资产文件元数据
- 不冻结积分
- 不确认扣费
- 不释放冻结积分
- 不维护模型价格
- 不做提示词审核
- 不保存渠道商密钥

对应职责归属：

| 能力 | 归属模块 |
| ---- | -------- |
| 模型、渠道、参数、价格 | `yudao-module-aigc-model` |
| 第三方模型调用适配 | `yudao-module-aigc-gen` |
| 钱包、冻结、扣费、退款 | `yudao-module-aigc-billing` |
| 图片、视频、音频、文档等文件资产 | `yudao-module-aigc-asset` |
| 敏感词、审核记录 | `yudao-module-aigc-safety` |

任务服务的抽象对象是“生成任务”，不是“图片/视频任务”。图片、视频只是第一阶段最优先落地的生成类型，后续文本、语音、音乐、数字人、代码、PPT、文档、工作流等大模型任务都应复用同一套任务状态机、日志、回调和补偿能力。

## 3. 模块结构

### 3.1 Maven 结构

```text
yudao-module-aigc-task
  ├── yudao-module-aigc-task-api
  └── yudao-module-aigc-task-server
```

命名规则遵循当前项目规范：

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-task` |
| 聚合 artifactId | `yudao-module-aigc-task` |
| API 子模块 artifactId | `yudao-module-aigc-task-api` |
| Server 子模块 artifactId | `yudao-module-aigc-task-server` |
| Spring 应用名 | `aigc-task-server` |

### 3.2 根包名

```text
cn.iocoder.yudao.module.aigc.task
```

### 3.3 API 模块结构

```text
yudao-module-aigc-task-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/task
      ├── api
      │   └── AigcTaskApi.java
      ├── dto
      │   ├── AigcTaskCreateReqDTO.java
      │   ├── AigcTaskRespDTO.java
      │   ├── AigcTaskStatusUpdateReqDTO.java
      │   ├── AigcTaskCallbackCreateReqDTO.java
      │   ├── AigcTaskRetryCreateReqDTO.java
      │   └── AigcTaskLogCreateReqDTO.java
      └── enums
          ├── AigcTaskTypeEnum.java
          ├── AigcTaskStatusEnum.java
          ├── AigcTaskCallbackTypeEnum.java
          ├── AigcTaskRetryStatusEnum.java
          ├── AigcTaskFailReasonEnum.java
          └── ErrorCodeConstants.java
```

### 3.4 Server 模块结构

```text
yudao-module-aigc-task-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/task
      ├── AigcTaskServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── task
      │   │   ├── log
      │   │   ├── callback
      │   │   └── retry
      │   └── app
      │       └── task
      ├── framework
      │   └── web
      │       └── config
      │           └── AigcTaskWebConfiguration.java
      ├── service
      │   ├── task
      │   ├── log
      │   ├── callback
      │   ├── retry
      │   └── compensate
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcTaskDO.java
      │   │   ├── AigcTaskLogDO.java
      │   │   ├── AigcTaskCallbackDO.java
      │   │   └── AigcTaskRetryDO.java
      │   └── mysql
      │       ├── AigcTaskMapper.java
      │       ├── AigcTaskLogMapper.java
      │       ├── AigcTaskCallbackMapper.java
      │       └── AigcTaskRetryMapper.java
      ├── job
      │   ├── AigcTaskTimeoutJob.java
      │   └── AigcTaskRetryJob.java
      └── convert
```

## 4. 依赖设计

### 4.1 API 模块依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-common</artifactId>
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
        <artifactId>yudao-module-aigc-task-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-model-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-billing-api</artifactId>
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
    </dependency>
</dependencies>
```

说明：

- `aigc-task` 必须依赖 `aigc-model-api`，用于任务展示模型信息、补偿时校验模型基础信息。
- `aigc-task` 必须依赖 `aigc-billing-api`，用于异常任务退款补偿和任务账务状态核对。超时补偿、失败补偿、取消补偿进入 `REFUNDING` 后，需要真实调用 `AigcBillingApi.releaseFreeze` 释放冻结积分，成功后再推进任务到 `REFUNDED`。
- `aigc-task` 不直接依赖 `aigc-gen-api`，生成服务主动调用任务服务推进状态，避免任务服务反向调用生成执行逻辑。
- `aigc-task` 不依赖 `infra-api` 上传文件，资产入库职责归 `aigc-asset`。

## 5. 数据库设计

### 5.1 表清单

| 表名 | 说明 | 第一阶段 |
| ---- | ---- | -------- |
| `aigc_task` | 任务主表 | 是 |
| `aigc_task_log` | 任务日志表 | 是 |
| `aigc_task_callback` | 第三方回调记录表 | 是 |
| `aigc_task_retry` | 任务重试记录表 | 是 |

### 5.2 aigc_task

任务主表，记录 AIGC 异步任务的核心状态、价格快照、模型信息、业务参数摘要、输出结果索引和失败信息。该表既支持图片/视频等文件型结果，也支持文本、代码、文档结构化内容等非文件型结果。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| task_no | varchar(64) | 平台任务编号，全局唯一 |
| user_id | bigint | 用户 ID |
| task_type | varchar(64) | 任务类型 |
| capability | varchar(64) | 模型能力 |
| model_id | bigint | 模型 ID |
| provider_id | bigint | 渠道商 ID |
| status | varchar(32) | 任务状态 |
| progress | int | 进度，0 到 100 |
| estimated_duration_millis | bigint | 预计耗时毫秒，用于用户端平滑进度条 |
| request_params | json | 用户请求参数快照 |
| price_snapshot | json | 价格快照 |
| freeze_id | bigint | 冻结记录 ID |
| sale_price | decimal(18,6) | 销售价快照 |
| cost_price | decimal(18,6) | 成本价快照 |
| currency_type | varchar(32) | 货币类型 |
| external_task_id | varchar(128) | 第三方任务编号 |
| output_asset_id | bigint | 输出资产 ID |
| output_asset_type | varchar(32) | 输出资产类型 |
| output_text | longtext | 文本/代码类结果摘要或短结果，可选 |
| output_data | json | 结构化输出结果索引，可保存文本、代码、文档、PPT、数字人等结果元信息 |
| fail_code | varchar(128) | 失败错误码 |
| fail_reason | varchar(1024) | 失败原因 |
| submit_time | datetime | 提交时间 |
| start_time | datetime | 开始执行时间 |
| callback_time | datetime | 回调时间 |
| finish_time | datetime | 完成时间 |
| expire_time | datetime | 超时时间 |
| retry_count | int | 已重试次数 |
| max_retry_count | int | 最大重试次数 |
| remark | varchar(512) | 备注 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_task_no = task_no
idx_user_status = user_id + status
idx_status_create_time = status + create_time
idx_external_task_id = external_task_id
idx_model_id = model_id
idx_tenant_status = tenant_id + status
```

### 5.3 aigc_task_log

任务日志表，记录任务每一次关键状态变化和操作来源，用于用户进度展示、管理端排障和审计。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| task_id | bigint | 任务 ID |
| task_no | varchar(64) | 任务编号 |
| from_status | varchar(32) | 变更前状态 |
| to_status | varchar(32) | 变更后状态 |
| action | varchar(64) | 操作动作 |
| message | varchar(1024) | 日志消息 |
| operator_type | varchar(32) | 操作来源类型 |
| operator_id | bigint | 操作人或系统 ID |
| extra_info | json | 扩展信息 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
idx_task_id = task_id
idx_task_no = task_no
idx_create_time = create_time
```

### 5.4 aigc_task_callback

第三方回调记录表，保存第三方模型渠道回调原始内容，保证回调处理幂等、可追踪、可重放。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| callback_no | varchar(64) | 回调编号 |
| task_id | bigint | 任务 ID |
| task_no | varchar(64) | 任务编号 |
| provider_id | bigint | 渠道商 ID |
| provider_code | varchar(64) | 渠道商编码 |
| external_task_id | varchar(128) | 第三方任务编号 |
| callback_type | varchar(64) | 回调类型 |
| callback_status | varchar(32) | 回调处理状态 |
| raw_body | longtext | 原始回调内容 |
| headers | json | 请求头摘要 |
| signature | varchar(512) | 签名或验签摘要 |
| process_result | json | 处理结果 |
| fail_reason | varchar(1024) | 处理失败原因 |
| received_time | datetime | 接收时间 |
| processed_time | datetime | 处理时间 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_external_callback = provider_code + external_task_id + callback_type
idx_task_id = task_id
idx_callback_status = callback_status
idx_received_time = received_time
```

### 5.5 aigc_task_retry

任务重试记录表，记录任务失败后的自动或人工重试信息，避免无限重试和重复执行。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| retry_no | varchar(64) | 重试编号 |
| task_id | bigint | 任务 ID |
| task_no | varchar(64) | 任务编号 |
| retry_type | varchar(32) | 重试类型 |
| retry_status | varchar(32) | 重试状态 |
| retry_count | int | 第几次重试 |
| next_retry_time | datetime | 下次重试时间 |
| start_time | datetime | 开始时间 |
| finish_time | datetime | 结束时间 |
| fail_reason | varchar(1024) | 失败原因 |
| operator_id | bigint | 人工触发人 ID |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_retry_no = retry_no
idx_task_id = task_id
idx_retry_status_time = retry_status + next_retry_time
```

## 6. 枚举设计

### 6.1 AigcTaskTypeEnum

```text
TEXT_GENERATE
TEXT_CHAT
TEXT_SUMMARY
TEXT_TRANSLATE
TEXT_EMBEDDING
IMAGE_TEXT_TO_IMAGE
IMAGE_TO_IMAGE
VIDEO_TEXT_TO_VIDEO
VIDEO_IMAGE_TO_VIDEO
VIDEO_FIRST_LAST_FRAME
VIDEO_EXTEND
AUDIO_TEXT_TO_SPEECH
AUDIO_SPEECH_TO_TEXT
AUDIO_MUSIC_GENERATE
DIGITAL_HUMAN_VIDEO
CODE_GENERATE
CODE_REVIEW
DOCUMENT_GENERATE
PPT_GENERATE
WORKFLOW_RUN
AUDIT_PROMPT
ASSET_DOWNLOAD
```

任务类型命名建议采用“模态 + 动作”方式：

| 前缀 | 说明 | 示例 |
| ---- | ---- | ---- |
| TEXT | 文本类大模型任务 | TEXT_GENERATE、TEXT_CHAT、TEXT_SUMMARY |
| IMAGE | 图片类生成任务 | IMAGE_TEXT_TO_IMAGE、IMAGE_TO_IMAGE |
| VIDEO | 视频类生成任务 | VIDEO_TEXT_TO_VIDEO、VIDEO_EXTEND |
| AUDIO | 音频类生成任务 | AUDIO_TEXT_TO_SPEECH、AUDIO_MUSIC_GENERATE |
| DIGITAL_HUMAN | 数字人任务 | DIGITAL_HUMAN_VIDEO |
| CODE | 代码类任务 | CODE_GENERATE、CODE_REVIEW |
| DOCUMENT | 文档类任务 | DOCUMENT_GENERATE |
| PPT | 演示文稿任务 | PPT_GENERATE |
| WORKFLOW | 工作流编排任务 | WORKFLOW_RUN |
| AUDIT | 审核类任务 | AUDIT_PROMPT |
| ASSET | 资产处理任务 | ASSET_DOWNLOAD |

第一阶段优先开放：

```text
TEXT_GENERATE
TEXT_CHAT
IMAGE_TEXT_TO_IMAGE
IMAGE_TO_IMAGE
VIDEO_TEXT_TO_VIDEO
VIDEO_IMAGE_TO_VIDEO
AUDIO_TEXT_TO_SPEECH
```

如果业务上线节奏仍以图片/视频变现为主，可以先只接入图片/视频任务，但表结构、枚举、接口和状态机必须按全模态大模型任务预留，避免后续接入文本、音频、代码、PPT、数字人时重新设计任务中心。

### 6.2 AigcTaskStatusEnum

```text
CREATED
PRICE_CALCULATED
FROZEN
QUEUED
RUNNING
SUBMITTED
CALLBACK_WAITING
DOWNLOADING
ASSET_CREATING
AUDITING
SUCCESS
FAILED
CANCELLED
REFUNDING
REFUNDED
```

状态说明：

| 状态 | 说明 |
| ---- | ---- |
| CREATED | 任务已创建 |
| PRICE_CALCULATED | 已记录价格快照 |
| FROZEN | 已冻结积分 |
| QUEUED | 已进入队列 |
| RUNNING | 生成服务处理中 |
| SUBMITTED | 已提交第三方任务 |
| CALLBACK_WAITING | 等待第三方回调 |
| DOWNLOADING | 下载第三方文件结果中，文本类任务可跳过 |
| ASSET_CREATING | 创建资产中，纯文本类任务可跳过 |
| AUDITING | 审核中 |
| SUCCESS | 任务成功 |
| FAILED | 任务失败 |
| CANCELLED | 任务取消 |
| REFUNDING | 退款处理中 |
| REFUNDED | 已退款或已释放冻结 |

### 6.3 AigcTaskCallbackTypeEnum

```text
PROVIDER_TASK_SUCCESS
PROVIDER_TASK_FAILED
PROVIDER_TASK_PROGRESS
PROVIDER_TASK_CANCELLED
MANUAL_CALLBACK
```

### 6.4 AigcTaskRetryStatusEnum

```text
WAITING
RUNNING
SUCCESS
FAILED
CANCELLED
```

### 6.5 AigcTaskFailReasonEnum

```text
MODEL_VALIDATE_FAILED
PARAM_VALIDATE_FAILED
PRICE_CALCULATE_FAILED
BALANCE_NOT_ENOUGH
SAFETY_REJECTED
PROVIDER_SUBMIT_FAILED
PROVIDER_CALLBACK_FAILED
DOWNLOAD_FAILED
ASSET_CREATE_FAILED
BILLING_REFUND_FAILED
TIMEOUT
UNKNOWN
```

## 7. RPC API 设计

### 7.1 AigcTaskApi

`AigcTaskApi` 放在 `yudao-module-aigc-task-api`，供 `aigc-gen`、`aigc-billing`、`aigc-asset`、管理后台相关服务调用。

核心接口：

```java
@FeignClient(name = ApiConstants.NAME)
public interface AigcTaskApi {

    CommonResult<Long> createTask(AigcTaskCreateReqDTO reqDTO);

    CommonResult<AigcTaskRespDTO> getTask(Long taskId);

    CommonResult<AigcTaskRespDTO> getTaskByTaskNo(String taskNo);

    CommonResult<Boolean> markQueued(Long taskId);

    CommonResult<Boolean> markRunning(Long taskId);

    CommonResult<Boolean> markSubmitted(AigcTaskStatusUpdateReqDTO reqDTO);

    CommonResult<Boolean> markCallbackWaiting(AigcTaskStatusUpdateReqDTO reqDTO);

    CommonResult<Boolean> markDownloading(Long taskId);

    CommonResult<Boolean> markAssetCreating(Long taskId);

    CommonResult<Boolean> markSuccess(AigcTaskStatusUpdateReqDTO reqDTO);

    CommonResult<Boolean> markFailed(AigcTaskStatusUpdateReqDTO reqDTO);

    CommonResult<Boolean> markRefunding(Long taskId);

    CommonResult<Boolean> markRefunded(Long taskId);

    CommonResult<Long> createCallbackRecord(AigcTaskCallbackCreateReqDTO reqDTO);

    CommonResult<Long> createRetryRecord(AigcTaskRetryCreateReqDTO reqDTO);

}
```

### 7.2 createTask

输入：

| 字段 | 说明 |
| ---- | ---- |
| userId | 用户 ID |
| taskType | 任务类型 |
| capability | 模型能力 |
| modelId | 模型 ID |
| providerId | 渠道商 ID |
| requestParams | 用户请求参数快照 |
| priceSnapshot | 价格快照 |
| freezeId | 冻结记录 ID，可后置更新 |
| salePrice | 销售价 |
| costPrice | 成本价 |
| currencyType | 货币类型 |

处理要求：

- 生成全局唯一 `taskNo`。
- 创建任务状态为 `CREATED` 或 `FROZEN`，具体取决于调用链路是否先冻结再创建任务。
- 保存请求参数和价格快照，避免后续模型价格变化影响任务结算。
- 写入一条任务创建日志。

### 7.3 状态推进接口

状态推进接口必须做合法状态流转校验，禁止随意回退或跨状态跳转。

| 接口 | 目标状态 | 主要调用方 |
| ---- | -------- | ---------- |
| `markQueued` | QUEUED | `aigc-gen` 或队列生产者 |
| `markRunning` | RUNNING | `aigc-gen` |
| `markSubmitted` | SUBMITTED | `aigc-gen` |
| `markCallbackWaiting` | CALLBACK_WAITING | `aigc-gen` |
| `markDownloading` | DOWNLOADING | `aigc-gen` |
| `markAssetCreating` | ASSET_CREATING | `aigc-gen` 或 `aigc-asset` |
| `markSuccess` | SUCCESS | `aigc-gen` 或 `aigc-asset` |
| `markFailed` | FAILED | `aigc-gen`、补偿任务 |
| `markRefunding` | REFUNDING | `aigc-task` 补偿或 `aigc-billing` |
| `markRefunded` | REFUNDED | `aigc-billing` |

### 7.4 createCallbackRecord

用于保存第三方回调原文，必须先落库后处理业务状态。

幂等要求：

```text
providerCode + externalTaskId + callbackType 唯一
```

如果重复回调，应直接返回已有回调记录或处理成功，不允许重复推进任务状态、重复创建资产、重复扣费或重复退款。

## 8. 管理端接口设计

### 8.1 任务管理

Controller：`AigcTaskController`

路径：`/aigc/task`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 任务详情 |
| GET | `/page` | 任务分页 |
| PUT | `/cancel` | 取消任务 |
| PUT | `/retry` | 人工重试任务 |
| PUT | `/mark-failed` | 人工标记失败 |
| GET | `/statistics` | 任务统计 |

### 8.2 任务日志

Controller：`AigcTaskLogController`

路径：`/aigc/task/log`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/list` | 查询任务日志列表 |
| GET | `/page` | 查询任务日志分页 |

### 8.3 回调记录

Controller：`AigcTaskCallbackController`

路径：`/aigc/task/callback`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 回调详情 |
| GET | `/page` | 回调分页 |
| POST | `/replay` | 回调重放 |

### 8.4 重试记录

Controller：`AigcTaskRetryController`

路径：`/aigc/task/retry`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/page` | 重试记录分页 |
| PUT | `/cancel` | 取消待重试记录 |
| POST | `/trigger` | 手动触发重试 |

## 9. 用户端接口设计

Controller：`AigcTaskAppController`

代码路径：`/aigc/task`

网关对用户端接口可统一增加 `/app-api` 前缀，最终外部路径以网关配置为准。

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 获取当前用户任务详情 |
| GET | `/page` | 获取当前用户任务分页 |
| PUT | `/cancel` | 用户取消任务 |
| GET | `/progress` | 查询任务进度 |

用户端返回字段只允许包含：

- 任务 ID
- 任务编号
- 任务类型
- 模型名称
- 状态
- 进度
- 失败原因脱敏描述
- 输出资产 ID
- 文本/代码类输出摘要
- 结构化输出结果索引
- 创建时间
- 完成时间
- 销售价

不返回：

- 成本价
- 渠道商密钥
- 第三方原始回调
- 内部错误堆栈
- 其他租户或其他用户任务

## 10. 核心流程

### 10.1 用户提交生成任务流程

```text
aigc-gen 接收用户请求
  ↓
调用 aigc-model.validateModel(modelId, capability)
  ↓
调用 aigc-model.validateParams(reqDTO)
  ↓
调用 aigc-model.calculatePrice(reqDTO)
  ↓
调用 aigc-billing.freeze(userId, bizType, bizId, amount)
  ↓
调用 aigc-task.createTask(reqDTO)
  ↓
aigc-task 创建任务并保存价格快照、冻结记录和参数快照
  ↓
aigc-gen 提交第三方模型任务
  ↓
aigc-task 推进状态到 SUBMITTED / CALLBACK_WAITING
```

说明：同步返回类大模型任务，例如短文本生成、翻译、摘要、代码生成，可以由 `aigc-gen` 在同一次调用中完成生成，然后直接推进到 `SUCCESS`；异步类任务，例如图片、视频、音乐、数字人、PPT 长任务，则进入 `SUBMITTED`、`CALLBACK_WAITING` 或轮询补偿流程。

### 10.2 回调成功流程

```text
第三方渠道回调 aigc-gen
  ↓
aigc-gen 调用 aigc-task.createCallbackRecord 保存回调原文
  ↓
aigc-gen 验证回调幂等和签名
  ↓
aigc-task 根据结果类型推进状态
  ↓
文件型结果进入 DOWNLOADING，文本/代码/结构化结果可跳过下载
  ↓
文件型结果由 aigc-asset 创建图片/视频/音频/文档等资产，非文件型结果写入 outputText 或 outputData
  ↓
aigc-task 推进状态到 SUCCESS 并记录 outputAssetId、outputText 或 outputData
  ↓
aigc-billing.confirmFreeze 确认扣费
  ↓
aigc-model.recordUsage 记录模型调用计量
```

### 10.3 失败退款流程

```text
aigc-gen 或补偿任务发现失败
  ↓
aigc-task.markFailed 记录失败状态和原因
  ↓
aigc-task.markRefunding 标记退款处理中
  ↓
aigc-task 通过 AigcBillingApi.releaseFreeze 真实调用 aigc-billing 释放冻结积分
  ↓
aigc-task.markRefunded 标记已退款
  ↓
aigc-model.recordUsage 记录失败调用计量
```

### 10.4 超时补偿流程

```text
定时任务扫描 RUNNING / SUBMITTED / CALLBACK_WAITING 超时任务
  ↓
判断任务是否可查询第三方状态
  ↓
可查询则由 aigc-gen 同步第三方任务状态
  ↓
不可恢复或超过最大等待时间则 markFailed
  ↓
markRefunding 并通过 AigcBillingApi.releaseFreeze 触发 billing 释放冻结积分
  ↓
billing 释放成功后 markRefunded
  ↓
写入任务日志和重试记录
```

## 11. 状态机设计

### 11.1 正常状态流转

```text
CREATED
  ↓
PRICE_CALCULATED
  ↓
FROZEN
  ↓
QUEUED
  ↓
RUNNING
  ↓
SUBMITTED
  ↓
CALLBACK_WAITING
  ↓
DOWNLOADING
  ↓
ASSET_CREATING
  ↓
AUDITING
  ↓
SUCCESS
```

文本、代码、翻译、摘要等非文件型同步任务可以从 `RUNNING` 或 `SUBMITTED` 直接进入 `SUCCESS`。图片、视频、音频、数字人、PPT、文档等文件型任务通常需要经过 `DOWNLOADING`、`ASSET_CREATING`。第一阶段如果不做资产后置审核，可以跳过 `AUDITING`，直接从 `ASSET_CREATING` 到 `SUCCESS`。

### 11.2 失败状态流转

```text
任意执行中状态
  ↓
FAILED
  ↓
REFUNDING
  ↓
REFUNDED
```

### 11.3 取消状态流转

```text
CREATED / PRICE_CALCULATED / FROZEN / QUEUED
  ↓
CANCELLED
  ↓ 如果已冻结
REFUNDING
  ↓
REFUNDED
```

已经提交第三方模型的任务原则上不允许用户直接取消，除非渠道商支持取消接口，且取消成功后才能进入退款流程。

### 11.4 状态流转约束

| 当前状态 | 允许目标状态 |
| -------- | ------------ |
| CREATED | PRICE_CALCULATED、FROZEN、FAILED、CANCELLED |
| PRICE_CALCULATED | FROZEN、FAILED、CANCELLED |
| FROZEN | QUEUED、FAILED、CANCELLED |
| QUEUED | RUNNING、FAILED、CANCELLED |
| RUNNING | SUBMITTED、CALLBACK_WAITING、DOWNLOADING、SUCCESS、FAILED |
| SUBMITTED | CALLBACK_WAITING、DOWNLOADING、SUCCESS、FAILED |
| CALLBACK_WAITING | DOWNLOADING、SUCCESS、FAILED |
| DOWNLOADING | ASSET_CREATING、FAILED |
| ASSET_CREATING | AUDITING、SUCCESS、FAILED |
| AUDITING | SUCCESS、FAILED |
| SUCCESS | 不允许变更 |
| FAILED | REFUNDING、REFUNDED |
| CANCELLED | REFUNDING、REFUNDED |
| REFUNDING | REFUNDED |
| REFUNDED | 不允许变更 |

## 12. 幂等设计

### 12.1 幂等键

| 场景 | 幂等键 |
| ---- | ------ |
| 任务创建 | `taskNo` |
| 用户重复提交 | `clientRequestId + userId`，可选 |
| 回调处理 | `providerCode + externalTaskId + callbackType` |
| 状态推进 | `taskId + fromStatus + toStatus + action` |
| 重试记录 | `retryNo` |
| 资产入库联动 | `taskId + assetType`，由 `aigc-asset` 保证 |
| 扣费确认 | `freezeId`，由 `aigc-billing` 保证 |
| 退款释放 | `freezeId` 或 `taskId + REFUND`，由 `aigc-billing` 保证 |

### 12.2 状态更新方式

任务状态更新必须使用条件更新，避免并发回调、补偿任务、人工操作同时更新导致状态错乱。

```sql
UPDATE aigc_task
SET status = #{toStatus},
    progress = #{progress},
    update_time = NOW()
WHERE id = #{id}
  AND status = #{fromStatus}
  AND deleted = 0
```

如果影响行数为 0，需要重新查询任务状态，判断是否已被其他流程推进。

## 13. 多租户设计

### 13.1 租户隔离原则

- `aigc_task`、`aigc_task_log`、`aigc_task_callback`、`aigc_task_retry` 都必须包含 `tenant_id`。
- 用户端只能查询当前租户、当前用户自己的任务。
- 管理端默认查询当前租户任务，平台管理员跨租户查询需要显式使用租户切换能力。
- RPC 调用必须透传租户上下文，任务创建、状态推进、退款补偿都必须在正确租户下执行。
- 任务中的模型、价格、资产、钱包都必须属于同一租户上下文。

### 13.2 租户上下文使用

普通用户端接口：

```text
TenantContextHolder.getRequiredTenantId()
  ↓
过滤当前租户任务
  ↓
再过滤当前登录用户 userId
```

内部 RPC：

```text
aigc-gen 接收请求时获取租户上下文
  ↓
调用 aigc-task-api 时透传租户上下文
  ↓
aigc-task 写入 tenant_id
```

## 14. 缓存与队列设计

### 14.1 缓存设计

任务是强状态数据，第一阶段不建议缓存任务主表详情，以数据库为准。

可缓存内容：

| Key | 说明 |
| --- | ---- |
| `aigc:task:progress:{taskId}` | 任务进度短缓存，可选 |
| `aigc:task:submit:lock:{userId}:{clientRequestId}` | 防重复提交锁 |
| `aigc:task:callback:lock:{providerCode}:{externalTaskId}` | 回调处理锁 |

### 14.2 队列设计

第一阶段任务服务可以只做状态记录，由 `aigc-gen` 负责队列消费和第三方调用。

后续如果任务队列收敛到 `aigc-task`，可设计：

```text
aigc-task 创建任务
  ↓
发送 task-created 事件
  ↓
aigc-gen 消费事件并执行生成
  ↓
aigc-gen 回调 aigc-task 推进状态
```

事件消息必须包含：

- taskId
- taskNo
- tenantId
- userId
- taskType
- modelId
- capability
- requestParams

## 15. 安全设计

### 15.1 用户数据隔离

- 用户端任务查询必须校验 `userId`。
- 用户端取消任务必须校验任务归属。
- 用户不能查询其他用户任务、回调原文、重试记录。
- 管理端必须经过权限校验。

### 15.2 敏感信息保护

任务表中可以保存用户请求参数快照，但需要控制敏感内容：

- 不保存渠道商 API Key、Secret Key。
- 不保存完整第三方鉴权 Header。
- 提示词可保存用于任务追踪，但管理端展示应遵循平台隐私策略。
- 日志不打印完整请求参数和完整回调原文。
- 用户端失败原因需要脱敏，不返回内部异常栈。

### 15.3 权限控制

管理端权限建议：

| 权限标识 | 说明 |
| -------- | ---- |
| `aigc:task:query` | 任务查询 |
| `aigc:task:cancel` | 任务取消 |
| `aigc:task:retry` | 任务重试 |
| `aigc:task:update` | 任务状态人工处理 |
| `aigc:task:log:query` | 任务日志查询 |
| `aigc:task:callback:query` | 回调记录查询 |
| `aigc:task:callback:replay` | 回调重放 |
| `aigc:task:retry:query` | 重试记录查询 |

## 16. 错误码设计

使用 AIGC task 模块错误码段：

```text
1-041-200-000 ~ 1-041-299-999
```

| 错误码 | 常量 | 说明 |
| ------ | ---- | ---- |
| 1-041-200-000 | TASK_NOT_EXISTS | 任务不存在 |
| 1-041-200-001 | TASK_NO_DUPLICATE | 任务编号重复 |
| 1-041-200-002 | TASK_STATUS_INVALID | 任务状态不合法 |
| 1-041-200-003 | TASK_STATUS_TRANSFER_INVALID | 任务状态流转不合法 |
| 1-041-200-004 | TASK_NOT_OWNER | 无权访问该任务 |
| 1-041-200-005 | TASK_CANCEL_NOT_ALLOWED | 当前状态不允许取消 |
| 1-041-200-006 | TASK_RETRY_NOT_ALLOWED | 当前状态不允许重试 |
| 1-041-201-000 | TASK_LOG_NOT_EXISTS | 任务日志不存在 |
| 1-041-202-000 | TASK_CALLBACK_NOT_EXISTS | 回调记录不存在 |
| 1-041-202-001 | TASK_CALLBACK_DUPLICATE | 回调记录重复 |
| 1-041-202-002 | TASK_CALLBACK_PROCESS_FAILED | 回调处理失败 |
| 1-041-203-000 | TASK_RETRY_NOT_EXISTS | 重试记录不存在 |
| 1-041-203-001 | TASK_RETRY_EXCEED_LIMIT | 超过最大重试次数 |
| 1-041-204-000 | TASK_COMPENSATE_FAILED | 任务补偿失败 |

## 17. 观测与统计

### 17.1 指标口径

任务服务至少需要保留以下统计口径：

- 今日创建任务数
- 今日成功任务数
- 今日失败任务数
- 今日退款任务数
- 各任务类型数量
- 各状态任务数量
- 平均执行耗时
- P95 执行耗时
- 超时任务数量
- 重试任务数量
- 按 `providerId + modelId + capability` 维度统计的成功任务平均耗时
- 按 `providerId + modelId + capability` 维度统计的成功任务 P95 耗时
- 回调重复次数
- 回调处理失败次数

### 17.2 管理端看板

第一阶段管理端建议展示：

| 指标 | 说明 |
| ---- | ---- |
| 任务总数 | 当前租户或平台任务总量 |
| 成功率 | SUCCESS / 已结束任务 |
| 失败率 | FAILED / 已结束任务 |
| 平均耗时 | finishTime - submitTime |
| P95 耗时 | 最近完成任务样本按耗时升序取 95 分位 |
| 队列积压 | QUEUED / RUNNING / CALLBACK_WAITING 数量 |
| 退款中任务 | REFUNDING 数量 |
| 超时任务 | 超过 expireTime 未结束任务 |
| 重试任务 | retryCount > 0 的任务数量 |

### 17.3 任务预计耗时复用策略

任务统计不仅服务管理端看板，也作为用户端进度条估时的基础能力。

当 `aigc-gen` 已经确定本次请求的供应商、模型和能力后，应通过 `aigc-task-api` 查询最近成功任务耗时统计：

```text
providerId + modelId + capability
  ↓
aigc-task 查询最近 N 条 SUCCESS 任务
  ↓
返回 sampleCount、avgDurationMillis、p95DurationMillis
  ↓
aigc-gen 创建任务时写入 estimatedDurationMillis
  ↓
用户端根据 submitTime + estimatedDurationMillis 做平滑进度
```

统计口径：

- 只使用 `SUCCESS` 任务作为用户侧预计耗时样本，避免失败、取消、退款链路拉低或拉高进度预估。
- 默认取最近 `50` 条成功任务，最多允许取 `500` 条，后续可改为离线聚合表。
- 平均耗时用于默认进度估计；P95 耗时用于管理端观察长尾，也可作为前端进度上限保护参考。
- 当样本为空时返回 `0`，调用方应 fallback 到模型配置或供应商超时时间。
- 前端不能因为预计耗时到达就自行判定失败，失败必须以后端任务终态为准。

当前已实现：

- `aigc_task.estimated_duration_millis` 已落库。
- `AigcTaskCreateReqDTO` 支持写入 `estimatedDurationMillis`。
- `AigcTaskRespDTO` 返回 `estimatedDurationMillis`、`submitTime` 和 `startTime`，用户端响应继续隐藏供应商 ID、第三方任务编号和内部失败码。
- `aigc-gen` 创建任务前调用 `AigcTaskApi.getSuccessDurationStatistics`，优先使用 `providerId + modelId + capability` 最近成功任务平均耗时；样本为空或统计查询失败时 fallback 到模型或供应商 `timeoutSeconds`，不得阻断生成任务提交。

## 18. 测试方案

### 18.1 测试目标

测试目标是保证任务状态可靠、幂等、安全和可补偿，重点验证：

- 任务创建正确。
- 状态流转正确。
- 非法状态流转被拒绝。
- 回调幂等生效。
- 重试次数受控。
- 用户只能访问自己的任务。
- 多租户数据隔离生效。
- 失败任务可以触发退款补偿。

### 18.2 单元测试范围

#### AigcTaskServiceTest

必须覆盖：

- 创建任务成功。
- 重复 taskNo 创建失败。
- 获取任务详情成功。
- 用户获取他人任务失败。
- 状态从 CREATED 到 FROZEN 成功。
- 非法状态从 SUCCESS 到 FAILED 失败。
- 任务取消成功。
- 已提交第三方任务后取消失败。

#### AigcTaskCallbackServiceTest

必须覆盖：

- 创建回调记录成功。
- 重复回调不重复处理。
- 回调处理成功推进任务状态。
- 回调处理失败记录失败原因。
- 回调重放成功。

#### AigcTaskRetryServiceTest

必须覆盖：

- 创建重试记录成功。
- 超过最大重试次数失败。
- 待重试记录被定时任务扫描。
- 重试成功后更新状态。
- 重试失败后记录失败原因。

#### AigcTaskCompensateServiceTest

必须覆盖：

- 超时 RUNNING 任务被扫描。
- 超时 CALLBACK_WAITING 任务被扫描。
- 不超过超时时间的任务不处理。
- 失败任务触发 REFUNDING。
- billing 释放冻结成功后任务进入 REFUNDED。

### 18.3 Controller 测试范围

管理端接口：

- 未登录访问失败。
- 无权限访问失败。
- 任务分页查询正常。
- 任务详情查询正常。
- 任务取消权限校验。
- 回调记录分页查询正常。
- 回调原文不在普通列表中返回。

用户端接口：

- 用户查询自己的任务成功。
- 用户查询他人任务失败。
- 用户任务分页只返回自己的任务。
- 用户取消可取消状态任务成功。
- 用户取消不可取消状态任务失败。

### 18.4 RPC API 测试范围

`AigcTaskApi` 必须覆盖：

- `createTask` 正常创建任务。
- `markQueued` 正常推进状态。
- `markRunning` 正常推进状态。
- `markSubmitted` 记录第三方任务编号。
- `markSuccess` 记录输出资产。
- `markFailed` 记录失败原因。
- `createCallbackRecord` 幂等。
- `createRetryRecord` 正常创建重试记录。

### 18.5 边界测试用例

| 场景 | 期望 |
| ---- | ---- |
| 任务不存在 | 返回任务不存在错误 |
| 重复 taskNo | 创建失败 |
| 非法状态流转 | 返回状态流转不合法 |
| 回调重复 | 不重复推进状态 |
| 回调先于任务状态 | 等待或失败记录明确 |
| SUCCESS 后收到失败回调 | 忽略或记录但不改变状态 |
| FAILED 后收到成功回调 | 按策略人工处理，不自动成功 |
| 超过最大重试次数 | 不再重试 |
| 租户 A 查询租户 B 任务 | 查询为空或无权限 |
| 用户 A 查询用户 B 任务 | 无权限 |

### 18.6 质量门禁

提交前必须满足：

```text
mvn -pl yudao-module-aigc-task/yudao-module-aigc-task-server -am test
mvn -pl yudao-module-aigc-task/yudao-module-aigc-task-server -am -DskipTests compile
```

最低质量要求：

- Service 单元测试覆盖核心状态机。
- Controller 测试覆盖用户隔离和权限。
- RPC 测试覆盖状态推进。
- 多租户隔离测试通过。
- 编译无错误。
- 不引入旧 `yudao-module-ai` 依赖。

## 19. 开发顺序

```text
1. 创建 yudao-module-aigc-task 聚合模块
2. 创建 yudao-module-aigc-task-api
3. 创建 yudao-module-aigc-task-server
4. 配置 spring.application.name = aigc-task-server
5. 接入根 pom.xml modules
6. 定义 API DTO、枚举、错误码、AigcTaskApi
7. 创建数据库 SQL、DO、Mapper
8. 实现任务创建和查询
9. 实现状态机和状态推进
10. 实现任务日志
11. 实现回调记录和幂等
12. 实现重试记录
13. 实现超时补偿任务
14. 实现管理端 Controller
15. 实现用户端 Controller
16. 补充 RPC 实现
17. 补充 Service、Controller、RPC 测试
18. 编译、测试和接口验收
```

## 20. 与其他服务协作

### 20.1 aigc-gen 调用

`aigc-gen` 是任务状态推进的主要调用方：

```text
createTask
markQueued
markRunning
markSubmitted
markCallbackWaiting
createCallbackRecord
markDownloading
markAssetCreating
markSuccess
markFailed
```

### 20.2 aigc-billing 调用

`aigc-billing` 可以调用 `aigc-task`：

```text
getTask
markRefunding
markRefunded
```

用于扣费、退款和账务补偿时同步任务状态。

`aigc-task` 在补偿场景必须调用 `aigc-billing`：

```text
AigcBillingApi.releaseFreeze(AigcBillingReleaseReqDTO)
```

调用时机：任务超时、任务失败或可退款取消任务进入 `REFUNDING` 后，由任务补偿服务根据 `task.freezeId`、`task.id`、`task.taskNo` 组装释放冻结请求，真实调用 `aigc-billing-server` 释放冻结积分。`releaseFreeze` 调用成功后，`aigc-task` 再将任务推进到 `REFUNDED`。

失败处理：如果 `releaseFreeze` 返回错误或 RPC 调用失败，任务保持 `REFUNDING`，补偿任务后续继续扫描或由管理端人工处理，不允许在未释放冻结成功时直接标记 `REFUNDED`。

### 20.3 aigc-asset 调用

`aigc-asset` 可以调用：

```text
getTask
markAssetCreating
markSuccess
```

用于图片、视频、音频、文档、PPT、数字人视频等文件型结果入库完成后回写任务输出资产。文本、代码、翻译、摘要等非文件型结果可以不经过 `aigc-asset`，直接由 `aigc-gen` 回写 `outputText` 或 `outputData`。

### 20.4 aigc-model 调用

`aigc-task` 可以调用：

```text
getModel
calculatePrice
recordUsage
```

用于任务展示模型名称、补偿统计、记录模型调用计量。正常生成前的模型校验和价格计算优先由 `aigc-gen` 在用户提交入口完成。

## 21. 验收标准

### 21.1 管理端验收

- 可以分页查看任务。
- 可以查看任务详情。
- 可以查看任务状态流转日志。
- 可以查看第三方回调记录。
- 可以人工重试失败任务。
- 可以取消待执行任务。
- 可以查看任务统计。

### 21.2 用户端验收

- 用户可以查看自己的任务列表。
- 用户可以查看自己的任务详情。
- 用户可以查看任务进度。
- 用户可以取消未提交第三方的任务。
- 用户无法查看其他用户任务。
- 用户无法查看第三方回调原文和内部错误堆栈。

### 21.3 服务间调用验收

- `createTask` 能正确创建任务和价格快照。
- `markSubmitted` 能记录第三方任务编号。
- `createCallbackRecord` 能保证重复回调幂等。
- `markSuccess` 能记录输出资产 ID、文本结果或结构化结果索引。
- `markFailed` 能记录失败原因。
- `markRefunded` 能记录退款完成状态。
- 非法状态流转被正确拦截。

### 21.4 补偿验收

- 长时间 RUNNING 任务可被扫描。
- 长时间 CALLBACK_WAITING 任务可被扫描。
- 超时失败任务可进入退款流程，并真实调用 `AigcBillingApi.releaseFreeze` 释放冻结积分。
- billing 释放冻结成功后，任务状态进入 `REFUNDED`。
- billing 释放冻结失败时，任务保持 `REFUNDING`，可被后续补偿任务或人工操作继续处理。
- 重试次数不会超过最大限制。
- 补偿任务执行过程有日志可查。

### 21.5 测试验收

- Service 单元测试通过。
- Controller 接口测试通过。
- RPC API 测试通过。
- 状态机测试覆盖主要状态。
- 幂等测试覆盖回调、重试、退款。
- 多租户隔离测试通过。
- 编译命令通过。
- 测试命令通过。

## 22. 最终建议

`yudao-module-aigc-task` 要保持“任务状态中台”的纯粹性，只管理任务生命周期、状态机、日志、回调和补偿。

不要把第三方模型调用、文件上传、钱包扣费、模型配置、内容审核塞进任务服务。任务服务可以并且必须在补偿场景通过 `aigc-billing-api` 调用计费服务完成真实释放冻结；它只发起补偿协作，不在本模块直接修改钱包余额或计费流水。

第一阶段只要它能稳定提供：

```text
任务创建
任务状态推进
任务日志
回调幂等
失败补偿
用户任务查询
管理端任务监控
```

就足够支撑 `aigc-gen`、`aigc-billing`、`aigc-asset` 跑通文本、图片、视频、音频、代码、文档、数字人等大模型生成任务闭环。

## 23. 当前实现结果

截至本轮开发，`yudao-module-aigc-task` 已完成以下能力：

- 已接入根工程 `pom.xml`，并按 `api + server` 聚合模块结构落地。
- 已实现任务创建、任务编号生成、客户端请求幂等、任务查询、用户任务查询和用户归属校验。
- 已实现状态机合法流转校验、状态推进幂等和基于 `id + oldStatus` 的条件更新，避免并发回调、补偿和人工操作互相覆盖。
- 已实现任务日志、回调记录幂等、回调处理成功、回调处理失败、回调重放。
- 已实现重试记录、最大重试次数限制、待重试扫描、重试运行、重试成功、重试失败。
- 已实现超时补偿服务和 XXL-Job 任务：扫描 `RUNNING`、`SUBMITTED`、`CALLBACK_WAITING` 超时任务，推进 `FAILED -> REFUNDING`。
- 已真实调用 `AigcBillingApi.releaseFreeze` 释放冻结积分，释放成功后推进任务到 `REFUNDED`。
- 已实现管理端任务详情、分页、取消、人工标记失败、统计、日志分页、回调详情/分页/重放、重试分页/取消/触发。
- 已实现用户端任务详情、分页、取消、进度查询，并对成本价、渠道商、第三方任务编号、内部失败码做脱敏。
- 已扩展任务观测统计和预计耗时链路：管理端统计返回 P95 耗时、重试任务数；RPC 提供按 `providerId + modelId + capability` 查询最近成功任务平均耗时和 P95 耗时；`aigc-gen` 创建任务时写入 `estimatedDurationMillis`，用户端可按预计耗时平滑展示进度条。
- 已实现独立 OpenAPI 分组 `aigc-task`。
- 已补充任务服务、RPC、回调、重试、补偿测试，当前 `20` 个自动化测试用例通过。

当前已通过以下质量门禁：

```text
mvn -pl yudao-module-aigc-task/yudao-module-aigc-task-server -am test
mvn -pl yudao-module-aigc-task/yudao-module-aigc-task-server -am -DskipTests compile
```

测试日志中本地 Nacos `127.0.0.1:9848` 连接失败属于单元测试环境未启动 Nacos 的噪声，不影响测试结果。生产环境需要确保 `aigc-task-server` 可通过 Nacos 发现 `aigc-billing-server`。

## 24. 多 Attempt 自动重试下的任务状态边界

生成服务后续支持多 attempt fallback 后，`aigc-task` 仍然只维护用户视角的主任务生命周期，不直接感知每一次供应商尝试的细节。

### 24.1 主任务与 Attempt 的关系

- `aigc_task` 对应一次用户任务。
- `aigc_gen_record` 对应一次用户生成主单。
- `aigc_gen_attempt` 对应一次内部供应商尝试。

任务服务不需要保存 attempt 明细，attempt 明细归 `aigc-gen` 管理。任务服务只接收生成服务对主任务的状态推进。

### 24.2 单次 Attempt 失败不等于主任务失败

当某次供应商调用失败时：

- 不调用 `markFailed`。
- 不进入 `REFUNDING` 或 `REFUNDED`。
- 不触发任务级失败统计。
- 由 `aigc-gen` 继续尝试下一个渠道或供应商。

只有所有可重试、可 fallback、可 hedging 的候选都耗尽后，`aigc-gen` 才能调用 `markFailed`，并进入后续退款或释放冻结流程。

### 24.3 建议任务状态映射

用户端不需要看到内部切换供应商过程。任务状态建议按以下方式映射：

| gen 内部状态 | task 状态 | 用户展示 |
| ---- | ---- | ---- |
| `SUBMITTING` | `RUNNING` | 生成中 |
| `RETRYING` | `RUNNING` | 生成中 |
| `FALLBACKING` | `RUNNING` | 生成中 |
| `HEDGING` | `RUNNING` 或 `CALLBACK_WAITING` | 生成中 |
| winner 成功 | `SUCCESS` | 成功 |
| 所有 attempt 失败 | `FAILED -> REFUNDING -> REFUNDED` | 失败或已释放冻结 |

如果未来需要管理端可见内部状态，可以在任务日志中记录 `ATTEMPT_FAILED`、`CHANNEL_RETRY`、`PROVIDER_FALLBACK`、`HEDGING_START`、`HEDGING_WINNER` 等事件，但不建议扩展用户端任务状态。

### 24.4 重试记录边界

现有 `aigc_task_retry` 更适合任务级人工重试、补偿扫描或最终失败后的再执行，不建议直接复用为每一次供应商 attempt。

原因：

- attempt 需要记录 `channel_id`、`provider_id`、`provider_task_id`、成本、winner、并发批次。
- `aigc_task_retry` 当前是任务级重试，字段粒度不足。
- 将 attempt 塞进 task retry 会让任务服务承担供应商调度细节，破坏模块边界。

建议：

- 自动切渠道、切供应商、并发兜底：使用 `aigc_gen_attempt`。
- 人工重跑整个任务、补偿任务级失败：继续使用 `aigc_task_retry`。

### 24.5 补偿要求

任务补偿扫描不能只看到长时间 `RUNNING` 就直接失败退款。自动 fallback 落地后，需要先询问 `aigc-gen` 当前主单是否仍有 active attempt 或可继续 fallback：

- 有 active attempt：保持运行中，必要时触发 attempt 同步。
- 无 active attempt 但还有候选：由 `aigc-gen` 创建下一批 attempt。
- 无候选且均失败：任务服务才允许进入最终失败和退款流程。

任务服务仍然负责最终状态机合法性和日志审计，但供应商尝试策略由生成服务负责。
