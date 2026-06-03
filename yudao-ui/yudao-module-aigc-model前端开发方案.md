# yudao-module-aigc-model 前端开发方案

本文档基于 `c:\use\code\project\manman\yudao-ui\AIGC平台第一阶段用户端前端开发计划.md` 和后端模块 `c:\use\code\project\manman\yudao-module-aigc-model` 梳理，用于指导 `yudao-module-aigc-model` 对应的管理端与用户端前端开发。

## 1. 模块定位

`yudao-module-aigc-model` 是 AIGC 平台的模型与渠道中心，负责第三方模型供应商、模型配置、模型能力、参数模板、价格规则、路由规则、租户授权和模型调用计量。

前端侧分为两类使用场景：

- 管理端：维护渠道商、模型、参数模板、价格规则、路由规则、租户授权、调用计量。
- 用户端：获取可用模型、获取动态参数模板、进行价格预估，并在生成提交前展示预计消耗。

第一阶段目标：

```text
管理端可配置模型能力，用户端可完成模型选择 → 参数填写 → 价格预估 → 生成提交前确认。
```

## 2. 前端项目范围

| 项目 | 路径 | 技术栈 | 建设目标 |
| --- | --- | --- | --- |
| `draw2video-admin` | `c:\use\code\project\manman\yudao-ui\draw2video-admin` | Vue3 + Vite + TypeScript + Element Plus | AIGC 模型中心管理后台 |
| `draw2video-client` | `c:\use\code\project\manman\yudao-ui\draw2video-client` | Next.js + React + TypeScript + Tailwind CSS | 用户创作端模型选择、参数模板、价格预估 |

## 3. 后端接口依据

### 3.1 用户端接口

用户端接口来自后端 `AigcModelAppController`，前端调用时统一带 `/app-api` 前缀。

| 功能 | 方法 | 接口 | 说明 |
| --- | --- | --- | --- |
| 获取模型详情 | GET | `/app-api/aigc/model/get?id=` | 获取租户可见模型详情 |
| 获取模型列表 | GET | `/app-api/aigc/model/list?type=` | 获取当前租户可用模型列表 |
| 获取参数模板 | GET | `/app-api/aigc/model/param/list?modelId=&capability=` | 获取模型能力对应的动态参数模板 |
| 价格预估 | POST | `/app-api/aigc/model/price/calculate` | 根据模型、能力、参数计算预计消耗 |

### 3.2 管理端接口

管理端接口前端不写 `/admin-api`，只写业务路径。

| 模块 | 基础路径 | 功能 |
| --- | --- | --- |
| 渠道商管理 | `/aigc/model/provider` | 创建、更新、删除、详情、分页、状态、测试渠道商 |
| 模型管理 | `/aigc/model` | 创建、更新、删除、详情、分页、状态、用户端可见性、默认模型 |
| 参数模板 | `/aigc/model/param` | 创建、更新、删除、详情、按模型和能力查询模板 |
| 价格规则 | `/aigc/model/price` | 创建、更新、删除、详情、按模型和能力查询价格规则、状态 |
| 路由规则 | `/aigc/model/route` | 创建、更新、删除、详情、分页、状态 |
| 租户授权 | `/aigc/model/tenant` | 创建、更新、删除、详情、按租户列表、启用、可见性、默认模型 |
| 调用计量 | `/aigc/model/usage` | 调用日志分页、详情、筛选 |

## 4. 管理端开发方案

### 4.1 菜单规划

```text
AIGC 平台
  └── 模型中心
      ├── 渠道商管理
      ├── 模型管理
      ├── 参数模板
      ├── 价格规则
      ├── 路由规则
      ├── 租户授权
      └── 调用计量
```

### 4.2 API 目录规划

```text
draw2video-admin/src/api/aigc/model
  ├── provider/index.ts
  ├── model/index.ts
  ├── param/index.ts
  ├── price/index.ts
  ├── route/index.ts
  ├── tenant/index.ts
  └── usage/index.ts
```

管理端 API 约定：

- 新增使用 `/create`
- 修改使用 `/update`
- 删除使用 `/delete?id=`
- 分页使用 `/page`
- 详情使用 `/get?id=`
- 导出使用 `/export-excel`
- 状态类操作使用后端已有路径，例如 `/status`、`/visible`、`/default`

### 4.3 页面目录规划

