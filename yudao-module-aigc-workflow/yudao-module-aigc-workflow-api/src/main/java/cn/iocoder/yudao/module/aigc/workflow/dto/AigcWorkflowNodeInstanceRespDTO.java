package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 工作流节点实例 Response DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowNodeInstanceRespDTO {

    @Schema(description = "节点实例编号", example = "1024")
    private Long id;

    @Schema(description = "工作流实例编号", example = "2048")
    private Long workflowInstanceId;

    @Schema(description = "节点定义编号", example = "4096")
    private Long nodeId;

    @Schema(description = "节点键")
    private String nodeKey;

    @Schema(description = "节点类型", example = "IMAGE_GENERATE")
    private String nodeType;

    @Schema(description = "状态", example = "SUCCESS")
    private String status;

    @Schema(description = "任务编号", example = "8192")
    private Long taskId;

    @Schema(description = "生成记录编号", example = "16384")
    private Long genRecordId;

    @Schema(description = "节点入参 JSON")
    private String inputData;

    @Schema(description = "节点出参 JSON")
    private String outputData;

    @Schema(description = "输出资产 JSON")
    private String assetIds;

    @Schema(description = "重试次数", example = "1")
    private Integer retryCount;

    @Schema(description = "最大重试次数", example = "3")
    private Integer maxRetryCount;

    @Schema(description = "节点费用", example = "10")
    private Long costAmount;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime finishTime;

}
