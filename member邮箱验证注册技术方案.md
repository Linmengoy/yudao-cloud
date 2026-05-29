# Member 邮箱验证注册技术方案

## 1. 方案定位

本方案用于给 `yudao-module-member` 增加用户端邮箱账号能力，支持用户通过邮箱验证码完成注册，并在注册成功后自动登录；当前已进一步补齐邮箱密码登录、邮箱验证码登录、邮箱找回密码、绑定 / 换绑邮箱等 P1 能力。

本方案不包含管理端建设内容，不新增管理端页面，不规划管理端菜单，不改造管理端登录，不新增 `/admin-api` 业务接口。

当前项目已有前提：

1. 租户模块会自动注入租户 SQL，业务开发不需要手写租户过滤条件。
2. API 前缀由项目 Web 基建根据 Controller 包路径自动添加识别，Controller 不手写 `/app-api`。
3. 本方案仅开放用户端接口，对外路径统一为 `/app-api/member/**`。
4. 当前 Member 已有手机号密码登录、短信登录、短信验证码、Token 刷新、退出登录、会员用户信息等能力。
5. 当前 System 已有邮件账号、邮件模板、邮件日志、异步邮件发送和邮件发送 RPC 能力。

目标链路：

```text
用户输入邮箱
  ↓
发送邮箱验证码
  ↓
用户输入验证码和密码
  ↓
后端校验邮箱验证码
  ↓
创建 member 用户
  ↓
标记邮箱已验证
  ↓
创建 OAuth2 Token
  ↓
返回 accessToken / refreshToken
```

## 2. 当前能力与缺口

### 2.1 已有能力

会员认证已有接口：

```text
POST /app-api/member/auth/login
POST /app-api/member/auth/email-login
POST /app-api/member/auth/sms-login
POST /app-api/member/auth/email-code-login
POST /app-api/member/auth/send-sms-code
POST /app-api/member/auth/validate-sms-code
POST /app-api/member/auth/send-email-code
POST /app-api/member/auth/validate-email-code
POST /app-api/member/auth/email-register
POST /app-api/member/auth/refresh-token
POST /app-api/member/auth/logout
GET  /app-api/member/user/get
PUT  /app-api/member/user/update
PUT  /app-api/member/user/reset-password-by-email
PUT  /app-api/member/user/update-email
```

已有后端能力：

- 手机号密码登录
- 短信验证码登录
- 短信登录时手机号不存在自动创建用户
- OAuth2 accessToken / refreshToken 创建
- Token 刷新
- 退出登录
- 会员用户资料查询和修改
- 手机号找回密码
- 手机号换绑
- 邮箱验证码注册并自动登录
- 邮箱密码登录
- 邮箱验证码登录
- 邮箱找回密码
- 邮箱绑定 / 换绑
- 系统邮件账号配置
- 系统邮件模板配置
- 系统邮件日志
- 邮件 MQ 异步发送

### 2.2 当前缺口

| 能力 | 当前状态 | 处理方式 |
| ---- | ---- | ---- |
| 独立邮箱注册接口 | 已完成 | 已新增 `/member/auth/email-register` |
| 邮箱验证码发送接口 | 已完成 | 已新增 `/member/auth/send-email-code` |
| 邮箱验证码校验接口 | 已完成 | 已新增 `/member/auth/validate-email-code` |
| 会员邮箱字段 | 已完成 | `member_user` 已增加邮箱字段 |
| 邮箱唯一索引 | 已完成 | 已增加邮箱唯一索引 |
| 邮箱验证码表 | 已完成 | 已新增 `member_email_code` |
| 邮箱密码登录 | 已完成 | 已新增 `/member/auth/email-login` |
| 邮箱验证码登录 | 已完成 | 已新增 `/member/auth/email-code-login` |
| 邮箱找回密码 | 已完成 | 已新增 `/member/user/reset-password-by-email` |
| 邮箱绑定/换绑 | 已完成 | 已新增 `/member/user/update-email`，按用户当前是否已有邮箱动态选择 `BIND_EMAIL` 或 `CHANGE_EMAIL` 场景 |

