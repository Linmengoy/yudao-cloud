package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 资产下载 Request DTO")
@Data
public class AigcAssetDownloadReqDTO {

    @Schema(description = "资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "资产编号不能为空")
    private Long assetId;

    @Schema(description = "下载用户编号", example = "1024")
    private Long userId;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "User-Agent")
    private String userAgent;

    @Schema(description = "来源页面")
    private String referer;

}
