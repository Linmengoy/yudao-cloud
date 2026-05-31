package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布节点运行 Response VO")
@Data
public class AigcCanvasNodeRunRespVO {

    @Schema(description = "任务编号")
    private Long taskId;

    @Schema(description = "生成记录编号")
    private Long generateRecordId;

    @Schema(description = "生成流水号")
    private String generateNo;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "状态补丁操作")
    private AigcCanvasOperationRespVO operation;

}
