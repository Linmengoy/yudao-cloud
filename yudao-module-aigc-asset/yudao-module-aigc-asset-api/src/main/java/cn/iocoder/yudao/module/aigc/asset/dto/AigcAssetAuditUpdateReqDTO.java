package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 资产审核更新 Request DTO")
@Data
public class AigcAssetAuditUpdateReqDTO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "资产编号不能为空")
    private Long id;

    @Schema(description = "审核状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASS")
    @NotBlank(message = "审核状态不能为空")
    private String auditStatus;

    @Schema(description = "审核原因")
    private String auditReason;

}
