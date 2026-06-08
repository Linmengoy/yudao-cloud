# yudao-module-aigc-asset 技术方案

前提：

1. 租户模块会自动注入租户
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/admin-api`、`/app-api`
3. Swagger 开发后的注释要集成到 Gateway 上去，模块必须注册独立 OpenAPI 分组并同步 Gateway Knife4j 聚合配置

## 1. 模块定位

`yudao-module-aigc-asset` 是 AIGC 平台的统一资产中心服务，负责管理用户通过 AIGC 生成、上传、导入或后续编辑沉淀的文件型内容资产，包括图片、视频、音频、文档、PPT、字幕、封面、数字人视频等。

该模块不负责调用第三方模型生成内容，不负责任务状态机，不负责钱包扣费，不负责模型价格配置，不直接实现底层对象存储能力；它负责把生成结果文件持久化到平台文件服务后，沉淀资产元数据、资产可见性、审核状态、下载统计、删除状态、来源任务和业务关联。

第一阶段优先支持图片、视频、音频等可直接消费和下载的文件型资产。文本、代码、摘要、翻译等非文件型结果可优先保存在 `aigc-task` 的 `outputText` 或 `outputData` 中，不强制进入资产中心。

当前项目中 `yudao-module-aigc-asset` 已按 `api + server` 结构落地实现，与 `aigc-model`、`aigc-task`、`aigc-billing` 保持边界一致。本文档描述已实现的技术方案。

## 2. 核心职责

### 2.1 负责内容

- 资产元数据入库
- 图片、视频、音频、文档、PPT、字幕、封面等文件型资产管理
- 外部生成结果文件下载和转存到平台文件服务
- 资产标题、描述、标签、封面、缩略图、宽高、时长、大小、格式等信息维护
- 资产来源记录，包括任务、模型、渠道商、生成类型和业务来源
- 资产列表、详情、分页查询
- 用户资产归属校验
- 管理端资产检索和运营管理
- 资产下载计数、预览计数、使用计数
- 资产软删除和恢复
- 资产审核状态记录
- 资产可见性控制
- 资产与任务、项目、社区内容、模板、工作流等业务对象的关系记录
- 资产版本管理预留
- 与 `aigc-task` 协作回写输出资产 ID

### 2.2 不负责内容

- 不直接调用第三方模型 API
- 不创建和调度生成任务
- 不处理复杂任务状态机
- 不维护模型供应商、模型参数和模型价格
- 不冻结积分、扣费或退款
- 不直接保存钱包、计费和成本流水
- 不做深度内容审核识别
- 不实现底层对象存储服务
- 不直接发布社区内容
- 不直接生成模板或工作流

对应职责归属：

| 能力 | 归属模块 |
| ---- | -------- |
| 模型、渠道、参数、价格 | `yudao-module-aigc-model` |
| 统一任务状态机 | `yudao-module-aigc-task` |
| 第三方模型调用适配 | `yudao-module-aigc-gen` |
| 钱包、冻结、扣费、退款 | `yudao-module-aigc-billing` |
| 敏感词、内容审核策略 | `yudao-module-aigc-safety` |
| 文件上传、文件存储、文件访问 URL | `yudao-module-infra` |
| 社区发布、互动、作品流 | `yudao-module-aigc-community` |

资产服务的核心抽象是“文件型内容资产”，不是“生成任务”，也不是“对象存储文件”。任务服务记录生命周期状态，文件服务提供底层存储能力，资产服务沉淀 AIGC 业务可理解、可检索、可复用的文件资产元数据。

## 3. 模块结构

### 3.1 Maven 结构

```text
yudao-module-aigc-asset
  ├── yudao-module-aigc-asset-api
  └── yudao-module-aigc-asset-server
```

命名规则遵循当前项目规范：

| 类型 | 命名 |
| ---- | ---- |
| 聚合模块目录 | `yudao-module-aigc-asset` |
| 聚合 artifactId | `yudao-module-aigc-asset` |
| API 子模块 artifactId | `yudao-module-aigc-asset-api` |
| Server 子模块 artifactId | `yudao-module-aigc-asset-server` |
| Spring 应用名 | `aigc-asset-server` |

### 3.2 根包名

```text
cn.iocoder.yudao.module.aigc.asset
```

### 3.3 API 模块结构

```text
yudao-module-aigc-asset-api
  └── src/main/java/cn/iocoder/yudao/module/aigc/asset
      ├── api
      │   └── AigcAssetApi.java
      ├── dto
      │   ├── AigcAssetCreateReqDTO.java
      │   ├── AigcAssetCreateRespDTO.java
      │   ├── AigcAssetRespDTO.java
      │   ├── AigcAssetPageReqDTO.java
      │   ├── AigcAssetUpdateReqDTO.java
      │   ├── AigcAssetAuditUpdateReqDTO.java
      │   ├── AigcAssetVisibilityUpdateReqDTO.java
      │   ├── AigcAssetDownloadReqDTO.java
      │   ├── AigcAssetRelationCreateReqDTO.java
      │   └── AigcAssetVersionCreateReqDTO.java
      └── enums
          ├── AigcAssetTypeEnum.java
          ├── AigcAssetSourceTypeEnum.java
          ├── AigcAssetBizTypeEnum.java
          ├── AigcAssetVisibilityEnum.java
          ├── AigcAssetAuditStatusEnum.java
          ├── AigcAssetStatusEnum.java
          ├── AigcAssetRelationTypeEnum.java
          └── ErrorCodeConstants.java
