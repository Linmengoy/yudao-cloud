package cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 任务统计 Response VO")
@Data
public class AigcTaskStatisticsRespVO {

    @Schema(description = "任务总数", example = "100")
    private Long totalCount;

    @Schema(description = "成功任务数", example = "80")
    private Long successCount;

    @Schema(description = "失败任务数", example = "10")
    private Long failedCount;

    @Schema(description = "退款中任务数", example = "2")
    private Long refundingCount;

    @Schema(description = "队列积压任务数", example = "8")
    private Long backlogCount;

    @Schema(description = "超时任务数", example = "1")
    private Long timeoutCount;

    @Schema(description = "成功率", example = "0.8")
    private Double successRate;

    @Schema(description = "失败率", example = "0.1")
    private Double failedRate;

    @Schema(description = "平均耗时毫秒", example = "60000")
    private Long avgDurationMillis;

}