## 3. 建设范围

### 3.1 P0 必须建设

- 会员邮箱字段
- 邮箱唯一索引
- 邮箱验证码表
- 邮箱验证码场景枚举
- 发送邮箱验证码
- 校验邮箱验证码
- 消费邮箱验证码
- 邮箱注册
- 注册成功自动登录
- 邮件模板初始化 SQL
- 邮箱注册接口测试

### 3.2 P1 后续建设

- 邮箱密码登录：已完成
- 邮箱验证码登录：已完成
- 邮箱找回密码：已完成
- 用户绑定邮箱：已完成
- 用户换绑邮箱：已完成
- 注册欢迎邮件
- 图形验证码接入
- IP 风控策略增强

## 4. 接口设计

### 4.1 用户端接口

Controller 内部路径使用：

```text
/member/auth
```

对外路径由网关和前缀规则形成：

```text
/app-api/member/auth
```

P0 新增接口：

| 方法 | 对外路径 | Controller 路径 | 说明 |
| ---- | ---- | ---- | ---- |
| POST | `/app-api/member/auth/send-email-code` | `/member/auth/send-email-code` | 发送邮箱验证码 |
| POST | `/app-api/member/auth/validate-email-code` | `/member/auth/validate-email-code` | 校验邮箱验证码 |
| POST | `/app-api/member/auth/email-register` | `/member/auth/email-register` | 邮箱验证码注册 |

已补齐的 P1 接口：

| 方法 | 对外路径 | 说明 |
| ---- | ---- | ---- |
| POST | `/app-api/member/auth/email-login` | 邮箱密码登录 |
| POST | `/app-api/member/auth/email-code-login` | 邮箱验证码登录 |
| PUT | `/app-api/member/user/reset-password-by-email` | 邮箱找回密码 |
| PUT | `/app-api/member/user/update-email` | 绑定或换绑邮箱 |

实现说明：

- `email-code-login` 会先校验用户存在、账号未禁用、邮箱已验证，再消费 `LOGIN` 场景邮箱验证码，避免失败登录浪费验证码。
- `update-email` 会根据用户当前是否已有邮箱动态选择验证码场景：无邮箱时消费 `BIND_EMAIL`，已有邮箱时消费 `CHANGE_EMAIL`。
- 邮箱验证码消费使用 `id + used = false` 条件更新，保证同一个验证码并发消费只能成功一次。

### 4.2 发送邮箱验证码

请求：

```json
{
  "email": "user@example.com",
  "scene": "REGISTER"
}
```

响应：

```json
{
  "code": 0,
  "data": true,
  "msg": ""
}
```

### 4.3 校验邮箱验证码

请求：

```json
{
  "email": "user@example.com",
  "scene": "REGISTER",
  "code": "123456"
}
```

响应：

```json
{
  "code": 0,
  "data": true,
  "msg": ""
}
```

### 4.4 邮箱注册

请求：

```json
{
  "email": "user@example.com",
  "code": "123456",
  "password": "abc123456",
  "agreeTerms": true,
  "inviteCode": ""
}
```

响应复用登录响应：

```json
{
  "code": 0,
  "data": {
    "userId": 1001,
    "accessToken": "xxx",
    "refreshToken": "yyy",
    "expiresTime": 1710000000000,
    "openid": null
  },
  "msg": ""
}
```

## 5. 数据库设计

### 5.1 租户 SQL 说明

当前项目租户模块会自动注入租户 SQL，因此：

- Mapper 查询不需要手写 `tenant_id = xxx` 条件。
- 新增表仍然必须包含 `tenant_id` 字段，满足项目多租户规范。
- 唯一索引需要结合租户隔离设计，避免不同租户之间邮箱互相冲突。
- 如果产品定义会员账号全平台唯一，则邮箱唯一索引可以不带 `tenant_id`。
- 如果产品定义会员账号租户内唯一，则邮箱唯一索引必须带 `tenant_id`。

本方案推荐：

```text
邮箱在租户内唯一
```

原因：当前系统存在 SaaS 租户能力，同一个邮箱可能属于不同租户的不同会员。

