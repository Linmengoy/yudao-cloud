# AIGC 平台第一阶段前端完整开发计划

1. 保持前端规范统一
2. 保持css风格一致
3. 保持与后端的一致性

## 1. 计划定位

本文档面向 `c:\use\code\project\manman\yudao-ui`，覆盖第一阶段 AIGC 平台完整前端建设，包含管理端和用户端两个项目。

```text
draw2video-admin   管理端，面向运营、平台管理员、租户管理员
draw2video-client  用户端，面向普通用户、创作者
```

第一阶段目标：

```text
前端完整打通 AIGC MVP 赚钱闭环：注册登录 → 模型选择 → 价格预估 → 生成提交 → 任务进度 → 资产沉淀 → 钱包扣费 → 后台运营管理
```

第一阶段后端核心服务：

```text
yudao-module-member          会员注册登录、用户资料
yudao-module-aigc-model      模型与渠道服务
yudao-module-aigc-billing    计费钱包服务
yudao-module-aigc-task       任务调度服务
yudao-module-aigc-asset      资产中心服务
yudao-module-aigc-gen        生成服务
yudao-module-aigc-safety     审核风控服务
```

当前后端补充状态：

```text
member 邮箱验证码注册后端已完成
  ├── POST /app-api/member/auth/send-email-code
  ├── POST /app-api/member/auth/validate-email-code
  └── POST /app-api/member/auth/email-register
```

用户端开发时，邮箱注册不再按“待后端补接口”处理，而是直接对接上述接口。

后端数据库变更说明：

```text
yudao-module-member/yudao-module-member-server/src/main/resources/member-email-register.sql
```

部署或联调前需要执行该 SQL，补齐 `member_user` 邮箱字段、`member_email_code` 表和邮箱注册验证码邮件模板。

## 2. 前端项目范围


| 项目                | 路径                         | 技术栈                                      | 建设目标        |
| ------------------- | ---------------------------- | ------------------------------------------- | --------------- |
| `draw2video-admin`  | `yudao-ui/draw2video-admin`  | Vue3 + Vite + TypeScript + Element Plus     | AIGC 管理后台   |
| `draw2video-client` | `yudao-ui/draw2video-client` | Next.js + React + TypeScript + Tailwind CSS | AIGC 用户创作端 |

## 3. 第一阶段功能总览

### 3.1 管理端功能范围

管理端负责平台配置、运营管理、审核、账务、任务监控。

- AIGC 运营看板
- 模型渠道管理
- 模型配置管理
- 模型参数模板管理
- 模型价格规则管理
- 模型路由规则管理
- 租户模型授权管理
- 模型调用计量日志
- 生成记录管理
- 生成回调记录
- 渠道调用日志
- 任务列表监控
- 任务日志管理
- 任务回调记录
- 任务重试记录
- 资产列表管理
- 资产审核
- 资产下载日志
- 资产统计
- 钱包管理
- 冻结记录管理
- 计费流水管理
- 成本记录管理
- 充值订单管理
- 毛利统计
- 敏感词管理
- 审核记录管理
- 人工审核通过和拒绝

### 3.2 用户端功能范围

用户端负责用户注册登录、创作、任务、资产、钱包体验。

- 邮箱验证码注册
- 手机号验证码登录即注册
- 手机号密码登录
- Token 自动刷新
- 用户资料
- 钱包余额
- 钱包流水
- 模型列表
- 参数模板
- 价格预估
- 文本生成
- 图片生成
- 视频生成
- 任务列表
- 任务详情
- 资产列表
- 资产详情
- 资产下载
- 资产删除
- 扣费、冻结、退款提示

## 4. 管理端开发计划

### 4.1 管理端项目规范

项目路径：

```text
c:\use\code\project\manman\yudao-ui\draw2video-admin
```

开发规范：

- API 文件放在 `src/api/<module>/<resource>/index.ts`
- 页面放在 `src/views/<module>/<resource>/index.vue`
- API URL 不写 `/admin-api`，只写业务路径
- 新增使用 `/create`
- 修改使用 `/update`
- 删除使用 `/delete?id=`
- 分页使用 `/page`
- 详情使用 `/get?id=`
- 导出使用 `/export-excel`
- 按钮权限使用 `v-hasPermi="['模块:资源:动作']"`
- 菜单路由由后端动态返回，前端页面路径必须匹配 `src/views` 下组件路径

