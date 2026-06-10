package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 资产直传签名 Response DTO")
@Data
@Accessors(chain = true)
public class AigcAssetDirectUploadPrepareRespDTO {

    @Schema(description = "上传令牌")
    private String uploadToken;

    @Schema(description = "文件上传 URL")
    private String uploadUrl;

    @Schema(description = "文件访问 URL")
    private String url;

    @Schema(description = "文件配置编号")
    private Long configId;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "Bucket")
    private String bucket;

    @Schema(description = "对象 Key")
    private String objectKey;

    @Schema(description = "文件路径")
    private String path;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

}