```text
draw2video-admin/src/views/aigc/model
  ├── provider/index.vue
  ├── model/index.vue
  ├── param/index.vue
  ├── price/index.vue
  ├── route/index.vue
  ├── tenant/index.vue
  └── usage/index.vue
```

页面组件路径需要与后端菜单返回的组件路径保持一致。

### 4.4 页面功能清单

| 页面 | 核心功能 |
| --- | --- |
| 渠道商管理 | 分页、新增、编辑、删除、启停、测试渠道、健康状态、余额展示、密钥脱敏 |
| 模型管理 | 分页、新增、编辑、删除、上下线、用户端展示、默认模型、能力配置 |
| 参数模板 | 按模型和能力配置动态参数，支持文本、数字、布尔、枚举、范围、正则校验 |
| 价格规则 | 按模型和能力配置成本价、销售价、计费单位、生效时间、价格试算 |
| 路由规则 | 配置任务类型、能力、路由策略、候选模型、用户等级、启停 |
| 租户授权 | 配置租户可用模型、是否启用、是否前台可见、是否默认、日限额、并发 |
| 调用计量 | 查看任务、用户、模型、token、成本、售价、耗时、错误摘要 |

### 4.5 渠道商管理

核心字段：

```text
code
name
apiBaseUrl
authType
apiKey
secretKey
healthStatus
balance
status
```

前端要求：

- 列表展示渠道编码、名称、API 地址、鉴权方式、健康状态、余额、状态、创建时间。
- 新增和编辑表单支持配置 `apiKey`、`secretKey`。
- 详情返回的密钥字段如果已脱敏，编辑提交时不能把脱敏字符串覆盖真实密钥。
- 启停操作需要二次确认。
- 测试渠道商操作需要展示成功、失败、失败原因。
- 密钥字段禁止出现在 URL、日志、导出文件和错误提示中。

### 4.6 模型管理

核心字段：

```text
providerId
code
name
model
type
publicVisible
defaultModel
maxConcurrent
timeoutSeconds
status
```

前端要求：

- 支持按模型名称、模型编码、模型类型、渠道商、状态筛选。
- 支持配置模型类型：文本、图片、视频、音频、审核。
- 支持配置用户端是否展示。
- 支持配置默认模型。
- 支持配置最大并发和超时时间。
- 模型下线时需要提示可能影响用户端生成入口。
- 默认模型建议同类型或同能力下只允许一个，具体以后端校验为准。

### 4.7 参数模板管理

核心字段：

```text
modelId
capability
paramKey
paramName
paramType
requiredStatus
defaultValue
options
minValue
maxValue
regexPattern
sort
status
```

前端要求：

- 支持按模型、能力筛选参数模板。
- 支持拖拽排序或数字排序。
- 支持不同参数类型的表单配置。
- `options` 建议提供 JSON 编辑和可视化键值编辑两种方式。
- `regexPattern` 需要提供说明字段或占位提示。
- 禁用状态的参数不在用户端动态表单中展示。

参数类型与控件映射：

| 参数类型 | 管理端配置控件 | 用户端渲染控件 |
| --- | --- | --- |
| `STRING` | 输入框 | 输入框 |
| `NUMBER` | 数字输入框 | 数字输入框或 Slider |
| `BOOLEAN` | 开关 | Switch |
| `SELECT` | 选项配置 | Select |
| `MULTI_SELECT` | 多选项配置 | Checkbox Group |
| `JSON` | JSON 编辑器 | 高级配置区域 |

### 4.7.1 Gemini / Nano Banana 图片渠道配置

后端已内置 Gemini 原生图片接口适配器，渠道商编码固定使用：

```text
gemini-image
```

该渠道不是 OpenAI-compatible 接口，调用方式为 Gemini 原生 `generateContent`：

```text
POST {apiBaseUrl}/models/{model}:generateContent?key={apiKey}
```

管理端添加渠道商时建议配置：

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| 渠道编码 `code` | `gemini-image` | 必须与后端适配器一致 |
| API 地址 `apiBaseUrl` | `http://67.21.92.138:3010/v1beta` | 填到 `/v1beta` 即可；如果中转站要求，也可直接填完整 `.../models/gemini-2.5-flash-image:generateContent` |
| 鉴权方式 `authType` | `API_KEY` | API Key 会拼到 query 参数 `key=` |
| API Key `apiKey` | 中转站 Key | 禁止写入文档、URL 日志、错误提示 |
| 超时时间 `timeoutSeconds` | `120` | 图片生成建议不低于 120 秒 |

