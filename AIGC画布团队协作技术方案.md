# AIGC 画布团队协作与共享邀请技术方案

## 1. 背景与目标

当前 draw2video 用户端画布基于 React Flow 实现，节点、边、视口、撤销重做、自动保存等能力主要集中在 `draw2video-client/src/app/(app)/create/image/page.tsx`，画布轻量数据保存在 localStorage，图片和视频等大对象保存在 IndexedDB。该方案适合单人本地草稿，但无法支撑类似 Figma 的多人实时协作。

本方案目标是在尽量复用现有 React Flow 画布、Yudao WebSocket 基础设施、AIGC 资产与任务模块的前提下，将画布升级为服务端权威文档，并通过 WebSocket 实现多人实时协作。

核心目标：

- 多人进入同一个画布项目后，可以实时看到节点新增、移动、删除、连线、属性编辑等操作。
- 支持在线成员、头像光标、选中状态、正在编辑状态等 presence 能力。
- 服务端保存画布权威状态，本地缓存只作为加速和离线兜底。
- 图片、视频、生成结果等媒体资源统一进入资产服务，节点只保存资产 ID、版本 ID、预览 URL 和来源关系。
- 图片、文本、视频生成任务由服务端统一编排，任务进度和节点状态通过 WebSocket 推送。
- 第一阶段不全量引入 CRDT，先采用服务端版本号、节点级 operation log、WebSocket 房间广播实现可落地协作。

## 2. 当前项目基础

### 2.1 用户端画布

- 画布入口：`draw2video-client/src/app/(app)/create/image/page.tsx`
- 画布引擎：`@xyflow/react`
- 节点类型：图片节点、文本节点、视频节点、旧版 prompt/result 节点
- 类型定义：`draw2video-client/src/features/canvas/types.ts`
- 本地画布存储：`draw2video-client/src/features/canvas/use-canvas-storage.ts`
- 本地项目元数据：`draw2video-client/src/features/projects/project-store.ts`
- 大对象缓存：`draw2video-client/src/features/canvas/image-store.ts`

### 2.2 生成任务

- 生成 API 封装：`draw2video-client/src/features/generation/generation-api.ts`
- 生成结果轮询：`draw2video-client/src/features/generation/generation-poll.ts`
- 任务进度轮询：`draw2video-client/src/features/tasks/hooks/use-task-progress.ts`

当前前端节点直接创建任务并轮询结果。协作改造后，推荐改为前端提交“节点执行请求”，后端工作流调度任务，任务状态再回写节点并推送给画布房间。

### 2.3 后端实时通信

后端已有 Yudao WebSocket starter，可复用以下能力：

- WebSocket handler 自动注册
- 登录态握手拦截
- session 管理
- JSON 消息按 type 分发
- 按用户、用户类型、session 发送消息
- 支持 local、Redis、MQ 等消息发送实现

画布协作需要在此基础上增加“项目房间”概念，并按 `projectId` 做成员隔离和广播。

## 3. 总体架构

### 3.1 架构原则

- 服务端是画布权威状态来源，客户端只做乐观更新和缓存。
- 所有会影响画布结构的数据变更必须抽象为 operation。
- presence 状态不入库，只通过 WebSocket 临时广播。
- 媒体大文件不写入画布 JSON，只通过资产 ID 引用。
- 用户编辑操作和系统任务操作分离，但都可以最终表现为节点状态 patch。
- 先实现对象级协作，再按需引入 Yjs 处理文本或富文本的细粒度协作。

### 3.2 分层设计

```text
客户端 React Flow 画布
  -> useCanvasOperations 生成标准 operation
  -> 本地乐观更新 nodes/edges
  -> useCanvasRealtime 发送 WebSocket 消息
  -> 服务端鉴权、冲突处理、版本递增
  -> 默认路径同步落 MySQL；启用 Redis 热日志后先写 Redis，再由 Worker 周期落盘
  -> WebSocket 广播已确认 operation
  -> 其他客户端按版本顺序应用 operation
```

系统分层：

- 画布渲染层：React Flow、自定义节点、边、连接规则、选择框、视口。
- 前端协作层：WebSocket 连接、operation 队列、幂等去重、断线重连、presence。
- 服务端协作层：房间管理、成员鉴权、operation 校验、广播、版本控制。
- 服务端存储层：项目、成员、快照、操作日志、资产引用。
- AIGC 编排层：文本、图片、视频任务调度、计费、审核、结果入库、节点状态推送。

### 3.3 生产存储分层目标

逻辑表名以下按 `canvas_*` 描述；落到当前代码库时继续映射到已有的 `aigc_canvas_*` 表前缀。当前实现仍兼容小型 snapshot 直接写入 MySQL JSON 字段，生产目标按以下分层收敛：

MySQL：

- `canvas_project`：项目主元数据、封面资产 ID、当前版本、最新快照 ID、节点数和素材数等可检索字段。
- `canvas_snapshot` 元数据：只保存快照版本、存储位置、对象 key、大小、hash、创建人和创建时间等索引信息；大体积 snapshot body 不作为主存储放在 MySQL。
- `canvas_operation_log`：最终持久化 operation，用于审计、恢复、差量同步和快照压缩。
- `canvas_asset_ref`：节点与资产的稳定引用关系，用于权限校验、封面推导、素材统计和私有 URL 刷新。

OSS / MinIO：

- 大体积 snapshot JSON：节点、边、viewport、必要的轻量节点数据以 JSON 或压缩 JSON 对象保存。
- 历史 snapshot 包：按项目、时间或版本范围归档旧 snapshot body 和已压缩 operation，降低在线表膨胀。

Redis：

- 协作房间热状态：在线成员、热版本、当前画布热状态、短期增量索引等。
- pending operation / presence：未确认或待落盘操作、幂等键、光标/选区/在线状态等临时协作数据。

Snapshot 内联/对象存储判定策略：

- 服务端先生成已清洗的 snapshot body：剥离 `previewUrl`、`outputPreviewUrl`、`videoUrl`、`assetUrlExpireTime`、`dataUrl`、`blob:` 等运行时或大媒体字段，只保留节点、边、viewport 和稳定资产 ID。
- 默认内联 MySQL 条件：清洗后的 UTF-8 JSON body `<= 512KB`，节点数 `<= 200`，边数 `<= 500`，且单个节点 data JSON `<= 64KB`。
- 只要任一条件超过阈值，就写 OSS / MinIO，并在 `canvas_snapshot` 写入 `storage_type`、`snapshot_object_key`、`snapshot_size`、`snapshot_hash` 等元数据。
- 清洗后的 JSON body `>= 2MB` 时强制 OSS / MinIO，不允许通过配置回退到 MySQL 内联，避免大 JSON 字段影响在线查询、备份和恢复。
- 历史 snapshot 包默认进入 OSS / MinIO；MySQL 只保留最新可用 snapshot 元数据和必要的索引字段。
- 阈值做成配置项，例如 `canvas.snapshot.inlineMaxBytes`、`canvas.snapshot.inlineMaxNodes`、`canvas.snapshot.inlineMaxEdges`、`canvas.snapshot.inlineMaxNodeBytes`，便于压测后调整。

## 4. 数据模型设计

第 4 章到第 20 章默认描述当前同步 MySQL 路线；第 21 章为生产增强路线。启用 Redis 热日志后，operation 的实时接收、版本分配、pending 队列和短期补拉由 Redis 承担，MySQL 表作为最终持久化、恢复和审计来源；大体积 snapshot JSON 进入 OSS / MinIO，`canvas_snapshot` 只保留元数据和兼容字段。

### 4.1 canvas_project

保存画布项目主信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 项目 ID |
| tenant_id | bigint | 租户 ID |
| owner_user_id | bigint | 创建者用户 ID |
| name | varchar | 项目名称 |
| cover_asset_id | bigint | 封面资产 ID |
| current_version | bigint | 当前画布版本号 |
| latest_snapshot_id | bigint | 最新快照 ID |
| status | tinyint | 状态：正常、归档、删除 |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

### 4.2 canvas_member

保存项目成员和角色。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 成员记录 ID |
| project_id | bigint | 项目 ID |
| user_id | bigint | 用户 ID |
| role | varchar | owner/editor/viewer |
| invite_user_id | bigint | 邀请人 |
| joined_time | datetime | 加入时间 |
| last_active_time | datetime | 最后活跃时间 |

### 4.3 canvas_snapshot

保存画布快照元数据，用于初始化、恢复和操作日志压缩。当前小型快照可继续兼容 `nodes_json`、`edges_json`、`viewport_json` 直接落 MySQL；当快照体积变大或进入生产多项目协作时，应把完整 snapshot JSON 写入 OSS / MinIO，MySQL 只保存对象位置和校验信息。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 快照 ID |
| project_id | bigint | 项目 ID |
| version | bigint | 快照对应版本 |
| storage_type | varchar | `INLINE` / `OSS` / `MINIO`，标记快照 body 存储位置 |
| snapshot_object_key | varchar | OSS / MinIO 中的大体积 snapshot JSON 对象 key |
| snapshot_size | bigint | 快照 body 字节数，用于阈值判断和排障 |
| snapshot_hash | varchar | 快照 body hash，用于完整性校验和重复归档判断 |
| nodes_json | json/text | 兼容字段：小型快照或旧数据的 React Flow 节点列表 |
| edges_json | json/text | 兼容字段：小型快照或旧数据的 React Flow 边列表 |
| viewport_json | json/text | 兼容字段：小型快照或旧数据的视口状态 |
| created_by | bigint | 创建人，系统快照可为空 |
| create_time | datetime | 创建时间 |

### 4.4 canvas_operation_log

保存最终持久化后的画布结构变更。默认路径中 operation 实时写入该表；启用 Redis 热日志后，实时 operation 先进入 Redis Stream，再由 Worker 批量写入该表。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 操作日志 ID |
| project_id | bigint | 项目 ID |
| client_id | varchar | 客户端实例 ID |
| op_id | varchar | 客户端生成的操作 ID |
| actor_user_id | bigint | 操作用户 ID |
| base_version | bigint | 客户端提交时基于的版本 |
| next_version | bigint | 服务端应用后的版本 |
| operation_type | varchar | 操作类型 |
| operation_json | json/text | 操作内容 |
| inverse_operation_json | json/text | 可选，用于撤销 |
| create_time | datetime | 创建时间 |

唯一索引建议：

