# yudao-module-aigc-asset 前端开发方案

## 1. 方案定位

`yudao-module-aigc-asset` 是 AIGC 平台统一资产中心，前端建设分为管理端资产运营与用户端资产消费两条线。

- 管理端面向运营、审核、客服和技术排障，重点处理资产分页检索、预览、审核、可见性调整、软删除恢复、下载日志和资产统计。
- 用户端面向普通创作者，重点处理我的资产列表、资产详情、预览、下载、删除、可见性设置和生成结果资产化展示。
- 资产中心只展示和管理文件型结果，包括图片、视频、音频、文档、PPT、字幕、封面、数字人视频等。
- 文本生成结果第一阶段优先从任务中心读取 `outputText` 或 `outputData`，不强制进入资产中心。
- 前端不直接调用对象存储底层能力，上传和访问 URL 由 `infra` 文件服务与资产模块共同提供。
- 用户端只展示当前用户有权访问的资产，不展示其它用户隐私字段、内部审核操作记录、第三方来源细节和平台运营备注。
- 用户端不是 Yudao 管理后台的换皮页面，应保持 Copse 独立 ToC 创作产品定位，围绕 `/create/image` 画布、项目库和资产库形成轻量创作工作台。

### 1.1 当前落地状态

截至当前开发版本，用户端资产中心已在 `draw2video-client` 落地到可构建状态：

- `/assets` 已接入真实用户端资产分页接口 `/aigc/asset/my-page`。
- `/assets/[id]` 已接入真实用户端资产详情接口 `/aigc/asset/my-get`。
- `/assets` 已改为分页增量加载：首屏只请求一页，滚动接近底部后自动加载下一页，末尾显示“已加载全部”，避免一次性拉取全部资产造成性能压力。
- `/assets` 图片墙使用 Muuri 紧凑瀑布流展示，按真实图片/视频比例决定跨列宽度，并随容器宽度自动切换列数；列表卡片外框、元数据信息和圆角已移除，突出原图内容。
- `/assets` 搜索输入已增加防抖，查询参数优先走分页接口服务端过滤，不再对全量资产做前端一次性检索。
- `/assets` 已按图片来源拆分生成图和上传图：生成图片请求 `assetType=IMAGE&sourceType=GENERATE`，上传图片请求 `assetType=IMAGE&sourceType=UPLOAD`。
- `/assets/[id]` 已支持资产预览、下载、删除、标题/描述/标签编辑、可见性调整、审核状态展示和来源任务跳转。
- 已补齐真实接口不可用时的本地画布生成资产兜底，兜底资产会从 IndexedDB 回查图片和视频大媒体。
- 已统一审核和资产状态规则：仅 `PASS` 且非 `DELETED/DISABLED` 的资产允许下载，只有 `NORMAL + PASS` 的资产允许公开化。
- 已修复图片生成异常时节点永久 pending 的问题，生成异常会回写 `failed`、错误信息、完成时间和耗时。
- 已通过 `npm run lint` 和 `npm run build`。

## 2. 用户端产品与设计约束

### 2.1 Copse 产品约束

`draw2video-client` 是独立 Next.js 用户端应用，资产中心需要服务创作者的生成结果管理，而不是复刻管理后台表格体验。

- 未登录用户优先停留在营销页，触发资产、任务、钱包等工作台能力时打开登录弹窗，避免生硬路由跳转。
- 登录后主导航依赖左侧 workspace sidebar，`/assets` 是侧边栏资产入口，不新增顶部 Tab 导航。
- `/create/image` 是核心产品表面，图片和视频生成结果应在画布节点内原地替换，并提供进入资产详情的轻量入口。
- `/assets` 是资产库，不应变成通用文件管理器；第一阶段重点展示生成图片和生成视频，音频、文档等能力按后端资产类型兼容但不作为核心视觉重点。
- 上传参考图不等同于生成资产；除非用户明确保存或后端创建资产，资产库不应混入临时参考素材。
- 资产行或资产卡片应尽量能回到来源项目画布，后端字段不足时先通过 `taskId` 回到任务详情，再由任务详情关联生成上下文。

### 2.2 Copse 视觉约束

用户端资产页遵循 `draw2video-client/design/DESIGN.md` 的暖色、克制、实用视觉方向。

- 页面背景使用暖奶油色体系，避免纯白大面积背景。
- 字体层级保持克制，正文和按钮以 400 为主，标题最高使用 600，不使用过重字重。
- 卡片和容器用 `#eceae4` 一类暖色边框建立层次，避免重投影和强浮层感。
- 卡片圆角以 8px、12px、16px 为主，矩形按钮使用 6px，只有图标按钮、筛选胶囊、状态切换使用全圆角。
- 资产库内部不要使用营销页大卡片视觉，工作台内应更安静、紧凑、可读。
- 图片和视频容器统一使用 12px 圆角和细边框，缩略图不做过度装饰。
- 主按钮使用深色按钮和内嵌阴影，次级操作使用 outline 或 ghost，危险操作保持低干扰但确认弹窗文案明确。
- Lucide icons 用于图标按钮，所有纯图标操作必须有可访问标签或 tooltip。

### 2.3 浮层和交互约束

