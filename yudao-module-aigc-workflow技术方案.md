# yudao-module-aigc-workflow 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置

## 1. 方案定位

`yudao-module-aigc-workflow` 定位为 AIGC 第二阶段“工作流编排核心服务”，负责把文本、图片、视频、音频、文档、PPT、数字人等生成能力组合成可配置、可执行、可复用、可追踪的多节点内容生产流程。

当前仓库已落地该模块骨架与第一版核心闭环，现有两份建设方案中已明确其为第二阶段新增服务：

- `AIGC平台微服务分模块建设方案.md`：将 `aigc-workflow` 归为后续扩展服务，第二阶段建设。
- `AIGC平台第二三阶段分模块建设方案.md`：将 `yudao-module-aigc-workflow` 定义为工作流编排核心服务。

本模块建设目标是将第一阶段“单次生成”升级为第二阶段“多节点可编排生成”，支撑模板复用、批量生产、局部重跑、失败补偿、成本归集和资产溯源。当前实现已覆盖工作流定义、节点、连线、版本快照、实例节点图初始化、基础 DAG 推进、生成节点提交、节点回调推进、费用预估/冻结/确认/释放和执行日志。

## 2. 模块职责边界

### 2.1 负责内容

- 工作流定义管理
- 工作流节点定义
- 节点依赖关系管理
- 节点入参、出参映射
- 工作流实例创建
- 工作流实例执行状态
- 节点实例执行状态
- 节点失败重试
- 节点跳过
- 节点暂停和恢复
- 人工确认节点
- 局部重跑
- 批量执行
- 执行日志
- 工作流成本预估
- 工作流执行结果汇总
- 工作流模板引用
- 工作流版本管理
- 工作流发布、下线
- 租户维度工作流隔离

### 2.2 不负责内容

- 不直接调用第三方模型
- 不直接保存生成文件
- 不直接扣费和退款
- 不维护模型价格
- 不直接处理对象存储
- 不直接做社区发布
- 不直接做内容审核策略

### 2.3 核心边界原则

`aigc-workflow` 只做“编排和状态聚合”，不承接具体生成、计费、审核、文件存储和模型路由职责。

具体职责拆分如下：


| 能力                     | 承接模块        |
| ------------------------ | --------------- |
| 生成执行                 | `aigc-gen`      |
| 任务状态                 | `aigc-task`     |
| 资产归档                 | `aigc-asset`    |
| 费用冻结与扣费           | `aigc-billing`  |
| 内容安全检查             | `aigc-safety`   |
| 模型能力、价格、租户授权 | `aigc-model`    |
| 模板参数与一键同款       | `aigc-template` |

## 3. 建设目标

### 3.1 业务目标

- 支持文案生成图片、图片生成视频、视频生成配音、字幕和封面。
- 支持商品图生成模特图，再生成短视频和标题文案。
- 支持文章主题生成大纲、正文、配图和摘要。
- 支持 PPT 主题生成目录、页面内容、配图和 PPT 文件。
- 支持数字人口播脚本生成语音，再生成数字人视频和成片。
- 支持批量商品素材批量生成短视频。
- 支持模板参数输入后自动生成多种素材。

### 3.2 技术目标

- 将前端 React Flow 画布节点、边、项目草稿迁移为服务端工作流定义、节点定义、节点依赖和实例。
- 将前端直连生成代理改造为 `aigc-workflow -> aigc-task -> aigc-gen` 的后端编排链路。
- 将图片、视频等本地缓存迁移到 `aigc-asset`，形成正式资产、资产版本和节点产物关系。
- 将节点执行状态从前端本地状态改为服务端权威状态。
- 将费用预估、余额冻结、确认扣费、差额释放纳入统一计费链路。
- 将提示词、参考资产、节点结果和最终作品纳入审核链路。

## 4. Maven 与命名规范

### 4.1 Maven 结构