### 4.2 管理端菜单规划

```text
AIGC 平台
  ├── 运营看板
  ├── 模型中心
  │   ├── 渠道商管理
  │   ├── 模型管理
  │   ├── 参数模板
  │   ├── 价格规则
  │   ├── 路由规则
  │   ├── 租户授权
  │   └── 调用计量
  ├── 生成中心
  │   ├── 生成记录
  │   ├── 回调记录
  │   └── 渠道日志
  ├── 任务中心
  │   ├── 任务列表
  │   ├── 任务日志
  │   ├── 回调记录
  │   └── 重试记录
  ├── 资产中心
  │   ├── 资产列表
  │   ├── 下载日志
  │   └── 资产统计
  ├── 计费中心
  │   ├── 钱包管理
  │   ├── 冻结记录
  │   ├── 计费流水
  │   ├── 成本记录
  │   ├── 充值套餐
  │   ├── 充值订单
  │   └── 毛利统计
  └── 安全审核
      ├── 敏感词管理
      └── 审核记录
```

### 4.3 管理端 API 目录规划

```text
draw2video-admin/src/api/aigc
  ├── dashboard
  ├── model
  │   ├── provider
  │   ├── model
  │   ├── param
  │   ├── price
  │   ├── route
  │   ├── tenant
  │   └── usage
  ├── gen
  │   ├── record
  │   ├── callback
  │   └── provider-log
  ├── task
  │   ├── task
  │   ├── log
  │   ├── callback
  │   └── retry
  ├── asset
  │   ├── asset
  │   ├── download-log
  │   └── statistics
  ├── billing
  │   ├── wallet
  │   ├── freeze
  │   ├── record
  │   ├── cost
  │   ├── recharge-package
  │   ├── recharge
  │   └── statistics
  └── safety
      ├── sensitive-word
      └── audit-record
```

### 4.4 管理端页面目录规划

```text
draw2video-admin/src/views/aigc
  ├── dashboard
  ├── model
  │   ├── provider
  │   ├── model
  │   ├── param
  │   ├── price
  │   ├── route
  │   ├── tenant
  │   └── usage
  ├── gen
  │   ├── record
  │   ├── callback
  │   └── provider-log
  ├── task
  │   ├── task
  │   ├── log
  │   ├── callback
  │   └── retry
  ├── asset
  │   ├── asset
  │   ├── download-log
  │   └── statistics
  ├── billing
  │   ├── wallet
  │   ├── freeze
  │   ├── record
  │   ├── cost
  │   ├── recharge-package
  │   ├── recharge
  │   └── statistics
  └── safety
      ├── sensitive-word
      └── audit-record
```

### 4.5 管理端页面清单

运营看板：

- 今日生成次数、成功任务数、失败任务数
- 今日消费积分、成本、毛利
- 累计资产数
- 生成类型分布
- 模型调用排行
- 渠道失败排行
- 近 7 日生成趋势
- 近 7 日收入和成本趋势

模型中心：

- 渠道商分页、新增、编辑、删除、启停、密钥脱敏、健康状态
- 模型分页、新增、编辑、删除、上下线、能力配置、用户端展示配置
- 参数模板分页、新增、编辑、删除、参数类型、默认值、范围、枚举值配置
- 价格规则分页、新增、编辑、删除、成本价、销售价、租户覆盖、价格试算
- 路由规则分页、模型渠道映射、权重、优先级、启停状态
- 租户授权分页、启用状态、用户端可见、默认模型、日限额、并发限制
- 调用计量日志分页、token 用量、成本价、销售价、第三方任务号、错误摘要

生成中心：

- 生成记录分页、筛选、详情、手动同步、失败原因查看
- 回调记录分页、回调原文摘要、解析数据、处理状态
- 渠道日志分页、HTTP 状态码、调用耗时、请求响应摘要、敏感字段脱敏

任务中心：

- 任务分页、按任务类型、状态、用户、模型筛选
- 任务进度展示
- 任务详情抽屉
- 任务日志时间线
- 回调幂等记录
- 重试记录
- 超时任务标识
- 失败和退款状态展示

资产中心：

