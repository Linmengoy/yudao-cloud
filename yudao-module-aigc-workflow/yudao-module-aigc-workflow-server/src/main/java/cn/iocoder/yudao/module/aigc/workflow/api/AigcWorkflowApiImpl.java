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
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasProjectService;
import cn.iocoder.yudao.module.aigc.workflow.service.instance.AigcWorkflowInstanceService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcWorkflowApiImpl implements AigcWorkflowApi {

    @Resource
    private AigcWorkflowInstanceService workflowInstanceService;
    @Resource
    private AigcCanvasProjectService canvasProjectService;

    @Override
    public CommonResult<AigcWorkflowExecuteRespDTO> execute(Long userId, AigcWorkflowExecuteReqDTO reqDTO) {
        return success(workflowInstanceService.execute(userId, reqDTO));
    }

    @Override
    public CommonResult<AigcWorkflowInstanceRespDTO> getInstance(Long instanceId) {
        return success(workflowInstanceService.getInstanceDetail(instanceId));
    }

    @Override
    public CommonResult<Boolean> retryNode(AigcWorkflowRetryNodeReqDTO reqDTO) {
        workflowInstanceService.retryNode(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> cancel(AigcWorkflowCancelReqDTO reqDTO) {
        workflowInstanceService.cancel(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<AigcWorkflowCostEstimateRespDTO> estimateCost(AigcWorkflowCostEstimateReqDTO reqDTO) {
        return success(workflowInstanceService.estimateCost(reqDTO));
    }

    @Override
    public CommonResult<Boolean> validateReadableCanvasProject(Long projectId, Long userId) {
        canvasProjectService.validateReadableProject(projectId, userId);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> handleNodeCallback(AigcWorkflowNodeCallbackReqDTO reqDTO) {
        workflowInstanceService.handleNodeCallback(reqDTO);
        return success(true);
    }

}
