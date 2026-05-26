package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 任务状态更新 Request DTO")
@Data
public class AigcTaskStatusUpdateReqDTO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "第三方任务编号")
    private String externalTaskId;

    @Schema(description = "输出资产编号")
    private Long outputAssetId;

    @Schema(description = "输出资产类型")
    private String outputAssetType;

    @Schema(description = "文本输出")
    private String outputText;

    @Schema(description = "结构化输出 JSON")
    private String outputData;

    @Schema(description = "失败错误码")
    private String failCode;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "进度", example = "80")
    private Integer progress;

}
