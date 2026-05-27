# AIGC 平台第二三阶段分模块建设方案

## 1. 方案定位

本方案基于当前 AIGC 平台第一阶段微服务建设成果，补充第二阶段、第三阶段的完整生产级分模块建设方案。

第一阶段已经围绕“先上线赚钱”完成最小商业闭环，核心模块包括：

```text
yudao-module-aigc-model      模型与渠道服务
yudao-module-aigc-billing    计费钱包服务
yudao-module-aigc-task       任务调度服务
yudao-module-aigc-asset      资产中心服务
yudao-module-aigc-gen        生成服务
yudao-module-aigc-safety     审核风控服务
```

第二阶段、第三阶段不推翻第一阶段架构，而是在现有服务边界之上继续演进。

总体演进目标：

```text
第一阶段：最小赚钱闭环
第二阶段：复杂内容生产、模板复用、工作流编排
第三阶段：社区分发、发布导出、模板市场、增长运营
```

## 2. 阶段目标

### 2.1 第二阶段目标

第二阶段目标是从“单次生成赚钱”升级为“可复用、可编排、可批量生产的内容生产平台”。

核心能力：

- 工作流编排
- 模板体系
- 资产版本
- 资产关系
- 批量生产
- 高级审核
- 成本与毛利精细化
- 管理后台增强
- 多渠道模型路由增强
- 生产级补偿与可观测性

第二阶段建议建设模块：

| 模块 | 是否新建服务 | 阶段定位 |
| ---- | ------------ | -------- |
| `yudao-module-aigc-workflow` | 是 | 工作流编排核心服务 |
| `yudao-module-aigc-template` | 是 | 模板基础能力服务 |
| `yudao-module-aigc-asset` | 否 | 增加资产关系、版本、项目化能力 |
| `yudao-module-aigc-gen` | 否 | 扩展音频、图生图、图生视频、文档、PPT、数字人 |
| `yudao-module-aigc-task` | 否 | 支持工作流子任务、批量任务、任务编排追踪 |
| `yudao-module-aigc-billing` | 否 | 支持组合计费、预估冻结、节点成本归集 |
| `yudao-module-aigc-safety` | 否 | 支持多对象审核、发布前审核、审核补偿 |
| `yudao-module-aigc-model` | 否 | 支持高级路由、渠道健康、租户限额、模型 SLA |

### 2.2 第三阶段目标

第三阶段目标是从“内容生产工具”升级为“内容分发、模板复用、社区增长和商业生态平台”。

核心能力：

- 社区发布
- 作品流
- 点赞、收藏、评论
- 关注关系
- 模板市场
- 一键同款
- 发布导出
- 举报与治理
- 榜单与推荐
- 创作者体系预留
- 增长运营

第三阶段建议建设模块：

| 模块 | 是否新建服务 | 阶段定位 |
| ---- | ------------ | -------- |
| `yudao-module-aigc-community` | 是 | 社区与作品流核心服务 |
| `yudao-module-aigc-publish` | 是 | 发布、导出、分发服务 |
| `yudao-module-aigc-template` | 否 | 从模板基础能力升级为模板市场 |
| `yudao-module-aigc-safety` | 否 | 社区内容审核、举报处理、风控策略 |
| `yudao-module-aigc-asset` | 否 | 公共资产访问、作品关联、版权状态 |
| `yudao-module-aigc-billing` | 否 | 模板购买、作品复用、收益分账预留 |
| `yudao-module-aigc-creator` | 建议第三阶段末或第四阶段 | 创作者主页、收益、激励体系 |

## 3. 第二阶段总体架构

第二阶段新增核心服务：

```text
yudao-module-aigc-workflow   工作流服务
yudao-module-aigc-template   模板服务
```

第二阶段核心调用关系：

```text
用户端 / 管理端
      ↓
aigc-template
      ↓
选择模板 / 套用模板
      ↓
aigc-workflow
      ↓
创建工作流实例
      ↓
aigc-task 创建主任务与节点任务
      ↓
aigc-gen 执行具体生成节点
      ↓
aigc-model 校验模型、路由、价格
      ↓
aigc-billing 冻结、扣费、成本归集
      ↓
aigc-asset 保存节点产物、版本和关系
      ↓
aigc-safety 审核提示词、节点结果、最终作品
```

第二阶段不是简单增加功能，而是把“一次生成”升级为“多节点可编排生成”。

### 3.1 现有用户端创作工作台评估与兼容边界

当前用户端 `yudao-ui/draw2video-client` 已经具备第二阶段工作流编排的前端基础，但只能兼容其中符合生产级架构方向的交互和数据模型雏形，不能把现有实现整体原样纳入第二阶段。第二阶段应遵循“保留合规体验、剥离不合规实现、迁移到服务端闭环”的原则。

现有能力包括：

- 已有统一创作画布，入口位于 `src/app/(app)/create/image/page.tsx`
- 已基于 React Flow 支持图片、文本、视频等节点的拖拽、连线和局部编辑
- 已支持从画布创建上下游节点，具备“文生图、图生图、图生视频、图文扩展”的节点编排雏形
- 已支持图片、视频素材拖入画布，并通过 IndexedDB 与 localStorage 保存本地草稿
- 已接入图片生成代理、Seedance 视频生成代理、Wan 视频生成代理，视频任务支持异步轮询
- 已有项目草稿、节点数据、边关系和本地素材缓存，可作为后端工作流定义、实例和资产关系的迁移基础

第二阶段可兼容内容：

- 保留现有创作工作台交互，不重新设计一套割裂的用户端画布
- 将当前前端节点、边、项目草稿映射为 `aigc-workflow` 的工作流定义、节点定义、节点依赖和工作流实例
- 将当前本地图片、视频缓存逐步迁移到 `aigc-asset`，形成正式资产、资产版本和节点产物关系
- 将当前前端直连生成代理逐步收敛为 `aigc-workflow -> aigc-task -> aigc-gen` 的后端编排链路
- 将当前图片、视频生成状态迁移为主任务、节点任务和节点实例状态，支持失败重试、局部重跑、执行日志和成本归集
- 保留本地草稿能力作为离线编辑和异常兜底，但以服务端工作流实例为主数据源

现有实现中不合规或不达标内容：

