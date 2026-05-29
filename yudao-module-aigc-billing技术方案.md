# yudao-module-aigc-billing 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置

## 1. 模块定位

`yudao-module-aigc-billing` 是 AIGC 平台的计费钱包服务，负责用户积分钱包、余额查询、充值入账、运营赠送、积分冻结、成功扣费、失败退款、计费流水、成本记录、毛利统计和账务对账。

该模块是 AIGC MVP 赚钱版的商业化闭环核心，不负责模型价格配置，不负责模型生成调用，不负责任务状态机，不保存生成结果，不直接管理第三方模型渠道密钥。

第一阶段建议以“积分钱包”为主，使用 `balance` 表示可用积分，使用 `frozenBalance` 表示已冻结积分。后续接入真实支付时，通过 `pay-api` 创建支付订单和接收支付结果，再由 `aigc-billing` 负责把支付成功结果转换为 AIGC 积分入账。

## 2. 核心职责

### 2.1 负责内容

- 用户钱包初始化
- 用户钱包余额查询
- 管理端钱包分页和详情查询
- 管理端手动调整积分
- 运营赠送积分
- 充值订单创建和支付结果入账
- 任务提交前积分冻结
- 任务成功后确认扣费
- 任务失败后释放冻结
- 任务取消后释放冻结
- 计费流水记录
- 成本记录
- 毛利计算
- 账务幂等控制
- 账务异常补偿
- 用户端钱包流水查询
- 管理端计费流水、冻结记录、成本记录和充值订单查询

### 2.2 不负责内容

- 不维护模型供应商和模型配置
- 不维护模型参数模板
- 不计算模型基础价格规则
- 不直接提交第三方生成任务
- 不处理第三方模型回调
- 不管理任务状态机
- 不保存图片、视频、音频、文档等资产文件
- 不做内容安全审核
- 不保存支付渠道密钥
- 不直接实现微信、支付宝等支付渠道适配

对应职责归属：

| 能力 | 归属模块 |
| ---- | -------- |
| 模型、渠道、参数、价格 | `yudao-module-aigc-model` |
| 统一任务状态机 | `yudao-module-aigc-task` |
| 第三方模型调用适配 | `yudao-module-aigc-gen` |
| 图片、视频、音频、文档等文件资产 | `yudao-module-aigc-asset` |
| 敏感词、审核记录 | `yudao-module-aigc-safety` |
| 微信、支付宝等真实支付能力 | `yudao-module-pay` |

计费服务的核心抽象是“积分账务”，不是“支付渠道”。真实支付、退款通道、支付回调等能力优先复用 `yudao-module-pay`，AIGC 侧只沉淀积分充值订单、钱包余额、冻结记录、消费流水和成本毛利。

## 3. 模块结构

### 3.1 Maven 结构

```text
yudao-module-aigc-billing
  ├── yudao-module-aigc-billing-api
  └── yudao-module-aigc-billing-server
```

命名规则遵循当前项目规范：

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-billing` |
| 聚合 artifactId | `yudao-module-aigc-billing` |
| API 子模块 artifactId | `yudao-module-aigc-billing-api` |
| Server 子模块 artifactId | `yudao-module-aigc-billing-server` |
| Spring 应用名 | `aigc-billing-server` |

### 3.2 根包名

```text
cn.iocoder.yudao.module.aigc.billing
```

### 3.3 API 模块结构

```text
yudao-module-aigc-billing-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/billing
      ├── api
      │   └── AigcBillingApi.java
      ├── dto
      │   ├── AigcWalletRespDTO.java
      │   ├── AigcBillingFreezeReqDTO.java
      │   ├── AigcBillingFreezeRespDTO.java
      │   ├── AigcBillingConfirmReqDTO.java
      │   ├── AigcBillingReleaseReqDTO.java
      │   ├── AigcBillingRecordCreateReqDTO.java
      │   ├── AigcCostRecordCreateReqDTO.java
      │   ├── AigcGrossProfitRespDTO.java
      │   └── AigcRechargeNotifyReqDTO.java
      └── enums
          ├── AigcBillingBizTypeEnum.java
          ├── AigcBillingRecordTypeEnum.java
          ├── AigcBillingFreezeStatusEnum.java
          ├── AigcBillingRechargeStatusEnum.java
          ├── AigcBillingCurrencyTypeEnum.java
          └── ErrorCodeConstants.java
```

### 3.4 Server 模块结构

```text
yudao-module-aigc-billing-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/billing
      ├── AigcBillingServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── wallet
      │   │   ├── freeze
      │   │   ├── record
      │   │   ├── cost
      │   │   ├── recharge
      │   │   └── statistics
      │   └── app
      │       ├── wallet
      │       └── recharge
      ├── framework
      │   └── web
      │       └── config
      │           └── AigcBillingWebConfiguration.java
      ├── service
      │   ├── wallet
      │   ├── freeze
      │   ├── record
      │   ├── cost
      │   ├── recharge
      │   ├── statistics
      │   └── compensate
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcWalletDO.java
      │   │   ├── AigcQuotaFreezeDO.java
      │   │   ├── AigcBillingRecordDO.java
      │   │   ├── AigcCostRecordDO.java
      │   │   └── AigcRechargeOrderDO.java
      │   └── mysql
      │       ├── AigcWalletMapper.java
      │       ├── AigcQuotaFreezeMapper.java
      │       ├── AigcBillingRecordMapper.java
      │       ├── AigcCostRecordMapper.java
      │       └── AigcRechargeOrderMapper.java
      ├── job
      │   ├── AigcBillingFreezeTimeoutJob.java
      │   └── AigcBillingReconcileJob.java
      └── convert
```

## 4. 依赖设计

### 4.1 API 模块依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 4.2 Server 模块依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-env</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-system-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-billing-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-model-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-task-api</artifactId>
        <version>${revision}</version>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-pay-api</artifactId>
        <version>${revision}</version>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-biz-tenant</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-mybatis</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-rpc</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-job</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-excel</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-monitor</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-spring-boot-starter-test</artifactId>
    </dependency>
</dependencies>
```

说明：

