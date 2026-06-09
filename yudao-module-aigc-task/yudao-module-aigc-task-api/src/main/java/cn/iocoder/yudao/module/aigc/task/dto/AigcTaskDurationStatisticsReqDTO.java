package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 任务耗时统计 Request DTO")
@Data
public class AigcTaskDurationStatisticsReqDTO {

    @Schema(description = "渠道商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "渠道商 ID 不能为空")
    private Long providerId;

    @Schema(description = "模型 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "模型 ID 不能为空")
    private Long modelId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "IMAGE_GENERATE")
    @NotBlank(message = "模型能力不能为空")
    private String capability;

    @Schema(description = "样本数量，按最近成功任务倒序取样", example = "50")
    private Integer sampleSize;

}
