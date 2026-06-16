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

    @Schema(description = "thumbnail access URL")
    private String thumbnailUrl;

    @Schema(description = "thumbnail storage config ID")
    private Long thumbnailConfigId;

    @Schema(description = "thumbnail storage type")
    private String thumbnailStorageType;

    @Schema(description = "thumbnail bucket")
    private String thumbnailBucket;

    @Schema(description = "thumbnail object key")
    private String thumbnailObjectKey;

    @Schema(description = "thumbnail file path")
    private String thumbnailPath;

    @Schema(description = "thumbnail file name")
    private String thumbnailFileName;

    @Schema(description = "thumbnail MIME type")
    private String thumbnailMimeType;

    @Schema(description = "thumbnail file size")
    private Long thumbnailFileSize;

    @Schema(description = "thumbnail width")
    private Integer thumbnailWidth;

    @Schema(description = "thumbnail height")
    private Integer thumbnailHeight;

    @Schema(description = "thumbnail public access")
    private Boolean thumbnailPublicAccess;

}
