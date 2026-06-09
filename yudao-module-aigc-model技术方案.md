# yudao-module-aigc-model 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置

## 1. 模块定位

`yudao-module-aigc-model` 是 AIGC 平台的模型与渠道中台，负责统一管理第三方模型供应商、模型配置、模型能力、参数模板、价格规则、租户模型授权、基础路由策略和模型调用计量。

该模块不负责实际调用模型生成内容，不负责图片/视频任务执行，不负责钱包扣费，只提供模型相关的配置、校验、价格计算、租户可用模型查询和调用计量落库能力。

当前实际开发已完成 `api` 与 `server` 两个子模块，已具备模型、渠道商、参数模板、价格规则、路由规则、租户授权、调用计量、用户端查询和内部 RPC 能力。本文档已按 `c:\use\code\project\manman\yudao-module-aigc-model` 的当前代码实现同步修订。

## 2. 核心职责

### 2.1 负责内容

- 渠道商管理
- 渠道商账号和鉴权配置
- 模型管理
- 模型能力配置
- 模型参数模板配置
- 模型价格配置
- 模型启停与展示控制
- 模型可用性校验
- 模型参数合法性校验
- 任务预估价格计算
- 基础模型路由配置
- 渠道健康状态记录
- 租户模型授权和租户维度展示控制
- 模型调用计量日志记录

### 2.2 不负责内容

- 不直接提交图片生成任务
- 不直接提交视频生成任务
- 不处理第三方模型回调
- 不保存生成结果
- 不保存图片、视频资产
- 不冻结积分
- 不扣费和退款
- 不管理用户钱包
- 不做内容审核

对应职责归属：


| 能力                   | 归属模块                    |
| ---------------------- | --------------------------- |
| 图片/视频生成          | `yudao-module-aigc-gen`     |
| 任务状态机             | `yudao-module-aigc-task`    |
| 钱包、冻结、扣费、退款 | `yudao-module-aigc-billing` |
| 图片、视频文件资产     | `yudao-module-aigc-asset`   |
| 敏感词、审核           | `yudao-module-aigc-safety`  |

## 3. 模块结构

### 3.1 Maven 结构

```text
yudao-module-aigc-model
  ├── yudao-module-aigc-model-api
  └── yudao-module-aigc-model-server
```

### 3.2 根包名

```text
cn.iocoder.yudao.module.aigc.model
```

### 3.3 API 模块结构

```text
yudao-module-aigc-model-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/model
      ├── api
      │   └── AigcModelApi.java
      ├── dto
      │   ├── AigcModelRespDTO.java
      │   ├── AigcModelProviderRespDTO.java
      │   ├── AigcModelParamTemplateRespDTO.java
      │   ├── AigcModelPriceCalculateReqDTO.java
      │   ├── AigcModelPriceCalculateRespDTO.java
      │   ├── AigcModelValidateReqDTO.java
      │   └── AigcModelUsageRecordReqDTO.java
      └── enums
          ├── AigcModelTypeEnum.java
          ├── AigcModelCapabilityEnum.java
          ├── AigcModelProviderAuthTypeEnum.java
          ├── AigcModelBillingUnitEnum.java
          ├── AigcModelParamTypeEnum.java
          ├── AigcModelRouteStrategyEnum.java
          ├── AigcModelHealthStatusEnum.java
          └── ErrorCodeConstants.java
```

### 3.4 Server 模块结构

```text
yudao-module-aigc-model-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/model
      ├── AigcModelServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── provider
      │   │   ├── proxy
      │   │   ├── model
      │   │   ├── param
      │   │   ├── price
      │   │   ├── route
      │   │   └── tenant
      │   └── app
      │       └── model
      ├── framework
      │   ├── crypto
      │   └── web
      │       └── config
      │           └── AigcModelWebConfiguration.java
      ├── service
      │   ├── provider
      │   ├── proxy
      │   ├── model
      │   ├── param
      │   ├── price
      │   ├── route
      │   ├── tenant
      │   └── usage
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcModelProviderDO.java
      │   │   ├── AigcModelProxyDO.java
      │   │   ├── AigcModelDO.java
      │   │   ├── AigcModelCapabilityDO.java
      │   │   ├── AigcModelParamTemplateDO.java
      │   │   ├── AigcModelPriceDO.java
      │   │   ├── AigcModelRouteDO.java
      │   │   ├── AigcModelTenantDO.java
      │   │   └── AigcModelUsageLogDO.java
      │   └── mysql
      │       ├── AigcModelProviderMapper.java
      │       ├── AigcModelProxyMapper.java
      │       ├── AigcModelMapper.java
      │       ├── AigcModelCapabilityMapper.java
      │       ├── AigcModelParamTemplateMapper.java
      │       ├── AigcModelPriceMapper.java
      │       ├── AigcModelRouteMapper.java
      │       ├── AigcModelTenantMapper.java
      │       └── AigcModelUsageLogMapper.java
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
        <artifactId>yudao-module-aigc-model-api</artifactId>
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
        <artifactId>yudao-spring-boot-starter-excel</artifactId>
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

## 5. 数据库设计

### 5.1 表清单


| 表名                        | 说明             | 当前实现 |
| --------------------------- | ---------------- | -------- |
| `aigc_model_provider`       | 模型渠道商       | 已实现   |
| `aigc_model_proxy`          | 共享代理配置     | 已实现   |
| `aigc_model`                | 模型配置         | 已实现   |
| `aigc_model_capability`     | 模型能力         | 已实现   |
| `aigc_model_param_template` | 模型参数模板     | 已实现   |
| `aigc_model_price`          | 模型价格规则     | 已实现   |
| `aigc_model_route`          | 模型路由规则     | 已实现   |
| `aigc_model_tenant`         | 租户模型授权     | 已实现   |
| `aigc_model_usage_log`      | 模型调用计量日志 | 已实现   |

### 5.2 aigc_model_provider

模型渠道商表，管理可灵、即梦、OpenAI、通义、第三方聚合 API 等供应商。


| 字段                                                      | 类型          | 说明                 |
| --------------------------------------------------------- | ------------- | -------------------- |
| id                                                        | bigint        | 主键                 |
| code                                                      | varchar(64)   | 渠道商编码           |
| name                                                      | varchar(128)  | 渠道商名称           |
| api_base_url                                              | varchar(512)  | API 地址             |
| auth_type                                                 | varchar(32)   | 鉴权方式             |
| api_key                                                   | varchar(1024) | API Key，加密存储    |
| secret_key                                                | varchar(1024) | Secret Key，加密存储 |
| extra_config                                              | json          | 扩展配置             |
| timeout_seconds                                           | int           | 默认超时时间         |
| proxy_enabled                                             | tinyint(1)    | 是否启用代理         |
| proxy_id                                                  | bigint        | 共享代理 ID          |
| proxy_protocol/proxy_host/proxy_port                      | 标准字段      | 旧版内联代理兼容字段 |
| proxy_username/proxy_password                             | 标准字段      | 旧版内联代理兼容字段 |
| rate_limit_config                                         | json          | 限流配置             |
| health_status                                             | varchar(32)   | 健康状态             |
| balance                                                   | decimal(18,6) | 渠道余额             |
| status                                                    | int           | 状态                 |
| remark                                                    | varchar(512)  | 备注                 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段      | 标准字段             |

索引：

```text
uk_code_tenant = code + tenant_id
idx_status = status
```

说明：

- 新配置应优先使用 `proxy_id` 关联 `aigc_model_proxy`，不要在每个渠道商上重复填写代理主机、端口和账号。
- `proxy_protocol`、`proxy_host`、`proxy_port`、`proxy_username`、`proxy_password` 是旧版内联代理字段，保留用于兼容已有数据和回滚，不作为新增配置的主入口。
- 后端读取渠道商时会展开共享代理配置，供 `aigc-gen` 调用模型和 `aigc-asset` 下载生成结果文件使用。

### 5.3 aigc_model_proxy

共享代理配置表，管理 HTTP、SOCKS5、SOCKS5H 等代理。适合 Grok、OpenAI、Gemini 等境外模型渠道复用同一代理，避免每个渠道商重复配置。

| 字段                                                      | 类型          | 说明                   |
| --------------------------------------------------------- | ------------- | ---------------------- |
| id                                                        | bigint        | 主键                   |
| name                                                      | varchar(128)  | 代理名称               |
| protocol                                                  | varchar(32)   | HTTP、SOCKS5、SOCKS5H  |
| host                                                      | varchar(255)  | 代理主机               |
| port                                                      | int           | 代理端口               |
| username                                                  | varchar(255)  | 用户名，可为空         |
| password                                                  | varchar(1024) | 密码，可为空，敏感字段 |
| status                                                    | int           | 状态                   |
| remark                                                    | varchar(512)  | 备注                   |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段      | 标准字段               |

索引：

```text
idx_name = name
idx_status = status
```

管理端页面位于 AIGC 模型管理下的“代理管理”。页面支持新增、编辑、删除、启停和测试代理。测试接口当前以 `https://api.ipify.org` 为目标，只验证代理连通性、认证和延迟，不代表每个业务目标域名都一定可访问。Grok 图片结果下载仍需要单独验证 `https://imagine-public.x.ai/...`。

### 5.4 aigc_model

模型配置表，管理用户可以选择或平台内部可以路由的具体模型。