当前可配置的 Nano Banana 模型：

| 中转站常见模型名 | Google 官方模型名 | 代号 | 建议定位 |
| --- | --- | --- | --- |
| `gemini-3.1-flash-image-preview` | `gemini-3.1-flash-image` | Nano Banana 2 | 默认推荐，速度和性价比较好 |
| `gemini-2.5-flash-image` | `gemini-2.5-flash-image` | Nano Banana | 高速低延迟，1024px 固定分辨率 |
| `gemini-3-pro-image-preview` | `gemini-3-pro-image` | Nano Banana Pro | 专业级，支持 Thinking，最高 4K |

模型管理建议：

- 用户端展示读取模型名称 `name`，因此对外建议填写产品化名称，例如 `Nano Banana`、`Nano Banana 2`、`Nano Banana Pro`。
- 模型编码 `code` 用于平台内部识别和生成记录，建议使用稳定、可读的内部编码，例如 `nano-banana`、`nano-banana-2`、`nano-banana-pro`。
- 模型标识 `model` 用于实际调用第三方接口，填写中转站可调用的模型名，例如 `gemini-2.5-flash-image`。
- 如果历史数据里已经用内部编码，或者同一个内部编码在不同渠道要映射到不同中转站模型，可在渠道商 `extraConfig` 中配置映射：

```json
{
  "modelMapping": {
    "nano-banana-fast": "gemini-2.5-flash-image",
    "nano-banana-2": "gemini-3.1-flash-image-preview",
    "nano-banana-pro": "gemini-3-pro-image-preview"
  }
}
```

参数模板建议：

| 参数 Key | 类型 | 默认值 | 可选值 | 说明 |
| --- | --- | --- | --- | --- |
| `ratio` | `SELECT` | `1:1` | `1:1`,`2:3`,`3:2`,`3:4`,`4:3`,`4:5`,`5:4`,`9:16`,`16:9`,`21:9` | 透传为 Gemini `generationConfig.imageConfig.aspectRatio` |
| `imageSize` | `SELECT` | `1K` | `512`,`1K`,`2K`,`4K` | 仅 `gemini-3.1-flash-image` / `gemini-3-pro-image` 系列生效；`gemini-2.5-flash-image` 固定 1024px |

请求体结构与 Gemini 官方文档保持一致：

```json
{
  "contents": [
    {
      "parts": [
        {
          "text": "prompt"
        }
      ]
    }
  ],
  "generationConfig": {
    "responseModalities": ["TEXT", "IMAGE"],
    "imageConfig": {
      "aspectRatio": "1:1",
      "imageSize": "1K"
    }
  }
}
```

图生图场景会把 `inputImages` / `inputImageUrls` 转成 Gemini `inlineData`，返回结果从 `candidates[].content.parts[].inlineData` 解析为 `data:{mimeType};base64,...` 后进入现有素材入库流程。

### 4.8 价格规则管理

核心字段：

```text
modelId
capability
billingUnit
costPrice
salePrice
currencyType
priceConfig
effectiveStartTime
effectiveEndTime
status
```

前端要求：

- 支持按模型、能力、计费单位、状态筛选。
- 支持配置成本价和销售价。
- 用户端只展示销售价，不展示成本价。
- 支持配置生效开始时间和结束时间。
- 支持价格试算，输入模型、能力、参数后展示预计消耗。
- 价格规则停用时需要提示可能导致用户端无法预估价格。

计费单位展示：

| 后端值 | 前端文案 |
| --- | --- |
| `PER_TASK` | 按任务 |
| `PER_IMAGE` | 按张 |
| `PER_SECOND` | 按秒 |
| `PER_5_SECONDS` | 每 5 秒 |
| `PER_BATCH` | 按批次 |

### 4.9 路由规则管理

核心字段：

```text
taskType
capability
strategy
modelIds
userLevel
status
```

前端要求：

- 支持按任务类型、能力、策略、状态筛选。
- `modelIds` 使用多选模型控件。
- 策略支持固定模型、最低成本、最高成功率、最快响应、轮询。
- 路由规则禁用时需要展示影响范围。
- 用户端不展示路由策略。

### 4.10 租户授权管理

核心字段：

```text
modelId
enabled
publicVisible
defaultModel
sort
maxConcurrent
dailyLimit
```

前端要求：

