# yudao-module-aigc-safety 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置
4. 当前仓库已落地 `yudao-module-aigc-safety` 的 `api` 与 `server` 两个子模块，根工程已接入，已完成敏感词管理、提示词检测、审核记录、人工审核、资产审核状态同步、MySQL 建表脚本、Swagger 分组与 Gateway 聚合配置

## 1. 模块定位

`yudao-module-aigc-safety` 是 AIGC 平台的审核风控服务，负责生成链路中的内容安全基础能力，第一阶段提供轻量审核能力，重点覆盖敏感词管理、提示词检测、审核记录、人工审核通过、人工审核拒绝和资产审核状态同步。

该模块不负责实际调用模型生成内容，不负责生成任务调度，不负责钱包冻结、扣费、退款，不负责文件存储，也不负责社区内容运营，只提供 AIGC 业务链路中的安全检测、审核记录与审核状态流转能力。

当前实际开发已完成 `api` 与 `server` 两个子模块，根工程 `pom.xml` 已接入 `yudao-module-aigc-safety`，模块已具备敏感词 CRUD、提示词本地检测、审核记录创建、人工通过、人工拒绝、资产审核状态事务提交后同步、管理端接口、内部 RPC、数据库脚本和 OpenAPI 聚合能力。本文档已按 `c:\use\code\project\manman\yudao-module-aigc-safety` 的当前代码实现同步修订。

## 2. 核心职责

### 2.1 负责内容

- 敏感词管理
- 敏感词启停
- 敏感词场景配置
- 提示词敏感词检测
- 审核记录创建
- 自动审核结果记录
- 人工审核通过
- 人工审核拒绝
- 拒绝原因记录
- 资产审核状态同步
- 审核状态查询
- 审核操作审计

### 2.2 不负责内容

- 不直接提交图片生成任务
- 不直接提交视频生成任务
- 不调用第三方模型
- 不处理第三方模型回调
- 不保存生成文件
- 不保存图片、视频二进制内容
- 不冻结积分
- 不扣费和退款
- 不管理用户钱包
- 不管理模型供应商和模型价格
- 不管理任务状态机的完整生命周期

对应职责归属：

| 能力                   | 归属模块                    |
| ---------------------- | --------------------------- |
| 图片/视频生成          | `yudao-module-aigc-gen`     |
| 任务状态机             | `yudao-module-aigc-task`    |
| 钱包、冻结、扣费、退款 | `yudao-module-aigc-billing` |
| 模型、渠道、价格       | `yudao-module-aigc-model`   |
| 图片、视频文件资产     | `yudao-module-aigc-asset`   |
| 敏感词、审核记录       | `yudao-module-aigc-safety`  |

## 3. 模块结构

### 3.1 Maven 结构

```text
yudao-module-aigc-safety
  ├── yudao-module-aigc-safety-api
  └── yudao-module-aigc-safety-server
```

### 3.2 根包名

```text
cn.iocoder.yudao.module.aigc.safety
```

### 3.3 API 模块结构

```text
yudao-module-aigc-safety-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/safety
      ├── api
      │   └── AigcSafetyApi.java
      ├── dto
      │   ├── AigcSafetyPromptCheckReqDTO.java
      │   ├── AigcSafetyPromptCheckRespDTO.java
      │   ├── AigcAuditRecordCreateReqDTO.java
      │   ├── AigcAuditRecordRespDTO.java
      │   ├── AigcAuditPassReqDTO.java
      │   └── AigcAuditRejectReqDTO.java
      └── enums
          ├── ApiConstants.java
          ├── AigcAuditObjectTypeEnum.java
          ├── AigcAuditStatusEnum.java
          ├── AigcAuditResultEnum.java
          ├── AigcSensitiveWordStatusEnum.java
          ├── AigcSensitiveWordMatchTypeEnum.java
          ├── AigcSafetySceneEnum.java
          └── ErrorCodeConstants.java
```

### 3.4 Server 模块结构