| 不合规点 | 当前表现 | 风险 | 第二阶段整改方向 |
| ---- | ---- | ---- | ---- |
| 前端本地草稿作为主存储 | 项目、节点、边主要保存在 localStorage | 跨设备不可用，无法审计，无法恢复服务端状态 | 服务端工作流实例作为主数据源，本地草稿仅作为临时缓存 |
| 图片、视频二进制缓存在浏览器 | 图片和上传视频主要存 IndexedDB | 数据不可共享，容量不可控，资产无法纳管 | 上传到对象存储并登记到 `aigc-asset`，返回资产 ID 和版本 ID |
| 前端直接驱动生成代理 | 图片、视频节点直接调用 Next.js 代理接口 | 绕过统一任务、计费、审核、风控和成本归集 | 统一改为 `aigc-workflow -> aigc-task -> aigc-gen` 调度 |
| 文本生成为本地 mock | 文本节点未接真实生成服务 | 无法商业化计费，无法审核和追踪 | 接入 `aigc-gen` 文本生成能力，并纳入任务状态机 |
| 节点执行状态只在前端维护 | loading、error、result 主要由组件状态驱动 | 刷新、换端、失败恢复能力不足 | 使用工作流节点实例和任务状态作为权威状态 |
| 缺少统一计费冻结 | 生成前未走组合计费、预估冻结、差额释放 | 容易漏扣、重复扣或无法核算毛利 | 由 `aigc-billing` 按工作流和节点进行预估、冻结、扣费和释放 |
| 缺少内容审核链路 | 提示词、参考图、生成结果未统一进入审核服务 | 存在违规内容生成和发布风险 | 接入 `aigc-safety` 的提示词、资产、节点结果和最终作品审核 |
| 缺少服务端资产关系 | 节点与图片、视频结果关系主要在画布数据内 | 无法做版本、溯源、复用、版权和发布 | 在 `aigc-asset` 保存节点产物、资产版本、上游来源和引用关系 |
| 缺少工作流版本管理 | 当前画布草稿没有正式定义版本 | 模板复用、批量生产、回滚困难 | 在 `aigc-workflow` 中保存定义版本、发布状态和实例快照 |
| 缺少租户和权限隔离 | 本地数据天然绕过租户权限模型 | 多租户、团队协作和后台治理不可控 | 所有工作流、资产、任务、模板按租户和用户维度隔离 |
| 前端路由命名不清晰 | 统一工作台入口仍在 `/create/image` | 产品认知与后续多模态工作流不一致 | 后续改造为 `/create/workspace` 或保留兼容跳转 |
| 旧节点兼容逻辑残留 | `prompt/result` 旧流程仍保留迁移兼容 | 数据模型长期混杂，维护成本增加 | 仅保留迁移期读取能力，新数据统一使用 image/text/video/workflow 节点模型 |

前端与后端映射关系：

| 现有用户端能力 | 第二阶段后端承接模块 | 演进方式 |
| ---- | ---- | ---- |
| React Flow 节点 | `aigc-workflow` 节点定义、节点实例 | 保存节点类型、位置、参数、输入输出 Schema |
| React Flow 边 | `aigc-workflow` 节点依赖关系 | 保存上游节点、下游节点、输入映射和执行顺序 |
| 本地项目草稿 | `aigc-workflow` 工作流实例 / `aigc-asset` 项目关系 | 登录用户维度云端保存，保留本地缓存兜底 |
| 图片、视频上传缓存 | `aigc-asset` 资产与资产版本 | 上传对象存储，生成正式资产 ID 和版本 ID |
| 图片生成节点 | `aigc-task` + `aigc-gen` 图片生成任务 | 由工作流创建节点任务并回写节点产物 |
| 视频生成节点 | `aigc-task` + `aigc-gen` 视频生成任务 | 由工作流追踪异步任务、轮询和结果归档 |
| 前端生成参数 | `aigc-template` 参数 Schema / `aigc-workflow` 节点参数 | 支持模板复用、一键同款和批量生产 |

因此，第二阶段用户端建设重点不是“全部兼容现有实现”，而是明确兼容边界：保留画布交互、节点编辑、拖拽连线等用户体验；剥离本地化、直连化、不可审计、不可计费、不可审核的实现；最终统一收敛到服务端工作流、资产、任务、计费和审核闭环。

## 4. 工作流服务：aigc-workflow

### 4.1 服务定位

`yudao-module-aigc-workflow` 是 AIGC 平台的内容生产编排服务，负责把多个 AIGC 生成能力组合成一个可配置、可执行、可复用、可追踪的工作流。

该模块不直接调用第三方模型，不直接扣费，不保存文件，不维护模型价格。它负责编排流程，具体执行仍然交给 `aigc-gen`、`aigc-task`、`aigc-billing`、`aigc-asset`、`aigc-safety`、`aigc-model`。

### 4.2 核心业务场景

- 文案生成图片，图片生成视频，视频生成配音、字幕和封面
- 商品图生成模特图，再生成短视频和标题文案
- 文章主题生成大纲、正文、配图和摘要
- PPT 主题生成目录、页面内容、配图和 PPT 文件
- 数字人口播脚本生成语音，再生成数字人视频和成片
- 批量商品素材批量生成短视频
- 模板参数输入后自动生成多种素材

### 4.3 Maven 结构

```text
yudao-module-aigc-workflow
  ├── yudao-module-aigc-workflow-api
  └── yudao-module-aigc-workflow-server
```

