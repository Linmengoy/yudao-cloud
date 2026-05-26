package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 充值支付成功通知 DTO")
@Data
public class AigcRechargeNotifyReqDTO {

    @Schema(description = "充值订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "R202605260001")
    @NotBlank(message = "充值订单号不能为空")
    private String rechargeNo;

    @Schema(description = "支付订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "支付订单编号不能为空")
    private Long payOrderId;

    @Schema(description = "支付订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "P202605260001")
    @NotBlank(message = "支付订单号不能为空")
    private String payOrderNo;

    @Schema(description = "支付渠道编码", example = "wx_pub")
    private String payChannelCode;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

}
