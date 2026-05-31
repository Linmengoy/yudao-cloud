package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "用户端 - AIGC 画布操作同步 Response VO")
@Data
public class AigcCanvasOperationSyncRespVO {

    @Schema(description = "同步模式 delta/snapshot")
    private String mode;
    @Schema(description = "起始版本")
    private Long fromVersion;
    @Schema(description = "结束版本")
    private Long toVersion;
    @Schema(description = "操作列表")
    private List<AigcCanvasOperationRespVO> operations;
    @Schema(description = "快照")
    private AigcCanvasSnapshotRespVO snapshot;

}
