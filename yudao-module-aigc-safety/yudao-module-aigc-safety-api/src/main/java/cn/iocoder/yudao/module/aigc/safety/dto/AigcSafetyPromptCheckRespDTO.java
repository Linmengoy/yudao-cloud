package cn.iocoder.yudao.module.aigc.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "RPC 服务 - AIGC 提示词安全检查 Response DTO")
@Data
public class AigcSafetyPromptCheckRespDTO {

    @Schema(description = "是否通过", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean pass;

    @Schema(description = "命中的敏感词")
    private List<String> hitWords;

    @Schema(description = "风险等级", example = "3")
    private Integer riskLevel;

    @Schema(description = "拒绝原因")
    private String reason;

}
