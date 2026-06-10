package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 资产直传完成 Request DTO")
@Data
@Accessors(chain = true)
public class AigcAssetDirectUploadCompleteReqDTO {

    @Schema(description = "上传令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "上传令牌不能为空")
    private String uploadToken;

    @Schema(description = "资源宽度")
    private Integer width;

    @Schema(description = "资源高度")
    private Integer height;

    @Schema(description = "资源时长，单位秒")
    private java.math.BigDecimal duration;

    @Schema(description = "扩展元数据 JSON")
    private String metadata;

}