- `aigc-billing` 必须依赖 `system-api`，用于用户基础信息、管理员信息和运营操作审计。
- `aigc-billing` 可依赖 `aigc-model-api`，用于成本和销售价核对、模型信息展示、毛利报表维度补充。
- `aigc-billing` 可选依赖 `aigc-task-api`，用于异常冻结、退款补偿时核对任务状态。
- `aigc-billing` 后续接真实支付时依赖 `pay-api`，第一阶段可先支持后台赠送和手工充值入账。
- `aigc-billing` 不依赖 `aigc-gen-api`，生成服务主动调用计费服务完成冻结、扣费和退款。

## 5. 数据库设计

### 5.1 表清单

| 表名 | 说明 | 第一阶段 |
| ---- | ---- | -------- |
| `aigc_wallet` | 用户积分钱包 | 是 |
| `aigc_quota_freeze` | 积分冻结记录 | 是 |
| `aigc_billing_record` | 计费流水 | 是 |
| `aigc_cost_record` | 成本记录 | 是 |
| `aigc_recharge_order` | AIGC 充值订单 | 是 |

### 5.2 aigc_wallet

用户积分钱包表，每个租户下每个用户一条钱包记录。第一阶段积分可使用 `decimal(18,6)`，兼容按 token、秒、张、任务等细粒度计费。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| balance | decimal(18,6) | 可用积分余额 |
| frozen_balance | decimal(18,6) | 冻结积分余额 |
| total_recharge | decimal(18,6) | 累计充值积分 |
| total_gift | decimal(18,6) | 累计赠送积分 |
| total_consume | decimal(18,6) | 累计消费积分 |
| total_refund | decimal(18,6) | 累计退款积分 |
| status | int | 状态 |
| last_trans_time | datetime | 最近交易时间 |
| remark | varchar(512) | 备注 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_user_tenant = user_id + tenant_id
idx_status = status
idx_last_trans_time = last_trans_time
```

### 5.3 aigc_quota_freeze

积分冻结记录表，用于记录某个业务请求对用户积分的冻结、确认扣费或释放。冻结记录必须绑定 `bizType + bizId`，用于防止同一个任务重复冻结。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| freeze_no | varchar(64) | 冻结编号，全局唯一 |
| wallet_id | bigint | 钱包 ID |
| user_id | bigint | 用户 ID |
| biz_type | varchar(64) | 业务类型 |
| biz_id | varchar(128) | 业务 ID，如任务编号或请求编号 |
| task_id | bigint | 任务 ID，可为空 |
| task_no | varchar(64) | 任务编号，可为空 |
| amount | decimal(18,6) | 冻结积分 |
| confirmed_amount | decimal(18,6) | 已确认扣费积分 |
| released_amount | decimal(18,6) | 已释放积分 |
| status | varchar(32) | 冻结状态 |
| expire_time | datetime | 冻结过期时间 |
| confirm_time | datetime | 确认扣费时间 |
| release_time | datetime | 释放时间 |
| reason | varchar(512) | 冻结、扣费或释放原因 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_freeze_no = freeze_no
uk_biz = biz_type + biz_id + tenant_id
idx_user_status = user_id + status
idx_task_id = task_id
idx_expire_status = expire_time + status
```

### 5.4 aigc_billing_record

计费流水表，记录所有积分变动，包括充值、赠送、冻结、扣费、释放、退款、手动调整等。该表是用户账单、管理端对账和运营分析的基础。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| record_no | varchar(64) | 流水编号，全局唯一 |
| wallet_id | bigint | 钱包 ID |
| user_id | bigint | 用户 ID |
| biz_type | varchar(64) | 业务类型 |
| biz_id | varchar(128) | 业务 ID |
| record_type | varchar(64) | 流水类型 |
| title | varchar(128) | 流水标题 |
| amount | decimal(18,6) | 变动积分，收入为正、支出为负 |
| balance_after | decimal(18,6) | 变动后可用余额 |
| frozen_balance_after | decimal(18,6) | 变动后冻结余额 |
| freeze_id | bigint | 冻结记录 ID，可为空 |
| task_id | bigint | 任务 ID，可为空 |
| model_id | bigint | 模型 ID，可为空 |
| provider_id | bigint | 渠道商 ID，可为空 |
| currency_type | varchar(32) | 货币类型 |
| price_snapshot | json | 价格快照 |
| extra_info | json | 扩展信息 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_record_no = record_no
idx_wallet_time = wallet_id + create_time
idx_user_time = user_id + create_time
idx_biz = biz_type + biz_id
idx_task_id = task_id
idx_record_type = record_type
```

### 5.5 aigc_cost_record

成本记录表，用于记录平台调用第三方模型产生的成本、对用户收取的销售价和毛利。成本记录通常在模型调用完成后由 `aigc-gen` 或补偿任务创建。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| cost_no | varchar(64) | 成本记录编号 |
| task_id | bigint | 任务 ID |
| task_no | varchar(64) | 任务编号 |
| user_id | bigint | 用户 ID |
| model_id | bigint | 模型 ID |
| provider_id | bigint | 渠道商 ID |
| capability | varchar(64) | 模型能力 |
| billing_unit | varchar(32) | 计费单位 |
| usage_amount | decimal(18,6) | 实际用量 |
| cost_amount | decimal(18,6) | 平台成本 |
| sale_amount | decimal(18,6) | 用户销售价 |
| gross_profit | decimal(18,6) | 毛利 |
| gross_profit_rate | decimal(18,6) | 毛利率 |
| currency_type | varchar(32) | 货币类型 |
| usage_snapshot | json | 用量快照 |
| price_snapshot | json | 价格快照 |
| status | varchar(32) | 状态 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_cost_no = cost_no
uk_task_id = task_id
idx_model_time = model_id + create_time
idx_provider_time = provider_id + create_time
idx_user_time = user_id + create_time
idx_status = status
```

### 5.6 aigc_recharge_order