- 筛选弹层、更多操作菜单、可见性菜单、删除确认、预览弹层都需要支持外部点击关闭、Escape 关闭、操作完成关闭。
- 浮层内部点击不能误关闭，列表卡片上的菜单不能影响卡片主点击进入详情。
- 工作台交互动效使用 `motion/react` 时保持短促实用，适合 opacity、scale、局部宽高变化，不给列表和画布外层 transform 做复杂动画。
- 资产卡片、预览区、操作菜单应保证键盘可达，焦点状态使用项目既有 focus ring 或暖色阴影规则。

## 3. 后端能力依据

### 3.1 模块边界

资产模块负责资产元数据、文件信息、来源关系、可见性、审核状态、下载计数、使用计数、软删除和下载日志。

不属于资产模块的能力：

- 模型、渠道、参数、价格归属 `yudao-module-aigc-model`。
- 任务状态机、任务进度、失败退款状态归属 `yudao-module-aigc-task`。
- 第三方生成调用归属 `yudao-module-aigc-gen`。
- 钱包、冻结、扣费、退款归属 `yudao-module-aigc-billing`。
- 敏感词、内容审核策略归属 `yudao-module-aigc-safety`。
- 文件上传、文件存储、文件访问 URL 归属 `yudao-module-infra`。

### 3.2 核心字段

| 字段 | 说明 | 前端用途 |
| ---- | ---- | -------- |
| `id` | 资产 ID | 详情、下载、删除、可见性调整、任务结果跳转 |
| `assetNo` | 资产编号 | 管理端检索、客服查询、用户详情展示 |
| `userId` | 资产归属用户 | 管理端筛选和排障，用户端不作为筛选项暴露 |
| `assetType` | 资产类型 | 决定图片、视频、音频、文档等预览方式 |
| `sourceType` | 来源类型 | 区分 AIGC 生成、上传、导入、编辑、克隆 |
| `bizType` | 业务类型 | 区分任务、项目、社区、模板、工作流、发布导出 |
| `bizId` | 业务对象 ID | 关联项目、社区内容、模板等业务对象 |
| `taskId` | 来源任务 ID | 跳转任务详情、生成结果追溯 |
| `taskNo` | 来源任务编号 | 用户端来源展示、管理端排障 |
| `modelId` | 模型 ID | 管理端筛选和排障，用户端可展示模型名称映射 |
| `providerId` | 渠道商 ID | 仅管理端排障展示 |
| `title` | 标题 | 列表卡片、详情标题、用户编辑 |
| `description` | 描述 | 详情展示、用户编辑 |
| `tags` | 标签 | 详情展示、后续筛选预留 |
| `fileUrl` | 文件访问 URL | 预览、下载、复制链接 |
| `originUrl` | 原始外部 URL | 仅管理端排障展示 |
| `coverUrl` | 封面 URL | 视频、音频、文档卡片封面 |
| `thumbnailUrl` | 缩略图 URL | 列表卡片性能优化 |
| `mimeType` | MIME 类型 | 预览组件选择和下载文件名辅助 |
| `fileExt` | 文件扩展名 | 文件类型展示和下载文件名辅助 |
| `fileSize` | 文件大小 | 列表和详情格式化展示 |
| `width` | 宽度 | 图片、视频元信息展示 |
| `height` | 高度 | 图片、视频元信息展示 |
| `duration` | 时长 | 视频、音频时长展示 |
| `metadata` | 扩展元数据 | 管理端 JSON 查看，用户端按需展示摘要 |
| `visibility` | 可见性 | 私有、公开、链接、租户内可见状态展示和切换 |
| `auditStatus` | 审核状态 | 待审、通过、拒绝、人工复审状态展示 |
| `auditReason` | 审核原因 | 用户端展示拒绝原因，管理端填写审核原因 |
| `status` | 资产状态 | 正常、已删除、已禁用展示和操作判断 |
| `viewCount` | 预览次数 | 管理端统计展示，用户端详情可展示 |
| `downloadCount` | 下载次数 | 列表和详情展示 |
| `useCount` | 使用次数 | 管理端统计，用户端可展示复用次数 |
| `createTime` | 创建时间 | 排序和展示 |

### 3.3 枚举展示

| 枚举 | 值 | 用户端文案 | 管理端文案 |
| ---- | -- | ---------- | ---------- |
| `assetType` | `IMAGE` | 图片 | 图片 |
| `assetType` | `VIDEO` | 视频 | 视频 |
| `assetType` | `AUDIO` | 音频 | 音频 |
| `assetType` | `DOCUMENT` | 文档 | 文档 |
| `assetType` | `PPT` | PPT | PPT |
| `assetType` | `SUBTITLE` | 字幕 | 字幕 |
| `assetType` | `COVER` | 封面 | 封面 |
| `assetType` | `DIGITAL_HUMAN_VIDEO` | 数字人视频 | 数字人视频 |
| `assetType` | `OTHER` | 其它 | 其它 |
| `sourceType` | `GENERATE` | AI 生成 | AIGC 生成 |
| `sourceType` | `UPLOAD` | 手动上传 | 用户上传 |
| `sourceType` | `IMPORT` | 导入 | 外部导入 |
| `sourceType` | `EDIT` | 编辑产生 | 编辑产生 |
| `sourceType` | `CLONE` | 克隆产生 | 克隆产生 |
| `visibility` | `PRIVATE` | 仅自己可见 | 仅本人可见 |
| `visibility` | `PUBLIC` | 公开 | 公开可见 |
| `visibility` | `LINK` | 链接可见 | 链接可见 |
| `visibility` | `TENANT` | 团队内可见 | 租户内可见 |
| `auditStatus` | `PENDING` | 审核中 | 待审核 |
| `auditStatus` | `PASS` | 已通过 | 审核通过 |
| `auditStatus` | `REJECT` | 未通过 | 审核拒绝 |
| `auditStatus` | `MANUAL_REVIEW` | 人工复审中 | 人工复审 |
| `status` | `NORMAL` | 正常 | 正常 |
| `status` | `DELETED` | 已删除 | 已删除 |
| `status` | `DISABLED` | 已禁用 | 已禁用 |

