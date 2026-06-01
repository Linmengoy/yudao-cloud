package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布草图 Response VO")
@Data
public class AigcCanvasSketchRespVO {

    @Schema(description = "草图编号")
    private Long id;
    @Schema(description = "项目编号")
    private Long projectId;
    @Schema(description = "节点编号")
    private String nodeId;
    @Schema(description = "tldraw 场景 JSON")
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
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
