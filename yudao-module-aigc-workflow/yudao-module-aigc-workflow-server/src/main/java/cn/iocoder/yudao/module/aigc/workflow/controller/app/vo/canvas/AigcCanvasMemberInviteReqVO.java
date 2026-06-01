package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布邀请成员 Request VO")
@Data
public class AigcCanvasMemberInviteReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "成员角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "editor")
    @NotBlank(message = "成员角色不能为空")
    private String role;

}
