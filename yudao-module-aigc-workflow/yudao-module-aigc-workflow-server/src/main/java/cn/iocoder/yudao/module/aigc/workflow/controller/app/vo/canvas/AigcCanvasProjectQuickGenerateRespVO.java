package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户端 - AIGC 画布项目快速生成 Response VO")
@Data
@Accessors(chain = true)
public class AigcCanvasProjectQuickGenerateRespVO {

    @Schema(description = "项目编号")
    private Long projectId;

    @Schema(description = "节点编号")
    private String nodeId;

    @Schema(description = "任务编号")
    private Long taskId;

    @Schema(description = "生成记录编号")
    private Long generateRecordId;

    @Schema(description = "生成流水号")
    private String generateNo;

    @Schema(description = "任务状态")
    private String status;

}
