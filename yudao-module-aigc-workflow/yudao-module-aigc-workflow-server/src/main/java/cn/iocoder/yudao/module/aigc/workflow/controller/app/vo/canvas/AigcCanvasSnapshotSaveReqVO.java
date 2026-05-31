package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布快照保存 Request VO")
@Data
public class AigcCanvasSnapshotSaveReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目编号不能为空")
    private Long projectId;

    @Schema(description = "客户端基于的画布版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户端基于版本不能为空")
    private Long baseVersion;

    @Schema(description = "客户端实例编号")
    private String clientId;

    @Schema(description = "节点 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点 JSON 不能为空")
    private String nodesJson;

    @Schema(description = "连线 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "连线 JSON 不能为空")
    private String edgesJson;

    @Schema(description = "视口 JSON")
    private String viewportJson;

    @Schema(description = "节点数")
    private Integer nodeCount;

    @Schema(description = "资产数")
    private Integer assetCount;

}
