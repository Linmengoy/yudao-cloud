# AIGC 计费钱包前端开发计划

## 一、背景与目标

本计划参考 `yudao-module-aigc-billing` 后端计费钱包模块，并结合 `AIGC平台第一阶段用户端前端开发计划.md` 制定，面向 AIGC 平台第一阶段用户端与管理端的计费钱包前端建设。

计费钱包前端的核心目标是打通用户侧商业化闭环：登录后查看钱包余额，选择模型并查看价格预估，提交生成任务后展示积分冻结状态，任务成功后展示扣费结果，任务失败后展示冻结释放结果，并允许用户在钱包页查看完整流水。

第一阶段用户端统一以“积分”作为展示和结算单位，不向用户暴露供应商成本、毛利、支付内部字段、管理员备注等后台信息。管理端负责承接运营侧钱包管理、赠送积分、余额调整、冻结记录、计费流水、充值订单、成本记录和经营统计。

## 二、建设范围

### 2.1 用户端范围

- 钱包余额展示
- 冻结余额展示
- 钱包流水列表
- 冻结记录列表
- 消费统计展示
- 充值入口与充值订单
- 生成页价格预估展示
- 余额不足阻断
- 生成任务冻结、扣费、释放提示
- 任务详情中的计费结果展示

### 2.2 管理端范围

- 钱包管理
- 冻结记录管理
- 计费流水管理
- 充值订单管理
- 成本记录管理
- 经营统计看板
- 积分赠送
- 余额人工调整
- 数据导出

### 2.3 不在第一阶段范围

- 优惠券
- 套餐包权益
- 会员等级权益抵扣
- 分销返佣
- 多币种用户侧结算
- 复杂营销活动
- 用户侧成本或毛利展示

## 三、技术栈与目录规范

### 3.1 用户端

用户端项目参考 `draw2video-client`，采用 Next.js App Router、React、TypeScript、Tailwind CSS、React Query。

实际落地采用当前 `draw2video-client` 的 Feature 分层，不再新增 `src/lib/aigc-api` 目录，钱包相关接口与类型统一放在 `src/features/wallet` 下，符合 `AGENTS.md` 中“使用现有项目 helpers 和组件，避免新增不必要抽象”的要求。

实际接口封装目录：

```text
draw2video-client/src/features/wallet/
├── wallet-api.ts
├── wallet-types.ts
└── mock-wallet.ts
```

计费钱包新增或重点维护文件：

- `wallet-api.ts`：钱包、流水、冻结、充值接口与积分格式化工具
- `wallet-types.ts`：钱包、流水、冻结、充值订单类型定义
- `auth-store.tsx`：登录态初始化后拉取真实 AIGC 钱包
- `app/(app)/wallet/page.tsx`：用户端钱包与用量页面
- `app/(app)/layout.tsx`：左侧工作区底部钱包积分入口
- `model.ts`：价格预估接口返回积分消耗字段
- `gen.ts`：生成提交结果展示冻结信息
- `task.ts`：任务详情展示扣费、释放、失败原因等状态

用户端设计必须遵循 `draw2video-client/design/DESIGN.md` 与 `draw2video-client/AGENTS.md`：

- 登录后工作区使用左侧 sidebar 作为主导航
- 钱包入口和头像控制尽量放在 sidebar 底部区域
- 页面保持 warm、quiet、utilitarian，不做后台管理台风格
- 使用 cream 背景、warm border、charcoal 文本、紧凑控件
- 不使用饱和色强调收入/支出，避免破坏 Copse 的暖色克制风格

### 3.2 管理端

管理端项目参考 `draw2video-admin`，采用 Vue3、Vite、TypeScript、Element Plus。

建议 API 目录：

```text
draw2video-admin/src/api/aigc/billing/
├── wallet/index.ts
├── freeze/index.ts
├── record/index.ts
├── recharge/index.ts
├── cost/index.ts
└── statistics/index.ts
```

建议页面目录：