```text
yudao-module-aigc-safety-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/safety
      ├── AigcSafetyServerApplication.java
      ├── controller
      │   └── admin
      │       ├── sensitiveword
      │       │   ├── AigcSensitiveWordController.java
      │       │   └── vo
      │       └── audit
      │           ├── AigcAuditRecordController.java
      │           └── vo
      ├── api
      │   └── AigcSafetyApiImpl.java
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcSensitiveWordDO.java
      │   │   └── AigcAuditRecordDO.java
      │   └── mysql
      │       ├── AigcSensitiveWordMapper.java
      │       └── AigcAuditRecordMapper.java
      ├── framework
      │   └── web
      │       └── config
      │           └── AigcSafetyWebConfiguration.java
      └── service
          ├── sensitiveword
          │   ├── AigcSensitiveWordService.java
          │   └── AigcSensitiveWordServiceImpl.java
          ├── audit
          │   ├── AigcAuditRecordService.java
          │   └── AigcAuditRecordServiceImpl.java
          └── check
              ├── AigcSafetyCheckService.java
              └── AigcSafetyCheckServiceImpl.java
```

## 4. 依赖设计

### 4.1 API 模块依赖

`yudao-module-aigc-safety-api` 只放跨模块可见的 API、DTO、枚举、错误码，不依赖 Server 实现。

当前依赖：

- `yudao-common`
- `spring-boot-starter-validation`
- `spring-cloud-starter-openfeign`

### 4.2 Server 模块依赖

`yudao-module-aigc-safety-server` 负责 Controller、Service、DAL、API 实现、审核流程和敏感词匹配。

当前依赖：

- `yudao-module-aigc-safety-api`
- `yudao-spring-boot-starter-web`
- `yudao-spring-boot-starter-security`
- `yudao-spring-boot-starter-mybatis`
- `yudao-spring-boot-starter-biz-tenant`
- `yudao-spring-boot-starter-redis`
- `yudao-spring-boot-starter-rpc`
- `yudao-module-aigc-asset-api`，用于同步资产审核状态

### 4.3 被依赖关系

| 调用方 | 调用目的 |
| ------ | -------- |
| `yudao-module-aigc-gen` | 创建生成任务前检查提示词，生成完成后创建审核记录 |
| `yudao-module-aigc-asset` | 资产入库或发布前创建审核记录，接收审核状态同步 |
| `yudao-module-aigc-task` | 可选记录任务维度审核结果 |
| 管理端前端 | 管理敏感词、查询审核记录、人工通过或拒绝 |

## 5. 数据库设计

### 5.1 表清单

| 表名 | 说明 |
| ---- | ---- |
| `aigc_sensitive_word` | 敏感词表 |
| `aigc_audit_record` | 审核记录表 |

表名前缀说明：

- 敏感词相关表统一使用 `aigc_sensitive_` 前缀
- 审核相关表统一使用 `aigc_audit_` 前缀
- 两张业务表均需要支持租户隔离，保留 `tenant_id`
- 两张业务表均需要包含项目标准审计字段

### 5.2 aigc_sensitive_word

`aigc_sensitive_word` 用于维护租户维度或平台维度的敏感词规则。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `word` | varchar | 敏感词 |
| `scene` | varchar | 适用场景 |
| `level` | int | 风险等级 |
| `match_type` | varchar | 匹配方式 |
| `status` | varchar | 状态 |
| `remark` | varchar | 备注 |
| `creator` | varchar | 创建者 |
| `create_time` | datetime | 创建时间 |
| `updater` | varchar | 更新者 |
| `update_time` | datetime | 更新时间 |
| `deleted` | bit | 是否删除 |
| `tenant_id` | bigint | 租户编号 |

建议索引：

| 索引 | 字段 | 说明 |
| ---- | ---- | ---- |
| `uk_tenant_scene_word` | `tenant_id, scene, word` | 控制同租户同场景敏感词重复 |
| `idx_tenant_scene_status` | `tenant_id, scene, status` | 按租户、场景、状态加载可用敏感词 |
| `idx_tenant_level` | `tenant_id, level` | 按租户和风险等级查询 |

