package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 资产直传签名 Request DTO")
@Data
@Accessors(chain = true)
public class AigcAssetDirectUploadPrepareReqDTO {

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IMAGE")
    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    @Schema(description = "资产标题", example = "上传图片")
    private String title;

    @Schema(description = "原文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "demo.png")
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "MIME 类型", example = "image/png")
    private String mimeType;

    @Schema(description = "文件大小，单位字节", requiredMode = Schema.RequiredMode.REQUIRED, example = "102400")
    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

}
