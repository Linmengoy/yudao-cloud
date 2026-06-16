package cn.iocoder.yudao.module.aigc.workflow.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCancelReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowInstanceRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowNodeCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowRetryNodeReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 工作流")
public interface AigcWorkflowApi {

    String PREFIX = ApiConstants.PREFIX;

    @PostMapping(PREFIX + "/execute")
    @Operation(summary = "执行工作流")
    CommonResult<AigcWorkflowExecuteRespDTO> execute(@RequestParam("userId") Long userId,
                                                     @Valid @RequestBody AigcWorkflowExecuteReqDTO reqDTO);

    @GetMapping(PREFIX + "/get-instance")
    @Operation(summary = "获取工作流实例")
    @Parameter(name = "instanceId", description = "实例编号", required = true, example = "1024")
    CommonResult<AigcWorkflowInstanceRespDTO> getInstance(@RequestParam("instanceId") Long instanceId);

    @PostMapping(PREFIX + "/retry-node")
    @Operation(summary = "重试节点")
    CommonResult<Boolean> retryNode(@Valid @RequestBody AigcWorkflowRetryNodeReqDTO reqDTO);

    @PostMapping(PREFIX + "/cancel")
    @Operation(summary = "取消工作流实例")
    CommonResult<Boolean> cancel(@Valid @RequestBody AigcWorkflowCancelReqDTO reqDTO);

    @PostMapping(PREFIX + "/estimate-cost")
    @Operation(summary = "预估工作流费用")
    CommonResult<AigcWorkflowCostEstimateRespDTO> estimateCost(@Valid @RequestBody AigcWorkflowCostEstimateReqDTO reqDTO);

    @GetMapping(PREFIX + "/validate-readable-canvas-project")
    @Operation(summary = "Validate canvas project read permission")
    CommonResult<Boolean> validateReadableCanvasProject(@RequestParam("projectId") Long projectId,
                                                        @RequestParam("userId") Long userId);

    @PostMapping(PREFIX + "/handle-node-callback")
    @Operation(summary = "处理节点回调")
    CommonResult<Boolean> handleNodeCallback(@Valid @RequestBody AigcWorkflowNodeCallbackReqDTO reqDTO);

}