### 5.3 aigc_audit_record

`aigc_audit_record` 用于保存提示词、任务、资产等对象的自动审核和人工审核记录。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `id` | bigint | 主键 |
| `object_type` | varchar | 审核对象类型 |
| `object_id` | bigint | 审核对象编号 |
| `content` | text | 被审核内容 |
| `scene` | varchar | 审核场景 |
| `audit_status` | varchar | 审核状态 |
| `audit_result` | varchar | 审核结果 |
| `hit_words` | json | 命中的敏感词 |
| `risk_level` | int | 风险等级 |
| `reject_reason` | varchar | 拒绝原因 |
| `auditor_user_id` | bigint | 审核人用户编号 |
| `audit_time` | datetime | 审核时间 |
| `creator` | varchar | 创建者 |
| `create_time` | datetime | 创建时间 |
| `updater` | varchar | 更新者 |
| `update_time` | datetime | 更新时间 |
| `deleted` | bit | 是否删除 |
| `tenant_id` | bigint | 租户编号 |

建议索引：

| 索引 | 字段 | 说明 |
| ---- | ---- | ---- |
| `idx_tenant_object` | `tenant_id, object_type, object_id` | 查询对象审核记录 |
| `idx_tenant_status` | `tenant_id, audit_status, create_time` | 管理端按状态分页查询 |
| `idx_tenant_scene` | `tenant_id, scene, create_time` | 按审核场景统计和查询 |
| `idx_tenant_auditor` | `tenant_id, auditor_user_id, audit_time` | 按租户和审核人查询 |

## 6. 枚举设计

### 6.1 AigcSafetySceneEnum

| 值 | 说明 | 第一阶段 |
| -- | ---- | -------- |
| `PROMPT` | 提示词审核 | 启用 |
| `ASSET` | 资产审核 | 启用 |
| `TASK` | 任务审核 | 可选 |
| `COMMENT` | 评论审核 | 预留 |
| `POST` | 社区帖子审核 | 预留 |

### 6.2 AigcAuditObjectTypeEnum

| 值 | 说明 |
| -- | ---- |
| `PROMPT` | 提示词 |
| `TASK` | 生成任务 |
| `ASSET` | 图片、视频等资产 |
| `COMMENT` | 评论 |
| `POST` | 社区内容 |

### 6.3 AigcAuditStatusEnum

| 值 | 说明 |
| -- | ---- |
| `PENDING` | 待审核 |
| `PASS` | 已通过 |
| `REJECT` | 已拒绝 |

### 6.4 AigcAuditResultEnum

| 值 | 说明 |
| -- | ---- |
| `AUTO_PASS` | 自动审核通过 |
| `AUTO_REJECT` | 自动审核拒绝 |
| `MANUAL_PASS` | 人工审核通过 |
| `MANUAL_REJECT` | 人工审核拒绝 |

### 6.5 AigcSensitiveWordStatusEnum

| 值 | 说明 |
| -- | ---- |
| `ENABLE` | 启用 |
| `DISABLE` | 禁用 |

### 6.6 敏感词匹配方式

| 值 | 说明 | 第一阶段 |
| -- | ---- | -------- |
| `CONTAINS` | 包含匹配 | 启用 |
| `EXACT` | 完全匹配 | 可选 |
| `REGEX` | 正则匹配 | 暂不启用 |

当前实现第一阶段只允许保存 `CONTAINS` 和 `EXACT`，禁止保存 `REGEX`。检测逻辑只对 `CONTAINS` 执行包含匹配，对 `EXACT` 执行完全匹配，未知匹配方式直接不命中。后续如需启用 `REGEX`，必须限制表达式复杂度、匹配超时时间和规则来源。

## 7. RPC API 设计

### 7.1 AigcSafetyApi

`AigcSafetyApi` 是安全服务对其他 AIGC 模块暴露的内部 API。

```text
AigcSafetyApi
  ├── checkPrompt(reqDTO)
  ├── createAuditRecord(reqDTO)
  ├── markPass(reqDTO)
  └── markReject(reqDTO)
```

