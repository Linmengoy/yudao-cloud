# yudao-module-aigc-safety 前端开发方案

本文档基于 `c:\use\code\project\manman\yudao-ui\AIGC平台第一阶段用户端前端开发计划.md`、`c:\use\code\project\manman\yudao-module-aigc-safety技术方案.md`、后端模块 `c:\use\code\project\manman\yudao-module-aigc-safety`、`c:\use\code\project\manman\yudao-ui\draw2video-client\design\DESIGN.md` 和 `c:\use\code\project\manman\yudao-ui\draw2video-client\AGENTS.md` 梳理，用于指导 `yudao-module-aigc-safety` 对应的管理端与用户端前端开发。

## 1. 模块定位

`yudao-module-aigc-safety` 是 AIGC 平台的审核风控服务，第一阶段负责敏感词管理、提示词检测、审核记录、人工审核通过、人工审核拒绝和资产审核状态同步。

前端侧分为两类使用场景：

- 管理端：运营和审核人员维护敏感词规则、查询审核记录、处理待审核内容。
- 用户端：不直接调用审核风控服务，由生成服务统一接收用户请求并在后端内部调用 `AigcSafetyApi.checkPrompt` 完成安全检查。

第一阶段目标：

```text
管理端可完成敏感词规则维护和审核记录处理，用户端在生成提交链路中感知安全拦截、待审核、审核拒绝等结果，但不暴露敏感词库和内部审核策略。
```

## 2. 前端项目范围

| 项目 | 路径 | 技术栈 | 建设目标 |
| --- | --- | --- | --- |
| `draw2video-admin` | `c:\use\code\project\manman\yudao-ui\draw2video-admin` | Vue3 + Vite + TypeScript + Element Plus | AIGC 安全审核管理后台 |
| `draw2video-client` | `c:\use\code\project\manman\yudao-ui\draw2video-client` | Next.js + React + TypeScript + Tailwind CSS | 用户创作端安全状态感知与提示 |

## 3. 后端接口依据

### 3.1 管理端接口

管理端接口来自后端 `AigcSensitiveWordController` 和 `AigcAuditRecordController`。前端调用时不写 `/admin-api` 前缀，只写业务路径，由现有 axios 基建统一补齐。

| 模块 | 方法 | 业务路径 | 说明 | 权限点 |
| --- | --- | --- | --- | --- |
| 敏感词管理 | POST | `/aigc/safety/sensitive-word/create` | 新增敏感词 | `aigc:safety-sensitive-word:create` |
| 敏感词管理 | PUT | `/aigc/safety/sensitive-word/update` | 修改敏感词 | `aigc:safety-sensitive-word:update` |
| 敏感词管理 | DELETE | `/aigc/safety/sensitive-word/delete?id=` | 删除敏感词 | `aigc:safety-sensitive-word:delete` |
| 敏感词管理 | GET | `/aigc/safety/sensitive-word/get?id=` | 查询敏感词详情 | `aigc:safety-sensitive-word:query` |
| 敏感词管理 | GET | `/aigc/safety/sensitive-word/page` | 敏感词分页 | `aigc:safety-sensitive-word:query` |
| 敏感词管理 | PUT | `/aigc/safety/sensitive-word/update-status` | 启停敏感词 | `aigc:safety-sensitive-word:update` |
| 审核记录 | GET | `/aigc/safety/audit-record/get?id=` | 查询审核记录详情 | `aigc:safety-audit-record:query` |
| 审核记录 | GET | `/aigc/safety/audit-record/page` | 审核记录分页 | `aigc:safety-audit-record:query` |
| 审核记录 | PUT | `/aigc/safety/audit-record/pass` | 人工审核通过 | `aigc:safety-audit-record:audit` |
| 审核记录 | PUT | `/aigc/safety/audit-record/reject` | 人工审核拒绝 | `aigc:safety-audit-record:audit` |

### 3.2 用户端接口

第一阶段 `yudao-module-aigc-safety` 不直接开放用户端接口。

用户端只通过生成、任务、资产等业务接口间接感知安全结果：

| 用户动作 | 用户端调用模块 | 安全模块介入方式 | 用户端展示 |
| --- | --- | --- | --- |
| 提交文本、图片、视频生成 | `yudao-module-aigc-gen` | 后端内部调用 `checkPrompt` | 生成请求被拦截时展示通用安全提示 |
| 查询任务进度 | `yudao-module-aigc-task` | 任务记录关联审核状态或失败原因 | 展示任务失败、待审核或被拒绝状态 |
| 查看资产列表和详情 | `yudao-module-aigc-asset` | 资产审核状态由 safety 同步 | 展示资产审核中、已通过、未通过 |
| 下载资产 | `yudao-module-aigc-asset` | 资产未通过或不可见时后端拒绝 | 展示不可下载原因 |

