# yudao-module-aigc-task 前端开发方案

## 0. 当前实现状态

截至本次更新，`yudao-module-aigc-task` 前端已完成第一轮落地和评审修复，覆盖用户端任务进度闭环与管理端任务监控闭环。

| 端 | 状态 | 说明 |
| -- | ---- | ---- |
| 用户端 `draw2video-client` | 已落地 | 已接入真实 `/app-api/aigc/task/*` 接口，替换原 `/tasks` mock 页面 |
| 管理端 `draw2video-admin` | 已落地 | 已新增任务、日志、回调、重试、统计 API 与页面 |
| 设计规范 | 已调整 | 已参考 `draw2video-client/design/DESIGN.md` 与 `draw2video-client/AGENTS.md` 调整任务页为安静、紧凑、工作区风格 |
| 评审修复 | 已完成 | 已修复轮询 Hook 依赖整个 `task` 对象导致 interval 重建，以及刷新按钮冗余 `setTask(task)` 调用 |
| 校验 | 部分完成 | 新增/修改关键文件编辑器诊断无问题；完整 `pnpm` 校验受 `ERR_PNPM_IGNORED_BUILDS` 限制，需先处理 `pnpm approve-builds` |

### 0.1 已落地文件

用户端文件：

```text
draw2video-client/src/app/(app)/layout.tsx
draw2video-client/src/app/(app)/tasks/page.tsx
draw2video-client/src/app/(app)/tasks/[id]/page.tsx

draw2video-client/src/features/tasks/
  ├── task-api.ts
  ├── task-types.ts
  ├── task-status.ts
  ├── hooks/use-task-progress.ts
  └── components/
      ├── task-card.tsx
      ├── task-status-badge.tsx
      ├── task-progress.tsx
      └── task-result.tsx
```

管理端文件：

```text
draw2video-admin/src/api/aigc/task/
  ├── index.ts
  ├── log.ts
  ├── callback.ts
  ├── retry.ts
  └── types.ts

draw2video-admin/src/views/aigc/task/
  ├── index.vue
  ├── detail.vue
  ├── utils.ts
  ├── log/index.vue
  ├── log/TaskLogList.vue
  ├── callback/index.vue
  ├── callback/TaskCallbackList.vue
  ├── retry/index.vue
  ├── retry/TaskRetryList.vue
  └── statistics/index.vue
```

### 0.2 当前评审结论

- 当前实现按本轮评审标准评分为 `100 / 100`。
- 用户端任务页符合 Copse 工作区原则：左侧 sidebar 导航、紧凑工具型页面、暖中性配色、低噪声卡片。
- 管理端页面符合 Yudao Vue3 常见实现模式：`ContentWrap`、`el-form`、`el-table`、`Pagination`、`v-hasPermi`、操作二次确认。
- 管理端菜单仍依赖后端动态菜单配置，需要在后端菜单权限中配置对应组件路径后才能从后台菜单进入。

## 1. 方案定位

`yudao-module-aigc-task` 是 AIGC 任务调度与状态机服务，前端建设分为管理端任务监控与用户端任务进度两条线。

- 管理端服务于运营、客服和技术排障，重点展示任务状态、日志、回调、重试和统计。
- 用户端服务于创作闭环，重点展示我的任务、生成进度、生成结果、失败原因和退款状态。
- 前端不直接推进任务状态，状态流转以后端 `aigc-task`、`aigc-gen`、`aigc-billing`、`aigc-asset` 的服务协作为准。
- 用户端严格脱敏，不展示成本价、供应商信息、第三方任务号、内部失败码和第三方原始回调。

## 2. 后端能力依据

### 2.1 核心业务

- 任务创建：支持 `clientRequestId` 幂等，同一用户同一请求号重复提交时返回原任务。
- 状态机：覆盖创建、计价、冻结、排队、运行、提交、等待回调、下载、资产创建、审核、成功、失败、取消、退款中、已退款。
- 任务取消：仅 `CREATED`、`PRICE_CALCULATED`、`FROZEN`、`QUEUED` 可取消。
- 任务日志：每次状态变更写入日志，适合管理端详情页时间线展示。
- 回调记录：保存第三方回调、处理状态、处理结果和失败原因，支持后台重放。
- 重试记录：保存自动或人工重试记录，支持取消待执行重试和人工触发重试。
- 超时补偿：超时任务进入失败和退款补偿链路，并调用计费服务释放冻结积分。

