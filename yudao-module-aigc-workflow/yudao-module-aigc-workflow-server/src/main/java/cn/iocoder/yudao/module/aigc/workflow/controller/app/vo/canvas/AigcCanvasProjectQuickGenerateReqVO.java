package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布项目快速生成 Request VO")
@Data
public class AigcCanvasProjectQuickGenerateReqVO {

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @Schema(description = "提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    @Schema(description = "节点类型 image/video", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @Schema(description = "生成类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IMAGE")
    @NotBlank(message = "生成类型不能为空")
    private String generateType;

    @Schema(description = "生成模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    @NotBlank(message = "生成模式不能为空")
    private String generateMode;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模型编号不能为空")
    private Long modelId;

    @Schema(description = "模型展示名")
    private String modelName;

    @Schema(description = "渠道模型")
    private String providerModel;

    @Schema(description = "输入参数 JSON")
    private String inputParams;

    @Schema(description = "参考图资产编号")
    private Long referenceAssetId;

    @Schema(description = "参考图预览地址")
    private String referencePreviewUrl;

}