- 资产分页
- 图片、视频、音频、文档预览
- 按资产类型、审核状态、可见性、用户筛选
- 资产详情
- 审核通过、审核拒绝
- 可见性调整
- 下载次数、使用次数展示
- 软删除和恢复
- 下载日志分页
- 资产统计

计费中心：

- 钱包分页和详情
- 用户余额、冻结余额展示
- 管理端手动调整积分
- 冻结记录分页
- 计费流水分页
- 成本记录分页
- 充值套餐分页、新增、编辑、删除、启停、推荐状态、排序配置
- 充值订单分页
- 毛利统计
- 账务异常标识
- 任务 ID 关联跳转

安全审核：

- 敏感词分页、新增、编辑、删除、启停
- 按场景、状态、风险等级筛选
- 审核记录分页
- 审核内容摘要、命中词、风险等级展示
- 人工通过、人工拒绝、拒绝原因填写
- 资产审核状态同步结果展示

## 5. 用户端开发计划

## 5.1 用户端项目现状

项目路径：

```text
c:\use\code\project\manman\yudao-ui\draw2video-client
```

技术栈：

- Next.js App Router
- React
- TypeScript
- Tailwind CSS
- React Flow
- React Query
- IndexedDB 本地草稿缓存

当前产品形态：

- 营销页
- 登录弹窗
- 工作台 Shell
- `/app` 创作首页
- `/create/image` React Flow 图片画布
- `/create/video` 视频创作入口
- `/tasks` 任务列表
- `/tasks/[id]` 任务详情
- `/wallet` 钱包页
- `/profile` 个人中心

当前主要缺口：

- member 鉴权主链路已接入真实接口，后续重点是登录后继续原目标页面 / 原生成操作
- 邮箱验证码注册、手机号验证码登录、手机号密码登录已完成入口接入，邮箱验证码独立预校验尚未串联到注册提交流程
- 钱包数据需要接入真实接口
- 模型列表和价格预估需要接入 AIGC 后端
- 生成调用需要从本地 provider route 切到 `aigc-gen`
- 任务和资产需要从 mock 切到真实接口
- Profile 已接入真实会员资料并支持昵称、头像 URL 修改，头像上传和账号安全仍待补齐

## 6. 用户端第一阶段范围

### 3.1 P0 必须完成

- 邮箱验证码注册
- 手机号验证码登录即注册
- 手机号密码登录
- 登录态保持
- Token 自动刷新
- 退出登录
- 用户资料获取
- 钱包余额获取
- 用户可用模型列表
- 模型参数模板
- 价格预估
- 文本生成
- 图片生成
- 视频生成
- 任务列表
- 任务详情
- 资产列表
- 资产详情
- 资产下载
- 资产删除
- 生成成功扣费提示
- 生成失败退款或释放冻结提示

当前完成状态：

- 已完成邮箱验证码注册、手机号验证码登录即注册、手机号密码登录入口接入
- 已完成登录态保持、Token 自动刷新、refreshToken 失效清理和退出登录
- 已完成用户资料获取、Profile 基础展示和昵称、头像地址修改
- 已完成登录弹窗、独立 `/login`、独立 `/register` 页面
- 已完成工作台布局级登录守卫，未登录访问 `(app)` 路由会跳回首页并打开登录弹窗
- 钱包余额、模型、价格预估、生成、任务、资产仍需继续对接 AIGC 真实接口

### 3.2 P1 上线前建议完成

- 邮箱密码登录
- 邮箱找回密码
- 用户资料修改
- 修改密码
- 钱包流水
- 充值入口预留
- 图生图体验预留
- 视频轮询退避策略
- 画布草稿保存增强
- 文件上传资产化

### 3.3 P2 后续补齐

- 音频生成入口
- 文档生成入口
- PPT 生成入口
- 数字人入口
- 项目化保存
- 模板入口
- 工作流入口
- 社区入口

## 7. 用户端路由规划

第一阶段保留现有路由，并补充注册和资产页面。

```text
营销页
  ├── /
  ├── /pricing
  ├── /login
  ├── /register
  └── /forgot-password

工作台
  ├── /app
  ├── /create/image
  ├── /create/video
  ├── /tasks
  ├── /tasks/[id]
  ├── /assets
  ├── /assets/[id]
  ├── /wallet
  └── /profile
```

