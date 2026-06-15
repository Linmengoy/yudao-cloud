package cn.iocoder.yudao.module.aigc.community.controller.app.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "App - Community author posts request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityAuthorPostPageReqVO extends PageParam {

    @Schema(description = "Author user ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Author user ID cannot be null")
    private Long authorUserId;

    @Schema(description = "Sort: latest/hot")
    private String sort;

}