```

### 3.4 Server 模块结构

```text
yudao-module-aigc-asset-server
  └── src/main/java/cn/iocoder/yudao/module/aigc/asset
      ├── AigcAssetServerApplication.java
      ├── controller
      │   ├── admin
      │   │   ├── asset
      │   │   ├── relation
      │   │   └── statistics
      │   └── app
      │       └── asset
      ├── framework
      │   └── web
      │       └── config
      │           └── AigcAssetWebConfiguration.java
      ├── service
      │   ├── asset
      │   ├── file
      │   ├── relation
      │   ├── version
      │   └── statistics
      ├── dal
      │   ├── dataobject
      │   │   ├── AigcAssetDO.java
      │   │   ├── AigcAssetRelationDO.java
      │   │   ├── AigcAssetVersionDO.java
      │   │   └── AigcAssetDownloadLogDO.java
      │   └── mysql
      │       ├── AigcAssetMapper.java
      │       ├── AigcAssetRelationMapper.java
      │       ├── AigcAssetVersionMapper.java
      │       └── AigcAssetDownloadLogMapper.java
      ├── job
      │   └── AigcAssetCleanJob.java
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
        <artifactId>yudao-module-infra-api</artifactId>
        <version>${revision}</version>
    </dependency>
    <dependency>
        <groupId>cn.iocoder.cloud</groupId>
        <artifactId>yudao-module-aigc-asset-api</artifactId>
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
        <artifactId>yudao-module-aigc-model-api</artifactId>
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

- `aigc-asset` 必须依赖 `infra-api`，用于通过 `FileApi` 上传第三方生成文件、读取文件 URL 和复用平台文件能力。
- `aigc-asset` 必须依赖 `system-api`，用于管理端资产展示、用户基础信息补充和运营审计。
- `aigc-asset` 可选依赖 `aigc-task-api`，用于资产入库完成后调用 `markAssetCreating`、`markSuccess` 回写任务输出资产。
- `aigc-asset` 可选依赖 `aigc-model-api`，用于管理端资产详情展示模型名称、渠道商名称和生成能力信息。
- `aigc-asset` 不依赖 `aigc-billing-api`，资产入库不直接处理扣费，扣费由生成服务和计费服务完成。

## 5. 数据库设计

### 5.1 表清单

| 表名 | 说明 | 第一阶段 |
| ---- | ---- | -------- |
| `aigc_asset` | 资产主表 | 是 |
| `aigc_asset_download_log` | 资产下载日志 | 是 |
| `aigc_asset_relation` | 资产关系表 | 可选 |
| `aigc_asset_version` | 资产版本表 | 第二阶段 |

### 5.2 aigc_asset

资产主表，记录用户文件型资产的基础元数据、来源信息、审核状态、可见性和统计数据。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| asset_no | varchar(64) | 资产编号，全局唯一 |
| user_id | bigint | 用户 ID |
| asset_type | varchar(32) | 资产类型，IMAGE、VIDEO、AUDIO、DOCUMENT、PPT、SUBTITLE、COVER 等 |
| source_type | varchar(32) | 来源类型，GENERATE、UPLOAD、IMPORT、EDIT、CLONE 等 |
| biz_type | varchar(64) | 业务类型，TASK、PROJECT、COMMUNITY、TEMPLATE、WORKFLOW 等 |
| biz_id | varchar(128) | 业务 ID，可存任务编号、项目 ID、社区内容 ID 等 |
| task_id | bigint | 来源任务 ID，可为空 |
| task_no | varchar(64) | 来源任务编号，可为空 |
| model_id | bigint | 来源模型 ID，可为空 |
| provider_id | bigint | 来源渠道商 ID，可为空 |
| title | varchar(255) | 资产标题 |
| description | varchar(1024) | 资产描述 |
| tags | varchar(512) | 标签，逗号分隔或 JSON 字符串 |
| file_id | bigint | 平台文件 ID，可为空，按 infra 文件能力实际返回决定 |
| file_url | varchar(1024) | 平台文件 URL |
| origin_url | varchar(1024) | 第三方原始 URL，仅内部保存，不对用户端直接暴露 |
| cover_file_id | bigint | 封面文件 ID |
| cover_url | varchar(1024) | 封面 URL |
| thumbnail_url | varchar(1024) | 缩略图 URL |
| mime_type | varchar(128) | MIME 类型 |
| file_ext | varchar(32) | 文件扩展名 |
| file_size | bigint | 文件大小，单位字节 |
| width | int | 图片或视频宽度 |
| height | int | 图片或视频高度 |
| duration | decimal(18,3) | 视频或音频时长，单位秒 |
| metadata | json | 资产扩展元数据 |
| prompt_snapshot | json | 生成提示词快照，可脱敏保存 |
| generate_snapshot | json | 生成参数快照 |
| visibility | varchar(32) | 可见性，PRIVATE、PUBLIC、LINK、TENANT 等 |
| audit_status | varchar(32) | 审核状态，PENDING、PASS、REJECT、MANUAL_REVIEW 等 |
| audit_reason | varchar(512) | 审核原因 |
| status | varchar(32) | 状态，NORMAL、DELETED、DISABLED 等 |
| view_count | int | 预览次数 |
| download_count | int | 下载次数 |
| use_count | int | 使用次数 |
| last_used_time | datetime | 最近使用时间 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
uk_asset_no = asset_no
uk_task_type_tenant = task_id + asset_type + tenant_id
idx_user_type_time = user_id + asset_type + create_time
idx_task_id = task_id
idx_task_no = task_no
idx_biz = biz_type + biz_id
idx_model_id = model_id
idx_audit_status = audit_status
idx_visibility = visibility
idx_status = status
```

说明：`uk_task_type_tenant` 唯一索引用于保证同一任务同一类型资产的创建幂等性，防止重复入库。

### 5.3 aigc_asset_download_log

资产下载日志表，用于记录用户下载行为，支撑下载计数、风控、运营统计和管理端审计。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| asset_id | bigint | 资产 ID |
| asset_no | varchar(64) | 资产编号 |
| user_id | bigint | 下载用户 ID |
| owner_user_id | bigint | 资产归属用户 ID |
| download_url | varchar(1024) | 下载 URL 或签名 URL |
| client_ip | varchar(64) | 客户端 IP |
| user_agent | varchar(512) | User-Agent |
| referer | varchar(512) | 来源页面 |
| result | varchar(32) | 下载结果，SUCCESS、FAILED |
| fail_reason | varchar(512) | 失败原因 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
idx_asset_time = asset_id + create_time
idx_user_time = user_id + create_time
idx_owner_time = owner_user_id + create_time
```