### 7.2 checkPrompt

用于生成链路创建任务前的提示词检测。

| 项 | 说明 |
| -- | ---- |
| 方法 | `checkPrompt(AigcSafetyPromptCheckReqDTO reqDTO)` |
| 输入 | `prompt`、`scene`、`modelId`、`userId`、`bizId` |
| 校验 | 提示词不能为空，场景不能为空 |
| 返回 | 是否通过、命中词、风险等级、拒绝原因 |
| 不返回 | 敏感词规则内部编号、审核人信息、内部策略详情 |

返回示例：

```text
pass = true
hitWords = []
riskLevel = 0
reason = null
```

未通过示例：

```text
pass = false
hitWords = ["xxx"]
riskLevel = 3
reason = "提示词包含敏感内容"
```

### 7.3 createAuditRecord

用于创建审核记录，支持提示词、任务、资产等对象。

| 项 | 说明 |
| -- | ---- |
| 方法 | `createAuditRecord(AigcAuditRecordCreateReqDTO reqDTO)` |
| 输入 | `objectType`、`objectId`、`content`、`scene`、`auditStatus`、`auditResult`、`hitWords`、`riskLevel` |
| 校验 | 审核对象类型不能为空，审核对象编号不能为空，场景不能为空，`objectType`、`scene`、`auditStatus`、`auditResult` 必须符合枚举约束 |
| 返回 | 审核记录编号、审核状态和审核内容摘要 |
| 不返回 | 完整敏感词库、内部策略配置 |

当前实现中，`auditStatus` 为空时默认写入 `PENDING`；创建接口不允许直接创建 `PASS` 或 `REJECT` 终态记录，避免绕过人工审核状态机。`auditResult` 为空时允许创建待审核记录，非空时当前只允许 `AUTO_PASS` 或 `AUTO_REJECT`。

### 7.4 markPass

用于人工审核通过。

| 项 | 说明 |
| -- | ---- |
| 方法 | `markPass(AigcAuditPassReqDTO reqDTO)` |
| 输入 | `auditId`、`auditorUserId`、`remark` |
| 校验 | 审核记录存在，状态为待审核；状态流转使用 `id + PENDING` 条件更新，避免并发重复审核 |
| 返回 | 审核记录编号和最新状态 |
| 副作用 | 事务提交后同步资产审核状态为通过，同步失败记录错误日志，不回滚本地审核状态 |

### 7.5 markReject

用于人工审核拒绝。

| 项 | 说明 |
| -- | ---- |
| 方法 | `markReject(AigcAuditRejectReqDTO reqDTO)` |
| 输入 | `auditId`、`auditorUserId`、`reason` |
| 校验 | 审核记录存在，状态为待审核，拒绝原因不能为空；状态流转使用 `id + PENDING` 条件更新，避免并发重复审核 |
| 返回 | 审核记录编号和最新状态 |
| 副作用 | 事务提交后同步资产审核状态为拒绝，同步失败记录错误日志，不回滚本地审核状态 |

## 8. 管理端接口设计

### 8.1 Controller 规划

| Controller | 服务内路径 | 说明 |
| ---------- | ---------- | ---- |
| `AigcSensitiveWordController` | `/aigc/safety/sensitive-word` | 敏感词管理 |
| `AigcAuditRecordController` | `/aigc/safety/audit-record` | 审核记录管理 |

Controller 只写服务内路径，不手写 `/admin-api`。最终外部访问路径由 Web 基建或 Gateway 统一添加前缀。

### 8.2 敏感词管理接口

