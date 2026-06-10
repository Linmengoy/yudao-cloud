package cn.iocoder.yudao.module.infra.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - 文件上传预签名 Response DTO")
@Data
@Accessors(chain = true)
public class FilePresignPutRespDTO {

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

    @Schema(description = "文件上传 URL")
    private String uploadUrl;

    @Schema(description = "文件访问 URL")
    private String url;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

}
