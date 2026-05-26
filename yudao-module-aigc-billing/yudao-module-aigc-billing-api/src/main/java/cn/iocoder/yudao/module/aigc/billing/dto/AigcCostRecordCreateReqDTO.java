package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 创建成本记录请求 DTO")
@Data
public class AigcCostRecordCreateReqDTO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "任务号", example = "T202605260001")
    private String taskNo;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "模型编号", example = "100")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "200")
    private Long providerId;

    @Schema(description = "模型能力", example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "计费单位", example = "PER_TASK")
    private String billingUnit;

    @Schema(description = "实际用量", example = "1.000000")
    private BigDecimal usageAmount;

    @Schema(description = "平台成本", requiredMode = Schema.RequiredMode.REQUIRED, example = "5.000000")
    @NotNull(message = "平台成本不能为空")
    private BigDecimal costAmount;

    @Schema(description = "用户销售价", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    @NotNull(message = "用户销售价不能为空")
    private BigDecimal saleAmount;

    @Schema(description = "货币类型", example = "POINT")
    private String currencyType;

    @Schema(description = "用量快照 JSON")
    private String usageSnapshot;

    @Schema(description = "价格快照 JSON")
    private String priceSnapshot;

}
