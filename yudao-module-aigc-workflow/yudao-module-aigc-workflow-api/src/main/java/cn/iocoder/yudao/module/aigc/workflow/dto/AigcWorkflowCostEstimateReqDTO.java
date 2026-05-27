package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 工作流费用预估 Request DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowCostEstimateReqDTO {

    @Schema(description = "工作流编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工作流编号不能为空")
    private Long workflowId;

    @Schema(description = "工作流版本编号", example = "2048")
    private Long workflowVersionId;

    @Schema(description = "输入数据 JSON")
    private String inputData;

}
