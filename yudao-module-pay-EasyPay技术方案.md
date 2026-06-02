# yudao-module-pay EasyPay 支付接入技术方案

前提：

1. EasyPay 作为 `yudao-module-pay` 的新增第三方支付渠道接入，不由业务模块直接对接 EasyPay。
2. AIGC 计费充值、会员订单、商城订单等业务模块仍通过 `pay-api` 创建支付单，并通过 Pay 模块的业务通知完成入账或订单状态变更。
3. EasyPay 官方接口参数、签名算法、回调响应格式以实际商户文档为准，本方案定义项目内的接入边界、代码改造点、数据流和验收标准。
4. 不在代码仓库、Nacos 示例或技术文档中写入真实商户号、密钥、证书、回调密钥等敏感信息。

## 1. 目标

在现有统一支付模块 `yudao-module-pay` 中新增 EasyPay 支付方案，使平台可以通过统一支付接口支持 EasyPay 收银台、扫码支付或跳转支付能力，并复用现有支付订单、支付尝试单、渠道回调、主动同步、业务通知和补偿任务。

本方案目标不是单独为某个业务模块硬编码 EasyPay，而是把 EasyPay 沉淀为 Pay 模块的标准渠道能力。业务侧只感知渠道编码和支付结果，不感知 EasyPay 的签名、下单、查单、回调验签等细节。

## 2. 现状分析

当前 Pay 模块已经具备完整的统一支付抽象：

| 能力 | 现有实现 |
| ---- | -------- |
| 支付应用 | `pay_app`，按业务应用维护 `appKey`、订单通知地址、退款通知地址、转账通知地址 |
| 支付渠道 | `pay_channel`，按应用维护渠道编码、费率、状态和渠道配置 JSON |
| 支付主单 | `pay_order`，保存业务支付诉求和最终支付结果 |
| 支付拓展单 | `pay_order_extension`，保存每次提交渠道支付的实际三方单号和状态 |
| 渠道客户端 | `PayClient`，统一封装下单、查单、退款、转账和回调解析 |
| 渠道工厂 | `PayClientFactoryImpl`，按渠道枚举注册具体客户端实现 |
| 三方回调 | `PayNotifyController`，统一接收渠道支付、退款、转账回调 |
| 业务通知 | `PayNotifyService`，支付成功后异步通知业务模块并支持重试 |
| 状态补偿 | `PayOrderSyncJob`、`PayNotifyJob`，用于支付状态和业务通知补偿 |

当前支付渠道主要包括微信、支付宝、钱包支付和 Mock 支付。EasyPay 需要作为新的渠道编码接入上述统一链路。

## 3. 总体设计

### 3.1 架构原则

- 统一入口：业务模块继续调用 `PayOrderApi.createOrder` 创建支付单，不直接调用 EasyPay。
- 统一渠道：EasyPay 作为 `pay_channel.code` 的新增枚举值，由管理端配置到某个 `pay_app` 下。
- 统一回调：EasyPay 的异步通知回调到 Pay 模块统一地址，再由 Pay 模块解析、验签、落库和通知业务系统。
- 统一状态：支付成功、支付关闭、支付失败、退款成功等状态映射为项目内部 `PayOrderStatusEnum`、`PayRefundStatusEnum`。
- 统一展示：EasyPay 下单响应转换成 `displayMode` 和 `displayContent`，前端收银台无需理解 EasyPay 原始响应结构。

### 3.2 支付链路

```text
业务模块
  -> PayOrderApi.createOrder(appKey, merchantOrderId, price, notifyUrl...)
  -> pay_order 创建待支付主单
用户选择 EasyPay 渠道
  -> /pay/order/submit(id, channelCode=easypay_*)
  -> PayOrderService 创建 pay_order_extension
  -> EasyPayClient.unifiedOrder 调用 EasyPay 下单
  -> 返回 url / qr_code_url / form 等展示内容
用户完成支付
  -> EasyPay 回调 /pay/notify/order/{channelId}
  -> EasyPayClient.parseOrderNotify 验签并解析支付结果
  -> PayOrderService.notifyOrder 更新 pay_order_extension 和 pay_order
  -> PayNotifyService 创建业务通知任务
  -> 业务模块接收 PayOrderNotifyReqDTO 并完成订单成功、积分入账或权益发放
```