- `uk_project_client_op(project_id, client_id, op_id)`，用于幂等去重。
- `uk_project_next_version(project_id, next_version)`，用于保证版本单调。

### 4.5 canvas_asset_ref

保存画布节点与资产之间的引用关系。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | bigint | 引用 ID |
| project_id | bigint | 项目 ID |
| node_id | varchar | 节点 ID |
| asset_id | bigint | 资产 ID |
| usage_type | varchar | input/output/cover/reference |
| source_task_id | bigint | 来源任务 ID |
| create_time | datetime | 创建时间 |

## 5. Operation 协议设计

### 5.1 统一消息信封

客户端提交 operation 时使用统一信封。

```json
{
  "type": "canvas-op",
  "projectId": "p_123",
  "clientId": "client_abc",
  "opId": "op_001",
  "baseVersion": 21,
  "timestamp": 1710000000000,
  "operation": {
    "type": "NODE_MOVE",
    "payload": {
      "nodeId": "node_1",
      "position": {
        "x": 100,
        "y": 200
      }
    }
  }
}
```

服务端应用成功后广播：

```json
{
  "type": "canvas-op-applied",
  "projectId": "p_123",
  "clientId": "client_abc",
  "opId": "op_001",
  "actorUserId": 1001,
  "baseVersion": 21,
  "version": 22,
  "operation": {
    "type": "NODE_MOVE",
    "payload": {
      "nodeId": "node_1",
      "position": {
        "x": 100,
        "y": 200
      }
    }
  }
}
```

服务端拒绝时返回：

```json
{
  "type": "canvas-op-rejected",
  "projectId": "p_123",
  "clientId": "client_abc",
  "opId": "op_001",
  "reason": "PERMISSION_DENIED",
  "message": "当前用户没有编辑权限",
  "serverVersion": 22
}
```

### 5.2 第一阶段 Operation 类型

| 类型 | 说明 | 是否最终持久化 |
| --- | --- | --- |
| NODE_CREATE | 新增节点 | 是 |
| NODE_DELETE | 删除节点及关联边 | 是 |
| NODE_MOVE | 移动节点 | 是 |
| NODE_RESIZE | 节点尺寸变化 | 是 |
| NODE_UPDATE_DATA | 更新节点业务数据 | 是 |
| EDGE_CREATE | 新增连线 | 是 |
| EDGE_DELETE | 删除连线 | 是 |
| ASSET_ATTACH | 节点绑定资产 | 是 |
| ASSET_DETACH | 节点解绑资产 | 是 |
| TASK_STATUS_PATCH | 服务端任务状态回写节点 | 是 |

视口位置、鼠标位置、选中状态不作为持久化 operation。启用 Redis 热日志后，表中的“是”表示最终由 Worker 落入 MySQL，不代表 WebSocket ACK 时已经完成 MySQL 落盘。

### 5.3 Presence 消息

presence 只用于临时协作体验，不写入 operation log。

```json
{
  "type": "canvas-presence",
  "projectId": "p_123",
  "userId": 1001,
  "clientId": "client_abc",
  "cursor": {
    "x": 500,
    "y": 300
  },
  "selectedNodeIds": ["node_1"],
  "editingNodeId": "node_1",
  "viewport": {
    "x": 0,
    "y": 0,
    "zoom": 1
  }
}
```

以下状态只走 presence，不入库：

- 鼠标位置
- 用户头像位置
- 当前选中节点
- 正在编辑的节点
- 当前视口位置
- 属性面板展开状态
- 临时拖拽中的预览位置

## 6. 冲突处理策略

第一阶段采用“服务端顺序化 + 节点级最后写入 + 字段级合并”的策略。

### 6.1 通用规则

- 服务端按项目维度串行应用 operation，保证版本号单调递增。
- 每个 operation 必须携带 `baseVersion`，但第一阶段不要求 `baseVersion` 必须等于服务端当前版本。
- 服务端根据当前状态重新校验 operation 是否仍可应用。
- 应用成功后生成版本并广播。默认路径中该版本为 `nextVersion`；启用 Redis 热日志后广播版本为 `hotVersion`，落盘后再推进 `persistedVersion`。
- 应用失败时返回 reject，客户端回滚对应乐观更新或重新拉取状态。

### 6.2 类型级冲突规则

| 场景 | 处理策略 |
| --- | --- |
| 两个用户同时移动同一节点 | 后到服务端的 NODE_MOVE 覆盖前一次位置 |
| 一个用户删除节点，另一个用户编辑该节点 | 删除先到则编辑失败；编辑先到则删除继续生效 |
| 两个用户编辑同一节点不同字段 | 字段级合并 |
| 两个用户编辑同一节点同一字段 | 后到服务端的字段值生效 |
| 重复创建同一边 | 服务端去重，只保留一条 |
| 删除不存在的边 | 幂等成功或返回 NOOP |
| 给已删除节点添加连线 | 拒绝操作 |
| 任务状态更新已删除节点 | 不回写节点，只保留任务日志 |

### 6.3 文本节点协作

短文本字段第一阶段使用 `NODE_UPDATE_DATA` 后写覆盖。

如果后续要支持多人同时编辑同一个长文本或富文本节点，建议只对文本内容字段引入 Yjs 文档，而不是把整个画布都改成 CRDT。

## 7. 断线重连与可靠性

### 7.1 客户端状态

客户端需要维护：

- `clientId`：浏览器标签页级唯一 ID。
- `lastAppliedVersion`：当前已应用到本地画布的服务端版本。默认路径中对应 `nextVersion`；启用 Redis 热日志后对应已应用的 `hotVersion`，保存状态需额外依赖 `persistedVersion` 或服务端 checkpoint。
- `pendingOperations`：已本地乐观更新但未被服务端确认的操作。
- `appliedOperationIds`：已处理操作 ID 集合，用于去重。

### 7.2 重连流程

```text
WebSocket 断开
  -> 客户端继续允许本地编辑或进入只读模式，由产品决定
  -> 本地操作进入 pendingOperations
  -> WebSocket 重连
  -> 发送 canvas-join(projectId, lastAppliedVersion)
  -> 服务端返回 lastAppliedVersion 之后的 missed operations
  -> 客户端按 version 顺序应用 missed operations
  -> 客户端补发 pendingOperations
  -> 服务端逐个确认或拒绝
  -> 如果差量日志缺失，服务端返回最新 snapshot
```

启用 Redis 热日志后，missed operations 由 MySQL 持久日志和 Redis 热日志按版本拼接返回，`toVersion` 对应当前可同步的 `hotVersion`。

### 7.3 差量同步消息

```json
{
  "type": "canvas-join",
  "projectId": "p_123",
  "clientId": "client_abc",
  "lastAppliedVersion": 21
}
```

服务端返回差量：

```json
{
  "type": "canvas-sync-delta",
  "projectId": "p_123",
  "fromVersion": 22,
  "toVersion": 30,
  "persistedVersion": 28,
  "hotVersion": 30,
  "snapshotVersion": 20,
  "operations": []
}
```

服务端返回快照：

```json
{
  "type": "canvas-sync-snapshot",
  "projectId": "p_123",
  "version": 30,
  "nodes": [],
  "edges": [],
  "viewport": {
    "x": 0,
    "y": 0,
    "zoom": 1
  }
}
```

### 7.4 幂等机制

- 客户端每个 operation 必须生成唯一 `opId`。
- 服务端使用 `projectId + clientId + opId` 做幂等去重。
- 默认路径通过 MySQL 唯一键防重复；启用 Redis 热日志后，热路径先查 Redis 幂等键，落盘时继续依赖 MySQL 唯一键防重复。
- 如果收到重复 operation，服务端不重复应用，直接返回第一次应用后的结果；启用 Redis 热日志后，返回内容至少包含 `hotVersion` 和当前持久化状态。
- 客户端收到自己提交的 `canvas-op-applied` 后，从 pending 队列移除对应操作；如果 UI 要显示“已保存”，必须等待 `persisted` 状态或服务端 checkpoint。

## 8. 撤销重做设计

多人协作下不能简单回滚全局 snapshot，否则会覆盖其他用户的操作。推荐采用“反向 operation”。

### 8.1 原则

- 撤销只撤销当前用户自己的已确认操作。
- 撤销不是回退版本，而是生成一个新的 operation。
- 重做同理，也是生成新的 operation。
- 删除类操作要保存足够的 inverse 数据。

### 8.2 示例

| 原操作 | 撤销操作 |
| --- | --- |
| NODE_CREATE | NODE_DELETE |
| NODE_DELETE | NODE_CREATE，payload 包含被删除节点和关联边快照 |
| NODE_MOVE | NODE_MOVE 到旧 position |
| NODE_UPDATE_DATA | NODE_UPDATE_DATA 到旧字段值 |
| EDGE_CREATE | EDGE_DELETE |
| EDGE_DELETE | EDGE_CREATE |

### 8.3 前端历史策略

- 未确认操作可以保留本地临时 history。
- 已 applied 的操作可以进入协作 history；需要显示已保存状态时必须等待 persisted。
- 协作 undo 优先从当前用户已确认操作中寻找可撤销操作，数据来源包括 Redis 热日志和 MySQL operation log；如果产品只允许撤销已持久化操作，需要显式限制为 persisted 操作。
- 如果目标节点已经被他人删除，撤销操作应失败并提示状态已变化。

## 9. 权限与安全

### 9.1 角色权限

| 操作 | owner | editor | viewer |
| --- | --- | --- | --- |
| 查看画布 | 是 | 是 | 是 |
| 编辑节点/边 | 是 | 是 | 否 |
| 执行生成任务 | 是 | 是 | 否 |
| 上传/删除资产 | 是 | 是，删除受限 | 否 |
| 邀请成员 | 是 | 可配置 | 否 |
| 修改项目名称 | 是 | 可配置 | 否 |
| 删除项目 | 是 | 否 | 否 |

### 9.2 安全要求

- WebSocket 握手必须校验登录态。
- 每条 operation 都必须校验项目成员权限，不能只在 join 时校验。
- projectId 房间必须做租户隔离，防止跨租户消息泄漏。
- 资产访问 URL 需要遵守现有文件服务权限策略。
- 生成任务必须在后端进行计费、余额校验、内容安全审核。
- 服务端不能信任客户端传入的 `actorUserId`，应从登录态获取。
- operation payload 需要做大小限制和字段白名单校验。

### 9.3 共享邀请与成员管理

