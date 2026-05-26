package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 资产更新 Request DTO")
@Data
public class AigcAssetUpdateReqDTO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "资产编号不能为空")
    private Long id;

    @Schema(description = "资产标题")
    private String title;

    @Schema(description = "资产描述")
    private String description;

    @Schema(description = "标签")
    private String tags;

}