AIGC 充值订单表，用于记录用户购买积分包、后台手工充值或运营活动赠送的入账过程。真实支付订单号通过 `pay_order_id`、`pay_order_no` 关联 `yudao-module-pay`。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| recharge_no | varchar(64) | AIGC 充值订单号 |
| wallet_id | bigint | 钱包 ID |
| user_id | bigint | 用户 ID |
| recharge_type | varchar(32) | 充值类型 |
| pay_amount | int | 支付金额，单位分 |
| point_amount | decimal(18,6) | 充值获得积分 |
| gift_amount | decimal(18,6) | 赠送积分 |
| total_point_amount | decimal(18,6) | 总入账积分 |
| pay_order_id | bigint | 支付订单 ID |
| pay_order_no | varchar(64) | 支付订单号 |
| pay_channel_code | varchar(32) | 支付渠道编码 |
| status | varchar(32) | 充值状态 |
| pay_time | datetime | 支付成功时间 |
| close_time | datetime | 关闭时间 |
| refund_time | datetime | 退款时间 |
| operator_id | bigint | 后台操作人 ID |
| remark | varchar(512) | 备注 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_recharge_no = recharge_no
idx_wallet_time = wallet_id + create_time
idx_user_status = user_id + status
idx_pay_order_id = pay_order_id
idx_status_create_time = status + create_time
```

### 5.7 aigc_recharge_package

AIGC 充值套餐表，用于后台配置用户端可购买的积分包。用户端只读取启用套餐，创建订单时只传 `packageId`，支付金额和到账积分以后端套餐配置为准。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| name | varchar(64) | 套餐名称 |
| pay_amount | int | 支付金额，单位分 |
| point_amount | decimal(18,6) | 充值积分数量 |
| gift_amount | decimal(18,6) | 赠送积分数量 |
| total_point_amount | decimal(18,6) | 到账积分总数 |
| description | varchar(255) | 描述 |
| features | varchar(1000) | 权益说明，每行一条 |
| recommend_status | bit | 是否推荐 |
| sort | int | 排序 |
| status | tinyint | 启用状态 |
| remark | varchar(512) | 备注 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
idx_status_sort = status + sort
```

金额规则：

- 后端和数据库统一以“分”存储 `pay_amount`
- 管理端表单以“元”输入和回显，提交时转换成“分”
- 用户端展示时将“分”转换成“元”
- `total_point_amount = point_amount + gift_amount`，由后端保存时计算

## 6. 枚举设计

### 6.1 AigcBillingBizTypeEnum

```text
TASK_GENERATE
TASK_REFUND
WALLET_RECHARGE
WALLET_GIFT
MANUAL_ADJUST
ACTIVITY_REWARD
SYSTEM_COMPENSATE
```

### 6.2 AigcBillingRecordTypeEnum

```text
RECHARGE
GIFT
FREEZE
CONSUME
RELEASE
REFUND
ADJUST_INCREASE
ADJUST_DECREASE
COMPENSATE
```

流水金额规则：

| 类型 | amount 规则 | 说明 |
| ---- | ----------- | ---- |
| RECHARGE | 正数 | 充值积分入账 |
| GIFT | 正数 | 运营赠送积分入账 |
| FREEZE | 0 或负数 | 可用余额转冻结余额，推荐记录 0 并依赖余额快照展示 |
| CONSUME | 负数 | 确认扣费 |
| RELEASE | 0 或正数 | 冻结释放回可用余额，推荐记录 0 并依赖余额快照展示 |
| REFUND | 正数 | 已扣费后的退款 |
| ADJUST_INCREASE | 正数 | 后台手动增加 |
| ADJUST_DECREASE | 负数 | 后台手动减少 |
| COMPENSATE | 正数 | 系统补偿 |

### 6.3 AigcBillingFreezeStatusEnum

```text
FROZEN
CONFIRMED
RELEASED
EXPIRED
PART_CONFIRMED
PART_RELEASED
```

第一阶段建议只启用：

```text
FROZEN
CONFIRMED
RELEASED
EXPIRED
```

### 6.4 AigcBillingRechargeStatusEnum

```text
WAIT_PAY
PAID
CLOSED
REFUNDED
MANUAL_SUCCESS
FAILED
```

### 6.5 AigcBillingCurrencyTypeEnum

```text
POINT
CNY
USD
```

第一阶段用户侧统一使用 `POINT`。`CNY`、`USD` 主要用于支付订单、成本统计或第三方渠道账单折算。

## 7. RPC API 设计

### 7.1 AigcBillingApi

`AigcBillingApi` 放在 `yudao-module-aigc-billing-api`，供 `aigc-gen`、`aigc-task`、`aigc-model` 和后续运营统计服务调用。

核心接口：

```java
@FeignClient(name = ApiConstants.NAME)
public interface AigcBillingApi {

    CommonResult<AigcWalletRespDTO> getOrCreateWallet(Long userId);

    CommonResult<AigcWalletRespDTO> getWallet(Long userId);

    CommonResult<AigcBillingFreezeRespDTO> freeze(AigcBillingFreezeReqDTO reqDTO);

    CommonResult<Boolean> confirmFreeze(AigcBillingConfirmReqDTO reqDTO);

    CommonResult<Boolean> releaseFreeze(AigcBillingReleaseReqDTO reqDTO);

    CommonResult<Long> createBillingRecord(AigcBillingRecordCreateReqDTO reqDTO);

    CommonResult<Long> createCostRecord(AigcCostRecordCreateReqDTO reqDTO);

    CommonResult<AigcGrossProfitRespDTO> calculateGrossProfit(Long taskId);

    CommonResult<Boolean> notifyRechargePaid(AigcRechargeNotifyReqDTO reqDTO);

}
```

RPC 路径建议基于 `ApiConstants.PREFIX` 拼接：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get-or-create-wallet` | 获取或初始化钱包 |
| GET | `/get-wallet` | 获取钱包 |
| POST | `/freeze` | 冻结积分 |
| POST | `/confirm-freeze` | 确认冻结扣费 |
| POST | `/release-freeze` | 释放冻结积分 |
| POST | `/create-billing-record` | 创建计费流水 |
| POST | `/create-cost-record` | 创建成本记录 |
| GET | `/calculate-gross-profit` | 计算任务毛利 |
| POST | `/notify-recharge-paid` | 通知充值支付成功 |

### 7.2 getOrCreateWallet

输入：

| 字段 | 说明 |
| ---- | ---- |
| userId | 用户 ID |

处理要求：

- 当前租户下用户钱包不存在时自动创建。
- 钱包初始 `balance`、`frozenBalance`、`totalRecharge`、`totalConsume`、`totalRefund` 均为 0。
- 并发创建时依赖 `user_id + tenant_id` 唯一索引兜底。
- 返回钱包 ID、用户 ID、可用余额、冻结余额、累计充值、累计消费和状态。

### 7.3 freeze

输入：

| 字段 | 说明 |
| ---- | ---- |
| userId | 用户 ID |
| bizType | 业务类型 |
| bizId | 业务 ID，建议使用请求编号或任务编号 |
| taskId | 任务 ID，可为空 |
| taskNo | 任务编号，可为空 |
| amount | 冻结积分 |
| expireTime | 冻结过期时间 |
| title | 流水标题 |
| priceSnapshot | 价格快照 |

处理要求：

- `amount` 必须大于 0。
- 使用 `bizType + bizId + tenantId` 做幂等键。
- 如果已有冻结记录且状态为 `FROZEN`，直接返回已有冻结记录。
- 如果已有冻结记录且状态为 `CONFIRMED` 或 `RELEASED`，不允许重复冻结同一业务。
- 冻结钱包余额必须使用条件更新，禁止先查余额再无条件更新。

并发更新 SQL：

```sql
UPDATE aigc_wallet
SET balance = balance - #{amount},
    frozen_balance = frozen_balance + #{amount},
    last_trans_time = NOW()
