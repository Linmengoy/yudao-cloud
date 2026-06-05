package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 资产访问 URL Request DTO")
@Data
@Accessors(chain = true)
public class AigcAssetAccessUrlReqDTO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "文件角色", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件角色不能为空")
    private String fileRole;

    @Schema(description = "访问类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "访问类型不能为空")
    private String accessType;

}