```text
yudao-module-aigc-workflow
  ├── yudao-module-aigc-workflow-api
  └── yudao-module-aigc-workflow-server
```

### 4.2 命名规范


| 类型                     | 命名                                    |
| ------------------------ | --------------------------------------- |
| 聚合模块目录             | `yudao-module-aigc-workflow`            |
| 聚合 artifactId          | `yudao-module-aigc-workflow`            |
| API 子模块 artifactId    | `yudao-module-aigc-workflow-api`        |
| Server 子模块 artifactId | `yudao-module-aigc-workflow-server`     |
| Spring 应用名            | `aigc-workflow-server`                  |
| 根包名                   | `cn.iocoder.yudao.module.aigc.workflow` |

### 4.3 URL 前缀


| 类型   | 路径                        |
| ------ | --------------------------- |
| 管理端 | `/aigc/workflow/**`         |
| 用户端 | `/app-api/aigc/workflow/**` |
| RPC    | `/rpc-api/aigc/workflow/**` |

## 5. 推荐模块结构

### 5.1 API 模块结构

```text
yudao-module-aigc-workflow-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/workflow
      ├── api
      │   └── AigcWorkflowApi.java
      ├── dto
      │   ├── AigcWorkflowDefinitionRespDTO.java
      │   ├── AigcWorkflowExecuteReqDTO.java
      │   ├── AigcWorkflowExecuteRespDTO.java
      │   ├── AigcWorkflowInstanceRespDTO.java
      │   ├── AigcWorkflowNodeInstanceRespDTO.java
      │   ├── AigcWorkflowRetryNodeReqDTO.java
      │   ├── AigcWorkflowCancelReqDTO.java
      │   └── AigcWorkflowCostEstimateReqDTO.java
      └── enums
          ├── AigcWorkflowStatusEnum.java
          ├── AigcWorkflowNodeTypeEnum.java
          ├── AigcWorkflowNodeStatusEnum.java
          ├── AigcWorkflowTriggerTypeEnum.java
          ├── AigcWorkflowVisibilityEnum.java
          └── ErrorCodeConstants.java
```

### 5.2 Server 模块结构

```text
yudao-module-aigc-workflow-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/workflow
      ├── AigcWorkflowServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── definition
      │   │   ├── node
      │   │   ├── instance
      │   │   ├── log
      │   │   └── statistics
      │   └── app
      │       ├── workflow
      │       └── instance
      ├── api
      │   └── AigcWorkflowApiImpl.java
      ├── service
      │   ├── definition
      │   ├── node
      │   ├── instance
      │   ├── executor
      │   ├── scheduler
      │   ├── cost
      │   ├── log
      │   └── statistics
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcWorkflowDefinitionDO.java
      │   │   ├── AigcWorkflowNodeDO.java
      │   │   ├── AigcWorkflowEdgeDO.java
      │   │   ├── AigcWorkflowVersionDO.java
      │   │   ├── AigcWorkflowInstanceDO.java
      │   │   ├── AigcWorkflowNodeInstanceDO.java
      │   │   └── AigcWorkflowLogDO.java
      │   └── mysql
      ├── job
      │   ├── AigcWorkflowTimeoutJob.java
      │   ├── AigcWorkflowRetryJob.java
      │   ├── AigcWorkflowStuckNodeJob.java
      │   ├── AigcWorkflowBillingCompensateJob.java
      │   └── AigcWorkflowAssetCompensateJob.java
      └── convert
```

## 6. 核心数据模型

### 6.1 数据表清单


| 表名                          | 说明               |
| ----------------------------- | ------------------ |
| `aigc_workflow_definition`    | 工作流定义         |
| `aigc_workflow_node`          | 工作流节点定义     |
| `aigc_workflow_edge`          | 节点连线和依赖关系 |
| `aigc_workflow_version`       | 工作流版本         |
| `aigc_workflow_instance`      | 工作流执行实例     |
| `aigc_workflow_node_instance` | 节点执行实例       |
| `aigc_workflow_log`           | 工作流执行日志     |

