package cn.iocoder.yudao.module.aigc.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 人工审核通过 Request DTO")
@Data
public class AigcAuditPassReqDTO {

    @Schema(description = "审核记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "审核记录编号不能为空")
    private Long auditId;

    @Schema(description = "审核人用户编号", example = "1024")
    private Long auditorUserId;

    @Schema(description = "备注")
    private String remark;

}
