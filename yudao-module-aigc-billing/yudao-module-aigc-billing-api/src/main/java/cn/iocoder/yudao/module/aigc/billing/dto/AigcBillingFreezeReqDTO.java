package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 冻结积分请求 DTO")
@Data
public class AigcBillingFreezeReqDTO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TASK_GENERATE")
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "REQ202605260001")
    @NotBlank(message = "业务编号不能为空")
    private String bizId;

    @Schema(description = "任务编号", example = "1001")
    private Long taskId;

    @Schema(description = "任务号", example = "T202605260001")
    private String taskNo;

    @Schema(description = "冻结积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.000000")
    @NotNull(message = "冻结积分不能为空")
    @DecimalMin(value = "0.000001", message = "冻结积分必须大于 0")
    private BigDecimal amount;

    @Schema(description = "冻结过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "流水标题", example = "图片生成冻结")
    private String title;

    @Schema(description = "价格快照 JSON")
    private String priceSnapshot;

}