### 5.2 修改 member_user

新增字段：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `email` | varchar(255) | 邮箱 |
| `email_verified` | bit | 邮箱是否已验证 |
| `email_bind_time` | datetime | 邮箱绑定时间 |

推荐 SQL：

```sql
ALTER TABLE member_user
    ADD COLUMN email varchar(255) DEFAULT NULL COMMENT '邮箱' AFTER mobile,
    ADD COLUMN email_verified bit(1) NOT NULL DEFAULT b'0' COMMENT '邮箱是否已验证' AFTER email,
    ADD COLUMN email_bind_time datetime DEFAULT NULL COMMENT '邮箱绑定时间' AFTER email_verified;

CREATE UNIQUE INDEX uk_tenant_email_deleted ON member_user(tenant_id, email, deleted);
```

如果 `member_user` 当前没有 `tenant_id` 字段，需要先确认是否被租户插件排除；如果会员表参与租户隔离，应补齐 `tenant_id`。

### 5.3 新增 member_email_code

```sql
CREATE TABLE member_email_code (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    email varchar(255) NOT NULL COMMENT '邮箱',
    code varchar(16) NOT NULL COMMENT '验证码',
    scene varchar(64) NOT NULL COMMENT '发送场景',
    used bit(1) NOT NULL DEFAULT b'0' COMMENT '是否使用',
    used_time datetime DEFAULT NULL COMMENT '使用时间',
    used_ip varchar(50) DEFAULT NULL COMMENT '使用 IP',
    create_ip varchar(50) DEFAULT NULL COMMENT '创建 IP',
    today_index int NOT NULL DEFAULT 1 COMMENT '当天第几次发送',
    expires_time datetime NOT NULL COMMENT '过期时间',
    creator varchar(64) DEFAULT '' COMMENT '创建者',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater varchar(64) DEFAULT '' COMMENT '更新者',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant_email_scene_create_time (tenant_id, email, scene, create_time),
    KEY idx_tenant_email_scene_code (tenant_id, email, scene, code),
    KEY idx_tenant_create_ip_create_time (tenant_id, create_ip, create_time)
) COMMENT='会员邮箱验证码';
```

注意：

- `tenant_id` 必须保留，由租户模块自动注入。
- 查询时不需要手写租户条件。
- 索引中保留 `tenant_id` 是为了匹配真实 SQL 过滤和提高查询性能。

### 5.4 邮件模板 SQL

新增邮件模板编码：

```text
member_email_register_code
```

模板参数：

| 参数 | 说明 |
| ---- | ---- |
| `code` | 验证码 |
| `expireMinutes` | 过期分钟数 |
| `productName` | 产品名称 |

模板标题：

```text
【AIGC 平台】邮箱注册验证码
```

模板内容：

```text
您好，您的邮箱注册验证码为：${code}，${expireMinutes} 分钟内有效。如非本人操作，请忽略本邮件。
```

## 6. 枚举和错误码设计

### 6.1 邮箱验证码场景枚举

建议新增：

```java
public enum MemberEmailCodeSceneEnum {

    REGISTER("REGISTER", "邮箱注册"),
    LOGIN("LOGIN", "邮箱验证码登录"),
    RESET_PASSWORD("RESET_PASSWORD", "邮箱找回密码"),
    BIND_EMAIL("BIND_EMAIL", "绑定邮箱"),
    CHANGE_EMAIL("CHANGE_EMAIL", "换绑邮箱");

}
```

第一阶段只强制使用：

```text
REGISTER
```

### 6.2 错误码

建议新增错误码：

