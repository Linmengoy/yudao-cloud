package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "用户端 - AIGC 画布节点运行批量同步 Response VO")
@Data
public class AigcCanvasNodeRunBatchSyncRespVO {

    @Schema(description = "项目编号")
    private Long projectId;

    @Schema(description = "节点同步结果")
    private List<AigcCanvasNodeRunRespVO> results;

}