### 5.4 aigc_asset_relation

资产关系表，用于记录资产与项目、社区内容、模板、工作流、成片、素材包等对象之间的绑定关系。第一阶段可只预留结构，按业务使用情况逐步落地。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| asset_id | bigint | 资产 ID |
| relation_type | varchar(64) | 关系类型，PROJECT、COMMUNITY_CONTENT、TEMPLATE、WORKFLOW、PUBLISH、COLLECTION 等 |
| relation_id | varchar(128) | 关联业务 ID |
| relation_name | varchar(255) | 关联业务名称 |
| sort | int | 排序 |
| extra_info | json | 扩展信息 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
idx_asset_id = asset_id
idx_relation = relation_type + relation_id
uk_asset_relation = asset_id + relation_type + relation_id + tenant_id
```

### 5.5 aigc_asset_version

资产版本表，用于第二阶段支持图片高清修复、局部重绘、视频剪辑、音频降噪、文档改写等编辑链路的版本追踪。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| asset_id | bigint | 当前资产 ID |
| parent_asset_id | bigint | 父资产 ID |
| root_asset_id | bigint | 根资产 ID |
| version_no | int | 版本号 |
| version_name | varchar(128) | 版本名称 |
| operation_type | varchar(64) | 操作类型，GENERATE、EDIT、UPSCALE、REDRAW、CUT、MERGE 等 |
| file_url | varchar(1024) | 版本文件 URL |
| metadata | json | 版本元数据 |
| creator/create_time/updater/update_time/deleted/tenant_id | 标准字段 | 标准字段 |

索引：

```text
idx_asset_id = asset_id
idx_parent_asset_id = parent_asset_id
idx_root_version = root_asset_id + version_no
```

## 6. 枚举设计

### 6.1 AigcAssetTypeEnum

| 值 | 说明 |
| -- | ---- |
| IMAGE | 图片 |
| VIDEO | 视频 |
| AUDIO | 音频 |
| DOCUMENT | 文档 |
| PPT | PPT |
| SUBTITLE | 字幕 |
| COVER | 封面 |
| DIGITAL_HUMAN_VIDEO | 数字人视频 |
| OTHER | 其他 |

### 6.2 AigcAssetSourceTypeEnum

| 值 | 说明 |
| -- | ---- |
| GENERATE | AIGC 生成 |
| UPLOAD | 用户上传 |
| IMPORT | 外部导入 |
| EDIT | 编辑产生 |
| CLONE | 克隆产生 |

### 6.3 AigcAssetBizTypeEnum

| 值 | 说明 |
| -- | ---- |
| TASK | 生成任务 |
| PROJECT | 创作项目 |
| COMMUNITY | 社区内容 |
| TEMPLATE | 模板 |
| WORKFLOW | 工作流 |
| PUBLISH | 发布导出 |

### 6.4 AigcAssetVisibilityEnum

| 值 | 说明 |
| -- | ---- |
| PRIVATE | 仅本人可见 |
| PUBLIC | 公开可见 |
| LINK | 链接可见 |
| TENANT | 租户内可见 |

### 6.5 AigcAssetAuditStatusEnum

| 值 | 说明 |
| -- | ---- |
| PENDING | 待审核 |
| PASS | 审核通过 |
| REJECT | 审核拒绝 |
| MANUAL_REVIEW | 人工复审 |

### 6.6 AigcAssetStatusEnum

| 值 | 说明 |
| -- | ---- |
| NORMAL | 正常 |
| DELETED | 已删除 |
| DISABLED | 已禁用 |

## 7. API 设计

### 7.1 内部 RPC API

`aigc-asset-api` 对内部服务暴露 `AigcAssetApi`：

```text
AigcAssetApi
  ├── createAsset(AigcAssetCreateReqDTO) -> AigcAssetCreateRespDTO
  ├── createImageAsset(AigcAssetCreateReqDTO) -> AigcAssetCreateRespDTO
  ├── createVideoAsset(AigcAssetCreateReqDTO) -> AigcAssetCreateRespDTO
  ├── createAudioAsset(AigcAssetCreateReqDTO) -> AigcAssetCreateRespDTO
  ├── createDocumentAsset(AigcAssetCreateReqDTO) -> AigcAssetCreateRespDTO
  ├── getAsset(Long assetId) -> AigcAssetRespDTO
  ├── getAssetByTaskId(Long taskId) -> AigcAssetRespDTO
  ├── getUserAssets(AigcAssetPageReqDTO) -> PageResult<AigcAssetRespDTO>
  ├── increaseDownloadCount(AigcAssetDownloadReqDTO) -> Boolean
  ├── updateAuditStatus(AigcAssetAuditUpdateReqDTO) -> Boolean
  └── updateVisibility(AigcAssetVisibilityUpdateReqDTO) -> Boolean
```

### 7.2 管理端接口

服务内路径统一以 `/aigc/asset` 开头：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/aigc/asset/page` | 资产分页 |
| GET | `/aigc/asset/get` | 资产详情 |
| PUT | `/aigc/asset/update` | 修改资产基础信息 |
| PUT | `/aigc/asset/audit` | 更新审核状态 |
| PUT | `/aigc/asset/visibility` | 更新可见性 |
| DELETE | `/aigc/asset/delete` | 删除资产 |
| PUT | `/aigc/asset/recover` | 恢复资产 |
| GET | `/aigc/asset/download-log/page` | 下载日志分页 |
| GET | `/aigc/asset/statistics` | 资产统计 |
| GET | `/aigc/asset/export-excel` | 导出资产列表 |
| GET | `/aigc/asset/download-log/export-excel` | 导出下载日志 |

### 7.3 用户端接口