### 3.3 主动同步链路

```text
前端轮询 /pay/order/get?id={payOrderId}&sync=true
  -> PayOrderService 同步渠道订单
  -> EasyPayClient.getOrder 查询 EasyPay 订单状态
  -> 如果已支付，复用 notifyOrder 完成内部状态更新
```

主动同步用于提升前端支付完成后的状态刷新体验，不能替代 EasyPay 异步回调。最终一致性由异步回调、前端主动同步和定时补偿共同保障。

## 4. 渠道定义

### 4.1 渠道编码

建议在 `PayChannelEnum` 中新增 EasyPay 渠道：

| 渠道编码 | 名称 | 展示模式 | 使用场景 |
| -------- | ---- | -------- | -------- |
| `easypay_pc` | EasyPay 电脑网站支付 | `url` 或 `form` | PC 收银台跳转支付 |
| `easypay_wap` | EasyPay 手机网站支付 | `url` | H5 支付 |
| `easypay_qr` | EasyPay 扫码支付 | `qr_code_url` 或 `qr_code` | PC 二维码收银台 |

如果 EasyPay 官方只提供统一收银台接口，也可以先只落地一个渠道：

| 渠道编码 | 名称 | 展示模式 | 使用场景 |
| -------- | ---- | -------- | -------- |
| `easypay_cashier` | EasyPay 收银台支付 | `url` | PC/H5 统一跳转收银台 |

推荐第一阶段优先采用 `easypay_cashier`，降低渠道数量和配置复杂度；在确认 EasyPay 官方区分 PC、H5、扫码产品后，再拆分为 `easypay_pc`、`easypay_wap`、`easypay_qr`。

### 4.2 渠道能力范围

| 能力 | 第一阶段 | 说明 |
| ---- | -------- | ---- |
| 支付下单 | 支持 | 必须实现 |
| 支付回调 | 支持 | 必须实现验签、幂等和状态映射 |
| 支付查单 | 支持 | 用于前端 `sync=true` 和定时补偿 |
| 退款申请 | 可选 | 如果 EasyPay 支持退款接口则接入 |
| 退款回调 | 可选 | 如果 EasyPay 支持退款异步通知则接入 |
| 退款查询 | 可选 | 用于退款补偿 |
| 转账 | 暂不支持 | 除非 EasyPay 明确提供转账能力 |

不支持的能力需要在 `EasyPayClient` 中明确返回不支持或抛出业务异常，避免调用方误认为已接入完整能力。

## 5. 配置设计

### 5.1 EasyPay 配置对象

新增 `EasyPayClientConfig`，实现现有 `PayClientConfig` 配置接口，建议字段如下：

| 字段 | 必填 | 说明 |
| ---- | ---- | ---- |
| `serverUrl` | 是 | EasyPay 网关地址，区分正式和沙箱环境 |
| `merchantNo` | 是 | EasyPay 商户号 |
| `appId` | 否 | EasyPay 应用 ID，如官方要求则必填 |
| `signType` | 是 | 签名类型，如 `MD5`、`RSA2`、`HMAC_SHA256` |
| `privateKey` | 条件必填 | 商户私钥，RSA 类签名时使用 |
| `publicKey` | 条件必填 | EasyPay 平台公钥，RSA 类验签时使用 |
| `secretKey` | 条件必填 | 对称签名密钥，MD5/HMAC 类签名时使用 |
| `returnUrl` | 否 | 支付完成后的前端跳转地址，可由提交支付时覆盖 |
| `notifyContentType` | 否 | 回调内容类型，如 `FORM`、`JSON` |
| `sandbox` | 是 | 是否沙箱环境 |
| `timeoutSeconds` | 否 | 调用 EasyPay 接口超时时间 |
| `unifiedOrderPath` | 否 | EasyPay 下单接口路径，默认 `/pay/unified-order`，以官方文档为准 |
| `queryOrderPath` | 否 | EasyPay 查单接口路径，默认 `/pay/query-order`，以官方文档为准 |
| `successResponse` | 否 | EasyPay 回调成功响应文本，默认 `success`，需按官方要求配置 |

