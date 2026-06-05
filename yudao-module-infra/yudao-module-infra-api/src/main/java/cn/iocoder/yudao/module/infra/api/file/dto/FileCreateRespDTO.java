package cn.iocoder.yudao.module.infra.api.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - 文件创建 Response DTO")
@Data
@Accessors(chain = true)
public class FileCreateRespDTO {

    @Schema(description = "文件编号")
    private Long id;

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

    @Schema(description = "文件名")
    private String name;

    @Schema(description = "访问 URL")
    private String url;

    @Schema(description = "MIME 类型")
    private String type;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

}