服务内路径统一以 `/aigc/asset` 开头，对外用户端由网关补充 `/app-api` 前缀：

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/aigc/asset/my-page` | 我的资产分页 |
| GET | `/aigc/asset/my-get` | 我的资产详情 |
| GET | `/aigc/asset/my-category-counts` | 我的资产分类数量 |
| POST | `/aigc/asset/upload` | 上传资产（支持 MultipartFile） |
| POST | `/aigc/asset/access-url` | 获取单个资产文件运行时访问 URL |
| POST | `/aigc/asset/access-urls` | 批量获取资产文件运行时访问 URL |
| PUT | `/aigc/asset/update` | 修改我的资产信息 |
| PUT | `/aigc/asset/visibility` | 修改我的资产可见性 |
| DELETE | `/aigc/asset/delete` | 删除我的资产 |
| POST | `/aigc/asset/download` | 下载资产并记录下载日志 |
| POST | `/aigc/asset/use` | 标记资产被使用 |

用户端接口必须校验资产归属：私有资产只能由 `asset.userId` 对应用户访问；公开资产需审核通过后才能访问详情和预览，下载、复用、编辑仍需结合登录态和审核状态判断。权限校验逻辑已实现于 `getAccessibleAsset()` 方法。

`/aigc/asset/my-page` 支持按 `assetType` 和 `sourceType` 过滤，供用户端资产库拆分展示来源：

| 资产页分组 | 查询条件 |
| ---------- | -------- |
| 生成图片 | `assetType=IMAGE&sourceType=GENERATE` |
| 上传图片 | `assetType=IMAGE&sourceType=UPLOAD` |
| 生成视频 | `assetType=VIDEO` |
| 其它文件 | 客户端兜底分组，展示非图片、非视频资产 |

上传参考图如果通过 `/aigc/asset/upload` 资产化，应写入 `sourceType=UPLOAD`。生成服务创建的图片、视频资产应写入 `sourceType=GENERATE`。前端从资产库选择参考图只会引用已有资产，不应把参考图来源类型改成生成结果来源类型。

`/aigc/asset/my-category-counts` 与 `/my-page` 使用同一套用户归属、状态、标题搜索等公共过滤条件，但会分别返回以下计数字段：

| 字段 | 统计口径 |
| ---- | ---- |
| `allCount` | 当前用户全部未删除资产 |
| `generatedImageCount` | `assetType=IMAGE&sourceType=GENERATE` |
| `uploadedImageCount` | `assetType=IMAGE&sourceType=UPLOAD` |
| `videoCount` | `assetType=VIDEO` |
| `otherCount` | 非图片、非视频资产 |

资产库分类胶囊上的数量必须使用该接口，不能从当前分页结果推导。否则切换分类或分页加载不完整时，分类总数会随着当前页数据变化而不一致。

## 8. 核心流程

### 8.1 生成结果入库流程

```text
aigc-gen 收到第三方生成完成回调
  ↓
aigc-gen 调用 aigc-task markDownloading
  ↓
aigc-gen 下载第三方结果文件或把第三方 URL 传给 aigc-asset
  ↓
aigc-asset 调用 infra FileApi 上传到平台文件服务
  ↓
aigc-asset 创建 aigc_asset 记录
  ↓
aigc-asset 调用 aigc-task markAssetCreating
  ↓
aigc-asset 调用 aigc-task markSuccess，回写 outputAssetId、outputAssetType
  ↓
用户可在任务结果和资产中心查看资产
```

说明：

- 第三方 URL 不能长期作为资产主 URL 使用，必须转存到平台文件服务，避免第三方 URL 过期、鉴权失效或被替换。
- 如果 `aigc-gen` 已经完成文件下载，`aigc-asset` 可接收本地文件、字节流或临时 URL，并统一通过 `FileApi` 上传。
- 如果资产入库失败，任务不能标记 `SUCCESS`，应由 `aigc-gen` 或补偿任务继续重试或推进失败退款流程。
- 文件转存已通过 `prepareFile()` 方法实现，支持下载外部文件并调用 `FileApi.createFile()` 上传到平台存储。普通 HTTP 下载可走 Hutool；SOCKS/SOCKS5H 代理下载优先走 `curl --socks5-hostname`，避免 JDK/Hutool SOCKS 实现在部分代理和 Cloudflare 目标上认证或连接超时。
- 任务回写采用 `tryMarkTaskSuccess()` 方法包装，任务服务调用失败时记录日志但不回滚资产创建，保障资产入库可靠性。

### 8.2 用户上传资产流程

```text
用户选择文件上传
  ↓
用户端 Controller 校验文件类型、大小和权限
  ↓
aigc-asset 调用 infra FileApi 上传文件
  ↓
aigc-asset 创建 aigc_asset 记录，sourceType=UPLOAD
  ↓
根据安全策略设置 auditStatus=PENDING 或 PASS
  ↓
返回资产详情
```

### 8.3 资产下载流程

```text
用户请求下载资产
  ↓
校验资产存在、状态正常、审核通过、权限允许
  ↓
生成或返回下载 URL
  ↓
写入 aigc_asset_download_log
  ↓
download_count + 1
```

### 8.4 资产删除流程

```text
用户或管理员删除资产
  ↓
校验归属或管理权限
  ↓
标记 status=DELETED 或执行逻辑删除
  ↓
保留文件 URL 和审计记录
  ↓