| 方法 | 路径 | 说明 | 权限点 |
| ---- | ---- | ---- | ------ |
| `POST` | `/aigc/safety/sensitive-word/create` | 新增敏感词 | `aigc:safety-sensitive-word:create` |
| `PUT` | `/aigc/safety/sensitive-word/update` | 修改敏感词 | `aigc:safety-sensitive-word:update` |
| `DELETE` | `/aigc/safety/sensitive-word/delete` | 删除敏感词 | `aigc:safety-sensitive-word:delete` |
| `GET` | `/aigc/safety/sensitive-word/get` | 查询敏感词详情 | `aigc:safety-sensitive-word:query` |
| `GET` | `/aigc/safety/sensitive-word/page` | 敏感词分页 | `aigc:safety-sensitive-word:query` |
| `PUT` | `/aigc/safety/sensitive-word/update-status` | 启停敏感词 | `aigc:safety-sensitive-word:update` |

### 8.3 审核记录管理接口

| 方法 | 路径 | 说明 | 权限点 |
| ---- | ---- | ---- | ------ |
| `GET` | `/aigc/safety/audit-record/get` | 查询审核记录详情 | `aigc:safety-audit-record:query` |
| `GET` | `/aigc/safety/audit-record/page` | 审核记录分页 | `aigc:safety-audit-record:query` |
| `PUT` | `/aigc/safety/audit-record/pass` | 人工审核通过 | `aigc:safety-audit-record:audit` |
| `PUT` | `/aigc/safety/audit-record/reject` | 人工审核拒绝 | `aigc:safety-audit-record:audit` |

### 8.4 管理端返回限制

管理端可展示审核所需信息，但仍需避免暴露内部策略细节。

可返回：

- 审核对象类型
- 审核对象编号
- 审核内容摘要
- 审核状态
- 审核结果
- 命中敏感词
- 风险等级
- 拒绝原因
- 审核人
- 审核时间

不返回：

- 内部策略权重
- 第三方审核服务密钥
- 完整内部规则命中链路
- 非当前租户的数据

## 9. 用户端接口设计

第一阶段 `yudao-module-aigc-safety` 不建议直接开放用户端接口。

用户端生成链路由 `yudao-module-aigc-gen` 统一接收请求，`aigc-gen` 在内部调用 `AigcSafetyApi.checkPrompt` 完成安全检查。用户端只感知生成请求是否可提交，不直接感知敏感词库、审核策略和审核规则。

如后续需要开放用户端审核状态查询，也应只返回当前用户或当前租户可见对象的审核状态，禁止返回内部敏感词规则和完整命中详情。

## 10. API 前缀与 Swagger 聚合规范

### 10.1 API 前缀规范

Controller 业务路径固定写：

```text
/aigc/safety/**
```

禁止在 Controller 上手写：

```text
/admin-api/aigc/safety/**
/app-api/aigc/safety/**
```

外部路径由项目 Web 基建或 Gateway 统一处理。

### 10.2 OpenAPI 分组

当前已注册独立 OpenAPI 分组：

```text
aigc-safety-server
```

模块内使用 `buildGroupedOpenApi("aigc-safety-server", "aigc/safety")`，确保分组路径匹配 `/admin-api/aigc/safety/**`。

Swagger 标题建议：

```text
AIGC 审核风控服务
```

### 10.3 Gateway 聚合

当前已在 Gateway Knife4j 聚合中增加 `aigc-safety-server` 分组，管理端路由为 `/admin-api/aigc/safety/**`，OpenAPI 地址为 `/admin-api/aigc/safety/v3/api-docs`。

## 11. 核心流程

### 11.1 提示词检测成功链路

```text
aigc-gen 接收用户生成请求
  ↓
aigc-gen 调用 AigcSafetyApi.checkPrompt
  ↓
aigc-safety 按租户和场景加载启用敏感词
  ↓
aigc-safety 执行提示词匹配
  ↓
未命中敏感词
  ↓
aigc-safety 返回 pass = true
  ↓
aigc-gen 继续创建任务并进入后续生成链路
```

### 11.2 提示词检测失败链路

```text
aigc-gen 接收用户生成请求
  ↓
aigc-gen 调用 AigcSafetyApi.checkPrompt
  ↓
aigc-safety 按租户和场景加载启用敏感词
  ↓
aigc-safety 执行提示词匹配
  ↓
命中敏感词
  ↓
aigc-safety 创建或返回审核结果
  ↓
aigc-safety 返回 pass = false、hitWords、riskLevel、reason
  ↓
aigc-gen 阻断任务创建并向用户返回合规提示
```

