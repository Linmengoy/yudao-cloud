package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 工作流节点回调 Request DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowNodeCallbackReqDTO {

    @Schema(description = "节点实例编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "节点实例编号不能为空")
    private Long nodeInstanceId;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "生成记录编号", example = "4096")
    private Long genRecordId;

    @Schema(description = "回调流水号", example = "CB202605270001")
    private String callbackNo;

    @Schema(description = "状态", example = "SUCCESS")
    private String status;

    @Schema(description = "输出数据 JSON")
    private String outputData;

    @Schema(description = "输出资产编号 JSON")
    private String assetIds;

    @Schema(description = "失败原因")
    private String failReason;

}