## 4. 管理端方案

### 4.1 页面范围

| 页面 | 路由建议 | 说明 |
| ---- | -------- | ---- |
| 资产列表 | `/aigc/asset/asset` | 查询全量资产，支持用户、类型、来源、审核、可见性、状态和标题筛选 |
| 资产详情 | 列表抽屉或 `/aigc/asset/asset/detail/:id` | 展示文件预览、元数据、来源任务、模型、统计和审核信息 |
| 下载日志 | `/aigc/asset/download-log` | 查询资产下载记录，支持按资产、下载用户、归属用户和结果筛选 |
| 资产统计 | `/aigc/asset/statistics` | 展示资产总数、下载总数，并预留类型分布和趋势图 |

管理端建议落在 `draw2video-admin` 的 AIGC 菜单下，页面目录为 `src/views/aigc/asset`，接口目录为 `src/api/aigc/asset`。接口 URL 不写 `/admin-api` 前缀，保持现有管理端请求封装统一处理。

### 4.2 资产列表

- 筛选项：用户 ID、资产类型、来源类型、审核状态、可见性、资产状态、标题。
- 列表字段：缩略图、资产编号、标题、用户 ID、资产类型、来源类型、审核状态、可见性、状态、下载次数、使用次数、创建时间。
- 行操作：详情、预览、审核、调整可见性、删除、恢复、下载或复制 URL。
- 删除操作是软删除，需要二次确认；恢复只在已删除状态展示。
- 审核通过、审核拒绝、调整可见性需要操作成功后刷新列表和详情抽屉。
- 图片和视频优先展示 `thumbnailUrl` 或 `coverUrl`，缺失时回退 `fileUrl`。

### 4.3 资产详情

- 基础信息：资产 ID、资产编号、标题、描述、标签、用户 ID、创建时间、状态。
- 文件信息：文件 URL、封面 URL、缩略图 URL、MIME 类型、扩展名、大小、宽高、时长。
- 来源信息：来源类型、业务类型、业务 ID、任务 ID、任务编号、模型 ID、渠道商 ID。
- 统计信息：预览次数、下载次数、使用次数、最近使用时间。
- 审核信息：审核状态、审核原因、可见性、禁用或删除状态。
- 扩展信息：`metadata`、`promptSnapshot`、`generateSnapshot` 使用 JSON 查看器展示。

### 4.4 预览策略

| 资产类型 | 预览方式 | 兜底处理 |
| -------- | -------- | -------- |
| 图片 | 图片查看器，支持放大、下载、复制链接 | 加载失败展示文件图标和下载按钮 |
| 视频 | HTML5 video，优先封面占位 | 浏览器不支持时展示下载按钮 |
| 音频 | HTML5 audio | 加载失败展示下载按钮 |
| 文档 | 新窗口打开或 iframe 预览 | 不支持预览时展示下载按钮 |
| PPT | 新窗口打开或下载 | 不支持预览时展示下载按钮 |
| 字幕 | 文本预览或下载 | 编码异常时展示下载按钮 |
| 其它 | 文件卡片 | 仅支持下载和复制链接 |

### 4.5 下载日志

- 筛选项：资产 ID、下载用户 ID、资产归属用户 ID、下载结果。
- 列表字段：资产编号、资产 ID、下载用户 ID、归属用户 ID、下载 URL、下载结果、失败原因、客户端 IP、User-Agent、Referer、创建时间。
- `downloadUrl` 可脱敏或折叠展示，避免列表过宽和泄露长签名 URL。
- User-Agent、Referer 放在详情弹窗中展示，列表只展示摘要。

### 4.6 资产统计

- 第一阶段按后端已提供字段展示资产总数和下载总数。
- 若后端后续扩展统计接口，前端可补充资产类型分布、审核状态分布、近 7 日新增资产、近 7 日下载趋势。
- 统计卡片可嵌入 AIGC 运营看板，也可保留资产中心独立统计页。

### 4.7 管理端接口