共享邀请是画布协作能力的用户可见入口，本质上属于同一条协作链路：底层继续复用 `projectId`、`canvas_member`、snapshot、operation log 和 WebSocket 项目房间，不重新设计同步协议。

核心目标：

- 在画布页面右上角增加“共享”入口。
- 支持查看当前画布项目成员。
- 支持通过 userId 邀请用户加入当前画布项目，后续升级为手机号、邮箱、昵称搜索。
- 支持设置成员角色：owner、editor、viewer。
- 支持复制协作链接，被邀请用户通过 `/create/image?projectId=xxx` 打开同一个画布项目。
- 支持成员变更后的实时权限刷新，被降级用户立即只读，被移除用户立即退出或进入无权限状态。
- 支持租户边界校验、成员操作审计、敏感操作二次确认和基础防滥用策略。

第一版产品形态：

```text
共享画布

协作链接：
[ /create/image?projectId=123 ] [复制链接]

邀请成员：
[ 用户 ID 输入框 ] [角色选择 editor/viewer ] [邀请]

当前成员：
头像 / 用户名 / 角色 / 操作
- 张三 owner
- 李四 editor [改为 viewer] [移除]
- 王五 viewer [改为 editor] [移除]
```

第一版简化策略：

- 先用 userId 邀请，不做手机号、邮箱、昵称搜索。
- 角色只支持 editor、viewer，不能直接邀请 owner。
- owner 不允许被修改和移除，第一版不做所有权转移。
- 只有 owner 可以邀请、移除、修改角色。
- editor 可以复制链接，但不能管理成员。
- viewer 只能查看成员和复制链接，不能提交编辑 operation。
- 移除成员、将 editor 降级为 viewer 必须二次确认。
- 成员变更后通过 WebSocket 通知所有在线端刷新成员和权限。

成员管理 REST API：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/app-api/canvas/projects/{projectId}/members` | 查询成员列表 |
| POST | `/app-api/canvas/projects/{projectId}/members` | 邀请成员 |
| PUT | `/app-api/canvas/projects/{projectId}/members/{memberId}` | 修改成员角色 |
| DELETE | `/app-api/canvas/projects/{projectId}/members/{memberId}` | 移除成员 |
| GET | `/app-api/canvas/member-candidates?keyword=xxx` | 第二阶段用户搜索邀请 |

成员类型建议：

```ts
export type CanvasProjectRole = "owner" | "editor" | "viewer"

export interface CanvasMember {
  id: number
  projectId: number
  userId: number
  nickname?: string
  avatar?: string
  role: CanvasProjectRole
  joinedTime?: string
  lastActiveTime?: string
}

export interface InviteCanvasMemberRequest {
  userId: number
  role: Exclude<CanvasProjectRole, "owner">
}

export interface UpdateCanvasMemberRoleRequest {
  role: Exclude<CanvasProjectRole, "owner">
}
```

前端接入点：

- 在 `canvas-api.ts` 增加 `getProjectMembers`、`inviteProjectMember`、`updateProjectMemberRole`、`removeProjectMember`。
- 新增 `CanvasShareDialog.tsx`，负责展示协作链接、复制链接、查询成员、邀请成员、修改角色、移除成员和权限控制。
- 在 `page.tsx` 增加 `shareDialogOpen` 状态，并在右上角在线协作状态附近接入“共享”按钮。
- 使用 `window.location.origin` 和服务端数字 `projectId` 生成共享链接。
- 当 `projectId` 是本地 `project_*` 草稿时禁用共享、成员管理和 WebSocket join。

成员更新实时通知：

```json
{
  "type": "canvas-member-updated",
  "projectId": "10001",
  "operatorUserId": 20001,
  "targetUserId": 20002,
  "action": "role-updated"
}
```

`action` 可选值：

```text
member-added
role-updated
member-removed
```

建议增加强制退出消息：

```json
{
  "type": "canvas-member-kicked",
  "projectId": "10001",
  "targetUserId": 20002,
  "reason": "member-removed"
}
```

前端处理规则：

- 收到 `member-added`：刷新成员列表，不影响当前画布编辑状态。
- 收到 `role-updated`：刷新项目详情和成员列表；如果目标用户是当前用户，则重新计算 `isReadOnly`。
- 收到 `member-removed`：刷新成员列表；如果目标用户是当前用户，则关闭共享弹窗、停止提交 operation、离开或关闭 WebSocket 房间，并展示无权限状态。
- 如果刷新项目详情、snapshot 或 operation API 返回无权限，前端必须停止提交 operation，避免继续产生失败请求。

成员审计日志建议：

```sql
CREATE TABLE canvas_member_audit_log (
  id BIGINT NOT NULL PRIMARY KEY,
  project_id BIGINT NOT NULL,
  operator_user_id BIGINT NOT NULL,
  target_user_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  before_role VARCHAR(32) NULL,
  after_role VARCHAR(32) NULL,
  reason VARCHAR(255) NULL,
  create_time DATETIME NOT NULL
);
```

审计规则：

- 邀请成员、修改角色、移除成员必须写审计日志。
- 操作者必须从登录态获取，不能信任前端传入的 operatorUserId。
- 被邀请用户必须存在、状态正常，并属于当前租户或明确允许协作的组织范围。
- 成员列表只能被项目成员读取，不能通过 projectId 枚举其他项目成员。
- 成员管理接口需要频率限制，避免通过 userId 枚举或批量骚扰。

共享链接安全规则：

- `/create/image?projectId=10001` 只代表项目入口，不代表访问授权。
- 用户打开链接后，后端仍然必须校验当前用户是否为项目成员。
- 非成员应返回无权限，前端展示无权限页面或提示联系项目 owner。
- 第一版不做“任何拿到链接的人都能加入”，避免项目泄露。

后续邀请 token 模型：

```sql
CREATE TABLE canvas_invite_link (
  id BIGINT NOT NULL PRIMARY KEY,
  project_id BIGINT NOT NULL,
  token VARCHAR(128) NOT NULL,
  default_role VARCHAR(32) NOT NULL,
  expire_time DATETIME NULL,
  max_uses INT NULL,
  used_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  create_user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL
);
```

邀请 token 安全规则：

- token 必须使用高强度随机值，不能使用 projectId、userId 等可猜测信息。
- token 支持过期时间、使用次数和主动撤销。
- 默认角色只能是 viewer 或 editor，不能是 owner。
- 使用 token 加入项目前仍需要登录态和租户校验。

## 10. 前端改造方案

### 10.1 模块拆分

建议从 `page.tsx` 中拆出以下 hooks：

- `useCanvasProject`：负责项目创建、加载、成员权限、服务端 snapshot 初始化。
- `useCanvasOperations`：负责把节点/边变更转换为标准 operation，并执行本地乐观更新。
- `useCanvasRealtime`：负责 WebSocket 连接、join、重连、发送 operation、接收广播。
- `useCanvasPresence`：负责鼠标、选中、编辑状态等临时协作状态。
- `useCanvasHistory`：负责本地撤销重做和协作 undo/redo。
- `useCanvasAssets`：负责上传资产、绑定资产、解析节点资产 URL。

### 10.2 状态边界

需要明确哪些状态由服务端持久化，哪些只留在客户端。

持久化状态：

- 节点列表
- 边列表
- 节点位置和尺寸
- 节点业务数据
- 节点绑定的资产 ID
- 节点生成任务状态

临时状态：

- 当前鼠标位置
- 当前选区
- 当前打开的弹窗
- 属性面板展开状态
- 拖拽过程中的中间位置
- hover 状态

### 10.3 React Flow 事件接入

需要把 React Flow 事件统一收敛到 operation 层：

- `onNodesChange`：区分 position、select、dimensions、remove 等变化。
- `onEdgesChange`：处理边删除和选择变化。
- `onConnect`：生成 `EDGE_CREATE`。
- 节点属性面板保存：生成 `NODE_UPDATE_DATA`。
- 上传或生成结果：生成 `ASSET_ATTACH` 或 `TASK_STATUS_PATCH`。

拖拽优化：

- 拖动过程中通过 presence 广播临时位置，频率控制在 50ms 到 100ms。
- 拖动结束后发送一次最终 `NODE_MOVE` operation。
- 其他客户端可以渲染“某用户正在拖动”的 ghost 状态，但不持久化。

### 10.4 本地存储调整

现有 localStorage 和 IndexedDB 不再作为主存储：

- localStorage 可缓存最近打开项目 ID、最近一次 snapshot、用户偏好。
- IndexedDB 可缓存媒体预览和离线草稿，但必须能从服务端资产重新恢复。
- 项目列表、项目名称、节点数量、资产数量应来自服务端 API。
- 空项目或无 snapshot 的画布保持空画布，不自动创建默认节点；节点只能来自服务端 snapshot、用户操作或 `/app` 快速生成初始化。
- 恢复 canvas 中图片/视频节点时，长期身份以 `assetId/outputAssetId` 为准，展示 URL 通过资产服务批量访问接口刷新；历史 snapshot 或 operation 中的临时签名 URL 不作为恢复事实源。

### 10.5 项目 ID 边界

- 后端 `/app-api/canvas/projects/{id}` 系列 REST API 的 `id` 对应 `canvas_project.id`，类型为 `bigint`；WebSocket 消息里的 `projectId` 也必须使用该服务端项目 ID。
- 前端 `project-store.ts` 生成的 `project_*` 仅表示本地离线草稿，只能作为 localStorage/IndexedDB key 和离线项目列表 ID 使用。
- 画布页需要派生 `serverProjectId`：只有 `projectId` 为纯数字时，才允许调用项目详情、成员、快照、operation、资产绑定、节点运行、共享邀请和 WebSocket join。
- 当创建服务端项目失败并降级到本地草稿时，前端保留本地编辑和本地保存能力，但必须禁用协作、共享、成员管理、节点运行服务端编排和资产绑定接口，避免 `project_*` 进入后端 Long 路由参数。

## 11. 后端改造方案

### 11.1 REST API

建议新增应用端 API：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/app-api/canvas/projects` | 项目列表 |
| POST | `/app-api/canvas/projects` | 创建项目 |
| GET | `/app-api/canvas/projects/recycle-bin` | 项目回收站列表 |
| GET | `/app-api/canvas/projects/{id}` | 项目详情 |
| PUT | `/app-api/canvas/projects/{id}` | 更新项目名称等元数据 |
| DELETE | `/app-api/canvas/projects/{id}` | 删除项目并移入回收站 |
| PUT | `/app-api/canvas/projects/{id}/restore` | 从回收站恢复项目 |
| GET | `/app-api/canvas/projects/{id}/snapshot` | 获取最新快照 |
| GET | `/app-api/canvas/projects/{id}/operations` | 获取指定版本后的同步增量；默认查 MySQL operation log，启用 Redis 热日志后拼接 MySQL + Redis，并在缺失时返回 snapshot fallback |
| POST | `/app-api/canvas/projects/{id}/members` | 邀请成员 |
| GET | `/app-api/canvas/projects/{id}/members` | 成员列表 |
| PUT | `/app-api/canvas/projects/{id}/members/{memberId}` | 修改成员角色 |
| DELETE | `/app-api/canvas/projects/{id}/members/{memberId}` | 移除成员 |
| GET | `/app-api/canvas/projects/{id}/assets` | 分页查询项目内资产，用于项目显示图选择、素材统计和资产引用排障 |
| POST | `/app-api/canvas/projects/{id}/assets` | 上传或绑定资产 |
| POST | `/app-api/canvas/projects/{id}/nodes/{nodeId}/run` | 执行节点生成任务 |