### 5.2 配置存储

EasyPay 渠道配置存储在 `pay_channel.config` 字段中，与支付宝、微信渠道保持一致。示例结构如下，示例中的密钥必须替换为环境变量、密钥管理系统或 Nacos 加密配置中的值：

```json
{
  "serverUrl": "https://gateway.easypay.example.com",
  "merchantNo": "${EASYPAY_MERCHANT_NO}",
  "appId": "${EASYPAY_APP_ID}",
  "signType": "RSA2",
  "privateKey": "${EASYPAY_PRIVATE_KEY}",
  "publicKey": "${EASYPAY_PUBLIC_KEY}",
  "returnUrl": "https://www.example.com/pay/result",
  "notifyContentType": "JSON",
  "sandbox": false,
  "timeoutSeconds": 10,
  "unifiedOrderPath": "/pay/unified-order",
  "queryOrderPath": "/pay/query-order",
  "successResponse": "success"
}
```

### 5.3 配置解析

需要在渠道配置解析逻辑中增加 EasyPay 分支：

```text
PayChannelServiceImpl.parseConfig
  -> code 属于 EasyPay 渠道
  -> JsonUtils.parseObject(config, EasyPayClientConfig.class)
```

如果项目现有 `PayChannelDO.PayClientConfigTypeHandler` 对配置类型有白名单反序列化逻辑，也需要同步加入 `EasyPayClientConfig`。

## 6. 代码改造点

### 6.1 pay-api

| 文件 | 改造内容 |
| ---- | -------- |
| `PayChannelEnum` | 新增 `EASYPAY_CASHIER`，或按 EasyPay 产品新增 `EASYPAY_PC`、`EASYPAY_WAP`、`EASYPAY_QR` |

渠道枚举属于跨模块 API，前后端和业务模块都会引用，新增编码后需要保持稳定，避免后续随意改名。

### 6.2 pay-server

| 文件或目录 | 改造内容 |
| ---------- | -------- |
| `framework/pay/core/client/impl/easypay` | 新增 EasyPay 客户端实现目录 |
| `EasyPayClientConfig` | 定义 EasyPay 渠道配置字段、校验规则和敏感字段处理 |
| `EasyPayClient` | 实现 `PayClient` 统一接口 |
| `PayOrderRespDTO` | 增加渠道支付金额字段 `channelPrice`，用于回调与查单金额校验 |
| `PayClientFactoryImpl` | 注册 EasyPay 渠道编码和客户端实现类映射 |
| `PayChannelServiceImpl` | 支持 EasyPay 配置 JSON 反序列化 |
| `PayNotifyController` | 复用 `/pay/notify/order/{channelId}`，并按 EasyPay 配置返回回调成功响应文本 |
| `PayOrderServiceImpl` | 复用现有提交支付、回调更新和业务通知逻辑，并在订单置为成功前校验渠道支付金额 |

### 6.3 管理端前端

| 页面 | 改造内容 |
| ---- | -------- |
| 支付渠道配置表单 | 新增 EasyPay 配置表单，支持商户号、网关地址、签名类型、密钥、沙箱开关等字段 |
| 支付应用渠道列表 | 支持展示 EasyPay 渠道编码和名称 |
| 收银台渠道列表 | 根据 `/pay/channel/get-enable-code-list` 返回值展示 EasyPay 支付入口 |

### 6.4 用户端前端

用户端原则上复用现有支付接口：

| 接口 | 用途 |
| ---- | ---- |
| `GET /pay/channel/get-enable-code-list?appId={appId}` | 查询当前应用启用的支付渠道 |
| `POST /pay/order/submit` | 提交 EasyPay 支付 |
| `GET /pay/order/get?id={payOrderId}&sync=true` | 查询并同步支付状态 |

前端需要根据 `PayOrderSubmitRespVO.displayMode` 处理展示：

| displayMode | 前端处理 |
| ----------- | -------- |
| `url` | 跳转 EasyPay 收银台 |
| `form` | 渲染并自动提交 HTML 表单 |
| `qr_code_url` | 使用返回 URL 生成二维码 |
| `qr_code` | 直接展示返回的二维码内容 |

