package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布项目创建 Request VO")
@Data
public class AigcCanvasProjectCreateReqVO {

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String name;

}
