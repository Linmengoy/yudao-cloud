package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户端 - AIGC 工作流分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowAppPageReqVO extends PageParam {

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "分类编号", example = "1024")
    private Long categoryId;

}