项目详情和项目分页的封面处理规则：

- 响应优先使用 `cover_asset_id` 查询当前可用封面访问 URL。
- 如果项目历史数据缺少 `cover_asset_id`，服务端会从最新 snapshot + 后续 operation 重建的 image/sketch 节点，或 `aigc_canvas_asset_ref` 资产引用中推导首个图片资产 ID。
- 推导到资产 ID 后，服务端按 `project_id + cover_asset_id is null` 条件回写 `aigc_canvas_project.cover_asset_id`；后续读取直接使用项目字段，避免每次进入项目都重复走节点/资产引用兜底分支。
- 私有 OSS/S3 场景下，封面 URL 只作为本次响应的运行时访问地址，`cover_asset_id` 才是长期持久字段。
- 项目页在删除按钮旁展示图片图标，点击打开“修改项目显示图”弹窗；弹窗左栏分页展示项目内图片资源，右栏分页展示当前用户所有图片资源。
- 保存项目显示图时只提交 `coverAssetId`，服务端校验该图片资产属于当前用户或已经被该项目引用；禁止把临时签名 URL、`coverUrl` 或节点预览 URL 当作项目封面身份保存。

### 11.1.1 项目回收站

画布项目回收站采用“项目表逻辑删除 + 回收站表记录”的方式实现：

- 删除项目时，服务端先校验项目存在并校验当前用户为项目拥有者，再写入 `aigc_canvas_project_recycle_bin` 回收站记录，最后调用项目正常删除逻辑，使 `aigc_canvas_project.deleted = 1`。
- 回收站表冗余保存 `project_id`、`project_name`、`owner_user_id`、`deleted_by`、`deleted_time`、`current_version`、`latest_snapshot_id`、`node_count`、`asset_count` 等字段，用于回收站列表展示和恢复权限判断。
- 回收站列表只展示当前用户拥有的项目，普通项目列表继续由逻辑删除过滤，只展示 `deleted = 0` 的项目。
- 恢复项目时，服务端先校验回收站记录存在并校验当前用户为项目拥有者，再使用专门 SQL 将 `aigc_canvas_project.deleted` 从 `1` 恢复为 `0`，最后物理删除回收站记录。
- 删除项目和写入回收站记录、恢复项目和删除回收站记录都必须放在同一个事务中，避免项目状态和回收站记录不一致。

### 11.2 WebSocket 消息类型

| 类型 | 方向 | 说明 |
| --- | --- | --- |
| canvas-join | 客户端 -> 服务端 | 加入项目房间 |
| canvas-leave | 客户端 -> 服务端 | 离开项目房间 |
| canvas-init | 服务端 -> 客户端 | 首次进入返回初始化数据 |
| canvas-sync-delta | 服务端 -> 客户端 | 返回差量 operation |
| canvas-sync-snapshot | 服务端 -> 客户端 | 返回完整快照 |
| canvas-op | 客户端 -> 服务端 | 提交画布操作 |
| canvas-op-applied | 服务端 -> 客户端 | 广播已应用操作 |
| canvas-op-rejected | 服务端 -> 客户端 | 拒绝操作 |
| canvas-presence | 双向 | 临时协作状态 |
| canvas-member-updated | 服务端 -> 客户端 | 成员变化 |
| canvas-member-kicked | 服务端 -> 客户端 | 被移除成员强制退出 |
| canvas-task-progress | 服务端 -> 客户端 | 任务进度推送 |

### 11.3 房间管理

需要在 WebSocket session manager 之上增加项目房间管理：

- `projectId -> sessionId list`
- `sessionId -> projectId list`
- `projectId -> online member list`

如果单机部署，可以先用内存维护。多实例部署时，房间成员状态需要放 Redis，消息广播通过现有 Redis/MQ sender 扩展。

### 11.4 快照策略

- 每 100 到 500 个 operation 生成一次快照，具体阈值按画布规模压测确定。
- 项目长时间无人编辑时，可以异步压缩历史 operation。默认路径压缩 MySQL operation；启用 Redis 热日志后，只能基于 `persistedVersion` 生成快照，不能压缩仅在 Redis 中 accepted/applied 的热操作。
- 快照生成失败不能影响实时编辑链路。
- 获取项目时优先加载最新快照，再回放快照之后的 operation。启用 Redis 热日志后，先回放 MySQL 已持久日志，再回放 Redis 未落盘热日志，确保恢复到 `hotVersion`。

## 12. 生成任务协作改造

### 12.1 当前问题

当前图片、文本、视频节点更多由前端直接调用生成接口并轮询结果。多人协作后会出现：

- A 用户触发生成，B 用户不知道任务进度。
- 节点结果只写入 A 用户本地状态。
- 生成资产如果只在 IndexedDB，其他用户无法访问。
- 计费、审核、失败重试难以统一治理。

### 12.2 推荐流程

```text
用户点击节点生成
  -> 前端调用 run node API
  -> 后端校验权限、余额、内容安全
  -> 后端创建 AIGC 任务
  -> 服务端广播 TASK_STATUS_PATCH: queued/running
  -> 任务完成后生成资产入库
  -> 服务端绑定 asset 到 node
  -> 服务端广播 TASK_STATUS_PATCH + ASSET_ATTACH
  -> 所有在线客户端更新节点状态
```

### 12.3 节点任务状态

建议节点数据中保留：

- `taskId`
- `taskStatus`
- `progress`
- `errorMessage`
- `outputAssetId`
- `updatedAt`

`outputPreviewUrl`、`previewUrl`、`videoUrl`、`assetUrlExpireTime` 只作为客户端运行时显示字段，不写入 operation log 或 snapshot。任务完成后服务端 patch 持久化 `assetId` / `outputAssetId`，客户端再通过资产详情或访问 URL 接口刷新当前可用的预览/播放 URL。

任务状态更新也按统一 operation 提交流程处理，默认路径由服务端系统用户写入 MySQL operation log；启用 Redis 热日志后先写 Redis 热日志并广播，再由 Worker 落盘，`actorUserId` 可为空或使用 system 标识。

## 13. Yjs / CRDT 引入策略

### 13.1 第一阶段不全量引入

当前画布协作主要是节点、边、资产、任务状态，天然适合对象级 operation。全量引入 CRDT 会增加后端持久化、权限、审计、资产绑定和问题排查复杂度。

### 13.2 适合引入 Yjs 的场景

- 多人同时编辑同一个长文本节点。
- 富文本节点。
- 评论正文。
- 需要强离线编辑和自动合并的局部内容。

### 13.3 推荐方式

- 画布结构仍使用服务端 operation log 作为最终持久化记录；生产增强路径下 Redis Stream 承接热日志。
- 文本节点内容单独使用 Yjs document。
- 节点数据只保存 Yjs 文档 ID、摘要、最后更新时间。
- Yjs 文档持久化和权限仍由服务端项目权限控制。

## 14. 版本路线

### V1：基础多人协作

- 服务端项目和 snapshot。
- 节点/边 operation log。
- WebSocket 项目房间广播。
- 在线成员、头像、鼠标光标。
- 拖拽结束同步最终位置。
- localStorage 降级为缓存。

### V2：可靠协作

- 断线重连。
- 差量同步。
- operation 幂等。
- 乐观更新失败回滚。
- 快照压缩。
- 多人协作撤销重做。

### V3：生产级工作流

- 资产中心化。
- 节点生成任务服务端编排。
- 任务进度 WebSocket 推送。
- 成员权限和项目分享。
- 审计日志和版本恢复。
- 内容安全、计费、余额冻结和失败释放。

### V4：Figma 级体验增强

- 批量选择协作。
- 节点锁定。
- 评论批注。
- 分支版本。
- 文本节点 Yjs 协作。
- 离线编辑与自动合并。

## 15. 风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| React Flow 本地状态和服务端状态不一致 | 多端画布错乱 | 所有持久化变更统一经过 operation 层 |
| WebSocket 消息重复 | 节点重复创建或重复连线 | `projectId + clientId + opId` 幂等去重 |
| 操作日志过大 | 初始化和恢复变慢 | 定期生成 snapshot，压缩旧日志 |
| 多人撤销覆盖他人操作 | 数据误回滚 | undo 生成反向 operation，不回退全局版本 |
| IndexedDB 资产无法共享 | 协作者看不到媒体 | 资产中心化，节点只保存资产 ID |
| 权限只在进入时校验 | 越权编辑 | 每条 operation 都做成员权限校验 |
| 任务状态和用户编辑冲突 | 节点状态异常 | 用户操作和系统任务 patch 分类型处理 |
| 首版范围过大 | 开发周期失控 | V1 只做节点/边协作和 presence |

## 16. 最小可落地范围

建议第一版只交付以下能力：

- 多人进入同一项目房间。
- 服务端返回画布 snapshot。
- 节点创建、删除、移动、属性更新实时同步。
- 边创建、删除实时同步。
- 在线成员、鼠标光标、选中节点展示。
- operation 幂等和基础断线重连。
- localStorage 降级为缓存，不再作为主存储。

第一版暂不做：

- 完整离线编辑。
- 全画布 CRDT。
- 同一文本节点多人逐字符协作。
- 复杂版本分支。
- 评论批注。
- 高级权限模板。

## 17. 开发落地清单

### 17.1 前端