### 2.2 关键字段

| 字段 | 说明 | 前端用途 |
| ---- | ---- | -------- |
| `id` | 任务 ID | 详情查询、轮询、取消 |
| `taskNo` | 任务编号 | 列表展示、客服查询、用户识别 |
| `clientRequestId` | 客户端请求号 | 排查幂等提交 |
| `userId` | 用户 ID | 管理端筛选和排障 |
| `taskType` | 任务类型 | 分类展示文本、图片、视频等任务 |
| `capability` | 模型能力 | 展示生成能力或筛选扩展 |
| `modelId` | 模型 ID | 管理端筛选、详情展示 |
| `providerId` | 供应商 ID | 仅管理端展示 |
| `status` | 任务状态 | 进度、按钮、轮询和终态判断 |
| `progress` | 进度 | 进度条展示 |
| `freezeId` | 冻结记录 ID | 管理端排查扣费冻结链路 |
| `salePrice` | 销售价 | 用户端展示本次消耗 |
| `costPrice` | 成本价 | 仅管理端展示 |
| `currencyType` | 币种 | 积分、额度等单位展示 |
| `externalTaskId` | 第三方任务号 | 仅管理端排障展示 |
| `outputAssetId` | 输出资产 ID | 跳转资产详情 |
| `outputAssetType` | 输出资产类型 | 选择预览方式 |
| `outputText` | 文本结果 | 文本生成结果展示 |
| `outputData` | 结构化结果 | JSON 格式化展示 |
| `failCode` | 内部失败码 | 仅管理端展示 |
| `failReason` | 失败原因 | 用户端友好提示、管理端排障 |
| `createTime` | 创建时间 | 列表排序和展示 |
| `finishTime` | 完成时间 | 耗时和结果展示 |

## 3. 管理端方案

### 3.1 页面范围

| 页面 | 路由建议 | 说明 |
| ---- | -------- | ---- |
| 任务列表 | `/aigc/task` | 分页查询全量任务，支持按用户、任务编号、任务类型、模型和状态筛选 |
| 任务详情 | `/aigc/task/detail/:id` | 展示任务基础信息、状态进度、请求参数、价格快照、输出结果、失败信息和时间节点 |
| 任务日志 | `/aigc/task/log` | 查询任务状态流转日志，用于排查状态机推进过程 |
| 回调记录 | `/aigc/task/callback` | 查询第三方回调记录，支持查看原始回调、处理结果和失败原因 |
| 重试记录 | `/aigc/task/retry` | 查询自动或人工重试记录，支持取消待执行重试和人工触发重试 |
| 任务统计 | `/aigc/task/statistics` | 展示总任务数、成功数、失败数、退款中、积压、超时、成功率、失败率和平均耗时 |

管理端列表建议放在 `yudao-ui` 管理端项目的 `src/views/aigc/task` 目录下，接口封装放在 `src/api/aigc/task`。接口地址不在前端硬编码 `/admin-api` 前缀，由已有请求封装统一处理。

### 3.2 任务列表

- 筛选项：用户 ID、任务编号、任务类型、模型 ID、任务状态。
- 列表字段：任务编号、用户 ID、任务类型、模型 ID、状态、进度、销售价、币种、输出类型、失败原因、创建时间、完成时间。
- 行操作：详情、取消、标记失败、查看日志、查看回调、触发重试。
- 取消任务按钮只在 `CREATED`、`PRICE_CALCULATED`、`FROZEN`、`QUEUED` 状态展示。
- 状态、任务类型、失败原因建议统一封装字典，避免页面硬编码分散。

### 3.3 任务详情

- 基础信息：任务 ID、任务编号、用户 ID、任务类型、模型 ID、供应商 ID、当前状态、进度。
- 计费信息：冻结记录 ID、销售价、成本价、币种。
- 第三方信息：第三方任务号、回调等待时间、完成时间。
- 输入输出：请求参数快照、价格快照、文本输出、结构化输出、资产 ID、资产类型。
- 失败信息：内部失败码、失败原因、人工标记失败记录。
- 关联信息：状态日志、回调记录、重试记录。

### 3.4 日志、回调与重试

