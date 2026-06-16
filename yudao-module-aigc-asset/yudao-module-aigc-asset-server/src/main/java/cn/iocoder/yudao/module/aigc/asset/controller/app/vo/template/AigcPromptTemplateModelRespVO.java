package cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户端 - AIGC 提示词模板模型 Response VO")
@Data
public class AigcPromptTemplateModelRespVO {

    @Schema(description = "模型编码", example = "gpt-image-2")
    private String modelCode;

    @Schema(description = "模型名称", example = "GPT Image 2")
    private String modelName;

}