用户端禁止直接展示：

- 敏感词完整命中列表
- 敏感词规则编号
- 内部策略权重
- 第三方审核服务配置
- 非当前用户或当前租户对象的审核记录

## 4. 管理端开发方案

### 4.0 当前落地状态

当前已在 `draw2video-admin` 完成 safety 管理端第一版开发：

```text
draw2video-admin/src/api/aigc/safety
  ├── types.ts
  ├── sensitive-word/index.ts
  └── audit-record/index.ts

draw2video-admin/src/views/aigc/safety
  ├── constants.ts
  ├── sensitive-word/index.vue
  ├── sensitive-word/SensitiveWordForm.vue
  └── audit-record/index.vue
```

已落地能力：

- 敏感词分页查询、按词/场景/风险等级/匹配方式/状态/创建时间筛选。
- 敏感词新增、编辑、删除、启用、禁用。
- 审核记录分页查询、按对象类型/对象编号/场景/审核状态/审核结果/风险等级/创建时间筛选。
- 审核记录详情抽屉、命中敏感词 JSON 解析展示。
- 待审核记录人工通过、人工拒绝、拒绝原因必填。
- 操作入口增加 `id` 空值保护，拒绝提交时缓存 `auditId`，避免后续表单状态变化影响刷新详情。

待联调项：

- 需要后端菜单配置后验证动态路由可访问。
- 需要后端真实分页数据验证筛选参数和时间范围格式。
- 需要真实待审核资产记录验证人工通过/拒绝后资产审核状态同步。

### 4.1 菜单规划

```text
AIGC 平台
  └── 安全审核
      ├── 敏感词管理
      └── 审核记录
```

### 4.2 API 目录规划

```text
draw2video-admin/src/api/aigc/safety
  ├── sensitive-word/index.ts
  └── audit-record/index.ts
```

管理端 API 约定：

- 新增使用 `/create`
- 修改使用 `/update`
- 删除使用 `/delete?id=`
- 分页使用 `/page`
- 详情使用 `/get?id=`
- 启停使用 `/update-status`
- 审核操作使用 `/pass` 和 `/reject`

### 4.3 页面目录规划

```text
draw2video-admin/src/views/aigc/safety
  ├── sensitive-word/index.vue
  └── audit-record/index.vue
```

页面组件路径需要与后端菜单返回的组件路径保持一致。

### 4.4 页面功能清单

| 页面 | 核心功能 |
| --- | --- |
| 敏感词管理 | 分页、新增、编辑、删除、启停、按场景筛选、按风险等级筛选、按匹配方式筛选、按状态筛选 |
| 审核记录 | 分页、详情、按对象类型筛选、按对象编号筛选、按场景筛选、按审核状态筛选、按审核结果筛选、按风险等级筛选、人工通过、人工拒绝 |

## 5. 敏感词管理页面

### 5.1 核心字段

```text
id
word
scene
level
matchType
status
remark
createTime
```

### 5.2 查询条件

| 字段 | 控件 | 说明 |
| --- | --- | --- |
| `word` | 输入框 | 支持按敏感词关键字查询 |
| `scene` | Select | 审核场景，第一阶段重点使用 `PROMPT`、`ASSET` |
| `level` | Select 或数字输入 | 风险等级，建议展示 1 到 5 |
| `matchType` | Select | 匹配方式，第一阶段使用 `CONTAINS`、`EXACT` |
| `status` | Select | `ENABLE`、`DISABLE` |
| `createTime` | 日期范围 | 创建时间范围 |

### 5.3 列表展示

| 列 | 字段 | 展示要求 |
| --- | --- | --- |
| 敏感词 | `word` | 普通文本展示，支持较长内容省略 |
| 审核场景 | `scene` | 使用字典 Tag 展示 |
| 风险等级 | `level` | 高风险使用更醒目的 Tag |
| 匹配方式 | `matchType` | `CONTAINS` 展示为包含匹配，`EXACT` 展示为完全匹配 |
| 状态 | `status` | 使用开关或 Tag 展示 |
| 备注 | `remark` | 超长省略 |
| 创建时间 | `createTime` | 使用项目统一时间格式 |
| 操作 | - | 编辑、删除、启用、禁用 |

### 5.4 新增和编辑表单

| 字段 | 控件 | 校验 |
| --- | --- | --- |
| `word` | 输入框 | 必填，前后去空格，不允许空字符串 |
| `scene` | Select | 必填，枚举值来自安全场景 |
| `level` | 数字输入或 Select | 必填，建议限制 1 到 5，最终以后端校验为准 |
| `matchType` | Select | 默认 `CONTAINS`，第一阶段只允许 `CONTAINS`、`EXACT` |
| `status` | Radio 或 Switch | 默认 `ENABLE` |
| `remark` | Textarea | 非必填，限制长度并展示字数 |