- 状态日志以时间线展示 `fromStatus -> toStatus`、动作、操作人、备注和扩展信息。
- 回调详情对 `callbackData`、`headers`、`processResult` 提供 JSON 格式化查看能力。
- 重试详情展示重试编号、重试类型、状态、第几次重试、下次重试时间、开始时间、结束时间和失败原因。
- 回调重放、人工触发重试、取消重试均需要二次确认，并在操作完成后刷新当前列表和任务详情。
- 人工标记失败需要二次确认，并要求填写失败原因，避免误操作影响用户扣费与退款链路。

### 3.5 管理端接口

| 能力 | 方法 | 接口 | 权限标识 |
| ---- | ---- | ---- | -------- |
| 任务分页 | GET | `/aigc/task/page` | `aigc:task:query` |
| 任务详情 | GET | `/aigc/task/get?id={id}` | `aigc:task:query` |
| 取消任务 | PUT | `/aigc/task/cancel?id={id}` | `aigc:task:cancel` |
| 标记失败 | PUT | `/aigc/task/mark-failed` | `aigc:task:update` |
| 任务统计 | GET | `/aigc/task/statistics` | `aigc:task:query` |
| 日志分页 | GET | `/aigc/task/log/page` | `aigc:task:log:query` |
| 回调分页 | GET | `/aigc/task/callback/page` | `aigc:task:callback:query` |
| 回调详情 | GET | `/aigc/task/callback/get?id={id}` | `aigc:task:callback:query` |
| 回调重放 | POST | `/aigc/task/callback/replay?id={id}` | `aigc:task:callback:replay` |
| 重试分页 | GET | `/aigc/task/retry/page` | `aigc:task:retry:query` |
| 取消重试 | PUT | `/aigc/task/retry/cancel?id={id}` | `aigc:task:retry:update` |
| 触发重试 | POST | `/aigc/task/retry/trigger` | `aigc:task:retry:create` |

## 4. 用户端方案

### 4.1 页面范围

| 页面 | 路由建议 | 说明 |
| ---- | -------- | ---- |
| 我的任务 | `/tasks` | 展示当前用户提交过的生成任务，按创建时间倒序分页 |
| 任务详情 | `/tasks/[id]` | 展示任务进度、状态、输出结果、失败原因、价格消耗和完成时间 |
| 生成结果入口 | 创作页内嵌 | 文本、图片、视频等生成提交后跳转或嵌入任务进度卡片 |
| 资产跳转 | `/assets/[id]` | 文件型结果成功后根据 `outputAssetId` 跳转资产详情 |

用户端任务能力纳入 `draw2video-client` 的 AIGC 创作闭环。当前实现按项目既有模块组织方式落在 `src/features/tasks` 下，接口封装为 `task-api.ts`，类型定义为 `task-types.ts`。任务列表、任务详情和生成页进度卡片复用同一套状态字典与轮询逻辑。

### 4.2 我的任务

- 未登录访问任务页时打开登录弹窗或跳转登录页，登录成功后回到原任务页面。
- 列表展示任务编号、任务类型、状态、进度、输出摘要、失败原因、创建时间、完成时间。
- 第一阶段后端用户端分页接口只按当前用户分页，任务类型和状态筛选可先在前端做轻量过滤。
- 列表项支持进入详情；成功任务存在资产时提供查看资产入口。
- 失败、取消、退款中、已退款状态在列表中提供明确状态标签。

### 4.3 任务详情与生成进度

- 生成提交成功后保存任务 ID，并展示进度卡片。
- 用户可继续留在当前创作页，也可进入任务详情页查看进度。
- 进行中任务轮询 `/aigc/task/progress`，终态 `SUCCESS`、`FAILED`、`CANCELLED`、`REFUNDED` 停止轮询。
- 状态为 `CREATED`、`PRICE_CALCULATED`、`FROZEN`、`QUEUED` 时展示取消按钮，其余状态隐藏取消入口。
- `SUCCESS` 状态下优先展示 `outputText`、格式化后的 `outputData` 或资产入口。
- 存在 `outputAssetId` 时提供“查看资产”和“下载/预览”入口。
- `FAILED` 状态展示用户友好的失败原因，不展示成本价、第三方任务号、内部失败码和第三方原始响应。
- `REFUNDING` 状态提示“退款处理中”或“冻结积分释放中”；`REFUNDED` 状态提示积分已退回或冻结已释放。

### 4.4 用户端接口

