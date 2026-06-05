package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 资产文件 Response DTO")
@Data
@Accessors(chain = true)
public class AigcAssetFileRespDTO {

    @Schema(description = "资产文件编号")
    private Long assetFileId;

    @Schema(description = "文件角色")
    private String fileRole;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "时长")
    private BigDecimal duration;

    @Schema(description = "访问 URL")
    private String accessUrl;

    @Schema(description = "有效期，单位秒")
    private Integer expireSeconds;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

}