### 5.5 交互要求

- 删除敏感词前需要二次确认。
- 禁用敏感词前提示“禁用后新生成请求不再使用该规则检测”。
- 启用敏感词前提示“启用后会影响后续提示词或资产审核”。
- 保存失败时展示后端错误信息，例如重复敏感词、枚举非法、正则暂不支持等。
- `REGEX` 第一阶段不在前端选项中展示，避免用户配置后端不启用的规则。

## 6. 审核记录页面

### 6.1 核心字段

```text
id
objectType
objectId
contentSummary
scene
auditStatus
auditResult
hitWords
riskLevel
rejectReason
auditorUserId
auditTime
createTime
```

### 6.2 查询条件

| 字段 | 控件 | 说明 |
| --- | --- | --- |
| `objectType` | Select | `PROMPT`、`TASK`、`ASSET`，后续预留 `COMMENT`、`POST` |
| `objectId` | 输入框 | 支持按审核对象编号精确查询 |
| `scene` | Select | `PROMPT`、`ASSET`、`TASK` 等 |
| `auditStatus` | Select | `PENDING`、`PASS`、`REJECT` |
| `auditResult` | Select | `AUTO_PASS`、`AUTO_REJECT`、`MANUAL_PASS`、`MANUAL_REJECT` |
| `riskLevel` | Select 或数字输入 | 风险等级 |
| `createTime` | 日期范围 | 创建时间范围 |

### 6.3 列表展示

| 列 | 字段 | 展示要求 |
| --- | --- | --- |
| 审核编号 | `id` | 可点击打开详情抽屉 |
| 对象类型 | `objectType` | 使用 Tag 展示 |
| 对象编号 | `objectId` | 支持复制 |
| 内容摘要 | `contentSummary` | 默认摘要展示，详情中展示更完整内容摘要 |
| 场景 | `scene` | 使用字典 Tag 展示 |
| 审核状态 | `auditStatus` | 待审核、已通过、已拒绝分别使用不同颜色 |
| 审核结果 | `auditResult` | 自动通过、自动拒绝、人工通过、人工拒绝 |
| 风险等级 | `riskLevel` | 高风险突出展示 |
| 审核人 | `auditorUserId` | 第一阶段展示用户编号，后续可联动用户昵称 |
| 审核时间 | `auditTime` | 空值展示 `-` |
| 创建时间 | `createTime` | 使用项目统一时间格式 |
| 操作 | - | 待审核记录展示通过、拒绝；终态记录只展示详情 |

### 6.4 详情抽屉

详情抽屉展示：

- 审核对象类型和对象编号
- 审核场景
- 审核内容摘要
- 审核状态和审核结果
- 命中敏感词摘要
- 风险等级
- 拒绝原因
- 审核人和审核时间
- 创建时间

命中敏感词展示要求：

- 后端返回 `hitWords` 为 JSON 字符串时，前端尝试解析为数组并以 Tag 展示。
- 解析失败时按普通字符串展示，避免页面报错。
- 不支持导出完整命中规则链路。

### 6.5 人工审核通过

入口：

- 审核记录列表操作列
- 审核记录详情抽屉底部操作区

请求：

```text
PUT /aigc/safety/audit-record/pass
```

请求体：

```json
{
  "auditId": 1024,
  "remark": "内容符合平台规范"
}
```

前端要求：

- 仅 `auditStatus = PENDING` 的记录允许展示通过按钮。
- 操作前弹出确认框。
- 操作成功后刷新列表和详情。
- 如果后端返回状态已变更或重复审核错误，提示用户刷新列表。

### 6.6 人工审核拒绝

入口：

- 审核记录列表操作列
- 审核记录详情抽屉底部操作区

请求：

```text
PUT /aigc/safety/audit-record/reject
```

请求体：

```json
{
  "auditId": 1024,
  "reason": "内容不符合平台规范"
}
```

前端要求：

- 仅 `auditStatus = PENDING` 的记录允许展示拒绝按钮。
- 拒绝原因必填，建议提供常用原因快捷选择。
- 提交前提示“拒绝后可能同步资产审核状态并影响用户端可见性”。
- 操作成功后刷新列表和详情。
- 如果资产状态同步失败，由后端记录日志；前端以审核操作结果为准，不自行回滚。

## 7. 字典和枚举规划

建议在管理端补充或复用以下字典：