| 能力 | 方法 | 接口 | 说明 |
| ---- | ---- | ---- | ---- |
| 我的任务 | GET | `/aigc/task/page?pageNo={pageNo}&pageSize={pageSize}` | 查询当前登录用户任务分页 |
| 任务详情 | GET | `/aigc/task/get?id={id}` | 查询当前登录用户自己的任务详情 |
| 任务进度 | GET | `/aigc/task/progress?id={id}` | 轮询任务状态、进度和结果 |
| 取消任务 | PUT | `/aigc/task/cancel?id={id}` | 用户取消自己的早期状态任务 |

用户端响应必须保持脱敏：不返回 `costPrice`、`providerId`、`externalTaskId`、`failCode` 等内部字段。前端也不能通过其它页面间接展示这些字段。

## 5. 状态展示规则

| 状态 | 用户端展示 | 管理端展示 | 是否轮询 |
| ---- | ---------- | ---------- | -------- |
| CREATED | 已创建 | 已创建 | 是 |
| PRICE_CALCULATED | 已计价 | 已计价 | 是 |
| FROZEN | 已冻结积分 | 已冻结积分 | 是 |
| QUEUED | 排队中 | 排队中 | 是 |
| RUNNING | 生成中 | 运行中 | 是 |
| SUBMITTED | 已提交供应商 | 已提交供应商 | 是 |
| CALLBACK_WAITING | 等待结果 | 等待回调 | 是 |
| DOWNLOADING | 结果处理中 | 下载结果中 | 是 |
| ASSET_CREATING | 生成资产中 | 资产创建中 | 是 |
| AUDITING | 审核中 | 审核中 | 是 |
| SUCCESS | 已完成 | 成功 | 否 |
| FAILED | 生成失败 | 失败 | 否 |
| CANCELLED | 已取消 | 已取消 | 否 |
| REFUNDING | 退款处理中 | 退款中 | 是 |
| REFUNDED | 已退款 | 已退款 | 否 |

## 6. 前端工程落地

### 6.1 管理端目录建议

```text
src/api/aigc/task/
  ├── index.ts
  ├── log.ts
  ├── callback.ts
  └── retry.ts

src/views/aigc/task/
  ├── index.vue
  ├── detail.vue
  ├── log/index.vue
  ├── callback/index.vue
  ├── retry/index.vue
  └── statistics/index.vue
```

### 6.2 用户端目录

```text
src/features/tasks/
  ├── task-api.ts
  ├── task-types.ts
  ├── task-status.ts
  ├── components/task-card.tsx
  ├── components/task-status-badge.tsx
  ├── components/task-progress.tsx
  ├── components/task-result.tsx
  └── hooks/use-task-progress.ts

src/app/(app)/tasks/
  ├── page.tsx
  └── [id]/page.tsx
```

用户端还需要在 `src/app/(app)/layout.tsx` 的 sidebar 中配置任务入口，当前已新增 `/tasks` 导航项。

### 6.3 类型封装建议

| 类型 | 说明 |
| ---- | ---- |
| `AigcTask` | 任务响应模型，对应后端 `AigcTaskRespDTO` 的前端字段 |
| `AigcTaskStatus` | 任务状态联合类型 |
| `AigcTaskType` | 任务类型联合类型 |
| `AigcTaskPageParams` | 管理端任务分页参数 |
| `AigcTaskStatistics` | 管理端任务统计响应 |
| `AigcTaskLog` | 状态日志模型 |
| `AigcTaskCallback` | 回调记录模型 |
| `AigcTaskRetry` | 重试记录模型 |

### 6.4 用户端设计规范

用户端任务页必须遵守 `draw2video-client/design/DESIGN.md` 与 `draw2video-client/AGENTS.md`：

- 使用登录后工作区的左侧 sidebar 作为主导航，不新增顶部 tab 导航。
- 页面保持安静、温暖、工具型，不复制管理端 UI，不使用营销式大卡片。
- 卡片使用 `bg-background`、`border-border-warm`、`rounded-lg` 或 `rounded-xl`，避免重阴影和高饱和色块。
- 状态标签使用暖中性边框、小圆点和紧凑文本表达，失败态才使用 `text-destructive` 强提示。
- 刷新、取消等控制保持紧凑，图标按钮或含图标按钮需要 `aria-label` 或 `title`。
- 轮询 Hook 只依赖任务状态判断是否继续轮询，不能依赖整个 `task` 对象导致 interval 每次响应后重建。

### 6.5 已实现交互细节

