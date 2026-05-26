package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 任务日志创建 Request DTO")
@Data
public class AigcTaskLogCreateReqDTO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "平台任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "变更前状态")
    private String fromStatus;

    @Schema(description = "变更后状态")
    private String toStatus;

    @Schema(description = "操作动作", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作动作不能为空")
    private String action;

    @Schema(description = "日志消息")
    private String message;

    @Schema(description = "操作来源类型")
    private String operatorType;

    @Schema(description = "操作人编号")
    private Long operatorId;

    @Schema(description = "扩展信息 JSON")
    private String extraInfo;

}
