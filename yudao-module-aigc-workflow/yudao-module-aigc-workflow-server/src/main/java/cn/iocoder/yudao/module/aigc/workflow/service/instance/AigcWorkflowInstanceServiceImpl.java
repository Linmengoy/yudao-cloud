package cn.iocoder.yudao.module.aigc.workflow.service.instance;

import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.gen.api.AigcGenerateApi;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowInstancePageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowDefinitionDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowInstanceDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeInstanceDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowInstanceMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowNodeInstanceMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowNodeMapper;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCancelReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowNodeCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowRetryNodeReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowInstanceStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowNodeStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowNodeTypeEnum;
import cn.iocoder.yudao.module.aigc.workflow.service.definition.AigcWorkflowDefinitionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.getCheckedData;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_INSTANCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NODE_INSTANCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NO_EXECUTABLE_NODE;

@Service
@Validated
public class AigcWorkflowInstanceServiceImpl implements AigcWorkflowInstanceService {

    @Resource
    private AigcWorkflowInstanceMapper instanceMapper;
    @Resource
    private AigcWorkflowNodeMapper nodeMapper;
    @Resource
    private AigcWorkflowNodeInstanceMapper nodeInstanceMapper;
    @Resource
    private AigcWorkflowDefinitionService definitionService;
    @Resource
    private AigcGenerateApi generateApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcWorkflowExecuteRespDTO execute(Long userId, AigcWorkflowExecuteReqDTO reqDTO) {
        AigcWorkflowDefinitionDO definition = definitionService.validateDefinitionExists(reqDTO.getWorkflowId());
        Long versionId = reqDTO.getWorkflowVersionId() != null ? reqDTO.getWorkflowVersionId() : definition.getCurrentVersionId();
        AigcWorkflowInstanceDO instance = new AigcWorkflowInstanceDO()
                .setInstanceNo(generateInstanceNo())
                .setWorkflowId(definition.getId())
                .setWorkflowVersionId(versionId)
                .setTemplateId(reqDTO.getTemplateId())
                .setUserId(userId)
                .setStatus(AigcWorkflowInstanceStatusEnum.RUNNING.getCode())
                .setInputData(reqDTO.getInputData())
                .setProgress(0)
                .setStartTime(LocalDateTime.now());
        instanceMapper.insert(instance);
        AigcWorkflowNodeInstanceDO nodeInstance = createFirstExecutableNode(instance, versionId, reqDTO.getInputData());
        instance.setMainTaskId(nodeInstance.getTaskId());
        instanceMapper.updateById(new AigcWorkflowInstanceDO().setId(instance.getId()).setMainTaskId(nodeInstance.getTaskId()));
        return new AigcWorkflowExecuteRespDTO()
                .setInstanceId(instance.getId())
                .setInstanceNo(instance.getInstanceNo())
                .setMainTaskId(instance.getMainTaskId())
                .setStatus(instance.getStatus());
    }

    @Override
    public AigcWorkflowCostEstimateRespDTO estimateCost(AigcWorkflowCostEstimateReqDTO reqDTO) {
        AigcWorkflowDefinitionDO definition = definitionService.validateDefinitionExists(reqDTO.getWorkflowId());
        Long versionId = reqDTO.getWorkflowVersionId() != null ? reqDTO.getWorkflowVersionId() : definition.getCurrentVersionId();
        int nodeCount = nodeMapper.selectListByVersionId(reqDTO.getWorkflowId(), versionId).size();
        return new AigcWorkflowCostEstimateRespDTO().setEstimateAmount(0L).setNodeCount(nodeCount).setDetailData("{}");
    }

    @Override
    public AigcWorkflowInstanceDO getInstance(Long id) {
        return instanceMapper.selectById(id);
    }