### 6.2 工作流定义表

表名：`aigc_workflow_definition`


| 字段                 | 类型     | 说明            |
| -------------------- | -------- | --------------- |
| `id`                 | bigint   | 主键            |
| `name`               | varchar  | 工作流名称      |
| `code`               | varchar  | 工作流编码      |
| `description`        | varchar  | 描述            |
| `cover_url`          | varchar  | 封面            |
| `category_id`        | bigint   | 分类            |
| `visibility`         | varchar  | 可见性          |
| `status`             | varchar  | 状态            |
| `current_version_id` | bigint   | 当前版本        |
| `input_schema`       | json     | 输入参数 Schema |
| `output_schema`      | json     | 输出参数 Schema |
| `config`             | json     | 工作流配置      |
| `creator_user_id`    | bigint   | 创建用户        |
| `creator`            | varchar  | 创建者          |
| `create_time`        | datetime | 创建时间        |
| `updater`            | varchar  | 更新者          |
| `update_time`        | datetime | 更新时间        |
| `deleted`            | bit      | 是否删除        |
| `tenant_id`          | bigint   | 租户编号        |

建议索引：


| 索引                  | 字段                     | 说明                 |
| --------------------- | ------------------------ | -------------------- |
| `uk_tenant_code`      | `tenant_id, code`        | 租户下工作流编码唯一 |
| `idx_tenant_status`   | `tenant_id, status`      | 按状态查询           |
| `idx_tenant_category` | `tenant_id, category_id` | 按分类查询           |

### 6.3 工作流节点表

表名：`aigc_workflow_node`


| 字段              | 类型    | 说明         |
| ----------------- | ------- | ------------ |
| `id`              | bigint  | 主键         |
| `workflow_id`     | bigint  | 工作流 ID    |
| `version_id`      | bigint  | 版本 ID      |
| `node_key`        | varchar | 节点唯一键   |
| `node_name`       | varchar | 节点名称     |
| `node_type`       | varchar | 节点类型     |
| `generate_type`   | varchar | 生成类型     |
| `generate_mode`   | varchar | 生成模式     |
| `model_id`        | bigint  | 默认模型     |
| `input_mapping`   | json    | 入参映射     |
| `output_mapping`  | json    | 出参映射     |
| `param_config`    | json    | 节点参数     |
| `retry_config`    | json    | 重试配置     |
| `timeout_seconds` | int     | 超时时间     |
| `position`        | json    | 前端画布位置 |
| `tenant_id`       | bigint  | 租户编号     |

### 6.4 工作流实例表

表名：`aigc_workflow_instance`


| 字段                  | 类型     | 说明        |
| --------------------- | -------- | ----------- |
| `id`                  | bigint   | 主键        |
| `instance_no`         | varchar  | 实例编号    |
| `workflow_id`         | bigint   | 工作流 ID   |
| `workflow_version_id` | bigint   | 版本 ID     |
| `template_id`         | bigint   | 来源模板    |
| `user_id`             | bigint   | 用户 ID     |
| `status`              | varchar  | 状态        |
| `input_data`          | json     | 输入数据    |
| `output_data`         | json     | 输出数据    |
| `main_task_id`        | bigint   | 主任务 ID   |
| `freeze_id`           | bigint   | 计费冻结 ID |
| `estimate_amount`     | bigint   | 预估费用    |
| `actual_amount`       | bigint   | 实际费用    |
| `progress`            | int      | 进度        |
| `fail_reason`         | varchar  | 失败原因    |
| `fail_message`        | varchar  | 失败详情    |
| `start_time`          | datetime | 开始时间    |
| `finish_time`         | datetime | 结束时间    |
| `tenant_id`           | bigint   | 租户编号    |

### 6.5 节点实例表

表名：`aigc_workflow_node_instance`


