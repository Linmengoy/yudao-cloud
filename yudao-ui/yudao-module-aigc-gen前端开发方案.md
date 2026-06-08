# yudao-module-aigc-gen 前端开发方案

本文档基于 `c:\use\code\project\manman\yudao-ui\AIGC平台第一阶段用户端前端开发计划.md`、`c:\use\code\project\manman\yudao-ui\draw2video-client\AGENTS.md`、`c:\use\code\project\manman\yudao-ui\draw2video-client\design\DESIGN.md`、`c:\use\code\project\manman\yudao-module-aigc-gen技术方案.md` 和后端模块 `c:\use\code\project\manman\yudao-module-aigc-gen` 梳理，用于指导 `yudao-module-aigc-gen` 对应的管理端与用户端前端开发。

## 1. 模块定位

`yudao-module-aigc-gen` 是 AIGC 平台的统一生成执行服务，负责承接用户侧文本、图片、视频等生成请求，并完成模型调用适配、生成任务提交、第三方任务状态同步、回调处理、结果解析和结果回填。

前端侧分为两类使用场景：

- 管理端：生成记录、生成详情、同步第三方任务、生成回调记录、渠道调用日志和运维排障。
- 用户端：创作画布内提交生成、展示生成进度、查询生成结果，并将结果写回文本、图片、视频节点。

第一阶段目标：

```text
用户端：模型选择 → 参数填写 → 价格预估 → 生成提交 → 任务进度 → 结果写回画布/资产
管理端：生成记录 → 生成详情 → 同步补偿 → 回调记录 → 渠道日志 → 运维排障
```

模块边界：

| 模块 | 前端职责 | 说明 |
| --- | --- | --- |
| `aigc-model` | 模型列表、参数模板、价格预估、渠道商配置 | 负责“用什么模型、多少钱、走哪个渠道” |
| `aigc-gen` | 提交生成、结果查询、记录查询、同步、回调和渠道日志 | 负责“生成执行与渠道适配” |
| `aigc-task` | 任务列表、进度、状态机、取消、重试、日志 | 负责“任务生命周期事实源” |
| `aigc-billing` | 钱包余额、冻结、扣费、退款提示 | 负责“钱的变化” |
| `aigc-asset` | 图片、视频、文本结果资产化和下载 | 负责“结果沉淀” |
| `aigc-safety` | 提示词检查、生成后审核、人工审核 | 负责“安全合规” |

## 2. 前端项目范围

| 项目 | 路径 | 技术栈 | 建设目标 |
| --- | --- | --- | --- |
| `draw2video-admin` | `c:\use\code\project\manman\yudao-ui\draw2video-admin` | Vue3 + Vite + TypeScript + Element Plus | 生成记录、回调记录、渠道日志、同步补偿 |
| `draw2video-client` | `c:\use\code\project\manman\yudao-ui\draw2video-client` | Next.js App Router + React + TypeScript + Tailwind CSS | 用户创作端生成提交、任务结果查询、画布结果写回 |

## 3. 后端接口依据

### 3.1 用户端接口

用户端接口来自后端 `AigcGenerateAppController`，前端调用时统一带 `/app-api` 前缀。

| 功能 | 方法 | 接口 | 说明 |
| --- | --- | --- | --- |
| 通用生成提交 | POST | `/app-api/aigc/gen/submit` | 根据 `generateType`、`generateMode` 提交生成 |
| 文本生成 | POST | `/app-api/aigc/gen/text/generate` | 后端自动设置 `TEXT`、`TEXT_GENERATE` |
| 文生图 | POST | `/app-api/aigc/gen/image/text-to-image` | 后端自动设置 `IMAGE`、`TEXT_TO_IMAGE` |
| 文生视频 | POST | `/app-api/aigc/gen/video/text-to-video` | 后端自动设置 `VIDEO`、`TEXT_TO_VIDEO` |
| 获取生成结果 | GET | `/app-api/aigc/gen/result?taskId=` | 按任务 ID 获取生成结果 |

### 3.2 管理端接口

管理端接口前端不写 `/admin-api`，只写业务路径。

| 功能 | 方法 | 接口 | 权限标识 |
| --- | --- | --- | --- |
| 生成记录分页 | GET | `/aigc/gen/record/page` | `aigc:gen:query` |
| 生成记录详情 | GET | `/aigc/gen/record/get?id=` | `aigc:gen:query` |
| 同步第三方任务 | POST | `/aigc/gen/record/sync?taskId=` | `aigc:gen:update` |
| 生成回调分页 | GET | `/aigc/gen/callback/page` | `aigc:gen:query` |
| 渠道调用日志分页 | GET | `/aigc/gen/provider-log/page` | `aigc:gen:query` |

