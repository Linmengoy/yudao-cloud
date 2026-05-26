package cn.iocoder.yudao.module.aigc.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 提示词安全检查 Request DTO")
@Data
public class AigcSafetyPromptCheckReqDTO {

    @Schema(description = "提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提示词不能为空")
    private String prompt;

    @Schema(description = "审核场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMPT")
    @NotBlank(message = "审核场景不能为空")
    private String scene;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "业务编号", example = "1024")
    private String bizId;

}
