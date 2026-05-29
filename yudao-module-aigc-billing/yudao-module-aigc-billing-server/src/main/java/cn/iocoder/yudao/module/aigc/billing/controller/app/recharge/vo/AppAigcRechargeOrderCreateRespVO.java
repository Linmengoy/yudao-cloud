package cn.iocoder.yudao.module.aigc.billing.controller.app.recharge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户端 - AIGC 充值订单创建 Response VO")
@Data
public class AppAigcRechargeOrderCreateRespVO {

    @Schema(description = "AIGC 充值订单 ID", example = "1024")
    private Long rechargeOrderId;

    @Schema(description = "AIGC 充值订单号", example = "R202605260001")
    private String rechargeNo;

    @Schema(description = "Pay 支付订单 ID", example = "2048")
    private Long payOrderId;

    @Schema(description = "Pay 支付订单号", example = "P202605260001")
    private String payOrderNo;

    @Schema(description = "Pay 应用 ID", example = "1")
    private Long payAppId;

    @Schema(description = "支付金额，单位：分", example = "4990")
    private Integer payAmount;

    @Schema(description = "充值积分", example = "500")
    private BigDecimal pointAmount;

    @Schema(description = "赠送积分", example = "100")
    private BigDecimal giftAmount;

    @Schema(description = "到账积分总数", example = "600")
    private BigDecimal totalPointAmount;

}