管理端生成记录接口必须返回管理端专用响应对象，不能复用用户端结果查询 DTO。

```text
yudao-module-aigc-gen-server/src/main/java/cn/iocoder/yudao/module/aigc/gen/controller/admin/record/vo/AigcGenerateRecordRespVO.java
```

原因：用户端 `AigcGenerateResultRespDTO` 只覆盖结果查询字段，不包含管理端列表和详情所需的用户、模型、渠道、计费、提交时间、回调时间和内部失败原因等排障字段。管理端 `/get` 与 `/page` 应返回 `AigcGenerateRecordRespVO`，确保前端生成记录页面字段完整。

## 4. 核心数据模型

### 4.1 生成提交请求

| 字段 | 类型 | 前端来源 | 说明 |
| --- | --- | --- | --- |
| `clientRequestId` | string | 前端生成 | 幂等请求号，建议每次提交生成 UUID |
| `generateType` | string | 生成入口 | `TEXT`、`IMAGE`、`VIDEO` |
| `generateMode` | string | 生成入口 | `TEXT_GENERATE`、`TEXT_TO_IMAGE`、`TEXT_TO_VIDEO` |
| `modelId` | number | 模型选择 | 来自 `aigc-model` 用户端模型列表 |
| `providerId` | number | 可选 | 用户端一般不展示，除非后端允许指定 |
| `prompt` | string | Prompt 输入框 | 用户提示词 |
| `inputParams` | string | 动态参数表单 | JSON 字符串，来自参数模板渲染结果 |
| `sync` | boolean | 可选 | 第一阶段建议默认异步 |
| `priceAmount` | number | 价格预估结果 | 用于提交前确认，最终以后端计费为准 |

用户端要求：

- `userId` 由后端从登录态写入，前端不传用户 ID。
- `providerId` 不建议在用户端暴露，除非产品明确需要“指定渠道”。
- `priceAmount` 只作为前端确认快照，真实扣费以后端为准。
- `inputParams` 必须由动态参数模板统一序列化，避免每个页面散落拼 JSON。

### 4.2 生成提交响应

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| `id` | number | 生成记录 ID |
| `taskId` | number | 后续轮询任务进度、结果查询 |
| `generateNo` | string | 用户可见流水号、客服排查 |
| `status` | string | 初始状态展示 |

### 4.3 生成结果

| 字段 | 前端用途 |
| --- | --- |
| `id`、`taskId`、`generateNo` | 详情展示和排查 |
| `generateType`、`generateMode` | 决定结果渲染组件 |
| `status` | 生成状态 |
| `outputText` | 文本结果 |
| `outputData` | 结构化结果，JSON 格式化展示 |
| `outputUrls` | 图片、视频、音频等文件 URL 列表 |
| `assetIds` | 关联资产 ID，用于跳转资产详情 |
| `failMessage` | 用户友好失败提示 |
| `createTime`、`finishTime` | 耗时和时间展示 |

### 4.4 生成记录

管理端重点展示：

- 用户与任务：`userId`、`taskId`、`clientRequestId`
- 生成标识：`generateNo`、`generateType`、`generateMode`
- 模型渠道：`modelId`、`modelCode`、`providerId`、`providerCode`
- 第三方任务：`providerTaskId`、`providerStatus`
- 输入输出：`prompt`、`inputParams`、`outputText`、`outputData`、`outputUrls`、`assetIds`
- 计费快照：`freezeId`、`priceAmount`、`costAmount`
- 时间节点：`submitTime`、`callbackTime`、`finishTime`
- 失败信息：`failReason`、`failMessage`

管理端响应 VO 要求：

```text
AigcGenerateRecordRespVO
  ├── id
  ├── taskId
  ├── userId
  ├── generateNo
  ├── clientRequestId
  ├── generateType
  ├── generateMode
  ├── modelId
  ├── modelCode
  ├── providerId
  ├── providerCode
  ├── providerTaskId
  ├── providerStatus
  ├── status
  ├── prompt
  ├── inputParams
  ├── outputText
  ├── outputData
  ├── outputUrls
  ├── assetIds
  ├── freezeId
  ├── priceAmount
  ├── costAmount
  ├── submitTime
  ├── callbackTime
  ├── finishTime
  ├── failReason
  ├── failMessage
  └── createTime
```

实现要求：

- 管理端生成记录 `/get` 返回 `CommonResult<AigcGenerateRecordRespVO>`。
- 管理端生成记录 `/page` 返回 `CommonResult<PageResult<AigcGenerateRecordRespVO>>`。
- `AigcGenerateRecordRespVO` 与 `AigcGenerateRecordDO` 字段名保持一致，便于 `BeanUtils.toBean` 转换。
- 用户端 `/app-api/aigc/gen/result?taskId=` 继续返回 `AigcGenerateResultRespDTO`，避免向用户端泄露成本价、渠道编码、内部失败原因等管理端字段。

