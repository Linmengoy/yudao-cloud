package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布项目资源访问 URL Request VO")
@Data
public class AigcCanvasProjectAssetAccessUrlReqVO {

    @Schema(description = "资源编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "资源编号不能为空")
    private Long assetId;

    @Schema(description = "文件角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORIGINAL")
    @NotBlank(message = "文件角色不能为空")
    private String fileRole;

    @Schema(description = "访问类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PREVIEW")
    @NotBlank(message = "访问类型不能为空")
    private String accessType;

}
