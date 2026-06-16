package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "App - Community like request")
@Data
public class AigcCommunityPostLikeReqVO {

    @Schema(description = "Post ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Post ID cannot be null")
    private Long postId;

}