    @Override
    public AigcWorkflowInstanceDO validateInstanceExists(Long id) {
        AigcWorkflowInstanceDO instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw exception(WORKFLOW_INSTANCE_NOT_EXISTS);
        }
        return instance;
    }

    @Override
    public PageResult<AigcWorkflowInstanceDO> getInstancePage(AigcWorkflowInstancePageReqVO reqVO) {
        return instanceMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcWorkflowInstanceDO> getUserInstancePage(AigcWorkflowInstancePageReqVO reqVO, Long userId) {
        reqVO.setUserId(userId);
        return instanceMapper.selectPage(reqVO);
    }

    @Override
    public void retryNode(AigcWorkflowRetryNodeReqDTO reqDTO) {
        validateInstanceExists(reqDTO.getInstanceId());
        AigcWorkflowNodeInstanceDO nodeInstance = validateNodeInstanceExists(reqDTO.getNodeInstanceId());
        nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                .setId(nodeInstance.getId())
                .setStatus(AigcWorkflowNodeStatusEnum.READY.getCode())
                .setRetryCount(nodeInstance.getRetryCount() + 1));
    }

    @Override
    public void cancel(AigcWorkflowCancelReqDTO reqDTO) {
        validateInstanceExists(reqDTO.getInstanceId());
        instanceMapper.updateById(new AigcWorkflowInstanceDO()
                .setId(reqDTO.getInstanceId())
                .setStatus(AigcWorkflowInstanceStatusEnum.CANCELED.getCode())
                .setFailReason(reqDTO.getCancelReason())
                .setFinishTime(LocalDateTime.now()));
    }

    @Override
    public void handleNodeCallback(AigcWorkflowNodeCallbackReqDTO reqDTO) {
        AigcWorkflowNodeInstanceDO nodeInstance = validateNodeInstanceExists(reqDTO.getNodeInstanceId());
        nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                .setId(nodeInstance.getId())
                .setStatus(reqDTO.getStatus())
                .setTaskId(reqDTO.getTaskId())
                .setGenRecordId(reqDTO.getGenRecordId())
                .setOutputData(reqDTO.getOutputData())
                .setAssetIds(reqDTO.getAssetIds())
                .setFinishTime(LocalDateTime.now()));
    }

    private AigcWorkflowNodeInstanceDO createFirstExecutableNode(AigcWorkflowInstanceDO instance, Long versionId, String inputData) {
        List<AigcWorkflowNodeDO> nodes = nodeMapper.selectListByVersionId(instance.getWorkflowId(), versionId);
        AigcWorkflowNodeDO node = nodes.stream()
                .filter(item -> AigcWorkflowNodeTypeEnum.TEXT_GENERATE.getCode().equals(item.getNodeType())
                        || AigcWorkflowNodeTypeEnum.IMAGE_GENERATE.getCode().equals(item.getNodeType())
                        || AigcWorkflowNodeTypeEnum.VIDEO_GENERATE.getCode().equals(item.getNodeType()))
                .findFirst()
                .orElseThrow(() -> exception(WORKFLOW_NO_EXECUTABLE_NODE));
        AigcGenerateSubmitRespDTO submitRespDTO = getCheckedData(generateApi.submit(new AigcGenerateSubmitReqDTO()
                .setUserId(instance.getUserId())
                .setClientRequestId(instance.getInstanceNo() + ":" + node.getNodeKey())
                .setGenerateType(node.getGenerateType())
                .setGenerateMode(node.getGenerateMode())
                .setModelId(node.getModelId())
                .setInputParams(inputData)
                .setSync(false)));
        AigcWorkflowNodeInstanceDO nodeInstance = new AigcWorkflowNodeInstanceDO()
                .setWorkflowInstanceId(instance.getId())
                .setNodeId(node.getId())
                .setNodeKey(node.getNodeKey())
                .setNodeType(node.getNodeType())
                .setStatus(AigcWorkflowNodeStatusEnum.RUNNING.getCode())
                .setTaskId(submitRespDTO.getTaskId())
                .setGenRecordId(submitRespDTO.getId())
                .setInputData(inputData)
                .setRetryCount(0)
                .setMaxRetryCount(3)
                .setStartTime(LocalDateTime.now());
        nodeInstanceMapper.insert(nodeInstance);
        return nodeInstance;
    }

    private AigcWorkflowNodeInstanceDO validateNodeInstanceExists(Long id) {
        AigcWorkflowNodeInstanceDO nodeInstance = nodeInstanceMapper.selectById(id);
        if (nodeInstance == null) {
            throw exception(WORKFLOW_NODE_INSTANCE_NOT_EXISTS);
        }
        return nodeInstance;
    }

    private String generateInstanceNo() {
        return "WF" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmssSSS");
    }

}