## 5. 用户端开发方案

### 5.1 用户端产品原则

用户端不是 Yudao 管理后台的复制品，而是轻量创作工作区：

- 登录后以左侧 workspace sidebar 作为主导航。
- `/create/image` 是核心创作画布。
- 图像、视频生成都应尽量在画布节点内完成。
- 生成结果应写回当前节点，而不是跳出到割裂的后台式页面。
- 不在前端暴露 provider key、模型密钥、第三方任务内部细节。

### 5.2 用户端页面范围

| 页面/区域 | 路由建议 | 说明 |
| --- | --- | --- |
| 创作画布 | `/create/image` | 图片、视频、文本生成主入口 |
| 任务列表 | `/tasks` | 复用 `aigc-task` 已落地页面，展示我的任务 |
| 任务详情 | `/tasks/[id]` | 复用任务详情，展示进度和结果 |
| 资产库 | `/assets` | 展示生成资产，按图片、视频分类 |
| 项目库 | `/projects` | 打开项目并回到创作画布 |
| 钱包入口 | sidebar 底部 | 展示余额、扣费和冻结提示 |

### 5.3 用户端目录规划

建议新增或完善：

```text
draw2video-client/src/features/generation
  ├── generation-api.ts
  ├── generation-types.ts
  ├── generation-status.ts
  ├── generation-client-request.ts
  ├── generation-result.ts
  └── hooks
      ├── use-generation-submit.ts
      └── use-generation-result.ts
```

与既有模块协作：

```text
draw2video-client/src/features/image-generation
  ├── 模型参数选择
  ├── 尺寸、比例、风格等参数
  └── 调用 generation-api 提交

draw2video-client/src/features/canvas
  ├── ImageNode.tsx
  ├── TextNode.tsx
  ├── VideoNode.tsx
  └── 生成完成后写回当前节点

draw2video-client/src/features/tasks
  ├── 任务列表
  ├── 任务详情
  └── 根据 taskId 轮询任务状态
```

### 5.4 用户端 API 封装

基于现有 `draw2video-client/src/lib/api-client.ts`，新增 `generation-api.ts`：

```ts
import { api } from "@/lib/api-client";

export interface GenerateSubmitRequest {
  clientRequestId?: string;
  generateType: "TEXT" | "IMAGE" | "VIDEO";
  generateMode: string;
  modelId: number;
  providerId?: number;
  prompt?: string;
  inputParams?: string;
  sync?: boolean;
  priceAmount?: number;
}

export interface GenerateSubmitResponse {
  id: number;
  taskId: number;
  generateNo: string;
  status: string;
}

export interface GenerateResult {
  id: number;
  taskId: number;
  generateNo: string;
  generateType: string;
  generateMode: string;
  status: string;
  outputText?: string;
  outputData?: string;
  outputUrls?: string;
  assetIds?: string;
  failMessage?: string;
  createTime?: string;
  finishTime?: string;
}

export const generationApi = {
  submit: (data: GenerateSubmitRequest) =>
    api.post<GenerateSubmitResponse>("/aigc/gen/submit", data),

  generateText: (data: Omit<GenerateSubmitRequest, "generateType" | "generateMode">) =>
    api.post<GenerateSubmitResponse>("/aigc/gen/text/generate", data),

  textToImage: (data: Omit<GenerateSubmitRequest, "generateType" | "generateMode">) =>
    api.post<GenerateSubmitResponse>("/aigc/gen/image/text-to-image", data),

  textToVideo: (data: Omit<GenerateSubmitRequest, "generateType" | "generateMode">) =>
    api.post<GenerateSubmitResponse>("/aigc/gen/video/text-to-video", data),

  getResult: (taskId: number) =>
    api.get<GenerateResult>(`/aigc/gen/result?taskId=${taskId}`),
};
```

### 5.5 用户端生成流程

`/app` 快捷生成入口流程：

```text
用户输入 prompt
  ↓
可选粘贴、上传或从资产库选择参考图
  ↓
按参考图数量选择生成能力
  ↓
按 type + capability 加载图片/视频模型
  ↓
选择模型并加载参数模板
  ↓
滑杆按钮打开 DynamicParamForm 参数弹窗
  ↓
提交 quickGenerateProject
  ↓
服务端创建项目、参考图节点、目标生成节点和连线
  ↓
进入 /canvas?projectId=...
```

`/app` 参考图交互约定：

