package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 确认扣费请求 DTO")
@Data
public class AigcBillingConfirmReqDTO {

    @Schema(description = "冻结记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "冻结记录编号不能为空")
    private Long freezeId;

    @Schema(description = "任务编号", example = "1001")
    private Long taskId;

    @Schema(description = "任务号", example = "T202605260001")
    private String taskNo;

    @Schema(description = "实际扣费积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    @NotNull(message = "实际扣费积分不能为空")
    @DecimalMin(value = "0.000001", message = "实际扣费积分必须大于 0")
    private BigDecimal actualAmount;

    @Schema(description = "模型编号", example = "100")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "200")
    private Long providerId;

    @Schema(description = "价格快照 JSON")
    private String priceSnapshot;

}