```text
draw2video-admin/src/views/aigc/billing/
├── wallet/index.vue
├── freeze/index.vue
├── record/index.vue
├── recharge/index.vue
├── cost/index.vue
└── statistics/index.vue
```

管理端接口 URL 遵循现有前端规范，业务路径不写 `/admin-api` 前缀。

管理端已按上述目录落地 API 与页面。管理端业务页面由后端菜单动态路由匹配，菜单 `component` 建议配置：

- `aigc/billing/wallet/index`
- `aigc/billing/freeze/index`
- `aigc/billing/record/index`
- `aigc/billing/recharge/index`
- `aigc/billing/cost/index`
- `aigc/billing/statistics/index`

## 四、用户端页面规划

### 4.1 工作台钱包入口

位置：工作台顶部、侧边栏、个人中心入口。

展示内容：

- 可用积分
- 冻结积分
- 充值入口
- 钱包页入口

交互规则：

- 页面初始化后调用钱包查询接口
- 生成任务状态变化后刷新钱包余额
- 余额不足时展示充值引导
- 未登录时引导登录，不请求钱包接口

接口：

- `GET /app-api/aigc/billing/wallet/get`

### 4.2 钱包首页

展示内容：

- 可用积分
- 冻结积分
- 累计充值
- 累计赠送
- 累计消费
- 累计退款
- 最近流水
- 充值入口

页面规则：

- 金额统一展示为积分
- 冻结余额旁提供说明：“生成中的任务暂时占用，失败后自动退回”
- 最近流水默认展示最新 10 条
- 点击“查看全部”进入完整流水列表

接口：

- `GET /app-api/aigc/billing/wallet/get`
- `GET /app-api/aigc/billing/wallet/record/page`
- `GET /app-api/aigc/billing/wallet/statistics`

### 4.3 钱包流水列表

展示内容：

- 流水编号
- 流水类型
- 积分变动
- 变动后可用余额
- 变动后冻结余额
- 关联任务编号
- 备注
- 创建时间

筛选条件：

- 流水类型
- 时间范围
- 任务编号

交互规则：

- 入账类流水展示为 `+积分`
- 消费类流水展示为 `-积分`
- 冻结类流水需要区分“从可用转冻结”，避免用户误解为已扣费
- 点击任务编号跳转任务详情

接口：

- `GET /app-api/aigc/billing/wallet/record/page`

### 4.4 冻结记录列表

展示内容：

- 冻结编号
- 冻结金额
- 已扣费金额
- 已释放金额
- 冻结状态
- 关联任务编号
- 过期时间
- 创建时间

筛选条件：

- 冻结状态
- 时间范围
- 任务编号

接口：

- `GET /app-api/aigc/billing/wallet/freeze/page`

### 4.5 充值页面

第一阶段充值能力采用“后台可配置充值套餐 + 用户端按套餐创建充值订单”的方式落地。

充值套餐展示：

- 用户端 `/pricing` 页面读取后台启用的充值套餐
- 套餐字段包含套餐名称、支付金额、充值积分、赠送积分、到账总积分、描述、权益说明、是否推荐、排序、状态
- 支付金额后端按“分”存储，用户端展示为“元”
- 权益说明按换行拆分为卡片列表
- 接口加载失败时展示错误态，不再把接口异常误展示为“暂无可用价格方案”

创建订单：

- 用户点击套餐后调用按套餐创建充值订单接口
- 前端只传 `packageId`，不传支付金额和到账积分
- 后端校验套餐存在且启用
- 后端以套餐配置快照生成充值订单，保证展示价、支付金额、到账积分一致
- 创建成功后跳转充值收银台页，并携带 `rechargeOrderId`、`payOrderId` 和 `payAppId`

支付链路：

- 充值收银台路径建议为 `/checkout/recharge?rechargeOrderId=xxx&payOrderId=xxx`
- 收银台根据 `payAppId` 查询可用支付渠道，展示支付宝、微信等渠道入口
- 用户选择渠道后调用 Pay 模块提交支付订单
- 根据 Pay 返回的 `displayMode/displayContent` 展示二维码、跳转链接或其它支付内容
- 收银台轮询 Pay 支付订单状态，并同步 AIGC 充值订单状态
- 支付成功后跳转钱包页 `/wallet?rechargeOrderId=xxx`，刷新钱包余额、充值订单和计费流水
- 支付取消、超时或失败时停留收银台，允许用户重新选择渠道或返回价格页