- 新增 canvas 项目 API client。
- 新增 WebSocket client 封装。
- 新增 `useCanvasProject`。
- 新增 `useCanvasOperations`。
- 新增 `useCanvasRealtime`。
- 新增 `useCanvasPresence`。
- 重构画布页面的数据加载和保存逻辑。
- 将 React Flow 变更事件转为 operation。
- 将 localStorage/IndexedDB 主存储逻辑降级为缓存。
- 增加协作者头像、光标、选中框 UI。

### 17.2 后端

- 新增画布项目、成员、快照、操作日志表。
- 新增画布项目 REST API。
- 新增 WebSocket canvas 消息监听器。
- 新增项目房间管理。
- 新增 operation 校验和应用服务。
- 新增快照生成任务。
- 新增操作日志差量查询。
- 接入租户、登录态和成员权限校验。
- 接入资产模块和任务模块。

### 17.3 验收标准

- 两个浏览器同时打开同一画布，A 创建节点，B 能实时看到。
- A 移动节点，B 能在拖拽结束后看到最终位置。
- A 删除节点，B 本地对应节点和关联边消失。
- A 创建连线，B 能实时看到连线。
- A 和 B 同时移动同一节点，最终状态以服务端最后确认版本为准。
- A 断线后重连，可以补齐断线期间 B 的操作。
- 重复发送同一个 operation 不会导致重复节点或重复边。
- viewer 角色可以查看 presence，但不能提交编辑 operation。
- 刷新页面后从服务端恢复最新画布状态。

## 18. 剩余重点开发规划

当前基础协作链路已经完成服务端项目化存储、快照保存、operation log、WebSocket `canvas-op`、远端 operation 应用、`NODE_UPDATE_DATA`、presence 光标和断线差量补拉。后续开发重点应从“能同步”转向“多人真正可用、可靠、可恢复、可共享素材”。

### 18.1 优先级规划

#### P0：媒体资产中心化

目标：解决多人协作时上传图片、视频仍依赖本机 IndexedDB，其他协作者无法看到素材的问题。

开发内容：

- 复用 `yudao-module-aigc-asset`，将画布上传图片、视频统一写入资产服务。
- 画布节点持久化只保存 `assetId`、`assetVersionId`、`mimeType`、`width`、`height`、`sourceTaskId` 等稳定字段；`previewUrl`、`outputPreviewUrl`、`videoUrl`、`assetUrlExpireTime` 只留在前端运行时状态。
- IndexedDB 降级为本机缓存，不能作为协作主数据源。
- 新增或复用画布节点资产绑定关系，例如 `canvas_asset_ref` 或 `ASSET_ATTACH` operation。

影响范围：

- 前端：`image-upload.ts`、`image-store.ts`、`ImageNode.tsx`、`VideoNode.tsx`、`canvas-api.ts`。
- 后端：`yudao-module-aigc-asset`、`yudao-module-aigc-workflow` 画布资产绑定 API。

验收标准：

- A 用户上传图片后，B 用户打开同一画布可以看到图片预览。
- A 用户上传视频后，B 用户可以看到视频预览或播放地址。
- 刷新页面后，节点仍能通过服务端资产 URL 恢复，不依赖本机 IndexedDB。

#### P0：服务端快照恢复兜底

目标：当 operation log 缺失、被压缩或版本跨度过大时，客户端可以自动回退加载最新 snapshot。

开发内容：

- 改造 operation 差量查询接口，返回 `delta` 或 `snapshot` 模式。
- 如果 `afterVersion` 早于服务端可回放的最小版本，返回最新 snapshot。
- 前端根据返回模式选择回放 operation 或重新 hydrate snapshot。

推荐响应结构：

```json
{
  "mode": "delta",
  "fromVersion": 10,
  "toVersion": 20,
  "operations": []
}
```

```json
{
  "mode": "snapshot",
  "version": 30,
  "snapshot": {
    "nodesJson": "[]",
    "edgesJson": "[]",
    "viewportJson": "{}"
  }
}
```

影响范围：

- 后端：`AigcCanvasOperationServiceImpl`、`AigcCanvasAppController`、operation 查询 VO。
- 前端：`canvas-api.ts`、`page.tsx` 差量回放逻辑。

验收标准：

- 删除旧 operation log 后，客户端重连不会卡住或出现错乱。
- 服务端提示 snapshot fallback 时，前端能自动恢复最新画布。

#### P1：协作者选区与在线成员 UI

目标：让协作者不仅看到鼠标光标，也能看到其他人选中的节点和在线成员列表。

开发内容：

- 基于 presence 中的 `selectedNodeIds` 渲染远端节点选中框。
- 为每个协作者分配稳定颜色。
- 右上角协作人数升级为成员头像或颜色列表。
- 增加 presence 超时清理，超过 10 秒未更新则移除光标和选区。

影响范围：

- 前端：`page.tsx`，建议新增 `CollaboratorCursor.tsx`、`CollaboratorSelection.tsx`。
- 后端：当前 `canvas-presence` 已能广播，暂不需要大改。

验收标准：

- A 用户选中节点，B 用户能看到该节点外框显示 A 的颜色标识。
- A 用户关闭页面或断线后，B 用户界面中的 A 光标和选区能自动消失。

#### P1：操作冲突与幂等增强

目标：多人同时操作同一节点时，系统行为稳定，不产生重复边、脏节点或前端异常。

开发内容：

- 服务端对 `NODE_DELETE`、`EDGE_CREATE`、`NODE_UPDATE_DATA` 做类型级校验。
- 删除不存在节点、边时按 NOOP 或幂等成功处理。
- 重复创建同一条边时返回已有结果，不重复写入。
- 远端 operation 应用前检查节点、边是否存在；异常时补拉 snapshot。

影响范围：

- 后端：`AigcCanvasOperationServiceImpl`。
- 前端：`page.tsx` 的 operation reducer。

验收标准：

- A 删除节点同时 B 编辑该节点，不会导致前端报错。
- 多次重复发送同一条连线创建，不会产生重复边。

#### P1：节点数据 patch 字段白名单

目标：区分持久化业务字段和临时 UI 字段，避免弹窗、hover、临时状态、大对象进入 operation log。

开发内容：

- 定义 `SYNCABLE_NODE_DATA_KEYS`。
- 前端派发 `NODE_UPDATE_DATA` 前过滤字段。
- 后端保存 operation 前再做字段白名单和 payload 大小限制。
- 图片、视频大对象字段，例如 `dataUrl`、本地 `blob:` URL，不进入协作主数据。

影响范围：

- 前端：`types.ts`、`use-canvas-operations.ts`、`ImageNode.tsx`、`TextNode.tsx`、`VideoNode.tsx`。
- 后端：`AigcCanvasOperationServiceImpl`。

验收标准：

- prompt、params、content、status、taskId、assetId 等稳定字段可同步；私有 OSS/S3 访问 URL 相关字段不可同步。
- 弹窗开关、hover、临时拖拽状态不会写入 operation log。

#### P2：节点生成任务服务端编排

目标：将图片、文本、视频生成从前端直接调用和轮询，升级为服务端节点执行、任务状态推送和统一计费审核。

开发内容：

- 新增 `POST /canvas/projects/{id}/nodes/{nodeId}/run`。
- workflow 服务负责校验权限、余额、内容安全、模型参数。
- workflow 调用 `aigc-task`、`aigc-gen`、`aigc-billing`、`aigc-safety`。
- 任务状态通过 `TASK_STATUS_PATCH` 或 `NODE_UPDATE_DATA` 广播到画布房间。
- 生成结果写入资产服务，再通过 `ASSET_ATTACH` 绑定节点。

影响范围：

- 前端：`ImageNode.tsx`、`TextNode.tsx`、`VideoNode.tsx`、`generation-api.ts`。
- 后端：workflow controller/service、task/gen/billing/safety/asset API。

验收标准：

- A 触发节点生成，B 能看到该节点进入 pending/running/success/failed。
- 生成结果是服务端资产，所有协作者可见。

#### P2：协作 undo/redo

目标：多人环境下撤销当前用户自己的操作，而不是回滚全局画布状态。

开发内容：

- operation log 保存 `inverseOperationJson`。
- 已确认操作的 undo 生成反向 operation。
- 本地未确认操作仍可保留本地 history。
- 删除节点的反向操作需要包含被删除节点和关联边快照。

影响范围：

- 前端：`use-canvas-operations.ts`、`page.tsx`。
- 后端：operation log、operation service。

验收标准：

- A 撤销自己移动节点，不会覆盖 B 后续新增节点。
- A 撤销自己创建的节点，只删除该节点及合法关联边。

#### P2：性能与日志压缩

目标：避免 operation log 无限增长、快照过大、初始化变慢。

开发内容：

- 每 100 到 500 条 operation 生成一次 snapshot。
- snapshot 生成异步执行，不阻塞实时编辑链路。
- 旧 operation 可归档或仅保留最新 snapshot 之后的差量。
- 前端自动保存 snapshot 降频，operation 默认实时写入 MySQL；启用 Redis 热日志后实时写 Redis Stream，MySQL 周期批量落盘。

影响范围：

- 后端：workflow job/service、operation log、snapshot service。
- 前端：`page.tsx` 自动保存策略。

验收标准：

- 长时间协作后，重新打开项目仍能快速加载。
- operation log 压缩后，客户端能通过 snapshot fallback 正常恢复。

### 18.2 推荐开发顺序

1. 图片资产中心化。
2. 视频资产中心化。
3. snapshot fallback 与差量同步协议升级。
4. 协作者选区与在线成员 UI。
5. operation 冲突处理与字段白名单。
6. 节点生成任务服务端编排。
7. 协作 undo/redo。
8. 定期 snapshot 与 operation log 压缩。

### 18.3 具体任务拆分

#### 任务 A：图片资产中心化

- 后端新增或复用资产上传 API。
- 后端新增画布节点资产绑定 API：`POST /canvas/projects/{id}/nodes/{nodeId}/assets`。
- 前端上传图片后调用资产上传 API。
- `ImageNodeData` 增加 `assetId?: number`、`assetVersionId?: number` 等稳定资产字段；`previewUrl` 仅用于当前渲染态。
- `dataUrl` 仅作为本机缓存字段，不作为协作主字段。

#### 任务 B：视频资产中心化

- 视频上传后进入资产服务。
- `VideoNodeData` 增加 `assetId?: number`、`assetVersionId?: number` 等稳定资产字段；`videoUrl` / `previewUrl` 仅用于当前渲染态。
- 远端用户通过 `assetId` 拉取新的运行时访问 URL 后访问视频。
- IndexedDB 仅做本机缓存。