| 错误码常量 | 说明 |
| ---- | ---- |
| `MEMBER_EMAIL_EXISTS` | 邮箱已被使用 |
| `MEMBER_EMAIL_NOT_EXISTS` | 邮箱不存在 |
| `MEMBER_EMAIL_CODE_NOT_FOUND` | 邮箱验证码不存在 |
| `MEMBER_EMAIL_CODE_EXPIRED` | 邮箱验证码已过期 |
| `MEMBER_EMAIL_CODE_USED` | 邮箱验证码已使用 |
| `MEMBER_EMAIL_CODE_NOT_CORRECT` | 邮箱验证码不正确 |
| `MEMBER_EMAIL_CODE_SEND_TOO_FAST` | 邮箱验证码发送过于频繁 |
| `MEMBER_EMAIL_CODE_SEND_TOO_MANY` | 邮箱验证码发送次数过多 |
| `MEMBER_EMAIL_FORMAT_INVALID` | 邮箱格式不正确 |
| `MEMBER_EMAIL_NOT_VERIFIED` | 邮箱未验证 |
| `MEMBER_AUTH_AGREE_TERMS_REQUIRED` | 请阅读并同意用户协议 |

## 7. 后端代码结构设计

### 7.1 VO 设计

路径：

```text
yudao-module-member-server
  └── src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/vo
```

新增：

```text
AppAuthEmailCodeSendReqVO.java
AppAuthEmailCodeValidateReqVO.java
AppAuthEmailRegisterReqVO.java
```

发送邮箱验证码 VO：

```java
public class AppAuthEmailCodeSendReqVO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "发送场景不能为空")
    private String scene;

}
```

校验邮箱验证码 VO：

```java
public class AppAuthEmailCodeValidateReqVO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "发送场景不能为空")
    private String scene;

}
```

邮箱注册 VO：

```java
public class AppAuthEmailRegisterReqVO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "密码不能为空")
    @Length(min = 8, max = 32, message = "密码长度为 8-32 位")
    private String password;

    @NotNull(message = "请阅读并同意用户协议")
    @AssertTrue(message = "请阅读并同意用户协议")
    private Boolean agreeTerms;

    private String inviteCode;

}
```

### 7.2 DO 设计

新增：

```text
MemberEmailCodeDO.java
```

路径：

```text
cn.iocoder.yudao.module.member.dal.dataobject.auth
```

核心字段：

```java
@TableName("member_email_code")
@KeySequence("member_email_code_seq")
public class MemberEmailCodeDO extends BaseDO {

    @TableId
    private Long id;

    private String email;

    private String code;

    private String scene;

    private Boolean used;

    private LocalDateTime usedTime;

    private String usedIp;

    private String createIp;

    private Integer todayIndex;

    private LocalDateTime expiresTime;

}
```

说明：

- DO 不需要手写 `tenantId` 字段时，按项目现有 BaseDO / Tenant 基建规范处理。
- 如果项目其他多租户业务 DO 显式声明 `tenantId`，则该 DO 也保持一致。

### 7.3 Mapper 设计

新增：

```text
MemberEmailCodeMapper.java
```

路径：

```text
cn.iocoder.yudao.module.member.dal.mysql.auth
```

方法建议：

```java
default MemberEmailCodeDO selectLastByEmailAndScene(String email, String scene) {
    return selectOne(new LambdaQueryWrapperX<MemberEmailCodeDO>()
            .eq(MemberEmailCodeDO::getEmail, email)
            .eq(MemberEmailCodeDO::getScene, scene)
            .orderByDesc(MemberEmailCodeDO::getId)
            .last("LIMIT 1"));
}

default Long selectCountByEmailAndSceneToday(String email, String scene, LocalDateTime beginTime) {
    return selectCount(new LambdaQueryWrapperX<MemberEmailCodeDO>()
            .eq(MemberEmailCodeDO::getEmail, email)
            .eq(MemberEmailCodeDO::getScene, scene)
            .ge(MemberEmailCodeDO::getCreateTime, beginTime));
}

default MemberEmailCodeDO selectUnusedCode(String email, String scene, String code) {
    return selectOne(new LambdaQueryWrapperX<MemberEmailCodeDO>()
            .eq(MemberEmailCodeDO::getEmail, email)
            .eq(MemberEmailCodeDO::getScene, scene)
            .eq(MemberEmailCodeDO::getCode, code)
            .eq(MemberEmailCodeDO::getUsed, false)
            .orderByDesc(MemberEmailCodeDO::getId)
            .last("LIMIT 1"));
}
```

注意：

