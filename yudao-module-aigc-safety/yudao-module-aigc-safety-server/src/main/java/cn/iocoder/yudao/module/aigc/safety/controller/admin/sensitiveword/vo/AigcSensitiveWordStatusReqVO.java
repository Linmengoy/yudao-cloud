package cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 敏感词状态更新 Request VO")
@Data
public class AigcSensitiveWordStatusReqVO {

    @Schema(description = "敏感词编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "敏感词编号不能为空")
    private Long id;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "ENABLE")
    @NotBlank(message = "状态不能为空")
    private String status;

}