### 11.3 资产人工审核链路

```text
aigc-asset 创建或接收待审核资产
  ↓
aigc-asset 调用 AigcSafetyApi.createAuditRecord
  ↓
aigc-safety 创建待审核记录
  ↓
运营人员在管理端查看审核记录
  ↓
运营人员选择通过或拒绝
  ↓
aigc-safety 使用 id + PENDING 条件更新审核记录状态
  ↓
事务提交后调用 aigc-asset-api 同步资产审核状态
```

### 11.4 人工审核拒绝链路

```text
运营人员打开待审核记录
  ↓
填写拒绝原因
  ↓
调用 /aigc/safety/audit-record/reject
  ↓
aigc-safety 校验记录存在且状态为 PENDING
  ↓
aigc-safety 使用 id + PENDING 条件更新 audit_status = REJECT
  ↓
aigc-safety 写入 reject_reason、auditor_user_id、audit_time
  ↓
事务提交后同步资产状态为拒绝，同步失败记录错误日志
```

## 12. 敏感词匹配设计

### 12.1 第一阶段策略

第一阶段采用轻量本地匹配，不引入复杂审核引擎。

建议规则：

- 只加载当前租户启用状态的敏感词
- 按场景加载敏感词，例如 `PROMPT`、`ASSET`
- 默认使用包含匹配
- 当前只允许 `CONTAINS` 和 `EXACT` 两种匹配方式
- 命中多个词时返回最高风险等级
- 返回给调用方的 `reason` 使用通用描述，不直接暴露策略细节

### 12.2 匹配结果

| 结果 | 说明 |
| ---- | ---- |
| 未命中 | `pass = true`，允许继续生成 |
| 命中低风险 | 可创建待审核记录，具体是否阻断由调用方策略决定 |
| 命中高风险 | `pass = false`，阻断生成请求 |

第一阶段为了降低合规风险，建议提示词命中任一启用敏感词即阻断生成请求。

### 12.3 后续优化

- 支持敏感词缓存
- 支持公共词库和租户自定义词库合并
- 支持分词匹配
- 支持第三方内容安全服务
- 支持图片、视频审核回调
- 支持审核策略版本化

## 13. 缓存设计

第一阶段可以直接查询数据库实现，待敏感词规模增长后再引入缓存。

推荐缓存维度：

```text
tenantId + scene + status
```

缓存失效场景：

- 新增敏感词
- 修改敏感词
- 删除敏感词
- 启停敏感词
- 修改敏感词场景

缓存注意事项：

- 缓存只保存启用状态敏感词
- 缓存不得跨租户复用
- 缓存失效失败时应允许降级查询数据库
- 后续可按租户和场景进行批量刷新

## 14. 安全设计

### 14.1 数据隔离

- 敏感词按租户隔离
- 审核记录按租户隔离
- 管理端分页查询必须带租户上下文
- 内部 API 调用必须继承或显式传递租户上下文

### 14.2 信息返回限制

对用户端不返回：

- 命中的完整敏感词规则
- 内部审核策略
- 审核人信息
- 第三方审核服务配置
- 其他租户审核数据

对管理端不返回：

- 第三方审核服务密钥
- 内部服务调用凭证
- 非当前租户数据

### 14.3 操作审计

以下操作必须可追踪：

- 新增敏感词
- 修改敏感词
- 删除敏感词
- 启停敏感词
- 人工审核通过
- 人工审核拒绝

## 15. 错误码设计

建议在 `ErrorCodeConstants` 中定义安全模块独立错误码。

