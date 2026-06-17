package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "用户端 - AIGC 画布节点运行批量同步 Request VO")
@Data
public class AigcCanvasNodeRunBatchSyncReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @Schema(description = "客户端基于版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户端基于版本不能为空")
    private Long baseVersion;

    @Schema(description = "节点同步列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "节点同步列表不能为空")
    @Valid
    private List<AigcCanvasNodeRunSyncReqVO> nodes;

}
