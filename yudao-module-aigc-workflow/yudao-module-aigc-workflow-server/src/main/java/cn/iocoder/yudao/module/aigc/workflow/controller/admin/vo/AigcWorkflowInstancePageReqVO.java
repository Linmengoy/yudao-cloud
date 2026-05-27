package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - AIGC 工作流实例分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowInstancePageReqVO extends PageParam {

    @Schema(description = "工作流编号", example = "1024")
    private Long workflowId;

    @Schema(description = "用户编号", example = "2048")
    private Long userId;

    @Schema(description = "状态", example = "RUNNING")
    private String status;

}
