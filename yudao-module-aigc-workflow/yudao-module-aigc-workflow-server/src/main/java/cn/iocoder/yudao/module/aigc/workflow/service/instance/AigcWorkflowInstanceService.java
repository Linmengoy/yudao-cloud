package cn.iocoder.yudao.module.aigc.workflow.service.instance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowInstancePageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCancelReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowNodeCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowRetryNodeReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowInstanceDO;

public interface AigcWorkflowInstanceService {

    AigcWorkflowExecuteRespDTO execute(Long userId, AigcWorkflowExecuteReqDTO reqDTO);
    AigcWorkflowCostEstimateRespDTO estimateCost(AigcWorkflowCostEstimateReqDTO reqDTO);
    AigcWorkflowInstanceDO getInstance(Long id);
    AigcWorkflowInstanceDO validateInstanceExists(Long id);
    PageResult<AigcWorkflowInstanceDO> getInstancePage(AigcWorkflowInstancePageReqVO reqVO);
    PageResult<AigcWorkflowInstanceDO> getUserInstancePage(AigcWorkflowInstancePageReqVO reqVO, Long userId);
    void retryNode(AigcWorkflowRetryNodeReqDTO reqDTO);
    void cancel(AigcWorkflowCancelReqDTO reqDTO);
    void handleNodeCallback(AigcWorkflowNodeCallbackReqDTO reqDTO);

}
