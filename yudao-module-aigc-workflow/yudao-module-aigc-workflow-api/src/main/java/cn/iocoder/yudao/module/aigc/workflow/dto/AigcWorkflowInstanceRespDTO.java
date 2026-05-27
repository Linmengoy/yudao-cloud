package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 工作流实例 Response DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowInstanceRespDTO {

    @Schema(description = "实例编号", example = "1024")
    private Long id;

    @Schema(description = "实例流水号", example = "WF202605270001")
    private String instanceNo;

    @Schema(description = "工作流编号", example = "2048")
    private Long workflowId;

    @Schema(description = "工作流版本编号", example = "4096")
    private Long workflowVersionId;

    @Schema(description = "来源模板编号", example = "8192")
    private Long templateId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "状态", example = "RUNNING")
    private String status;

    @Schema(description = "输入数据 JSON")
    private String inputData;

    @Schema(description = "输出数据 JSON")
    private String outputData;

    @Schema(description = "主任务编号", example = "2048")
    private Long mainTaskId;

    @Schema(description = "冻结记录编号", example = "4096")
    private Long freezeId;

    @Schema(description = "预估费用", example = "100")
    private Long estimateAmount;

    @Schema(description = "实际费用", example = "80")
    private Long actualAmount;

    @Schema(description = "进度", example = "80")
    private Integer progress;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "失败详情")
    private String failMessage;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime finishTime;

}