- 因为租户模块自动注入 SQL，Mapper 不手写租户条件。
- 查询字段要建立包含 `tenant_id` 的索引，保证自动注入租户 SQL 后性能可控。

## 8. Service 设计

### 8.1 MemberEmailCodeService

新增接口：

```java
public interface MemberEmailCodeService {

    void sendEmailCode(String email, String scene, String createIp);

    void validateEmailCode(String email, String scene, String code);

    void useEmailCode(String email, String scene, String code, String usedIp);

}
```

### 8.2 发送验证码流程

```text
校验邮箱格式
  ↓
校验场景
  ↓
根据场景校验邮箱是否已存在 / 是否不存在
  ↓
校验发送频率
  ↓
校验当天发送次数
  ↓
生成 6 位验证码
  ↓
写入 member_email_code
  ↓
调用 MailSendApi 发送邮件
```

场景校验：

| 场景 | 校验 |
| ---- | ---- |
| `REGISTER` | 邮箱不能已存在 |
| `LOGIN` | 邮箱必须存在 |
| `RESET_PASSWORD` | 邮箱必须存在 |
| `BIND_EMAIL` | 邮箱不能已存在 |
| `CHANGE_EMAIL` | 邮箱不能已存在 |

频控建议：

| 规则 | 建议 |
| ---- | ---- |
| 同邮箱同场景发送间隔 | 60 秒 |
| 同邮箱同场景每日上限 | 10 次 |
| 同 IP 每小时上限 | 30 次 |
| 验证码有效期 | 10 分钟 |
| 验证码长度 | 6 位数字 |

### 8.3 校验验证码流程

校验不消费，用于前端单独校验场景：

```text
查询最新未使用验证码
  ↓
校验验证码是否存在
  ↓
校验是否过期
  ↓
校验 code 是否一致
```

### 8.4 消费验证码流程

注册、登录、重置密码时必须消费验证码：

```text
查询最新未使用验证码
  ↓
校验存在
  ↓
校验未过期
  ↓
校验 code 一致
  ↓
使用 id + used = false 条件更新
  ↓
记录 usedTime 和 usedIp
```

条件更新建议：

```sql
UPDATE member_email_code
SET used = 1,
    used_time = NOW(),
    used_ip = ?
WHERE id = ?
  AND used = 0
```

## 9. MemberUserService 改造

### 9.1 MemberUserDO 增加字段

新增字段：

```java
private String email;

private Boolean emailVerified;

private LocalDateTime emailBindTime;
```

### 9.2 MemberUserMapper 增加查询

```java
default MemberUserDO selectByEmail(String email) {
    return selectOne(MemberUserDO::getEmail, email);
}
```

说明：租户 SQL 自动注入，方法内不需要补 `tenant_id` 条件。

### 9.3 MemberUserService 增加方法

```java
MemberUserDO getUserByEmail(String email);

Long createUserByEmail(String email, String password, String registerIp, Integer terminal);

void updateEmailVerified(Long userId, String email);
```

### 9.4 创建邮箱用户逻辑

```text
校验邮箱不存在
  ↓
BCrypt 加密密码
  ↓
创建 member_user
  ↓
设置 email
  ↓
设置 emailVerified = true
  ↓
设置 emailBindTime = now
  ↓
设置 status = ENABLE
  ↓
设置 registerIp / registerTerminal
  ↓
设置 nickname
  ↓
发送用户创建 MQ 消息
```

昵称建议：

```text
邮箱前缀 + 随机后缀
```

示例：

```text
user@example.com -> user_4821
```

## 10. MemberAuthService 改造

### 10.1 新增方法

```java
void sendEmailCode(AppAuthEmailCodeSendReqVO reqVO);

void validateEmailCode(AppAuthEmailCodeValidateReqVO reqVO);

AppAuthLoginRespVO emailRegister(AppAuthEmailRegisterReqVO reqVO);
```

### 10.2 邮箱注册流程

```text
接收 email、code、password、agreeTerms
  ↓
校验 agreeTerms = true
  ↓
校验邮箱未注册
  ↓
消费 REGISTER 场景邮箱验证码
  ↓
创建邮箱会员用户
  ↓
创建 OAuth2 Token
  ↓
记录登录日志
  ↓
返回 AppAuthLoginRespVO
```

