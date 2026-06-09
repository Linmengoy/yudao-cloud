package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布项目资源访问 URL Response VO")
@Data
public class AigcCanvasProjectAssetAccessUrlRespVO {

    @Schema(description = "资源编号")
    private Long assetId;

    @Schema(description = "资源文件编号")
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

}