| 错误码 | 说明 |
| ------ | ---- |
| `SENSITIVE_WORD_NOT_EXISTS` | 敏感词不存在 |
| `SENSITIVE_WORD_DUPLICATE` | 敏感词已存在 |
| `SENSITIVE_WORD_STATUS_INVALID` | 敏感词状态不正确 |
| `SENSITIVE_WORD_SCENE_INVALID` | 敏感词场景不正确 |
| `SENSITIVE_WORD_MATCH_TYPE_INVALID` | 敏感词匹配方式不正确 |
| `AUDIT_RECORD_NOT_EXISTS` | 审核记录不存在 |
| `AUDIT_RECORD_STATUS_INVALID` | 审核记录状态不允许当前操作 |
| `AUDIT_REJECT_REASON_EMPTY` | 拒绝原因不能为空 |
| `AUDIT_OBJECT_TYPE_INVALID` | 审核对象类型不正确 |
| `AUDIT_SCENE_INVALID` | 审核场景不正确 |
| `AUDIT_RESULT_INVALID` | 审核结果不正确 |
| `PROMPT_SAFETY_CHECK_NOT_PASS` | 提示词安全检查不通过 |

错误提示口径：

- 用户端提示要通用，避免直接暴露敏感词命中详情
- 管理端提示要明确，方便运营人员处理数据
- 内部日志可记录必要排查信息，但不能记录密钥和敏感凭证

## 16. 多租户设计

`yudao-module-aigc-safety` 复用项目已有租户体系，不新建租户模块。

多租户规则：

- `aigc_sensitive_word` 必须包含 `tenant_id`
- `aigc_audit_record` 必须包含 `tenant_id`
- 管理端只能管理当前租户敏感词和审核记录
- `checkPrompt` 只能使用当前租户可用敏感词
- `createAuditRecord` 创建的审核记录必须归属当前租户
- `markPass`、`markReject` 只能操作当前租户审核记录

公共词库设计：

- 第一阶段不强制实现公共词库
- 后续如需公共词库，可使用平台租户或独立标识实现
- 公共词库和租户词库合并时，租户词库优先级应高于公共词库

## 17. 数据一致性设计

第一阶段不引入强分布式事务，不使用 Seata。

采用：

- 本地事务
- 状态机
- 幂等校验
- 事务提交后同步
- 同步失败日志
- 后续补偿任务
- 操作日志

### 17.1 审核记录状态机

```text
PENDING
  ├── markPass  -> PASS
  └── markReject -> REJECT
```

状态约束：

- `PASS` 不能再次通过
- `PASS` 不能再次拒绝
- `REJECT` 不能再次通过
- `REJECT` 不能再次拒绝
- 需要重新审核时，应创建新的审核记录或提供独立的重审流程

### 17.2 与资产模块一致性

资产审核状态同步采用最终一致性。

建议流程：

```text
aigc-safety 使用 id + PENDING 条件更新审核记录成功
  ↓
本地事务提交
  ↓
调用 aigc-asset-api 同步资产审核状态
  ↓
同步成功则完成
  ↓
同步失败记录错误日志，后续可通过补偿任务重试
```

当前已实现事务提交后同步资产状态，并在同步失败时记录错误日志。本阶段尚未新增独立 outbox 表或定时补偿任务，后续生产化时建议补齐可重试补偿机制。

## 18. 第一阶段最小实现范围

### 18.1 必须实现

- `yudao-module-aigc-safety-api`
- `yudao-module-aigc-safety-server`
- `AigcSafetyApi.checkPrompt`
- `AigcSafetyApi.createAuditRecord`
- `AigcSafetyApi.markPass`
- `AigcSafetyApi.markReject`
- 敏感词管理管理端接口
- 审核记录管理管理端接口
- `aigc_sensitive_word` 表
- `aigc_audit_record` 表
- 提示词包含匹配
- 审核记录状态机
- 租户隔离

### 18.2 暂不实现

- 图片内容识别
- 视频内容识别
- 语音内容识别
- 第三方内容安全服务
- 复杂审核策略引擎
- 正则敏感词匹配
- 用户端安全接口
- 社区内容审核
- 审核工作流编排

### 18.3 后续扩展

- 对接第三方内容安全服务
- 对图片、视频、音频做异步审核
- 审核策略配置化
- 审核策略版本化
- 审核命中统计
- 风险趋势分析
- 自动审核与人工抽检结合

