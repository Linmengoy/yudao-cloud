package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 提示词模板导入 Request VO")
@Data
public class AigcPromptTemplateImportReqVO {

    @Schema(description = "cases.json 文件绝对路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "/Users/jesse/Downloads/Copse/Prompts/awesome-gpt-image-2/data/cases.json")
    @NotBlank(message = "cases.json 文件路径不能为空")
    private String casesJsonPath;

    @Schema(description = "图片目录绝对路径", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "/Users/jesse/Downloads/Copse/Prompts/awesome-gpt-image-2/data/images")
    @NotBlank(message = "图片目录不能为空")
    private String imageDirPath;

    @Schema(description = "OSS 存储目录", example = "aigc/templates")
    private String storageDirectory = "aigc/templates";

}