接口：

- `GET /app-api/aigc/billing/recharge-package/list-enabled`
- `POST /app-api/aigc/billing/recharge/create`
- `POST /app-api/aigc/billing/recharge/create-by-package?packageId=xxx`
- `GET /app-api/aigc/billing/recharge/get`
- `GET /app-api/aigc/billing/recharge/page`
- `POST /app-api/aigc/billing/recharge/sync-pay-status`
- `GET /app-api/pay/channel/get-enable-code-list?appId=xxx`
- `GET /app-api/pay/order/get?id=xxx&sync=true`
- `POST /app-api/pay/order/submit`

注意：`/pay/order/submit` 的 `id` 是 Pay 支付订单 ID，即 `payOrderId`，不是 AIGC 充值订单 ID。

### 4.6 生成页计费提示

展示节点：

- 选择模型后展示模型价格说明
- 调整参数后重新请求价格预估
- 提交生成前展示预计消耗积分
- 余额不足时禁用生成按钮
- 提交成功后展示冻结提示
- 任务成功后展示扣费提示
- 任务失败后展示释放提示

文案规则：

- 生成前：“预计消耗 20 积分”
- 余额不足：“积分不足，请充值后再生成”
- 提交成功：“已冻结 20 积分，任务成功后扣费”
- 任务成功：“生成成功，已扣费 20 积分”
- 任务失败：“任务失败，冻结积分已释放”

## 五、用户端接口封装

### 5.1 类型定义

```ts
export interface AigcWallet {
  id: number
  userId: number
  balance: number
  frozenBalance: number
  totalRecharge: number
  totalGift: number
  totalConsume: number
  totalRefund: number
  status: number
  lastTransTime?: string
}

export interface AigcWalletRecord {
  id: number
  recordNo: string
  recordType: number
  recordTypeName?: string
  amount: number
  balanceAfter: number
  frozenBalanceAfter: number
  bizType: number
  bizId: string
  taskId?: number
  taskNo?: string
  modelId?: number
  remark?: string
  createTime: string
}

export interface AigcWalletFreeze {
  id: number
  freezeNo: string
  amount: number
  confirmedAmount: number
  releasedAmount: number
  status: number
  statusName?: string
  taskId?: number
  taskNo?: string
  expireTime?: string
  createTime: string
}

export interface AigcRechargeOrder {
  id: number
  rechargeNo: string
  payOrderId?: number
  payOrderNo?: string
  payAppId?: number
  payAmount: number
  pointAmount: number
  giftAmount: number
  totalPointAmount: number
  status: number
  statusName?: string
  payTime?: string
  createTime: string
}

export interface AigcRechargeCreateResult {
  rechargeOrderId: number
  rechargeNo: string
  payOrderId: number
  payOrderNo: string
  payAppId: number
  payAmount: number
  pointAmount: number
  giftAmount: number
  totalPointAmount: number
}

export interface PayOrder {
  id: number
  appId: number
  channelCode?: string
  merchantOrderId: string
  subject: string
  body: string
  price: number
  status: number
  expireTime: string
  successTime?: string
  no: string
}

export interface PayOrderSubmitResult {
  status: number
  displayMode: string
  displayContent: string
}

export interface AigcRechargePackage {
  id: number
  name: string
  payAmount: number
  pointAmount: number
  giftAmount: number
  totalPointAmount: number
  description?: string
  features?: string
  recommendStatus?: boolean
  sort?: number
  status?: number
}
```

### 5.2 API 封装