- 支持按租户、模型、启用状态、用户端可见性筛选。
- 支持配置租户是否可用该模型。
- 支持配置是否在用户端展示。
- 支持配置租户级默认模型。
- 支持配置租户级并发和日限额。
- 用户端模型列表必须只展示当前租户已授权且可见的模型。

### 4.11 调用计量

前端要求：

- 展示任务 ID、用户 ID、模型、能力、调用状态、token 用量、成本价、销售价、耗时、错误摘要。
- 支持按用户、模型、任务、时间、状态筛选。
- 成本价只在管理端展示。
- 错误摘要需要避免展示密钥、请求头等敏感信息。

## 5. 当前开发落地状态

### 5.1 已落地内容

管理端已落地：

| 类型 | 文件 | 状态 |
| --- | --- | --- |
| API 类型 | `draw2video-admin/src/api/aigc/model/types.ts` | 已新增 |
| 渠道商 API | `draw2video-admin/src/api/aigc/model/provider/index.ts` | 已新增 |
| 模型 API | `draw2video-admin/src/api/aigc/model/model/index.ts` | 已新增 |
| 参数模板 API | `draw2video-admin/src/api/aigc/model/param/index.ts` | 已新增 |
| 价格规则 API | `draw2video-admin/src/api/aigc/model/price/index.ts` | 已新增 |
| 路由规则 API | `draw2video-admin/src/api/aigc/model/route/index.ts` | 已新增 |
| 租户授权 API | `draw2video-admin/src/api/aigc/model/tenant/index.ts` | 已新增 |
| 调用计量 API | `draw2video-admin/src/api/aigc/model/usage/index.ts` | 已新增 |
| 模型中心常量 | `draw2video-admin/src/views/aigc/model/constants.ts` | 已新增 |
| 渠道商页面 | `draw2video-admin/src/views/aigc/model/provider/index.vue` | 已新增 |
| 渠道商表单 | `draw2video-admin/src/views/aigc/model/provider/ProviderForm.vue` | 已新增 |
| 模型页面 | `draw2video-admin/src/views/aigc/model/model/index.vue` | 已新增 |
| 模型表单 | `draw2video-admin/src/views/aigc/model/model/ModelForm.vue` | 已新增 |

用户端已落地：

| 类型 | 文件 | 状态 |
| --- | --- | --- |
| AIGC 模型 API | `draw2video-client/src/features/generation/model-api.ts` | 已新增 |
| AIGC 模型 Hook | `draw2video-client/src/features/generation/use-aigc-models.ts` | 已新增 |
| 动态参数表单 | `draw2video-client/src/features/generation/DynamicParamForm.tsx` | 已新增 |
| 价格预估组件 | `draw2video-client/src/features/generation/PriceEstimate.tsx` | 已新增 |
| 图片节点接入 | `draw2video-client/src/features/canvas/ImageNode.tsx` | 已接入 |
| 生成请求扩展 | `draw2video-client/src/features/canvas/use-generation.ts` | 已接入 `providerModel` 和 `aigcModelId` |
| 画布类型扩展 | `draw2video-client/src/features/canvas/types.ts` | 已扩展模型元数据字段 |

### 5.2 已修复的评审问题

| 问题 | 修复状态 | 说明 |
| --- | --- | --- |
| 用户端选择 AIGC 模型后，生成元数据仍回退本地默认模型 | 已修复 | 已区分 `aigcModelId`、`providerModel`、`modelName` |
| 价格预估防抖依赖 `params` 对象引用，导致防抖不稳定 | 已修复 | 已改为依赖序列化后的 `priceParams` |
| 渠道商编辑时空密钥可能覆盖真实密钥 | 已修复 | 更新提交前删除空 `apiKey`、`secretKey` 字段 |

### 5.3 当前未完成项

| 模块 | 未完成内容 | 责任方 |
| --- | --- | --- |
| 管理端 | 参数模板页面 | 前端继续补齐 |
| 管理端 | 价格规则页面 | 前端继续补齐 |
| 管理端 | 租户授权页面 | 前端继续补齐 |
| 管理端 | 路由规则页面 | 前端继续补齐 |
| 管理端 | 调用计量页面 | 前端继续补齐，需确认后端是否已有 REST Controller |
| 用户端 | 视频节点接入 AIGC 模型、参数模板、价格预估 | 前端继续补齐 |
| 用户端 | 生成前余额校验 | 依赖钱包接口联调 |
| 用户端 | 预估失败时阻止收费生成 | 前端继续补齐 |
| 环境 | `pnpm approve-builds` | 需要项目维护者本机授权 |