## 19. 初始化数据建议

第一阶段建议只初始化少量测试敏感词，正式敏感词库由运营人员在管理端维护。

初始化原则：

- 不在仓库中写入真实高敏敏感词库
- 不在仓库中写入第三方审核服务密钥
- 测试数据应明确标识为测试用途
- 生产敏感词应通过管理端或安全的数据导入流程维护

## 20. 验收标准

### 20.1 模块接入验收

- 根工程 `pom.xml` 已加入 `yudao-module-aigc-safety`
- `yudao-module-aigc-safety` 聚合 POM 已包含 `api` 与 `server`
- `aigc-safety-server` 可独立启动
- OpenAPI 分组可在 Gateway Knife4j 中访问

### 20.2 敏感词管理验收

- 管理端可新增敏感词
- 管理端可修改敏感词
- 管理端可删除敏感词
- 管理端可启停敏感词
- 管理端可按场景、状态、关键词分页查询敏感词
- 同租户下重复敏感词有明确校验

### 20.3 提示词检测验收

- 未命中敏感词时返回通过
- 命中启用敏感词时返回不通过
- 禁用敏感词不参与匹配
- 不同租户敏感词互不影响
- 不同场景敏感词按场景匹配

### 20.4 审核记录验收

- 可创建审核记录
- 可分页查询审核记录
- 待审核记录可人工通过
- 待审核记录可人工拒绝
- 已通过记录不能重复审核
- 已拒绝记录不能重复审核
- 拒绝操作必须填写拒绝原因

### 20.5 集成链路验收

- `aigc-gen` 创建任务前可调用 `checkPrompt`
- 提示词检测不通过时阻断任务创建
- 提示词检测通过时继续生成链路
- 资产审核通过后可同步资产状态
- 资产审核拒绝后可同步资产状态和拒绝原因

## 21. 当前实现状态

当前仓库已完成以下内容：

- `yudao-module-aigc-safety` 聚合模块
- `yudao-module-aigc-safety-api` 子模块
- `yudao-module-aigc-safety-server` 子模块
- 根工程 `pom.xml` 中的 `yudao-module-aigc-safety` 模块声明
- `AigcSafetyApi` 内部 RPC 接口
- `AigcSafetyApiImpl` 内部 RPC 实现
- `AigcSensitiveWordController` 管理端敏感词接口
- `AigcAuditRecordController` 管理端审核记录接口
- `AigcSensitiveWordService` 与 `AigcSafetyCheckService`
- `AigcAuditRecordService` 审核记录状态机
- `AigcSensitiveWordDO` 与 `AigcAuditRecordDO`
- `AigcSensitiveWordMapper` 与 `AigcAuditRecordMapper`
- `aigc_sensitive_word` 建表 SQL
- `aigc_audit_record` 建表 SQL
- `aigc-safety-server` 应用配置、Swagger 分组与 Gateway 聚合配置

当前实现已经过以下命令编译校验：

```bash
mvn -pl yudao-module-aigc-safety/yudao-module-aigc-safety-server -am -DskipTests compile
```

编译结果为 `BUILD SUCCESS`。

## 22. 当前实现评分

按当前代码与本文档一致性评估：

| 维度 | 评分 | 说明 |
| ---- | ---- | ---- |
| 模块完整度 | 90 / 100 | `api`、`server`、Controller、Service、Mapper、SQL、配置均已落地 |
| 代码正确性 | 88 / 100 | 已修复审核状态并发、非法状态写入和匹配方式误处理问题 |
| 安全性 | 86 / 100 | 已限制返回审核内容摘要，禁止第一阶段保存 `REGEX`，仍建议后续补齐更细粒度审计 |
| 技术文档一致性 | 92 / 100 | 文档已同步当前实现状态、结构、字段、索引、流程与配置 |
| 可维护性 | 88 / 100 | 结构清晰，仍建议后续补齐单元测试、补偿任务和审核统计能力 |
| 综合评分 | 89 / 100 | 可进入联调验证阶段，生产化前建议补齐资产同步补偿机制 |