```ts
export function getAigcWallet() {
  return api.get<AigcWallet>('/aigc/billing/wallet/get')
}

export function getAigcWalletRecordPage(params: {
  pageNo: number
  pageSize: number
  recordType?: number
}) {
  return api.get(`/aigc/billing/wallet/record/page${toQuery(params)}`)
}

export function getAigcWalletFreezePage(params: {
  pageNo: number
  pageSize: number
  status?: number
}) {
  return api.get(`/aigc/billing/wallet/freeze/page${toQuery(params)}`)
}

export function getAigcWalletStatistics() {
  return api.get('/aigc/billing/wallet/statistics')
}

export function createAigcRechargeOrder(data: {
  payAmount: number
  rechargeType?: number
  payChannelCode?: string
}) {
  return api.post('/aigc/billing/recharge/create', data)
}

export function getEnabledAigcRechargePackages() {
  return api.get<AigcRechargePackage[]>('/aigc/billing/recharge-package/list-enabled')
}

export function createAigcRechargeOrderByPackage(packageId: number) {
  return api.post<AigcRechargeCreateResult>(`/aigc/billing/recharge/create-by-package${toQuery({ packageId })}`)
}

export function getAigcRechargeOrder(id: number) {
  return api.get(`/aigc/billing/recharge/get${toQuery({ id })}`)
}

export function getAigcRechargeOrderPage(params: {
  pageNo: number
  pageSize: number
  status?: number
}) {
  return api.get(`/aigc/billing/recharge/page${toQuery(params)}`)
}

export function syncRechargePayStatus(id: number) {
  return api.post('/aigc/billing/recharge/sync-pay-status', { id })
}

export function getEnablePayChannelCodeList(appId: number) {
  return api.get<string[]>(`/pay/channel/get-enable-code-list${toQuery({ appId })}`)
}

export function getPayOrder(params: { id?: number; no?: string; sync?: boolean }) {
  return api.get<PayOrder | null>(`/pay/order/get${toQuery(params)}`)
}

export function submitPayOrder(data: {
  id: number
  channelCode: string
  channelExtras?: Record<string, string>
  displayMode?: string
  returnUrl?: string
}) {
  return api.post<PayOrderSubmitResult>('/pay/order/submit', data)
}
```

说明：`draw2video-client/src/lib/api-client.ts` 会自动拼接 `NEXT_PUBLIC_API_BASE_URL + NEXT_PUBLIC_APP_API_PREFIX`，默认前缀为 `/app-api`，因此业务 API 封装中只写 `/aigc/billing/**`，不要重复写 `/app-api`。

## 六、用户端任务拆分

### 6.1 钱包 API 封装

- 新增 `wallet.ts`
- 定义钱包、流水、冻结、充值订单接口
- 定义分页请求和分页响应类型
- 接入统一请求实例
- 处理未登录、登录过期、接口失败等异常

### 6.2 工作台钱包余额展示

- 登录后自动拉取钱包信息
- 展示可用积分和冻结积分
- 点击余额进入钱包页
- 余额不足时展示充值引导
- 生成任务完成后刷新余额

### 6.3 价格预估联动

- 模型或参数变化后请求价格预估
- 展示预计消耗积分
- 前端比较可用余额和预计消耗
- 余额不足时禁用生成按钮
- 提交前二次确认预计消耗

### 6.4 生成计费状态提示

- 提交成功后提示积分冻结
- 任务轮询成功后提示扣费完成
- 任务失败后提示冻结释放
- 任务详情展示计费状态
- 计费状态变更后刷新钱包余额和流水

### 6.5 钱包页面

- 展示钱包概览卡片
- 展示消费统计
- 展示最近流水
- 支持进入完整流水列表
- 支持进入冻结记录列表
- 支持进入充值记录列表

### 6.6 流水列表

- 支持分页加载
- 支持类型筛选
- 支持时间筛选
- 支持任务编号跳转
- 支持空态、加载态、错误态

### 6.7 冻结记录

- 展示冻结中、已扣费、已释放状态
- 支持按状态筛选
- 支持点击任务编号跳转任务详情
- 对冻结中记录展示过期时间

### 6.8 充值入口