WHERE user_id = #{userId}
  AND tenant_id = #{tenantId}
  AND balance >= #{amount}
  AND deleted = 0
```

冻结成功后：

- 创建 `aigc_quota_freeze`。
- 创建一条 `FREEZE` 类型 `aigc_billing_record`。
- 返回冻结记录 ID、冻结编号、冻结金额和状态。

### 7.4 confirmFreeze

用于任务成功后确认扣费。

输入：

| 字段 | 说明 |
| ---- | ---- |
| freezeId | 冻结记录 ID |
| taskId | 任务 ID |
| taskNo | 任务编号 |
| actualAmount | 实际扣费金额，第一阶段通常等于冻结金额 |
| modelId | 模型 ID |
| providerId | 渠道商 ID |
| priceSnapshot | 价格快照 |

处理要求：

- 只允许 `FROZEN` 状态确认扣费。
- 第一阶段 `actualAmount` 必须等于冻结金额，后续可支持部分扣费。
- 钱包 `frozen_balance` 减少，`total_consume` 增加。
- 冻结记录状态改为 `CONFIRMED`。
- 创建 `CONSUME` 类型计费流水。
- 如果重复调用且冻结记录已是 `CONFIRMED`，直接返回成功，保证幂等。

确认扣费 SQL：

```sql
UPDATE aigc_wallet
SET frozen_balance = frozen_balance - #{amount},
    total_consume = total_consume + #{amount},
    last_trans_time = NOW()
WHERE id = #{walletId}
  AND frozen_balance >= #{amount}
  AND deleted = 0
```

### 7.5 releaseFreeze

用于任务失败、取消、超时或安全审核拒绝后释放冻结积分。

输入：

| 字段 | 说明 |
| ---- | ---- |
| freezeId | 冻结记录 ID |
| taskId | 任务 ID |
| taskNo | 任务编号 |
| reason | 释放原因 |

处理要求：

- 只允许 `FROZEN` 状态释放。
- 钱包 `frozen_balance` 减少，`balance` 增加。
- 冻结记录状态改为 `RELEASED`。
- 创建 `RELEASE` 类型计费流水。
- 如果重复调用且冻结记录已是 `RELEASED`，直接返回成功，保证幂等。

释放冻结 SQL：

```sql
UPDATE aigc_wallet
SET balance = balance + #{amount},
    frozen_balance = frozen_balance - #{amount},
    last_trans_time = NOW()
WHERE id = #{walletId}
  AND frozen_balance >= #{amount}
  AND deleted = 0
```

### 7.6 createCostRecord

用于记录任务成本和毛利，建议由 `aigc-gen` 在模型调用完成后创建，也可以由补偿任务根据 `aigc_model_usage_log` 回补。

输入字段包括：

| 字段 | 说明 |
| ---- | ---- |
| taskId/taskNo | 任务标识 |
| userId | 用户 ID |
| modelId/providerId | 模型和渠道商 |
| capability | 模型能力 |
| billingUnit | 计费单位 |
| usageAmount | 实际用量 |
| costAmount | 平台成本 |
| saleAmount | 用户销售价 |
| currencyType | 货币类型 |
| usageSnapshot | 用量快照 |
| priceSnapshot | 价格快照 |

处理要求：

- `taskId` 唯一，避免重复创建成本记录。
- `grossProfit = saleAmount - costAmount`。
- `grossProfitRate = grossProfit / saleAmount`，销售价为 0 时毛利率记为 0。
- 成本记录不直接改变钱包余额。

## 8. 管理端接口设计

### 8.1 钱包管理

Controller：`AigcWalletController`

路径：`/aigc/billing/wallet`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 钱包详情 |
| GET | `/page` | 钱包分页 |
| PUT | `/adjust` | 手动调整积分 |
| POST | `/gift` | 运营赠送积分 |
| GET | `/statistics` | 钱包统计 |

权限建议：

```text
aigc:billing:wallet:query
aigc:billing:wallet:update
aigc:billing:wallet:gift
```

### 8.2 冻结记录

Controller：`AigcQuotaFreezeController`

路径：`/aigc/billing/freeze`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 冻结记录详情 |
| GET | `/page` | 冻结记录分页 |
| PUT | `/release` | 人工释放冻结 |
| PUT | `/confirm` | 人工确认扣费，默认仅超级管理员可用 |

### 8.3 计费流水

Controller：`AigcBillingRecordController`

路径：`/aigc/billing/record`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 流水详情 |
| GET | `/page` | 流水分页 |
| GET | `/export-excel` | 导出流水 |

### 8.4 成本记录

Controller：`AigcCostRecordController`

路径：`/aigc/billing/cost`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 成本详情 |
| GET | `/page` | 成本分页 |
| GET | `/statistics` | 成本和毛利统计 |
| GET | `/export-excel` | 导出成本记录 |

### 8.5 充值订单

Controller：`AigcRechargeOrderController`

路径：`/aigc/billing/recharge`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 充值订单详情 |
| GET | `/page` | 充值订单分页 |
| POST | `/manual-create` | 后台手工充值 |
| PUT | `/close` | 关闭充值订单 |
| GET | `/export-excel` | 导出充值订单 |

### 8.6 经营统计

Controller：`AigcBillingStatisticsController`

路径：`/aigc/billing/statistics`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/overview` | 经营总览 |
| GET | `/daily` | 按日统计 |
| GET | `/model` | 按模型统计 |
| GET | `/provider` | 按渠道商统计 |
| GET | `/user-rank` | 用户消费排行 |