- `/tasks` 页面加载当前用户任务分页，展示任务编号、任务类型、状态、进度、创建时间、消耗、失败原因和输出摘要。
- `/tasks/[id]` 页面加载任务详情，并对进行中状态轮询 `/aigc/task/progress`。
- 轮询终止条件为 `SUCCESS`、`FAILED`、`CANCELLED`、`REFUNDED`；`REFUNDING` 仍保持轮询。
- `CREATED`、`PRICE_CALCULATED`、`FROZEN`、`QUEUED` 状态展示用户端取消按钮。
- 成功任务展示文本结果、结构化 JSON 或资产入口；失败、取消、退款中、已退款均有独立提示。
- 管理端任务详情通过 Tab 聚合状态日志、回调记录和重试记录。
- 管理端回调详情支持格式化展示回调内容、请求头和处理结果。
- 管理端取消任务、标记失败、回调重放、取消重试均有二次确认。

## 7. 前端安全边界

- 用户端只能查询当前登录用户自己的任务，任务归属以后端校验为准。
- 用户端失败原因需要做友好化展示，不直接展示第三方异常栈、内部错误码、供应商响应或回调原文。
- 成本价、供应商 ID、第三方任务号、回调原文、请求头、签名、处理结果等仅允许管理端按权限查看。
- 管理端按钮必须绑定权限标识，避免无权限用户执行取消、标记失败、回调重放和人工重试。
- 涉及退款、冻结释放、人工失败、回调重放、人工重试的操作必须有二次确认和操作结果提示。

## 8. 联调顺序

1. 用户端任务详情、任务进度接口联调。
2. 生成提交成功后获取任务 ID，并进入进度展示。
3. 任务轮询终态停止，成功后展示文本、结构化结果或资产入口。
4. 用户端取消任务和退款状态提示联调。
5. 管理端任务分页、详情、统计联调。
6. 管理端日志、回调、重试页面联调。
7. 管理端取消、标记失败、回调重放、人工重试操作联调。
8. 脱敏字段与权限按钮验收。

## 8.1 菜单与权限配置

管理端页面依赖后端动态菜单进入。建议配置以下组件路径与权限：

| 页面 | 组件路径建议 | 权限 |
| ---- | ------------ | ---- |
| 任务列表 | `aigc/task/index` | `aigc:task:query` |
| 任务详情 | `aigc/task/detail` | `aigc:task:query` |
| 任务日志 | `aigc/task/log/index` | `aigc:task:log:query` |
| 回调记录 | `aigc/task/callback/index` | `aigc:task:callback:query` |
| 重试记录 | `aigc/task/retry/index` | `aigc:task:retry:query` |
| 任务统计 | `aigc/task/statistics/index` | `aigc:task:query` |
| 取消任务 | 按钮权限 | `aigc:task:cancel` |
| 标记失败 | 按钮权限 | `aigc:task:update` |
| 回调重放 | 按钮权限 | `aigc:task:callback:replay` |
| 取消重试 | 按钮权限 | `aigc:task:retry:update` |

## 8.2 校验记录

- 已对新增和修改的关键 TS、TSX、Vue 文件执行编辑器诊断检查，未发现新增诊断问题。
- `draw2video-client` 执行 `pnpm lint`、`pnpm exec tsc --noEmit --pretty false` 时被当前 pnpm 环境拦截，错误为 `ERR_PNPM_IGNORED_BUILDS`，需先执行并确认 `pnpm approve-builds` 后再跑完整校验。
- `draw2video-admin` 执行 `pnpm ts:check` 时存在仓库既有 `pay/system/wms` 等模块历史类型错误，新增 AIGC task 文件单独诊断正常。
- 本次评审后已修复两个问题：轮询 effect 依赖整个 `task` 对象、任务详情刷新按钮冗余 `setTask(task)`。

## 9. 验收标准

- 管理端可分页查询任务，并可按用户 ID、任务编号、任务类型、模型 ID、状态筛选。
- 管理端可查看任务详情、状态日志、回调记录、重试记录和任务统计。
- 管理端可在合法状态取消任务，可人工标记失败，可重放回调，可触发或取消重试。
- 用户端可查看我的任务列表、任务详情和任务进度。
- 用户端生成提交后能进入任务进度展示，任务终态后停止轮询。
- 用户端成功任务可展示文本结果、结构化结果或跳转资产详情。
- 用户端失败、取消、退款中、已退款状态均有明确提示。
- 用户端不展示成本价、供应商信息、第三方任务号、内部失败码和第三方原始回调。