## 7. EasyPayClient 设计

### 7.1 类结构

建议类结构如下：

```text
yudao-module-pay-server
  └── src/main/java/cn/iocoder/yudao/module/pay/framework/pay/core/client/impl/easypay
      ├── EasyPayClient.java
      ├── EasyPayClientConfig.java
      ├── EasyPaySigner.java
      ├── EasyPayRequestUtils.java
      ├── EasyPayOrderStatusMapping.java
      └── dto
          ├── EasyPayUnifiedOrderRequest.java
          ├── EasyPayUnifiedOrderResponse.java
          ├── EasyPayQueryOrderRequest.java
          ├── EasyPayQueryOrderResponse.java
          └── EasyPayNotifyRequest.java
```

### 7.2 下单实现

`EasyPayClient.unifiedOrder` 负责把项目内部订单转换为 EasyPay 下单请求：

| 项目字段 | EasyPay 字段 | 说明 |
| -------- | ------------ | ---- |
| `PayOrderUnifiedReqDTO.outTradeNo` | 商户订单号 | 使用 `pay_order_extension.no`，不能使用业务订单号 |
| `PayOrderUnifiedReqDTO.price` | 金额 | 项目内部金额通常为分，按 EasyPay 要求转换为元或分 |
| `PayOrderUnifiedReqDTO.subject` | 商品标题 | 控制长度，避免超过 EasyPay 限制 |
| `PayOrderUnifiedReqDTO.body` | 商品描述 | 可为空 |
| `PayOrderUnifiedReqDTO.notifyUrl` | 异步通知地址 | Pay 模块生成的 `/pay/notify/order/{channelId}` |
| `PayOrderUnifiedReqDTO.returnUrl` | 同步跳转地址 | 前端支付完成跳转地址 |
| `PayOrderUnifiedReqDTO.expireTime` | 过期时间 | 按 EasyPay 格式转换 |

下单返回需要转换为 `PayOrderRespDTO`：

| EasyPay 返回 | Pay 返回 |
| ------------ | ------- |
| 收银台跳转链接 | `displayMode=url`，`displayContent=payUrl` |
| HTML 表单 | `displayMode=form`，`displayContent=formHtml` |
| 二维码链接 | `displayMode=qr_code_url`，`displayContent=qrCodeUrl` |
| 已支付状态 | `status=SUCCESS`，携带 EasyPay 订单号、支付用户标识、支付金额 `channelPrice` |
| 等待支付状态 | `status=WAITING`，并返回展示内容 |

下单响应如果直接返回成功状态，也必须携带 EasyPay 实际支付金额字段。Pay 模块会将该金额标准化为分并写入 `PayOrderRespDTO.channelPrice`，用于后续统一订单成功校验。

### 7.3 查单实现

`EasyPayClient.getOrder` 负责根据 `pay_order_extension.no` 查询 EasyPay 订单状态，并转换为 `PayOrderRespDTO`。

查单响应如果返回支付成功，必须解析 EasyPay 返回的交易金额字段，并转换为内部分单位写入 `PayOrderRespDTO.channelPrice`。主动同步、定时补偿和异步回调走同一套 `notifyOrder` 成功更新逻辑，因此查单金额也必须与本地 `pay_order.price` 完全一致，否则拒绝把订单更新为成功。

状态映射建议如下：

| EasyPay 状态 | 内部状态 | 处理方式 |
| ------------ | -------- | -------- |
| `SUCCESS`、`PAID`、`TRADE_SUCCESS` | 支付成功 | 更新支付拓展单和支付主单 |
| `WAITING`、`UNPAID`、`PROCESSING` | 支付中 | 保持待支付 |
| `CLOSED`、`CANCELLED`、`EXPIRED` | 已关闭 | 关闭支付拓展单 |
| `FAILED` | 支付失败 | 关闭支付拓展单或按失败状态处理 |

实际状态值以 EasyPay 官方文档为准，不能在实现中只依赖本方案示例值。

### 7.4 回调解析

`EasyPayClient.parseOrderNotify` 必须完成以下动作：

