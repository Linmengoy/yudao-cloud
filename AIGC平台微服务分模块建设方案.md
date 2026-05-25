# AIGC 平台微服务分模块建设方案

## 1. 方案定位

本方案基于 AIGC 平台 MVP 赚钱版目标，将系统从“单一独立模块”调整为“微服务分模块建设”。

核心目标：

```text
先上线赚钱，后持续扩展
```

第一阶段优先建设：

- 图片生成
- 视频生成
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
yudao-module-aigc-gen        生成服务，图片/视频生成
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

| 模块 | 是否第一阶段建设 | 说明 |
| --- | --- | --- |
| aigc-model | 是 | 渠道商、模型、参数模板、价格配置 |
| aigc-billing | 是 | 钱包、积分冻结、扣费、退款、成本 |
| aigc-task | 是 | 统一任务、状态机、回调、日志 |
| aigc-asset | 是 | 图片、视频资产管理 |
| aigc-gen | 是 | 图片生成、视频生成、模型调用适配 |
| aigc-safety | 是 | 敏感词、基础审核 |
| aigc-workflow | 否 | 第二阶段建设 |
| aigc-template | 否 | 社区或模板市场阶段建设 |
| aigc-community | 否 | 第三阶段建设 |
| aigc-publish | 否 | 成片导出阶段建设 |

## 3. 微服务命名规范

### 3.1 Maven 模块命名

每个微服务都采用 `api + server` 结构。

示例：

```text
yudao-module-aigc-model
  ├── yudao-module-aigc-model-api
  └── yudao-module-aigc-model-server
```

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

### 3.2 包名规范

| 微服务 | 根包名 |
| --- | --- |
| aigc-model | `cn.iocoder.yudao.module.aigc.model` |
| aigc-billing | `cn.iocoder.yudao.module.aigc.billing` |
| aigc-task | `cn.iocoder.yudao.module.aigc.task` |
| aigc-asset | `cn.iocoder.yudao.module.aigc.asset` |
| aigc-gen | `cn.iocoder.yudao.module.aigc.gen` |
| aigc-safety | `cn.iocoder.yudao.module.aigc.safety` |

### 3.3 URL 前缀规范

| 微服务 | 管理端 URL | 用户端 URL |
| --- | --- | --- |
| aigc-model | `/aigc/model` | `/app-api/aigc/model` |
| aigc-billing | `/aigc/billing` | `/app-api/aigc/wallet` |
| aigc-task | `/aigc/task` | `/app-api/aigc/task` |
| aigc-asset | `/aigc/asset` | `/app-api/aigc/asset` |
| aigc-gen | `/aigc/gen` | `/app-api/aigc/gen` |
| aigc-safety | `/aigc/safety` | 暂不开放或仅内部调用 |

### 3.4 表名前缀规范

所有 AIGC 微服务表统一使用 `aigc_` 前缀。

| 微服务 | 表名前缀 |
| --- | --- |
| aigc-model | `aigc_model_` |
| aigc-billing | `aigc_wallet`、`aigc_billing_`、`aigc_quota_` |
| aigc-task | `aigc_task_` |
| aigc-asset | `aigc_asset_` |
| aigc-gen | `aigc_image`、`aigc_video` |
| aigc-safety | `aigc_audit_`、`aigc_sensitive_` |

## 4. 服务职责边界

## 4.1 模型与渠道服务：aigc-model

### 4.1.1 服务定位

负责所有模型供应商、模型配置、模型能力、参数模板、价格配置。

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

### 4.1.3 核心表

| 表名 | 说明 |
| --- | --- |
| aigc_model_provider | 模型渠道商 |
| aigc_model | 模型配置 |
| aigc_model_capability | 模型能力，可选拆表 |
| aigc_model_param_template | 模型参数模板 |
| aigc_model_price | 模型价格配置，可选拆表 |

### 4.1.4 API 暴露

`aigc-model-api` 对其他服务暴露 RPC API：

```text
AigcModelApi
  ├── validateModel(modelId, capability)
  ├── getModel(modelId)
  ├── getProvider(providerId)
  ├── getParamTemplates(modelId)
  ├── validateParams(modelId, params)
  ├── calculatePrice(modelId, taskType, params)
  └── listAvailableModels(type, capability)
```

### 4.1.5 依赖关系

依赖：

- system-api：用户或管理员信息，非强依赖