伪代码：

```java
@Override
public AppAuthLoginRespVO emailRegister(AppAuthEmailRegisterReqVO reqVO) {
    if (Boolean.FALSE.equals(reqVO.getAgreeTerms())) {
        throw exception(MEMBER_AUTH_AGREE_TERMS_REQUIRED);
    }

    MemberUserDO user = memberUserService.getUserByEmail(reqVO.getEmail());
    if (user != null) {
        throw exception(MEMBER_EMAIL_EXISTS);
    }

    emailCodeService.useEmailCode(
            reqVO.getEmail(),
            MemberEmailCodeSceneEnum.REGISTER.getScene(),
            reqVO.getCode(),
            getClientIP());

    Long userId = memberUserService.createUserByEmail(
            reqVO.getEmail(),
            reqVO.getPassword(),
            getClientIP(),
            getTerminal());

    return createTokenAfterLoginSuccess(userId, reqVO.getEmail(), LoginLogTypeEnum.LOGIN_EMAIL);
}
```

## 11. Controller 设计

在 `AppAuthController` 增加：

```java
@PostMapping("/send-email-code")
@Operation(summary = "发送邮箱验证码")
@PermitAll
public CommonResult<Boolean> sendEmailCode(@Valid @RequestBody AppAuthEmailCodeSendReqVO reqVO) {
    authService.sendEmailCode(reqVO);
    return success(true);
}

@PostMapping("/validate-email-code")
@Operation(summary = "校验邮箱验证码")
@PermitAll
public CommonResult<Boolean> validateEmailCode(@Valid @RequestBody AppAuthEmailCodeValidateReqVO reqVO) {
    authService.validateEmailCode(reqVO);
    return success(true);
}

@PostMapping("/email-register")
@Operation(summary = "邮箱注册")
@PermitAll
public CommonResult<AppAuthLoginRespVO> emailRegister(@Valid @RequestBody AppAuthEmailRegisterReqVO reqVO) {
    return success(authService.emailRegister(reqVO));
}
```

## 12. MailSendApi 扩展

### 12.1 当前问题

当前 `MailSendApi` 已支持发送给已存在用户，但邮箱注册时用户尚未创建，不能依赖 `memberUserId` 获取邮箱。

因此需要扩展一个“直接按邮箱发送模板邮件”的 RPC 方法。

### 12.2 建议新增方法

```java
Long sendSingleMailToEmail(String email, String templateCode, Map<String, Object> templateParams);
```

### 12.3 发送流程

```text
校验 email
  ↓
查询邮件模板
  ↓
查询邮件账号
  ↓
创建 system_mail_log
  ↓
MQ 异步发送
  ↓
返回 mailLogId
```

### 12.4 调用示例

```java
Map<String, Object> params = Map.of(
    "code", code,
    "expireMinutes", 10,
    "productName", "AIGC 平台"
);
mailSendApi.sendSingleMailToEmail(email, "member_email_register_code", params);
```

## 13. 前端配合方案

### 13.1 注册入口

用户端登录注册弹窗新增：

```text
邮箱注册
```

### 13.2 注册表单字段

| 字段 | 说明 |
| ---- | ---- |
| `email` | 邮箱 |
| `code` | 邮箱验证码 |
| `password` | 密码 |
| `confirmPassword` | 确认密码 |
| `agreeTerms` | 用户协议 |

### 13.3 前端调用流程

```text
输入邮箱
  ↓
点击发送验证码
  ↓
调用 send-email-code
  ↓
输入验证码和密码
  ↓
调用 email-register
  ↓
保存 token
  ↓
拉取用户信息
  ↓
拉取钱包信息
  ↓
进入工作台
```

## 14. 安全设计

### 14.1 防枚举

发送验证码阶段不建议直接提示：

```text
邮箱已注册
```

推荐提示：

```text
如果邮箱可用，验证码将发送到该邮箱
```

注册提交阶段可以明确提示：

```text
邮箱已被使用
```

### 14.2 频率限制

至少实现：