1. 读取 EasyPay 回调原始参数，兼容 `application/x-www-form-urlencoded` 和 `application/json`。
2. 校验签名，签名失败直接拒绝，不更新订单状态。
3. 校验商户号、应用 ID、订单号、金额、币种和支付状态。
4. 使用 EasyPay 回调中的商户订单号匹配 `pay_order_extension.no`。
5. 将 EasyPay 交易号、支付用户标识、支付成功时间、实际支付金额、手续费等字段写入 `PayOrderRespDTO`。
6. 返回 EasyPay 要求的成功响应文本，避免 EasyPay 重复通知。

回调验签必须使用原始请求参数构造待签名字符串，不能使用反序列化后无序 Map 直接拼接，避免参数顺序、空值处理、字符集差异导致验签失败。当前实现兼容 `body` 与 `params` 两类来源，但同名字段值不一致时必须直接拒绝回调，避免查询参数覆盖请求体参数造成验签对象和业务对象不一致。

EasyPay 回调金额必须转换为内部分单位写入 `PayOrderRespDTO.channelPrice`。`PayOrderServiceImpl.notifyOrder` 在更新 `pay_order` 为成功前，必须校验 `channelPrice` 与 `pay_order.price` 完全一致；缺少金额或金额不一致均不得更新订单状态，也不得创建业务通知任务。

### 7.5 退款实现

如果 EasyPay 官方支持退款，第二阶段实现：

| 方法 | 说明 |
| ---- | ---- |
| `unifiedRefund` | 发起退款，使用项目退款单号作为商户退款单号 |
| `getRefund` | 查询退款状态，用于补偿 |
| `parseRefundNotify` | 解析退款异步通知 |

如果 EasyPay 不支持退款或第一阶段不接入退款，需要在管理端和业务侧明确该渠道不支持自动退款，避免支付成功后产生无法自动退款的资金风险。

## 8. 数据库设计

### 8.1 表结构

第一阶段不需要新增表，复用现有 Pay 表：

| 表名 | 用途 |
| ---- | ---- |
| `pay_app` | 配置业务应用和业务回调地址 |
| `pay_channel` | 新增 EasyPay 渠道记录，`config` 保存 EasyPay 配置 |
| `pay_order` | 保存支付主单最终状态 |
| `pay_order_extension` | 保存 EasyPay 下单尝试、商户订单号和 EasyPay 交易号 |
| `pay_notify_task` | 保存 Pay 到业务模块的通知任务 |
| `pay_notify_log` | 保存业务通知日志 |
| `pay_refund` | 如果接入退款则复用该表 |

### 8.2 渠道初始化示例

EasyPay 渠道建议通过管理端创建，不建议直接在 SQL 中写入真实配置。测试环境可以插入脱敏示例：

```sql
INSERT INTO pay_channel (
  code, status, fee_rate, remark, app_id, config,
  creator, create_time, updater, update_time, deleted
) VALUES (
  'easypay_cashier', 0, 0, 'EasyPay 收银台支付', 1,
  '{"serverUrl":"https://gateway.easypay.example.com","merchantNo":"${EASYPAY_MERCHANT_NO}","appId":"${EASYPAY_APP_ID}","signType":"RSA2","privateKey":"${EASYPAY_PRIVATE_KEY}","publicKey":"${EASYPAY_PUBLIC_KEY}","sandbox":true}',
  'system', NOW(), 'system', NOW(), b'0'
);
```

如果当前表结构启用了租户字段，需要同步补充 `tenant_id`，以实际 `pay_channel` 表定义为准。

## 9. 安全设计

### 9.1 密钥安全

- EasyPay 商户私钥、平台公钥、对称密钥不得提交到 Git 仓库。
- Nacos 配置中的敏感字段优先使用加密配置或环境变量占位符。
- 日志中不得输出完整请求参数、签名、私钥、密钥、证书内容。
- 管理端返回渠道配置时需要对密钥字段脱敏。
- 修改 EasyPay 配置需要管理端权限控制和操作日志审计。

### 9.2 回调安全

- 必须验签后再更新订单状态。
- 必须校验回调金额与 `pay_order_extension` 或 `pay_order` 金额一致。
- 必须校验回调商户号与当前 `pay_channel.config` 一致。
- 必须通过 `channelId` 定位渠道配置，不能只相信回调参数中的渠道信息。
- 必须保证重复回调幂等，已成功订单不能被关闭或改为失败。
- 回调响应成功文本必须符合 EasyPay 要求，否则会导致重复通知。

