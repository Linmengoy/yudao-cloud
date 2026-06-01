# AIGC 画布共享邀请技术方案

## 1. 目标

在现有 AIGC 画布多人协作底层能力之上，补齐用户可见的共享入口和成员管理能力。

核心目标：

- 在画布页面右上角增加“共享”入口。
- 支持查看当前画布项目成员。
- 支持通过 userId 邀请用户加入当前画布项目，后续升级为手机号、邮箱、昵称搜索。
- 支持设置成员角色：owner、editor、viewer。
- 支持复制协作链接。
- 支持被邀请用户通过链接打开同一个画布项目。
- 复用现有 projectId 协作模型，不重新设计画布同步协议。
- 支持成员变更后的实时权限刷新，被移除用户应立即退出或进入无权限状态。
- 支持租户边界校验、成员操作审计和基础防滥用策略。

## 2. 当前基础

当前项目已经具备以下基础能力：

- 画布项目通过 projectId 标识。
- 前端画布入口是 `/create/image?projectId=xxx`。
- 前端已有画布 API 封装：`yudao-ui/draw2video-client/src/features/canvas/canvas-api.ts`。
- 前端已有协作实时通信：`yudao-ui/draw2video-client/src/features/canvas/use-canvas-realtime.ts`。
- 前端已有协作消息定义：`yudao-ui/draw2video-client/src/features/canvas/canvas-realtime.ts`。
- 前端已有成员和角色类型定义：`yudao-ui/draw2video-client/src/features/canvas/types.ts`。
- 画布主页面已经维护 projectRole、projectMembers、isReadOnly 等状态。
- 后端已有画布项目、成员、快照、operation log、WebSocket 房间广播等协作基础。

当前缺口：

- 没有“共享”按钮。
- 没有邀请成员 UI。
- 没有复制协作链接 UI。
- 没有成员角色管理 UI。
- 没有被移除或角色变更后的完整前端交互闭环。
- 成员列表缺少 nickname、avatar、在线状态等协作体验字段。
- 邀请成员只支持 userId 时更适合内部测试，正式产品需要用户搜索。
- 成员管理缺少审计日志、租户校验、操作确认和频率限制。

## 3. 产品形态

第一版采用轻量共享弹窗，不做复杂团队空间。

入口位置：

- 画布页面右上角。
- 放在在线协作状态、保存状态、用户操作区附近。
- 按钮文案为“共享”。

弹窗内容：

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
- 角色只支持 editor、viewer。
- owner 不允许被修改和移除。
- 只有 owner 可以邀请、移除、修改角色。
- editor 可以复制链接，但不能管理成员。
- viewer 只能查看成员和复制链接。
- 移除成员、降级角色需要二次确认，避免误操作。
- 成员变更后需要通过 WebSocket 通知所有在线端刷新成员和权限。

正式版体验增强：

- 邀请输入框支持手机号、邮箱、昵称、用户 ID 搜索。
- 成员列表展示昵称、头像、角色、在线状态、最后活跃时间。
- 在线协作者颜色与 presence 光标颜色保持一致。
- 被移除用户实时关闭 WebSocket、禁用编辑能力，并跳转到项目列表或无权限页。
- 成员操作结果通过轻量 toast 提示，不阻断画布编辑。

## 4. 权限模型

沿用现有角色模型。

| 角色 | 查看画布 | 编辑画布 | 邀请成员 | 修改角色 | 删除成员 |
| --- | --- | --- | --- | --- | --- |
| owner | 是 | 是 | 是 | 是 | 是 |
| editor | 是 | 是 | 否 | 否 | 否 |
| viewer | 是 | 否 | 否 | 否 | 否 |

前端权限判断：

```ts
const canManageMembers = projectRole === "owner"
const canEditCanvas = projectRole === "owner" || projectRole === "editor"
const isReadOnly = projectRole === "viewer"
```

后端必须再次校验权限，不能只依赖前端判断。

## 5. 后端接口设计

### 5.1 查询成员列表

```http
GET /app-api/canvas/projects/{projectId}/members
```

