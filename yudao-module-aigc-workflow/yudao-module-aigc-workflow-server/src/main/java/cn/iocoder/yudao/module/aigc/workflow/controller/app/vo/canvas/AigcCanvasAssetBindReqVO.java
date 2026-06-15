package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布资产绑定 Request VO")
@Data
public class AigcCanvasAssetBindReqVO {

    @Schema(description = "项目编号")
    private Long projectId;

    @Schema(description = "节点编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nodeId;

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "资产版本编号")
    private Long assetVersionId;

    @Schema(description = "预览地址")
    private String previewUrl;

    @Schema(description = "用途", example = "source")
    private String usageType;

    @Schema(description = "来源任务编号")
    private Long sourceTaskId;

}
