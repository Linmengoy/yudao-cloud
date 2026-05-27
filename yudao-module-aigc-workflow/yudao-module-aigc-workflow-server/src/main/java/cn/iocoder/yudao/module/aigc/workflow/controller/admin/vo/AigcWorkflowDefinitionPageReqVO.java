package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - AIGC 工作流定义分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowDefinitionPageReqVO extends PageParam {

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流编码")
    private String code;

    @Schema(description = "状态", example = "PUBLISHED")
    private String status;

    @Schema(description = "可见性", example = "PRIVATE")
    private String visibility;

    @Schema(description = "分类编号", example = "1024")
    private Long categoryId;

}