响应：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "projectId": 10001,
      "userId": 20001,
      "nickname": "张三",
      "avatar": "https://example.com/avatar.png",
      "role": "owner",
      "joinedTime": "2026-06-01 10:00:00",
      "lastActiveTime": "2026-06-01 11:00:00"
    }
  ]
}
```

### 5.2 邀请成员

```http
POST /app-api/canvas/projects/{projectId}/members
```

请求：

```json
{
  "userId": 20002,
  "role": "editor"
}
```

响应：

```json
{
  "code": 0,
  "data": true
}
```

规则：

- 只有 owner 可以调用。
- 不能重复邀请同一个用户。
- 如果成员已存在，可以返回业务错误，或者更新其角色。
- role 只能是 editor 或 viewer，不能直接邀请 owner。
- 必须校验被邀请用户存在、状态正常、属于当前租户或允许协作的组织范围。
- 必须限制邀请频率，例如单项目每分钟最多邀请 10 次，防止滥用。
- 邀请成功后记录审计日志，并广播 `canvas-member-updated`。

第二阶段建议新增用户搜索接口：

```http
GET /app-api/canvas/member-candidates?keyword=xxx
```

响应：

```json
{
  "code": 0,
  "data": [
    {
      "userId": 20002,
      "nickname": "李四",
      "avatar": "https://example.com/avatar2.png",
      "email": "li@example.com",
      "mobileMask": "138****1234"
    }
  ]
}
```

搜索规则：

- keyword 支持用户 ID、昵称、邮箱、手机号。
- 手机号和邮箱返回时需要脱敏。
- 只能返回当前租户或当前组织可协作范围内的用户。
- 已经在项目内的成员需要标记 `joined=true`，前端禁用重复邀请。

### 5.3 修改成员角色

```http
PUT /app-api/canvas/projects/{projectId}/members/{memberId}
```

请求：

```json
{
  "role": "viewer"
}
```

规则：

- 只有 owner 可以修改。
- 不能修改项目创建者 owner。
- 第一版不做所有权转移。
- 只能在 editor 和 viewer 之间切换。
- 从 editor 降级为 viewer 属于敏感操作，前端需要二次确认。
- 当前用户被降级为 viewer 时，前端要立即进入只读模式，并关闭当前未提交的编辑交互。
- 后端需要记录角色变更审计日志。

### 5.4 移除成员

```http
DELETE /app-api/canvas/projects/{projectId}/members/{memberId}
```

规则：

- 只有 owner 可以移除。
- 不能移除 owner 自己。
- 被移除用户如果在线，后端通过 WebSocket 推送成员更新或踢出消息。
- 前端收到后退出画布或切换无权限状态。
- 移除成员属于敏感操作，前端必须二次确认。
- 后端移除成员后应广播 `member-removed`，目标用户在线时应收到明确的强制退出事件。
- 被移除用户的后续 REST API 和 WebSocket operation 必须被后端拒绝。
- 后端需要记录移除成员审计日志。

### 5.5 获取当前用户项目角色

如果项目详情接口已经返回 role，直接复用。

建议项目详情响应包含：

```json
{
  "id": 10001,
  "name": "我的画布",
  "currentVersion": 35,
  "role": "owner"
}
```

如果没有，则新增：

```http
GET /app-api/canvas/projects/{projectId}/my-role
```

## 6. 前端改造方案

### 6.1 API 封装

在 `yudao-ui/draw2video-client/src/features/canvas/canvas-api.ts` 增加：

```ts
getProjectMembers(projectId)
inviteProjectMember(projectId, payload)
updateProjectMemberRole(projectId, memberId, payload)
removeProjectMember(projectId, memberId)
```

类型建议放在 `yudao-ui/draw2video-client/src/features/canvas/types.ts`。

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

### 6.2 新增共享弹窗组件

建议新增：

```text
yudao-ui/draw2video-client/src/features/canvas/CanvasShareDialog.tsx
```

组件职责：

- 展示协作链接。
- 复制链接。
- 查询成员列表。
- 邀请成员。
- 修改成员角色。
- 移除成员。
- 根据当前用户角色控制按钮是否可用。

组件 props：

```ts
interface CanvasShareDialogProps {
  open: boolean
  projectId: number
  projectRole: CanvasProjectRole
  members: CanvasMember[]
  onOpenChange: (open: boolean) => void
  onMembersChange: (members: CanvasMember[]) => void
}
```

### 6.3 画布页面接入按钮

在 `yudao-ui/draw2video-client/src/app/(app)/create/image/page.tsx` 中：

- 增加 shareDialogOpen 状态。
- 在右上角操作区增加“共享”按钮。
- 点击后打开 CanvasShareDialog。
- 弹窗里使用当前 projectId 生成链接。

```ts
const shareUrl = `${window.location.origin}/create/image?projectId=${projectId}`
```

注意：

- 链接本身不代表权限。
- 用户打开链接后，后端仍然要校验他是否是项目成员。
- 如果不是成员，应展示“无权限访问”或跳转项目列表。

### 6.4 成员更新实时通知

复用现有 WebSocket 消息 `canvas-member-updated`。

成员变化后：

- 后端向项目房间广播 canvas-member-updated。
- 前端收到后重新调用 getProjectMembers(projectId)。
- 如果当前用户被降级为 viewer，前端立即进入只读模式。
- 如果当前用户被移除，前端提示无权限并跳转。

建议消息结构：

```json
{
  "type": "canvas-member-updated",
  "projectId": "10001",
  "operatorUserId": 20001,
  "targetUserId": 20002,
  "action": "role-updated"
}
```

action 可选值：

```text
member-added
role-updated
member-removed
```

前端处理规则：

- 收到 `member-added`：刷新成员列表，不影响当前画布编辑状态。
- 收到 `role-updated`：刷新项目详情和成员列表；如果目标用户是当前用户，则重新计算 `isReadOnly`。
- 收到 `member-removed`：刷新成员列表；如果目标用户是当前用户，则关闭共享弹窗、关闭 WebSocket、禁用画布编辑，并跳转到项目列表或展示无权限页。
- 如果刷新项目详情返回无权限，前端必须停止提交 operation，避免继续产生失败请求。

建议增加强制退出消息：

```json
{
  "type": "canvas-member-kicked",
  "projectId": "10001",
  "targetUserId": 20002,
  "reason": "member-removed"
}
```

`canvas-member-updated` 用于广播刷新，`canvas-member-kicked` 用于目标用户立即退出。第一版也可以只使用 `member-removed`，但前端必须能识别当前用户是否为 targetUserId。

### 6.5 交互安全与确认

以下操作必须二次确认：

- 移除成员。
- 将 editor 改为 viewer。
- 后续支持所有权转移时，转移 owner。

确认文案建议：

```text
确认移除该成员？移除后对方将无法继续访问当前画布，正在进行的协作连接也会断开。
```

```text
确认将该成员改为 viewer？对方将只能查看画布，不能继续编辑节点和连线。
```

### 6.6 共享入口布局

右上角当前已有在线协作提示，新增共享按钮时需要避免重叠。

推荐布局：

```text
右上角：成员头像/在线人数  共享按钮
顶部居中：新建、上传、清空等编辑工具
顶部提示：只读模式、参考图选择等临时状态
```

如果屏幕宽度不足，在线人数可以折叠为头像组或“协作中”图标。

## 7. 数据库设计

如果已有 canvas_member 表，直接复用。

建议字段：

```sql
CREATE TABLE canvas_member (
  id BIGINT NOT NULL PRIMARY KEY,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  invite_user_id BIGINT NULL,
  joined_time DATETIME NOT NULL,
  last_active_time DATETIME NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  deleted BIT NOT NULL DEFAULT 0
);
```

关键索引：

```sql
UNIQUE KEY uk_canvas_member_project_user (project_id, user_id, deleted);
KEY idx_canvas_member_user_id (user_id);
KEY idx_canvas_member_project_id (project_id);
```

注意：

- 如果项目使用逻辑删除，唯一索引要结合实际数据库规范处理。
- owner 可以来自 canvas_project.owner_user_id，也可以同步写入 canvas_member。
- 建议创建项目时，同时写入一条 owner 成员记录。

### 7.1 成员审计日志

生产环境建议新增成员操作审计表，便于追踪邀请、改角色、移除等敏感操作。

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

action 建议值：

```text
member-added
role-updated
member-removed
owner-transferred
```

审计规则：

- 邀请成员、修改角色、移除成员必须写审计日志。
- 审计日志不参与普通用户页面展示，优先供管理员、客服、风控排查。
- 后续如果支持邀请链接，也需要记录 token 创建、撤销、使用记录。

### 7.2 邀请链接表

第三阶段支持邀请链接时建议新增独立 token 表，不把公开授权直接绑定在 projectId 上。

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

关键索引：

```sql
UNIQUE KEY uk_canvas_invite_link_token (token);
KEY idx_canvas_invite_link_project_id (project_id);
```

邀请链接安全规则：

- token 必须使用高强度随机值，不能使用 projectId、userId 可猜测信息。
- token 支持过期时间、使用次数、主动撤销。
- 默认角色只能是 viewer 或 editor，不能是 owner。
- 使用 token 加入项目前仍需要登录态和租户校验。

## 8. 后端服务逻辑

建议新增或完善 AigcCanvasMemberService。

核心方法：

```java
List<CanvasMemberRespVO> getMembers(Long projectId)

