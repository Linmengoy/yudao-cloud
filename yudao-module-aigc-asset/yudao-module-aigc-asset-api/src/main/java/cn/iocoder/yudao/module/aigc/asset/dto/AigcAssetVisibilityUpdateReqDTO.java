package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 资产可见性更新 Request DTO")
@Data
public class AigcAssetVisibilityUpdateReqDTO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "资产编号不能为空")
    private Long id;

    @Schema(description = "可见性", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRIVATE")
    @NotBlank(message = "可见性不能为空")
    private String visibility;

}