| 字段                   | 类型     | 说明         |
| ---------------------- | -------- | ------------ |
| `id`                   | bigint   | 主键         |
| `workflow_instance_id` | bigint   | 工作流实例   |
| `node_id`              | bigint   | 节点定义     |
| `node_key`             | varchar  | 节点键       |
| `status`               | varchar  | 节点状态     |
| `task_id`              | bigint   | 关联任务 ID  |
| `gen_record_id`        | bigint   | 生成记录 ID  |
| `input_data`           | json     | 节点入参     |
| `output_data`          | json     | 节点出参     |
| `asset_ids`            | json     | 输出资产     |
| `retry_count`          | int      | 重试次数     |
| `max_retry_count`      | int      | 最大重试次数 |
| `cost_amount`          | bigint   | 节点费用     |
| `start_time`           | datetime | 开始时间     |
| `finish_time`          | datetime | 结束时间     |
| `tenant_id`            | bigint   | 租户编号     |

## 7. 节点类型设计


| 节点类型            | 说明       | 第一版建议 |
| ------------------- | ---------- | ---------- |
| `START`             | 开始节点   | 必做       |
| `TEXT_GENERATE`     | 文本生成   | 必做       |
| `IMAGE_GENERATE`    | 图片生成   | 必做       |
| `VIDEO_GENERATE`    | 视频生成   | 必做       |
| `AUDIO_GENERATE`    | 音频生成   | 可选       |
| `DOCUMENT_GENERATE` | 文档生成   | 后续       |
| `PPT_GENERATE`      | PPT 生成   | 后续       |
| `DIGITAL_HUMAN`     | 数字人生成 | 后续       |
| `ASSET_INPUT`       | 资产输入   | 必做       |
| `ASSET_OUTPUT`      | 资产输出   | 必做       |
| `CONDITION`         | 条件判断   | 二期增强   |
| `MANUAL_CONFIRM`    | 人工确认   | 二期增强   |
| `END`               | 结束节点   | 必做       |

第一版建议优先支持 `START`、`TEXT_GENERATE`、`IMAGE_GENERATE`、`VIDEO_GENERATE`、`ASSET_INPUT`、`ASSET_OUTPUT`、`END`，满足当前前端画布和第一阶段生成能力接入需要。

### 7.1 快速生成项目初始化

用户端 `/app` 快捷生成会直接创建 canvas 项目并运行首个图片或视频生成节点。后端快速生成请求需要同时兼容旧版单图字段和新版多图字段：

| 字段 | 说明 |
| ---- | ---- |
| `referenceAssetId` | 旧版单张参考图资产 ID |
| `referenceAssetIds` | 新版多张参考图资产 ID |
| `referencePreviewUrl` | 旧版单张参考图预览 URL |
| `referencePreviewUrls` | 新版多张参考图预览 URL |

归一化规则：

- `referenceAssetIds` / `referencePreviewUrls` 非空时优先使用数组字段。
- 数组为空时回退到 `referenceAssetId` / `referencePreviewUrl`。
- 第一张参考图继续作为项目封面和旧版单图兼容字段。
- 多图请求仍保留完整数组，用于画布节点、资产绑定和生成请求。

画布初始化规则：

- 每张参考图创建一个 `image` 类型参考节点，节点数据写入 `assetId`、`previewUrl`、`fileName`、`mimeType` 等资产元信息。
- 目标生成节点根据 `nodeType` 创建为 `image` 或 `video`。
- 每个参考图节点通过 `signal` 边连接到目标生成节点，使 canvas 的 `ImageNode` / `VideoNode` 可以通过 incoming edges 读取参考图。
- `runReqVO.inputParams` 接收完整归一化参数，用于真正发起生成任务。
- 节点展示用的 `params` 应剥离请求专用字段，例如 `referenceImages`、`referenceAssetIds`、`referenceImageIds`、`inputImages`、`inputImageUrls`、`inputImageIds`，避免把参考图传输字段重复展示为模型参数。

## 8. 状态机设计

### 8.1 工作流实例状态

