package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布草图保存 Request VO")
@Data
public class AigcCanvasSketchSaveReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目编号不能为空")
    private Long projectId;

    @Schema(description = "节点编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nodeId;

    @Schema(description = "tldraw 场景 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "草图内容不能为空")
    private String sceneJson;

    @Schema(description = "预览图地址")
    private String previewUrl;

    @Schema(description = "预览图 data URL")
    private String previewDataUrl;

    @Schema(description = "预览资产 ID")
    private Long previewAssetId;

    @Schema(description = "预览资产版本 ID")
    private Long previewAssetVersionId;

    @Schema(description = "导出图片类型")
    private String mimeType;

    @Schema(description = "导出宽度")
    private Integer width;

    @Schema(description = "导出高度")
    private Integer height;

    @Schema(description = "导出背景")
    private String background;

}