- `Ctrl+V` / 粘贴剪贴板图片时，会把图片上传为 `IMAGE` 资产并加入参考图列表。
- 纸夹按钮支持选择一张或多张本地图片文件。
- 加号按钮打开资产选择弹窗，弹窗从当前用户资产库中加载图片资产。
- 粘贴、本地上传、资产选择得到的图片都会追加到同一个参考图列表。
- 第一张参考图作为主预览图展示，并继续用于旧版单图字段兼容。
- 多余参考图在缩略图条中展示，可以逐张移除。
- 参考图列表缓存到 `localStorage` 的 `copse:workspace:reference-images`，切走后再回到 `/app` 会恢复当前参考图；缓存主体应是 `assetId`、文件名、MIME、尺寸等稳定信息，预览 URL 只作为运行时显示值，私有 OSS/S3 URL 过期前需要重新读取资产详情刷新。
- 右侧滑杆按钮打开模型参数弹窗，表单复用 canvas 节点使用的 `DynamicParamForm`。

`/app` 提交前置条件：

- 必须选择支持当前输入形态的图片或视频模型。
- 参考图上传完成后才能提交。
- 参数模板加载完成后才能提交。
- 如果模板加载失败或必填参数缺失，页面会展示原因并打开参数弹窗。
- 提交 `quickGenerateProject` 时同时发送旧版单图字段和新版多图数组字段：单图取唯一参考图，多图取首张作为旧版兼容字段，并把完整 `referenceAssetIds` / `referencePreviewUrls` 传给后端；模型参数使用参数弹窗中合并模板默认值后的结果。

图片生成流程：

```text
用户选择 ImageNode
  ↓
打开 composer
  ↓
选择模型
  ↓
读取参数模板
  ↓
填写 prompt 和参数
  ↓
调用模型价格预估
  ↓
展示预计消耗
  ↓
提交 /app-api/aigc/gen/image/text-to-image
  ↓
返回 taskId
  ↓
节点进入 generating 状态
  ↓
通过画布 run/sync 或 task 模块轮询任务进度
  ↓
成功后读取 TASK_STATUS_PATCH / 生成结果
  ↓
解析 outputUrls / assetIds
  ↓
写回当前 ImageNode
```

当前服务端画布生成路径使用：

```text
POST /app-api/canvas/projects/{projectId}/nodes/{nodeId}/run
POST /app-api/canvas/projects/{projectId}/nodes/{nodeId}/run/sync
```

`run/sync` 返回 `operation.operationJson`，其中 `payload.patch` 是画布节点的最终写回数据。前端发起生成的当前节点必须在接口返回 `SUCCESS` 后主动解析并本地合并该 patch，不能只依赖 websocket 或增量 sync 回流，否则同一个客户端可能因为过滤自身 clientId、网络延迟或版本同步节奏，导致节点仍停留在 `pending`。

视频生成流程：

```text
用户创建或选择 VideoNode
  ↓
选择视频模型和参数
  ↓
可选连接 image -> video 引用
  ↓
价格预估
  ↓
提交 /app-api/aigc/gen/video/text-to-video 或通用 submit
  ↓
返回 taskId
  ↓
VideoNode 显示队列中、运行中、成功、失败
  ↓
成功后用 outputUrls 渲染 video
```

文本生成流程：

```text
用户选择 TextNode
  ↓
输入 prompt
  ↓
选择文本模型
  ↓
提交 /app-api/aigc/gen/text/generate
  ↓
成功后把 outputText 写回 TextNode
```

### 5.6 用户端状态映射

建议统一封装 `generation-status.ts`：

| 后端状态 | 用户端文案 | UI 表现 |
| --- | --- | --- |
| `CREATED` | 已创建 | 普通等待 |
| `SUBMITTING` | 提交中 | loading |
| `SUBMITTED` | 已提交 | 等待渠道响应 |
| `RUNNING` | 生成中 | 进度条或 loading |
| `CALLBACK_WAITING` | 等待回调 | 进度条或 loading |
| `SYNCING` | 同步中 | 进度条或 loading |
| `DOWNLOADING` | 下载中 | 进度条或 loading |
| `ASSET_CREATING` | 资产创建中 | 进度条或 loading |
| `SUCCESS` | 已完成 | 展示结果 |
| `FAILED` | 生成失败 | 展示 `failMessage` |
| `CANCELLED` | 已取消 | 灰色终态 |

用户端轮询规则：