## 9. 用户端接口设计

### 9.1 用户钱包

Controller：`AigcWalletAppController`

代码路径：`/aigc/billing/wallet`

网关对用户端接口可统一增加 `/app-api` 前缀，最终外部路径以网关配置为准。

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/get` | 获取当前用户钱包 |
| GET | `/record/page` | 获取当前用户积分流水 |
| GET | `/freeze/page` | 获取当前用户冻结记录 |
| GET | `/statistics` | 获取当前用户消费统计 |

用户端钱包返回字段只允许包含：

- 钱包 ID
- 可用积分
- 冻结积分
- 累计充值积分
- 累计赠送积分
- 累计消费积分
- 累计退款积分
- 最近交易时间

不返回：

- 其他用户钱包
- 成本金额
- 毛利
- 管理员备注
- 支付渠道内部字段
- 异常补偿内部信息

### 9.2 用户充值

Controller：`AigcRechargeAppController`

代码路径：`/aigc/billing/recharge`

接口：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/create` | 创建充值订单 |
| POST | `/create-by-package` | 按套餐创建充值订单 |
| GET | `/get` | 获取充值订单详情 |
| GET | `/page` | 获取当前用户充值订单分页 |
| POST | `/sync-pay-status` | 主动同步支付状态 |

用户端充值必须走“业务充值订单 + Pay 支付订单 + 收银台”链路。价格页点击套餐后先创建 AIGC 充值订单和 Pay 支付订单，再跳转用户端充值收银台；支付成功后再跳转钱包页并刷新钱包余额、充值订单和计费流水。

按套餐创建充值订单建议返回：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| rechargeOrderId | Long | AIGC 充值订单 ID |
| rechargeNo | String | AIGC 充值订单号 |
| payOrderId | Long | Pay 模块支付订单 ID，用于 `/pay/order/submit` |
| payOrderNo | String | Pay 模块支付订单号 |
| payAppId | Long | Pay 应用 ID，用于查询可用支付渠道 |
| payAmount | Integer | 支付金额，单位：分 |
| pointAmount | BigDecimal | 充值积分 |
| giftAmount | BigDecimal | 赠送积分 |
| totalPointAmount | BigDecimal | 到账积分总数 |

如果后端暂时无法创建 Pay 支付订单，前端不能进入正式收银台，只能展示“待支付订单已创建”的占位状态；正式方案必须补齐 `payOrderId` 和 `payAppId`。

## 10. 核心流程

### 10.1 用户提交生成任务冻结流程

```text
aigc-gen 接收用户生成请求
  ↓
调用 aigc-model.validateModel(modelId, capability)
  ↓
调用 aigc-model.validateParams(reqDTO)
  ↓
调用 aigc-model.calculatePrice(reqDTO)
  ↓
调用 aigc-billing.freeze(reqDTO)
  ↓
调用 aigc-task.createTask(reqDTO)，写入 freezeId 和价格快照
  ↓
aigc-gen 提交模型调用或进入队列
```

关键要求：

- 价格以 `aigc-model.calculatePrice` 返回结果为准。
- 冻结金额以销售价 `salePrice` 为准，不使用成本价。
- `freeze` 必须传入 `bizType + bizId`，避免用户重复点击导致重复冻结。
- 创建任务失败时，调用 `releaseFreeze` 释放冻结。

### 10.2 任务成功扣费流程

```text
aigc-gen 收到第三方成功结果
  ↓
调用 aigc-task.markSuccess(reqDTO)
  ↓
调用 aigc-billing.confirmFreeze(reqDTO)
  ↓
调用 aigc-billing.createCostRecord(reqDTO)
  ↓
调用 aigc-model.recordUsage(reqDTO)
```

关键要求：

- 确认扣费必须幂等。
- 成本记录必须按 `taskId` 唯一。
- `confirmFreeze` 成功后才算用户实际消费。
- 如果扣费成功但成本记录失败，由补偿任务基于任务和模型计量日志回补。

### 10.3 任务失败释放冻结流程

```text
aigc-gen 或 aigc-task 判断任务失败
  ↓
调用 aigc-task.markFailed(reqDTO)
  ↓
调用 aigc-billing.releaseFreeze(reqDTO)
  ↓
调用 aigc-task.markRefunded(taskId)
```

关键要求：

- 未确认扣费前失败，执行释放冻结，不叫退款。
- 已确认扣费后发生售后补偿，执行退款或补偿流水。
- 释放失败需要进入补偿任务，不能静默丢失。

### 10.4 充值入账流程

```text
用户在价格页选择充值套餐
  ↓
调用 aigc-billing create-by-package 创建 aigc_recharge_order
  ↓
aigc-billing 调用 pay-api 创建 Pay 支付订单，绑定 payOrderId/payOrderNo
  ↓
前端跳转 /checkout/recharge?rechargeOrderId=xxx&payOrderId=xxx
  ↓
收银台根据 payAppId 查询可用支付渠道
  ↓
用户选择支付渠道，调用 /app-api/pay/order/submit
  ↓
根据 displayMode 展示二维码、跳转链接或其它支付内容
  ↓
pay 模块完成支付回调
  ↓
aigc-billing 接收或主动同步支付成功结果
  ↓
更新 aigc_recharge_order 为 PAID
  ↓
增加 aigc_wallet.balance 和 total_recharge
  ↓
创建 RECHARGE 类型 aigc_billing_record
```

收银台支付状态同步：

```text
收银台轮询 /app-api/pay/order/get?id=payOrderId&sync=true
  ↓
Pay 支付订单状态变为支付成功
  ↓
调用 /app-api/aigc/billing/recharge/sync-pay-status 或等待支付回调入账
  ↓
查询 /app-api/aigc/billing/recharge/get?id=rechargeOrderId
  ↓
确认 AIGC 充值订单已支付并跳转 /wallet?rechargeOrderId=xxx
```

Pay 模块用户端接口复用：

