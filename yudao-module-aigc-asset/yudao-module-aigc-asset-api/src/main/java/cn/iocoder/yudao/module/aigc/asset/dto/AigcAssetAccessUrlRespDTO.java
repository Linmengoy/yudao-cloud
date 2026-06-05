package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 资产访问 URL Response DTO")
@Data
@Accessors(chain = true)
public class AigcAssetAccessUrlRespDTO {

    @Schema(description = "资产编号")
    private Long assetId;

    @Schema(description = "资产文件编号")
    private Long assetFileId;

    @Schema(description = "文件角色")
    private String fileRole;

    @Schema(description = "访问类型")
    private String accessType;

    @Schema(description = "访问 URL")
    private String url;

    @Schema(description = "有效期，单位秒")
    private Integer expireSeconds;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

    @Schema(description = "是否命中缓存")
    private Boolean cacheHit;

}
