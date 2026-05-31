package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布节点运行同步 Request VO")
@Data
public class AigcCanvasNodeRunSyncReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目编号不能为空")
    private Long projectId;

    @Schema(description = "节点编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点编号不能为空")
    private String nodeId;

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务编号不能为空")
    private Long taskId;

    @Schema(description = "客户端基于版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户端基于版本不能为空")
    private Long baseVersion;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

}
