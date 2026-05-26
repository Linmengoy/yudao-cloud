package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 冻结积分响应 DTO")
@Data
public class AigcBillingFreezeRespDTO {

    @Schema(description = "冻结记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "冻结编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "F202605260001")
    private String freezeNo;

    @Schema(description = "钱包编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long walletId;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long userId;

    @Schema(description = "冻结积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    private BigDecimal amount;

    @Schema(description = "冻结状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "FROZEN")
    private String status;

}