- 展示后台启用的充值套餐
- 点击套餐后按 `packageId` 创建充值订单
- 前端不允许自行传入支付金额、充值积分和赠送积分
- 创建订单成功后携带 `rechargeOrderId`、`payOrderId` 和 `payAppId` 跳转 `/checkout/recharge`
- 加载套餐失败时展示错误态
- 展示待支付状态
- 支持同步支付状态
- 支付成功后刷新钱包余额

### 6.8.1 充值收银台

- 路由建议：`draw2video-client/src/app/(app)/checkout/recharge/page.tsx`
- 页面路径：`/checkout/recharge?rechargeOrderId=xxx&payOrderId=xxx`
- 页面初始化时读取 AIGC 充值订单和 Pay 支付订单，校验订单属于当前用户
- 根据 `payAppId` 调用 Pay 渠道接口，展示可用支付方式
- 用户选择渠道后提交 Pay 支付订单
- `displayMode=url` 时打开或跳转支付链接
- `displayMode=qr_code` 时展示二维码内容
- `displayMode=form` 时渲染或提交支付表单内容
- 支付中轮询 `GET /pay/order/get?id=payOrderId&sync=true`
- Pay 支付成功后调用 AIGC 充值订单同步接口或等待回调完成
- AIGC 充值订单确认已支付后跳转 `/wallet?rechargeOrderId=xxx`
- 支付失败、取消、超时时允许重新提交支付或返回 `/pricing`

### 6.9 价格方案页

- `/pricing` 页面不再维护静态套餐常量
- 从 `GET /aigc/billing/recharge-package/list-enabled` 拉取启用套餐
- 推荐套餐使用 `recommendStatus` 高亮展示
- `payAmount` 按分转元展示
- `totalPointAmount` 展示到账积分总数
- `features` 按换行拆分为权益列表
- 点击“立即充值”调用 `create-by-package` 创建订单
- 创建成功后跳转 `/checkout/recharge?rechargeOrderId=xxx&payOrderId=xxx`

## 七、管理端页面规划

### 7.1 钱包管理

页面能力：

- 钱包分页查询
- 钱包详情查看
- 按用户 ID、手机号、邮箱、状态筛选
- 展示可用余额、冻结余额、累计充值、累计赠送、累计消费、累计退款
- 手动调整余额
- 赠送积分

接口：

- `GET /aigc/billing/wallet/get`
- `GET /aigc/billing/wallet/page`
- `PUT /aigc/billing/wallet/adjust`
- `POST /aigc/billing/wallet/gift`

### 7.2 冻结记录管理

页面能力：

- 冻结记录分页查询
- 冻结详情查看
- 按用户、任务、状态、时间筛选
- 人工释放冻结
- 人工确认扣费
- 展示冻结金额、已扣费金额、已释放金额、过期时间

### 7.3 计费流水管理

页面能力：

- 计费流水分页查询
- 流水详情查看
- 按用户、任务、业务类型、流水类型、时间筛选
- 查看价格快照
- 查看扩展信息
- 导出流水

### 7.4 充值订单管理

页面能力：

- 充值订单分页查询
- 充值详情查看
- 按用户、充值状态、支付渠道、时间筛选
- 后台手工充值
- 关闭充值订单
- 导出充值订单

### 7.5 充值套餐管理

页面能力：

- 充值套餐分页查询
- 新增、编辑、删除充值套餐
- 配置支付金额、充值积分、赠送积分、描述、权益说明、推荐状态、排序、启用状态
- 管理端支付金额按“元”输入和回显，提交时转换为后端“分”
- 只允许用户端读取启用套餐

接口：

- `GET /aigc/billing/recharge-package/page`
- `GET /aigc/billing/recharge-package/get`
- `POST /aigc/billing/recharge-package/create`
- `PUT /aigc/billing/recharge-package/update`
- `DELETE /aigc/billing/recharge-package/delete`
- `GET /aigc/billing/recharge-package/list-enabled`

### 7.6 成本记录管理

页面能力：

- 成本记录分页查询
- 成本详情查看
- 按模型、供应商、能力、时间筛选
- 展示销售金额、成本金额、毛利、毛利率
- 导出成本数据

### 7.6 经营统计