| 方法 | 路径 | 用途 |
| ---- | ---- | ---- |
| GET | `/app-api/pay/channel/get-enable-code-list?appId=xxx` | 查询当前 Pay 应用可用支付渠道 |
| GET | `/app-api/pay/order/get?id=xxx&sync=true` | 查询支付订单并可同步渠道状态 |
| POST | `/app-api/pay/order/submit` | 提交支付订单，返回支付展示内容 |

`/pay/order/submit` 的 `id` 必须是 Pay 支付订单 ID，不是 AIGC 充值订单 ID。

第一阶段可先实现后台手工充值：

```text
管理员提交手工充值
  ↓
创建 aigc_recharge_order，状态为 MANUAL_SUCCESS
  ↓
增加 aigc_wallet.balance 和 total_recharge 或 total_gift
  ↓
创建 RECHARGE 或 GIFT 类型计费流水
```

## 11. 幂等与并发设计

### 11.1 幂等键

| 场景 | 幂等键 | 处理方式 |
| ---- | ------ | -------- |
| 钱包创建 | `user_id + tenant_id` | 唯一索引兜底 |
| 任务冻结 | `biz_type + biz_id + tenant_id` | 已冻结直接返回，已完成禁止重复处理 |
| 确认扣费 | `freeze_id` | 已确认直接返回成功 |
| 释放冻结 | `freeze_id` | 已释放直接返回成功 |
| 成本记录 | `task_id` | 已存在直接返回记录 ID |
| 充值入账 | `recharge_no` 或 `pay_order_id` | 已入账直接返回成功 |
| 计费流水 | `record_no` | 全局唯一 |

### 11.2 钱包余额更新规则

钱包余额所有变更必须遵循：

- 禁止应用层先查余额后无条件更新。
- 冻结必须校验 `balance >= amount`。
- 确认扣费和释放冻结必须校验 `frozen_balance >= amount`。
- 手动扣减必须校验 `balance >= amount`，除非产品明确允许负余额。
- 同一个事务内先更新钱包，再写冻结记录或流水；如果使用唯一键幂等，要处理唯一键冲突后的查询返回。

### 11.3 事务边界

| 操作 | 事务内动作 |
| ---- | ---------- |
| 冻结 | 条件更新钱包、创建冻结记录、创建冻结流水 |
| 确认扣费 | 条件更新钱包、更新冻结记录、创建消费流水 |
| 释放冻结 | 条件更新钱包、更新冻结记录、创建释放流水 |
| 充值入账 | 更新充值订单、增加钱包余额、创建充值流水 |
| 手动调整 | 条件更新钱包、创建调整流水、记录操作人 |

不建议把跨服务 RPC 放进本地数据库事务。调用方需要根据返回结果做补偿或重试。

## 12. 多租户与权限规则

### 12.1 多租户规则

- 钱包、冻结记录、流水、成本记录、充值订单都必须带 `tenant_id`。
- 用户端只能查询当前登录用户在当前租户下的钱包和流水。
- 管理端只能查询当前租户下的数据，平台超管场景按项目现有租户插件规则处理。
- RPC 调用必须透传租户上下文，保证冻结、扣费、退款不会跨租户。
- `user_id + tenant_id` 是钱包唯一身份，不允许只按 `user_id` 查询和更新钱包。

### 12.2 权限规则

管理端权限建议按资源拆分：

```text
aigc:billing:wallet:query
aigc:billing:wallet:update
aigc:billing:wallet:gift
aigc:billing:freeze:query
aigc:billing:freeze:update
aigc:billing:record:query
aigc:billing:record:export
aigc:billing:cost:query
aigc:billing:cost:export
aigc:billing:recharge:query
aigc:billing:recharge:create
aigc:billing:statistics:query
```

敏感操作要求：

- 手动调整积分必须记录操作人、调整原因和调整前后余额。
- 人工确认扣费默认只允许超级管理员或财务角色使用。
- 人工释放冻结需要记录原因。
- 导出流水和成本记录需要独立权限。

## 13. 异常补偿设计

### 13.1 冻结超时补偿

Job：`AigcBillingFreezeTimeoutJob`

扫描条件：

```text
status = FROZEN
expire_time < now()
```

处理逻辑：

- 查询任务状态。
- 如果任务不存在、已失败、已取消或已退款，释放冻结。
- 如果任务仍在运行，延长冻结过期时间或跳过。
- 如果任务已成功但未扣费，触发确认扣费或进入人工对账。

### 13.2 对账补偿

Job：`AigcBillingReconcileJob`

对账内容：

- `aigc_task` 成功但冻结记录仍为 `FROZEN`。
- `aigc_task` 失败但冻结记录仍为 `FROZEN`。
- 已确认扣费但缺少 `CONSUME` 流水。
- 已成功任务缺少 `aigc_cost_record`。
- 支付订单已支付但 `aigc_recharge_order` 未入账。

补偿原则：

- 先基于幂等键查询现有结果。
- 能自动修复的自动修复。
- 涉及金额不一致的写入异常日志并进入人工处理。

### 13.3 生产级补充要求

为达到生产级计费钱包标准，第一阶段在核心冻结、扣费、释放链路之外，还需要补齐以下增强项：

- 统计接口必须返回真实聚合数据，不能只保留占位返回。统计数据来源包括 `aigc_wallet`、`aigc_billing_record`、`aigc_cost_record`、`aigc_recharge_order`。
- 导出接口必须基于分页查询同源数据返回可导出的明细数据，后续再接 Excel 文件流；禁止只返回固定成功标记。
- 对账 Job 必须具备可执行逻辑，至少能扫描冻结超时、重复通知、缺流水、缺成本等账务异常，并返回处理数量。
- 充值入账必须具备订单创建、订单查询、支付状态同步和重复通知幂等能力；真实接入 `pay-api` 前，至少保留明确的模拟支付/手工充值路径。
- 测试必须覆盖冻结、确认扣费、释放冻结、充值入账、成本毛利、统计聚合、重复调用幂等和补偿任务。

### 13.4 生产级验收标准

生产级验收以“账务事实源可靠”为核心，至少满足：