异步清理任务按策略决定是否物理删除底层文件
```

第一阶段建议优先软删除，不立即删除底层文件，避免任务结果、社区内容、模板引用资产后出现不可恢复的问题。物理清理应在确认无业务引用后由定时任务执行。

## 9. 与其他模块协作

### 9.1 与 aigc-gen 协作

`aigc-gen` 是资产创建的主要调用方：

```text
createImageAsset
createVideoAsset
createAudioAsset
createDocumentAsset
```

`aigc-gen` 负责第三方模型提交、轮询、回调处理和生成结果解析；`aigc-asset` 负责将结果文件转存并生成资产记录。

### 9.2 与 aigc-task 协作

`aigc-asset` 可以调用 `aigc-task`：

```text
getTask
markAssetCreating
markSuccess
```

用于图片、视频、音频、文档、PPT、数字人视频等文件型结果入库完成后回写任务输出资产。`markSuccess` 必须携带 `outputAssetId` 和 `outputAssetType`，便于任务详情和用户端生成结果页直接展示。

### 9.3 与 aigc-model 协作

`aigc-asset` 可在管理端详情、资产统计或资产来源展示中调用：

```text
getModel
getProvider
```

模型信息只用于展示和分析，不参与资产核心写入链路，避免资产入库因模型服务短暂不可用而失败。

### 9.4 与 aigc-billing 协作

`aigc-asset` 不直接调用 `aigc-billing`。资产创建成功不是扣费事实源，扣费应由 `aigc-gen` 在生成成功后调用 `aigc-billing` 确认扣费，或由任务、计费补偿链路协同处理。

### 9.5 与 aigc-safety 协作

第一阶段可通过同步或异步方式接入基础审核：

```text
aigc-asset 创建资产
  ↓
调用或发送审核请求给 aigc-safety
  ↓
aigc-safety 返回审核结果
  ↓
aigc-asset 更新 auditStatus
```

如果 `aigc-safety` 暂未落地，资产服务可先使用 `PENDING`、`PASS`、`REJECT` 字段预留审核状态，并支持管理端人工更新。

### 9.6 与 infra 文件服务协作

`aigc-asset` 必须通过 `infra-api` 的 `FileApi` 使用平台统一文件能力：

```text
uploadFile
getFile
```

资产表中的 `fileUrl`、`coverUrl`、`thumbnailUrl` 应保存平台文件服务返回的 URL。第三方原始 URL 只作为内部排查字段保存，不应作为用户端长期访问地址。

## 10. 权限与安全

### 10.1 用户隔离

- 用户只能查看、修改、删除自己的私有资产。
- 管理端可按权限查看租户内资产，但不得越租户访问。
- 租户隔离依赖基础租户插件自动注入 `tenant_id`，所有查询默认带租户条件。
- 用户端详情接口必须校验 `userId`、`visibility`、`auditStatus`、`status`。

### 10.2 URL 安全

- 用户端不直接暴露第三方生成原始 URL。
- 如文件服务支持签名 URL，下载接口应返回短期有效下载地址。
- 私有资产下载前必须校验登录态和资产归属。
- 管理端可查看原始 URL，但应避免导出敏感临时鉴权参数。

### 10.3 内容安全

- 资产创建后应进入审核链路，默认可配置为 `PENDING` 或先审后展。
- 审核拒绝资产不允许公开展示、社区发布或模板复用。
- 管理端人工审核必须记录审核人、审核时间和审核原因。

### 10.4 文件校验

- 上传资产必须限制文件类型、文件大小和 MIME 类型。
- 生成结果入库必须校验下载文件大小，避免异常大文件耗尽存储。
- 文件扩展名不能作为唯一可信依据，应结合 MIME 和文件服务校验结果。

## 11. 幂等与一致性

### 11.1 创建幂等

资产创建应支持通过以下组合实现幂等：

```text
sourceType + taskId + assetType + tenantId
```

对于同一个任务多次回调或补偿重试，若已存在同类型输出资产，应直接返回已创建资产，避免重复入库和重复回写任务成功。

### 11.2 任务回写一致性

资产入库和任务成功回写不是同一个数据库事务，必须按最终一致性处理：

```text
资产创建成功
  ↓
回写任务成功失败
  ↓
补偿任务扫描 taskId 已有资产但任务仍处于 ASSET_CREATING/DOWNLOADING
  ↓