页面能力：

- 经营总览
- 总充值
- 总消费
- 总成本
- 总毛利
- 毛利率
- 按日趋势
- 按模型统计
- 按渠道统计
- 用户消费排行

## 八、管理端 API 示例

```ts
export const BillingWalletApi = {
  getWallet: (userId: number) => {
    return request.get({ url: '/aigc/billing/wallet/get', params: { userId } })
  },

  getWalletPage: (params: any) => {
    return request.get({ url: '/aigc/billing/wallet/page', params })
  },

  adjustWallet: (data: any) => {
    return request.put({ url: '/aigc/billing/wallet/adjust', data })
  },

  giftWallet: (data: any) => {
    return request.post({ url: '/aigc/billing/wallet/gift', data })
  }
}
```

### 8.1 管理端接口契约注意事项

前端必须严格对齐 `yudao-module-aigc-billing` 后端 Controller 与 VO，避免接口可见但操作失败。

| 场景 | 前端请求 | 后端契约 | 说明 |
| --- | --- | --- | --- |
| 查询钱包 | `GET /aigc/billing/wallet/get?userId=...` | `@RequestParam("userId")` | 参数名必须是 `userId`，不是钱包 `id` |
| 调整积分 | `PUT /aigc/billing/wallet/adjust` body `{ userId, amount, remark }` | `AigcWalletAmountReqVO` | `remark` 对应备注，不能传 `reason` |
| 赠送积分 | `POST /aigc/billing/wallet/gift` body `{ userId, amount, remark }` | `AigcWalletAmountReqVO` | 赠送也使用同一 VO |
| 人工释放冻结 | `PUT /aigc/billing/freeze/release` body `{ freezeId, taskId?, taskNo?, reason? }` | `AigcBillingReleaseReqDTO` | `freezeId` 必填 |
| 人工确认扣费 | `PUT /aigc/billing/freeze/confirm` body `{ freezeId, actualAmount, taskId?, taskNo? }` | `AigcBillingConfirmReqDTO` | `actualAmount` 必填且大于 0 |
| 关闭充值订单 | `PUT /aigc/billing/recharge/close?id=...` | `@RequestParam("id")` | 参数放 query，不放 body |
| 成本统计 | `GET /aigc/billing/cost/statistics` | `@GetMapping("/statistics")` | 不使用 `/summary` |

## 九、状态映射

### 9.1 流水类型

| 类型 | 用户端文案 | 管理端文案 |
| --- | --- | --- |
| RECHARGE | 充值 | 充值 |
| GIFT | 赠送 | 赠送 |
| FREEZE | 冻结 | 冻结 |
| CONSUME | 消费 | 消费 |
| RELEASE | 释放 | 释放 |
| REFUND | 退款 | 退款 |
| ADJUST_INCREASE | 调增 | 余额调增 |
| ADJUST_DECREASE | 调减 | 余额调减 |
| COMPENSATE | 补偿 | 系统补偿 |

### 9.2 冻结状态

| 类型 | 文案 |
| --- | --- |
| FROZEN | 冻结中 |
| CONFIRMED | 已扣费 |
| RELEASED | 已释放 |
| EXPIRED | 已过期 |
| PART_CONFIRMED | 部分扣费 |
| PART_RELEASED | 部分释放 |

### 9.3 充值状态

| 类型 | 文案 |
| --- | --- |
| WAIT_PAY | 待支付 |
| PAID | 已支付 |
| CLOSED | 已关闭 |
| REFUNDED | 已退款 |
| MANUAL_SUCCESS | 人工成功 |
| FAILED | 失败 |

## 十、交互与展示规则

- 用户端统一展示为“积分”
- 用户端不展示供应商成本、毛利、内部支付单号、管理员备注
- 生成任务提交成功后展示“冻结”，不要展示“已扣费”
- 任务成功后展示“已扣费”
- 任务失败、取消、超时后展示“已释放”
- 冻结余额需要提供解释说明
- 余额不足时生成按钮不可点击，并引导充值
- 流水中的入账类金额展示为正数
- 流水中的消费类金额展示为负数
- 冻结类流水需要通过文案说明是“暂时占用”
- 管理端所有人工操作需要二次确认
- 管理端调整余额、赠送积分需要填写原因
- 管理端导出需要保留当前筛选条件