| 字段                                                      | 类型         | 说明                      |
| --------------------------------------------------------- | ------------ | ------------------------- |
| id                                                        | bigint       | 主键                      |
| provider_id                                               | bigint       | 渠道商 ID                 |
| code                                                      | varchar(64)  | 平台内部模型编码          |
| name                                                      | varchar(128) | 模型展示名称              |
| model                                                     | varchar(128) | 渠道商模型标识            |
| type                                                      | varchar(32)  | IMAGE、VIDEO、AUDIO、TEXT |
| public_visible                                            | bit(1)       | 用户端是否展示            |
| default_model                                             | bit(1)       | 是否默认模型              |
| sort                                                      | int          | 排序                      |
| max_concurrent                                            | int          | 最大并发                  |
| timeout_seconds                                           | int          | 模型超时时间              |
| queue_priority                                            | int          | 默认队列优先级            |
| status                                                    | int          | 启用/禁用                 |
| remark                                                    | varchar(512) | 备注                      |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段     | 标准字段                  |

索引：

```text
uk_code_tenant = code + tenant_id
idx_provider_id = provider_id
idx_type_status = type + status
idx_public_visible = public_visible
```

### 5.5 aigc_model_capability

模型能力表，一个模型可以支持多个能力。


| 字段       | 类型         | 说明     |
| ---------- | ------------ | -------- |
| id         | bigint       | 主键     |
| model_id   | bigint       | 模型 ID  |
| capability | varchar(64)  | 能力编码 |
| status     | int          | 状态     |
| remark     | varchar(512) | 备注     |

能力编码：


| 编码                   | 说明       |
| ---------------------- | ---------- |
| TEXT_TO_IMAGE          | 文生图     |
| IMAGE_TO_IMAGE         | 图生图     |
| TEXT_TO_VIDEO          | 文生视频   |
| IMAGE_TO_VIDEO         | 图生视频   |
| FIRST_LAST_FRAME_VIDEO | 首尾帧视频 |
| VIDEO_EXTEND           | 视频延长   |

索引：

```text
uk_model_capability = model_id + capability
idx_capability = capability
```

### 5.6 aigc_model_param_template

模型参数模板表，用于配置不同模型支持的参数，避免前后端写死。


| 字段            | 类型          | 说明     |
| --------------- | ------------- | -------- |
| id              | bigint        | 主键     |
| model_id        | bigint        | 模型 ID  |
| capability      | varchar(64)   | 能力编码 |
| param_key       | varchar(64)   | 参数键   |
| param_name      | varchar(128)  | 参数名称 |
| param_type      | varchar(32)   | 参数类型 |
| required_status | bit(1)        | 是否必填 |
| default_value   | varchar(512)  | 默认值   |
| options         | json          | 可选值   |
| min_value       | decimal(18,6) | 最小值   |
| max_value       | decimal(18,6) | 最大值   |
| regex_pattern   | varchar(512)  | 正则校验 |
| sort            | int           | 排序     |
| status          | int           | 状态     |

参数类型：


| 类型         | 说明   |
| ------------ | ------ |
| STRING       | 字符串 |
| NUMBER       | 数字   |
| BOOLEAN      | 布尔   |
| SELECT       | 单选   |
| MULTI_SELECT | 多选   |
| JSON         | JSON   |

常见参数：


| 参数           | 说明                     |
| -------------- | ------------------------ |
| ratio          | 比例，如 1:1、9:16、16:9 |
| width          | 宽度                     |
| height         | 高度                     |
| duration       | 视频时长                 |
| resolution     | 分辨率                   |
| style          | 风格                     |
| seed           | 种子                     |
| batchSize      | 批量数量                 |
| cameraMovement | 运镜                     |

索引：

```text
uk_model_capability_param = model_id + capability + param_key
idx_model_id = model_id
```

### 5.7 aigc_model_price

模型价格规则表。


| 字段                 | 类型          | 说明                   |
| -------------------- | ------------- | ---------------------- |
| id                   | bigint        | 主键                   |
| model_id             | bigint        | 模型 ID                |
| capability           | varchar(64)   | 能力编码               |
| billing_unit         | varchar(32)   | 计费单位               |
| cost_price           | decimal(18,6) | 平台成本价             |
| sale_price           | decimal(18,6) | 用户销售价             |
| currency_type        | varchar(32)   | POINT、CNY             |
| price_config         | json          | 阶梯价格、规格加价配置 |
| effective_start_time | datetime      | 生效开始时间           |
| effective_end_time   | datetime      | 生效结束时间           |
| status               | int           | 状态                   |

计费单位：


| 编码          | 说明    |
| ------------- | ------- |
| PER_TASK      | 按任务  |
| PER_IMAGE     | 按张    |
| PER_SECOND    | 按秒    |
| PER_5_SECONDS | 每 5 秒 |
| PER_BATCH     | 按批次  |

价格配置示例：

```json
{
  "durationMultiplier": true,
  "resolutionExtra": {
    "720p": 0,
    "1080p": 20
  },
  "batchMultiplier": true
}
```

### 5.8 aigc_model_route

模型路由规则表，用于将用户端选择的“展示模型”路由到一个或多个真实执行模型。展示模型负责用户端可见名称、参数模板和价格规则；执行模型负责绑定真实渠道商、第三方模型标识和实际调用配置。


| 字段       | 类型         | 说明        |
| ---------- | ------------ | ----------- |
| id         | bigint       | 主键        |
| name       | varchar(128) | 路由名称    |
| task_type  | varchar(64)  | 展示模型编码 |
| capability | varchar(64)  | 能力        |
| strategy   | varchar(64)  | 路由策略    |
| model_ids  | json         | 候选模型 ID |
| user_level | varchar(64)  | 用户等级    |
| status     | int          | 状态        |

路由策略：

- `FIXED_MODEL`：固定使用候选模型列表第一个。
- `ROUND_ROBIN`：在候选模型列表中轮询。
- `LOWEST_COST`、`HIGHEST_SUCCESS_RATE`、`FASTEST_RESPONSE`：当前预留，暂按候选模型列表第一个执行，后续接入成本、成功率和延迟统计后再实现。

配置规则：

- `task_type` 填展示模型的 `code`，例如用户端展示模型 `GPT Image 2` 的编码 `gpt-image-2`。
- `capability` 填该路由适用的能力。管理端新增时支持多选能力，系统会按每个能力创建一条路由规则。
- `model_ids` 填真实执行模型 ID 列表。管理端以模型名称多选，保存为 JSON 数组。
- 未配置路由、路由禁用、策略异常、候选模型为空时，系统回退到用户端传入的原始展示模型。

推荐配置示例：

| 模型角色 | 模型名称 | 用户端展示 | 供应商 | 说明 |
| -------- | -------- | ---------- | ------ | ---- |
| 展示模型 | GPT Image 2 | 开启 | 任一可用供应商 | 用户端只看到这一条；参数模板和价格规则配置在这条模型上 |
| 执行模型 | GPT Image 2 - Copse | 关闭 | Copse | 真实调用 Copse 渠道 |
| 执行模型 | GPT Image 2 - SubRouter | 关闭 | SubRouter.ai | 真实调用 SubRouter 渠道 |

以上三条模型都需要配置能力并在租户模型授权中启用；执行模型可以不对用户端展示。

### 5.9 aigc_model_tenant

租户模型授权表，用于控制某个租户可使用哪些平台模型，以及租户维度的展示、默认、排序和额度策略。当前生产 SQL 已建表，Server 侧已有 `AigcModelTenantDO`、`AigcModelTenantMapper`、`AigcModelTenantService` 和管理端 Controller。

| 字段           | 类型         | 说明               |
| -------------- | ------------ | ------------------ |
| id             | bigint       | 主键               |
| tenant_id      | bigint       | 租户 ID            |
| model_id       | bigint       | 模型 ID            |
| enabled        | bit(1)       | 租户是否启用该模型 |
| public_visible | bit(1)       | 用户端是否展示     |
| default_model  | bit(1)       | 是否租户默认模型   |
| sort           | int          | 租户内排序         |
| max_concurrent | int          | 租户模型并发限制   |
| daily_limit    | int          | 租户日调用限制     |
| remark         | varchar(512) | 备注               |

索引：

```text
uk_tenant_model = tenant_id + model_id
idx_model_id = model_id
```

### 5.10 aigc_model_usage_log

模型调用计量日志表，用于记录模型调用结果，给后续结算、统计、审计和成本分析提供基础数据。当前已通过内部 RPC `recordUsage` 暴露记录能力。

| 字段             | 类型          | 说明                     |
| ---------------- | ------------- | ------------------------ |
| id               | bigint        | 主键                     |
| trace_id         | varchar(128)  | 链路追踪编号             |
| task_id          | bigint        | 业务任务 ID              |
| user_id          | bigint        | 用户 ID                  |
| model_id         | bigint        | 模型 ID                  |
| provider_id      | bigint        | 渠道商 ID                |
| capability       | varchar(64)   | 模型能力                 |
| request_no       | varchar(128)  | 内部请求编号             |
| external_task_id | varchar(128)  | 第三方任务编号           |
| input_tokens     | int           | 输入 token 数            |
| output_tokens    | int           | 输出 token 数            |
| total_tokens     | int           | 总 token 数              |
| cost_price       | decimal(18,6) | 平台成本价               |
| sale_price       | decimal(18,6) | 用户销售价               |
| currency_type    | varchar(32)   | 货币类型                 |
| status           | int           | 调用状态                 |
| latency_ms       | int           | 调用耗时                 |
| raw_usage        | json          | 第三方原始 usage 信息    |
| error_code       | varchar(128)  | 错误码                   |
| error_message    | varchar(1024) | 错误信息                 |
| tenant_id        | bigint        | 租户 ID，由租户插件维护  |

