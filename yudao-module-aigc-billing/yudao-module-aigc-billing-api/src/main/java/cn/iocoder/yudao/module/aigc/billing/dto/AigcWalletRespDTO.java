package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 钱包响应 DTO")
@Data
public class AigcWalletRespDTO {

    @Schema(description = "钱包编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long userId;

    @Schema(description = "可用积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.000000")
    private BigDecimal balance;

    @Schema(description = "冻结积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "20.000000")
    private BigDecimal frozenBalance;

    @Schema(description = "累计充值积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "200.000000")
    private BigDecimal totalRecharge;

    @Schema(description = "累计赠送积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "50.000000")
    private BigDecimal totalGift;

    @Schema(description = "累计消费积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "80.000000")
    private BigDecimal totalConsume;

    @Schema(description = "累计退款积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    private BigDecimal totalRefund;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "最近交易时间")
    private LocalDateTime lastTransTime;

}
