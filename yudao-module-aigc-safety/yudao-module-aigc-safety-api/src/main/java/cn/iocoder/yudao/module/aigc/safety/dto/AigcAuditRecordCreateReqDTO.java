package cn.iocoder.yudao.module.aigc.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 审核记录创建 Request DTO")
@Data
public class AigcAuditRecordCreateReqDTO {

    @Schema(description = "审核对象类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET")
    @NotBlank(message = "审核对象类型不能为空")
    private String objectType;

    @Schema(description = "审核对象编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "审核对象编号不能为空")
    private Long objectId;

    @Schema(description = "被审核内容")
    private String content;

    @Schema(description = "审核场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASSET")
    @NotBlank(message = "审核场景不能为空")
    private String scene;

    @Schema(description = "审核状态", example = "PENDING")
    private String auditStatus;

    @Schema(description = "审核结果", example = "AUTO_PASS")
    private String auditResult;

    @Schema(description = "命中敏感词 JSON")
    private String hitWords;

    @Schema(description = "风险等级", example = "3")
    private Integer riskLevel;

}