再次调用 markSuccess
```

### 11.3 下载计数一致性

下载日志写入和 `download_count` 增加可以使用本地事务。高并发下载场景可先写日志，再异步汇总计数，第一阶段可直接数据库自增。

### 11.4 删除一致性

资产删除先更新业务状态，底层文件物理删除异步执行。若底层文件删除失败，不影响业务侧资产不可见状态，但需要记录清理失败日志并支持补偿。

## 12. 缓存与性能

- 用户资产分页以数据库查询为主，按 `user_id + asset_type + create_time` 建索引。
- 热门公开资产详情可使用 Redis 缓存，但私有资产权限校验不能只依赖缓存。
- 下载计数、预览计数、使用计数可以使用 Redis 累加后定时刷库，第一阶段可直接数据库更新。
- 管理端统计建议按天、资产类型、来源类型、审核状态聚合，避免直接扫描大表。
- 大文件下载不经资产服务转发流量，资产服务只做鉴权、日志和 URL 下发。

## 13. OpenAPI 与网关接入

### 13.1 OpenAPI 分组

模块需要注册独立 OpenAPI 分组：

```text
分组名：aigc-asset
扫描包：cn.iocoder.yudao.module.aigc.asset.controller
```

### 13.2 Gateway Knife4j 聚合

Gateway 需要增加 `aigc-asset` 文档聚合配置，使管理端和用户端接口在统一 Swagger 页面可见。

### 13.3 URL 前缀

| 类型 | 对外 URL | 服务内路径 |
| ---- | -------- | ---------- |
| 管理端 | `/aigc/asset` | `/aigc/asset` |
| 用户端 | `/app-api/aigc/asset` | `/aigc/asset` |

Controller 不手写 `/admin-api`、`/app-api`。

## 14. 错误码设计

建议在 `ErrorCodeConstants` 中预留以下错误码：

| 错误码 | 说明 |
| ------ | ---- |
| ASSET_NOT_EXISTS | 资产不存在 |
| ASSET_NO_PERMISSION | 无权访问该资产 |
| ASSET_STATUS_INVALID | 资产状态不可操作 |
| ASSET_AUDIT_NOT_PASS | 资产审核未通过 |
| ASSET_FILE_EMPTY | 资产文件为空 |
| ASSET_FILE_TYPE_UNSUPPORTED | 不支持的文件类型 |
| ASSET_FILE_SIZE_EXCEED | 文件大小超限 |
| ASSET_UPLOAD_FAILED | 文件上传失败 |
| ASSET_DOWNLOAD_FAILED | 文件下载失败 |
| ASSET_CREATE_DUPLICATE | 资产重复创建 |
| ASSET_TASK_NOT_EXISTS | 来源任务不存在 |
| ASSET_TASK_STATUS_INVALID | 来源任务状态不允许创建资产 |
| ASSET_RELATION_EXISTS | 资产关系已存在 |
| ASSET_VERSION_NOT_EXISTS | 资产版本不存在 |

## 15. 测试建议

### 15.1 Service 测试

- 创建图片资产成功。
- 创建视频资产成功并保存封面。
- 同一任务重复创建资产时返回已有资产。
- 私有资产归属校验通过和失败。
- 审核状态更新成功。
- 可见性更新成功。
- 资产删除后用户端不可见。
- 下载日志写入后下载计数增加。

### 15.2 RPC 测试

- `createAsset` 能创建资产并返回资产 ID。
- `getAsset` 能返回完整资产信息。
- `getAssetByTaskId` 能按任务查询输出资产。
- `increaseDownloadCount` 能幂等记录下载行为。
- `updateAuditStatus` 能更新审核状态。

### 15.3 Controller 测试

- 管理端分页查询支持类型、用户、审核状态、来源类型筛选。
- 用户端只能查询自己的私有资产。
- 用户端无法删除其他用户资产。
- 公开资产详情按审核状态和可见性控制访问。
- 上传接口能拦截非法文件类型和超限文件。

### 15.4 集成测试

- 模拟 `aigc-gen` 创建文件型资产后，任务状态能被推进到 `SUCCESS`。
- 模拟任务回写失败后，补偿逻辑能再次回写成功。
- 模拟 `FileApi` 上传失败时，资产记录不应创建成功。
- 多租户场景下不同租户用户不能互查资产。

## 16. 验收标准

### 16.1 管理端验收

- 可以分页查看所有资产。
- 可以按资产类型、来源类型、审核状态、用户、创建时间筛选。
- 可以查看资产详情、来源任务、模型信息和文件信息。
- 可以更新资产审核状态。
- 可以禁用、删除和恢复资产。
- 可以查看资产下载日志和基础统计。

### 16.2 用户端验收

- 用户可以查看自己的图片、视频、音频等资产列表。
- 用户可以查看自己的资产详情。
- 用户可以上传资产并在资产列表中看到。
- 用户可以下载自己的资产。
- 用户可以删除自己的资产。
- 用户无法查看、修改、删除其他用户私有资产。

### 16.3 服务间调用验收

- `aigc-gen` 能调用 `createImageAsset`、`createVideoAsset`、`createAudioAsset` 创建资产。
- 第三方文件能被转存到平台文件服务。
- 资产创建成功后能回写 `aigc-task` 的 `outputAssetId` 和 `outputAssetType`。
- 重复回调或补偿重试不会生成重复资产。
- 非文件型文本结果可以不经过资产服务。

### 16.4 安全验收

- 私有资产访问必须校验用户归属。
- 公开资产展示必须校验审核状态。
- 用户端不暴露第三方原始 URL。
- 删除资产不会立即破坏仍被业务引用的底层文件。
- 多租户隔离测试通过。

### 16.5 质量门禁

```text
mvn -pl yudao-module-aigc-asset/yudao-module-aigc-asset-server -am test
mvn -pl yudao-module-aigc-asset/yudao-module-aigc-asset-server -am -DskipTests compile
```

当前模块尚未落地代码，以上命令为模块实现后的验收门禁。

## 17. 第一阶段落地建议

第一阶段建议只实现最小可赚钱闭环所需能力：

```text
资产主表
图片/视频/音频资产创建
第三方文件转存
用户资产列表和详情
资产下载
管理端资产分页和审核
任务成功回写
创建幂等
多租户隔离
```

暂缓实现：

```text
复杂资产版本树
素材项目库
团队共享资产
资产市场
复杂公开链接权限
大规模统计看板
物理文件清理策略
```

`yudao-module-aigc-asset` 要保持“资产中心”的纯粹性：它是 AIGC 文件型结果的业务元数据中心，不是生成服务、不是任务服务、不是计费服务、也不是底层文件存储服务。

第一阶段只要它能稳定提供：

```text
文件转存
资产入库
资产查询
权限隔离
审核状态
下载记录
任务回写
```

就足够支撑 `aigc-gen`、`aigc-task`、`aigc-billing` 跑通图片、视频、音频等 AIGC 生成结果的商业化闭环，并为后续社区、模板、工作流和发布导出服务沉淀可复用资产。

## 18. 签名 URL 存储改造方案

本节记录 2026-06-05 已落地的资产访问 URL 改造。此前资产表长期保存 `fileUrl`、`coverUrl`、`thumbnailUrl`，在 OSS/S3 私有桶场景下可能保存带 `X-Amz-Signature`、`X-Amz-Expires`、`X-Amz-Credential` 的临时签名 URL，导致资产过期后无法预览或下载。

改造后的核心原则：

- 数据库不保存 OSS/S3 预签名 URL。
- `aigc_asset` 只保存资产业务信息。
- `aigc_asset_file` 保存稳定文件引用，包括 `file_id`、`storage_config_id`、`object_key`、`file_path`、`access_mode`。
- 私有资源访问 URL 由后端根据 `storage_config_id + file_path` 动态生成。
- Redis 缓存未超时签名 URL，避免每次访问都重新生成。
- 下载日志不保存完整签名 URL。

### 18.1 数据库模型

`aigc_asset` 移除以下长期 URL 和文件字段：

```text
file_id
file_url
origin_url
cover_file_id
cover_url
thumbnail_url
mime_type
file_ext
file_size
```

新增 `aigc_asset_file`，用于保存文件级稳定引用：

```text
asset_id              资产 ID
file_role             文件角色：ORIGINAL/PREVIEW/THUMBNAIL/COVER/WATERMARK/TRANSCODED
file_id               Infra 文件 ID
storage_config_id     文件存储配置 ID
storage_type          存储类型
bucket                Bucket
object_key            对象 Key
file_path             平台文件路径
origin_url            第三方原始 URL，仅溯源
access_mode           访问模式：1 私有签名、2 公开、3 CDN 公开
public_url            公开访问 URL，仅公开资源/CDN 资源使用
```

SQL 已同步维护在：

- `sql/mysql/asset/asset_db.sql`
- `yudao-module-aigc-asset/yudao-module-aigc-asset-server/src/main/resources/schema/asset_db.sql`

### 18.2 Infra 文件服务 V2

新增文件创建响应 DTO：

```text
FileCreateRespDTO
  id
  configId
  storageType
  bucket
  objectKey
  path
  name
  url
  type
  size
  publicAccess
