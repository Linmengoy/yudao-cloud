package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcAssetPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "资产类型", example = "IMAGE")
    private String assetType;

    @Schema(description = "来源类型", example = "GENERATE")
    private String sourceType;

    @Schema(description = "审核状态", example = "PASS")
    private String auditStatus;

    @Schema(description = "可见性", example = "PRIVATE")
    private String visibility;

    @Schema(description = "状态", example = "NORMAL")
    private String status;

    @Schema(description = "资产标题")
    private String title;

}
