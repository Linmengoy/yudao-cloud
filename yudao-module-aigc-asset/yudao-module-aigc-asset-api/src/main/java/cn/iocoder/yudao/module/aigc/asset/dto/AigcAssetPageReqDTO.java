package cn.iocoder.yudao.module.aigc.asset.dto;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "RPC 服务 - AIGC 资产分页 Request DTO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcAssetPageReqDTO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "资产类型", example = "IMAGE")
    private String assetType;

    @Schema(description = "来源类型", example = "GENERATE")
    private String sourceType;

    @Schema(description = "审核状态", example = "PASS")
    private String auditStatus;

    @Schema(description = "状态", example = "NORMAL")
    private String status;

}
