package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "App - Community post publish request")
@Data
public class AigcCommunityPostPublishReqVO {

    @Schema(description = "Asset ID")
    private Long assetId;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Cover asset ID")
    private Long coverAssetId;

    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "Tags")
    private String tags;

    @Schema(description = "Prompt snapshot")
    private String promptSnapshot;

    @Schema(description = "Metadata")
    private String metadata;

}