#### 任务 C：差量同步兜底

- 后端 operations API 返回 `mode=delta` 或 `mode=snapshot`。
- 前端根据 mode 回放 operation 或重载 snapshot。
- 对 operation 应用失败场景触发 snapshot fallback。

#### 任务 D：协作者选区 UI

- 将 presence 中的 `selectedNodeIds` 映射到 React Flow 节点 DOM bounds。
- 使用协作者颜色渲染远端选中框。
- 增加在线成员列表。
- 增加 presence 超时清理。

#### 任务 E：字段白名单

- 定义 `SYNCABLE_NODE_DATA_KEYS`。
- 前端提交 `NODE_UPDATE_DATA` 前过滤 patch。
- 后端保存 operation 前校验字段和 payload 大小。
- 禁止 `dataUrl`、`blob:` 本地 URL、大型媒体内容进入 operation log。

#### 任务 F：节点执行 API

- 后端新增 `POST /canvas/projects/{id}/nodes/{nodeId}/run`。
- 前端生成按钮改为调用 run API。
- 服务端广播 `TASK_STATUS_PATCH`。
- 生成结果写入资产并广播 `ASSET_ATTACH`。

### 18.4 里程碑验收

#### M1：协作可用

- A 上传图片，B 可见。
- A 创建、移动、编辑节点，B 实时同步。
- A 断线重连后能补齐 B 的操作。
- A、B 都能看到彼此光标和选区。

#### M2：协作可靠

- operation log 缺失时能 snapshot fallback。
- 重复 operation 不重复应用。
- 节点删除和编辑冲突不会导致前端异常。
- 快照和 operation 不无限增长。

#### M3：生成协作

- A 发起生成，B 能看到任务进度。
- 生成结果变成服务端资产，所有成员可见。
- 计费、审核、任务失败由后端统一控制。

## 19. 当前开发进度评审与评分

### 19.1 综合结论

当前实现已经从“单人本地草稿”推进到“可靠协作雏形”：项目化存储、快照、operation log、WebSocket 房间广播、presence 光标、远端 operation 应用等主链路已经具备，并已补齐 delta/snapshot 同步兜底、operation 拒绝响应、前端版本分离、payload 白名单、资产绑定广播、服务端当前图状态校验和图状态缓存。整体已经超过 V1 基础协作范围，正在进入 V2 可靠协作收口阶段。

综合评分：74 / 100。

阶段判断：

- V1 基础多人协作：约 85% 完成。
- V2 可靠协作：约 65% 完成。
- V3 生产级工作流：约 30% 完成。

当前可以用于内部联调和小范围内测。若要作为生产级多人协作能力对外发布，仍需补齐自动重连队列、成员 UI、节点生成任务服务端编排、多实例房间和快照压缩等能力。

### 19.2 已完成能力

- 后端已建立画布项目、成员、快照、operation log、资产引用等核心表。
- 后端已提供项目 CRUD、最新快照读写、operation 查询和提交、节点资产绑定等 REST API。
- 后端已支持 `canvas-join`、`canvas-leave`、`canvas-op`、`canvas-presence` WebSocket 消息。
- 前端画布已接入服务端项目、快照、operation、WebSocket 实时连接和 presence 光标。
- 前端已将节点创建、移动、删除、尺寸变化、数据更新、边创建、边删除等部分 React Flow 事件转成 operation。
- operation 已具备基础幂等键 `projectId + clientId + opId`，可避免同一客户端重复提交同一操作。
- operation 查询已升级为 `mode=delta/snapshot`，当差量日志不可回放时可返回最新 snapshot 兜底。
- WebSocket `canvas-op` 提交失败时已支持 `canvas-op-rejected`，前端收到拒绝后可触发同步补拉。
- 前端已拆分 `lastAppliedVersion` 和 `latestKnownVersion`，避免收到较新实时消息后漏拉中间 operation。
- presence 已增加异常断线超时清理，远端光标和选区状态不会长期残留。
- 前端和后端均已增加 operation payload 白名单、大小限制、JSON 合法性校验，并禁止 `data:`、`blob:` 本地媒体内容进入 operation log。
- 前端 `NODE_CREATE` 已剥离本地媒体大对象，只同步轻量节点数据和资产引用字段。
- 后端已增加当前图状态重建能力，基于最新 snapshot 和后续 operation log 校验节点/边是否存在、重复边、删除后编辑、给不存在节点连线等语义。
- 后端已为当前图状态增加项目级内存缓存，普通 operation 成功后可增量更新缓存，snapshot 保存和资产绑定会触发缓存失效。
- 图片、视频上传后已接入资产上传与节点资产绑定，后端绑定资产时会写入 `ASSET_ATTACH` operation log 并广播给画布房间。
- 项目封面已收敛为 `cover_asset_id` 稳定字段；历史项目读取时可从节点或资产引用兜底推导首个图片资产 ID，并回写项目表，后续不再重复分支判断。

### 19.3 主要缺口

- 服务端当前图状态仍是基于 snapshot + operation log 重建和内存缓存，尚未持久化为独立权威图状态表。
- `baseVersion` 仍主要用于记录和补拉依据，尚未实现严格乐观锁或版本冲突自动合并。
- 快照仍由客户端全量保存，多个客户端并发保存时仍需进一步收敛为服务端异步快照策略。
- WebSocket 缺少完整自动重连、pending operation 队列、ack 超时和发送失败重试。
- presence 已支持光标和超时清理，但远端选区框、在线成员头像、编辑中状态 UI 还不完整。
- 资产中心化已具备上传、绑定、`ASSET_ATTACH` 广播链路，但仍需补齐资产权限校验、资产版本表和刷新后跨端预览恢复细节。
- 节点生成任务仍未完成服务端编排，缺少 run node API、任务状态推送、计费审核和结果资产绑定闭环。
- 房间管理仍是单机内存模型，多实例部署时需要 Redis/MQ 房间成员同步。

### 19.4 风险评级

| 风险 | 等级 | 说明 | 优先处理 |
| --- | --- | --- | --- |
| 客户端快照覆盖协作结果 | 高 | 多端自动保存全量 snapshot 仍可能覆盖其他用户已确认操作 | 是 |
| operation 并发冲突 | 中 | 已有当前图状态校验，但还缺严格 baseVersion 冲突策略和自动合并 | 是 |
| 断线恢复不可靠 | 中 | 已有 snapshot fallback 和版本分离，但缺 pending 队列、ack 超时和自动重连重放 | 是 |
| 媒体资源不可共享 | 中 | 已有资产上传、绑定和广播链路，但资产权限、版本和刷新恢复仍需补齐 | 是 |
| 多实例房间不可用 | 中 | 房间状态目前偏单机内存模型 | 否 |
| payload 脏数据入库 | 低 | 已增加前后端字段白名单、大小限制和本地媒体拦截 | 已处理 |

### 19.5 整改优先级

P0：可靠同步闭环。

- 收敛客户端自动保存 snapshot，改为服务端定期或阈值触发快照；启用 Redis 热日志后，快照只能覆盖已 `persistedVersion` 的操作，不能覆盖仅在 Redis 中 accepted/applied 的热操作。
- 为 WebSocket 增加自动重连、pending operation 队列、ack 超时和失败重试。
- 将 `baseVersion` 从记录字段升级为冲突检测输入，明确旧版本操作的接受、合并或拒绝规则。
- 将当前图状态缓存升级为可观测、可限流的缓存组件，后续支持 Redis 或持久化当前图状态。

P1：协作体验补齐。

- 渲染远端选区和编辑中状态。
- 补齐在线成员列表、头像和稳定颜色。
- 增加成员列表、邀请、角色修改和 viewer 只读 UI。
- 将项目分页从仅 owner 项目扩展为成员可见项目。

P2：生产能力增强。

- 新增节点运行 API，由服务端编排生成任务并广播任务状态。
- 完善资产权限校验、资产版本和跨端预览恢复。
- 定期生成 snapshot，压缩旧 operation log。
- 房间状态迁移到 Redis/MQ，支持多实例部署。

### 19.6 最近开发更新记录

本轮围绕“可靠协作”和“资产中心化”完成了以下增量开发：

| 模块 | 更新内容 | 状态 |
| --- | --- | --- |
| 差量同步 | 新增 `operations/sync`，支持 `delta` 和 `snapshot` 双模式返回 | 已完成 |
| WebSocket 可靠性 | 新增 `canvas-op-rejected`，operation 被拒绝时前端触发补拉 | 已完成 |
| 前端版本管理 | 拆分 `lastAppliedVersion` 与 `latestKnownVersion`，发现版本缺口时从已应用版本补拉 | 已完成 |
| Presence 生命周期 | 增加 10 秒超时清理，异常断线后光标不再长期残留 | 已完成 |
| Payload 安全 | 前后端增加字段白名单、payload 大小限制、本地媒体内容拦截 | 已完成 |
| NODE_CREATE 安全 | 创建节点 operation 提交前剥离 `dataUrl`、`blob:`、本地 URL 和大对象数组 | 已完成 |
| Operation 语义校验 | 服务端校验节点存在、边存在、重复节点、重复边、给不存在节点连线等场景 | 已完成 |
| 当前图状态 | 服务端基于 snapshot + operation log 重建当前节点/边状态 | 已完成 |
| 图状态缓存 | 按项目和版本缓存当前图状态，operation 后增量更新，snapshot/资产绑定后失效 | 已完成 |
| 资产绑定协作 | 上传后绑定资产，后端写入 `ASSET_ATTACH` operation log 并广播 | 已完成 |

### 19.7 最新下一步开发建议

推荐下一阶段优先推进：

1. 节点生成任务服务端编排：新增 `/canvas/projects/{id}/nodes/{nodeId}/run`，由后端创建任务并广播 `TASK_STATUS_PATCH`。
2. 快照策略服务端化：从客户端 debounce 全量保存，升级为服务端按 operation 数量或空闲时间异步生成 snapshot。
3. WebSocket 重连队列：前端维护 pending operation、ack 超时、重连后补拉再重放未确认操作。
4. 协作者 UI：渲染远端选区、在线成员头像、编辑中状态，补齐 viewer 只读体验。
5. 多实例房间：将当前内存房间状态升级到 Redis/MQ 或复用 Yudao WebSocket sender 的集群能力。

## 20. 飞书共享画布参考价值与路线调整