| 字典 | 值 | 说明 |
| --- | --- | --- |
| `aigc_safety_scene` | `PROMPT`、`ASSET`、`TASK`、`COMMENT`、`POST` | 审核场景 |
| `aigc_audit_object_type` | `PROMPT`、`TASK`、`ASSET`、`COMMENT`、`POST` | 审核对象类型 |
| `aigc_audit_status` | `PENDING`、`PASS`、`REJECT` | 审核状态 |
| `aigc_audit_result` | `AUTO_PASS`、`AUTO_REJECT`、`MANUAL_PASS`、`MANUAL_REJECT` | 审核结果 |
| `aigc_sensitive_word_status` | `ENABLE`、`DISABLE` | 敏感词状态 |
| `aigc_sensitive_word_match_type` | `CONTAINS`、`EXACT` | 敏感词匹配方式 |

如果后端暂未配置字典，前端可先在模块 API 或页面内维护本地常量，但后续应迁移到系统字典，保持管理端筛选、表格 Tag 和表单选项统一。

## 8. 用户端开发方案

### 8.1 用户端定位

用户端不建设独立“安全审核”页面，不展示敏感词管理、审核记录和内部审核策略。`draw2video-client` 是独立 ToC AI 创作工作台，不复制 Yudao 管理后台形态，安全能力必须融入 Copse 的画布、任务、资产和侧边栏工作台体验。

用户端只在以下位置补齐安全状态体验：

- `/create/image` 创作画布提交生成时
- `ImageNode`、`TextNode`、`VideoNode` 的生成状态与错误提示
- `/tasks` 任务列表和 `/tasks/[id]` 任务详情
- `/assets` 资产库和 `/assets/[id]` 资产详情
- 左侧工作台侧边栏中的任务、资产状态入口
- 下载资产、复用资产或回到来源项目画布时

用户端安全体验原则：

- 安全提示是创作流程的一部分，不把用户带到管理式审核页面。
- 安全状态优先贴近节点、任务行、资产卡片展示，减少全局打断。
- 画布仍以直接编辑节点为核心，生成结果继续替换当前 draft 节点，不新增独立审核节点。
- 审核状态不改变 React Flow 连接规则，不引入新的节点类型连接组合。
- 所有文案保持克制、温和、可行动，不暴露具体敏感词和策略细节。

### 8.2 创作提交安全提示

在 `draw2video-client` 的图片、文本、视频生成提交链路中，统一处理生成接口返回的安全类错误。

建议交互：

| 场景 | 展示位置 | 用户提示 | 交互 |
| --- | --- | --- | --- |
| 提示词被安全规则拦截 | 当前节点 composer 下方 | `内容可能不符合平台规范，请调整提示词后重试` | 保留用户输入，聚焦 prompt 输入区 |
| 生成请求进入人工审核 | 当前 draft 节点预览槽 | `内容已提交审核` | 节点进入轻量 waiting 状态，不开始生成耗时计时 |
| 审核拒绝 | 当前 draft 节点预览槽和 composer 下方 | `内容未通过审核，请修改后重新提交` | 保留参数和引用边，允许修改后再次提交 |
| 资产不可见或不可下载 | 资产卡片或详情页操作区 | `该资产暂不可用，请查看审核状态或重新生成` | 禁用下载或复用按钮，保留返回来源项目入口 |

前端不展示具体敏感词，避免用户通过提示反推出规则库。

画布节点要求：

- `ImageNode` 安全拦截后仍保持 draft 节点，不创建 `ResultNode`。
- `TextNode` 安全拦截后不覆盖原文本内容，只在 composer 或节点状态区展示提示。
- `VideoNode` 审核中不进入 running 计时，只有上游任务真正进入 running 后才开始生成耗时。
- 上传的图片和视频节点是 media-only 节点，安全提示不应让它们打开生成 composer。
- 被拒绝节点允许用户继续编辑 prompt、参数和引用关系，然后再次提交。
- 安全状态变化属于系统更新，应尽量避免写入高频历史记录，防止撤销/重做体验混乱。

### 8.3 任务页面安全状态

任务列表和任务详情建议增加安全状态感知：

| 后端状态来源 | 展示方式 |
| --- | --- |
| 任务等待审核 | 状态 Tag 展示 `审核中` |
| 任务审核拒绝 | 状态 Tag 展示 `未通过`，详情展示通用拒绝说明 |
| 任务生成失败且失败原因为安全拦截 | 展示 `内容安全校验未通过` |
| 任务通过审核并继续生成 | 正常展示排队、生成中、成功 |

如果任务接口第一阶段未返回独立审核字段，前端先通过任务状态和失败原因兜底展示，待任务模块补字段后再扩展类型。

任务页交互要求：

