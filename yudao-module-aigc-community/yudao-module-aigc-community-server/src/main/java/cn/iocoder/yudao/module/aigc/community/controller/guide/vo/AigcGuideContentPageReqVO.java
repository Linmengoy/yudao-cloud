package cn.iocoder.yudao.module.aigc.community.controller.guide.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "Admin - Guide content page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcGuideContentPageReqVO extends PageParam {

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Category")
    private String category;

    @Schema(description = "Publish status")
    private String publishStatus;

    @Schema(description = "Create time range")
    private LocalDateTime[] createTime;

}