飞书共享画布的节点树、混合协同算法、分层渲染和 WebSocket 实时通信对本项目有参考价值，但更适合作为长期架构参照和体验标杆，不适合作为当前阶段的直接重构目标。

当前项目已经基于 React Flow、服务端项目、快照、operation log、WebSocket 房间广播和成员权限形成可落地路径。现阶段应继续坚持“服务端权威状态 + 节点级 operation log + WebSocket 项目房间广播”的路线，优先补齐可靠同步、共享邀请、资产中心化和节点任务服务端编排，而不是直接迁移到全量节点树、全画布 CRDT 或自研渲染引擎。

### 20.1 可吸收能力

- 操作原子化：所有持久化编辑都拆解为 `NODE_CREATE`、`NODE_MOVE`、`NODE_UPDATE_DATA`、`EDGE_CREATE`、`ASSET_ATTACH`、`TASK_STATUS_PATCH` 等 operation。
- 增量同步：继续以 `clientId + opId + baseVersion + nextVersion` 支撑幂等、补拉和断线恢复，避免反复传输完整画布。
- Presence 分层：鼠标、选区、编辑中状态、在线头像和视口等临时协作状态只走 WebSocket，不进入 operation log。
- 权限实时刷新：成员角色变化、成员移除和踢出事件需要实时广播，客户端收到后立即刷新角色、切换只读或退出画布。
- 局部 CRDT：后续只对长文本节点、富文本节点、评论正文等高并发文本内容引入 Yjs，不把整个画布结构改成 CRDT。
- 多实例广播：当进入生产多实例部署后，项目房间、成员变更、踢出和任务进度事件应通过 Redis/MQ 或现有 Yudao WebSocket 集群 sender 广播。

### 20.2 暂不照搬能力

- 暂不重构为全量节点树模型：React Flow 的 nodes 和 edges 已能覆盖当前 AIGC 工作流画布，强行改为无限层级节点树会导致编辑器、存储、operation 和渲染链路大范围重做。
- 暂不引入全画布 CRDT：当前对象级协作更适合服务端顺序化和字段级合并，全量 CRDT 会增加权限、审计、资产绑定、任务状态回写和问题排查复杂度。
- 暂不自研 Canvas/SVG/WebGL 渲染引擎：当前瓶颈主要是协作可靠性、共享入口、成员权限和资产共享，不是 React Flow 渲染内核。
- 暂不切换 Protocol Buffers：JSON WebSocket 足够支持当前内部联调和小规模协作，待消息体积、延迟或吞吐成为瓶颈后再升级二进制协议。
- 暂不做公开无门槛链接加入：`projectId` 链接只作为入口，后端仍必须通过成员表校验访问权限。

### 20.3 调整后的优先级

P0：可靠同步闭环。

- 快照策略服务端化，避免多个客户端全量 snapshot 相互覆盖。
- WebSocket 增加自动重连、pending operation 队列、ack 超时和失败重试。
- `baseVersion` 升级为冲突检测输入，明确旧版本 operation 的接受、合并或拒绝规则。
- 当前图状态缓存继续可观测化，并预留 Redis 缓存或持久化当前图状态的升级空间。

P0：共享邀请闭环。

- 补齐共享按钮、成员弹窗、复制链接、邀请 userId、成员角色修改和移除成员。
- 成员变更通过 `canvas-member-updated` 或 `canvas-member-kicked` 实时通知在线端。
- 被降级用户立即进入 viewer 只读，被移除用户立即停止提交 operation 并进入无权限状态。

P1：协作者体验。

- 基于 presence 渲染远端光标、远端选区、编辑中状态和在线成员头像。
- 为协作者分配稳定颜色，并与成员列表、光标、选区边框保持一致。
- 增加 presence 超时清理，避免异常断线后残留光标或选区。

P1：资产和任务协作。

- 图片、视频等媒体继续进入资产服务，节点只保存资产 ID、版本 ID 和预览 URL。
- 节点生成任务改由服务端编排，任务状态通过 `TASK_STATUS_PATCH` 广播，生成结果通过 `ASSET_ATTACH` 绑定。

P2：长期增强。

- 长文本、富文本和评论正文引入局部 Yjs。
- 增加邀请 token、有效期、撤销、使用次数和邀请审计。
- 多实例房间、成员变更、任务进度和踢出事件接入 Redis/MQ 广播。
- 在节点数量和渲染复杂度明显上升后，再评估自研渲染分层、虚拟化和 WebGL 加速。

## 21. Redis 热日志与 MySQL 周期落盘方案

当画布进入多人协作、高频拖拽、节点数据连续更新和长时间项目编辑阶段后，如果每条 operation 都同步写入 MySQL，`aigc_canvas_operation_log` 的 insert、唯一索引竞争、项目版本更新和差量查询会逐步成为性能瓶颈。本节作为生产级性能增强方案，推荐采用“Redis 做热日志 + MySQL 周期落盘”的架构：Redis 承接实时协作热路径，MySQL 承接最终持久化、快照、审计和恢复。

### 21.1 设计目标

- 降低 MySQL 高频 insert 和项目版本锁竞争压力。
- 保持 WebSocket 实时协作体验，operation 接收和广播尽量低延迟。
- 支持客户端断线后从 Redis 热日志和 MySQL 持久日志拼接增量。
- 支持 Worker 周期批量落盘，减少事务次数和索引写入次数。
- 支持定期 snapshot 压缩，避免 operation log 无限增长。
- 保留最终可恢复、可审计、可追踪的数据链路。

### 21.2 总体架构

```text
前端 React Flow 画布
  -> WebSocket / REST 提交 operation
  -> 后端接入层鉴权、校验、幂等检查
  -> Redis Stream 写入热日志并分配 hotVersion
  -> Redis 保存项目热状态和短期增量
  -> WebSocket 广播 canvas-op-applied
  -> OperationPersistWorker 周期批量写 MySQL
  -> SnapshotCompactWorker 定期生成 snapshot
  -> 清理或归档旧 operation
```

核心分工：

- MySQL：`canvas_project`、`canvas_snapshot` 元数据、`canvas_operation_log`、`canvas_asset_ref`，承接最终持久化、恢复、审计、权限和统计。
- OSS / MinIO：大体积 snapshot JSON、历史 snapshot 包和旧版本归档，避免把长 JSON 长期压在在线 MySQL 表里。
- Redis：协作房间热状态、热 operation、项目热版本、幂等键、pending operation、presence 和短期增量同步。
- Worker：Redis 到 MySQL 的周期落盘、snapshot body 写入 OSS / MinIO、snapshot 元数据写入 MySQL、异常恢复和积压监控。
- 前端：本地乐观更新、pending/accepted/persisted 状态管理、重连补拉和必要时补交 operation。

### 21.3 版本模型

需要区分 Redis 热版本和 MySQL 持久版本：

| 版本 | 存储位置 | 说明 |
| --- | --- | --- |
| `hotVersion` | Redis | Redis 接收 operation 后分配的实时协作版本 |
| `persistedVersion` | Redis + MySQL checkpoint | MySQL 已持久化到的 operation 版本 |
| `snapshotVersion` | Redis + MySQL snapshot | 最新 snapshot 覆盖到的版本 |

正常情况下必须满足：

```text
snapshotVersion <= persistedVersion <= hotVersion
```

示例：

```text
snapshotVersion = 100
persistedVersion = 120
hotVersion = 129
```

含义：100 之前已经被 snapshot 覆盖，101 到 120 已落 MySQL，121 到 129 仍在 Redis 热日志中。

### 21.4 Redis Key 设计

建议所有 key 按 `projectId` 拆分，避免全局热点大 key。

| Key | 类型 | 说明 |
| --- | --- | --- |
| `canvas:ops:{projectId}` | Stream | 项目热 operation 日志 |
| `canvas:version:{projectId}` | String | 当前 Redis 热版本 `hotVersion` |
| `canvas:persisted-version:{projectId}` | String | MySQL 已落盘版本 |
| `canvas:snapshot-version:{projectId}` | String | 最新 snapshot 覆盖版本 |
| `canvas:state:{projectId}` | String / Hash | 当前画布热状态 |
| `canvas:op:idempotent:{projectId}:{clientId}:{opId}` | String | operation 幂等键 |
| `canvas:active-projects` | Set / ZSet | 活跃项目集合，供 Worker 调度 |

Redis Stream 消息建议字段：

```json
{
  "projectId": "1001",
  "clientId": "client-a",
  "opId": "op-001",
  "actorUserId": "2001",
  "baseVersion": "128",
  "hotVersion": "129",
  "operationType": "NODE_MOVE",
  "operationJson": "{\"nodeId\":\"node-1\",\"position\":{\"x\":100,\"y\":200}}",
  "inverseOperationJson": "{}",
  "createdAt": "1710000000000"
}
```

幂等键 TTL 建议设置为 1 到 7 天，避免客户端重试或重连补交导致重复应用。

### 21.5 Operation 提交流程

接入层收到 `canvas-op` 后，不再同步写 MySQL operation log，而是先写 Redis 热日志：

```text
submitOperation(req)
  -> 校验登录态、租户、项目成员和 editor 权限
  -> 校验 operationType、operationJson、payload 大小和字段白名单
  -> 检查 canvas:op:idempotent:{projectId}:{clientId}:{opId}
  -> 如果已存在，返回已有 hotVersion
  -> INCR canvas:version:{projectId} 得到 hotVersion
  -> XADD canvas:ops:{projectId} 写入 operation
  -> SET 幂等键并设置 TTL
  -> 应用 operation 到 canvas:state:{projectId}
  -> 将 projectId 写入 canvas:active-projects
  -> 广播 canvas-op-applied
  -> 返回 accepted
```

为了保证 `INCR`、`XADD`、幂等键写入的原子性，推荐用 Redis Lua 脚本完成。这样同一个项目在 Redis 层获得单调递增版本，MySQL 不参与实时抢版本。

### 21.6 前端 ACK 语义调整

引入 Redis 热日志后，前端不能再把“服务端返回成功”理解为“已经落 MySQL”。建议 operation 状态拆分为：

| 状态 | 说明 |
| --- | --- |
| `pending` | 本地已产生，尚未被服务端接收 |
| `accepted` | 已写入 Redis 热日志，已获得 hotVersion |
| `applied` | 已被服务端广播并应用到协作房间 |
| `persisted` | 已由 Worker 落入 MySQL |
| `rejected` | 权限、校验、冲突或 payload 问题导致拒绝 |

