package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 资产下载日志 Response VO")
@Data
public class AigcAssetDownloadLogRespVO {

    @Schema(description = "日志编号")
    private Long id;

    @Schema(description = "资产编号")
    private Long assetId;

    @Schema(description = "资产流水号")
    private String assetNo;

    @Schema(description = "下载用户编号")
    private Long userId;

    @Schema(description = "资产归属用户编号")
    private Long ownerUserId;

    @Schema(description = "下载 URL")
    private String downloadUrl;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "来源页面")
    private String referer;

    @Schema(description = "结果")
    private String result;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
