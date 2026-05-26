package cn.iocoder.yudao.module.aigc.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 释放冻结请求 DTO")
@Data
public class AigcBillingReleaseReqDTO {

    @Schema(description = "冻结记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "冻结记录编号不能为空")
    private Long freezeId;

    @Schema(description = "任务编号", example = "1001")
    private Long taskId;

    @Schema(description = "任务号", example = "T202605260001")
    private String taskNo;

    @Schema(description = "释放原因", example = "任务失败")
    private String reason;

}