void inviteMember(Long projectId, Long userId, String role)

void updateMemberRole(Long projectId, Long memberId, String role)

void removeMember(Long projectId, Long memberId)

CanvasProjectRole getUserRole(Long projectId, Long userId)
```

权限校验统一封装：

```java
checkProjectOwner(projectId, loginUserId)
checkProjectEditor(projectId, loginUserId)
checkProjectMember(projectId, loginUserId)
```

不同场景使用不同校验：

- 查看项目：checkProjectMember。
- 编辑节点：checkProjectEditor。
- 邀请成员：checkProjectOwner。
- 修改成员：checkProjectOwner。
- 删除成员：checkProjectOwner。

成员管理服务需要额外处理：

- 校验被邀请用户是否存在、是否属于当前租户、是否被禁用。
- 处理重复邀请：第一版返回“成员已存在”，第二版可支持直接修改角色。
- 处理 owner 保护：不能移除 owner，不能把 owner 降级，所有权转移单独设计。
- 写入审计日志：记录 operator、target、action、beforeRole、afterRole。
- 广播成员事件：成员新增、角色变更、成员移除后通知项目房间。
- 强制权限刷新：被移除或被降级用户的后续 REST 和 WebSocket operation 必须被拒绝。

### 8.1 租户与安全校验

后端必须遵守以下安全要求：

- 不能信任前端传入的 operatorUserId，操作者必须从登录态获取。
- 邀请 userId 时必须校验用户属于当前租户，或属于明确允许协作的组织范围。
- 成员列表只能被项目成员读取，不能通过 projectId 枚举其他项目成员。
- viewer 不能提交任何会修改画布的 operation。
- 被移除用户即使 WebSocket 还未断开，提交 operation 时也必须被服务端拒绝。
- 成员管理接口需要频率限制，避免通过 userId 枚举或批量骚扰。

## 9. 打开共享链接流程

用户 B 拿到链接：

```text
/create/image?projectId=10001
```

流程：

```text
用户打开链接
  -> 前端读取 projectId
  -> 调用项目详情或 snapshot API
  -> 后端校验当前用户是否为项目成员
  -> 是成员：返回项目、角色、snapshot
  -> 非成员：返回无权限
  -> 前端展示无权限页面或提示联系项目 owner
