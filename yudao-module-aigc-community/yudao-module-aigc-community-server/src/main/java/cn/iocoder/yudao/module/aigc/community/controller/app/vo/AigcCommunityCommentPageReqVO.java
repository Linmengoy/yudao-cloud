package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "App - Community comment page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityCommentPageReqVO extends PageParam {

    @Schema(description = "Post ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Post ID cannot be null")
    private Long postId;

}
