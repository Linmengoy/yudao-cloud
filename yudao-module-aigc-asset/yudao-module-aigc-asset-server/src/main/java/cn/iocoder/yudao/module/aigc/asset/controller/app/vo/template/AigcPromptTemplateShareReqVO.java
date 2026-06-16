package cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Schema(description = "用户端 - AIGC 用户分享模板 Request VO")
@Data
public class AigcPromptTemplateShareReqVO {

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "写实头像提示词")
    @NotBlank(message = "标题不能为空")
    @Length(max = 60, message = "标题最多 60 个字符")
    private String title;

    @Schema(description = "完整提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    @Schema(description = "适配模型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "model_image_kling")
    @NotBlank(message = "模型编码不能为空")
    private String modelCode;

    @Schema(description = "适配模型名称", example = "可灵图片生成")
    private String modelName;

    @Schema(description = "生成参数 JSON", example = "{\"size\":\"1024x1024\",\"steps\":30}")
    private String modelParams;

    @Schema(description = "封面资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "封面资产不能为空")
    private Long coverAssetId;

    @Schema(description = "可见性", requiredMode = Schema.RequiredMode.REQUIRED, example = "PUBLIC")
    @NotBlank(message = "可见性不能为空")
    private String visibility;

}