- `/tasks` 使用工作台内的紧凑列表或卡片，不使用营销页大卡片视觉。
- 任务行的安全状态使用低饱和 Tag 或小型状态点，不用高饱和警告色铺满区域。
- 任务详情保留来源项目、来源节点、生成参数和费用状态的上下文。
- 审核拒绝时提供“回到画布修改”入口，优先跳转 `/create/image?projectId=<id>` 并定位来源节点。
- 任务失败如涉及冻结释放或退款，由 billing 状态补充展示，不由 safety 前端自行推断。

### 8.4 资产页面安全状态

资产列表和详情建议展示资产审核状态：

| 审核状态 | 列表展示 | 详情展示 | 操作限制 |
| --- | --- | --- | --- |
| 待审核 | `审核中` Tag | 提示资产正在审核 | 禁止公开分享，下载按后端规则处理 |
| 已通过 | `已通过` Tag | 正常展示 | 允许预览、下载、使用 |
| 已拒绝 | `未通过` Tag | 展示通用拒绝原因 | 禁止下载和再次使用 |

资产模块接口应只返回当前用户可见资产的审核状态和安全可用状态，用户端不直接查询 safety 审核记录。

资产库交互要求：

- `/assets` 按 AGENTS.md 要求分开展示 generated images 和 generated videos。
- 上传的参考图、参考视频不作为 generated assets 列入安全审核资产库。
- 资产卡片保留来源项目链接，用户可回到来源画布修改 prompt 或重新生成。
- 审核中资产可以显示占位预览，但不应误导为最终可用结果。
- 未通过资产禁止公开分享和再次使用，是否允许删除以后端资产服务规则为准。
- 资产审核状态 Tag 使用克制中性色，只有不可用操作处用轻量提示解释原因。

### 8.5 客户端错误处理规范

用户端统一在 API client 或生成提交层处理安全错误：

- 401：引导登录，不当作安全错误。
- 403：展示无权限或资产不可用提示。
- 业务错误码：如果 message 包含安全审核含义，统一转成用户友好的通用文案。
- 网络错误：展示网络重试提示，不误判为审核失败。

错误处理落点：

- `src/lib/api-client.ts` 负责基础请求、鉴权和错误归一化。
- 生成提交逻辑负责把安全错误映射到当前 `ImageNode`、`TextNode` 或 `VideoNode`。
- 任务轮询逻辑负责把安全状态映射为 queued、reviewing、failed、succeeded 等 UI 状态。
- 资产操作逻辑负责把不可下载、不可复用、不可分享转换为局部提示。
- 不在前端写 provider key、模型密钥或审核服务密钥。

### 8.6 视觉设计要求

用户端安全提示必须遵循 `DESIGN.md` 的暖色、克制、工具化设计系统：

| 设计项 | 要求 |
| --- | --- |
| 背景 | 使用 `#f7f4ed` 暖奶油色作为页面和卡片基础，不使用纯白大面积背景 |
| 文本 | 使用 `#1c1c1c` 与透明度派生灰阶，正文和说明保持低噪声 |
| 边框 | 卡片、节点状态、资产预览使用 `#eceae4` 细边框，不使用重阴影 |
| 按钮 | 主要操作使用深色按钮和 inset shadow，次要操作使用 outline 或 cream surface |
| 圆角 | 普通按钮和输入 6px，卡片 12px，只有 action pill 和 icon button 使用 `9999px` |
| 动效 | 使用 `motion/react` 的短动效，约 `0.12s` 到 `0.22s`，只作用于节点内部、浮层、状态提示 |
| 图标 | 使用 Lucide icons，所有 icon-only 控件必须有可访问标签或 tooltip |

用户端安全提示不应：

- 使用管理后台式红色大面积告警卡片。
- 使用饱和安全色作为主视觉。
- 在工作台内部放置营销页式大卡片或宣传文案。
- 用弹窗打断每一次安全状态变更。
- 使用重阴影、过圆卡片或多套灰色系统。

### 8.7 浮层和局部提示要求

安全提示可能出现在 composer、参数 popover、任务详情、资产操作菜单和未来账号/钱包菜单中，必须遵循现有浮层规则：

- 浮层支持点击外部关闭。
- 浮层支持 Escape 关闭。
- 执行动作后自动关闭。
- 点击浮层内部不误关闭。
- 节点级 tooltip、context menu 或状态说明使用 viewport 坐标时，应 portal 到 `document.body`。
- 安全提示不阻断 canvas pan、zoom、drag、connect 的基础交互。

### 8.8 React Flow 适配要求

安全状态属于节点业务状态，不改变画布图结构规则：

