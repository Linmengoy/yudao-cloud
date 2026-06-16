package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "App - Community comment create request")
@Data
public class AigcCommunityCommentCreateReqVO {

    @Schema(description = "Post ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Post ID cannot be null")
    private Long postId;

    @Schema(description = "Content", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Comment content cannot be blank")
    private String content;

}