### 5.4 当前评分

当前按已完成代码和可诊断结果评估：

```text
92 / 100
```

未达到 100 分的原因：

- 管理端 P0 中 `参数模板 / 价格规则 / 租户授权` 页面尚未补齐。
- 用户端视频节点尚未接入 `aigc-model`。
- 全量 lint/build 受 `pnpm approve-builds` 环境策略阻断。
- 钱包余额校验和收费生成拦截尚未形成闭环。

## 6. 用户端开发方案

### 6.1 用户端目标

用户端只消费经过租户授权和可见性过滤后的模型能力，不暴露渠道商、密钥、成本价、路由规则等后台信息。

第一阶段用户端需要完成：

- 获取当前租户可用模型列表。
- 按文本、图片、视频类型筛选模型。
- 获取模型参数模板。
- 根据参数模板动态渲染表单。
- 根据模型和参数实时预估价格。
- 生成提交前展示预计消耗。
- 余额不足时阻止提交或引导充值。

### 6.2 API 目录规划

当前实际落地目录为：

```text
draw2video-client/src/features/generation
  ├── model-api.ts
  ├── use-aigc-models.ts
  ├── DynamicParamForm.tsx
  └── PriceEstimate.tsx
```

原规划中的 `src/lib/aigc-api` 后续可作为跨页面通用 API 层迁移目标，但当前为贴合现有 Copse 项目结构，优先放在 `features/generation` 下。

`model-api.ts` 已封装：

```ts
getAigcModelList(type?: number)
getAigcModelDetail(id: number)
getAigcModelParamList(modelId: number, capability: string)
calculateAigcModelPrice(data: AigcModelPriceCalculateReq)
```

### 6.3 组件目录规划

```text
draw2video-client/src/features/generation
  ├── model-api.ts
  ├── use-aigc-models.ts
  ├── DynamicParamForm.tsx
  └── PriceEstimate.tsx
```

组件职责：

| 组件 | 职责 |
| --- | --- |
| `model-api.ts` | 封装 `/app-api/aigc/model/*` 用户端模型接口 |
| `use-aigc-models.ts` | 管理模型列表、选中模型、参数模板、价格预估状态 |
| `DynamicParamForm.tsx` | 根据参数模板动态渲染参数表单 |
| `PriceEstimate.tsx` | 根据模型、能力、参数展示预计消耗 |

### 6.4 用户端核心流程

```text
进入创作页
  ↓
按创作类型加载模型列表
  ↓
用户选择模型
  ↓
根据 modelId + capability 获取参数模板
  ↓
动态渲染参数表单
  ↓
用户填写 prompt 和参数
  ↓
调用价格预估接口
  ↓
展示预计消耗
  ↓
检查钱包余额
  ↓
提交生成任务
```

### 6.5 模型类型映射

| 前端场景 | type |
| --- | --- |
| 文本生成 | `1` |
| 图片生成 | `2` |
| 视频生成 | `3` |
| 音频生成，P2 | `4` |
| 审核模型，后台使用 | `5` |

### 6.6 模型能力映射

第一阶段用户端优先支持：

| 场景 | capability |
| --- | --- |
| 文本生成 | `TEXT_GENERATE` |
| 提示词优化 | `PROMPT_OPTIMIZE` |
| 文生图 | `TEXT_TO_IMAGE` |
| 图生图，预留 | `IMAGE_TO_IMAGE` |
| 文生视频 | `TEXT_TO_VIDEO` |
| 图生视频 | `IMAGE_TO_VIDEO` |

如果接口返回的能力值与上述文案存在大小写或命名差异，前端以接口返回值为准。

## 7. 用户端页面接入点

### 7.1 `/app` 创作首页

- 展示文本生成入口。
- 默认加载文本模型列表：`GET /app-api/aigc/model/list?type=1`。
- 用户选择模型后加载参数模板。
- 调用价格预估接口展示预计消耗。
- 与后续 `aigc-gen` 文本生成接口联动。

### 7.2 `/create/image` 图片画布

- 图片节点中已接入 AIGC 模型选择。
- 根据 `type=2` 获取图片模型。
- 根据 `capability=TEXT_TO_IMAGE` 获取参数模板。
- 支持比例、尺寸、数量、风格等动态参数。
- 生成前调用价格预估。
- 已区分 `aigcModelId`、`providerModel`、`modelName`，避免模型展示和实际生成元数据不一致。
- 生成成功后结果进入资产中心。