- `SUCCESS`、`FAILED`、`CANCELLED`、兼容拼写 `CANCELED` 为终态，停止轮询。
- 其他状态均视为非终态，继续轮询生成结果。
- 不要只轮询 `CREATED`、`SUBMITTED`、`RUNNING`，因为后端异步提交后会进入 `CALLBACK_WAITING`，同步补偿时会进入 `SYNCING`，文件结果处理时可能进入 `DOWNLOADING`、`ASSET_CREATING`。
- 用户端 `generation-status.ts` 应以“非终态继续轮询”为默认策略，避免后端新增中间态后前端提前停止刷新。

如果任务真实状态来自 `aigc-task`，则 `aigc-gen.status` 只作为结果层辅助展示，进度和取消以 `aigc-task` 为准。

### 5.7 用户端设计规范

生成相关 UI 需要遵守：

- 页面底色使用 `#f7f4ed`，避免纯白。
- 文本主色使用 `#1c1c1c`，次级文本使用 `#5f5f5d`。
- 卡片边框使用 `1px solid #eceae4`，不要使用重阴影。
- 主按钮使用深色 `#1c1c1c`、`#fcfbf8` 文本和 inset shadow。
- 控件保持紧凑，符合“创作工具”而不是“营销卡片”。
- 真实图片和 sketch 预览应以媒体本身作为节点主体，按真实宽高比缩放，不额外包固定比例黑边或暖色外框；空 draft 占位才使用边框卡片。
- 图像、视频预览保留圆角，连接加号必须能显示在节点外侧，因此外层节点不能裁剪 overflow；只允许内部媒体层裁剪圆角。
- 生成中状态内嵌在节点内部，不弹出大面积遮挡层。图片节点使用从左到右循环扫过的高光渐变，并在中心展示当前后端状态文案和耗时。

## 6. 管理端开发方案

### 6.1 管理端菜单规划

生成服务归属于“生成中心”：

```text
AIGC 平台
  └── 生成中心
      ├── 生成记录
      ├── 回调记录
      └── 渠道日志
```

### 6.2 管理端目录规划

API 目录：

```text
draw2video-admin/src/api/aigc/gen
  ├── record
  │   └── index.ts
  ├── callback
  │   └── index.ts
  ├── provider-log
  │   └── index.ts
  └── types.ts
```

页面目录：

```text
draw2video-admin/src/views/aigc/gen
  ├── record
  │   ├── index.vue
  │   └── detail.vue
  ├── callback
  │   └── index.vue
  ├── provider-log
  │   └── index.vue
  └── utils.ts
```

### 6.3 管理端 API 封装

`src/api/aigc/gen/record/index.ts`：

```ts
import request from '@/config/axios'

export const getGenerateRecordPage = (params: any) => {
  return request.get({ url: '/aigc/gen/record/page', params })
}

export const getGenerateRecord = (id: number) => {
  return request.get({ url: '/aigc/gen/record/get?id=' + id })
}

export const syncGenerateTask = (taskId: number) => {
  return request.post({ url: '/aigc/gen/record/sync?taskId=' + taskId })
}
```

`src/api/aigc/gen/callback/index.ts`：

```ts
import request from '@/config/axios'

export const getGenerateCallbackPage = (params: any) => {
  return request.get({ url: '/aigc/gen/callback/page', params })
}
```

`src/api/aigc/gen/provider-log/index.ts`：

```ts
import request from '@/config/axios'

export const getGenerateProviderLogPage = (params: any) => {
  return request.get({ url: '/aigc/gen/provider-log/page', params })
}
```

### 6.4 生成记录页面

页面路径：

```text
draw2video-admin/src/views/aigc/gen/record/index.vue
```

筛选项：

| 筛选项 | 字段 |
| --- | --- |
| 用户 ID | `userId` |
| 任务 ID | `taskId` |
| 生成流水号 | `generateNo` |
| 生成类型 | `generateType` |
| 生成模式 | `generateMode` |
| 模型 ID | `modelId` |
| 渠道编码 | `providerCode` |
| 状态 | `status` |

列表字段：

| 字段 | 说明 |
| --- | --- |
| `generateNo` | 生成流水号 |
| `taskId` | 任务 ID |
| `userId` | 用户 ID |
| `generateType` | 生成类型 |
| `generateMode` | 生成模式 |
| `modelCode` | 模型编码 |
| `providerCode` | 渠道编码 |
| `providerStatus` | 第三方状态 |
| `status` | 生成状态 |
| `priceAmount` | 销售价 |
| `costAmount` | 成本价 |
| `submitTime` | 提交时间 |
| `finishTime` | 完成时间 |
| `failMessage` | 失败信息 |

行操作：

