package cn.iocoder.yudao.module.aigc.community.controller.guide.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Admin - Guide content save request")
@Data
public class AigcGuideContentSaveReqVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Slug", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Slug cannot be blank")
    @Size(max = 128, message = "Slug cannot exceed 128 characters")
    private String slug;

    @Schema(description = "Title", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 128, message = "Title cannot exceed 128 characters")
    private String title;

    @Schema(description = "Category")
    @Size(max = 64, message = "Category cannot exceed 64 characters")
    private String category;

    @Schema(description = "Summary")
    @Size(max = 512, message = "Summary cannot exceed 512 characters")
    private String summary;

    @Schema(description = "Markdown content", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Content cannot be blank")
    private String content;

    @Schema(description = "Sort")
    private Integer sort;

}