### 9.3 金额安全

- 内部金额和 EasyPay 金额单位必须明确转换，避免分和元混用。
- 金额转换建议使用 `BigDecimal`，禁止使用浮点类型。
- 回调金额必须和下单金额完全一致，除非 EasyPay 官方明确存在手续费扣减字段且交易金额字段不变。

## 10. 异常与补偿

| 场景 | 处理策略 |
| ---- | -------- |
| EasyPay 下单超时 | 返回支付提交失败，允许用户重新提交并生成新的 `pay_order_extension` |
| EasyPay 下单成功但响应丢失 | 通过前端同步或定时任务按拓展单号查单补偿 |
| EasyPay 回调验签失败 | 记录安全日志，不更新订单，不通知业务 |
| EasyPay 回调参数冲突 | 同名字段值不一致时拒绝回调，不更新订单，不通知业务 |
| EasyPay 回调金额缺失或不一致 | 拒绝更新订单成功，不创建业务通知任务 |
| EasyPay 重复回调 | 依赖 `notifyOrder` 幂等处理，订单已成功则直接返回成功响应 |
| Pay 通知业务失败 | 写入 `pay_notify_task` 和 `pay_notify_log`，由 `PayNotifyJob` 重试 |
| 前端支付后未跳转 | 用户端继续轮询 `/pay/order/get?id={id}&sync=true` |
| EasyPay 查单返回支付中 | 保持待支付，等待后续回调或下一次补偿 |
| EasyPay 查单返回成功但本地未成功 | 校验查单金额与本地订单金额一致后，复用 `notifyOrder` 更新本地状态并触发业务通知 |

## 11. AIGC 充值接入方式

AIGC 计费模块不直接集成 EasyPay。推荐链路如下：

```text
用户选择充值套餐
  -> aigc-billing 创建 aigc_recharge_order
  -> aigc-billing 调用 PayOrderApi.createOrder
  -> 前端进入 Pay 收银台并选择 easypay_cashier
  -> EasyPay 支付成功回调 Pay 模块
  -> Pay 模块通知 aigc-billing 充值订单支付成功
  -> aigc-billing 幂等入账积分并写入计费流水
```

AIGC 充值订单需要保存 `payOrderId`、`payAppKey`、`merchantOrderId`、充值金额、赠送积分、支付状态和入账状态。支付成功后以 Pay 模块通知的 `merchantOrderId` 和 `payOrderId` 做幂等入账，禁止仅依赖前端跳转结果入账。

## 12. 实施步骤

### 12.1 后端实施

1. 在 `PayChannelEnum` 新增 EasyPay 渠道编码和名称。
2. 新增 `EasyPayClientConfig`，定义 EasyPay 商户号、网关、签名、密钥、沙箱等配置字段。
3. 新增 `EasyPayClient`，实现支付下单、查单、回调解析和验签。
4. 新增 EasyPay 签名工具，封装参数排序、空值过滤、字符集、摘要或 RSA 签名逻辑。
5. 修改 `PayClientFactoryImpl`，注册 EasyPay 渠道编码到 `EasyPayClient`。
6. 修改 `PayChannelServiceImpl`，支持 EasyPay 渠道配置反序列化。
7. 如果支持退款，补充退款申请、退款查询和退款回调解析。
8. 增加 EasyPay 单元测试，覆盖签名、验签、下单参数构造、状态映射和回调解析。

### 12.2 管理端实施

1. 支付渠道表单新增 EasyPay 类型。
2. EasyPay 配置字段按签名类型动态展示。
3. 密钥字段保存后展示脱敏值。
4. 渠道列表和详情页显示 EasyPay 渠道名称、费率和状态。

### 12.3 用户端实施

1. 收银台识别 `easypay_cashier` 渠道并展示 EasyPay 支付按钮。
2. 根据 `displayMode` 处理跳转、表单或二维码展示。
3. 支付后轮询订单状态，使用 `sync=true` 触发 Pay 模块查单。
4. 支付成功后跳回业务结果页，不在前端直接做入账。