桌面端主体验仍然以登录注册弹窗为主，独立页面用于移动端、刷新兜底、支付回跳和链接分享。

## 8. 用户端目录规划

### 8.1 API 封装目录

建议新增：

```text
draw2video-client/src/lib/aigc-api
  ├── auth.ts
  ├── model.ts
  ├── gen.ts
  ├── task.ts
  ├── asset.ts
  ├── wallet.ts
  └── types.ts
```

当前 member 实现说明：认证与个人资料已按 feature 拆分到 `src/features/auth`、`src/features/profile`，并统一复用 `src/lib/api-client.ts`；后续 AIGC 业务 API 不再新增独立 auth fetch 客户端。

### 8.2 认证模块目录

```text
draw2video-client/src/features/auth
  ├── AuthModal.tsx
  ├── auth-api.ts
  ├── auth-store.tsx
  └── auth-types.ts

draw2video-client/src/features/profile
  ├── profile-api.ts
  └── profile-types.ts
```

后续可在不破坏现有主链路的前提下继续拆分 `login-form.tsx`、`register-form.tsx`、`sms-code-button.tsx`、`email-code-button.tsx`、`forgot-password-form.tsx`。

### 8.3 资产模块目录

```text
draw2video-client/src/features/assets
  ├── asset-card.tsx
  ├── asset-preview.tsx
  ├── asset-actions.tsx
  └── asset-types.ts
```

### 8.4 生成模块目录

```text
draw2video-client/src/features/generation
  ├── model-select.tsx
  ├── price-estimate.tsx
  ├── generation-status.tsx
  ├── generation-result.tsx
  └── generation-types.ts
```

## 9. 注册登录方案

### 9.1 产品形态

登录注册入口：

- 营销页右上角“登录 / 注册”
- 首页 CTA 点击后未登录时打开登录弹窗
- 工作台未登录访问时打开登录弹窗
- 生成提交前未登录时打开登录弹窗
- 钱包、资产、任务页面未登录时打开登录弹窗

弹窗 Tab：

```text
手机号登录
邮箱注册
密码登录
找回密码
```

### 9.2 当前后端已有能力

已有接口：

```text
POST /app-api/member/auth/login
POST /app-api/member/auth/sms-login
POST /app-api/member/auth/send-sms-code
POST /app-api/member/auth/validate-sms-code
POST /app-api/member/auth/refresh-token
POST /app-api/member/auth/logout
GET  /app-api/member/user/get
PUT  /app-api/member/user/update
```

当前用户端实现状态：`auth-api.ts` 已封装 `login`、`sms-login`、`send-sms-code`、`send-email-code`、`validate-email-code`、`email-register`、`logout`；`refresh-token` 由 `api-client.ts` 在 401 场景统一处理。

当前后端手机号体系特点：

```text
手机号验证码登录 = 登录 + 自动注册
```

也就是说，手机号不存在时，调用 `sms-login` 会自动创建会员。

### 9.3 邮箱注册接口

邮箱注册接口已由后端落地：

```text
POST /app-api/member/auth/send-email-code
POST /app-api/member/auth/validate-email-code
POST /app-api/member/auth/email-register
```

接口说明：


| 接口                                            | 状态   | 用户端用途                           |
| ----------------------------------------------- | ------ | ------------------------------------ |
| `POST /app-api/member/auth/send-email-code`     | 已落地 | 发送邮箱验证码                       |
| `POST /app-api/member/auth/validate-email-code` | 已落地 | 校验邮箱验证码，可用于提交前预校验   |
| `POST /app-api/member/auth/email-register`      | 已落地 | 邮箱验证码注册，成功后返回登录 Token |

请求体约定：

```text
send-email-code:
  email: string
  scene: REGISTER

validate-email-code:
  email: string
  scene: REGISTER
  code: string

email-register:
  email: string
  code: string
  password: string
  agreeTerms: boolean
  inviteCode?: string
```

注册成功返回值复用登录响应：

```text
userId
accessToken
refreshToken
expiresTime
openid
```

### 9.4 邮箱注册流程

