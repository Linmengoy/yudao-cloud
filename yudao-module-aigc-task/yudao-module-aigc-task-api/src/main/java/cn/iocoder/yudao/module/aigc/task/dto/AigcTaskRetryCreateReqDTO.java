package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 任务重试创建 Request DTO")
@Data
public class AigcTaskRetryCreateReqDTO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "平台任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "重试类型", example = "AUTO")
    private String retryType;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "操作人编号")
    private Long operatorId;

}