- 同邮箱 60 秒内不能重复发送
- 同邮箱每日最多 10 次
- 同 IP 每小时最多 30 次
- 邮箱验证码 10 分钟过期

### 14.3 验证码安全

- 验证码不在日志打印
- 邮件日志展示时对验证码参数脱敏
- 验证码消费后不能复用
- 同一个验证码并发消费只能成功一次
- 发送验证码接口可按风险接入图形验证码

### 14.4 密码安全

- 密码必须 BCrypt 加密
- 不允许明文日志
- 密码复杂度前后端都校验
- 注册成功后不返回密码相关字段

## 15. 兼容现有手机号体系

邮箱注册不影响现有手机号登录。

| 账号形态 | 是否允许 |
| ---- | ---- |
| 只有手机号 | 允许 |
| 只有邮箱 | 允许 |
| 手机号 + 邮箱 | 允许 |
| 同一邮箱同租户多个用户 | 不允许 |
| 同一手机号同租户多个用户 | 不允许 |

第一阶段支持：

```text
手机号短信登录即注册
邮箱验证码注册
手机号密码登录
```

当前已补齐支持：

```text
邮箱密码登录
邮箱验证码登录
邮箱找回密码
邮箱绑定 / 换绑
```

## 16. 测试与验收

### 16.1 发送邮箱验证码验收

- 邮箱格式不正确时返回参数错误
- 注册场景下已注册邮箱不能继续注册
- 60 秒内重复发送被拦截
- 超过每日发送次数被拦截
- 发送成功后 `member_email_code` 有记录
- 发送成功后 `system_mail_log` 有记录
- 邮件内容包含验证码和有效期
- 不需要手写租户条件也能按当前租户隔离数据

### 16.2 邮箱注册验收

- 验证码错误不能注册
- 验证码过期不能注册
- 验证码已使用不能重复注册
- 邮箱已注册不能重复注册
- 密码不符合规则不能注册
- 未同意协议不能注册
- 注册成功后 `member_user.email` 正确
- 注册成功后 `email_verified = true`
- 注册成功后返回 accessToken 和 refreshToken
- 注册成功后可调用 `/app-api/member/user/get`
- 注册成功后不能再次使用同一验证码注册
- 不同租户下邮箱唯一性符合产品定义

### 16.3 邮箱登录与账号安全验收

- 用户可通过邮箱和密码登录
- 用户可通过 `LOGIN` 场景邮箱验证码登录
- 邮箱验证码登录失败时，禁用账号和邮箱未验证账号不会消耗验证码
- 用户可通过 `RESET_PASSWORD` 场景邮箱验证码重置密码
- 未绑定邮箱用户可通过 `BIND_EMAIL` 场景验证码绑定邮箱
- 已绑定邮箱用户可通过 `CHANGE_EMAIL` 场景验证码换绑邮箱
- `BIND_EMAIL` 与 `CHANGE_EMAIL` 场景不能混用
- 同一个邮箱验证码并发消费只能成功一次

### 16.4 工程验收

- `yudao-module-member-api` 可正常编译
- `yudao-module-member-server` 可正常编译
- `yudao-module-system-api` 可正常编译
- `yudao-module-system-server` 可正常编译
- 用户端接口文档能展示新增接口
- `/app-api/member/**` 能访问新增接口
- 新增 SQL 可重复执行或按项目迁移规范执行
- 已通过 `mvn -pl yudao-module-member/yudao-module-member-server -am -DskipTests compile` 编译校验

## 17. 最小落地版本

最小生产可用版本只做 3 个接口：

```text
POST /app-api/member/auth/send-email-code
POST /app-api/member/auth/validate-email-code
POST /app-api/member/auth/email-register
```

最小数据改造：

```text
member_user 增加 email、email_verified、email_bind_time
新增 member_email_code
新增 system_mail_template 邮箱注册验证码模板
```

最小用户体验：

```text
邮箱注册 → 发验证码 → 填验证码和密码 → 注册成功自动登录
```

该版本不会影响现有手机号登录和短信登录即注册能力，可以快速补齐 AIGC 用户端注册闭环。