### 4.4 命名规范

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-workflow` |
| API 子模块 | `yudao-module-aigc-workflow-api` |
| Server 子模块 | `yudao-module-aigc-workflow-server` |
| Spring 应用名 | `aigc-workflow-server` |
| 根包名 | `cn.iocoder.yudao.module.aigc.workflow` |

### 4.5 URL 前缀

| 类型 | 路径 |
| ---- | ---- |
| 管理端 | `/aigc/workflow/**` |
| 用户端 | `/app-api/aigc/workflow/**` |
| RPC | `/rpc-api/aigc/workflow/**` |

### 4.6 核心职责

负责内容：

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

不负责内容：

- 不直接调用第三方模型
- 不直接保存生成文件
- 不直接扣费和退款
- 不维护模型价格
- 不直接处理对象存储
- 不直接做社区发布
- 不直接做内容审核策略

### 4.7 API 模块结构

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

### 4.8 Server 模块结构

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
      │   └── AigcWorkflowStuckNodeJob.java
      └── convert
```

### 4.9 数据库设计

| 表名 | 说明 |
| ---- | ---- |
| `aigc_workflow_definition` | 工作流定义 |
| `aigc_workflow_node` | 工作流节点定义 |
| `aigc_workflow_edge` | 节点连线和依赖关系 |
| `aigc_workflow_version` | 工作流版本 |
| `aigc_workflow_instance` | 工作流执行实例 |
| `aigc_workflow_node_instance` | 节点执行实例 |
| `aigc_workflow_log` | 工作流执行日志 |

#### 4.9.1 aigc_workflow_definition

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `name` | varchar | 工作流名称 |
| `code` | varchar | 工作流编码 |
| `description` | varchar | 描述 |
| `cover_url` | varchar | 封面 |
| `category_id` | bigint | 分类 |
| `visibility` | varchar | 可见性 |
| `status` | varchar | 状态 |
| `current_version_id` | bigint | 当前版本 |
| `input_schema` | json | 输入参数 Schema |
| `output_schema` | json | 输出参数 Schema |
| `config` | json | 工作流配置 |
| `creator_user_id` | bigint | 创建用户 |
| `creator` | varchar | 创建者 |
| `create_time` | datetime | 创建时间 |
| `updater` | varchar | 更新者 |
| `update_time` | datetime | 更新时间 |
| `deleted` | bit | 是否删除 |
| `tenant_id` | bigint | 租户编号 |

建议索引：

| 索引 | 字段 | 说明 |
| ---- | ---- | ---- |
| `uk_tenant_code` | `tenant_id, code` | 租户下工作流编码唯一 |
| `idx_tenant_status` | `tenant_id, status` | 按状态查询 |
| `idx_tenant_category` | `tenant_id, category_id` | 按分类查询 |

#### 4.9.2 aigc_workflow_node

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `workflow_id` | bigint | 工作流 ID |
| `version_id` | bigint | 版本 ID |
| `node_key` | varchar | 节点唯一键 |
| `node_name` | varchar | 节点名称 |
| `node_type` | varchar | 节点类型 |
| `generate_type` | varchar | 生成类型 |
| `generate_mode` | varchar | 生成模式 |
| `model_id` | bigint | 默认模型 |
| `input_mapping` | json | 入参映射 |
| `output_mapping` | json | 出参映射 |
| `param_config` | json | 节点参数 |
| `retry_config` | json | 重试配置 |
| `timeout_seconds` | int | 超时时间 |
| `position` | json | 前端画布位置 |
| `tenant_id` | bigint | 租户编号 |

节点类型建议：

| 节点类型 | 说明 |
| ---- | ---- |
| `START` | 开始节点 |
| `TEXT_GENERATE` | 文本生成 |
| `IMAGE_GENERATE` | 图片生成 |
| `VIDEO_GENERATE` | 视频生成 |
| `AUDIO_GENERATE` | 音频生成 |
| `DOCUMENT_GENERATE` | 文档生成 |
| `PPT_GENERATE` | PPT 生成 |
| `DIGITAL_HUMAN` | 数字人生成 |
| `ASSET_INPUT` | 资产输入 |
| `ASSET_OUTPUT` | 资产输出 |
| `CONDITION` | 条件判断 |
| `MANUAL_CONFIRM` | 人工确认 |
| `END` | 结束节点 |

#### 4.9.3 aigc_workflow_instance

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `instance_no` | varchar | 实例编号 |
| `workflow_id` | bigint | 工作流 ID |
| `workflow_version_id` | bigint | 版本 ID |
| `template_id` | bigint | 来源模板 |
| `user_id` | bigint | 用户 ID |
| `status` | varchar | 状态 |
| `input_data` | json | 输入数据 |
| `output_data` | json | 输出数据 |
| `main_task_id` | bigint | 主任务 ID |
| `freeze_id` | bigint | 计费冻结 ID |
| `estimate_amount` | bigint | 预估费用 |
| `actual_amount` | bigint | 实际费用 |
| `progress` | int | 进度 |
| `fail_reason` | varchar | 失败原因 |
| `fail_message` | varchar | 失败详情 |
| `start_time` | datetime | 开始时间 |
| `finish_time` | datetime | 结束时间 |
| `tenant_id` | bigint | 租户编号 |

#### 4.9.4 aigc_workflow_node_instance

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `workflow_instance_id` | bigint | 工作流实例 |
| `node_id` | bigint | 节点定义 |
| `node_key` | varchar | 节点键 |
| `status` | varchar | 节点状态 |
| `task_id` | bigint | 关联任务 ID |
| `gen_record_id` | bigint | 生成记录 ID |
| `input_data` | json | 节点入参 |
| `output_data` | json | 节点出参 |
| `asset_ids` | json | 输出资产 |
| `retry_count` | int | 重试次数 |
| `max_retry_count` | int | 最大重试次数 |
| `cost_amount` | bigint | 节点费用 |
| `start_time` | datetime | 开始时间 |
| `finish_time` | datetime | 结束时间 |
| `tenant_id` | bigint | 租户编号 |

### 4.10 状态机设计

工作流实例状态：

```text
CREATED
  ↓
ESTIMATING
  ↓
FROZEN
  ↓
RUNNING
  ↓
WAITING_MANUAL
  ↓
SUCCESS

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

节点实例状态：

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

### 4.11 核心接口

管理端接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/aigc/workflow/definition/create` | 创建工作流 |
| PUT | `/aigc/workflow/definition/update` | 修改工作流 |
| GET | `/aigc/workflow/definition/get` | 工作流详情 |
| GET | `/aigc/workflow/definition/page` | 工作流分页 |
| POST | `/aigc/workflow/definition/publish` | 发布工作流 |
| POST | `/aigc/workflow/definition/offline` | 下线工作流 |
| POST | `/aigc/workflow/version/create` | 创建版本 |
| GET | `/aigc/workflow/instance/page` | 实例分页 |
| GET | `/aigc/workflow/instance/get` | 实例详情 |
| POST | `/aigc/workflow/instance/retry-node` | 重试节点 |
| POST | `/aigc/workflow/instance/cancel` | 取消实例 |
| GET | `/aigc/workflow/statistics/summary` | 工作流统计 |

用户端接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/aigc/workflow/list` | 可用工作流列表 |
| GET | `/aigc/workflow/get` | 工作流详情 |
| POST | `/aigc/workflow/estimate` | 费用预估 |
| POST | `/aigc/workflow/execute` | 执行工作流 |
| GET | `/aigc/workflow/instance/get` | 实例详情 |
| GET | `/aigc/workflow/instance/page` | 我的工作流实例 |
| POST | `/aigc/workflow/instance/cancel` | 取消执行 |

RPC 接口：

```text
AigcWorkflowApi
  ├── execute(userId, reqDTO)
  ├── getInstance(instanceId)
  ├── retryNode(instanceId, nodeInstanceId)
  ├── cancel(instanceId)
  ├── estimateCost(reqDTO)
  └── handleNodeCallback(nodeInstanceId, taskId, result)
```

### 4.12 服务协作

| 被调用服务 | 用途 |
| ---- | ---- |
| `aigc-model` | 校验节点模型、能力、参数、价格、租户授权 |
| `aigc-billing` | 工作流费用预估、冻结、确认扣费、释放差额 |
| `aigc-task` | 创建主任务、子任务、记录状态和日志 |
| `aigc-gen` | 执行具体生成节点、查询第三方任务状态 |
| `aigc-asset` | 保存节点输出文件、建立资产关系、版本关系 |
| `aigc-safety` | 检查工作流输入、节点结果和最终资产 |

### 4.13 生产级要求

幂等要求：

| 场景 | 幂等键 |
| ---- | ---- |
| 工作流实例创建 | `instance_no` |
| 节点实例创建 | `workflow_instance_id + node_key` |
| 节点执行 | `node_instance_id + execute_version` |
| 节点调用生成 | `node_instance_id + task_id` |
| 计费冻结 | `biz_type + biz_id` |
| 节点资产创建 | `node_instance_id + asset_type` |
| 工作流回调 | `node_instance_id + callback_no` |

补偿任务：

| Job | 说明 |
| ---- | ---- |
| `AigcWorkflowTimeoutJob` | 扫描超时工作流 |
| `AigcWorkflowRetryJob` | 扫描可重试失败节点 |
| `AigcWorkflowStuckNodeJob` | 扫描卡住节点 |
| `AigcWorkflowBillingCompensateJob` | 对账冻结、扣费、释放 |
| `AigcWorkflowAssetCompensateJob` | 补偿节点资产关系 |

并发控制：

- 工作流状态更新必须使用 `id + old_status` 条件更新
- 节点状态更新必须使用 `id + old_status` 条件更新
- 节点执行前必须加分布式锁或数据库状态锁
- 同一个工作流实例不能重复触发相同节点
- 回调处理必须幂等
- 人工确认节点必须防重复提交

可观测性：

- 记录工作流实例日志
- 记录节点执行日志
- 记录节点输入输出摘要
- 记录节点耗时
- 记录节点模型、渠道、费用
- 记录节点失败原因
- 记录第三方任务 ID
- 记录资产 ID
- 记录计费冻结 ID
- 记录用户 ID 和租户 ID

## 5. 模板服务：aigc-template

### 5.1 服务定位

`yudao-module-aigc-template` 是 AIGC 平台的模板服务，负责沉淀可复用的提示词模板、图片模板、视频模板、工作流模板、参数模板和一键同款配置。

第二阶段先做模板基础能力，第三阶段升级为模板市场。

### 5.2 核心职责

第二阶段负责内容：

- 提示词模板
- 图片生成模板
- 视频生成模板
- 音频生成模板
- 文档/PPT 模板
- 工作流模板
- 模板分类
- 模板标签
- 模板参数 Schema
- 模板默认模型
- 模板默认参数
- 模板预览资产
- 模板复制
- 模板启停
- 模板版本
- 用户端模板列表
- 管理端模板运营
- 一键生成
- 一键同款基础能力

不负责内容：

- 不直接生成内容
- 不直接扣费
- 不直接保存生成结果
- 不直接做社区发布
- 不直接做创作者收益
- 不直接管理模型价格

### 5.3 Maven 结构

```text
yudao-module-aigc-template
  ├── yudao-module-aigc-template-api
  └── yudao-module-aigc-template-server
```

### 5.4 命名规范

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-template` |
| API 子模块 | `yudao-module-aigc-template-api` |
| Server 子模块 | `yudao-module-aigc-template-server` |
| Spring 应用名 | `aigc-template-server` |
| 根包名 | `cn.iocoder.yudao.module.aigc.template` |

### 5.5 URL 前缀

| 类型 | 路径 |
| ---- | ---- |
| 管理端 | `/aigc/template/**` |
| 用户端 | `/app-api/aigc/template/**` |
| RPC | `/rpc-api/aigc/template/**` |

### 5.6 API 模块结构

```text
yudao-module-aigc-template-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/template
      ├── api
      │   └── AigcTemplateApi.java
      ├── dto
      │   ├── AigcTemplateRespDTO.java
      │   ├── AigcTemplateUseReqDTO.java
      │   ├── AigcTemplateUseRespDTO.java
      │   ├── AigcTemplateParamSchemaDTO.java
      │   └── AigcTemplateVersionRespDTO.java
      └── enums
          ├── AigcTemplateTypeEnum.java
          ├── AigcTemplateStatusEnum.java
          ├── AigcTemplateVisibilityEnum.java
          ├── AigcTemplateSourceEnum.java
          └── ErrorCodeConstants.java
```

### 5.7 Server 模块结构

```text
yudao-module-aigc-template-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/template
      ├── AigcTemplateServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── template
      │   │   ├── category
      │   │   ├── tag
      │   │   ├── version
      │   │   └── statistics
      │   └── app
      │       ├── template
      │       └── use
      ├── api
      │   └── AigcTemplateApiImpl.java
      ├── service
      │   ├── template
      │   ├── category
      │   ├── tag
      │   ├── version
      │   ├── use
      │   └── statistics
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcTemplateDO.java
      │   │   ├── AigcTemplateCategoryDO.java
      │   │   ├── AigcTemplateTagDO.java
      │   │   ├── AigcTemplateTagRelationDO.java
      │   │   ├── AigcTemplateVersionDO.java
      │   │   └── AigcTemplateUseRecordDO.java
      │   └── mysql
      └── convert
```

### 5.8 数据库设计

| 表名 | 说明 |
| ---- | ---- |
| `aigc_template` | 模板主表 |
| `aigc_template_category` | 模板分类 |
| `aigc_template_tag` | 模板标签 |
| `aigc_template_tag_relation` | 模板标签关系 |
| `aigc_template_version` | 模板版本 |
| `aigc_template_use_record` | 模板使用记录 |

#### 5.8.1 aigc_template

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `name` | varchar | 模板名称 |
| `type` | varchar | 模板类型 |
| `category_id` | bigint | 分类 ID |
| `cover_asset_id` | bigint | 封面资产 |
| `preview_asset_ids` | json | 预览资产 |
| `description` | varchar | 描述 |
| `prompt_template` | text | 提示词模板 |
| `negative_prompt_template` | text | 反向提示词 |
| `param_schema` | json | 参数 Schema |
| `default_params` | json | 默认参数 |
| `model_id` | bigint | 默认模型 |
| `workflow_id` | bigint | 关联工作流 |
| `visibility` | varchar | 可见性 |
| `status` | varchar | 状态 |
| `sort` | int | 排序 |
| `use_count` | int | 使用次数 |
| `favorite_count` | int | 收藏次数 |
| `tenant_id` | bigint | 租户编号 |

### 5.9 模板类型

| 类型 | 说明 |
| ---- | ---- |
| `PROMPT` | 提示词模板 |
| `IMAGE` | 图片模板 |
| `VIDEO` | 视频模板 |
| `AUDIO` | 音频模板 |
| `DOCUMENT` | 文档模板 |
| `PPT` | PPT 模板 |
| `DIGITAL_HUMAN` | 数字人模板 |
| `WORKFLOW` | 工作流模板 |

### 5.10 核心链路

模板生成链路：

```text
用户选择模板
  ↓
填写模板参数
  ↓
aigc-template 渲染 prompt 和参数
  ↓
如果是普通生成模板，调用 aigc-gen
  ↓
如果是工作流模板，调用 aigc-workflow
  ↓
aigc-task 记录任务
  ↓
aigc-billing 冻结和扣费
  ↓
aigc-asset 保存生成结果
```

一键同款链路：

```text
用户选择作品或模板
  ↓
提取模板配置、模型、参数、提示词变量
  ↓
用户替换变量
  ↓
重新生成
  ↓
产出新资产
```

### 5.11 生产级要求

- 模板发布前必须经过审核
- 模板引用的资产必须存在且可访问
- 模板默认模型必须对当前租户可用
- 模板参数必须通过 JSON Schema 校验
- 模板版本发布后不可直接覆盖，需要新版本
- 用户端只能看到已发布、已审核、当前租户可见模板
- 模板使用记录必须落库，用于统计和后续收益分账
- 一键同款不能泄露原作者私有提示词，公开策略需要配置

## 6. 第二阶段已有模块增强方案

### 6.1 aigc-asset 增强

第一阶段资产中心已经管理图片、视频、音频、文档等文件型资产。第二阶段要增强为“内容生产资产库”。

新增能力：

- 资产关系
- 资产版本
- 项目资产
- 中间资产
- 工作流节点资产
- 模板预览资产
- 资产派生关系
- 资产复用记录
- 批量资产管理
- 资产审核补偿
- 资产版权状态
- 资产公开策略

建议新增或完善表：

| 表名 | 说明 |
| ---- | ---- |
| `aigc_asset_relation` | 资产与任务、模板、工作流、社区作品的关系 |
| `aigc_asset_version` | 资产版本 |
| `aigc_asset_project` | 资产项目，可选 |
| `aigc_asset_project_relation` | 项目资产关系，可选 |
| `aigc_asset_usage_log` | 资产使用记录 |

资产关系类型：

| 类型 | 说明 |
| ---- | ---- |
| `TASK_OUTPUT` | 任务输出 |
| `WORKFLOW_INPUT` | 工作流输入 |
| `WORKFLOW_NODE_OUTPUT` | 工作流节点输出 |
| `WORKFLOW_FINAL_OUTPUT` | 工作流最终输出 |
| `TEMPLATE_PREVIEW` | 模板预览 |
| `COMMUNITY_WORK` | 社区作品 |
| `PUBLISH_OUTPUT` | 发布导出产物 |
| `DERIVED_FROM` | 派生自某资产 |

### 6.2 aigc-gen 增强

新增生成能力：

- 图生图
- 图生视频
- 文本转语音
- 语音转文本
- 音乐生成
- 代码生成
- 文档生成
- PPT 生成
- 数字人视频生成
- 批量生成

渠道适配增强：

- 每个渠道独立客户端
- 渠道验签
- 渠道限流
- 渠道熔断
- 渠道失败自动切换
- 渠道异步轮询
- 渠道成本记录
- 渠道响应原文脱敏保存
- 渠道超时配置
- 渠道健康检查

建议拆出垂直 Service：

```text
service
  ├── record
  ├── text
  ├── image
  ├── video
  ├── audio
  ├── document
  ├── ppt
  ├── digitalhuman
  └── provider
```

### 6.3 aigc-task 增强

新增能力：

- 主子任务
- 批量任务
- 工作流任务
- 节点任务
- 任务分组
- 任务进度聚合
- 任务优先级
- 任务取消增强
- 任务重试策略配置
- 任务 SLA 监控
- 任务人工介入

建议新增字段：

| 字段 | 说明 |
| ---- | ---- |
| `parent_task_id` | 父任务 |
| `root_task_id` | 根任务 |
| `biz_source` | 业务来源 |
| `biz_id` | 业务 ID |
| `workflow_instance_id` | 工作流实例 |
| `node_instance_id` | 节点实例 |
| `batch_no` | 批次号 |
| `priority` | 优先级 |
| `progress` | 进度 |
| `estimate_finish_time` | 预计完成时间 |

### 6.4 aigc-billing 增强

新增能力：

- 工作流组合计费
- 节点级成本归集
- 批量任务计费
- 差额释放
- 失败部分扣费
- 租户级套餐
- 用户级限额
- 成本预警
- 毛利看板
- 模型渠道成本对账

新增计费业务类型：

| 类型 | 说明 |
| ---- | ---- |
| `GEN_TEXT` | 文本生成 |
| `GEN_IMAGE` | 图片生成 |
| `GEN_VIDEO` | 视频生成 |
| `GEN_AUDIO` | 音频生成 |
| `GEN_DOCUMENT` | 文档生成 |
| `GEN_PPT` | PPT 生成 |
| `GEN_DIGITAL_HUMAN` | 数字人生成 |
| `WORKFLOW` | 工作流 |
| `TEMPLATE_USE` | 模板使用 |
| `BATCH_GENERATE` | 批量生成 |

### 6.5 aigc-safety 增强

新增能力：

- 资产发布前审核
- 模板发布前审核
- 工作流发布前审核
- 图片审核接入
- 视频审核接入
- 音频审核接入
- 文本大模型审核
- 审核策略配置
- 审核队列
- 审核补偿任务
- 审核统计

建议新增表：

| 表名 | 说明 |
| ---- | ---- |
| `aigc_audit_strategy` | 审核策略 |
| `aigc_audit_queue` | 审核队列 |
| `aigc_audit_operation_log` | 审核操作日志 |

### 6.6 aigc-model 增强

新增能力：

- 渠道健康检查
- 高级路由策略
- 成本优先路由
- 质量优先路由
- 失败切换路由
- 租户限流
- 模型日限额
- 模型并发限制
- 模型 SLA 统计
- 模型能力标签
- 模型推荐配置

高级路由策略：

| 策略 | 说明 |
| ---- | ---- |
| `DEFAULT` | 默认路由 |
| `COST_FIRST` | 成本优先 |
| `QUALITY_FIRST` | 质量优先 |
| `LATENCY_FIRST` | 低延迟优先 |
| `FAILOVER` | 故障切换 |
| `WEIGHT` | 权重路由 |
| `TENANT_CUSTOM` | 租户自定义 |

## 7. 第二阶段验收标准

### 7.1 工作流验收

- 管理端可创建、编辑、发布、下线工作流
- 支持文本、图片、视频、音频、文档、PPT 等节点
- 支持节点依赖、输入输出映射
- 用户端可执行工作流
- 工作流执行前可预估费用
- 工作流执行时可冻结积分
- 节点执行成功后可生成资产
- 节点失败可重试
- 工作流失败可释放未消费冻结
- 工作流成功可确认扣费
- 可查看工作流实例、节点实例、日志
- 支持人工确认节点
- 支持局部重跑
- 支持超时补偿
- 支持多租户隔离

### 7.2 模板验收

- 管理端可创建模板
- 模板支持分类、标签、封面、预览图
- 模板支持参数 Schema
- 模板支持版本
- 模板可发布和下线
- 用户端可查看模板列表
- 用户端可使用模板生成
- 模板可调用 `aigc-gen` 或 `aigc-workflow`
- 模板使用记录完整
- 模板发布前可审核
- 模板引用资产权限正确

### 7.3 生产级技术验收

- 所有新增模块接入根 `pom.xml`
- 所有新增模块采用 `api + server` 结构
- 所有服务注册到 Nacos
- 所有服务接入 Gateway 路由
- 所有服务注册独立 OpenAPI 分组
- 所有 RPC 返回 `CommonResult`
- 所有表包含租户字段和审计字段
- 所有关键状态更新使用条件更新
- 所有跨服务调用具备幂等
- 所有异步链路具备补偿 Job
- 所有核心接口具备单元测试或集成测试
- 所有敏感数据不明文输出到日志
- 所有用户端接口不能暴露成本价、渠道密钥、内部错误码

## 8. 第三阶段总体架构

第三阶段新增核心服务：

```text
yudao-module-aigc-community   社区服务
yudao-module-aigc-publish     发布导出服务
```

第三阶段增强服务：

```text
yudao-module-aigc-template    模板市场化
yudao-module-aigc-safety      社区治理和举报审核
yudao-module-aigc-asset       公共资产和作品资产关系
yudao-module-aigc-billing     模板购买、复用计费、收益预留
```

第三阶段核心调用关系：

```text
用户生成内容
  ↓
aigc-asset 沉淀资产
  ↓
aigc-publish 导出或发布
  ↓
aigc-safety 发布前审核
  ↓
aigc-community 创建社区作品
  ↓
用户点赞 / 收藏 / 评论 / 关注 / 举报
  ↓
aigc-template 支持一键同款和模板市场
  ↓
aigc-billing 支持模板购买、作品复用、收益预留
```

## 9. 社区服务：aigc-community

### 9.1 服务定位

`yudao-module-aigc-community` 是 AIGC 平台的内容社区服务，负责作品发布、作品流、互动、关注、话题、标签、榜单、举报和社区治理。

该模块不负责生成内容，不负责文件存储，不负责模型调用，不负责钱包主账务。

### 9.2 核心职责

负责内容：

- 作品发布
- 作品草稿
- 作品上下架
- 作品详情
- 作品流
- 点赞
- 收藏
- 评论
- 回复
- 关注
- 标签
- 话题
- 榜单
- 浏览记录
- 分享记录
- 举报
- 社区审核状态
- 一键同款入口
- 作品关联模板
- 作品关联资产
- 作品运营推荐
- 管理端社区运营

不负责内容：

- 不直接生成 AIGC 内容
- 不直接保存文件
- 不直接审核底层策略
- 不直接扣费
- 不直接管理模型和渠道
- 不直接管理模板生成逻辑

### 9.3 Maven 结构

```text
yudao-module-aigc-community
  ├── yudao-module-aigc-community-api
  └── yudao-module-aigc-community-server
```

### 9.4 命名规范

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-community` |
| API 子模块 | `yudao-module-aigc-community-api` |
| Server 子模块 | `yudao-module-aigc-community-server` |
| Spring 应用名 | `aigc-community-server` |
| 根包名 | `cn.iocoder.yudao.module.aigc.community` |

### 9.5 URL 前缀

| 类型 | 路径 |
| ---- | ---- |
| 管理端 | `/aigc/community/**` |
| 用户端 | `/app-api/aigc/community/**` |
| RPC | `/rpc-api/aigc/community/**` |

### 9.6 API 模块结构

```text
yudao-module-aigc-community-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/community
      ├── api
      │   └── AigcCommunityApi.java
      ├── dto
      │   ├── AigcCommunityWorkRespDTO.java
      │   ├── AigcCommunityPublishReqDTO.java
      │   ├── AigcCommunityWorkStatusUpdateReqDTO.java
      │   ├── AigcCommunityInteractionRespDTO.java
      │   └── AigcCommunityReportReqDTO.java
      └── enums
          ├── AigcCommunityWorkStatusEnum.java
          ├── AigcCommunityWorkVisibilityEnum.java
          ├── AigcCommunityInteractionTypeEnum.java
          ├── AigcCommunityReportStatusEnum.java
          └── ErrorCodeConstants.java
```

### 9.7 Server 模块结构

```text
yudao-module-aigc-community-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/community
      ├── AigcCommunityServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── work
      │   │   ├── comment
      │   │   ├── topic
      │   │   ├── tag
      │   │   ├── report
      │   │   └── statistics
      │   └── app
      │       ├── work
      │       ├── interaction
      │       ├── comment
      │       ├── follow
      │       ├── topic
      │       └── report
      ├── api
      │   └── AigcCommunityApiImpl.java
      ├── service
      │   ├── work
      │   ├── interaction
      │   ├── comment
      │   ├── follow
      │   ├── topic
      │   ├── tag
      │   ├── report
      │   ├── feed
      │   └── statistics
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcCommunityWorkDO.java
      │   │   ├── AigcCommunityWorkAssetDO.java
      │   │   ├── AigcCommunityInteractionDO.java
      │   │   ├── AigcCommunityCommentDO.java
      │   │   ├── AigcCommunityFollowDO.java
      │   │   ├── AigcCommunityTopicDO.java
      │   │   ├── AigcCommunityTagDO.java
      │   │   ├── AigcCommunityWorkTagDO.java
      │   │   └── AigcCommunityReportDO.java
      │   └── mysql
      ├── job
      │   ├── AigcCommunityStatisticsJob.java
      │   ├── AigcCommunityHotRankJob.java
      │   └── AigcCommunityAuditSyncJob.java
      └── convert
```

### 9.8 数据库设计

| 表名 | 说明 |
| ---- | ---- |
| `aigc_community_work` | 社区作品 |
| `aigc_community_work_asset` | 作品资产关系 |
| `aigc_community_interaction` | 点赞、收藏等互动 |
| `aigc_community_comment` | 评论 |
| `aigc_community_follow` | 关注关系 |
| `aigc_community_topic` | 话题 |
| `aigc_community_tag` | 标签 |
| `aigc_community_work_tag` | 作品标签关系 |
| `aigc_community_report` | 举报 |
| `aigc_community_statistics` | 社区统计，可选 |

#### 9.8.1 aigc_community_work

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `work_no` | varchar | 作品编号 |
| `user_id` | bigint | 作者 |
| `title` | varchar | 标题 |
| `description` | varchar | 描述 |
| `cover_asset_id` | bigint | 封面资产 |
| `work_type` | varchar | 作品类型 |
| `source_type` | varchar | 来源类型 |
| `source_id` | bigint | 来源 ID |
| `template_id` | bigint | 关联模板 |
| `workflow_instance_id` | bigint | 工作流实例 |
| `visibility` | varchar | 可见性 |
| `status` | varchar | 状态 |
| `audit_status` | varchar | 审核状态 |
| `like_count` | int | 点赞数 |
| `favorite_count` | int | 收藏数 |
| `comment_count` | int | 评论数 |
| `view_count` | int | 浏览数 |
| `share_count` | int | 分享数 |
| `copy_count` | int | 同款次数 |
| `hot_score` | decimal | 热度分 |
| `publish_time` | datetime | 发布时间 |
| `tenant_id` | bigint | 租户编号 |

### 9.9 作品状态

```text
DRAFT
  ↓
PENDING_AUDIT
  ↓
PUBLISHED
  ↓
OFFLINE

PENDING_AUDIT
  ↓
AUDIT_REJECTED

PUBLISHED
  ↓
DELETED
```

### 9.10 核心接口

用户端接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/aigc/community/work/publish` | 发布作品 |
| GET | `/aigc/community/work/get` | 作品详情 |
| GET | `/aigc/community/work/feed` | 作品流 |
| GET | `/aigc/community/work/my-page` | 我的作品 |
| POST | `/aigc/community/work/delete` | 删除作品 |
| POST | `/aigc/community/interaction/like` | 点赞 |
| POST | `/aigc/community/interaction/favorite` | 收藏 |
| POST | `/aigc/community/comment/create` | 评论 |
| GET | `/aigc/community/comment/page` | 评论列表 |
| POST | `/aigc/community/follow/create` | 关注 |
| POST | `/aigc/community/report/create` | 举报 |
| POST | `/aigc/community/work/copy-same` | 一键同款 |

管理端接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/aigc/community/work/page` | 作品分页 |
| POST | `/aigc/community/work/recommend` | 推荐作品 |
| POST | `/aigc/community/work/offline` | 下架作品 |
| GET | `/aigc/community/comment/page` | 评论管理 |
| POST | `/aigc/community/comment/delete` | 删除评论 |
| GET | `/aigc/community/report/page` | 举报分页 |
| POST | `/aigc/community/report/handle` | 处理举报 |
| GET | `/aigc/community/statistics/summary` | 社区统计 |

### 9.11 服务协作

| 被调用模块 | 用途 |
| ---- | ---- |
| `aigc-asset` | 校验作品资产、读取封面、公开资产 |
| `aigc-safety` | 发布审核、评论审核、举报审核 |
| `aigc-template` | 一键同款、模板引用 |
| `aigc-gen` | 一键同款触发生成 |
| `aigc-billing` | 模板复用收费、作品复用收费预留 |
| `system-api` | 用户信息、作者信息 |

### 9.12 生产级要求

- 发布作品必须校验资产归属或资产公开权限
- 发布作品必须经过审核
- 私有资产不能直接发布为公开作品，必须走公开授权
- 评论必须支持敏感词审核
- 点赞、收藏必须幂等
- 浏览计数必须防刷，可用 Redis 聚合后异步落库
- 热榜计算不能实时扫全表，需要定时任务
- 举报必须可追踪处理人和处理结果
- 用户删除作品应软删除，不直接删除资产
- 管理员下架作品必须记录原因
- 社区接口不能暴露原始提示词，除非模板明确公开

## 10. 发布导出服务：aigc-publish

### 10.1 服务定位

`yudao-module-aigc-publish` 是 AIGC 平台的发布与导出服务，负责把 AIGC 资产、工作流产物或社区作品导出为可下载、可分享、可分发的最终内容。

该模块主要解决：

- 多资产合成导出
- 视频成片导出
- PPT/文档导出
- 社区发布前整理
- 第三方平台分发预留
- 下载包生成
- 水印、封面、字幕、元信息处理

### 10.2 Maven 结构

```text
yudao-module-aigc-publish
  ├── yudao-module-aigc-publish-api
  └── yudao-module-aigc-publish-server
```

### 10.3 命名规范

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-publish` |
| API 子模块 | `yudao-module-aigc-publish-api` |
| Server 子模块 | `yudao-module-aigc-publish-server` |
| Spring 应用名 | `aigc-publish-server` |
| 根包名 | `cn.iocoder.yudao.module.aigc.publish` |

### 10.4 核心职责

负责内容：

- 发布任务创建
- 导出任务创建
- 多资产打包
- 视频成片导出
- 字幕合成
- 封面生成
- 水印处理
- 文档导出
- PPT 导出
- 下载链接生成
- 发布记录
- 第三方平台分发预留
- 发布结果回调
- 发布失败重试
- 发布统计

不负责内容：

- 不直接生成模型内容
- 不直接管理社区互动
- 不直接保存底层文件
- 不直接处理钱包主账务
- 不直接维护模板

### 10.5 数据库设计

| 表名 | 说明 |
| ---- | ---- |
| `aigc_publish_record` | 发布记录 |
| `aigc_publish_task` | 发布任务 |
| `aigc_publish_channel` | 发布渠道 |
| `aigc_publish_export_file` | 导出文件 |
| `aigc_publish_callback` | 发布回调 |

#### 10.5.1 aigc_publish_record

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `publish_no` | varchar | 发布编号 |
| `user_id` | bigint | 用户 |
| `source_type` | varchar | 来源类型 |
| `source_id` | bigint | 来源 ID |
| `asset_ids` | json | 资产列表 |
| `target_type` | varchar | 发布目标 |
| `status` | varchar | 状态 |
| `output_asset_id` | bigint | 输出资产 |
| `download_url` | varchar | 下载地址 |
| `fail_reason` | varchar | 失败原因 |
| `tenant_id` | bigint | 租户编号 |

### 10.6 发布类型

| 类型 | 说明 |
| ---- | ---- |
| `DOWNLOAD` | 下载 |
| `PACKAGE` | 打包下载 |
| `VIDEO_EXPORT` | 视频成片 |
| `PPT_EXPORT` | PPT 导出 |
| `DOCUMENT_EXPORT` | 文档导出 |
| `COMMUNITY` | 发布到社区 |
| `EXTERNAL_PLATFORM` | 第三方平台预留 |

### 10.7 核心链路

导出下载链路：

```text
用户选择资产
  ↓
aigc-publish 校验资产权限
  ↓
创建发布/导出任务
  ↓
aigc-task 创建异步任务
  ↓
执行合成、打包、水印或格式转换
  ↓
aigc-asset 保存导出文件
  ↓
aigc-task 标记成功
  ↓
返回下载链接
```

发布社区链路：

```text
用户选择资产或工作流结果
  ↓
aigc-publish 整理发布数据
  ↓
aigc-safety 审核发布内容
  ↓
aigc-community 创建作品
  ↓
aigc-asset 建立作品关系
  ↓
返回社区作品 ID
```

### 10.8 生产级要求

- 导出任务必须异步化
- 大文件导出必须支持超时补偿
- 下载链接必须有有效期或权限校验
- 发布前必须做资产归属校验
- 发布到社区必须走审核
- 导出文件必须进入资产中心
- 第三方平台发布必须保存请求和响应摘要
- 发布失败必须可重试
- 发布记录必须可审计

## 11. 第三阶段模板市场增强

第三阶段 `aigc-template` 从“模板管理”升级为“模板市场”。

新增能力：

- 模板公开市场
- 模板收藏
- 模板购买
- 模板评分
- 模板评论
- 模板榜单
- 模板推荐
- 模板一键同款
- 模板复制授权
- 模板收益预留
- 模板作者
- 模板审核
- 模板违规下架

建议新增表：

| 表名 | 说明 |
| ---- | ---- |
| `aigc_template_market` | 模板市场扩展信息 |
| `aigc_template_favorite` | 模板收藏 |
| `aigc_template_order` | 模板购买订单，可选 |
| `aigc_template_rating` | 模板评分 |
| `aigc_template_comment` | 模板评论 |
| `aigc_template_rank` | 模板榜单 |

模板商业化策略：

| 策略 | 说明 |
| ---- | ---- |
| 免费模板 | 所有人可用 |
| 积分模板 | 使用前扣积分 |
| 会员模板 | 会员可用，后续预留 |
| 租户私有模板 | 租户内部可用 |
| 官方模板 | 平台运营维护 |
| 创作者模板 | 第四阶段接创作者收益 |

## 12. 第三阶段审核风控增强

第三阶段审核从“生成链路审核”升级为“社区治理审核”。

新增审核对象：

| 对象 | 说明 |
| ---- | ---- |
| `COMMUNITY_WORK` | 社区作品 |
| `COMMUNITY_COMMENT` | 评论 |
| `COMMUNITY_REPORT` | 举报 |
| `TEMPLATE` | 模板 |
| `PUBLISH_RECORD` | 发布记录 |
| `USER_PROFILE` | 用户资料，预留 |

新增能力：

- 社区作品审核
- 评论审核
- 举报处理
- 模板市场审核
- 批量审核
- 审核队列
- 审核分配
- 审核统计
- 用户违规记录
- 黑名单
- 风险等级策略

生产级要求：

- 审核内容默认只展示摘要
- 审核操作必须记录操作日志
- 审核通过后同步社区作品状态
- 审核拒绝后同步下架或拒绝原因
- 举报成立后可自动下架作品
- 评论命中敏感词可先隐藏再审核
- 高风险用户可限制发布频率

## 13. 第三阶段计费增强

第三阶段计费不只是生成扣费，还要支持模板和社区复用商业化。

新增计费场景：

| 场景 | 说明 |
| ---- | ---- |
| 模板购买 | 用户购买付费模板 |
| 模板使用 | 每次使用模板扣费 |
| 一键同款 | 使用别人作品配置生成 |
| 高清导出 | 导出高清文件扣费 |
| 去水印 | 去除水印扣费 |
| 批量发布 | 批量导出或发布扣费 |
| 创作者收益 | 第四阶段正式建设 |

新增业务类型：

| 类型 | 说明 |
| ---- | ---- |
| `TEMPLATE_BUY` | 模板购买 |
| `TEMPLATE_USE` | 模板使用 |
| `WORK_COPY` | 作品同款 |
| `PUBLISH_EXPORT` | 发布导出 |
| `WATERMARK_REMOVE` | 去水印 |
| `CREATOR_INCOME` | 创作者收入预留 |

生产级要求：

- 模板购买必须幂等
- 一键同款扣费和生成任务必须绑定
- 导出扣费失败不能创建导出任务
- 退款补偿必须关联发布任务或模板订单
- 后续分账不能直接混入用户主钱包，建议单独收益账户

## 14. 第三阶段验收标准

### 14.1 社区服务验收

- 用户可发布作品
- 发布作品必须关联资产
- 发布作品必须经过审核
- 用户可浏览作品流
- 用户可点赞、收藏、评论、关注
- 点赞收藏必须幂等
- 用户可举报作品或评论
- 管理端可审核、下架、推荐作品
- 管理端可处理举报
- 热榜可定时计算
- 作品详情不泄露私有提示词
- 多租户隔离正确

### 14.2 发布导出服务验收

- 用户可选择资产导出
- 导出任务异步执行
- 导出结果进入资产中心
- 用户可下载导出文件
- 下载权限正确
- 发布社区可成功创建作品
- 发布前审核有效
- 发布失败可重试
- 发布记录可追踪
- 大文件任务可超时补偿

### 14.3 模板市场验收

- 用户可浏览模板市场
- 用户可收藏模板
- 用户可使用模板一键生成
- 用户可对公开作品一键同款
- 模板可审核、上架、下架
- 模板使用次数可统计
- 模板可收费，扣费幂等
- 模板详情不泄露非公开配置

### 14.4 安全治理验收

- 社区作品可审核
- 评论可审核
- 举报可处理
- 审核状态可同步到社区
- 审核操作有日志
- 高风险内容不公开展示
- 敏感字段不直接返回用户端

## 15. 服务依赖矩阵

| 调用方 | 被调用方 | 用途 |
| ---- | ---- | ---- |
| `aigc-workflow` | `aigc-gen` | 执行节点生成 |
| `aigc-workflow` | `aigc-task` | 创建主任务、节点任务 |
| `aigc-workflow` | `aigc-billing` | 费用预估、冻结、扣费、释放 |
| `aigc-workflow` | `aigc-model` | 校验模型、计算价格 |
| `aigc-workflow` | `aigc-asset` | 保存节点资产、最终资产 |
| `aigc-workflow` | `aigc-safety` | 输入和结果审核 |
| `aigc-template` | `aigc-gen` | 普通模板生成 |
| `aigc-template` | `aigc-workflow` | 工作流模板执行 |
| `aigc-template` | `aigc-asset` | 模板封面、预览资产 |
| `aigc-template` | `aigc-safety` | 模板发布审核 |
| `aigc-community` | `aigc-asset` | 作品资产校验 |
| `aigc-community` | `aigc-safety` | 作品、评论、举报审核 |
| `aigc-community` | `aigc-template` | 一键同款、模板引用 |
| `aigc-publish` | `aigc-asset` | 导出文件入库 |
| `aigc-publish` | `aigc-task` | 异步导出任务 |
| `aigc-publish` | `aigc-safety` | 发布前审核 |
| `aigc-publish` | `aigc-community` | 发布社区作品 |
| `aigc-billing` | `pay-api` | 付费模板、充值支付 |

## 16. 推荐建设顺序

### 16.1 第二阶段建设顺序

```text
1. 用户端兼容梳理：复用 draw2video-client 现有创作工作台，确定节点、边、项目草稿与后端工作流的数据映射
2. aigc-asset 增强：资产关系、资产版本、工作流资产关系，承接当前本地图片和视频素材缓存
3. aigc-task 增强：主子任务、批量任务、工作流任务字段，承接当前图片和视频节点生成状态
4. aigc-gen 增强：图生图、图生视频、音频、文档、PPT、数字人入口，逐步替换前端直连生成代理
5. aigc-billing 增强：组合计费、差额释放、节点成本
6. aigc-workflow 新建：定义、节点、实例、执行器，承接用户端画布节点编排
7. aigc-template 新建：模板基础、参数 Schema、一键生成，支持工作台节点图保存为模板
8. aigc-safety 增强：模板审核、工作流审核、资产审核补偿
9. 用户端增强：创作工作台接入服务端工作流、资产、任务、模板和成本预估
10. 管理端增强：工作流画布、模板管理、执行监控、成本报表
```

### 16.2 第三阶段建设顺序

```text
1. aigc-community 新建：作品、互动、评论、举报
2. aigc-safety 增强：社区审核、评论审核、举报处理
3. aigc-publish 新建：下载、导出、社区发布
4. aigc-template 增强：模板市场、收藏、评分、榜单
5. aigc-billing 增强：模板购买、一键同款、导出收费
6. aigc-asset 增强：公共资产、作品关系、版权状态
7. 运营后台增强：社区运营、榜单、推荐、违规治理
8. 创作者服务预研：主页、收益、激励、分账
```

## 17. 部署组合

### 17.1 第二阶段部署服务

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
aigc-workflow-server
aigc-template-server
```

如接入真实支付，再增加：

```text
pay-server
```

### 17.2 第三阶段部署服务

```text
yudao-gateway
system-server
infra-server
pay-server
aigc-model-server
aigc-billing-server
aigc-task-server
aigc-asset-server
aigc-safety-server
aigc-gen-server
aigc-workflow-server
aigc-template-server
aigc-community-server
aigc-publish-server
```

第三阶段末或第四阶段增加：

```text
aigc-creator-server
```

## 18. 数据库拆分建议

### 18.1 第二阶段

第二阶段建议继续共用一个业务库，但必须严格按服务表前缀隔离。

| 服务 | 表前缀 |
| ---- | ---- |
| `aigc-workflow` | `aigc_workflow_` |
| `aigc-template` | `aigc_template_` |

### 18.2 第三阶段

第三阶段社区和发布流量上来后，建议准备拆库。

| 服务 | 数据库 |
| ---- | ---- |
| `aigc-model` | `aigc_model_db` |
| `aigc-billing` | `aigc_billing_db` |
| `aigc-task` | `aigc_task_db` |
| `aigc-asset` | `aigc_asset_db` |
| `aigc-gen` | `aigc_gen_db` |
| `aigc-safety` | `aigc_safety_db` |
| `aigc-workflow` | `aigc_workflow_db` |
| `aigc-template` | `aigc_template_db` |
| `aigc-community` | `aigc_community_db` |
| `aigc-publish` | `aigc_publish_db` |

拆库后禁止跨库 JOIN，所有跨模块数据通过 RPC 或冗余快照解决。

## 19. 关键风险与控制方案

### 19.1 工作流复杂度风险

风险：

- 节点状态复杂
- 回调乱序
- 重复执行
- 费用不一致

控制：

- 节点状态机独立建模
- 所有状态更新使用条件更新
- 节点执行幂等
- 节点费用单独记录
- 工作流总账和节点账分开核对

### 19.2 模板泄露风险

风险：

- 用户通过一键同款看到原始提示词
- 付费模板被复制
- 私有模板被公开访问

控制：

- 模板公开字段和内部字段分离
- 用户端不返回完整私有 Prompt
- 一键同款只返回可编辑变量
- 模板权限统一校验
- 付费模板使用前校验订单或权益

### 19.3 社区内容风险

风险：

- 违规作品公开
- 评论违规
- 举报处理不及时

控制：

- 发布前审核
- 评论敏感词过滤
- 举报进入审核队列
- 高风险内容先隐藏
- 审核操作留痕

### 19.4 成本失控风险

风险：

- 工作流多节点导致成本过高
- 用户批量生成恶意消耗
- 渠道失败重复扣费

控制：

- 执行前费用预估
- 先冻结后执行
- 租户和用户限额
- 节点失败不重复扣费
- 渠道成本每日对账
- 毛利低于阈值告警

### 19.5 资产权限风险

风险：

- 私有资产被社区公开
- 导出链接被盗用
- 第三方 URL 过期

控制：

- 发布前校验资产归属
- 公开资产必须审核通过
- 导出链接带有效期
- 第三方文件必须转存平台文件服务
- 下载接口做权限校验

## 20. 最终建议

第二阶段建议定调为：

```text
第二阶段 = 工作流 + 模板 + 资产版本 + 批量生产 + 高级计费
```

第三阶段建议定调为：

```text
第三阶段 = 社区 + 发布导出 + 模板市场 + 内容治理 + 增长运营
```

最终模块演进路线：

```text
第一阶段：model + billing + task + asset + gen + safety，完成最小赚钱闭环
第二阶段：workflow + template，完成复杂内容生产和复用
第三阶段：community + publish，完成内容分发、社区增长和模板市场
第四阶段：creator，完成创作者生态、收益分账和平台激励
```

核心边界原则：

```text
模型归 model
钱归 billing
任务归 task
文件归 asset
生成归 gen
审核归 safety
编排归 workflow
模板归 template
社区归 community
发布归 publish
创作者归 creator
```

生产级建设的关键不是模块数量，而是每个模块边界清晰、状态幂等、账务可对账、失败可补偿、内容可审核、资产可追踪。