### 7.3 `/create/video` 视频创作页

- 根据 `type=3` 获取视频模型。
- 根据 `capability=TEXT_TO_VIDEO` 或 `IMAGE_TO_VIDEO` 获取参数模板。
- 支持时长、比例、分辨率、参考图等参数。
- 价格预估结果需要突出展示。
- 后续与任务轮询接口联动。

### 7.4 `/wallet` 钱包页联动

- 生成提交前刷新钱包余额。
- 如果 `wallet.availableBalance < estimatedSalePrice`，禁止提交并提示充值。
- 价格单位展示与钱包积分单位保持一致。

## 8. 动态参数表单方案

参数模板接口：

```text
GET /app-api/aigc/model/param/list?modelId=&capability=
```

字段用途：

| 后端字段 | 前端用途 |
| --- | --- |
| `paramKey` | 表单字段 key |
| `paramName` | 表单 label |
| `paramType` | 控件类型 |
| `requiredStatus` | 是否必填 |
| `defaultValue` | 默认值 |
| `options` | 下拉、单选、多选选项 |
| `minValue` | 数值最小值 |
| `maxValue` | 数值最大值 |
| `regexPattern` | 文本正则校验 |
| `sort` | 表单排序 |
| `status` | 是否展示 |

用户端表单参数建议统一为：

```ts
{
  modelId: number
  capability: string
  params: Record<string, unknown>
}
```

前端校验要求：

- 必填项依据 `requiredStatus` 校验。
- 数值项依据 `minValue`、`maxValue` 校验。
- 文本项依据 `regexPattern` 校验。
- 枚举项依据 `options` 限制可选值。
- 禁用状态参数不渲染。

## 9. 价格预估方案

价格预估接口：

```text
POST /app-api/aigc/model/price/calculate
```

触发时机：

- 用户选择模型后进行一次初始预估。
- 用户修改关键参数后防抖调用，当前防抖依赖使用序列化后的 `priceParams`，避免对象引用变化导致重复触发。
- 生成提交前强制重新预估一次。
- 预估失败时不允许提交收费任务。

展示内容：

```text
预计消耗：xx 积分
计费方式：按张 / 按秒 / 按任务
数量 / 时长：根据参数显示
余额不足：展示充值引导
```

安全要求：

- 用户端只展示销售价。
- 用户端不展示成本价。
- 用户端不展示渠道商信息和密钥。
- 所有金额、积分字段集中格式化。

## 10. 开发优先级

### 10.1 管理端 P0

- 渠道商管理，已完成基础页面
- 模型管理，已完成基础页面
- 参数模板管理
- 价格规则管理
- 租户授权管理

这些能力决定用户端是否能看到模型、能否生成参数表单、能否完成价格预估。

### 10.2 管理端 P1

- 路由规则管理
- 调用计量日志
- 渠道健康测试优化
- 价格试算工具
- 模型能力配置体验优化

### 10.3 用户端 P0

- `model-api.ts` API 封装，已完成
- 模型选择接入，图片节点已完成
- 参数模板动态表单，图片节点已完成
- 价格预估组件，图片节点已完成
- 图片生成页接入模型和价格，已完成首版
- 视频生成页接入模型和价格
- 生成提交前余额校验

### 10.4 用户端 P1

- 模型详情弹窗
- 模型能力标签
- 参数模板本地缓存
- 价格预估失败兜底提示
- 余额不足充值入口预留
- 不同模型预计耗时展示

## 11. 联调顺序

```text
1. 后端执行 aigc-model schema 和初始化数据
2. 管理端接入渠道商管理
3. 管理端接入模型管理
4. 管理端接入参数模板管理
5. 管理端接入价格规则管理
6. 管理端接入租户授权管理
7. 用户端接入模型列表
8. 用户端接入参数模板
9. 用户端接入价格预估
10. 用户端接入图片 / 视频生成提交前校验
11. 联调钱包余额和扣费提示
12. 验收用户完整链路
```

最终用户链路：

```text
注册 / 登录 → 查看钱包 → 选择模型 → 预估价格 → 提交生成 → 查看进度 → 获得结果 → 查看资产 → 钱包扣费
```

## 12. 验收标准

