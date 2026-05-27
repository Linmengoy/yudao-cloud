package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 工作流取消 Request DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowCancelReqDTO {

    @Schema(description = "工作流实例编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "工作流实例编号不能为空")
    private Long instanceId;

    @Schema(description = "取消原因")
    private String cancelReason;

}