| 操作 | 条件 | 说明 |
| --- | --- | --- |
| 详情 | 始终展示 | 跳转详情页 |
| 同步第三方任务 | 非成功终态可展示 | 调用 `/sync?taskId=` |
| 查看任务 | 有 `taskId` | 跳转任务详情 |
| 查看回调 | 有 `taskId` | 带筛选进入回调记录 |
| 查看渠道日志 | 有 `taskId` | 带筛选进入渠道日志 |

### 6.5 生成记录详情页

页面路径：

```text
draw2video-admin/src/views/aigc/gen/record/detail.vue
```

详情页区块：

| 区块 | 内容 |
| --- | --- |
| 基础信息 | ID、生成流水号、任务 ID、用户 ID、客户端请求号 |
| 类型信息 | 生成类型、生成模式、状态 |
| 模型渠道 | 模型 ID、模型编码、渠道 ID、渠道编码、第三方任务号、第三方状态 |
| 输入信息 | prompt、inputParams JSON |
| 输出信息 | outputText、outputData、outputUrls、assetIds |
| 计费信息 | freezeId、priceAmount、costAmount |
| 时间节点 | submitTime、callbackTime、finishTime、createTime |
| 失败信息 | failReason、failMessage |
| 关联信息 | 任务详情、回调记录、渠道日志入口 |

交互要求：

- `inputParams`、`outputData`、`outputUrls`、`assetIds` 使用 JSON 格式化组件。
- `prompt` 支持复制，但不自动写入日志。
- `outputUrls` 支持图片、视频链接预览。
- 同步第三方任务需要二次确认。
- 失败信息展示时区分内部 `failReason` 和用户可读 `failMessage`。

### 6.6 回调记录页面

页面路径：

```text
draw2video-admin/src/views/aigc/gen/callback/index.vue
```

列表字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 回调 ID |
| `recordId` | 生成记录 ID |
| `taskId` | 任务 ID |
| `providerCode` | 渠道编码 |
| `providerTaskId` | 第三方任务 ID |
| `callbackType` | 回调类型 |
| `callbackNo` | 回调编号 |
| `signatureValid` | 验签结果 |
| `processStatus` | 处理状态 |
| `processMessage` | 处理说明 |
| `processTime` | 处理时间 |

详情抽屉：

- `rawBody` 原始回调 JSON。
- `parsedData` 解析后数据。
- `processMessage` 处理结果说明。
- `signatureValid=false` 时使用危险色标识。
- 原始回调内容只在管理端展示，用户端永不展示。

### 6.7 渠道调用日志页面

页面路径：

```text
draw2video-admin/src/views/aigc/gen/provider-log/index.vue
```

列表字段：

| 字段 | 说明 |
| --- | --- |
| `recordId` | 生成记录 ID |
| `taskId` | 任务 ID |
| `providerCode` | 渠道编码 |
| `modelCode` | 模型编码 |
| `apiAction` | 调用动作 |
| `requestId` | 请求 ID |
| `success` | 是否成功 |
| `httpStatus` | HTTP 状态码 |
| `errorCode` | 错误码 |
| `errorMessage` | 错误信息 |
| `durationMs` | 耗时 |

详情抽屉：

- `requestSummary` 请求摘要。
- `responseSummary` 响应摘要。
- `errorMessage` 错误详情。
- 只展示摘要，不展示密钥、Authorization、完整敏感请求头。

### 6.8 管理端实现规范

- API 文件放在 `src/api/aigc/gen/<resource>/index.ts`。
- 页面放在 `src/views/aigc/gen/<resource>/index.vue`。
- 使用 `ContentWrap`、`el-form`、`el-table`、`Pagination`。
- 操作按钮使用 `v-hasPermi`。
- 删除、同步、重试类操作必须二次确认。
- 页面组件路径必须匹配后端动态菜单配置。
- 管理端接口不硬编码 `/admin-api`。
- 不在错误提示、导出文件、日志中展示密钥或第三方敏感凭证。

## 7. 模型与渠道服务协作方案

### 7.1 用户端协作

用户端生成提交前需要串联 `aigc-model`：

```text
GET /app-api/aigc/model/list?type=IMAGE
  ↓
用户选择模型
  ↓
GET /app-api/aigc/model/param/list?modelId=&capability=
  ↓
动态渲染参数表单
  ↓
POST /app-api/aigc/model/price/calculate
  ↓
展示预计消耗
  ↓
POST /app-api/aigc/gen/image/text-to-image
```

协作原则：

- 模型列表、参数模板、价格预估来自 `aigc-model`。
- 生成提交、结果查询来自 `aigc-gen`。
- 任务进度、取消、失败退款状态来自 `aigc-task`。
- 钱包余额和流水来自 `aigc-billing`。
- 结果下载和资产详情来自 `aigc-asset`。

### 7.2 管理端协作

管理端排障链路：