```

新增文件预签名响应 DTO：

```text
FilePresignRespDTO
  url
  expireSeconds
  expireTime
  publicAccess
```

新增 RPC 接口：

```text
FileApi.createFileV2(...)
FileApi.presignGetUrlV2(configId, path, expirationSeconds)
```

V2 上传不再只返回 URL，而是返回稳定文件引用。`infra_file.url` 入库前会移除 query 参数，避免私有桶签名参数落库。

### 18.3 资产创建流程

资产创建统一走以下流程：

```text
1. 用户上传、第三方 URL 或 Data URL 进入资产服务
2. 资产服务下载/解码文件内容
3. 生成唯一存储文件名后调用 FileApi.createFileV2 上传到平台文件服务
4. aigc_asset 写业务主记录
5. aigc_asset_file 写 ORIGINAL 文件引用
6. 接口返回资产 ID、资产文件 ID 和运行时访问 URL
```

`origin_url` 只写入 `aigc_asset_file.origin_url` 用于溯源，不作为展示、预览、下载地址。

资产服务在调用文件服务前统一生成服务端存储文件名，用户上传文件名、资产标题和生成标题只作为展示元数据。用户上传、第三方 URL 转存、Data URL 解码入库都必须走同一套唯一命名逻辑，避免同一天同路径下 `IMAGE生成资产.png`、`image.png` 这类固定名称互相覆盖，导致多个不同 `assetId` 指向同一个底层对象。

### 18.4 访问 URL 与 Redis 缓存

访问 URL 生成遵循以下流程：

```text
1. 查询 asset 和 asset_file
2. 校验资产状态、审核状态、用户权限
3. access_mode=PUBLIC/CDN_PUBLIC 时直接返回 public_url
4. access_mode=PRIVATE_SIGNED 时先查 Redis
5. Redis 命中且剩余 TTL > 60 秒，直接复用缓存 URL
6. Redis 未命中或即将过期，调用 FileApi.presignGetUrlV2 生成新 URL
7. 新 URL 写入 Redis，TTL = 签名有效期 - 60 秒
8. 返回访问 URL
```

缓存 Key：

```text
aigc:asset:access-url:{tenantId}:{assetFileId}:{fileRole}:{accessType}:{userKey}
```

锁 Key：

```text
aigc:asset:access-url:lock:{tenantId}:{assetFileId}:{fileRole}:{accessType}:{userKey}
```

缓存策略：

- `PREVIEW`、`THUMBNAIL`、`COVER` 不按用户缓存，`userKey=PUBLIC`。
- `DOWNLOAD` 按用户缓存，`userKey=userId`。
- Redis 剩余 TTL 小于等于 60 秒视为不可复用。
- 使用 Redisson 锁防止同一个资产文件缓存击穿。

默认有效期：

| 访问类型 | 签名有效期 | Redis TTL |
| -------- | ---------- | --------- |
| PREVIEW | 900 秒 | 840 秒 |
| THUMBNAIL | 900 秒 | 840 秒 |
| COVER | 900 秒 | 840 秒 |
| DOWNLOAD | 1800 秒 | 1740 秒 |

### 18.5 用户端接口

新增/调整用户端接口：

```text
GET  /aigc/asset/my-get
GET  /aigc/asset/my-page
POST /aigc/asset/access-url
POST /aigc/asset/access-urls
POST /aigc/asset/download
```

下载接口现在返回 `AigcAssetAccessUrlRespDTO`，不再直接返回数据库中的 `fileUrl` 字符串。下载接口每次调用仍增加下载次数，但下载日志不保存完整签名 URL。

`POST /aigc/asset/access-urls` 接收多个 `assetId + fileRole + accessType` 请求，批量返回 `AigcAssetAccessUrlRespDTO`。Canvas 打开项目、恢复 snapshot 或 URL 接近过期时应优先调用批量接口刷新图片/视频节点展示地址；单个详情接口 `/my-get` 只作为兼容兜底，避免大量节点逐个请求导致首屏图片显示很慢。

### 18.6 响应兼容

`AigcAssetRespDTO` 新增 `files` 文件列表，包含每个文件角色的访问 URL 和过期信息。

同时保留兼容字段：

```text
fileUrl
coverUrl
thumbnailUrl
mimeType
fileExt
fileSize
```

这些兼容字段现在表示运行时访问 URL，不再表示数据库长期保存字段。画布和工作流当前读取这些字段的代码可继续工作，但不得把这些运行时 URL 重新持久化为资产主数据。

### 18.7 生成模块影响

`aigc-gen` 对 Data URL 不再提前上传并写入 `fileUrl`，而是把 Data URL 交给资产服务统一资产化。生成记录仍保存 `assetIds`，展示 URL 通过资产查询或访问 URL 接口动态获取。

### 18.8 安全约束

- `aigc_asset` 不保存签名 URL。
- `aigc_asset_file.public_url` 只允许公开资源或 CDN 资源使用。
- 私有资源只保存稳定引用，访问时动态签名。
- 下载日志不保存完整签名 URL。
- Redis 可短期保存签名 URL，但 TTL 必须小于签名 URL 有效期。
- 画布 JSON、项目封面、生成记录不得长期保存带签名参数的访问 URL。

### 18.9 上传/转存文件命名防覆盖

当前实现约束：

- `/aigc/asset/upload` 用户上传资产时，即使原始文件名相同，也必须创建新的底层存储对象。
- `createImageAsset`、`createVideoAsset`、`createFromRemoteUrl`、`createFromDataUrl` 等生成结果资产化入口，不能直接使用固定标题或第三方文件名作为对象存储 Key。
- 原始 `fileName`、`title`、`mimeType`、宽高、大小等信息继续保存为资产展示和检索元数据，不参与对象存储唯一性判断。
- Canvas 上传、`/app` 参考图上传和生成结果资产化都依赖资产服务这一层防覆盖逻辑，前端不应通过相同文件名覆盖旧资产。

### 18.10 已验证命令

```text
mvn -pl yudao-module-aigc-asset/yudao-module-aigc-asset-server -am -DskipTests compile
mvn -pl yudao-module-infra/yudao-module-infra-server,yudao-module-aigc-gen/yudao-module-aigc-gen-server,yudao-module-aigc-workflow/yudao-module-aigc-workflow-server -am -DskipTests compile
```

验证结果均为 `BUILD SUCCESS`。

## 19. 境外生成结果下载代理方案

本节记录 2026-06-08 已落地的境外生成结果下载方案，主要用于 Grok 等模型返回的 `https://imagine-public.x.ai/...` 图片 URL。

