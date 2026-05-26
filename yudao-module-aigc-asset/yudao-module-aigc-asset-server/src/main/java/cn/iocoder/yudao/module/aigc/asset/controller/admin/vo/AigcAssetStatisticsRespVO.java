package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - AIGC 资产统计 Response VO")
@Data
@Accessors(chain = true)
public class AigcAssetStatisticsRespVO {

    @Schema(description = "资产总数")
    private Long assetCount;

    @Schema(description = "下载总数")
    private Long downloadCount;

}