### 12.4 运维配置

1. 在 EasyPay 商户后台配置 Pay 模块公网回调地址：`https://{pay-domain}/admin-api/pay/notify/order/{channelId}`。
2. 在 Nacos 或管理端配置 EasyPay 商户号、网关地址、签名类型和密钥。
3. 确认网关、Pay 服务、业务服务之间的公网和内网访问链路。
4. 为 EasyPay 回调接口配置访问日志、错误告警和通知失败告警。

## 13. 测试方案

### 13.1 单元测试

| 测试项 | 验证内容 |
| ------ | -------- |
| 签名生成 | 参数排序、空值过滤、字符集、签名类型正确 |
| 回调验签 | 正确签名通过，篡改金额或订单号失败 |
| 回调参数冲突 | `body` 与 `params` 存在同名不同值字段时拒绝回调 |
| 下单参数 | 金额单位、订单号、回调地址、过期时间转换正确 |
| 金额转换 | EasyPay 金额字段正确转换为内部分单位 |
| 状态映射 | EasyPay 成功、支付中、关闭、失败状态映射正确 |
| 重复回调 | 已成功订单重复通知不重复入账 |

### 13.2 集成测试

| 测试项 | 验证内容 |
| ------ | -------- |
| 创建支付单 | 业务模块通过 `pay-api` 成功创建支付单 |
| EasyPay 提交支付 | `/pay/order/submit` 返回正确展示模式和内容 |
| EasyPay 支付成功回调 | Pay 订单成功，业务模块收到通知 |
| 前端主动同步 | `sync=true` 可以把 EasyPay 已支付订单同步为成功 |
| 通知失败重试 | 业务回调临时失败后可由通知任务重试成功 |
| 订单过期关闭 | 未支付订单过期后不会误关闭已支付订单 |

### 13.3 安全测试

| 测试项 | 验证内容 |
| ------ | -------- |
| 签名错误回调 | 不更新订单状态 |
| 金额不一致回调 | 不更新订单状态 |
| 金额缺失回调 | 不更新订单状态 |
| 商户号不一致回调 | 不更新订单状态 |
| 回调参数冲突 | 不更新订单状态 |
| 重放回调 | 幂等处理，不重复通知业务入账 |
| 日志脱敏 | 日志不出现私钥、密钥、完整签名 |

## 14. 验收标准

- 管理端可以为指定 `pay_app` 创建并启用 EasyPay 渠道。
- 用户端可以查询到启用的 EasyPay 渠道并提交支付。
- EasyPay 下单成功后，前端可以按 `displayMode` 正确跳转或展示二维码。
- EasyPay 支付成功回调后，`pay_order_extension` 和 `pay_order` 状态正确更新为成功。
- EasyPay 支付成功回调或主动查单成功时，渠道支付金额必须与 `pay_order.price` 完全一致后才允许更新成功。
- Pay 模块可以向业务模块发送支付成功通知，并在失败时自动重试。
- 重复回调、主动同步、定时同步不会造成重复入账。
- 签名错误、金额错误、金额缺失、商户号错误、回调参数冲突的回调不会更新订单。
- Pay 模块返回给 EasyPay 的回调成功响应文本可通过 `successResponse` 配置，并符合 EasyPay 官方要求。
- 日志、配置、文档中不包含真实密钥和证书。

## 15. 支付链路增强落地记录

### 15.1 最终一致性链路

本轮开发不再把 EasyPay 回调作为唯一成功来源，而是形成完整支付最终一致性链路：

```text
EasyPay 异步回调
  -> Pay 模块验签、验金额、更新 pay_order
  -> PayNotifyService 创建业务通知任务
  -> AIGC 接收 Pay 通知并幂等入账

前端 sync=true 主动同步
  -> Pay 模块主动查询 EasyPay 订单
  -> 成功后复用 Pay 内部订单成功逻辑
  -> AIGC sync-pay-status 只触发后端反查，不相信前端支付结果

PayOrderSyncJob 定时查单补偿
  -> 扫描待支付订单
  -> 主动查询渠道状态
  -> 支付成功后触发业务通知

PayNotifyJob 业务通知重试
  -> 扫描 pay_notify_task
  -> 重试通知业务模块
  -> 直到成功或达到最大通知次数
```