### 19.1 背景

Grok 生图链路里，`aigc-gen` 调用上游模型成功后会拿到第三方图片 URL，再调用 `aigc-asset` 创建资产。线上曾出现以下状态：

```text
provider_status = SUCCESS
output_urls = ["https://imagine-public.x.ai/...jpg"]
status = FAILED
asset_ids = NULL
fail_message = 文件下载失败 / Read timed out executing POST ...create-image-asset
```

这表示模型生成已成功，失败点在 `aigc-asset` 下载第三方图片并转存平台文件服务。

### 19.2 代理配置来源

代理不由用户端或画布传入。配置链路为：

```text
管理端 AIGC 模型管理 / 代理管理
  -> model_db.aigc_model_proxy
  -> 渠道商启用代理并选择 proxy_id
  -> aigc-model 返回渠道配置时展开代理
  -> aigc-gen 调用模型并把代理快照传给 aigc-asset
  -> aigc-asset 下载 originUrl 时使用该代理
```

`aigc_model_provider` 上保留的 `proxy_protocol`、`proxy_host`、`proxy_port`、`proxy_username`、`proxy_password` 是旧版内联代理兼容字段。新增配置应使用 `proxy_id`。

### 19.3 SOCKS 下载实现约定

Java/Hutool 的 `basicProxyAuth` 只适合 HTTP 代理；SOCKS5 认证应使用 JDK `Authenticator`。但实测部分 SOCKS5 代理访问 `imagine-public.x.ai` 时，Java 路径会卡在：

```text
java.net.SocksSocketImpl.readSocksReply
SocketTimeoutException: Connect timed out
```

同一台容器内使用 curl 可以成功。因此资产服务对 SOCKS/SOCKS5H 远程文件下载使用 `ProcessBuilder` 调用 curl：

```text
curl --socks5-hostname host:port --proxy-user username:password -L --max-time ...
```

注意：代码里通过 `ProcessBuilder` 传参，密码中的 `$` 不会被 shell 展开。手工在服务器 shell 里测试时，如果密码包含 `$`，必须用单引号或环境变量避免被 shell 改写。

容器镜像要求：`aigc-asset` 运行镜像必须包含 `curl`。

### 19.4 代理测试目标

管理端“代理管理”的测试按钮当前访问：

```text
https://api.ipify.org
```

该测试只证明代理连通、认证可用，并返回延迟；不等价于业务目标域名可用。Grok 图片下载还需要验证：

```text
https://imagine-public.x.ai/
```

### 19.5 服务器排查命令

检查 `aigc-asset` 下载日志：

```bash
docker logs --since 10m aigc-asset 2>&1 | grep -A80 -B20 'downloadOriginFile'
docker logs --since 10m aigc-asset 2>&1 | grep -A80 -B20 'downloadOriginFileByCurl'
```

在容器内测试代理访问 Grok 图片域名：

```bash
docker exec aigc-asset sh -lc 'curl --socks5-hostname 代理IP:1080 --proxy-user "用户名:密码" -L -I --max-time 20 https://imagine-public.x.ai/'
```

密码包含 `$` 时使用环境变量：

```bash
docker exec -e SOCKS_PASS='N8v$Qz2!mR7#xLp9@T4w' aigc-asset sh -lc \
  'curl --socks5-hostname 136.118.240.18:1080 --proxy-user "copseAI:$SOCKS_PASS" -L -I --max-time 20 https://imagine-public.x.ai/'
```

如果返回 `HTTP/2 403` 但不是 `timeout` 或 `authentication failed`，通常说明代理已经连通到 Cloudflare，后续真实图片 URL 可能仍可下载；应继续用生成记录里的完整 `output_urls` 测试。

### 19.6 需要重跑的服务

根据改动范围重跑：

| 改动 | 需要重跑 |
| ---- | -------- |
| 代理管理页面 | `draw2video-admin` |
| 代理表、代理测试接口、渠道商下拉选择 | `aigc-model`、`draw2video-admin` |
| 生成时读取渠道代理并传给资产服务 | `aigc-gen` |
| 第三方结果 URL 代理下载、curl fallback | `aigc-asset` |

SQL 迁移需在生产库执行：

```text
sql/mysql/aigc_model_proxy.sql
sql/mysql/system/aigc_model_proxy_menu.sql
```
