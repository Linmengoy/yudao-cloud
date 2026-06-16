package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "App - Community post page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityPostPageReqVO extends PageParam {

    @Schema(description = "Sort: latest/hot", example = "latest")
    private String sort;

    @Schema(description = "Asset type", example = "IMAGE")
    private String assetType;

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Tag")
    private String tag;

}