| 能力 | 方法 | 接口 | 权限标识 |
| ---- | ---- | ---- | -------- |
| 创建资产 | POST | `/aigc/asset/create` | `aigc:asset:create` |
| 更新资产 | PUT | `/aigc/asset/update` | `aigc:asset:update` |
| 删除资产 | DELETE | `/aigc/asset/delete?id={id}` | `aigc:asset:delete` |
| 恢复资产 | PUT | `/aigc/asset/recover?id={id}` | `aigc:asset:update` |
| 资产详情 | GET | `/aigc/asset/get?id={id}` | `aigc:asset:query` |
| 资产分页 | GET | `/aigc/asset/page` | `aigc:asset:query` |
| 更新审核 | PUT | `/aigc/asset/audit` | `aigc:asset:audit` |
| 更新可见性 | PUT | `/aigc/asset/visibility` | `aigc:asset:update` |
| 下载日志分页 | GET | `/aigc/asset/download-log/page` | `aigc:asset:download-log:query` |
| 资产统计 | GET | `/aigc/asset/statistics` | `aigc:asset:query` |
| 导出 Excel | GET | `/aigc/asset/export-excel` | `aigc:asset:export` |

## 5. 用户端方案

### 5.1 页面范围

| 页面 | 路由建议 | 说明 |
| ---- | -------- | ---- |
| 我的资产 | `/assets` | 工作台侧边栏资产入口，展示生成图片和生成视频，兼容其它文件型资产 |
| 资产详情 | `/assets/[id]` | 展示预览、文件信息、来源任务、审核状态、下载和删除操作 |
| 生成结果入口 | 创作页内嵌 | 图片、视频、音频等生成成功后提供查看资产入口 |
| 任务详情入口 | `/tasks/[id]` | 任务成功且存在 `outputAssetId` 时跳转资产详情 |

用户端资产能力纳入 `draw2video-client`，符合现有 Next.js App Router、React、TypeScript、Tailwind CSS、React Query、Lucide icons、motion/react 和统一 `api-client` 约定。未登录访问资产页时优先打开登录弹窗，独立登录页只作为刷新、移动端和兜底路径，登录成功后回到原地址。

### 5.2 我的资产列表

- 默认按创建时间倒序分页展示当前用户资产，禁止一次性拉取全部资产。
- 顶部提供紧凑筛选胶囊：全部、生成图片、上传图片、生成视频、其它文件；不使用顶部大 Tab。
- 图片资产不再把上传图和生成图混在一起。生成图片使用 `assetType=IMAGE&sourceType=GENERATE`；上传图片使用 `assetType=IMAGE&sourceType=UPLOAD`。
- 加号资产选择弹窗可从生成图片或上传图片中选择参考图，选中后只作为当前生成请求的参考输入，不改变生成结果资产类型。
- 搜索输入需要防抖，优先通过分页接口的标题参数请求服务端过滤；不要为了搜索把全部资产预加载到前端。
- 列表采用 Muuri 紧凑瀑布流图片墙，按容器宽度动态计算列数，窗口变窄时自然降为多列、两列或单列。
- 图片和视频使用全尺寸可见预览，不裁剪、不套外层卡片、不显示圆角边和元数据信息；点击媒体进入资产详情。
- 图片/视频加载完成后读取真实宽高比，横图可跨 2 列，超宽横图可跨更多列，但布局必须使用最矮连续列放置以减少上下对齐空隙。
- 滚动接近底部时自动加载下一页，加载中显示“加载更多...”，最后一页后显示“已加载全部”。
- 刷新、下拉刷新、切换分类、搜索条件变化都应重置到第一页重新请求。
- 审核拒绝资产展示拒绝原因入口，禁用资产隐藏下载按钮。
- 资产库不展示上传参考媒体，除非后端已明确创建资产并返回 `sourceType=UPLOAD` 且产品决定进入素材库。
- 当前实现中，真实接口失败但用户仍有 token 时，会降级扫描本地项目画布生成记录；若 token 已失效，则不展示本地资产并提示重新登录。
- 本地降级资产会保留 `localProjectId` 和 `localNodeId`，卡片点击和“打开项目”入口回到 `/create/image?projectId=<id>`。

### 5.3 资产详情

- 顶部展示标题、资产编号、类型、审核状态、可见性和创建时间。
- 主区域按资产类型展示图片、视频、音频或文件预览，图片和视频容器保持暖色边框、12px 圆角和克制背景。
- 侧栏展示文件大小、格式、宽高、时长、来源任务、下载次数、使用次数。
- 存在 `taskId` 时提供查看任务入口，跳转 `/tasks/[taskId]`。
- 支持编辑标题、描述、标签，提交 `PUT /aigc/asset/update`。
- 支持调整可见性，提交 `PUT /aigc/asset/visibility`。
- 支持下载，调用 `POST /aigc/asset/download` 后使用返回 URL 下载或打开。
- 支持删除，调用 `DELETE /aigc/asset/delete?id={id}`。
- 来源项目入口优先回到 `/create/image?projectId=<id>`，当前后端字段不足时先展示来源任务入口。
- 从 `PRIVATE` 扩大到 `PUBLIC`、`LINK`、`TENANT` 时必须二次确认，避免误公开。
- 审核未通过、待审核、人工复审、已删除或已禁用资产不能下载；未审核通过或非正常状态资产不能公开化。

### 5.4 上传资产