索引：

```text
idx_task_id = task_id
idx_model_id = model_id
idx_provider_id = provider_id
idx_trace_id = trace_id
idx_create_time = create_time
```

## 6. 枚举设计

### 6.1 AigcModelTypeEnum

```text
TEXT
IMAGE
VIDEO
AUDIO
AUDIT
```

第一阶段只启用：

```text
IMAGE
VIDEO
```

### 6.2 AigcModelCapabilityEnum

```text
TEXT_TO_IMAGE
IMAGE_TO_IMAGE
TEXT_TO_VIDEO
IMAGE_TO_VIDEO
FIRST_LAST_FRAME_VIDEO
VIDEO_EXTEND
```

### 6.3 AigcModelProviderAuthTypeEnum

```text
API_KEY
BEARER_TOKEN
AK_SK
CUSTOM_HEADER
NONE
```

### 6.4 AigcModelBillingUnitEnum

```text
PER_TASK
PER_IMAGE
PER_SECOND
PER_5_SECONDS
PER_BATCH
```

### 6.5 AigcModelHealthStatusEnum

```text
UNKNOWN
HEALTHY
UNHEALTHY
LIMITED
BALANCE_LOW
```

## 7. RPC API 设计

### 7.1 AigcModelApi

`AigcModelApi` 放在 `yudao-module-aigc-model-api`，供 `aigc-gen`、`aigc-task`、`aigc-billing` 调用。

核心接口：

```java
@FeignClient(name = ApiConstants.NAME)
public interface AigcModelApi {

    CommonResult<AigcModelRespDTO> validateModel(Long modelId, String capability);

    CommonResult<AigcModelProviderRespDTO> getProvider(Long providerId);

    CommonResult<AigcModelRespDTO> getModel(Long modelId);

    CommonResult<List<AigcModelRespDTO>> listAvailableModels(Integer type, String capability);

    CommonResult<List<AigcModelParamTemplateRespDTO>> getParamTemplates(Long modelId, String capability);

    CommonResult<Boolean> validateParams(AigcModelValidateReqDTO reqDTO);

    CommonResult<AigcModelPriceCalculateRespDTO> calculatePrice(AigcModelPriceCalculateReqDTO reqDTO);

    CommonResult<Long> recordUsage(AigcModelUsageRecordReqDTO reqDTO);

}
```

当前实际 RPC 路径均基于 `ApiConstants.PREFIX` 拼接：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/validate-model` | 校验模型是否可用 |
| GET | `/get-provider` | 获取内部调用所需渠道商信息 |
| GET | `/get-model` | 获取模型详情 |
| GET | `/list-available-models` | 获取当前租户可用模型列表 |
| GET | `/get-param-templates` | 获取模型参数模板 |
| POST | `/validate-params` | 校验模型参数 |
| POST | `/calculate-price` | 计算模型价格 |
| POST | `/record-usage` | 记录模型调用计量 |

### 7.2 validateModel

输入：


| 字段       | 说明         |
| ---------- | ------------ |
| modelId    | 模型 ID      |
| capability | 要使用的能力 |

校验：

- 校验用户端传入的展示模型存在、启用、渠道商启用、支持该能力、当前租户已授权并启用。
- 使用展示模型 `code + capability` 查询启用状态的路由规则。
- 如果路由命中候选执行模型，则按路由策略选出真实执行模型，并重新校验该执行模型存在、启用、渠道商启用、支持该能力、当前租户已授权并启用。
- 如果路由未命中或路由配置不可用，则回退到展示模型自身。

返回：

- 实际执行模型信息
- 实际执行渠道商 ID
- 实际执行模型标识
- 超时时间
- 最大并发

不返回：

- API Key
- Secret Key

### 7.3 getProvider

该接口仅内部服务可调用，返回渠道商调用所需信息。

安全要求：

- API Key 解密只在 server 内部完成。
- 不允许暴露给前端。
- DTO 中敏感字段不出现在管理端普通查询接口中。

### 7.4 calculatePrice

输入：


| 字段       | 说明     |
| ---------- | -------- |
| modelId    | 模型 ID  |
| capability | 能力     |
| taskType   | 任务类型 |
| params     | 生成参数 |

输出：


| 字段        | 说明       |
| ----------- | ---------- |
| costPrice   | 平台成本价 |
| salePrice   | 用户销售价 |
| billingUnit | 计费单位   |
| priceDetail | 价格明细   |

### 7.5 recordUsage

`recordUsage` 用于由 `aigc-gen`、`aigc-task` 或模型调用执行方在模型调用完成后记录计量日志，当前实现会写入 `aigc_model_usage_log`。

输入字段包括：

| 字段 | 说明 |
| ---- | ---- |
| traceId | 链路追踪编号 |
| taskId | 业务任务 ID |
| userId | 用户 ID |
| modelId | 模型 ID |
| providerId | 渠道商 ID |
| capability | 模型能力 |
| requestNo | 内部请求编号 |
| externalTaskId | 第三方任务编号 |
| inputTokens/outputTokens/totalTokens | token 计量信息 |
| costPrice/salePrice/currencyType | 价格快照 |
| status/latencyMs | 调用结果与耗时 |
| rawUsage | 第三方原始 usage 信息 |
| errorCode/errorMessage | 失败时错误信息 |

返回：

- 新增的调用计量日志 ID。

## 8. 管理端接口设计

### 8.1 渠道商管理

Controller：`AigcModelProviderController`

路径：`/aigc/model/provider`

接口：


| 方法   | 路径             | 说明           |
| ------ | ---------------- | -------------- |
| POST   | `/create`        | 新增渠道商     |
| PUT    | `/update`        | 修改渠道商     |
| DELETE | `/delete`        | 删除渠道商     |
| GET    | `/get`           | 渠道商详情     |
| GET    | `/page`          | 渠道商分页     |
| PUT    | `/update-status` | 启用/禁用      |
| POST   | `/test`          | 测试渠道连通性 |

### 8.2 模型管理

Controller：`AigcModelController`

路径：`/aigc/model`

接口：


| 方法   | 路径              | 说明               |
| ------ | ----------------- | ------------------ |
| POST   | `/create`         | 新增模型           |
| PUT    | `/update`         | 修改模型           |
| DELETE | `/delete`         | 删除模型           |
| GET    | `/get`            | 模型详情           |
| GET    | `/page`           | 模型分页           |
| PUT    | `/update-status`  | 启用/禁用          |
| PUT    | `/update-visible` | 修改用户端展示状态 |
| PUT    | `/update-default` | 设置默认模型       |

### 8.3 参数模板管理

Controller：`AigcModelParamController`

路径：`/aigc/model/param`

接口：


| 方法   | 路径          | 说明             |
| ------ | ------------- | ---------------- |
| POST   | `/create`     | 新增参数         |
| PUT    | `/update`     | 修改参数         |
| DELETE | `/delete`     | 删除参数         |
| GET    | `/list`       | 模型参数列表     |
| POST   | `/batch-save` | 批量保存参数模板 |

### 8.4 价格管理

Controller：`AigcModelPriceController`

路径：`/aigc/model/price`

接口：


| 方法   | 路径         | 说明         |
| ------ | ------------ | ------------ |
| POST   | `/create`    | 新增价格规则 |
| PUT    | `/update`    | 修改价格规则 |
| DELETE | `/delete`    | 删除价格规则 |
| GET    | `/list`      | 模型价格列表 |
| POST   | `/calculate` | 模拟价格计算 |

### 8.5 路由管理

Controller：`AigcModelRouteController`

路径：`/aigc/model/route`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/create` | 新增路由规则 |
| PUT | `/update` | 修改路由规则 |
| DELETE | `/delete` | 删除路由规则 |
| GET | `/get` | 路由规则详情 |
| GET | `/list` | 路由规则列表 |
| PUT | `/update-status` | 启用/禁用路由规则 |

### 8.6 租户模型授权管理

Controller：`AigcModelTenantController`

