package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 工作流费用预估 Response DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowCostEstimateRespDTO {

    @Schema(description = "预估费用", example = "100")
    private Long estimateAmount;

    @Schema(description = "节点数量", example = "3")
    private Integer nodeCount;

    @Schema(description = "费用明细 JSON")
    private String detailData;

}