- 第一阶段上传资产作为 P1 能力，优先服务参考图、用户上传素材和项目素材沉淀。
- 上传流程：选择文件、调用 `/app-api/infra/file/upload`、拿到文件 URL、调用 `/aigc/asset/upload` 创建资产元数据。
- 上传前校验文件类型和大小，图片、视频、音频提取宽高或时长后尽量写入资产元数据。
- 上传成功后跳转资产详情或追加到当前素材列表。
- 上传失败需要区分文件服务失败和资产元数据创建失败；如果资产创建失败，前端提示用户重试，不在前端删除已上传文件。
- 上传文件不直接写入本地项目 metadata，大媒体仍遵循现有 IndexedDB 和轻量 metadata 分离策略。

### 5.5 生成结果资产化

- 图片、视频、音频生成成功后，生成服务或任务服务负责创建资产并回写 `outputAssetId`。
- 创作页收到任务成功结果后优先读取 `outputAssetId`，在当前 ImageNode 或 VideoNode 内保留原地替换体验，并提供“查看资产”“下载结果”“复制链接”。
- 任务详情页存在 `outputAssetId` 时跳转 `/assets/[id]`。
- 资产详情页存在 `taskId` 时跳转 `/tasks/[taskId]`。
- 文本生成结果不进入资产中心时，仍在任务详情或创作节点中展示文本输出。
- 画布节点中不新增复杂资产管理面板，只提供轻量入口，完整编辑和删除放在资产详情页。
- 当前图片画布节点一次只承载一张生成图，因此前端将图片生成数量限制为 `1`，避免后端返回多张但只落第一张造成资产丢失。
- `ImageNode` 调用生成接口异常时会写回失败状态，避免节点永久显示生成中。
- 生成服务把 provider 返回的 `data:` URL 转存为文件资产时，文件名必须唯一，至少包含 `generateNo`、`taskId` 或雪花 ID。禁止所有生成图共用 `IMAGE生成资产.png` 这类固定文件名，否则不同 `assetId` 会拥有相同 `fileUrl` 并在文件服务中互相覆盖。
- 同一 sketch 连接多个 ImageNode 且 prompt 不同时，应创建不同生成记录、不同资产 ID 和不同 `fileUrl`；前端展示结果以 `previewUrl/outputPreviewUrl` 为准，不应复用旧节点 URL。

### 5.6 用户端接口

| 能力 | 方法 | 接口 | 说明 |
| ---- | ---- | ---- | ---- |
| 我的资产分页 | GET | `/aigc/asset/my-page?pageNo={pageNo}&pageSize={pageSize}&assetType=&sourceType=` | 查询当前登录用户资产分页 |
| 我的资产详情 | GET | `/aigc/asset/my-get?id={id}` | 查询当前登录用户有权访问的资产详情 |
| 上传资产 | POST | `/aigc/asset/upload` | 创建当前用户上传资产元数据 |
| 更新资产 | PUT | `/aigc/asset/update` | 更新标题、描述和标签 |
| 更新可见性 | PUT | `/aigc/asset/visibility` | 更新当前用户资产可见性 |
| 删除资产 | DELETE | `/aigc/asset/delete?id={id}` | 删除当前用户自己的资产 |
| 下载资产 | POST | `/aigc/asset/download` | 记录下载日志并返回可下载 URL |
| 标记使用 | POST | `/aigc/asset/use?id={id}` | 增加资产使用次数 |
| 文件上传 | POST | `/infra/file/upload` | 上传文件到底层文件服务 |

用户端接口以后端 app Controller 为准，不应使用管理端 `/aigc/asset/page` 和 `/aigc/asset/get`。现有第一阶段总计划中旧写法 `/aigc/asset/page`、`/aigc/asset/get` 需要在实施时修正为 `/aigc/asset/my-page`、`/aigc/asset/my-get`。

## 6. 前端工程落地

### 6.1 管理端目录建议

```text
draw2video-admin/src/api/aigc/asset/
  ├── asset.ts
  ├── download-log.ts
  ├── statistics.ts
  └── types.ts

draw2video-admin/src/views/aigc/asset/
  ├── asset/index.vue
  ├── asset/detail.vue
  ├── download-log/index.vue
  └── statistics/index.vue
```

### 6.2 用户端目录建议

```text
draw2video-client/src/lib/aigc-api/
  ├── asset.ts
  └── types.ts

draw2video-client/src/features/assets/
  ├── components/asset-card.tsx
  ├── components/asset-preview.tsx
  ├── components/asset-actions.tsx
  ├── components/asset-meta-panel.tsx
  ├── components/asset-status-badge.tsx
  ├── hooks/use-assets.ts
  ├── hooks/use-asset-detail.ts
  ├── asset-api.ts
  ├── asset-dictionaries.ts
  └── asset-types.ts

draw2video-client/src/app/(app)/assets/
  ├── page.tsx
  └── [id]/page.tsx
```

如果继续沿用当前 `draw2video-client` 的 `src/features/*/*-api.ts` 组织，也可以将用户端 API 放在 `src/features/assets/asset-api.ts`。关键是底层必须复用统一 `src/lib/api-client.ts`，避免重复处理 token、租户、terminal、401 刷新和错误结构。

当前实现采用 `src/features/assets/asset-api.ts` 组织用户端资产接口，页面和组件文件如下：