WebSocket 事件建议：

| 事件 | 说明 |
| --- | --- |
| `canvas-op-accepted` | 服务端已接收并写入 Redis |
| `canvas-op-applied` | 服务端已分配版本并广播给房间 |
| `canvas-op-persisted` | Worker 已落盘到 MySQL，可选发送 |
| `canvas-op-rejected` | 服务端拒绝 operation |

如果第一版不希望增加 `persisted` UI，可以只在重连时校验落盘状态；但如果页面右上角要显示“已保存”，则必须区分 `accepted` 和 `persisted`。

### 21.7 差量同步流程

客户端重连或补拉时携带 `afterVersion`，服务端需要从 MySQL 和 Redis 拼接增量：

```text
afterVersion = 120
persistedVersion = 125
hotVersion = 130

返回：
  MySQL operation_log 中 121 到 125
  Redis 热日志中 126 到 130
```

同步规则：

- 如果 `afterVersion < snapshotVersion`，直接返回最新 snapshot，并附带 snapshot 之后可回放的 operation。
- 如果 `afterVersion <= persistedVersion`，先查 MySQL 中 `afterVersion + 1` 到 `persistedVersion`。
- 如果 `persistedVersion < hotVersion`，再查 Redis 中 `persistedVersion + 1` 到 `hotVersion`。
- 如果 Redis 热日志缺失必要版本，返回 snapshot fallback，避免客户端停留在不完整状态。

### 21.8 周期落盘 Worker

`OperationPersistWorker` 负责将 Redis 热日志批量写入 MySQL：

```text
persistWorker(projectId)
  -> 读取 persistedVersion 和 hotVersion
  -> 如果 hotVersion <= persistedVersion，跳过
  -> 从 Redis Stream 拉取 persistedVersion + 1 到 hotVersion 的 operation
  -> 按 hotVersion 排序并做幂等过滤
  -> 批量 insert aigc_canvas_operation_log
  -> 更新 aigc_canvas_project.current_version = hotVersion
  -> 更新 aigc_canvas_persist_checkpoint.persisted_version = hotVersion
  -> 更新 canvas:persisted-version:{projectId} = hotVersion
  -> 必要时广播 canvas-op-persisted
```

推荐触发条件：

- 每 1 到 3 秒落盘一次。
- 每累计 50 到 200 条 operation 落盘一次。
- 项目空闲超过 5 到 10 秒后主动补一次落盘。
- 用户关闭页面、房间无人在线或服务关闭前尽量补一次落盘。

MySQL 写入应保持批量事务：多条 operation 一次 insert，项目 `currentVersion` 一次 update，避免每条 operation 一个事务。

### 21.9 快照压缩 Worker

`SnapshotCompactWorker` 负责把已落盘 operation 压缩成 snapshot：

```text
snapshotWorker(projectId)
  -> 读取 snapshotVersion 和 persistedVersion
  -> 判断 persistedVersion - snapshotVersion 是否超过阈值
  -> 从 Redis state 或 MySQL snapshot + operation 重建当前画布
  -> 小型 snapshot 可内联写入 aigc_canvas_snapshot
  -> 大体积 snapshot JSON 写入 OSS / MinIO
  -> aigc_canvas_snapshot 写入版本、对象 key、大小、hash 等元数据
  -> 更新项目 latest_snapshot_id 和 snapshotVersion
  -> 将旧 snapshot body 或历史 snapshot 包归档到 OSS / MinIO
  -> 归档或清理 snapshotVersion 之前的旧 operation
  -> 清理 Redis 中不再需要的热日志
```

推荐阈值：

- 每 300 到 500 条 operation 生成一次 snapshot。
- 或每 5 到 10 分钟生成一次 snapshot。
- 或项目停止编辑 30 秒后生成一次 snapshot。
- snapshot body 超过配置阈值时必须走 OSS / MinIO 对象存储；MySQL 只保留 `canvas_snapshot` 元数据，避免 JSON 大字段拖慢项目列表、差量查询和备份恢复。

快照生成失败不能阻塞实时编辑链路，只能告警并稍后重试。

### 21.10 Redis 热日志清理

Redis 只保留短期热日志，不能无限增长。建议保留策略：

```text
保留 max(snapshotVersion, persistedVersion - safetyWindow) 之后的日志
```

示例：

```text
snapshotVersion = 1000
persistedVersion = 1300
hotVersion = 1320
safetyWindow = 1000
```

可以保留 1001 到 1320 的日志，或者按项目规模保留最近 1000 到 5000 条 operation。清理时要保证客户端仍能通过 snapshot fallback 恢复，不能只删除 Redis 而不更新 snapshot 和 checkpoint。

### 21.11 MySQL Checkpoint 表

建议新增落盘检查点表，用于服务重启、Redis 异常恢复和监控：

```sql
CREATE TABLE aigc_canvas_persist_checkpoint (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  hot_version BIGINT NOT NULL DEFAULT 0,
  persisted_version BIGINT NOT NULL DEFAULT 0,
  snapshot_version BIGINT NOT NULL DEFAULT 0,
  last_persist_time DATETIME NULL,
  last_snapshot_time DATETIME NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_project_id (project_id)
);
```

用途：

- 服务启动时恢复每个项目的 `persistedVersion` 和 `snapshotVersion`。
- Redis 数据丢失时判断 MySQL 已保存到哪里。
- 监控 `hotVersion - persistedVersion` 的落盘积压。
- 支持运维排查落盘延迟和 snapshot 压缩延迟。

### 21.12 一致性与可靠性

推荐可靠性等级：`Redis Stream + AOF everysec + 客户端重试 + MySQL 周期批量落盘`。

Redis 建议配置：

```text
appendonly yes
appendfsync everysec
```

一致性处理规则：

- Redis 写入成功但 MySQL 尚未落盘时，客户端状态为 `accepted`，不能等同于最终持久化完成。
- Worker 写 MySQL 失败时，Redis Stream 消息不能 ack 或不能标记为已落盘，必须重试。
- MySQL 批量写入必须继续使用 `projectId + clientId + opId` 幂等唯一键，防止重复落盘。
- 同一 `projectId` 的落盘需要按版本顺序推进，不能跳过中间版本直接更新 `persistedVersion`。
- 服务重启后 `RecoveryWorker` 要扫描 Redis 和 checkpoint，补齐 `persistedVersion < hotVersion` 的项目。
- Redis 热日志缺失时必须返回 snapshot fallback，不能让客户端回放不完整 operation。

如果业务要求“服务端确认后绝不丢操作”，可以增加 MySQL 轻量接收表，只记录 `projectId、clientId、opId、hotVersion、createdAt`，完整 operation 仍由 Redis 周期落盘。但该方案会重新引入一部分 MySQL 写压力，建议在可靠性要求明确后再采用。

### 21.13 监控指标与告警

必须监控以下指标：

| 指标 | 说明 | 建议告警 |
| --- | --- | --- |
| `hotVersion - persistedVersion` | Redis 未落盘积压条数 | 单项目超过 1000 条 |
| `persistedVersion - snapshotVersion` | snapshot 压缩落后条数 | 单项目超过 3000 条 |
| Redis Stream 长度 | 热日志内存压力 | 超过项目阈值 |
| Operation 落盘延迟 | 从 accepted 到 persisted 的耗时 | P95 超过 10 秒 |
| MySQL batch insert 耗时 | 周期落盘性能 | P95 超过 1 秒 |
| `syncOperations` P95/P99 | 断线补拉体验 | 明显高于实时接口 |
| Worker 重试次数 | 落盘异常 | 连续失败立即告警 |

### 21.14 推荐落地阶段

第一阶段先做低风险改造：

- 前端继续降低高频提交，拖拽结束只提交最终 `NODE_MOVE`。
- `NODE_UPDATE_DATA` 继续 debounce 和 patch 合并。
- 服务端 snapshot 改为定期或阈值触发，减少客户端全量覆盖风险。
- 保留当前同步写 MySQL 作为默认路径。

第二阶段引入 Redis 热日志：

- 新增 Redis Stream 和 Lua 原子写入脚本。
- 提交 operation 改为写 Redis 并返回 `accepted`。
- WebSocket 广播使用 Redis `hotVersion`。
- `syncOperations` 支持 MySQL + Redis 拼接增量。

第三阶段引入周期落盘和恢复：

- 新增 `OperationPersistWorker` 批量写 MySQL。
- 新增 checkpoint 表和 `RecoveryWorker`。
- 前端区分 `accepted` 和 `persisted`。
- 增加积压、延迟、失败重试监控。

第四阶段引入自动压缩和冷热分离：

- 新增 `SnapshotCompactWorker`。
- snapshot 成功后归档或清理旧 operation。
- Redis 热日志按窗口清理。
- 历史 operation 可迁移到归档表，在线同步只查最新 snapshot 之后的日志。

### 21.15 适用边界

适合使用本方案的场景：

- 多人同时编辑同一画布。
- 节点拖拽、缩放、数据 patch 高频发生。
- MySQL operation insert TPS 或项目版本锁等待已经明显升高。
- 客户端断线重连需要快速补拉最近增量。
- 需要保留最终审计和恢复能力，但允许 Redis 与 MySQL 存在秒级短暂不一致。

不建议过早使用本方案的场景：

- 当前仍是单人或少量协作，MySQL 指标没有压力。
- 团队暂时没有 Worker、Redis AOF、恢复任务和监控告警的维护能力。
- 业务要求服务端 ack 后必须立即完成强持久化，不能接受 Redis 与 MySQL 短暂不一致。

最终推荐路线：先完成“前端降频 + 服务端 snapshot 化 + operation 压缩”，当 `submitOperation` P95、数据库 insert TPS、锁等待或单项目 operation 增长达到瓶颈后，再升级到“Redis 热日志 + MySQL 周期落盘”。

## 22. 结论

本方案推荐采用“服务端权威状态 + 节点级 operation log + WebSocket 项目房间广播”的路线，先实现可落地的 Figma 式基础协作体验，再逐步补齐可靠性、任务编排、资产中心化和局部 CRDT 能力。

该路线与当前项目技术栈匹配度高，可以复用现有 React Flow 画布和 Yudao WebSocket 基建，避免一开始全量引入 CRDT 带来的工程复杂度。首版重点应控制范围，只做节点/边协作、presence、服务端存储和基础断线恢复；待协作链路稳定后，再扩展到文本节点 Yjs、评论、版本分支、离线编辑等高级能力。
