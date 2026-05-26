package cn.iocoder.yudao.module.aigc.billing.controller.admin.wallet.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - AIGC 钱包金额操作 Request VO")
@Data
public class AigcWalletAmountReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "积分金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    @NotNull(message = "积分金额不能为空")
    private BigDecimal amount;

    @Schema(description = "备注", example = "运营赠送")
    private String remark;

}
