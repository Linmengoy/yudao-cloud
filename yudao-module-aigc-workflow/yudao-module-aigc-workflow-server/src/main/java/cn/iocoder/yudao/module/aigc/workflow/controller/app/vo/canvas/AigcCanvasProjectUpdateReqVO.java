package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布项目更新 Request VO")
@Data
public class AigcCanvasProjectUpdateReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目编号不能为空")
    private Long id;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "封面资产编号")
    private Long coverAssetId;
    @Schema(description = "节点数")
    private Integer nodeCount;
    @Schema(description = "资产数")
    private Integer assetCount;

}