```

第一版不建议做“任何拿到链接的人都能加入”，避免项目泄露。

后续可以扩展邀请链接模型：

```text
/canvas/invite?token=xxxxx
```

## 10. 分阶段落地

### 10.1 第一阶段：最小可用共享

开发内容：

- 画布右上角增加“共享”按钮。
- 弹窗展示当前链接。
- 支持复制链接。
- 展示成员列表。
- 支持 owner 输入 userId 邀请成员。
- 支持 owner 移除成员。
- 支持 editor、viewer 角色。
- 移除成员、降级成员角色增加二次确认。
- 成员变更后通过 `canvas-member-updated` 刷新成员和权限。
- 当前用户被移除后立即进入无权限状态，不再提交 operation。
- 后端校验 owner 权限、租户边界、成员是否存在和角色合法性。
- 成员新增、改角色、移除写入审计日志。

验收标准：

- A 创建项目。
- A 邀请 B 为 editor。
- B 打开链接可以进入同一画布。
- B 移动节点，A 可以看到同步。
- A 把 B 改为 viewer 后，B 页面进入只读。
- A 移除 B 后，B 再打开链接提示无权限。
- A 移除在线的 B 后，B 当前页面立即提示无权限并停止编辑。
- viewer 直接调用编辑 operation 接口会被后端拒绝。
- 重复邀请同一用户不会产生重复成员记录。
- owner 不能被移除，也不能被降级。

### 10.2 第二阶段：体验增强

开发内容：

- 邀请输入支持手机号、邮箱、昵称搜索。
- 成员头像展示。
- 在线状态展示。
- 成员角色下拉切换。
- 被移除用户实时退出。
- 项目列表显示“我参与的项目”。
- 成员响应增加 nickname、avatar、emailMask、mobileMask。
- 在线成员头像组与远端 presence 颜色统一。
- 站内通知或消息中心提示被邀请加入项目。
- 成员管理操作增加 toast、loading、失败重试和空状态。
- 项目列表支持“我创建的 / 我参与的”筛选。

验收标准：

- owner 可以通过昵称、手机号、邮箱搜索到同租户用户。
- 已加入项目的用户在搜索结果中展示已加入状态，不能重复邀请。
- 成员列表展示头像、昵称、角色、在线状态。
- B 被邀请后能在站内通知中看到项目入口。

### 10.3 第三阶段：高级共享

开发内容：

- 邀请链接。
- 链接有效期。
- 链接默认角色。
- 禁止转发。
- 审批加入。
- 团队空间。
- 项目所有权转移。
- 邀请链接可撤销、可限制使用次数。
- 邀请链接使用记录进入审计日志。
- 多实例部署下，成员变更和踢出事件通过 Redis/MQ 广播。

验收标准：

- owner 创建 viewer 邀请链接后，新用户登录并确认加入，默认成为 viewer。
- 过期、撤销、超出使用次数的链接不能继续加入项目。
- 多实例部署时，被移除用户无论连接在哪个实例都能收到退出事件。
- 所有权转移后，旧 owner 降级为 editor 或 viewer，新 owner 可以管理成员。

## 11. 推荐实现顺序

1. 修复前端构建阻断问题，确保 `npm run build` 可作为交付门禁。
2. 后端确认 canvas_member 表和成员接口是否已经存在。
3. 补齐 POST /members、PUT /members/{id}、DELETE /members/{id}。
4. 补齐 owner 权限校验、租户校验、角色白名单和 owner 保护。
5. 增加成员操作审计日志。
6. 前端在 canvas-api.ts 增加成员管理 API。
7. 新增 CanvasShareDialog.tsx。
8. 在 page.tsx 右上角接入“共享”按钮，并调整右上角布局避免重叠。
9. 接入 canvas-member-updated 后自动刷新成员和角色。
10. 补充无权限、只读、被移除实时退出提示。
11. 增加移除成员和降级角色二次确认。
12. 两个账号联调验证共享协作闭环。
13. 补充接口测试：邀请、重复邀请、修改 owner、移除 owner、viewer 越权、跨租户邀请。

## 12. 方案评分与升级目标

当前方案按生产级共享协作评估为 78 / 100。

主要扣分项：

- 邀请体验偏工程化，第一版只支持 userId 邀请。
- 成员资料不足，缺少 nickname、avatar、在线状态。
- 被移除用户实时退出和强制断开协议需要明确。
- 缺少完整审计日志、租户校验、频率限制和接口测试。
- 构建存在历史类型问题，需要先修复为交付门禁。

提升到 85 分需要补齐：

- 用户搜索邀请。
- 成员头像和昵称。
- 被移除用户实时退出。
- 敏感操作二次确认。
- 租户边界校验。
- 成员操作审计日志。
- 后端接口测试和前端构建通过。

提升到 90 分以上需要补齐：

- 邀请链接 token。
- 链接有效期、撤销、使用次数。
- 站内通知或邮件邀请。
- 多实例房间事件广播。
- 项目所有权转移。
- 项目列表“我参与的”完整体验。

## 13. 结论

当前协作能力的主要问题不是底层同步链路缺失，而是共享入口和成员管理 UI 缺失。

第一版推荐采用“基于 projectId + canvas_member 的成员制共享”方案，不做公开链接。最小闭环为：共享按钮、成员弹窗、邀请 userId、复制链接、权限校验、只读模式、成员变更实时刷新、被移除实时退出。

该方案可以先满足内部协作和小范围内测；若要达到生产级体验，需要继续补齐用户搜索、成员资料、审计日志、租户安全、邀请链接和多实例事件广播。