```text
draw2video-client/src/app/(app)/assets/
  ├── page.tsx                 已实现，我的资产列表
  └── [id]/page.tsx            已实现，我的资产详情

draw2video-client/src/features/assets/
  ├── asset-api.ts             已实现，真实资产接口封装
  ├── asset-types.ts           已实现，资产类型定义
  ├── asset-dictionaries.ts    已实现，枚举文案、格式化、权限规则
  ├── asset-library.ts         已增强，本地画布资产兜底扫描
  └── components/
      ├── asset-preview.tsx
      └── asset-status-badge.tsx
```

### 6.3 类型封装建议

| 类型 | 说明 |
| ---- | ---- |
| `AigcAsset` | 资产响应模型，对应后端资产响应字段 |
| `AigcAssetType` | 资产类型联合类型 |
| `AigcAssetSourceType` | 来源类型联合类型 |
| `AigcAssetBizType` | 业务类型联合类型 |
| `AigcAssetVisibility` | 可见性联合类型 |
| `AigcAssetAuditStatus` | 审核状态联合类型 |
| `AigcAssetStatus` | 资产状态联合类型 |
| `AigcAssetPageParams` | 管理端和用户端分页查询参数 |
| `AigcAssetUpdateReq` | 用户端更新标题、描述、标签请求 |
| `AigcAssetUploadReq` | 用户端上传资产元数据请求 |
| `AigcAssetDownloadReq` | 下载请求参数 |
| `AigcAssetDownloadLog` | 管理端下载日志响应 |
| `AigcAssetStatistics` | 管理端资产统计响应 |

当前用户端 `AigcAsset` 额外保留本地兜底字段：

| 字段 | 说明 |
| ---- | ---- |
| `localProjectId` | 本地画布兜底资产所属项目 ID，用于回到 `/create/image?projectId=<id>` |
| `localNodeId` | 本地画布兜底资产所属节点 ID，用于后续定位节点预留 |

### 6.4 React Query Key 建议

```text
['aigc-assets', params]
['aigc-asset', id]
['aigc-asset-download', assetId]
['aigc-asset-statistics']
['aigc-asset-download-logs', params]
```

资产更新、删除、可见性变更、下载成功后，需要失效资产列表和资产详情缓存。上传成功后需要失效资产列表缓存。

当前实现暂未引入 React Query，沿用页面内 `useState + useEffect + api-client` 的轻量实现，原因是资产中心第一阶段页面状态简单，且项目中任务页已采用同类模式。后续若统一远端缓存策略，可迁移到上述 Query Key。

### 6.4.1 本地兜底资产发现机制

当前本地兜底机制用于真实资产接口不可用但用户仍处于登录态时，展示历史画布生成结果：

```text
/assets 请求 /aigc/asset/my-page
  ├── 成功：展示真实资产
  ├── 失败且 token 已失效：提示重新登录，不展示本地资产
  └── 失败但 token 仍存在：扫描本地项目画布草稿
        ├── 从 localStorage 读取轻量 nodes
        ├── 图片按 imageId 从 IndexedDB images store 回查 dataUrl
        ├── 视频按 videoId 从 IndexedDB videos store 回查 videoUrl/blob
        └── 转成只读 fallback asset，点击回到来源项目画布
```

实现文件：

```text
src/features/assets/asset-library.ts
src/features/canvas/image-store.ts
src/features/canvas/use-canvas-storage.ts
```

该机制遵循 Copse 画布持久化规则：大媒体不写入 localStorage，只在需要展示资产库兜底时从 IndexedDB 恢复。

### 6.4.2 资产操作规则函数

当前用户端将下载和公开规则集中在 `asset-dictionaries.ts`：

| 函数 | 规则 | 使用场景 |
| ---- | ---- | -------- |
| `canDownloadAsset` | `status` 不是 `DELETED/DISABLED` 且 `auditStatus=PASS` | 列表页和详情页下载按钮 |
| `canPublishAsset` | `status=NORMAL` 且 `auditStatus=PASS` | 详情页可见性公开化 |

列表页和详情页必须复用同一规则，避免出现列表能下载、详情不能下载的状态不一致。

### 6.5 组件职责

| 组件 | 职责 |
| ---- | ---- |
| `AssetCard` | 列表卡片，展示封面、标题、类型、审核状态、可见性和快捷操作 |
| `AssetPreview` | 根据资产类型选择图片、视频、音频、文档或文件兜底预览 |
| `AssetActions` | 下载、删除、复制链接、编辑、调整可见性 |
| `AssetMetaPanel` | 展示大小、格式、尺寸、时长、来源任务、统计信息 |
| `AssetStatusBadge` | 审核状态、可见性、资产状态标签 |
| `AssetEditForm` | 标题、描述、标签编辑表单 |
| `AssetUploadDialog` | P1 上传资产弹窗 |

### 6.6 用户端组件风格建议

