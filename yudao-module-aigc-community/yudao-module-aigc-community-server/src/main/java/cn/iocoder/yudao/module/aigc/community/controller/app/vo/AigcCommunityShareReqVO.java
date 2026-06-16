package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "App - Community share request")
@Data
public class AigcCommunityShareReqVO {

    @Schema(description = "Post ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Post ID cannot be null")
    private Long postId;

    @Schema(description = "Share channel")
    private String shareChannel;

}