| 类别 | 验收标准 |
| ---- | -------- |
| 核心账务 | 冻结、确认扣费、释放冻结均幂等，余额和冻结余额不重复变更 |
| 充值 | 手工充值、支付通知、重复通知不会重复入账 |
| 流水 | 所有余额变化都有流水，流水包含变动后余额快照 |
| 成本 | 同一任务成本记录唯一，毛利和毛利率可重复计算 |
| 统计 | 管理端总览、按日、按模型、按渠道商、用户排行返回真实数据 |
| 导出 | 流水和成本导出返回真实明细数据，权限独立控制 |
| 补偿 | 冻结超时 Job 和对账 Job 可执行、可重入、返回处理数量 |
| 测试 | 核心 Service 测试通过，覆盖正常、重复、异常和边界场景 |

## 14. 安全与审计要求

- 不记录支付渠道密钥、模型渠道密钥和用户敏感身份信息。
- 计费流水、冻结记录、充值订单不允许物理删除。
- 用户端错误信息不能暴露内部账务异常堆栈。
- 管理端手动调整、人工释放、人工确认扣费必须写操作日志。
- 充值入账必须校验支付订单所属用户、金额、租户和状态。
- 成本价、毛利、渠道成本只允许管理端有权限用户查看。
- 账务金额统一使用 `BigDecimal`，禁止使用浮点数。
- 积分展示可按产品需要做小数位格式化，但数据库保留 6 位小数。

## 15. OpenAPI 与 Gateway 接入

### 15.1 服务注册

`application.yaml`：

```yaml
spring:
  application:
    name: aigc-billing-server
```

### 15.2 OpenAPI 分组

Server 模块需要注册独立文档分组：

```text
aigc-billing-server
```

建议分组标题：

```text
AIGC 计费钱包服务
```

### 15.3 Gateway 路由

Gateway 路由建议：

```yaml
- id: aigc-billing-server-admin-api
  uri: lb://aigc-billing-server
  predicates:
    - Path=/admin-api/aigc/billing/**
  filters:
    - RewritePath=/admin-api/(?<segment>.*), /${segment}

- id: aigc-billing-server-app-api
  uri: lb://aigc-billing-server
  predicates:
    - Path=/app-api/aigc/billing/**
  filters:
    - RewritePath=/app-api/(?<segment>.*), /${segment}
```

收银台还依赖 Pay 模块用户端路由：

```yaml
- id: pay-app-api
  uri: lb://pay-server
  predicates:
    - Path=/app-api/pay/**
  filters:
    - RewritePath=/app-api/(?<segment>.*), /${segment}
```


### 15.5 用户端充值套餐接口

