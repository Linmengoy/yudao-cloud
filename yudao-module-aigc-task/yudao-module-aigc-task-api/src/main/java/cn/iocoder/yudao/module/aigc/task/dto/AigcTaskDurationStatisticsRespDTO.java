package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 任务耗时统计 Response DTO")
@Data
public class AigcTaskDurationStatisticsRespDTO {

    @Schema(description = "样本数量", example = "50")
    private Long sampleCount;

    @Schema(description = "平均耗时毫秒", example = "60000")
    private Long avgDurationMillis;

    @Schema(description = "P95 耗时毫秒", example = "120000")
    private Long p95DurationMillis;

}