### 15.2 Pay 侧已完成增强

| 能力 | 落地内容 | 涉及文件 |
| ---- | -------- | -------- |
| 查单补偿可观测 | `syncOrder` 输出总数、成功、等待、关闭、客户端缺失、异常等分类统计 | `PayOrderServiceImpl.java` |
| 业务通知日志 | 创建通知任务、通知成功、待重试、最终失败均输出结构化日志 | `PayNotifyServiceImpl.java` |
| 通知任务诊断 RPC | 新增 `PayNotifyApi.getNotifyDiagnostic(type, dataId)`，返回通知任务和通知日志 | `PayNotifyApi.java`、`PayNotifyApiImpl.java` |
| 通知任务查询 | 支持按 `type + dataId` 查询最新通知任务 | `PayNotifyTaskMapper.java` |
| 通知诊断 DTO | 暴露 `PayNotifyDiagnosticRespDTO`、`PayNotifyTaskRespDTO`、`PayNotifyLogRespDTO` | `pay-api notify dto` |

### 15.3 前端收银台已完成增强

| 能力 | 落地内容 | 涉及文件 |
| ---- | -------- | -------- |
| 二维码展示 | 支持 `qr_code_url` 直接展示二维码 URL，支持 `qr_code` 文本生成二维码 | `page.tsx` |
| 跳转/表单展示 | 继续支持 `url`、`form` 等 Pay 统一展示模式 | `page.tsx` |
| 支付轮询 | 每 3 秒轮询 Pay 订单，使用 `sync=true` 触发 Pay 后端查单 | `page.tsx` |
| 轮询保护 | 页面隐藏时暂停轮询，最大轮询 5 分钟，网络失败连续提示 | `page.tsx` |
| 入账同步 | Pay 成功后调用 AIGC `sync-pay-status`，由后端反查 Pay 状态后入账 | `page.tsx` |

### 15.4 排障与安全边界

- Pay 成功但业务未入账时，可以通过 PayNotify 诊断查看通知任务、通知状态、下次通知时间、通知次数和通知日志。
- 前端 `returnUrl` 只负责用户支付完成后的页面跳转，不替代 EasyPay 到 Pay 的异步回调地址，也不和后端回调地址冲突。
- 前端 `sync=true` 只触发 Pay 后端查单，不允许前端直接声明支付成功。
- AIGC 入账入口会反查 Pay 订单和 PayNotifyTask，防止外部伪造 `PayOrderNotifyReqDTO` 直接触发入账。

## 16. 风险与待确认事项

| 风险或问题 | 影响 | 处理建议 |
| ---------- | ---- | -------- |
| EasyPay 官方状态码未知 | 影响状态映射准确性 | 开发前确认官方支付、退款、关闭状态枚举 |
| EasyPay 金额单位未知 | 可能导致金额错误 | 开发前确认接口使用分、元或字符串金额 |
| EasyPay 回调格式未知 | 影响解析和验签 | 开发前确认 JSON、表单、XML 以及成功响应文本 |
| EasyPay 签名算法未知 | 影响安全接入 | 开发前确认 MD5、RSA、HMAC 及参数排序规则 |
| EasyPay 是否支持退款未知 | 影响售后能力 | 第一阶段明确是否禁用自动退款或接入退款 API |
| EasyPay 是否有沙箱环境未知 | 影响联调效率 | 优先申请沙箱商户和测试密钥 |
| EasyPay 回调 IP 白名单未知 | 影响生产稳定性 | 确认是否需要配置服务器出口 IP 或回调白名单 |

## 17. 推荐落地顺序

第一阶段只接入 `easypay_cashier` 统一收银台支付，支持下单、回调、查单和主动同步，暂不接入退款和转账。该阶段可以满足 AIGC 充值、会员订单等核心收款场景。

第二阶段根据业务需要补充 EasyPay 退款能力，接入退款申请、退款查询和退款回调，并补充管理端退款操作验证。

第三阶段再根据 EasyPay 官方产品形态拆分 PC、H5、扫码等多个渠道编码，优化前端收银台展示和支付转化率。