- 不新增 `safety` 节点类型。
- 不新增 `review -> image`、`review -> video` 等连接类型。
- 审核中、未通过、可重试等状态写入节点 data 的轻量字段。
- 保存 canvas 时不写入大体积审核详情，只保留轻量状态、任务 ID、资产 ID 和展示文案 key。
- Hydration 完成前不要因为安全默认状态覆盖已保存草稿。
- 如果资产缓存缺失，展示 missing-preview，不因为审核状态删除节点和边。
- 安全状态更新不应动画 React Flow 外层 transform，只允许节点内部状态条或预览槽轻量过渡。

### 8.9 用户端目录建议

在不增加过度抽象的前提下，已将 safety 用户端逻辑作为横切能力放在现有模块附近：

```text
draw2video-client/src/features/safety
  ├── safety-copy.ts
  ├── safety-status.ts
  └── safety-ui.tsx
```

职责建议：

- `safety-copy.ts` 维护通用安全提示文案，不包含敏感词和内部策略。
- `safety-status.ts` 维护生成、任务、资产可复用的安全状态映射。
- `safety-ui.tsx` 提供轻量状态 Tag、节点内提示和局部行动提示。
- 具体页面仍在 `features/canvas`、任务页、资产页内组合使用，避免创建脱离业务上下文的大型安全模块。

### 8.10 用户端当前落地状态

当前已在 `draw2video-client` 完成 safety 用户端第一版实际接入：

```text
draw2video-client/src/features/safety
  ├── safety-copy.ts
  ├── safety-status.ts
  └── safety-ui.tsx

draw2video-client/src/features/tasks
  ├── task-types.ts
  ├── components/task-card.tsx
  └── components/task-result.tsx

draw2video-client/src/features/assets
  ├── asset-library.ts
  └── components/asset-status-badge.tsx

draw2video-client/src/app/(app)/assets
  ├── page.tsx
  └── [id]/page.tsx

draw2video-client/src/features/canvas
  ├── types.ts
  ├── use-generation.ts
  └── ImageNode.tsx
```

已落地能力：

- `safety-status.ts` 统一标准化 `idle`、`reviewing`、`blocked`、`rejected`、`available` 状态。
- `normalizeSafetyStatus` 已兼容 `PENDING`、`AUDITING`、`MANUAL_REVIEW`、`PASS`、`REJECT`、`AUTO_REJECT`、`MANUAL_REJECT` 等后端或业务状态。
- `normalizeSafetyStatusFromError` 提供错误文案兜底识别，命中“安全、审核、规范、敏感、违规、不符合”等关键词时转为安全拒绝态。
- `SafetyStatusPill` 用于任务卡片和资产状态徽标。
- `SafetyInlineNotice` 用于任务结果、资产详情和画布图片节点失败层。
- 任务类型 `AigcTask` 已扩展 `safetyStatus`、`auditStatus`、`auditReason`。
- 任务卡片在 `TaskStatusBadge` 旁展示 safety 状态，并在安全类失败时优先展示安全提示。
- 任务结果页在审核中、安全拒绝、安全拦截时优先展示 `SafetyInlineNotice`，普通失败仍使用原失败展示。
- 资产审核徽标复用 safety 状态展示，`PENDING/MANUAL_REVIEW/REJECT/PASS` 统一映射。
- 资产详情在审核中、复审中、未通过时展示局部安全提示，并禁用下载按钮。
- 本地画布资产库 `GeneratedAsset` 已支持透传 `auditStatus` 和 `auditReason`。
- 画布节点类型 `ImageNodeData`、`TextNodeData`、`VideoNodeData` 已扩展 `safetyStatus`、`safetyReason`。
- 图片生成请求结果支持接收 `safetyStatus`、`safetyReason`，图片节点失败态会优先展示安全提示。

暂未落地但已预留的数据契约：

- 视频生成节点 `VideoNode` 已有类型字段，后续可按图片节点模式接入安全提示。
- 文本生成节点 `TextNode` 已有类型字段，当前文本生成仍偏 mock，后续真实接口接入后复用同一套 safety 状态。
- `/app-api/ai/video/**` 和 Wan 代理路由后续应统一返回 `safetyStatus`、`safetyReason`，避免前端解析供应商错误详情。

### 8.11 用户端状态映射规范

