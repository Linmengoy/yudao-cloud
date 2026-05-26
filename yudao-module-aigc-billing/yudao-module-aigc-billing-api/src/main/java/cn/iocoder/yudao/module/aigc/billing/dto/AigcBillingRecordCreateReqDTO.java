package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 创建计费流水请求 DTO")
@Data
public class AigcBillingRecordCreateReqDTO {

    @Schema(description = "钱包编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "钱包编号不能为空")
    private Long walletId;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TASK_GENERATE")
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "T202605260001")
    @NotBlank(message = "业务编号不能为空")
    private String bizId;

    @Schema(description = "流水类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CONSUME")
    @NotBlank(message = "流水类型不能为空")
    private String recordType;

    @Schema(description = "流水标题", example = "图片生成扣费")
    private String title;

    @Schema(description = "变动积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "-10.000000")
    @NotNull(message = "变动积分不能为空")
    private BigDecimal amount;

    @Schema(description = "冻结记录编号", example = "300")
    private Long freezeId;

    @Schema(description = "任务编号", example = "400")
    private Long taskId;

    @Schema(description = "模型编号", example = "500")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "600")
    private Long providerId;

    @Schema(description = "货币类型", example = "POINT")
    private String currencyType;

    @Schema(description = "价格快照 JSON")
    private String priceSnapshot;

    @Schema(description = "扩展信息 JSON")
    private String extraInfo;

}
