package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 工作流执行 Request DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowExecuteReqDTO {

    @Schema(description = "工作流编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工作流编号不能为空")
    private Long workflowId;

    @Schema(description = "工作流版本编号", example = "2048")
    private Long workflowVersionId;

    @Schema(description = "来源模板编号", example = "4096")
    private Long templateId;

    @Schema(description = "客户端请求编号", example = "REQ202605260001")
    private String clientRequestId;

    @Schema(description = "触发类型", example = "MANUAL")
    private String triggerType;

    @Schema(description = "输入数据 JSON")
    private String inputData;

}