```text
输入邮箱
  ↓
点击发送邮箱验证码
  ↓
输入验证码
  ↓
设置密码
  ↓
确认密码
  ↓
勾选用户协议和隐私政策
  ↓
提交注册
  ↓
注册成功后保存 token
  ↓
拉取用户信息
  ↓
拉取钱包信息
  ↓
进入工作台
```

当前实现状态：邮箱验证码发送和邮箱注册已接入真实接口，注册成功后保存 token、拉取用户资料和钱包并进入 `/app`；`validate-email-code` 已封装但未作为提交前单独步骤串联。

表单字段：


| 字段              | 必填 | 校验                        |
| ----------------- | ---- | --------------------------- |
| `email`           | 是   | 邮箱格式                    |
| `code`            | 是   | 6 位验证码                  |
| `password`        | 是   | 8-32 位，至少包含字母和数字 |
| `confirmPassword` | 是   | 必须和密码一致              |
| `agreeTerms`      | 是   | 必须勾选                    |
| `inviteCode`      | 否   | 邀请码预留                  |

### 9.5 手机号验证码登录即注册

流程：

```text
输入手机号
  ↓
获取短信验证码
  ↓
输入验证码
  ↓
调用 sms-login
  ↓
如果手机号不存在，后端自动创建用户
  ↓
登录成功后保存 token
  ↓
进入工作台
```

接口：

```text
POST /app-api/member/auth/send-sms-code
POST /app-api/member/auth/sms-login
```

### 9.6 密码登录

流程：

```text
输入手机号
  ↓
输入密码
  ↓
调用 login
  ↓
登录成功后保存 token
  ↓
进入工作台
```

接口：

```text
POST /app-api/member/auth/login
```

### 9.7 Token 管理

存储建议：


| 数据           | 存储位置                   | 说明                      |
| -------------- | -------------------------- | ------------------------- |
| `accessToken`  | localStorage 或安全 Cookie | 第一阶段可用 localStorage |
| `refreshToken` | localStorage 或安全 Cookie | 用于刷新登录态            |
| `expiresTime`  | localStorage               | 判断是否接近过期          |
| `userInfo`     | 内存 + localStorage 缓存   | 页面刷新后恢复展示        |
| `wallet`       | 内存状态                   | 进入钱包或生成前刷新      |

当前实现状态：`accessToken`、`refreshToken`、`expiresTime` 已持久化到 localStorage；用户和钱包保存在 Auth Store 内存状态，刷新页面时通过 token 恢复并重新拉取。

认证 Store 状态：

```text
AuthState
  ├── user
  ├── accessToken
  ├── refreshToken
  ├── expiresTime
  ├── isAuthenticated
  ├── isInitializing
  ├── isLoginModalOpen
  ├── authMode
  └── redirectTo
```

认证 Store 方法：

```text
loginByPassword(mobile, password)
loginBySms(mobile, code)
registerByEmail(req)
sendSmsCode(scene, mobile)
sendEmailCode(scene, email)
validateEmailCode(scene, email, code)
refreshToken()
fetchUser()
logout()
openLoginModal(mode, redirectTo)
closeLoginModal()
```

### 9.8 注册登录验收

- 新用户可通过邮箱验证码完成注册
- 邮箱注册成功后自动登录
- 新用户可通过手机号验证码登录即注册
- 老用户可通过手机号密码登录
- 登录后刷新页面仍保持登录态
- accessToken 过期后可自动刷新
- refreshToken 失效后清理登录态并打开登录弹窗
- 未登录访问工作台会触发登录弹窗
- 退出登录后无法访问任务、资产、钱包页面
- 密码、验证码、token 不出现在日志和 URL 中

当前缺口：未登录访问工作台已触发登录弹窗，但登录后继续原始目标页面或继续生成提交原操作仍未完整闭环；找回密码入口保留但未实现表单和接口。

## 10. 钱包页面

页面路径：

```text
/wallet
```

开发内容：

- 替换 mock 钱包数据
- 展示可用积分
- 展示冻结积分
- 展示累计充值
- 展示累计消费
- 展示流水列表
- 展示生成任务关联流水
- 充值按钮预留
- 余额不足时引导充值

接口：

```text
GET /app-api/aigc/billing/wallet/get
GET /app-api/aigc/billing/wallet/record/page
POST /app-api/aigc/billing/recharge/create
```

## 11. 模型和价格

开发内容：

