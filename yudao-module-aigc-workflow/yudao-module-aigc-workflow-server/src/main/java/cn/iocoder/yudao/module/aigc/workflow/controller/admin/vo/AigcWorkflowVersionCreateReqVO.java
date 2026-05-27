package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 工作流版本创建 Request VO")
@Data
public class AigcWorkflowVersionCreateReqVO {

    @Schema(description = "工作流编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工作流编号不能为空")
    private Long workflowId;

    @Schema(description = "版本名称", example = "首发版本")
    private String versionName;

}