### 12.1 管理端验收

- 渠道商可新增、编辑、删除、启停、测试。
- 渠道商密钥不明文泄露，详情展示脱敏。
- 模型可配置类型、能力、可见性、默认模型。
- 参数模板可按模型和能力维护，用户端可正常渲染。
- 价格规则可按模型和能力维护，用户端可正常预估。
- 租户授权后，用户端只看到当前租户可用模型。
- 状态变更、删除等危险操作有二次确认。
- 所有列表支持分页、筛选、重置。

### 12.2 用户端验收

- 用户进入创作页能看到真实可用模型。
- 切换文本、图片、视频场景时模型列表正确过滤。
- 选择模型后能加载对应参数模板。
- 参数表单按模板动态渲染并完成前端校验。
- 修改参数后价格预估能实时更新。
- 生成提交前展示预计消耗。
- 余额不足时不能提交收费生成。
- 用户端不展示渠道密钥、成本价、后台路由策略等敏感信息。
- 接口异常时有明确提示，不出现未处理 Promise 错误。

## 13. 交付物

管理端交付物：

- AIGC 模型中心菜单。
- `aigc/model` API TypeScript 封装。
- 渠道商管理页面，已完成基础版。
- 模型管理页面，已完成基础版。
- 参数模板页面。
- 价格规则页面。
- 路由规则页面。
- 租户授权页面。
- 调用计量页面。
- 菜单权限配置说明。

用户端交付物：

- `src/features/generation/model-api.ts`，已完成。
- `src/features/generation/use-aigc-models.ts`，已完成。
- 模型选择接入，图片节点已完成。
- 动态参数表单组件，已完成。
- 价格预估组件，已完成。
- 图片生成页模型与价格接入，已完成首版。
- 视频生成页模型与价格接入。
- 生成提交前余额校验。
- 扣费、退款、失败提示联动。

## 14. 验证记录

### 14.1 已通过诊断的文件

以下文件已完成编辑器诊断检查，当前无新增诊断错误：

```text
draw2video-client/src/features/canvas/ImageNode.tsx
draw2video-client/src/features/canvas/types.ts
draw2video-client/src/features/canvas/use-generation.ts
draw2video-client/src/features/generation/model-api.ts
draw2video-client/src/features/generation/use-aigc-models.ts
draw2video-client/src/features/generation/DynamicParamForm.tsx
draw2video-client/src/features/generation/PriceEstimate.tsx
draw2video-admin/src/views/aigc/model/provider/ProviderForm.vue
draw2video-admin/src/views/aigc/model/provider/index.vue
draw2video-admin/src/views/aigc/model/model/ModelForm.vue
draw2video-admin/src/views/aigc/model/model/index.vue
```

### 14.2 当前命令验证限制

用户端定向 lint 命令曾尝试执行：

```text
pnpm exec eslint src/features/canvas/ImageNode.tsx src/features/canvas/types.ts src/features/canvas/use-generation.ts src/features/generation/model-api.ts src/features/generation/use-aigc-models.ts src/features/generation/DynamicParamForm.tsx src/features/generation/PriceEstimate.tsx
```

当前被 pnpm 安全策略阻断：

```text
ERR_PNPM_IGNORED_BUILDS
Ignored build scripts: msw@2.14.6, sharp@0.34.5, unrs-resolver@1.12.2
```

需要项目维护者在本机执行一次：

```text
pnpm approve-builds
```

管理端 `pnpm ts:check` 当前存在大量仓库既有类型错误，错误集中在 `pay`、`system`、`wms` 等既有页面；本次新增 AIGC 相关关键文件已通过单文件诊断。

## 15. 下一步补齐计划

### 15.1 达到 100 分需要补齐

```text
1. 管理端参数模板页面
2. 管理端价格规则页面
3. 管理端租户授权页面
4. 管理端路由规则页面
5. 管理端调用计量页面，需确认后端 REST 接口是否完整
6. 用户端 VideoNode 接入 AIGC 模型列表、参数模板、价格预估
7. 用户端生成提交前余额校验
8. 用户端价格预估失败时禁止提交收费任务
9. 环境授权后重新运行 lint/build
```

### 15.2 推荐补齐顺序

```text
1. 管理端参数模板页面
2. 管理端价格规则页面
3. 管理端租户授权页面
4. 用户端 VideoNode 接入
5. 生成前余额校验
6. 全量 lint/build 验证
```