## 十一、异常处理

### 11.1 用户未登录

- 不请求钱包接口
- 展示登录引导
- 登录成功后重新拉取钱包

### 11.2 钱包不存在

- 后端通常会自动创建钱包
- 前端展示加载态
- 接口失败时允许用户重试

### 11.3 余额不足

- 禁用生成按钮
- 展示预计消耗和当前余额
- 引导用户充值

### 11.4 任务失败

- 展示任务失败原因
- 展示冻结积分释放提示
- 刷新钱包余额
- 刷新流水列表

### 11.5 支付状态未同步

- 充值订单页展示“同步支付状态”按钮
- 同步成功后刷新订单和钱包
- 同步失败时提示稍后重试

### 11.6 接口失败

- 展示错误提示
- 保留页面已有数据
- 提供重试入口

## 十二、联调计划

### 12.1 钱包基础联调

- 登录后调用钱包查询接口
- 工作台展示可用积分和冻结积分
- 钱包页展示钱包概览
- 钱包接口失败时展示重试

### 12.2 价格预估联调

- 选择模型后展示价格说明
- 修改参数后重新预估价格
- 展示预计消耗积分
- 余额不足时阻断提交

### 12.3 生成扣费联调

- 提交生成后验证冻结提示
- 任务成功后验证扣费提示
- 任务失败后验证释放提示
- 钱包余额和流水实时刷新

### 12.4 钱包流水联调

- 钱包页分页加载流水
- 按流水类型筛选
- 按时间筛选
- 点击任务编号跳转任务详情

### 12.5 充值联调

- 创建充值订单
- 展示待支付状态
- 支付成功后同步状态
- 钱包余额刷新
- 充值记录分页展示

### 12.6 管理端联调

- 钱包分页查询
- 钱包详情查看
- 余额调整
- 积分赠送
- 冻结记录查询
- 计费流水查询
- 充值订单查询
- 成本毛利统计

### 12.7 当前验证结果

用户端 `draw2video-client` 已完成工程验证：

- `npm run lint`：通过
- `npm run build`：通过
- 已修复 `react-hooks/set-state-in-effect` 阻塞项
- 已修复 Next.js build 暴露的 TypeScript 类型问题

管理端 `draw2video-admin` 当前验证结果：

- AIGC Billing 新增与调整文件诊断通过
- `pnpm ts:check` 仍被仓库既有 `pay/system/wms` 等模块类型错误阻塞
- 当前阻塞不是本次 AIGC Billing 前端新增文件引入
- 后续如需全仓 `ts:check` 通过，需要单独治理既有模块类型问题

## 十三、优先级

### 13.1 P0

- 用户端钱包查询
- 工作台余额展示
- 价格预估展示
- 余额不足阻断
- 生成后冻结提示
- 任务成功扣费提示
- 任务失败释放提示
- 钱包流水列表

### 13.2 P1

- 用户端充值入口
- 用户端充值记录
- 用户端冻结记录
- 管理端钱包管理
- 管理端流水查询
- 管理端冻结记录

### 13.3 P2

- 管理端充值订单
- 管理端成本记录
- 经营统计看板
- 导出功能
- 更完整的支付闭环

## 十四、验收标准

- 用户登录后可以看到自己的钱包余额
- 用户可以看到可用积分和冻结积分
- 生成前可以看到预计消耗积分
- 余额不足时无法提交生成任务
- 提交生成任务后可以看到积分冻结提示
- 任务成功后可以看到扣费提示
- 任务失败后可以看到冻结释放提示
- 钱包页可以分页查看流水
- 流水金额、类型、时间展示正确
- 管理端可以查询用户钱包
- 管理端可以赠送积分和调整余额
- 管理端可以查询冻结记录和计费流水
- 管理端可以追踪一笔任务对应的冻结、消费或释放流水
- 用户端不展示成本、毛利、供应商结算等后台字段