- 获取当前租户可用模型列表
- 按文本、图片、视频类型筛选模型
- 展示模型名称、能力、价格、预计耗时
- 获取模型参数模板
- 根据参数实时预估价格
- 生成前展示预计消耗积分

接口：

```text
GET /app-api/aigc/model/list
GET /app-api/aigc/model/get
GET /app-api/aigc/model/param/list
POST /app-api/aigc/model/price/calculate
```

## 12. 文本生成

页面入口：

```text
/app
/create/image 中的 TextNode
```

开发内容：

- 文本生成 prompt 输入
- 模型选择
- 参数选择
- 价格预估
- 提交生成
- 展示生成状态
- 成功后展示文本结果
- 失败后展示失败原因和退款提示

接口：

```text
POST /app-api/aigc/gen/text/generate
GET /app-api/aigc/gen/result
```

## 13. 图片生成

页面入口：

```text
/create/image
```

开发内容：

- 保留现有 React Flow 画布
- 图片节点生成接入后端 `aigc-gen`
- 支持文生图
- 支持图生图预留
- 支持参考图上传
- 生成结果原地替换节点
- 生成结果进入资产中心
- 节点展示任务状态
- 节点展示扣费结果
- 失败时展示失败原因

接口：

```text
POST /app-api/aigc/gen/image/text-to-image
POST /app-api/infra/file/upload
GET /app-api/aigc/gen/result
```

## 14. 视频生成

页面入口：

```text
/create/video
/create/image 中 image -> video 节点
```

开发内容：

- 视频 prompt 输入
- 支持图片作为参考
- 支持比例、时长等参数
- 提交异步视频任务
- 轮询任务状态
- 成功后展示视频播放器
- 失败后展示失败原因
- 支持任务详情跳转

接口：

```text
POST /app-api/aigc/gen/video/text-to-video
GET /app-api/aigc/gen/result
GET /app-api/aigc/task/get
```

## 15. 任务中心

页面路径：

```text
/tasks
/tasks/[id]
```

开发内容：

- 替换 mock 任务列表
- 按状态筛选
- 按类型筛选
- 显示任务进度
- 显示任务结果
- 显示消费积分
- 显示失败原因
- 显示退款状态
- 支持跳转资产详情

接口：

```text
GET /app-api/aigc/task/page
GET /app-api/aigc/task/get
```

## 16. 资产中心

页面路径：

```text
/assets
/assets/[id]
```

开发内容：

- 新增用户资产列表页
- 新增用户资产详情页
- 图片、视频、音频、文档预览
- 资产下载
- 资产删除
- 资产来源任务展示
- 资产审核状态展示
- 私有和公开状态展示

接口：

```text
GET /app-api/aigc/asset/page
GET /app-api/aigc/asset/get
POST /app-api/aigc/asset/upload
POST /app-api/aigc/asset/download
DELETE /app-api/aigc/asset/delete
```

## 17. 用户资料页

页面路径：

```text
/profile
```

开发内容：

- 展示头像
- 展示昵称
- 展示手机号
- 展示邮箱和邮箱验证状态
- 修改昵称
- 修改头像地址
- 修改密码，P1
- 绑定邮箱，P1
- 退出登录

当前实现状态：`/profile` 已展示真实头像、昵称、手机号、邮箱、账号状态，支持昵称和头像 URL 保存，支持退出登录；头像文件上传 / 裁剪、修改密码、绑定或换绑邮箱、换绑手机号仍未完成。

接口：

```text
GET /app-api/member/user/get
PUT /app-api/member/user/update
PUT /app-api/member/user/update-password
PUT /app-api/member/user/update-email
```

## 18. 联调顺序

### 18.1 管理端联调顺序

```text
1. AIGC 菜单和权限配置
2. AIGC API 文件创建
3. 模型中心页面联调
4. 计费中心页面联调
5. 任务中心页面联调
6. 资产中心页面联调
7. 安全审核页面联调
8. 生成中心页面联调
9. 运营看板联调
10. 导出、详情、状态操作、权限按钮验收
```

### 18.2 用户端联调顺序