用户端充值套餐接口用于营销价格页和钱包充值入口，不开放创建、修改、删除能力。

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/aigc/billing/recharge-package/list-enabled` | 查询启用充值套餐列表 |
| POST | `/aigc/billing/recharge/create-by-package?packageId=xxx` | 按套餐创建充值订单 |

按套餐创建订单要求：

- 前端只传 `packageId`
- 后端校验套餐存在且状态启用
- 订单的支付金额、充值积分、赠送积分、到账总积分全部来自套餐配置
- 订单 `recharge_type` 使用 `PACKAGE`
- 正式支付链路中，接口需要创建或返回 Pay 支付订单信息，供前端跳转收银台
- 前端创建订单成功后跳转 `/checkout/recharge?rechargeOrderId=xxx&payOrderId=xxx`
- 支付成功后跳转 `/wallet?rechargeOrderId=xxx`
## 16. 实现更新记录


### 16.1 已完成的核心改进

#### 16.1.1 冻结幂等语义修复
- **问题**：重复冻结不区分状态，会把已确认/已释放的业务伪装成新冻结成功
- **修复**：区分 `FROZEN/CONFIRMED/RELEASED` 状态
  - 状态为 `CONFIRMED`：抛出 `FREEZE_ALREADY_CONFIRMED` 错误（错误码：1042001004）
  - 状态为 `RELEASED`：抛出 `FREEZE_ALREADY_RELEASED` 错误（错误码：1042001005）
- **文件**：`ErrorCodeConstants.java`、`AigcQuotaFreezeServiceImpl.java`

#### 16.1.2 成本记录并发幂等
- **问题**：并发同 taskId 写入时一个请求会异常
- **修复**：捕获 `DuplicateKeyException` 后回查并返回已有记录 ID
- **文件**：`AigcCostRecordServiceImpl.java`

#### 16.1.3 支付重复通知处理
- **问题**：订单已 PAID 直接返回，不校验钱包入账和流水
- **修复**：检查流水是否存在，缺失则执行补偿入账
- **文件**：`AigcRechargeOrderServiceImpl.java`

#### 16.1.4 对账补偿 Job 实现
- **问题**：`reconcile()` 空实现，无法修复异常账务
- **修复**：实现完整对账逻辑
  - `compensateTimeoutFreeze()`：处理超时冻结补偿
  - `reconcileRechargeOrders()`：检查已支付订单缺流水
  - `reconcileConfirmedFreezes()`：检查已确认冻结缺消费流水
- **文件**：`AigcBillingJobServiceImpl.java`

#### 16.1.5 退款/系统补偿闭环
- **新增**：`refundWithRecord()` 和 `compensateWithRecord()` 接口
- **新增**：`AigcWalletService` 扩展 `refund()` 和 `compensate()` 方法
- **新增**：`AigcWalletMapper` 扩展 `refund()` 和 `compensate()` 方法
- **文件**：`AigcWalletService.java`、`AigcWalletServiceImpl.java`、`AigcWalletMapper.java`

#### 16.1.6 统计服务时间范围过滤
- **增强**：支持 `startTime/endTime` 参数过滤
- **增强**：支持自定义 TopN 限制
- **文件**：`AigcBillingStatisticsService.java`、`AigcBillingStatisticsServiceImpl.java`

#### 16.1.7 流水幂等增强
- **增强**：支持按 `bizType + bizId` 幂等写入
- **增强**：重复调用返回已有记录
- **文件**：`AigcBillingRecordServiceImpl.java`、`AigcBillingRecordMapper.java`

#### 16.1.8 测试用例补充
- **新增**：`AigcCostRecordServiceImplTest.java` - 成本记录幂等测试
- **文件**：`AigcCostRecordServiceImplTest.java`

#### 16.1.9 充值套餐配置和按套餐下单
- **新增**：`aigc_recharge_package` 充值套餐表
- **新增**：管理端充值套餐 CRUD 接口 `/aigc/billing/recharge-package/**`
- **新增**：用户端启用套餐列表接口 `/aigc/billing/recharge-package/list-enabled`
- **新增**：用户端按套餐创建订单接口 `/aigc/billing/recharge/create-by-package`
- **新增**：AIGC 充值订单创建时同步创建 Pay 支付订单，并返回 `payOrderId/payAppId` 供收银台使用
- **新增**：用户端支付状态同步接口 `/aigc/billing/recharge/sync-pay-status?id=xxx`
- **规则**：前端只传 `packageId`，后端按启用套餐配置生成订单金额和到账积分
- **文件**：`AigcRechargePackageDO.java`、`AigcRechargePackageController.java`、`AigcRechargePackageServiceImpl.java`、`AigcRechargeOrderServiceImpl.java`、`AigcRechargeAppController.java`、`AppAigcRechargeOrderCreateRespVO.java`

### 16.2 新增错误码

| 错误码 | 错误信息 | 说明 |
| ------ | -------- | ---- |
| 1042001004 | 冻结记录已确认扣费，不允许重复冻结 | 重复冻结已确认的业务 |
| 1042001005 | 冻结记录已释放，不允许重复冻结 | 重复冻结已释放的业务 |
| 1042005000 | 充值套餐不存在 | 套餐不存在或未启用 |

### 16.3 事务顺序优化

调整事务内操作顺序，优先更新钱包（更关键）：
1. 条件更新钱包余额
2. 更新冻结记录状态
3. 创建计费流水

### 16.4 当前实现评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 数据模型 | 90 | 五张核心表齐全，索引合理 |
| 核心账务链路 | 90 | 冻结/扣费/释放幂等完整，事务顺序正确 |
| 幂等与并发 | 85 | 关键场景已覆盖 |
| 事务一致性 | 85 | 流水余额快照需优化为变更后余额 |
| API/Controller 覆盖 | 85 | 接口齐全，部分统计参数已增强 |
| 统计与导出 | 75 | 时间过滤已加，SQL聚合待优化 |
| 补偿与对账 | 75 | 对账 Job 已实现 |
| 安全与审计 | 70 | 权限拆分有，操作日志待完善 |
| 测试覆盖 | 65 | 核心场景有测试 |

**综合评分：82 / 100**

### 16.5 待优化项

| 优先级 | 优化项 | 说明 |
|--------|--------|------|
| P0 | 流水余额快照 | 当前是变更前余额，需改为变更后余额 |
| P1 | 安全审计日志 | 记录操作人、调整原因 |
| P1 | SQL聚合优化 | MyBatis XML 优化统计性能 |
| P2 | 补充测试场景 | 并发、多租户隔离、异常边界 |
| P2 | 导出接口 | 实现 Excel 文件流能力 |

### 15.4 用户端路径

| 类型 | 服务内路径 | 对外路径 |
| ---- | ---------- | -------- |
| 管理端 | `/aigc/billing/**` | `/admin-api/aigc/billing/**` |
| 用户端 | `/aigc/billing/**` | `/app-api/aigc/billing/**` |
| RPC | `/rpc-api/aigc/billing/**` | 内部服务调用 |

## 16. 第一阶段落地范围

### 16.1 必须实现

- `aigc_wallet` 钱包表
- `aigc_quota_freeze` 冻结表
- `aigc_billing_record` 流水表
- `aigc_cost_record` 成本表
- `aigc_recharge_order` 充值订单表
- `aigc_recharge_package` 充值套餐表
- `AigcBillingApi` 内部 RPC
- 钱包获取和自动创建
- 积分冻结
- 确认扣费
- 释放冻结
- 计费流水创建
- 成本记录创建
- 用户端钱包详情和流水分页
- 管理端钱包、冻结、流水、成本分页查询
- 后台赠送积分和手工充值
- 管理端充值套餐配置
- 用户端读取启用充值套餐
- 用户端按套餐创建充值订单
- 冻结超时补偿任务

### 16.2 可以后置

- 真实支付订单接入
- 退款到原支付渠道
- 多币种成本换算
- 分销佣金
- 会员等级折扣
- 优惠券抵扣
- 企业账户和团队共享钱包
- 月账单和发票
- 完整财务对账中心

### 16.3 建议测试覆盖

- 钱包不存在时自动创建
- 并发创建钱包只生成一条记录
- 余额充足时冻结成功
- 余额不足时冻结失败
- 同一 `bizType + bizId` 重复冻结幂等
- 任务成功确认扣费幂等
- 任务失败释放冻结幂等
- 冻结后钱包余额和冻结余额正确变化
- 确认扣费后累计消费正确增加
- 释放冻结后可用余额正确恢复
- 手动赠送积分生成正确流水
- 成本记录毛利计算正确
- 充值重复通知不重复入账
- 统计接口返回真实聚合数据
- 冻结超时补偿任务释放过期冻结
- 用户端无法查询其他用户钱包流水
- 不同租户同一用户钱包互相隔离

## 17. 与其他 AIGC 服务的边界

| 调用方 | 调用 billing 的场景 | billing 返回 |
| ------ | ------------------- | ------------ |
| `aigc-gen` | 生成前冻结积分 | 冻结记录 ID 和冻结状态 |
| `aigc-gen` | 生成成功确认扣费 | 扣费成功结果 |
| `aigc-gen` | 生成失败释放冻结 | 释放成功结果 |
| `aigc-gen` | 模型调用完成记录成本 | 成本记录 ID |
| `aigc-task` | 异常任务补偿退款 | 释放或退款结果 |
| `aigc-model` | 成本、销售价对账统计 | 毛利和成本数据 |
| `pay` | 支付成功通知入账 | 入账结果 |

推荐调用方向：

```text
aigc-gen → aigc-model：校验模型、校验参数、计算价格
aigc-gen → aigc-billing：冻结、扣费、退款、成本
aigc-gen → aigc-task：创建任务、推进状态
aigc-gen → aigc-asset：保存生成资产
aigc-billing → pay：真实支付订单和支付状态
```

账务动作只以 `aigc-billing` 为事实源。任务状态可以失败重试，模型调用可以补偿，但钱包余额、冻结余额和计费流水必须由计费服务统一维护，避免多服务各自改余额导致账务不一致。
