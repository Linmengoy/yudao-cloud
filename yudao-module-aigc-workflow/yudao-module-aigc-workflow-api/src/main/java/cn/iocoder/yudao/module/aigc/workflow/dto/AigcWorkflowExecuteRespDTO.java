package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 工作流执行 Response DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowExecuteRespDTO {

    @Schema(description = "工作流实例编号", example = "1024")
    private Long instanceId;

    @Schema(description = "工作流实例流水号", example = "WF202605270001")
    private String instanceNo;

    @Schema(description = "主任务编号", example = "2048")
    private Long mainTaskId;

    @Schema(description = "状态", example = "RUNNING")
    private String status;

}