```text
CREATED
  ↓
ESTIMATING
  ↓
FROZEN
  ↓
RUNNING
  ↓
SUCCESS

RUNNING
  ↓
WAITING_MANUAL

RUNNING
  ↓
FAILED
  ↓
REFUNDING
  ↓
REFUNDED

RUNNING
  ↓
CANCELING
  ↓
CANCELED
```

### 8.2 节点实例状态

```text
PENDING
  ↓
READY
  ↓
RUNNING
  ↓
SUCCESS

RUNNING
  ↓
FAILED
  ↓
RETRYING
  ↓
READY

RUNNING
  ↓
SKIPPED

RUNNING
  ↓
WAITING_MANUAL
```

### 8.3 状态更新要求

- 工作流状态更新必须使用 `id + old_status` 条件更新。
- 节点状态更新必须使用 `id + old_status` 条件更新。
- 节点执行前必须加分布式锁或数据库状态锁。
- 同一个工作流实例不能重复触发相同节点。
- 回调处理必须幂等。
- 人工确认节点必须防重复提交。

## 9. 核心接口设计

### 9.1 管理端接口


| 方法   | 路径                                 | 说明       |
| ------ | ------------------------------------ | ---------- |
| `POST` | `/aigc/workflow/definition/create`   | 创建工作流 |
| `PUT`  | `/aigc/workflow/definition/update`   | 修改工作流 |
| `GET`  | `/aigc/workflow/definition/get`      | 工作流详情 |
| `GET`  | `/aigc/workflow/definition/page`     | 工作流分页 |
| `POST` | `/aigc/workflow/definition/publish`  | 发布工作流 |
| `POST` | `/aigc/workflow/definition/offline`  | 下线工作流 |
| `POST` | `/aigc/workflow/version/create`      | 创建版本   |
| `GET`  | `/aigc/workflow/instance/page`       | 实例分页   |
| `GET`  | `/aigc/workflow/instance/get`        | 实例详情   |
| `POST` | `/aigc/workflow/instance/retry-node` | 重试节点   |
| `POST` | `/aigc/workflow/instance/cancel`     | 取消实例   |
| `GET`  | `/aigc/workflow/statistics/summary`  | 工作流统计 |

### 9.2 用户端接口


| 方法   | 路径                             | 说明           |
| ------ | -------------------------------- | -------------- |
| `GET`  | `/aigc/workflow/list`            | 可用工作流列表 |
| `GET`  | `/aigc/workflow/get`             | 工作流详情     |
| `POST` | `/aigc/workflow/estimate`        | 费用预估       |
| `POST` | `/aigc/workflow/execute`         | 执行工作流     |
| `GET`  | `/aigc/workflow/instance/get`    | 实例详情       |
| `GET`  | `/aigc/workflow/instance/page`   | 我的工作流实例 |
| `POST` | `/aigc/workflow/instance/cancel` | 取消执行       |

### 9.3 RPC 接口

```text
AigcWorkflowApi
  ├── execute(userId, reqDTO)
  ├── getInstance(instanceId)
  ├── retryNode(instanceId, nodeInstanceId)
  ├── cancel(instanceId)
  ├── estimateCost(reqDTO)
  └── handleNodeCallback(nodeInstanceId, taskId, result)
```

## 10. 执行链路

```text
用户端 / 管理端
  ↓
选择工作流或模板
  ↓
aigc-workflow 创建工作流实例
  ↓
aigc-workflow 加载版本快照、节点和边
  ↓
aigc-model 校验模型能力、参数、租户授权、价格
  ↓
aigc-billing 预估费用并冻结余额
  ↓
aigc-task 创建主任务和节点任务
  ↓
aigc-workflow 调度 READY 节点
  ↓
aigc-gen 执行文本 / 图片 / 视频 / 音频生成
  ↓
aigc-safety 审核输入、节点结果和最终资产
  ↓
aigc-asset 保存节点产物、资产版本和来源关系
  ↓
aigc-workflow 汇总输出、推进后继节点
  ↓
aigc-billing 确认扣费、释放差额
  ↓
返回工作流实例结果
```

