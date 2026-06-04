package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户端 - AIGC 画布项目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasProjectPageReqVO extends PageParam {

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "项目状态", example = "NORMAL")
    private String status;

}