被依赖：

- aigc-gen
- aigc-task
- aigc-billing

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

| 表名 | 说明 |
| --- | --- |
| aigc_wallet | 用户钱包 |
| aigc_quota_freeze | 积分冻结记录 |
| aigc_billing_record | 计费流水 |
| aigc_cost_record | 成本记录 |
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

统一管理图片生成、视频生成、审核等异步任务。

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

### 4.3.3 核心表

| 表名 | 说明 |
| --- | --- |
| aigc_task | 任务主表 |
| aigc_task_log | 任务日志 |
| aigc_task_callback | 任务回调记录 |
| aigc_task_retry | 任务重试记录，可选 |

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
  ├── markSuccess(taskId, outputAssetId)
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

统一管理用户生成和上传的图片、视频等资产。

第一阶段只管理图片和视频，后续扩展音频、字幕、模板素材。

### 4.4.2 核心能力

- 图片资产入库
- 视频资产入库
- 缩略图/封面管理
- 资产列表
- 资产详情
- 下载计数
- 删除资产
- 审核状态
- 资产可见性

### 4.4.3 核心表

| 表名 | 说明 |
| --- | --- |
| aigc_asset | 资产主表 |
| aigc_asset_relation | 资产关系，第二阶段建设 |
| aigc_asset_version | 资产版本，第二阶段建设 |

### 4.4.4 API 暴露

`aigc-asset-api` 暴露：

```text
AigcAssetApi
  ├── createImageAsset(userId, taskId, fileUrl, metadata)
  ├── createVideoAsset(userId, taskId, fileUrl, coverUrl, metadata)
  ├── getAsset(assetId)
  ├── getUserAssets(userId, type)
  ├── increaseDownloadCount(assetId)
  └── updateAuditStatus(assetId, auditStatus)
```

### 4.4.5 文件存储

`aigc-asset` 依赖 `infra-api` 的 `FileApi`。

外部模型返回的图片、视频文件必须下载后上传到平台文件服务，避免第三方 URL 过期。

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

负责图片生成、视频生成以及第三方模型调用适配。

这是第一阶段用户直接感知最强的服务。

### 4.5.2 核心能力

- 文生图
- 图生图
- 文生视频
- 图生视频
- 模型调用客户端
- 第三方任务提交
- 第三方任务查询
- 第三方回调处理
- 结果文件下载
- 调用资产服务入库
- 调用任务服务推进状态

### 4.5.3 核心表

| 表名 | 说明 |
| --- | --- |
| aigc_image | 图片生成记录 |
| aigc_video | 视频生成记录 |

### 4.5.4 API 暴露

`aigc-gen-api` 暴露给内部使用：

```text
AigcImageGenerateApi
  ├── textToImage(userId, req)
  ├── imageToImage(userId, req)
  └── getImage(id)

AigcVideoGenerateApi
  ├── textToVideo(userId, req)
  ├── imageToVideo(userId, req)
  ├── handleCallback(providerCode, callbackData)
  └── syncVideoTask(taskId)
```

### 4.5.5 用户接口

用户端可以直接请求 `aigc-gen`：

```text
/app-api/aigc/gen/image/text-to-image
/app-api/aigc/gen/image/image-to-image
/app-api/aigc/gen/video/text-to-video
/app-api/aigc/gen/video/image-to-video
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

| 表名 | 说明 |
| --- | --- |
| aigc_sensitive_word | 敏感词 |
| aigc_audit_record | 审核记录 |

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
aigc-gen 下载结果
  ↓
aigc-asset 上传并创建资产
  ↓
aigc-task 标记成功
  ↓
aigc-billing 确认扣费
```

失败链路：