| 来源状态或字段 | 标准状态 | 展示组件 | 说明 |
| --- | --- | --- | --- |
| `PENDING` | `reviewing` | `SafetyStatusPill`、`SafetyInlineNotice` | 待审核 |
| `AUDITING` | `reviewing` | `SafetyStatusPill`、`SafetyInlineNotice` | 任务审核中 |
| `MANUAL_REVIEW` | `reviewing` | `SafetyStatusPill`、`SafetyInlineNotice` | 资产人工复审中 |
| `PASS`、`AUTO_PASS`、`MANUAL_PASS` | `available` | `SafetyStatusPill` | 内容已通过 |
| `REJECT`、`MANUAL_REJECT` | `rejected` | `SafetyStatusPill`、`SafetyInlineNotice` | 人工拒绝 |
| `AUTO_REJECT`、`safety_blocked` | `blocked` | `SafetyStatusPill`、`SafetyInlineNotice` | 自动拦截 |
| 错误文案包含安全关键词 | `rejected` | `SafetyInlineNotice` | 后端暂未返回标准字段时的兜底 |

安全状态展示优先级：

```text
safetyStatus > auditStatus > 业务状态 AUDITING > 安全文案兜底 > 普通错误展示
```

前端不得因为命中文案兜底而展示具体敏感词，只展示通用安全提示。

## 9. 与其他 AIGC 模块联动

| 模块 | 前端联动点 | 说明 |
| --- | --- | --- |
| `yudao-module-aigc-gen` | 生成提交错误提示 | safety 由 gen 内部调用，用户端只处理 gen 返回结果 |
| `yudao-module-aigc-task` | 任务审核状态展示 | 任务状态中需要可区分审核中、审核拒绝、生成失败 |
| `yudao-module-aigc-asset` | 资产审核状态展示 | safety 人工审核后同步资产审核状态 |
| `yudao-module-aigc-billing` | 失败退款或释放冻结提示 | 安全拒绝导致的失败需要展示扣费/退款状态 |
| `yudao-module-aigc-model` | 模型选择和参数填写 | safety 不参与模型选择，生成提交前由后端统一校验 |

## 10. 权限与菜单配置

### 10.1 管理端菜单

```text
菜单名称：安全审核
组件路径：aigc/safety

子菜单：敏感词管理
组件路径：aigc/safety/sensitive-word/index
权限：aigc:safety-sensitive-word:query

子菜单：审核记录
组件路径：aigc/safety/audit-record/index
权限：aigc:safety-audit-record:query
```

### 10.2 按钮权限

| 页面 | 操作 | 权限点 |
| --- | --- | --- |
| 敏感词管理 | 新增 | `aigc:safety-sensitive-word:create` |
| 敏感词管理 | 编辑 | `aigc:safety-sensitive-word:update` |
| 敏感词管理 | 删除 | `aigc:safety-sensitive-word:delete` |
| 敏感词管理 | 启停 | `aigc:safety-sensitive-word:update` |
| 审核记录 | 查询 | `aigc:safety-audit-record:query` |
| 审核记录 | 人工通过 | `aigc:safety-audit-record:audit` |
| 审核记录 | 人工拒绝 | `aigc:safety-audit-record:audit` |

按钮使用 `v-hasPermi` 控制展示，后端权限仍作为最终校验。

## 11. 数据类型建议

### 11.1 敏感词类型

```ts
export interface AigcSensitiveWordVO {
  id: number
  word: string
  scene: string
  level: number
  matchType: string
  status: string
  remark?: string
  createTime?: Date
}

export interface AigcSensitiveWordPageReqVO {
  pageNo: number
  pageSize: number
  word?: string
  scene?: string
  level?: number
  matchType?: string
  status?: string
  createTime?: Date[]
}
```

### 11.2 审核记录类型

```ts
export interface AigcAuditRecordVO {
  id: number
  objectType: string
  objectId: number
  contentSummary?: string
  scene: string
  auditStatus: string
  auditResult?: string
  hitWords?: string
  riskLevel?: number
  rejectReason?: string
  auditorUserId?: number
  auditTime?: Date
  createTime?: Date
}

export interface AigcAuditRecordPageReqVO {
  pageNo: number
  pageSize: number
  objectType?: string
  objectId?: number
  scene?: string
  auditStatus?: string
  auditResult?: string
  riskLevel?: number
  createTime?: Date[]
}
```

## 12. 开发步骤

### 12.0 当前开发完成情况

已完成：

- 管理端 P0 API 封装和页面开发。
- 管理端敏感词管理、审核记录管理、人工审核操作。
- 用户端 safety 横切模块。
- 用户端任务卡片、任务结果、资产徽标、资产详情、图片节点失败态接入。
- 管理端 safety 文件编辑器诊断通过。
- 用户端新增/修改范围 lint 通过。

未完成或需联调确认：

- 后端菜单数据配置和动态路由访问验证。
- 后端真实 safety、task、asset 数据联调。
- 视频节点、文本节点真实生成接口接入后的 safety 展示验证。
- 全量工程类型检查仍受仓库既有 pay/system/wms 等模块历史错误影响，需要独立治理。

### 12.1 管理端 P0

