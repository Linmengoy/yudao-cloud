package cn.iocoder.yudao.module.aigc.gen.controller.admin.prompt.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AIGC Text 系统提示词 Response VO")
@Data
public class AigcTextSystemPromptRespVO {

    @Schema(description = "配置键名", requiredMode = Schema.RequiredMode.REQUIRED, example = "aigc.text.system-prompt")
    private String key;

    @Schema(description = "系统提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

}