```text
aigc-gen 捕获异常
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

| 场景 | 幂等方式 |
| --- | --- |
| 任务创建 | taskNo 唯一 |
| 积分冻结 | freezeNo 唯一，bizType + bizId 唯一 |
| 成功扣费 | taskId + CONSUME 唯一 |
| 失败退款 | taskId + REFUND 唯一 |
| 回调处理 | externalTaskId + callbackType 唯一 |
| 资产入库 | taskId + assetType 唯一 |

### 6.3 补偿任务

每个关键服务需要定时补偿：

| 服务 | 补偿任务 |
| --- | --- |
| aigc-task | 扫描长时间 RUNNING / CALLBACK_WAITING 任务 |
| aigc-gen | 轮询视频渠道外部任务状态 |
| aigc-billing | 扫描超时冻结未释放记录 |
| aigc-asset | 检查任务成功但资产未入库记录 |

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

| 微服务 | 数据库 |
| --- | --- |
| aigc-model | aigc_model_db |
| aigc-task | aigc_task_db |
| aigc-billing | aigc_billing_db |
| aigc-asset | aigc_asset_db |
| aigc-gen | aigc_gen_db |
| aigc-safety | aigc_safety_db |

拆库后禁止跨库 JOIN，通过 RPC API 查询。

## 8. 网关与注册配置

### 8.1 Nacos 服务名

建议服务名：

```text
yudao-module-aigc-model-server
yudao-module-aigc-billing-server
yudao-module-aigc-task-server
yudao-module-aigc-asset-server
yudao-module-aigc-gen-server
yudao-module-aigc-safety-server
```

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
6. aigc-gen 图片生成
7. aigc-gen 视频生成
8. 管理后台监控
```

### 9.2 为什么这个顺序

- 没有模型服务，生成服务无法选择模型和价格。
- 没有计费服务，不能上线赚钱。
- 没有任务服务，异步生成不可控。
- 没有资产服务，生成结果无法沉淀。
- 没有审核服务，内容风险不可控。
- 图片生成先做，成本低、验证快。
- 视频生成后做，客单价高、变现强。

### 9.3 每个阶段验收

#### aigc-model 验收

- 可以新增渠道商。
- 可以新增图片模型。
- 可以新增视频模型。
- 可以配置模型价格。
- 可以配置参数模板。
- 可以上下线模型。
- 用户端只展示启用且公开的模型。

#### aigc-billing 验收

- 用户钱包可初始化。
- 可用余额、冻结余额准确。
- 冻结积分不会超扣。
- 成功扣费幂等。
- 失败退款幂等。
- 计费流水完整。

#### aigc-task 验收

- 任务可创建。
- 状态流转正确。
- 日志完整。
- 回调幂等。
- 失败任务可重试。
- 任务可按用户分页查询。

#### aigc-asset 验收

- 图片资产可入库。
- 视频资产可入库。
- 用户只能看自己的资产。
- 下载次数可统计。
- 资产可删除。

#### aigc-gen 验收

- 文生图可生成。
- 图生图可生成。
- 文生视频可生成。
- 图生视频可生成。
- 成功扣费。
- 失败退款。
- 生成结果进入资产中心。

## 10. 服务间 API 依赖矩阵

| 调用方 | 被调用方 | 用途 |
| --- | --- | --- |
| aigc-gen | aigc-model | 校验模型、获取渠道配置、获取价格 |
| aigc-gen | aigc-billing | 冻结、扣费、退款 |
| aigc-gen | aigc-task | 创建任务、更新状态 |
| aigc-gen | aigc-asset | 创建资产 |
| aigc-gen | aigc-safety | 提示词审核 |
| aigc-task | aigc-billing | 异常任务退款补偿 |
| aigc-task | aigc-model | 任务展示模型信息 |
| aigc-asset | infra-api | 文件上传 |
| aigc-billing | pay-api | 后续充值支付 |

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

| 服务 | 错误码段 |
| --- | --- |
| aigc-model | 1-041-000-000 |
| aigc-billing | 1-041-100-000 |
| aigc-task | 1-041-200-000 |
| aigc-asset | 1-041-300-000 |
| aigc-gen | 1-041-400-000 |
| aigc-safety | 1-041-500-000 |

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
yudao-module-system-server
yudao-module-infra-server
yudao-module-aigc-model-server
yudao-module-aigc-billing-server
yudao-module-aigc-task-server
yudao-module-aigc-asset-server
yudao-module-aigc-safety-server
yudao-module-aigc-gen-server
```

如需充值支付，再增加：

```text
yudao-module-pay-server
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
提交图片/视频生成
  ↓
冻结积分
  ↓
生成任务执行
  ↓
结果入资产
  ↓
成功扣费 / 失败退款
  ↓
后台统计成本和毛利
```

后续再扩展：

```text
工作流 → 模板 → 社区 → 创作者激励 → 平台生态
```

这样既能快速上线赚钱，也能避免旧 AI 模块的历史包袱，并且后续可以自然演进成完整的 AIGC 多模态创作社区。
