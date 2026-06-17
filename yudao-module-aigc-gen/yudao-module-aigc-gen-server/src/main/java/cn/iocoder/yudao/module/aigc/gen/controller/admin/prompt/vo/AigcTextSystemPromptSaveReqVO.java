package cn.iocoder.yudao.module.aigc.gen.controller.admin.prompt.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - AIGC Text 系统提示词保存 Request VO")
@Data
public class AigcTextSystemPromptSaveReqVO {

    @Schema(description = "系统提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "系统提示词不能为空")
    @Size(max = 50000, message = "系统提示词长度不能超过 50000 个字符")
    private String value;

}
