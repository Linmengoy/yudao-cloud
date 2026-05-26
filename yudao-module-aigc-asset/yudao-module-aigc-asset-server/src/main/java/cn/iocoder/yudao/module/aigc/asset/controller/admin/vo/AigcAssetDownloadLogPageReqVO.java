package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 资产下载日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcAssetDownloadLogPageReqVO extends PageParam {

    @Schema(description = "资产编号", example = "1024")
    private Long assetId;

    @Schema(description = "下载用户编号", example = "1024")
    private Long userId;

    @Schema(description = "资产归属用户编号", example = "1024")
    private Long ownerUserId;

    @Schema(description = "下载结果", example = "SUCCESS")
    private String result;

}
