package cn.iocoder.yudao.module.aigc.billing.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "yudao.aigc.billing.pay")
@Validated
@Data
public class AigcBillingPayProperties {

    @NotNull(message = "支付应用编号不能为空")
    private Long appId;

    @NotBlank(message = "支付应用标识不能为空")
    private String appKey;

    @Min(value = 1, message = "支付过期时间必须大于 0 分钟")
    private Integer expireMinutes = 30;

}