1. 新增 `src/api/aigc/safety/sensitive-word/index.ts`。
2. 新增 `src/api/aigc/safety/audit-record/index.ts`。
3. 新增敏感词管理页面，完成分页、查询、新增、编辑、删除、启停。
4. 新增审核记录页面，完成分页、查询、详情、人工通过、人工拒绝。
5. 配置菜单和按钮权限。
6. 联调后端接口和字典枚举。

### 12.2 用户端 P0

1. 在生成提交链路统一处理安全拦截错误。
2. 在任务列表和详情中预留审核状态展示。
3. 在资产列表和详情中预留审核状态展示。
4. 下载或使用资产失败时展示资产不可用提示。
5. 文案统一为通用安全提示，不展示敏感词和内部规则。

### 12.3 P1 优化

1. 审核记录详情支持跳转到关联任务或资产。
2. 审核拒绝弹窗增加常用拒绝原因模板。
3. 敏感词管理增加批量导入和批量启停。
4. 审核记录增加风险等级统计卡片。
5. 用户端资产详情展示更清晰的审核说明和重新生成入口。

### 12.4 P2 后续

1. 接入第三方审核服务结果展示。
2. 支持社区评论和帖子审核。
3. 支持审核工作台队列、分配和审核效率统计。
4. 支持更细粒度的内容安全申诉和复审流程。

## 13. 验收标准

### 13.1 管理端验收

- 敏感词列表可按词、场景、风险等级、匹配方式、状态、创建时间筛选。
- 敏感词可新增、编辑、删除、启用、禁用。
- 新增和编辑表单不允许选择 `REGEX`。
- 审核记录列表可按对象类型、对象编号、场景、审核状态、审核结果、风险等级、创建时间筛选。
- 待审核记录可人工通过，成功后状态更新为通过。
- 待审核记录可人工拒绝，拒绝原因必填，成功后状态更新为拒绝。
- 非待审核记录不展示通过和拒绝操作。
- 所有按钮受权限点控制。

### 13.2 用户端验收

- 生成提交被安全校验拦截时，用户端展示通用安全提示。
- 用户端不展示敏感词完整命中列表和内部规则。
- 任务审核中、审核拒绝、生成失败可被用户识别。
- 资产审核中、已通过、未通过可被用户识别。
- 未通过或不可用资产下载失败时有明确提示。

### 13.3 当前验证记录

已执行：

```text
npm run lint -- --quiet src/features/safety src/features/tasks/components/task-card.tsx src/features/tasks/components/task-result.tsx src/features/assets/components/asset-status-badge.tsx src/features/assets/asset-library.ts src/app/(app)/assets/page.tsx src/app/(app)/assets/[id]/page.tsx src/features/canvas/use-generation.ts src/features/canvas/ImageNode.tsx
```

结果：用户端新增/修改范围 lint 通过。

已执行：

```text
pnpm ts:check 2>&1 | Select-String -Pattern "aigc/safety|features/safety|task-card|task-result|asset-status-badge|asset-library|ImageNode|use-generation"
```

结果：管理端全量 `ts:check` 仍因仓库既有历史错误返回非 0，但过滤确认无 `aigc/safety` 相关错误。

已执行编辑器诊断：

- `draw2video-admin/src/views/aigc/safety/audit-record/index.vue`
- `draw2video-admin/src/views/aigc/safety/sensitive-word/index.vue`
- `draw2video-client/src/features/tasks/components/task-card.tsx`
- `draw2video-client/src/features/tasks/components/task-result.tsx`
- `draw2video-client/src/features/canvas/ImageNode.tsx`
- `draw2video-client/src/app/(app)/assets/[id]/page.tsx`
- `draw2video-client/src/features/assets/asset-library.ts`
- `draw2video-client/src/features/safety/safety-status.ts`

结果：上述文件无诊断问题。

## 14. 风险与注意事项

- 用户端不得直接调用 `/admin-api/aigc/safety/**`。
- 管理端不得把 `/admin-api` 写入 API 文件路径。
- 审核详情只展示审核所需摘要，不扩展展示内部策略权重和规则链路。
- 审核通过和拒绝存在并发处理，前端需要处理后端返回的状态已变化错误。
- 资产审核状态同步由后端事务提交后处理，前端不自行推断资产最终状态。
- 安全类用户提示应避免明确指出具体敏感词，降低规则被绕过的风险。
- 用户端当前通过标准字段优先、错误文案兜底的方式识别安全状态；后端联调完成后，应优先使用 `safetyStatus`、`auditStatus`、`auditReason`，减少对错误文案的依赖。
- 资产详情已禁用审核中、复审中、未通过资产的下载按钮；后端仍必须在下载接口做最终权限和审核状态校验。
