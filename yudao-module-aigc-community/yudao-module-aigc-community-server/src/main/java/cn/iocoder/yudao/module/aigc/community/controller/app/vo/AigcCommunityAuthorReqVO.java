package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "App - Community author request")
@Data
public class AigcCommunityAuthorReqVO {

    @Schema(description = "Author user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Author user ID cannot be null")
    private Long authorUserId;

}