## 11. 服务协作设计


| 被调用服务      | 用途                                         |
| --------------- | -------------------------------------------- |
| `aigc-model`    | 校验节点模型、能力、参数、价格、租户授权     |
| `aigc-billing`  | 工作流费用预估、余额冻结、确认扣费、释放差额 |
| `aigc-task`     | 创建主任务、节点任务、记录状态和任务日志     |
| `aigc-gen`      | 执行具体生成节点、查询第三方任务状态         |
| `aigc-asset`    | 保存节点输出文件、建立资产关系、版本关系     |
| `aigc-safety`   | 检查工作流输入、提示词、节点结果和最终资产   |
| `aigc-template` | 提供工作流模板、参数 Schema、一键同款配置    |

## 12. 幂等与补偿

### 12.1 幂等键


| 场景           | 幂等键                               |
| -------------- | ------------------------------------ |
| 工作流实例创建 | `instance_no`                        |
| 节点实例创建   | `workflow_instance_id + node_key`    |
| 节点执行       | `node_instance_id + execute_version` |
| 节点调用生成   | `node_instance_id + task_id`         |
| 计费冻结       | `biz_type + biz_id`                  |
| 节点资产创建   | `node_instance_id + asset_type`      |
| 工作流回调     | `node_instance_id + callback_no`     |

### 12.2 补偿任务


| Job                                | 说明                 |
| ---------------------------------- | -------------------- |
| `AigcWorkflowTimeoutJob`           | 扫描超时工作流       |
| `AigcWorkflowRetryJob`             | 扫描可重试失败节点   |
| `AigcWorkflowStuckNodeJob`         | 扫描卡住节点         |
| `AigcWorkflowBillingCompensateJob` | 对账冻结、扣费、释放 |
| `AigcWorkflowAssetCompensateJob`   | 补偿节点资产关系     |

## 13. 可观测性要求

- 记录工作流实例日志。
- 记录节点执行日志。
- 记录节点输入输出摘要。
- 记录节点耗时。
- 记录节点模型、渠道、费用。
- 记录节点失败原因。
- 记录第三方任务 ID。
- 记录资产 ID。
- 记录计费冻结 ID。
- 记录用户 ID 和租户 ID。

## 14. 前端迁移方案

### 14.1 当前前端可复用能力

- 保留现有创作工作台交互，不重新设计一套割裂的用户端画布。
- 保留 React Flow 节点拖拽、连线、位置、节点编辑能力。
- 保留从画布创建上下游节点的交互体验。
- 保留本地草稿能力作为离线编辑和异常兜底。

### 14.2 后端承接关系


| 现有用户端能力     | 后端承接模块                                           | 演进方式                                   |
| ------------------ | ------------------------------------------------------ | ------------------------------------------ |
| React Flow 节点    | `aigc-workflow` 节点定义、节点实例                     | 保存节点类型、位置、参数、输入输出 Schema  |
| React Flow 边      | `aigc-workflow` 节点依赖关系                           | 保存上游节点、下游节点、输入映射和执行顺序 |
| 本地项目草稿       | `aigc-workflow` 工作流实例 / `aigc-asset` 项目关系     | 登录用户维度云端保存，保留本地缓存兜底     |
| 图片、视频上传缓存 | `aigc-asset` 资产与资产版本                            | 上传对象存储，生成正式资产 ID 和版本 ID    |
| 图片生成节点       | `aigc-task` + `aigc-gen` 图片生成任务                  | 由工作流创建节点任务并回写节点产物         |
| 视频生成节点       | `aigc-task` + `aigc-gen` 视频生成任务                  | 由工作流追踪异步任务、轮询和结果归档       |
| 前端生成参数       | `aigc-template` 参数 Schema / `aigc-workflow` 节点参数 | 支持模板复用、一键同款和批量生产           |