```text
生成记录
  ↓
查看 taskId
  ↓
跳转任务详情
  ↓
查看任务日志
  ↓
查看生成回调
  ↓
查看渠道调用日志
  ↓
必要时同步第三方任务
```

模型渠道配置链路：

```text
渠道商管理
  ↓
模型管理
  ↓
参数模板
  ↓
价格规则
  ↓
路由规则
  ↓
租户授权
  ↓
用户端可见模型
  ↓
生成服务提交调用
```

## 8. 枚举与字典建议

### 8.1 生成类型

| 值 | 文案 |
| --- | --- |
| `TEXT` | 文本 |
| `IMAGE` | 图片 |
| `VIDEO` | 视频 |
| `AUDIO` | 音频 |
| `CODE` | 代码 |
| `DOCUMENT` | 文档 |
| `PPT` | PPT |
| `DIGITAL_HUMAN` | 数字人 |

第一阶段用户端重点实现：

- `TEXT`
- `IMAGE`
- `VIDEO`

### 8.2 生成模式

| 值 | 文案 |
| --- | --- |
| `TEXT_GENERATE` | 文本生成 |
| `TEXT_TO_IMAGE` | 文生图 |
| `IMAGE_TO_IMAGE` | 图生图 |
| `TEXT_TO_VIDEO` | 文生视频 |
| `IMAGE_TO_VIDEO` | 图生视频 |

后端当前显式用户端入口已覆盖：

- `TEXT_GENERATE`
- `TEXT_TO_IMAGE`
- `TEXT_TO_VIDEO`

图生图、图生视频可通过通用 `/submit` 扩展，但前端应以后端接口和模型能力开放为准。

### 8.3 生成状态

| 值 | 管理端文案 | 用户端文案 |
| --- | --- | --- |
| `CREATED` | 已创建 | 已创建 |
| `SUBMITTING` | 提交中 | 提交中 |
| `SUBMITTED` | 已提交 | 已提交 |
| `RUNNING` | 运行中 | 生成中 |
| `CALLBACK_WAITING` | 等待回调 | 等待回调 |
| `SYNCING` | 同步中 | 同步中 |
| `DOWNLOADING` | 下载中 | 下载中 |
| `ASSET_CREATING` | 资产创建中 | 资产创建中 |
| `SUCCESS` | 成功 | 已完成 |
| `FAILED` | 失败 | 生成失败 |
| `CANCELLED` | 已取消 | 已取消 |

实际枚举应以后端 `AigcGenerateStatusEnum` 为准；前端需要在 `draw2video-admin/src/views/aigc/gen/utils.ts` 和 `draw2video-client/src/features/generation/generation-status.ts` 中统一维护，不在页面硬编码。用户端应采用“非终态继续轮询”的策略，管理端筛选项应列出所有后端状态。

## 9. 安全与隐私要求

用户端：

- 不展示 `providerCode`、`providerTaskId`、`costAmount`、第三方错误原文。
- 不允许前端保存 provider API key。
- 不把 prompt、生成参数、输出 URL 打印到浏览器 console。
- 上传图片、视频数据按现有 IndexedDB 规则处理，不把大文件 dataURL 写入 localStorage。
- 认证走 `/app-api/member/auth/*`，不混用管理端账号体系。

管理端：

- 可展示渠道编码、第三方任务 ID、成本价、失败码，但不得展示密钥。
- `requestSummary`、`responseSummary` 后端若包含敏感字段，前端需要在展示前做兜底脱敏。
- JSON 查看器默认折叠大字段，避免页面卡顿。
- 导出如果后续增加，不能导出密钥、Authorization、完整请求头。

## 10. 联调顺序

1. 管理端 API 类型与页面骨架：先完成生成记录、回调记录、渠道日志的分页查询。
2. 管理端详情与同步：完成生成详情页、JSON 展示、同步第三方任务。
3. 用户端 API 封装：完成 `generation-api.ts`、类型定义、错误处理。
4. 用户端模型协作：接入模型列表、参数模板、价格预估。
5. 用户端画布提交：ImageNode、TextNode、VideoNode 调用生成接口。
6. 用户端任务联动：提交后写入 taskId，复用任务轮询展示进度。
7. 用户端结果写回：成功后调用生成结果接口，将 `outputText` 或 `outputUrls` 写回节点。
8. 资产联动：成功后使用 `assetIds` 跳转资产详情或写入资产库。
9. 异常联调：覆盖余额不足、提示词违规、渠道失败、任务超时、回调失败、同步补偿。
10. 权限菜单：配置管理端动态菜单和 `aigc:gen:*` 权限。

## 11. 验收标准

### 11.1 用户端验收

