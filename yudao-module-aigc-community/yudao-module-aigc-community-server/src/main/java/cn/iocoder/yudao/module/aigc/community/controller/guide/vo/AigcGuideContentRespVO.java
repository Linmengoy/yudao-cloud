package cn.iocoder.yudao.module.aigc.community.controller.guide.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Guide content response")
@Data
public class AigcGuideContentRespVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Slug")
    private String slug;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Category")
    private String category;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "Markdown content")
    private String content;

    @Schema(description = "Sort")
    private Integer sort;

    @Schema(description = "Publish status")
    private String publishStatus;

    @Schema(description = "Publish time")
    private LocalDateTime publishTime;

    @Schema(description = "Publisher user ID")
    private Long publisherUserId;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

}