| 组件 | 风格要求 |
| ---- | -------- |
| `AssetCard` | 暖色背景、细边框、12px 圆角，不使用重阴影，操作区保持紧凑 |
| `AssetPreview` | 图片和视频容器使用固定边界与温和背景，视频草稿和封面避免布局跳动 |
| `AssetActions` | 主要操作使用深色按钮，次级操作使用 ghost 或 outline，图标按钮提供 tooltip |
| `AssetStatusBadge` | 低饱和状态标签，避免大面积强色块，审核拒绝可用克制红色文本提示 |
| `AssetFilterBar` | 使用胶囊按钮或紧凑 segmented control，不使用管理后台式大筛选表单 |
| `AssetMoreMenu` | 浮层挂载到合适容器，支持外部点击、Escape 和操作完成关闭 |

## 7. 交互和状态规则

### 7.1 下载规则

- 用户点击下载时先调用 `/aigc/asset/download`，由后端校验权限、记录日志并返回下载 URL。
- 前端不直接用列表中的 `fileUrl` 做下载计数，避免绕过下载日志。
- 下载成功后刷新当前资产详情的 `downloadCount`。
- 下载失败展示后端错误提示，常见场景包括无权限、资产不存在、资产已删除、资产已禁用、文件不可访问。
- 前端下载按钮需要先经过 `canDownloadAsset` 判断：只有 `auditStatus=PASS` 且 `status` 不是 `DELETED/DISABLED` 的资产可点击。

### 7.2 删除规则

- 用户端删除仅允许删除自己的资产。
- 管理端删除为软删除，恢复只对已删除资产展示。
- 删除前弹出二次确认，明确提示删除后列表不再展示。
- 删除成功后返回上一页或刷新列表。

### 7.3 可见性规则

- 用户端默认创建资产建议为 `PRIVATE`。
- 用户端可从 `PRIVATE` 切换到 `PUBLIC` 或 `LINK`，`TENANT` 是否开放取决于产品是否存在团队或租户协作概念。
- 管理端可调整任意资产可见性，并需要记录操作提示。
- 审核未通过或禁用资产即使为公开可见，也不应在用户端公开场景展示。
- 用户端只有 `status=NORMAL` 且 `auditStatus=PASS` 的资产允许从私有扩大到公开、链接或团队可见。
- 从 `PRIVATE` 扩大到 `PUBLIC`、`LINK`、`TENANT` 时需要二次确认。

### 7.4 审核状态规则

- `PENDING`：用户端展示“审核中”，下载是否允许以后端权限为准。
- `PASS`：用户端展示“已通过”，允许正常预览和下载。
- `REJECT`：用户端展示“未通过”，详情页展示 `auditReason`，隐藏公开分享入口。
- `MANUAL_REVIEW`：用户端展示“人工复审中”，管理端可审核通过或拒绝。

### 7.5 预览权限规则

- 我的资产详情直接展示完整预览。
- 链接可见和公开可见场景如果后续开放分享页，需要单独设计匿名访问权限，不复用 `/my-get`。
- 管理端预览允许查看全量资产，但敏感原始 URL 和元数据应折叠展示。

## 8. 与其它模块联动

### 8.1 与任务中心联动

- 任务列表和任务详情中存在 `outputAssetId` 时展示“查看资产”。
- 资产详情中存在 `taskId` 时展示“查看任务”。
- 生成任务失败、退款中、已退款状态不直接创建资产入口。

### 8.2 与生成中心联动

- 文生图、文生视频、图生视频生成成功后，由后端将文件转存到文件服务并创建资产。
- 前端生成节点只消费任务结果和资产 ID，不自己拼装资产元数据入库。
- 如果前端上传参考图，参考图可作为 P1 上传资产化能力，也可以第一阶段仅作为临时文件 URL 传给生成接口。
- 图片生成节点当前一次只保留一张结果，参数面板将数量限制为 `1`；如后续支持批量生成，需要为每张结果创建独立资产或独立画布节点。
- 图片生成接口异常必须回写节点失败状态，避免画布节点永久停留在 `pending`。
- 资产排障时若多个生成结果画面完全相同，应同时检查 `gen_db.aigc_gen_record.prompt/client_request_id/provider_task_id/asset_ids` 和 `asset_db.aigc_asset.file_url`。若 prompt 与 task 均不同但 `file_url` 相同，优先判断为文件名覆盖问题，而不是 prompt 未生效。

### 8.3 与钱包计费联动

- 资产详情不展示扣费明细，只展示来源任务。
- 用户查看扣费、冻结、退款应跳转钱包流水或任务详情。
- 资产下载第一阶段不单独收费；如果后续下载收费，需要由计费模块提供价格和扣费接口。

### 8.4 与安全审核联动

- 审核状态来自资产模块或安全审核模块回写。
- 用户端仅展示审核结论和拒绝原因，不展示敏感词命中、风险等级、人工审核人等内部字段。
- 管理端资产审核操作可与安全审核记录互相跳转，但第一阶段可以先保留独立审核入口。

## 9. 实施顺序

### 9.1 管理端实施顺序

```text
1. 新增资产字典、类型和 API 封装
2. 新增资产列表页和分页筛选
3. 新增图片、视频、音频、文档预览组件
4. 新增资产详情抽屉或详情页
5. 接入审核和可见性调整
6. 接入软删除和恢复
7. 新增下载日志页
8. 新增资产统计页
9. 补齐权限按钮和菜单配置
10. 完成导出、错误提示、空状态和加载状态
```

