package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - AIGC 工作流日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowLogPageReqVO extends PageParam {

    @Schema(description = "工作流实例编号", example = "1024")
    private Long workflowInstanceId;

    @Schema(description = "节点实例编号", example = "2048")
    private Long nodeInstanceId;

    @Schema(description = "日志类型", example = "NODE_SUCCESS")
    private String logType;

}