路径：`/aigc/model/tenant`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/create` | 创建租户模型授权 |
| PUT | `/update` | 更新租户模型授权 |
| DELETE | `/delete` | 删除租户模型授权 |
| GET | `/get` | 获取租户模型授权详情 |
| GET | `/list` | 获取租户模型授权列表 |
| PUT | `/status` | 更新租户模型启用状态 |
| PUT | `/visible` | 更新租户模型用户端可见性 |
| PUT | `/default` | 更新租户默认模型 |

## 9. 用户端接口设计

Controller：`AigcModelAppController`

代码路径：`/aigc/model`

网关对用户端接口可统一增加 `/app-api` 前缀，最终外部路径以网关配置为准。

接口：


| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 获取当前租户可见模型详情 |
| GET | `/list` | 获取当前租户可用模型列表 |
| POST | `/price/calculate` | 计算预计消耗 |
| GET | `/param/list` | 获取模型参数模板 |

`/list` 支持按 `type` 和 `capability` 同时过滤，用户端快捷生成和 canvas 节点必须使用当前输入形态对应的能力查询模型：

| 输入形态 | type | capability |
| -------- | ---- | ---------- |
| 文生图   | `2`  | `TEXT_TO_IMAGE` |
| 图生图   | `2`  | `IMAGE_TO_IMAGE` |
| 文生视频 | `3`  | `TEXT_TO_VIDEO` |
| 图生视频 | `3`  | `IMAGE_TO_VIDEO` |

`/param/list` 必须按 `modelId + capability` 返回启用状态的参数模板。前端会把模板渲染成动态参数表单，并在提交前把用户选择值序列化到生成请求 `inputParams`。后端仍需在生成入口调用模型服务进行参数合法性校验，不能只信任前端默认值或表单校验。

用户端返回模型时只返回：

- 模型 ID
- 模型名称
- 模型类型
- 模型能力
- 展示排序
- 价格展示信息
- 参数模板

不返回：

- 渠道商 API 地址
- API Key
- Secret Key
- 成本价
- 内部路由规则

## 10. API 前缀与 Swagger 聚合规范

### 10.1 API 前缀规范

本模块复用项目 Web 基建自动添加 API 前缀，Controller 只声明模块业务路径，不允许在 `@RequestMapping` 中手写 `/admin-api` 或 `/app-api`。

基建规则：

- `cn.iocoder.yudao.module.aigc.model.controller.admin` 包下的 Controller 自动添加 `/admin-api` 前缀。
- `cn.iocoder.yudao.module.aigc.model.controller.app` 包下的 Controller 自动添加 `/app-api` 前缀。
- Controller 中的路径必须从 `/aigc/...` 开始，保持与 Gateway 路由和 Swagger 分组路径一致。

示例：

| Controller 包路径 | Controller 代码路径 | 外部最终路径 |
| ----------------- | ------------------- | ------------ |
| `controller.admin` | `/aigc/model` | `/admin-api/aigc/model` |
| `controller.admin` | `/aigc/model/provider` | `/admin-api/aigc/model/provider` |
| `controller.app` | `/aigc/model` | `/app-api/aigc/model` |

规范约定：

- 管理端 Controller 必须放在 `controller.admin` 包下。
- 用户端 Controller 必须放在 `controller.app` 包下。
- 禁止为了适配 Gateway 或 Swagger，在 Controller 路径上重复添加 `/admin-api`、`/app-api`。
- 新增 AIGC 模型子资源时，路径统一挂在 `/aigc/model/**` 或 `/aigc/**` 下，避免出现多个不一致的模块前缀。

### 10.2 Swagger 分组规范

本模块必须注册独立的 OpenAPI 分组 `aigc`，保证 Gateway 的 Knife4j 聚合页可以按 `aigc-model-server` 正确展示模块接口。

已落地配置类：

```java
package cn.iocoder.yudao.module.aigc.model.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AigcModelWebConfiguration {

    @Bean
    public GroupedOpenApi aigcGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("aigc");
    }

}
```

该配置会匹配：

- `/admin-api/aigc/**`
- `/app-api/aigc/**`

规范约定：

- AIGC 模型服务的 Swagger 分组名固定为 `aigc`。
- 配置类固定放在 `cn.iocoder.yudao.module.aigc.model.framework.web.config` 包下。
- Bean 方法命名固定为 `aigcGroupedOpenApi`。
- 不新增与 `aigc` 含义重复的 Swagger 分组，避免 Knife4j 聚合页出现重复模型文档。
- 新增 Controller 后，必须确认接口能进入 `/v3/api-docs/aigc` 文档分组。

### 10.3 Gateway 聚合规范

Gateway 必须配置 AIGC 服务路由和 Knife4j 聚合入口。

Gateway 路由约定：

```yaml
- id: aigc-model-admin-api
  uri: grayLb://aigc-model-server
  predicates:
    - Path=/admin-api/aigc/**
  filters:
    - RewritePath=/admin-api/aigc/v3/api-docs, /v3/api-docs
- id: aigc-model-app-api
  uri: grayLb://aigc-model-server
  predicates:
    - Path=/app-api/aigc/**
  filters:
    - RewritePath=/app-api/aigc/v3/api-docs, /v3/api-docs
```

Knife4j 聚合约定：

```yaml
knife4j:
  gateway:
    routes:
      - name: aigc-model-server
        service-name: aigc-model-server
        url: /admin-api/aigc/v3/api-docs
```

规范约定：

- `service-name` 必须与 AIGC 服务 `spring.application.name` 保持一致，即 `aigc-model-server`。
- Gateway 对外文档地址使用 `/admin-api/aigc/v3/api-docs`，由 `RewritePath` 转发到服务内部 `/v3/api-docs`。
- 如果 Gateway 运行时配置来自 Nacos，必须同步更新 Nacos 中对应 `gateway-server-${profile}.yaml`，不能只修改本地 `application.yaml`。
- Gateway 与 AIGC 服务必须注册到同一个 Nacos namespace/group，否则路由存在也无法发现实例。

### 10.4 验证规范

每次新增或调整 AIGC Controller、Swagger 分组、Gateway 聚合配置后，必须按以下顺序验证：

1. 编译验证：`mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am -DskipTests compile`
2. 服务内文档：访问 `http://localhost:48090/v3/api-docs/aigc`
3. Gateway 文档转发：访问 `http://localhost:48080/admin-api/aigc/v3/api-docs/aigc`
4. Knife4j 聚合页：访问 `http://localhost:48080/doc.html#/SwaggerModels/aigc-model-server`

如果服务内文档正常但 Gateway 文档不正常，优先检查 Gateway 路由、Nacos 服务发现和 namespace/group。  
如果 Gateway 文档正常但 Knife4j 聚合页不展示，优先检查 Gateway 运行时加载的 `knife4j.gateway.routes` 是否包含 `aigc-model-server`。

## 11. 核心流程

### 10.1 提交生成前校验流程

```text
aigc-gen 接收用户生成请求
  ↓
调用 aigc-model.validateModel(modelId, capability)
  ↓
aigc-model 校验展示模型并按路由规则解析实际执行模型
  ↓
调用 aigc-model.validateParams(modelId, params)
  ↓
aigc-model 按展示模型校验参数模板
  ↓
调用 aigc-model.calculatePrice(modelId, capability, params)
  ↓
按展示模型返回用户侧销售价和成本价
  ↓
aigc-gen 使用实际执行模型创建任务、记录 providerId，并调用 billing 冻结积分
```

说明：

- 请求中的 `modelId` 是用户端选择的展示模型 ID。
- `validateModel` 返回的是实际执行模型，生成记录、任务记录、供应商调用和用量统计使用实际执行模型 ID 与 providerId。
- 参数模板和价格规则按展示模型配置，避免同一个用户端模型背后的多个渠道执行模型重复维护参数和用户定价。
- 执行模型只需要配置能力、供应商、模型标识、租户授权和状态；通常不需要单独配置用户侧价格规则。

### 10.2 价格计算流程

```text
查询模型价格规则
  ↓
根据 billingUnit 计算基础价格
  ↓
读取 priceConfig
  ↓
按 batchSize、duration、resolution 等参数加价
  ↓
返回 costPrice、salePrice、priceDetail
```

### 10.3 参数校验流程

```text
读取模型参数模板
  ↓
校验必填参数
  ↓
校验参数类型
  ↓
校验枚举值
  ↓
校验数字范围
  ↓
校验正则规则
  ↓
返回校验通过或错误信息
```

### 10.4 调用计量记录流程

```text
aigc-gen 或模型调用执行方完成第三方模型调用
  ↓
整理 taskId、userId、modelId、providerId、capability、usage、价格快照和调用结果
  ↓
调用 aigc-model.recordUsage(reqDTO)
  ↓
aigc-model 写入 aigc_model_usage_log
  ↓
返回 usageLogId，供后续审计、统计和结算链路使用
```

调用计量只记录模型调用结果，不替代 `aigc-billing` 的冻结、扣费和退款职责。

## 11. 缓存设计

模型配置属于读多写少，建议使用 Redis 或本地缓存。

当前代码已引入 `yudao-spring-boot-starter-redis`，但核心 Service 暂未实现显式缓存读写，第一阶段主要依赖数据库实时查询，后续可在模型详情、租户可用模型列表、参数模板和价格规则上补充缓存。

缓存 Key：


| Key                                        | 说明               |
| ------------------------------------------ | ------------------ |
| `aigc:model:{id}`                          | 模型详情           |
| `aigc:model:provider:{id}`                 | 渠道商详情         |
| `aigc:model:param:{modelId}:{capability}`  | 参数模板           |
| `aigc:model:price:{modelId}:{capability}`  | 价格规则           |
| `aigc:model:available:{type}:{capability}` | 用户端可用模型列表 |

缓存失效：

- 修改渠道商时清理 provider 和相关 model 缓存。
- 修改模型时清理 model 和 available 缓存。
- 修改参数模板时清理 param 缓存。
- 修改价格时清理 price 和 available 缓存。

第一阶段也可以先不做复杂缓存，只保留缓存接口位置，避免过早复杂化。

## 12. 安全设计

### 12.1 API Key 存储

渠道商 API Key、Secret Key 必须加密存储。

当前配置中已配置 MyBatis Plus 字段加密器密码，渠道商表预留 `api_key`、`secret_key` 字段，后续需要结合 DO 字段加密注解和管理端脱敏返回规则继续完善。

建议：

- 入库前加密。
- 内部调用时解密。
- 管理端详情不返回完整密钥。
- 只展示前后 4 位。
- 日志禁止打印密钥。

### 12.2 权限控制

管理端权限建议：


| 权限标识                     | 说明         |
| ---------------------------- | ------------ |
| `aigc:model:provider:query`  | 渠道商查询   |
| `aigc:model:provider:create` | 渠道商新增   |
| `aigc:model:provider:update` | 渠道商修改   |
| `aigc:model:provider:delete` | 渠道商删除   |
| `aigc:model:query`           | 模型查询     |
| `aigc:model:create`          | 模型新增     |
| `aigc:model:update`          | 模型修改     |
| `aigc:model:delete`          | 模型删除     |
| `aigc:model:price:update`    | 价格配置     |
| `aigc:model:param:update`    | 参数模板配置 |

当前 Controller 已使用 `@PreAuthorize` 做权限控制，实际权限标识按 Controller 路径拆分为 `aigc:model:*`、`aigc:model:provider:*`、`aigc:model:param:*`、`aigc:model:price:*`、`aigc:model:route:*`、`aigc:model:tenant:*` 等。

### 12.3 日志要求

- 不打印 API Key。
- 不打印 Secret Key。
- 价格计算可以记录模型 ID、能力、参数摘要、价格结果。
- 参数校验失败可以记录失败字段，但不要记录用户完整提示词。

## 13. 错误码设计

使用 AIGC 模块错误码段：

```text
1-041-000-000 ~ 1-041-099-999
```


| 错误码        | 常量                           | 说明               |
| ------------- | ------------------------------ | ------------------ |
| 1-041-000-000 | MODEL_PROVIDER_NOT_EXISTS      | 渠道商不存在       |
| 1-041-000-001 | MODEL_PROVIDER_DISABLED        | 渠道商已禁用       |
| 1-041-000-002 | MODEL_PROVIDER_CODE_DUPLICATE  | 渠道商编码重复     |
| 1-041-000-003 | MODEL_PROVIDER_HAS_MODEL       | 渠道商下存在模型   |
| 1-041-001-000 | MODEL_NOT_EXISTS               | 模型不存在         |
| 1-041-001-001 | MODEL_DISABLED                 | 模型已禁用         |
| 1-041-001-002 | MODEL_CODE_DUPLICATE           | 模型编码重复       |
| 1-041-001-003 | MODEL_CAPABILITY_NOT_SUPPORTED | 模型能力不支持     |
| 1-041-001-004 | MODEL_CAPABILITY_INVALID       | 模型能力配置不合法 |
| 1-041-001-005 | MODEL_NOT_AUTHORIZED           | 模型未授权         |
| 1-041-002-000 | MODEL_PARAM_NOT_EXISTS         | 模型参数不存在     |
| 1-041-002-001 | MODEL_PARAM_INVALID            | 模型参数不合法     |
| 1-041-002-002 | MODEL_PARAM_CODE_DUPLICATE     | 模型参数编码重复   |
| 1-041-002-003 | MODEL_PARAM_TEMPLATE_NOT_EXISTS | 模型参数模板不存在 |
| 1-041-002-004 | MODEL_PARAM_KEY_DUPLICATE      | 模型参数键重复     |
| 1-041-002-005 | MODEL_PARAM_REQUIRED           | 模型参数必填       |
| 1-041-002-006 | MODEL_PARAM_TYPE_ERROR         | 模型参数类型错误   |
| 1-041-002-007 | MODEL_PARAM_RANGE_ERROR        | 模型参数超出范围   |
| 1-041-002-008 | MODEL_PARAM_OPTION_ERROR       | 模型参数选项错误   |
| 1-041-002-009 | MODEL_PARAM_FORMAT_ERROR       | 模型参数格式错误   |
| 1-041-003-000 | MODEL_PRICE_NOT_EXISTS         | 模型价格未配置     |
| 1-041-003-001 | MODEL_PRICE_INVALID            | 模型价格配置不合法 |
| 1-041-003-002 | MODEL_PRICE_NOT_FOUND          | 模型价格不存在     |
| 1-041-003-003 | MODEL_PRICE_DUPLICATE          | 模型价格配置重复   |
| 1-041-004-000 | MODEL_ROUTE_NOT_EXISTS         | 模型路由不存在     |
| 1-041-005-000 | MODEL_TENANT_NOT_EXISTS        | 租户模型授权不存在 |

## 14. 第一阶段最小实现范围

第一阶段为了尽快支撑图片/视频生成赚钱闭环，原计划只实现以下能力，当前代码已在此基础上补充租户授权、路由配置和调用计量：

- 渠道商 CRUD
- 模型 CRUD
- 模型能力配置
- 模型参数模板配置
- 模型价格配置
- 可用模型列表
- 参数校验
- 价格计算
- 内部 RPC API
- 租户模型授权
- 基础路由配置
- 调用计量记录

仍暂不实现或仅保留配置基础：

- 自动健康检查
- 成功率统计路由
- 自动降级
- 账户池多 Key 轮询
- 模型灰度发布
- 复杂会员差异定价

## 15. 方案审核与优化点

对当前方案进一步审核后，建议补充以下优化点，避免第一阶段上线后出现配置混乱、价格错误、密钥泄露或服务间调用不可控的问题。

### 15.1 数据初始化

需要准备基础初始化数据，包括：

- 常用渠道商示例数据，默认禁用。
- 图片模型示例数据，默认禁用。
- 视频模型示例数据，默认禁用。
- 常用参数模板，例如比例、尺寸、时长、分辨率、批量数量。
- 常用计费单位字典。
- 管理后台菜单和权限标识。

初始化数据必须避免包含真实 API Key。

### 15.2 配置变更审计

模型服务是计费和生成链路的配置源，建议对关键配置变更做审计：

- 渠道商 API 地址变更。
- 渠道商密钥变更。
- 模型启停变更。
- 模型价格变更。
- 模型能力变更。
- 参数模板变更。
- 默认模型变更。

第一阶段可以先复用操作日志，后续再增加独立配置变更记录表。

### 15.3 价格快照

`calculatePrice` 返回的价格必须由 `aigc-task` 或 `aigc-billing` 记录为任务价格快照。

原因：

- 用户提交任务后，后台可能修改模型价格。
- 后续扣费、退款、成本统计必须以提交任务时的价格为准。
- 不能在任务成功时重新计算价格，否则会导致用户争议。

### 15.4 模型能力和模型类型一致性

保存模型能力时必须校验类型匹配：


| 模型类型 | 允许能力                                                            |
| -------- | ------------------------------------------------------------------- |
| IMAGE    | TEXT_TO_IMAGE、IMAGE_TO_IMAGE                                       |
| VIDEO    | TEXT_TO_VIDEO、IMAGE_TO_VIDEO、FIRST_LAST_FRAME_VIDEO、VIDEO_EXTEND |
| AUDIO    | TTS、VOICE_CLONE、BGM_GENERATE、SOUND_EFFECT_GENERATE               |
| TEXT     | TEXT_GENERATE、PROMPT_OPTIMIZE、SCRIPT_GENERATE                     |

第一阶段只开放 IMAGE 和 VIDEO。

### 15.5 默认模型唯一性

同一个租户下，同一模型类型、同一能力只能有一个默认模型。

建议唯一约束或业务校验：

```text
tenant_id + type + capability + default_model
```

如果数据库层不好做条件唯一索引，则在 Service 层事务中保证：设置新默认模型前，先取消同类型同能力的旧默认模型。

### 15.6 密钥安全

需要补充密钥处理规则：

- 数据库存储加密值。
- 管理端列表不返回密钥。
- 管理端详情只返回脱敏值。
- 修改时如果密钥为空，保持原值不变。
- 日志禁止输出密钥。
- RPC `getProvider` 只允许内部服务调用。

### 15.7 删除策略

模型和渠道商不建议物理删除正在被使用的配置。

删除前需要校验：

- 是否有关联模型。
- 是否有关联价格配置。
- 是否有关联参数模板。
- 是否存在未完成任务正在使用该模型。

第一阶段可采用逻辑删除，但更推荐运营上使用禁用，减少历史任务展示异常。

### 15.8 可观测性指标

模型服务需要输出或至少保留统计口径：

- 渠道商数量。
- 启用模型数量。
- 禁用模型数量。
- 各能力模型数量。
- 价格配置缺失数量。
- 参数模板缺失数量。
- 模型校验失败次数。
- 价格计算失败次数。

第一阶段可以先在管理后台统计，后续接入监控指标。

### 15.9 多租户审核结论

再次审核后，多租户是该模块必须重点设计的能力。现有方案虽然在索引、测试中提到了 `tenant_id`，但还不够，需要明确租户级模型配置策略。

核心结论：

- AIGC 平台的模型服务不能只做全局配置，必须支持租户隔离。
- 平台可以提供全局共享模型，但租户需要有自己的启停、价格、默认模型和额度策略。
- 渠道商密钥可以是平台统一密钥，也可以是租户自有密钥。
- 用户端查询模型时必须只看到当前租户可用的模型。
- `aigc-gen`、`aigc-task`、`aigc-billing` 调用模型服务时必须携带或透传租户上下文。
- 价格计算必须优先使用租户级价格，没有租户级价格时再使用平台默认价格。
- 默认模型必须是租户维度的默认模型，不能全平台共用一个默认模型。

## 16. 多租户设计

### 16.1 租户模式

模型服务建议采用“平台模型 + 租户授权/覆盖”的模式。

```text
平台维护基础渠道商和模型
  ↓
租户选择启用哪些模型
  ↓
租户可覆盖价格、默认模型、展示状态、参数限制
  ↓
用户端只看到当前租户可用模型
```

这种模式比每个租户完全复制一套模型配置更容易运营，也比全平台共享一套配置更安全。

### 16.1.1 复用项目现有租户能力

本模块必须复用项目已有租户体系，而不是重新实现一套 tenant 模块。

当前项目已有能力：


| 能力           | 现有位置                               | AIGC 模型服务使用方式                    |
| -------------- | -------------------------------------- | ---------------------------------------- |
| 租户上下文     | `TenantContextHolder`                  | 获取当前请求租户 ID                      |
| Web 租户解析   | `TenantContextWebFilter`               | 从请求 Header 解析租户并写入上下文       |
| 访问租户切换   | `TenantVisitContextInterceptor`        | 平台管理员访问指定租户时复用             |
| 租户 SQL 隔离  | `yudao-spring-boot-starter-biz-tenant` | DO 表自动按`tenant_id` 隔离              |
| 忽略租户注解   | `@TenantIgnore`                        | 平台级配置、租户初始化、跨租户授权时使用 |
| 租户工具类     | `TenantUtils`                          | 按指定租户执行初始化和授权逻辑           |
| 租户合法性校验 | `TenantCommonApi#validTenant`          | 创建租户授权前校验租户存在且有效         |
| 租户列表       | `TenantCommonApi#getTenantIdList`      | 批量初始化或补偿租户授权                 |

模块依赖上必须引入：

```xml
<dependency>
    <groupId>cn.iocoder.cloud</groupId>
    <artifactId>yudao-spring-boot-starter-biz-tenant</artifactId>
</dependency>
<dependency>
    <groupId>cn.iocoder.cloud</groupId>
    <artifactId>yudao-module-system-api</artifactId>
    <version>${revision}</version>
</dependency>
```

实现要求：

- 普通用户端接口通过 `TenantContextHolder.getRequiredTenantId()` 获取当前租户。
- 普通管理端接口默认走当前租户上下文。
- 平台管理员做平台模型、平台渠道商、跨租户授权时使用 `@TenantIgnore` 或 `TenantUtils.execute(...)`。
- 不新增 `tenant` 表，不复制 `system_tenant` 数据。
- 不在 AIGC 模块里维护租户生命周期，只监听或调用现有租户能力完成授权初始化。

### 16.1.2 平台级数据与租户插件的关系

当前项目租户插件会自动给普通表追加 `tenant_id` 条件，因此平台级 `tenant_id = 0` 数据需要特别处理。

建议规则：

- 平台级模型、渠道商、价格、参数模板由平台管理员维护。
- 平台级查询和维护接口使用 `@TenantIgnore`，显式查询 `tenant_id = 0` 的数据。
- 租户侧用户接口不要直接查询平台模型表，而是通过 `aigc_model_tenant` 授权表查到已授权模型。
- 如果 Service 需要同时查询平台模型和租户授权，必须显式封装在专门方法中，避免普通 Mapper 查询被租户插件误过滤。

推荐封装方法：

```text
getPlatformModel(modelId)
getTenantModelGrant(tenantId, modelId)
listTenantAvailableModels(tenantId, type, capability)
calculateTenantPrice(tenantId, modelId, capability, params)
```

这些方法内部清晰区分：

```text
平台表查询：@TenantIgnore + tenant_id = 0
租户授权查询：正常租户上下文或显式 tenant_id
```

### 16.2 数据分层


| 数据         | 建议归属   | 是否带 tenant_id | 说明                         |
| ------------ | ---------- | ---------------- | ---------------------------- |
| 平台渠道商   | 平台级     | tenant_id = 0    | 平台统一维护的渠道商         |
| 租户渠道商   | 租户级     | 当前租户 ID      | 租户自有 API Key             |
| 平台模型     | 平台级     | tenant_id = 0    | 平台统一模型配置             |
| 租户模型授权 | 租户级     | 当前租户 ID      | 租户可用模型、展示、默认配置 |
| 平台价格     | 平台级     | tenant_id = 0    | 默认售价                     |
| 租户价格     | 租户级     | 当前租户 ID      | 租户差异化售价               |
| 参数模板     | 平台级为主 | tenant_id = 0    | 默认参数模板                 |
| 租户参数覆盖 | 租户级可选 | 当前租户 ID      | 限制租户可选参数范围         |

### 16.3 表结构优化

为了支持多租户运营，建议在原有表基础上增加两张租户配置表。

#### 16.3.1 aigc_model_tenant

租户模型授权表，用于控制某个租户可使用哪些模型，以及租户维度的展示、默认、限流策略。


| 字段           | 类型         | 说明               |
| -------------- | ------------ | ------------------ |
| id             | bigint       | 主键               |
| tenant_id      | bigint       | 租户 ID            |
| model_id       | bigint       | 平台模型 ID        |
| enabled        | bit(1)       | 租户是否启用该模型 |
| public_visible | bit(1)       | 用户端是否展示     |
| default_model  | bit(1)       | 是否租户默认模型   |
| sort           | int          | 租户内排序         |
| max_concurrent | int          | 租户模型并发限制   |
| daily_limit    | int          | 租户日调用限制     |
| remark         | varchar(512) | 备注               |

唯一索引：

```text
uk_tenant_model = tenant_id + model_id
```

业务约束：

```text
同一 tenant_id + type + capability 下只能有一个 default_model = true
```

#### 16.3.2 aigc_model_tenant_param

租户参数覆盖表，用于限制租户可用参数范围。例如平台模型支持 1080p，但某些租户只允许 720p。


| 字段            | 类型        | 说明     |
| --------------- | ----------- | -------- |
| id              | bigint      | 主键     |
| tenant_id       | bigint      | 租户 ID  |
| model_id        | bigint      | 模型 ID  |
| capability      | varchar(64) | 能力     |
| param_key       | varchar(64) | 参数键   |
| override_config | json        | 覆盖配置 |
| status          | int         | 状态     |

唯一索引：

```text
uk_tenant_model_capability_param = tenant_id + model_id + capability + param_key
```

### 16.4 现有表 tenant_id 策略

#### aigc_model_provider

渠道商支持两种归属：


| tenant_id   | 含义           |
| ----------- | -------------- |
| 0           | 平台统一渠道商 |
| 当前租户 ID | 租户自有渠道商 |

使用规则：

- 平台模型一般关联平台渠道商。
- 租户自有模型可以关联租户自己的渠道商。
- 租户不能读取其他租户的渠道商。
- 管理端超级管理员可以管理平台渠道商。
- 租户管理员只能管理本租户渠道商。

#### aigc_model

模型支持两种归属：


| tenant_id   | 含义                       |
| ----------- | -------------------------- |
| 0           | 平台模型，可授权给租户     |
| 当前租户 ID | 租户自建模型，仅本租户可用 |

推荐第一阶段优先做平台模型 + 租户授权，不急着开放租户自建模型。

#### aigc_model_price

价格支持优先级：

```text
租户级价格 tenant_id = 当前租户
  ↓ 如果不存在
平台默认价格 tenant_id = 0
```

价格计算必须返回实际命中的价格规则来源：

```text
priceSource = TENANT / PLATFORM
```

#### aigc_model_param_template

参数模板支持优先级：

```text
平台默认参数模板
  ↓
租户参数覆盖限制
```

例如：平台支持 `720p、1080p`，租户覆盖后只允许 `720p`。

### 16.5 租户级模型查询规则

用户端获取可用模型时，查询逻辑必须是：

```text
获取当前 tenantId
  ↓
查询 aigc_model_tenant 中 enabled = true 的模型
  ↓
关联 aigc_model，要求模型 status = ENABLE
  ↓
关联 aigc_model_provider，要求渠道商 status = ENABLE
  ↓
过滤 public_visible = true
  ↓
按租户 sort 排序
  ↓
返回用户端安全字段
```

不能直接查询全局 `aigc_model.public_visible = true`。

### 16.6 租户级模型校验规则

`validateModel(modelId, capability)` 必须增加租户校验：

```text
获取当前 tenantId
  ↓
校验展示模型存在且启用
  ↓
校验展示模型渠道商存在且启用
  ↓
校验展示模型支持 capability
  ↓
校验当前租户已授权并启用展示模型
  ↓
按展示模型 code + capability 查询路由规则
  ↓
如果命中路由，选择真实执行模型并重复模型、渠道、能力、租户授权校验
  ↓
返回实际执行模型信息
```

如果是内部平台任务，可以显式传 `tenantId = 0` 或使用系统租户上下文。

### 16.7 路由验证 SQL

提交生成后，可通过生成记录和任务记录确认路由是否生效：

```sql
SELECT
  r.task_id,
  r.model_id,
  m.name AS model_name,
  m.model AS provider_model,
  r.provider_id,
  p.name AS provider_name,
  p.code AS provider_code,
  r.provider_code AS record_provider_code,
  r.status,
  r.provider_task_id
FROM gen_db.aigc_gen_record r
LEFT JOIN model_db.aigc_model m ON m.id = r.model_id
LEFT JOIN model_db.aigc_model_provider p ON p.id = r.provider_id
WHERE r.task_id IN (80, 81)
ORDER BY r.task_id;
```

判断标准：

- 如果生成请求传入展示模型 ID，但 `aigc_gen_record.model_id` 变成候选执行模型 ID，说明路由生效。
- 如果 `provider_id` 在不同任务间切换，说明轮询或其他策略已切换到不同渠道。
- 如果 `model_id` 仍是展示模型 ID，说明路由未命中、未启用、候选为空，或服务未部署到包含路由逻辑的版本。

### 16.8 租户级默认模型

默认模型必须放在租户维度，不建议直接使用 `aigc_model.default_model` 作为最终默认模型。

规则：

- 平台模型可以有平台默认值，用于给新租户初始化。
- 租户真正使用的默认模型来自 `aigc_model_tenant.default_model`。
- 新租户开通 AIGC 服务时，根据平台默认配置生成租户授权记录。
- 租户管理员可以调整自己的默认模型。

### 16.9 租户级价格计算

`calculatePrice` 必须按以下顺序执行：

```text
获取 tenantId
  ↓
校验租户已授权模型
  ↓
查询租户级价格
  ↓ 如果没有
查询平台默认价格
  ↓
根据参数计算价格
  ↓
返回 priceSource、priceRuleId、costPrice、salePrice、priceDetail
```

注意：

- 成本价一般来自平台价格或渠道配置。
- 租户价格主要覆盖销售价。
- 如果允许代理商租户自定义售价，需要记录租户售价来源。

### 16.10 租户级密钥策略

渠道商密钥分两种：


| 类型     | 说明                 | 适用场景           |
| -------- | -------------------- | ------------------ |
| 平台密钥 | 平台统一采购模型能力 | C 端用户、普通租户 |
| 租户密钥 | 租户自己提供 API Key | 企业客户、私有渠道 |

第一阶段建议：

- 默认只开放平台密钥。
- 租户密钥能力保留表结构和权限，不在 C 端开放。
- 企业租户后续可开启自有渠道配置。

### 16.11 租户开通流程

新租户开通 AIGC 服务时，需要初始化：

```text
读取平台启用模型
  ↓
读取平台默认授权策略
  ↓
生成 aigc_model_tenant 授权记录
  ↓
设置租户默认图片模型
  ↓
设置租户默认视频模型
  ↓
初始化租户价格覆盖，可选
```

第一阶段可以提供管理端按钮：

```text
为租户初始化 AIGC 模型授权
```

### 16.12 租户权限边界

平台管理员：

- 管理平台渠道商。
- 管理平台模型。
- 管理平台价格。
- 给租户授权模型。
- 查看所有租户模型配置。

租户管理员：

- 查看本租户可用模型。
- 调整本租户模型展示状态。
- 调整本租户默认模型。
- 查看本租户价格。
- 如开启企业能力，可管理本租户自有渠道商。

普通用户：

- 只查看当前租户可用且公开展示的模型。
- 只能计算当前租户授权模型的价格。

### 16.13 多租户测试补充

必须补充以下测试：


| 编号  | 用例                         | 预期                        |
| ----- | ---------------------------- | --------------------------- |
| T-001 | 租户 A 查询可用模型          | 只返回租户 A 授权模型       |
| T-002 | 租户 B 查询可用模型          | 不返回租户 A 专属模型       |
| T-003 | 租户 A 使用未授权模型        | validateModel 失败          |
| T-004 | 租户 A 禁用模型后提交任务    | validateModel 失败          |
| T-005 | 租户 A 有租户价格            | calculatePrice 使用租户价格 |
| T-006 | 租户 A 无租户价格            | calculatePrice 使用平台价格 |
| T-007 | 租户 A 默认模型设置          | 不影响租户 B 默认模型       |
| T-008 | 租户 A 参数覆盖 720p         | 1080p 参数校验失败          |
| T-009 | 租户 A 无法查看租户 B 渠道商 | 查询为空或无权限            |
| T-010 | 平台管理员查看租户授权       | 可查看所有租户授权          |

### 16.14 多租户质量门禁

上线前必须满足：

- 所有业务表包含 `tenant_id` 或明确声明为平台级表。
- 用户端接口全部基于当前租户过滤。
- RPC API 支持租户上下文透传。
- 价格计算支持租户价格覆盖。
- 默认模型支持租户维度。
- 多租户数据隔离测试通过。
- API Key 不跨租户泄露。

## 17. 测试方案

### 17.0 当前测试现状

当前 `yudao-module-aigc-model-server` 已有 5 个测试类，Surefire 报告显示共 20 个用例，`errors=0`、`failures=0`、`skipped=0`。

| 测试类 | 当前覆盖重点 |
| ------ | ------------ |
| `AigcModelApiImplTest` | `validateModel`、`validateParams`、`calculatePrice` RPC 行为 |
| `AigcModelAppControllerTest` | 用户端模型详情、价格计算接口 |
| `AigcModelServiceImplTest` | 租户可见模型成功、不可见、渠道商禁用场景 |
| `AigcModelParamServiceImplTest` | 参数重复、必填缺失、数值范围、下拉选项、字符串格式、成功校验 |
| `AigcModelPriceServiceImplTest` | 批量倍率、时长/分辨率倍率、价格不存在、禁用/过期价格、租户价格优先、重复价格创建 |

测试资源现状：

- `src/test/resources/application-unit-test.yaml` 提供单元测试配置。
- `src/test/resources/sql/create_tables.sql` 提供 H2/兼容测试建表。
- `src/test/resources/sql/clean.sql` 提供测试数据清理。
- 生产 SQL 已包含 `aigc_model_usage_log`，测试建表脚本当前主要覆盖核心配置表，调用计量日志表的测试覆盖仍需补充。

后续建议优先补充管理端 Controller、租户授权服务、路由服务、渠道商服务、调用计量服务和密钥脱敏相关测试。

### 17.1 测试目标

测试目标是保证模型服务作为 AIGC 平台配置中台稳定可靠，重点验证：

- 渠道商配置正确。
- 模型配置正确。
- 模型能力校验正确。
- 参数模板校验正确。
- 价格计算正确。
- API Key 不泄露。
- 管理端权限生效。
- 内部 RPC 能稳定支撑生成、任务和计费服务。

### 17.2 测试分层


| 测试类型        | 目标                                | 工具/方式                 |
| --------------- | ----------------------------------- | ------------------------- |
| 单元测试        | 测 Service 规则、价格计算、参数校验 | JUnit、Mockito            |
| Mapper 测试     | 测分页、唯一约束、条件查询          | 项目现有测试框架          |
| Controller 测试 | 测接口参数、权限、返回脱敏          | MockMvc                   |
| RPC 测试        | 测`AigcModelApi` 对外行为           | Spring Boot Test          |
| 集成测试        | 测完整模型配置到价格计算流程        | Spring Boot Test + 测试库 |
| 安全测试        | 测密钥不返回、不打印                | 接口断言、日志检查        |
| 回归测试        | 改价格、改模板后旧能力不受影响      | 自动化用例                |

### 17.3 单元测试范围

#### AigcModelServiceTest

必须覆盖：

- 创建模型成功。
- 模型编码重复时失败。
- 渠道商不存在时创建模型失败。
- 禁用模型不能通过 `validateModel`。
- 禁用渠道商不能通过 `validateModel`。
- 模型不支持指定能力时失败。
- 设置默认模型时取消旧默认模型。
- 用户端列表只返回启用且公开的模型。

#### AigcModelProviderServiceTest

必须覆盖：

- 创建渠道商成功。
- 渠道商编码重复时失败。
- 修改 API Key 为空时保留原密钥。
- 返回管理端详情时密钥脱敏。
- 禁用渠道商后模型校验失败。
- 删除有关联模型的渠道商失败。

#### AigcModelParamTemplateServiceTest

必须覆盖：

- 必填参数缺失时失败。
- 数字参数超出最小值时失败。
- 数字参数超出最大值时失败。
- SELECT 参数不在可选值中时失败。
- BOOLEAN 参数类型错误时失败。
- 正则参数不匹配时失败。
- 未配置参数模板时按策略失败或跳过。

建议第一阶段策略：核心参数未配置模板时失败，避免错误请求进入模型调用链路。

#### AigcModelPriceServiceTest

必须覆盖：

- 按任务计费计算正确。
- 按张计费计算正确。
- 按批量数量计费计算正确。
- 按视频秒数计费计算正确。
- 按每 5 秒计费向上取整正确。
- 1080p 额外加价正确。
- 价格规则未配置时失败。
- 价格规则禁用时失败。
- 返回成本价、销售价和价格明细。

### 17.4 Controller 测试范围

管理端接口：

- 未登录访问失败。
- 无权限访问失败。
- 新增渠道商参数校验。
- 新增模型参数校验。
- 模型分页查询正常。
- 渠道商详情密钥脱敏。
- 价格模拟计算正常。

用户端接口：

- 可用模型列表不返回禁用模型。
- 可用模型列表不返回隐藏模型。
- 参数模板接口不返回内部字段。
- 价格计算接口不返回成本价。
- 用户端接口不返回 API Key、Secret Key、渠道商内部配置。

### 17.5 RPC API 测试范围

`AigcModelApi` 必须覆盖：

- `validateModel` 正常返回模型信息。
- `validateModel` 拦截禁用模型。
- `validateModel` 拦截禁用渠道商。
- `validateModel` 拦截能力不匹配。
- `getProvider` 返回内部调用所需渠道信息。
- `getProvider` 仅内部调用，不暴露到前端接口。
- `calculatePrice` 返回价格快照所需字段。
- `validateParams` 对非法参数抛出明确错误。

### 17.6 数据库测试范围

必须验证：

- `aigc_model_provider.code + tenant_id` 唯一。
- `aigc_model.code + tenant_id` 唯一。
- `aigc_model_capability.model_id + capability` 唯一。
- `aigc_model_param_template.model_id + capability + param_key` 唯一。
- 逻辑删除后分页不展示。
- 多租户数据隔离生效。

### 17.7 安全测试范围

必须验证：

- API Key 入库不是明文。
- 管理端列表不返回 API Key。
- 管理端详情只返回脱敏 API Key。
- 用户端任何接口不返回 API Key。
- 日志中不出现 API Key 原文。
- 价格计算不返回成本价给用户端。

### 17.8 边界测试用例


| 场景               | 期望                 |
| ------------------ | -------------------- |
| 模型不存在         | 返回模型不存在错误   |
| 渠道商不存在       | 返回渠道商不存在错误 |
| 模型禁用           | 返回模型已禁用错误   |
| 渠道商禁用         | 返回渠道商已禁用错误 |
| 能力不匹配         | 返回能力不支持错误   |
| 参数缺失           | 返回参数不合法错误   |
| 价格缺失           | 返回价格未配置错误   |
| 视频时长为 0       | 返回参数不合法错误   |
| batchSize 超出上限 | 返回参数不合法错误   |
| 价格配置为负数     | 保存失败             |
| 设置多个默认模型   | 最终只有一个默认模型 |

### 17.9 测试数据准备

建议准备固定测试数据：

```text
provider_kling_enabled
provider_kling_disabled
model_image_enabled_public
model_image_disabled
model_video_enabled_public
model_video_hidden
price_image_per_image
price_video_per_5_seconds
param_ratio_select
param_duration_number
param_resolution_select
```

测试数据必须使用假密钥，例如：

```text
test-api-key-please-replace
```

### 17.10 质量门禁

提交前必须满足：

```text
mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am test
mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am -DskipTests compile
```

最低质量要求：

- 核心 Service 单元测试覆盖。
- 价格计算测试覆盖全部计费单位。
- 参数校验测试覆盖全部参数类型。
- 用户端接口安全字段断言必须覆盖。
- 编译无错误。
- 不引入旧 `yudao-module-ai` 依赖。

## 18. 测试用例清单

### 18.1 渠道商测试用例


| 编号  | 用例                     | 预期           |
| ----- | ------------------------ | -------------- |
| P-001 | 新增 API_KEY 渠道商      | 成功           |
| P-002 | 新增重复 code 渠道商     | 失败           |
| P-003 | 禁用渠道商               | 成功           |
| P-004 | 禁用渠道商后校验模型     | 失败           |
| P-005 | 查询渠道商详情           | API Key 脱敏   |
| P-006 | 修改渠道商但不传 API Key | 保留原 API Key |
| P-007 | 删除有关联模型的渠道商   | 失败           |

### 18.2 模型测试用例


| 编号  | 用例               | 预期                   |
| ----- | ------------------ | ---------------------- |
| M-001 | 新增图片模型       | 成功                   |
| M-002 | 新增视频模型       | 成功                   |
| M-003 | 新增重复 code 模型 | 失败                   |
| M-004 | 禁用模型           | 成功                   |
| M-005 | 校验禁用模型       | 失败                   |
| M-006 | 用户端查询隐藏模型 | 不返回                 |
| M-007 | 设置默认模型       | 同类型同能力旧默认取消 |

### 18.3 能力测试用例


| 编号  | 用例                        | 预期 |
| ----- | --------------------------- | ---- |
| C-001 | 图片模型配置 TEXT_TO_IMAGE  | 成功 |
| C-002 | 图片模型配置 TEXT_TO_VIDEO  | 失败 |
| C-003 | 视频模型配置 IMAGE_TO_VIDEO | 成功 |
| C-004 | 重复配置同一能力            | 失败 |

### 18.4 参数模板测试用例


| 编号   | 用例                  | 预期 |
| ------ | --------------------- | ---- |
| PT-001 | SELECT 参数值合法     | 成功 |
| PT-002 | SELECT 参数值非法     | 失败 |
| PT-003 | NUMBER 参数低于最小值 | 失败 |
| PT-004 | NUMBER 参数高于最大值 | 失败 |
| PT-005 | 必填参数缺失          | 失败 |
| PT-006 | BOOLEAN 参数传字符串  | 失败 |

### 18.5 价格测试用例


| 编号   | 用例                        | 预期              |
| ------ | --------------------------- | ----------------- |
| PR-001 | 图片按张计费，batchSize=1   | 返回 1 倍价格     |
| PR-002 | 图片按张计费，batchSize=4   | 返回 4 倍价格     |
| PR-003 | 视频按 5 秒计费，duration=5 | 返回 1 倍价格     |
| PR-004 | 视频按 5 秒计费，duration=6 | 返回 2 倍价格     |
| PR-005 | 视频 1080p 加价             | 返回基础价 + 加价 |
| PR-006 | 未配置价格                  | 失败              |
| PR-007 | 价格禁用                    | 失败              |

### 18.6 用户端安全测试用例


| 编号  | 用例             | 预期                 |
| ----- | ---------------- | -------------------- |
| S-001 | 用户端模型列表   | 不包含 API Key       |
| S-002 | 用户端参数模板   | 不包含成本价         |
| S-003 | 用户端价格计算   | 只返回销售价         |
| S-004 | 管理端渠道商列表 | API Key 脱敏或不返回 |

## 19. 开发顺序

当前模块主体代码已完成，以下开发顺序作为历史实施路径和后续补齐参考。已完成项包括 Maven 模块、启动类、API DTO/枚举/RPC、DO/Mapper/SQL、渠道商、模型、参数模板、价格、路由、租户授权、调用计量、用户端接口和部分自动化测试。

```text
1. 创建 Maven 模块和启动类
2. 创建 api 模块 DTO、枚举、AigcModelApi
3. 创建 server 模块 DO、Mapper、SQL
4. 实现渠道商管理
5. 实现模型管理
6. 实现模型能力管理
7. 实现参数模板管理
8. 实现价格配置和价格计算
9. 实现内部 RPC API
10. 实现用户端可用模型接口
11. 实现租户模型授权
12. 实现模型调用计量 recordUsage
13. 实现权限标识和菜单 SQL
14. 补充 Service 单元测试
15. 补充 Controller 测试
16. 补充 RPC API 测试
17. 补充计量、租户、路由、密钥脱敏测试
18. 编译、测试和接口验收
```

## 20. 与其他服务协作

### 20.1 aigc-gen 调用

`aigc-gen` 在提交生成任务前必须调用：

```text
validateModel
validateParams
calculatePrice
getProvider
recordUsage
```

### 20.2 aigc-billing 调用

`aigc-billing` 可以调用：

```text
calculatePrice
getModel
```

用于记录计费展示、成本价和销售价。

### 20.3 aigc-task 调用

`aigc-task` 可以调用：

```text
getModel
recordUsage
```

用于任务详情展示模型名称、模型类型、渠道商信息，以及在任务执行完成后记录模型调用计量。

## 21. 验收标准

### 21.1 管理端验收

- 可以新增渠道商。
- 可以修改渠道商。
- 可以启用和禁用渠道商。
- 可以新增图片模型。
- 可以新增视频模型。
- 可以给模型配置多个能力。
- 可以配置模型参数模板。
- 可以配置模型价格。
- 可以启用和禁用模型。
- 可以控制模型是否在用户端展示。

### 21.2 用户端验收

- 用户只能看到启用且公开的模型。
- 用户可以获取模型参数模板。
- 用户可以计算生成预计消耗。
- 用户无法看到渠道商密钥、成本价和内部配置。

### 21.3 服务间调用验收

- `validateModel` 能正确拦截禁用模型。
- `validateModel` 能正确拦截禁用渠道商。
- `validateParams` 能正确校验必填、类型、枚举和范围。
- `calculatePrice` 能根据图片数量、视频时长、分辨率正确计算价格。
- `getProvider` 内部调用能拿到模型调用需要的渠道配置。

### 21.4 测试验收

- Service 单元测试通过。
- Controller 接口测试通过。
- RPC API 测试通过。
- 参数校验测试覆盖 STRING、NUMBER、BOOLEAN、SELECT。
- 价格计算测试覆盖 PER_TASK、PER_IMAGE、PER_SECOND、PER_5_SECONDS。
- 密钥脱敏测试通过。
- 多租户隔离测试通过。
- 编译命令通过。
- 测试命令通过。

## 22. 最终建议

`yudao-module-aigc-model` 要保持“模型中台”的纯粹性，只管理模型、渠道、参数、价格和路由基础配置。

不要把生成任务、文件资产、钱包计费、审核逻辑塞进这个模块。

第一阶段只要它能稳定提供：

```text
模型是否可用
模型支持什么能力
模型需要哪些参数
模型本次调用多少钱
模型属于哪个渠道商
```

就足够支撑 `aigc-gen`、`aigc-task`、`aigc-billing` 跑通第一条赚钱链路。
