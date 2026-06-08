package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Schema(description = "Admin - AIGC asset page request")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AigcAssetPageReqVO extends PageParam {

    @Schema(description = "User ID", example = "1024")
    private Long userId;

    @Schema(description = "Asset type", example = "IMAGE")
    private String assetType;

    @Schema(description = "Asset category", example = "OTHER")
    private String category;

    @Schema(description = "Source type", example = "GENERATE")
    private String sourceType;

    @Schema(description = "Audit status", example = "PASS")
    private String auditStatus;

    @Schema(description = "Visibility", example = "PRIVATE")
    private String visibility;

    @Schema(description = "Status", example = "NORMAL")
    private String status;

    @Schema(description = "Asset title")
    private String title;

}