```text
1. 邮箱验证码发送和邮箱验证码注册，已接入
2. 手机号验证码登录即注册，已接入
3. 手机号密码登录，已接入
4. Token 刷新、refreshToken 失效清理和退出登录，已接入
5. 用户资料获取和昵称、头像地址修改，已接入
6. 钱包余额
7. 模型列表和参数模板
8. 价格预估
9. 文本生成
10. 图片生成
11. 视频生成和轮询
12. 任务列表和详情
13. 资产列表和详情
14. 钱包扣费、冻结、退款提示
```

## 19. 工程验收

- `draw2video-client` 可通过 lint
- `draw2video-client` 可正常 build
- `draw2video-admin` 可通过类型检查
- `draw2video-admin` 可正常 build
- 新增 API 类型完整
- 新增组件命名规范
- 无浏览器控制台明显报错
- 无未处理 Promise 异常
- 无前端硬编码密钥

## 20. 业务验收

### 20.1 管理端验收

- 所有 AIGC 菜单可正常进入
- 所有列表支持分页、搜索、重置
- 所有新增、编辑、删除操作可用
- 所有状态操作有二次确认
- 所有按钮权限生效
- 所有接口错误有提示
- 所有敏感字段脱敏展示
- 所有时间、金额、状态格式统一
- 所有详情弹窗或抽屉可查看完整业务信息
- 所有跨模块 ID 可跳转或复制
- 导出接口可正常下载
- 多租户数据隔离正确

### 20.2 用户端验收

- 未登录访问工作台会触发登录弹窗
- 邮箱验证码注册可完成
- 手机号验证码登录即注册可完成
- 手机号密码登录可完成
- 登录后 token 可持久化和刷新
- refreshToken 失效后可清理登录态并重新打开登录弹窗
- `/login` 和 `/register` 可作为独立兜底页面进入登录注册流程
- `/profile` 可展示真实会员资料并修改昵称、头像地址
- 用户可看到钱包余额
- 用户可看到可用模型
- 用户提交生成前可看到预计消耗
- 余额不足时不能提交收费生成
- 文本生成可成功返回结果
- 图片生成可成功返回资产
- 视频生成可轮询到最终状态
- 任务列表展示真实任务
- 任务详情展示状态、模型、消耗、结果和失败原因
- 资产列表展示真实资产
- 资产详情可预览、下载、删除
- 生成成功后扣费提示准确
- 生成失败后退款或释放冻结提示准确
- 用户端不暴露模型渠道密钥和成本价

## 21. 最终交付物

管理端交付物：

- AIGC 管理菜单
- AIGC API TypeScript 封装
- 模型中心页面
- 生成中心页面
- 任务中心页面
- 资产中心页面
- 计费中心页面
- 安全审核页面
- 运营看板页面
- 权限按钮配置
- 菜单 SQL 或菜单配置说明

用户端交付物：

- 邮箱验证码注册
- 手机号验证码登录即注册
- 手机号密码登录
- 登录弹窗、独立登录页和独立注册页
- Token 自动刷新
- 用户资料页，已完成真实资料展示和昵称、头像地址修改
- 钱包页面
- 模型选择和价格预估
- 文本生成
- 图片生成
- 视频生成
- 任务中心
- 资产中心
- 生成结果展示
- 扣费、退款、失败提示

最终上线标准：

```text
用户端能完成：注册 / 登录 → 查看钱包 → 选择模型 → 预估价格 → 提交生成 → 查看进度 → 获得结果 → 查看资产 → 钱包扣费
```

会员模块双端前端开发计划详见：`yudao-module-member双端前端开发计划.md`。

当前 member 前端已完成 P0 主链路补强：用户端真实登录注册弹窗、`/login` 独立兜底页、`/register` 独立注册页、Token 保存与 401 自动刷新、refreshToken 失效清理、资料页基础展示与昵称 / 头像地址修改；管理端已补充会员列表邮箱 / 登录时间 / 标签 / 等级 / 分组筛选、手机号和邮箱脱敏、详情权限、启用 / 禁用独立入口、等级原因必填、积分和余额调整原因及二次确认。

当前 member 前端剩余缺口：用户端头像上传、账号安全、找回密码、邮箱验证码独立预校验、登录后继续原目标页或原生成操作；管理端 AIGC 任务 / 资产专属会员关联 Tab、邮箱验证状态、注册来源、最近登录 IP、AIGC 用户标识、会员账号安全后台操作和审计能力。
