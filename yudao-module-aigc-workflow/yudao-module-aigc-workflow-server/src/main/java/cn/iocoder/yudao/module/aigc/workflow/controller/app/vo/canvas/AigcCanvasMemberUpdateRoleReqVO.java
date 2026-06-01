package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布成员角色更新 Request VO")
@Data
public class AigcCanvasMemberUpdateRoleReqVO {

    @Schema(description = "成员角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "viewer")
    @NotBlank(message = "成员角色不能为空")
    private String role;

}
