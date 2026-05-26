package cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 敏感词新增/修改 Request VO")
@Data
public class AigcSensitiveWordSaveReqVO {

    @Schema(description = "敏感词编号", example = "1024")
    private Long id;

    @Schema(description = "敏感词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "敏感词不能为空")
    private String word;

    @Schema(description = "审核场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "PROMPT")
    @NotBlank(message = "审核场景不能为空")
    private String scene;

    @Schema(description = "风险等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "风险等级不能为空")
    private Integer level;

    @Schema(description = "匹配方式", example = "CONTAINS")
    private String matchType;

    @Schema(description = "状态", example = "ENABLE")
    private String status;

    @Schema(description = "备注")
    private String remark;

}