### 9.2 用户端实施顺序

```text
1. 新增资产类型、字典和 API 封装
2. 接入 /assets 我的资产列表
3. 接入 /assets/[id] 我的资产详情
4. 实现 AssetPreview 多类型预览
5. 实现下载、删除、复制链接
6. 实现标题、描述、标签编辑
7. 实现可见性展示和调整
8. 将任务详情 outputAssetId 跳转到资产详情
9. 将创作页生成成功结果跳转到资产详情
10. P1 接入上传资产能力
```

当前用户端 P0 已完成：

```text
1. 新增资产类型、字典和 API 封装
2. 接入 /assets 我的资产列表
3. 接入 /assets/[id] 我的资产详情
4. 实现 AssetPreview 多类型预览
5. 实现下载、删除、来源任务跳转
6. 实现标题、描述、标签编辑
7. 实现可见性展示、扩大可见性确认和状态规则限制
8. 将任务详情 outputAssetId 跳转到资产详情
9. 实现本地画布生成资产兜底发现
10. 修复 ImageNode 生成异常失败回写
```

## 10. 验收标准

### 10.1 管理端验收

- 资产列表可以按用户、类型、来源、审核状态、可见性、状态和标题筛选。
- 图片、视频、音频、文档可以按类型正确预览或兜底下载。
- 资产详情展示文件信息、来源信息、统计信息、审核信息和扩展 JSON。
- 审核通过、审核拒绝、调整可见性、软删除、恢复均可正常操作。
- 下载日志可以分页查看，长 URL、User-Agent、Referer 不撑破表格。
- 资产统计展示资产总数和下载总数。
- 权限按钮生效，接口错误有明确提示。
- 管理端不泄露未脱敏密钥和第三方敏感凭据。

### 10.2 用户端验收

- 未登录访问 `/assets` 和 `/assets/[id]` 会触发登录流程，登录后回到原页面。
- 用户可看到自己的真实资产列表，不看到其它用户私有资产。
- 用户可进入资产详情并按类型预览图片、视频、音频或文件。
- 用户可通过下载按钮完成下载，并触发后端下载计数和下载日志。
- 用户可删除自己的资产，删除后列表和详情状态正确刷新。
- 用户可编辑标题、描述和标签。
- 用户可查看和调整资产可见性。
- 审核拒绝资产展示拒绝原因，不提供公开分享入口。
- 任务详情和生成成功结果可以跳转到资产详情。
- `/assets` 保持工作台侧边栏导航体验，不出现管理后台式顶部 Tab 或营销大卡片。
- 资产卡片和详情页符合暖色背景、细边框、克制圆角、低阴影的 Copse 视觉规范。
- 所有图标按钮、更多菜单、筛选浮层、确认弹窗具备可访问标签、外部点击关闭和 Escape 关闭能力。
- 用户端不展示 `providerId`、`originUrl`、内部审核记录、第三方回调和平台运营备注。
- 真实资产接口失败但登录态仍有效时，资产库可展示本地画布生成资产兜底；登录态失效时不展示本地资产并提示重新登录。
- 本地兜底资产可以回到来源项目画布，不出现无效 `#` 链接。
- 审核中、人工复审、审核拒绝、已删除、已禁用资产不能下载。
- 只有审核通过且状态正常的资产可以公开化；从私有扩大可见范围需要二次确认。
- 图片生成异常不会让画布节点永久 pending。
- 当前单节点图片生成数量固定为 1，避免多结果丢失。

### 10.3 工程验收

- `draw2video-client` 通过 lint 和 build。
- `draw2video-admin` 通过类型检查和 build。
- 新增 TypeScript 类型完整，无大量 `any` 逃逸。
- API 封装统一复用现有请求客户端和错误处理。
- 页面具备加载中、空状态、错误状态和无权限状态。
- 涉及 Next.js App Router 改动时，先参考项目当前 Next.js 版本文档和现有目录约定。
- 用户端新增交互动效只使用 `motion/react` 做局部短动效，不影响 React Flow 节点拖拽和画布 transform。
- 前端不硬编码 `/admin-api`、`/app-api` 之外由请求封装管理的前缀。
- 前端不硬编码密钥、供应商 token、签名 URL 生成逻辑。

当前验证结果：

```text
npm run lint   通过
npm run build  通过
```

## 11. 第一阶段交付物

管理端交付物：

- AIGC 资产列表页
- AIGC 资产详情和多类型预览
- 资产审核和可见性调整
- 资产软删除和恢复
- 资产下载日志页
- 资产统计页
- 资产 API TypeScript 封装
- 资产枚举字典和状态标签
- 菜单和权限按钮配置说明

用户端交付物：

- `/assets` 我的资产列表
- `/assets/[id]` 我的资产详情
- 图片、视频、音频、文档预览能力
- 资产下载、删除、复制链接
- 标题、描述、标签编辑
- 可见性展示和调整
- 任务结果到资产详情跳转
- 创作页生成成功资产入口
- 用户端资产 API TypeScript 封装

最终上线标准：

```text
用户端能完成：生成任务成功 → 获得资产 → 查看资产列表 → 进入资产详情 → 预览 / 下载 / 删除资产 → 从资产追溯来源任务
```