### 14.3 必须整改的问题

- 本地草稿不能继续作为主存储，服务端工作流实例应成为主数据源。
- 图片、视频二进制不能继续主要缓存在浏览器，需要上传对象存储并登记资产。
- 前端不能继续直接驱动生成代理，应统一收敛到后端编排链路。
- 节点执行状态不能继续只由组件状态维护，应使用工作流节点实例和任务状态作为权威状态。
- 生成前必须接入组合计费、预估冻结和差额释放。
- 提示词、参考图、生成结果和最终作品必须进入统一审核链路。
- 节点产物需要在 `aigc-asset` 中保存资产版本、上游来源和引用关系。

## 15. 第一版建设范围

第一版建议优先完成生产闭环，而不是一次性实现所有复杂节点。

### 15.1 必做能力

- Maven 模块创建与根工程接入。
- 工作流定义、节点、边、版本管理。
- 工作流实例创建。
- `TEXT_GENERATE`、`IMAGE_GENERATE`、`VIDEO_GENERATE` 节点执行。
- 基础 DAG 调度。
- 费用预估、余额冻结、确认扣费、差额释放。
- 节点任务创建与状态同步。
- 节点产物归档到资产中心。
- 工作流输入、节点结果、最终结果审核。
- 失败重试、取消、超时扫描、卡住节点补偿。
- 执行日志与节点日志。

### 15.2 暂缓能力

- 复杂条件节点。
- 人工确认节点完整后台。
- PPT 生成。
- 数字人生成。
- 模板市场。
- 社区发布。
- 创作者收益。
- 多人协作编辑。

## 16. 落地步骤

### 16.1 阶段一：模块骨架

- 创建 `yudao-module-aigc-workflow` 聚合模块。
- 创建 `yudao-module-aigc-workflow-api` 和 `yudao-module-aigc-workflow-server`。
- 接入根工程 `pom.xml`。
- 配置 `spring.application.name=aigc-workflow-server`。
- 补齐 Nacos、RPC、MyBatis、Redis、Tenant、Security、Job、Monitor 等依赖。

### 16.2 阶段二：定义与版本

- 实现工作流定义 CRUD。
- 实现节点定义 CRUD。
- 实现边关系 CRUD。
- 实现工作流版本创建、发布、下线。
- 支持前端画布节点、边、位置、参数保存与回显。

### 16.3 阶段三：实例与调度

- 实现工作流实例创建。
- 根据版本快照初始化节点实例。
- 实现 DAG 依赖解析。
- 实现 READY 节点调度。
- 实现节点执行状态推进。
- 实现工作流整体进度计算。

### 16.4 阶段四：生成闭环

- 接入 `aigc-model` 校验模型能力、租户授权、参数和价格。
- 接入 `aigc-billing` 做费用预估和余额冻结。
- 接入 `aigc-task` 创建主任务和节点任务。
- 接入 `aigc-gen` 执行文本、图片、视频生成。
- 接入 `aigc-asset` 归档节点产物。
- 接入 `aigc-safety` 审核输入和输出。

### 16.5 阶段五：生产增强

- 实现节点失败重试。
- 实现局部重跑。
- 实现工作流取消。
- 实现超时扫描。
- 实现卡住节点补偿。
- 实现计费补偿。
- 实现资产关系补偿。
- 实现执行日志、统计报表和问题追踪。

## 17. 总结

`yudao-module-aigc-workflow` 是第二阶段从“单次生成工具”升级为“可复用、可编排、可批量生产的内容生产平台”的核心模块。

它的关键不是直接做生成，而是把模型、生成、任务、计费、资产、审核、模板串成生产级闭环。第一版应优先落地服务端工作流定义、图片/文本/视频节点执行、资产归档、计费冻结扣费、失败重试和执行日志，再逐步扩展条件节点、人工确认、PPT、数字人、模板市场和社区生态。