## 十五、建议实施顺序

1. 完成用户端钱包 API 封装和类型定义
2. 完成工作台余额展示
3. 完成价格预估和余额不足阻断
4. 完成生成任务冻结、扣费、释放提示
5. 完成钱包页和流水列表
6. 完成冻结记录和充值入口
7. 完成管理端钱包管理
8. 完成管理端冻结记录和计费流水
9. 完成充值订单、成本记录和经营统计
10. 完成整体联调与验收

## 十六、当前落地清单

### 16.1 用户端已落地文件

```text
draw2video-client/src/features/wallet/
├── wallet-api.ts
└── wallet-types.ts

draw2video-client/src/app/(app)/wallet/page.tsx
draw2video-client/src/app/(app)/checkout/recharge/page.tsx
draw2video-client/src/app/(app)/layout.tsx
draw2video-client/src/features/auth/auth-store.tsx
draw2video-client/src/components/Header.tsx
draw2video-client/src/components/WorkspaceShell.tsx
```

用户端同时修复了工程质量阻塞文件：

```text
draw2video-client/src/app/(app)/tasks/page.tsx
draw2video-client/src/app/(app)/assets/page.tsx
draw2video-client/src/features/tasks/hooks/use-task-progress.ts
draw2video-client/src/features/generation/use-aigc-models.ts
draw2video-client/src/features/auth/AuthPanel.tsx
draw2video-client/src/features/canvas/ImageNode.tsx
draw2video-client/src/features/tasks/task-api.ts
```

### 16.2 管理端已落地文件

```text
draw2video-admin/src/api/aigc/billing/
├── wallet/index.ts
├── freeze/index.ts
├── record/index.ts
├── recharge/index.ts
├── cost/index.ts
└── statistics/index.ts

draw2video-admin/src/views/aigc/billing/
├── utils.ts
├── wallet/index.vue
├── wallet/WalletAdjustForm.vue
├── wallet/WalletGiftForm.vue
├── freeze/index.vue
├── record/index.vue
├── recharge/index.vue
├── cost/index.vue
└── statistics/index.vue
```

### 16.3 当前完成度

- 用户端：100 / 100，已完成价格页下单、充值收银台、Pay 渠道查询、支付提交、支付状态同步与钱包回跳
- 管理端 AIGC Billing 模块：96 / 100，新增文件诊断通过，等待全仓既有类型问题治理后可完成全量 ts-check
- 整体交付：98 / 100

### 16.4 本次充值收银台落地

- 后端 `create-by-package` 返回 `rechargeOrderId`、`payOrderId`、`payAppId` 等收银台字段
- 后端创建 AIGC 充值订单后同步创建 Pay 支付订单，并以充值订单号作为 `merchantOrderId`
- 前端价格页创建订单成功后跳转 `/checkout/recharge`
- 收银台查询 AIGC 充值订单、Pay 支付订单和 Pay 可用渠道
- 收银台支持提交支付、展示支付返回内容、轮询 Pay 支付状态、同步 AIGC 充值状态
- 支付成功后跳转 `/wallet?rechargeOrderId=xxx` 并刷新钱包余额

## 十七、后续注意事项

- 管理端分页筛选字段需要与后端进一步联调确认，因为部分后端分页接口当前只接 `PageParam`，可能不会消费 `userId/taskNo/status` 等筛选条件。
- 冻结“确认扣费”当前默认以冻结金额作为 `actualAmount`，如后续支持部分扣费，应补充实际扣费金额弹窗。
- 管理端导出接口目前以后端实际实现为准，如后端返回 `PageResult` 而不是二进制文件，前端导出按钮需等待后端导出能力完善后再验收。
- AIGC Billing 的 Pay 应用配置需要在环境中确认 `yudao.aigc.billing.pay.app-id` 和 `app-key` 与 Pay 应用信息一致。
- 支付渠道真实可用性依赖 Pay 模块渠道配置、回调地址和前端域名 returnUrl 联调。
