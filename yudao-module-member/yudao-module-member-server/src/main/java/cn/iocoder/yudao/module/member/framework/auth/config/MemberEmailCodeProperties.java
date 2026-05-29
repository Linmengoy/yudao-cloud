package cn.iocoder.yudao.module.member.framework.auth.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "yudao.member.email-code")
@Validated
@Data
public class MemberEmailCodeProperties {

    @NotNull(message = "邮箱验证码过期时间不能为空")
    private Duration expireTime = Duration.ofMinutes(10);

    @NotNull(message = "邮箱验证码发送间隔不能为空")
    private Duration sendInterval = Duration.ofSeconds(60);

    @NotNull(message = "邮箱验证码每日发送上限不能为空")
    private Integer emailDailyLimit = 10;

    @NotNull(message = "邮箱验证码 IP 小时发送上限不能为空")
    private Integer ipHourlyLimit = 30;

    @NotNull(message = "邮箱验证码 IP 小时发送窗口不能为空")
    private Duration ipHourlyWindow = Duration.ofHours(1);

    @NotNull(message = "邮箱验证码产品名称不能为空")
    private String productName = "栖地平台";

}