- 可在创作画布中选择模型、填写参数、查看价格预估并提交生成。
- 文本生成成功后写回 `TextNode`。
- 图片生成成功后写回 `ImageNode`。
- 视频生成成功后写回 `VideoNode`。
- 生成中状态不阻塞整个画布，只影响当前节点。
- 发起生成的当前节点在 `run/sync` 返回 `SUCCESS` 时必须立即应用 `operation.operationJson.payload.patch`，无需等待 websocket 回流。
- 失败时展示用户可理解的 `failMessage`。
- 提交后可以通过任务列表和任务详情查看进度。
- 生成结果轮询覆盖所有非终态：`CREATED`、`SUBMITTING`、`SUBMITTED`、`RUNNING`、`CALLBACK_WAITING`、`SYNCING`、`DOWNLOADING`、`ASSET_CREATING` 均不会提前停止刷新。
- 不在用户端展示成本价、渠道商密钥、第三方任务原始信息。
- UI 符合暖色、安静、紧凑的 Copse 工作区风格。

### 11.2 管理端验收

- 生成记录支持分页、筛选、详情查看。
- 生成详情可查看 prompt、inputParams、outputData、outputUrls、assetIds、计费快照和失败信息。
- 管理端生成记录 `/get` 和 `/page` 返回 `AigcGenerateRecordRespVO`，页面展示的用户、模型、渠道、计费、时间节点和失败原因字段不为空。
- 可对指定 `taskId` 执行同步第三方任务，并有二次确认。
- 回调记录支持分页和原始回调详情查看。
- 渠道调用日志支持分页和请求/响应摘要查看。
- 所有操作按钮有权限控制。
- 页面路径与后端菜单组件路径一致。
- 不泄露密钥、Authorization 和敏感请求头。

## 12. 第一阶段交付清单

管理端：

```text
yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/cn/iocoder/yudao/module/aigc/gen/controller/admin/record/vo/AigcGenerateRecordRespVO.java

draw2video-admin/src/api/aigc/gen/types.ts
draw2video-admin/src/api/aigc/gen/record/index.ts
draw2video-admin/src/api/aigc/gen/callback/index.ts
draw2video-admin/src/api/aigc/gen/provider-log/index.ts

draw2video-admin/src/views/aigc/gen/utils.ts
draw2video-admin/src/views/aigc/gen/record/index.vue
draw2video-admin/src/views/aigc/gen/record/detail.vue
draw2video-admin/src/views/aigc/gen/callback/index.vue
draw2video-admin/src/views/aigc/gen/provider-log/index.vue
```

用户端：

```text
draw2video-client/src/features/generation/generation-api.ts
draw2video-client/src/features/generation/generation-types.ts
draw2video-client/src/features/generation/generation-status.ts
draw2video-client/src/features/generation/generation-client-request.ts
draw2video-client/src/features/generation/generation-result.ts
draw2video-client/src/features/generation/hooks/use-generation-submit.ts
draw2video-client/src/features/generation/hooks/use-generation-result.ts
```

集成点：

```text
draw2video-client/src/features/canvas/ImageNode.tsx
draw2video-client/src/features/canvas/TextNode.tsx
draw2video-client/src/features/canvas/VideoNode.tsx
draw2video-client/src/features/image-generation/*
draw2video-client/src/features/tasks/*
draw2video-client/src/lib/api-client.ts
```

## 13. 关键风险

- 后端 `aigc-gen` 当前用户端接口主要覆盖文本生成、文生图、文生视频；图生图、图生视频需要确认是否通过通用 `/submit` 放开。
- 生成结果 `outputUrls`、`assetIds` 是 JSON 字符串，前端需要约定解析结构，建议后端后续提供结构化 VO。
- 任务进度事实源在 `aigc-task`，不能只依赖 `aigc-gen/result` 做轮询。
- 生成资产转存文件名不能使用固定标题，例如 `IMAGE生成资产.png`，否则同一天同路径会覆盖，多个不同 assetId 会指向同一 `file_url`。data URL 落文件时必须包含 `generateNo`、`taskId` 或唯一 ID。
- 管理端回调与渠道日志目前只有分页接口，没有详情接口，可先用行数据打开抽屉展示；如果分页返回字段被裁剪，需要补详情接口。
- 用户端画布节点保存 taskId、recordId、assetIds 时，要遵守现有 canvas persistence 规则，避免把大媒体内容写入 localStorage。
- 用户端不能把私有 OSS/S3 签名 URL 当作稳定参考图来源。`/app` 和 canvas 都应以 `assetId` 为准刷新访问 URL，`previewUrl`、`outputPreviewUrl`、`videoUrl` 只能作为当前渲染态。
