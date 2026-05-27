package cn.iocoder.yudao.module.aigc.workflow.service.instance;

import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum;
import cn.iocoder.yudao.module.aigc.gen.api.AigcGenerateApi;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.model.api.AigcModelApi;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowInstancePageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowDefinitionDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowEdgeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowInstanceDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeInstanceDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowEdgeMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowInstanceMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowLogMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowNodeInstanceMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowNodeMapper;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCancelReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowInstanceRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowNodeCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowNodeInstanceRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowRetryNodeReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowInstanceStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowNodeStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowNodeTypeEnum;
import cn.iocoder.yudao.module.aigc.workflow.service.definition.AigcWorkflowDefinitionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_CURRENT_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_GRAPH_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_INSTANCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_INSTANCE_STATUS_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NODE_INSTANCE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NO_EXECUTABLE_NODE;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NO_PERMISSION;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_VERSION_NODE_EMPTY;

@Service
@Validated
public class AigcWorkflowInstanceServiceImpl implements AigcWorkflowInstanceService {

    @Resource
    private AigcWorkflowInstanceMapper instanceMapper;
    @Resource
    private AigcWorkflowNodeMapper nodeMapper;
    @Resource
    private AigcWorkflowEdgeMapper edgeMapper;
    @Resource
    private AigcWorkflowNodeInstanceMapper nodeInstanceMapper;
    @Resource
    private AigcWorkflowLogMapper logMapper;
    @Resource
    private AigcWorkflowDefinitionService definitionService;
    @Resource
    private AigcGenerateApi generateApi;
    @Resource
    private AigcModelApi modelApi;
    @Resource
    private AigcBillingApi billingApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcWorkflowExecuteRespDTO execute(Long userId, AigcWorkflowExecuteReqDTO reqDTO) {
        AigcWorkflowDefinitionDO definition = definitionService.validatePublishedDefinition(reqDTO.getWorkflowId());
        Long versionId = getExecuteVersionId(definition, reqDTO.getWorkflowVersionId());
        AigcWorkflowCostEstimateRespDTO estimate = estimateCost(new AigcWorkflowCostEstimateReqDTO()
                .setWorkflowId(reqDTO.getWorkflowId())
                .setWorkflowVersionId(versionId)
                .setInputData(reqDTO.getInputData()));
        AigcWorkflowInstanceDO instance = new AigcWorkflowInstanceDO()
                .setInstanceNo(generateInstanceNo())
                .setWorkflowId(definition.getId())
                .setWorkflowVersionId(versionId)
                .setTemplateId(reqDTO.getTemplateId())
                .setUserId(userId)
                .setStatus(AigcWorkflowInstanceStatusEnum.RUNNING.getCode())
                .setInputData(reqDTO.getInputData())
                .setEstimateAmount(estimate.getEstimateAmount())
                .setProgress(0)
                .setStartTime(LocalDateTime.now());
        instanceMapper.insert(instance);
        Long freezeId = freezeBilling(instance, estimate);
        if (freezeId != null) {
            instance.setFreezeId(freezeId);
            instanceMapper.updateById(new AigcWorkflowInstanceDO().setId(instance.getId()).setFreezeId(freezeId));
        }
        createNodeInstances(instance, versionId, reqDTO.getInputData());
        logInstance(instance, "INSTANCE_CREATED", null, instance.getStatus(), "工作流实例已创建", reqDTO.getInputData());
        dispatchReadyNode(instance.getId());
        AigcWorkflowInstanceDO latestInstance = validateInstanceExists(instance.getId());
        return new AigcWorkflowExecuteRespDTO()
                .setInstanceId(instance.getId())
                .setInstanceNo(instance.getInstanceNo())
                .setMainTaskId(latestInstance.getMainTaskId())
                .setStatus(latestInstance.getStatus());
    }

    @Override
    public AigcWorkflowCostEstimateRespDTO estimateCost(AigcWorkflowCostEstimateReqDTO reqDTO) {
        AigcWorkflowDefinitionDO definition = definitionService.validatePublishedDefinition(reqDTO.getWorkflowId());
        Long versionId = getExecuteVersionId(definition, reqDTO.getWorkflowVersionId());
        List<AigcWorkflowNodeDO> nodes = nodeMapper.selectListByVersionId(reqDTO.getWorkflowId(), versionId);
        long estimateAmount = 0L;
        List<Map<String, Object>> details = new ArrayList<>();
        for (AigcWorkflowNodeDO node : nodes) {
            if (!isGenerateNode(node.getNodeType()) || node.getModelId() == null) {
                continue;
            }
            AigcModelPriceCalculateRespDTO price = modelApi.calculatePrice(new AigcModelPriceCalculateReqDTO()
                    .setModelId(node.getModelId())
                    .setCapability(node.getGenerateMode())
                    .setTaskType(node.getGenerateType())
                    .setParams(new HashMap<>())).getCheckedData();
            long nodeAmount = toCent(price == null ? null : price.getSalePrice());
            estimateAmount += nodeAmount;
            Map<String, Object> detail = new HashMap<>();
            detail.put("nodeKey", node.getNodeKey());
            detail.put("modelId", node.getModelId());
            detail.put("amount", nodeAmount);
            detail.put("price", price);
            details.add(detail);
        }
        return new AigcWorkflowCostEstimateRespDTO()
                .setEstimateAmount(estimateAmount)
                .setNodeCount(nodes.size())
                .setDetailData(JsonUtils.toJsonString(details));
    }

    @Override
    public AigcWorkflowInstanceRespDTO getInstanceDetail(Long id) {
        AigcWorkflowInstanceDO instance = validateInstanceExists(id);
        return buildInstanceDetail(instance);
    }

    @Override
    public AigcWorkflowInstanceRespDTO getUserInstanceDetail(Long id, Long userId) {
        AigcWorkflowInstanceDO instance = validateUserInstance(id, userId);
        return buildInstanceDetail(instance);
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
    public AigcWorkflowInstanceDO validateUserInstance(Long id, Long userId) {
        AigcWorkflowInstanceDO instance = validateInstanceExists(id);
        if (!instance.getUserId().equals(userId)) {
            throw exception(WORKFLOW_NO_PERMISSION);
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
        AigcWorkflowInstanceDO instance = validateInstanceExists(reqDTO.getInstanceId());
        AigcWorkflowNodeInstanceDO nodeInstance = validateNodeInstanceExists(reqDTO.getNodeInstanceId());
        nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                .setId(nodeInstance.getId())
                .setStatus(AigcWorkflowNodeStatusEnum.READY.getCode())
                .setRetryCount(defaultInt(nodeInstance.getRetryCount()) + 1)
                .setFinishTime(null));
        instanceMapper.updateById(new AigcWorkflowInstanceDO()
                .setId(instance.getId())
                .setStatus(AigcWorkflowInstanceStatusEnum.RUNNING.getCode())
                .setFinishTime(null));
        logNode(instance, nodeInstance, "NODE_RETRY", nodeInstance.getStatus(), AigcWorkflowNodeStatusEnum.READY.getCode(),
                "节点已重新进入调度队列", null);
        dispatchReadyNode(instance.getId());
    }

    @Override
    public void cancel(AigcWorkflowCancelReqDTO reqDTO) {
        AigcWorkflowInstanceDO instance = validateInstanceExists(reqDTO.getInstanceId());
        instanceMapper.updateById(new AigcWorkflowInstanceDO()
                .setId(reqDTO.getInstanceId())
                .setStatus(AigcWorkflowInstanceStatusEnum.CANCELED.getCode())
                .setFailReason(reqDTO.getCancelReason())
                .setFinishTime(LocalDateTime.now()));
        releaseBilling(instance, reqDTO.getCancelReason());
        nodeInstanceMapper.selectListByInstanceId(reqDTO.getInstanceId()).stream()
                .filter(item -> AigcWorkflowNodeStatusEnum.PENDING.getCode().equals(item.getStatus())
                        || AigcWorkflowNodeStatusEnum.READY.getCode().equals(item.getStatus())
                        || AigcWorkflowNodeStatusEnum.RUNNING.getCode().equals(item.getStatus()))
                .forEach(item -> nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                        .setId(item.getId())
                        .setStatus(AigcWorkflowNodeStatusEnum.CANCELED.getCode())
                        .setFinishTime(LocalDateTime.now())));
        logInstance(instance, "INSTANCE_CANCELED", instance.getStatus(), AigcWorkflowInstanceStatusEnum.CANCELED.getCode(),
                "工作流实例已取消", reqDTO.getCancelReason());
    }

    @Override
    public void cancelUserInstance(AigcWorkflowCancelReqDTO reqDTO, Long userId) {
        validateUserInstance(reqDTO.getInstanceId(), userId);
        cancel(reqDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchReadyNode(Long instanceId) {
        AigcWorkflowInstanceDO instance = validateInstanceExists(instanceId);
        if (!AigcWorkflowInstanceStatusEnum.RUNNING.getCode().equals(instance.getStatus())) {
            throw exception(WORKFLOW_INSTANCE_STATUS_INVALID);
        }
        boolean dispatched;
        do {
            dispatched = false;
            List<AigcWorkflowNodeInstanceDO> nodeInstances = nodeInstanceMapper.selectListByInstanceId(instanceId);
            for (AigcWorkflowNodeInstanceDO nodeInstance : nodeInstances) {
                if (!AigcWorkflowNodeStatusEnum.READY.getCode().equals(nodeInstance.getStatus())) {
                    continue;
                }
                if (isGenerateNode(nodeInstance.getNodeType())) {
                    AigcGenerateSubmitRespDTO submitRespDTO = submitGenerate(instance, nodeInstance);
                    nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                            .setId(nodeInstance.getId())
                            .setStatus(AigcWorkflowNodeStatusEnum.RUNNING.getCode())
                            .setTaskId(submitRespDTO.getTaskId())
                            .setGenRecordId(submitRespDTO.getId())
                            .setStartTime(LocalDateTime.now()));
                    if (instance.getMainTaskId() == null) {
                        instanceMapper.updateById(new AigcWorkflowInstanceDO().setId(instance.getId()).setMainTaskId(submitRespDTO.getTaskId()));
                    }
                    logNode(instance, nodeInstance, "NODE_SUBMITTED", nodeInstance.getStatus(), AigcWorkflowNodeStatusEnum.RUNNING.getCode(),
                            "生成节点已提交", JsonUtils.toJsonString(submitRespDTO));
                } else {
                    completeNonGenerateNode(instance, nodeInstance);
                    activateNextNodes(instance, nodeInstance);
                    dispatched = true;
                }
            }
        } while (dispatched);
        refreshInstanceProgress(instanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleNodeCallback(AigcWorkflowNodeCallbackReqDTO reqDTO) {
        AigcWorkflowNodeInstanceDO nodeInstance = validateNodeInstanceExists(reqDTO.getNodeInstanceId());
        AigcWorkflowInstanceDO instance = validateInstanceExists(nodeInstance.getWorkflowInstanceId());
        if (isFinishedNodeStatus(nodeInstance.getStatus())) {
            logNode(instance, nodeInstance, "NODE_CALLBACK_IGNORED", nodeInstance.getStatus(), reqDTO.getStatus(),
                    "重复节点回调已忽略", reqDTO.getCallbackNo());
            return;
        }
        String targetStatus = reqDTO.getStatus();
        if (targetStatus == null) {
            targetStatus = AigcWorkflowNodeStatusEnum.SUCCESS.getCode();
        }
        nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                .setId(nodeInstance.getId())
                .setStatus(targetStatus)
                .setTaskId(reqDTO.getTaskId())
                .setGenRecordId(reqDTO.getGenRecordId())
                .setOutputData(reqDTO.getOutputData())
                .setAssetIds(reqDTO.getAssetIds())
                .setFinishTime(LocalDateTime.now()));
        AigcWorkflowNodeInstanceDO latestNodeInstance = validateNodeInstanceExists(nodeInstance.getId());
        logNode(instance, nodeInstance, "NODE_CALLBACK", nodeInstance.getStatus(), targetStatus,
                "节点回调已处理", JsonUtils.toJsonString(reqDTO));
        if (AigcWorkflowNodeStatusEnum.SUCCESS.getCode().equals(targetStatus)) {
            activateNextNodes(instance, latestNodeInstance);
            dispatchReadyNode(instance.getId());
        } else if (AigcWorkflowNodeStatusEnum.FAILED.getCode().equals(targetStatus)) {
            failInstance(instance, reqDTO.getFailReason(), JsonUtils.toJsonString(reqDTO));
        }
        refreshInstanceProgress(instance.getId());
    }

    private void createNodeInstances(AigcWorkflowInstanceDO instance, Long versionId, String inputData) {
        List<AigcWorkflowNodeDO> nodes = nodeMapper.selectListByVersionId(instance.getWorkflowId(), versionId);
        if (nodes.isEmpty()) {
            throw exception(WORKFLOW_VERSION_NODE_EMPTY);
        }
        List<AigcWorkflowEdgeDO> edges = edgeMapper.selectListByVersionId(instance.getWorkflowId(), versionId);
        Set<String> targetNodeKeys = edges.stream().map(AigcWorkflowEdgeDO::getTargetNodeKey).collect(Collectors.toSet());
        for (AigcWorkflowNodeDO node : nodes) {
            boolean entryNode = !targetNodeKeys.contains(node.getNodeKey())
                    || AigcWorkflowNodeTypeEnum.START.getCode().equals(node.getNodeType());
            nodeInstanceMapper.insert(new AigcWorkflowNodeInstanceDO()
                    .setWorkflowInstanceId(instance.getId())
                    .setNodeId(node.getId())
                    .setNodeKey(node.getNodeKey())
                    .setNodeType(node.getNodeType())
                    .setStatus(entryNode ? AigcWorkflowNodeStatusEnum.READY.getCode() : AigcWorkflowNodeStatusEnum.PENDING.getCode())
                    .setInputData(inputData)
                    .setRetryCount(0)
                    .setMaxRetryCount(3));
        }
        if (nodeInstanceMapper.selectListByInstanceId(instance.getId()).stream()
                .noneMatch(item -> AigcWorkflowNodeStatusEnum.READY.getCode().equals(item.getStatus()))) {
            throw exception(WORKFLOW_GRAPH_INVALID);
        }
    }

    private AigcGenerateSubmitRespDTO submitGenerate(AigcWorkflowInstanceDO instance, AigcWorkflowNodeInstanceDO nodeInstance) {
        AigcWorkflowNodeDO node = nodeMapper.selectById(nodeInstance.getNodeId());
        return generateApi.submit(new AigcGenerateSubmitReqDTO()
                .setUserId(instance.getUserId())
                .setClientRequestId(instance.getInstanceNo() + ":" + nodeInstance.getNodeKey() + ":" + nodeInstance.getRetryCount())
                .setGenerateType(node.getGenerateType())
                .setGenerateMode(node.getGenerateMode())
                .setModelId(node.getModelId())
                .setInputParams(nodeInstance.getInputData())
                .setSync(false)).getCheckedData();
    }

    private void completeNonGenerateNode(AigcWorkflowInstanceDO instance, AigcWorkflowNodeInstanceDO nodeInstance) {
        nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                .setId(nodeInstance.getId())
                .setStatus(AigcWorkflowNodeStatusEnum.SUCCESS.getCode())
                .setOutputData(nodeInstance.getInputData())
                .setStartTime(nodeInstance.getStartTime() == null ? LocalDateTime.now() : nodeInstance.getStartTime())
                .setFinishTime(LocalDateTime.now()));
        logNode(instance, nodeInstance, "NODE_AUTO_SUCCESS", nodeInstance.getStatus(), AigcWorkflowNodeStatusEnum.SUCCESS.getCode(),
                "非生成节点自动完成", null);
    }

    private void activateNextNodes(AigcWorkflowInstanceDO instance, AigcWorkflowNodeInstanceDO finishedNode) {
        List<AigcWorkflowEdgeDO> edges = edgeMapper.selectListByVersionId(instance.getWorkflowId(), instance.getWorkflowVersionId());
        List<AigcWorkflowNodeInstanceDO> nodeInstances = nodeInstanceMapper.selectListByInstanceId(instance.getId());
        Map<String, AigcWorkflowNodeInstanceDO> nodeInstanceMap = nodeInstances.stream()
                .collect(Collectors.toMap(AigcWorkflowNodeInstanceDO::getNodeKey, item -> item, (a, b) -> a));
        for (AigcWorkflowEdgeDO edge : edges) {
            if (!Objects.equals(edge.getSourceNodeKey(), finishedNode.getNodeKey())) {
                continue;
            }
            AigcWorkflowNodeInstanceDO targetNode = nodeInstanceMap.get(edge.getTargetNodeKey());
            if (targetNode == null || !AigcWorkflowNodeStatusEnum.PENDING.getCode().equals(targetNode.getStatus())) {
                continue;
            }
            if (!allUpstreamSuccess(edges, nodeInstanceMap, edge.getTargetNodeKey())) {
                continue;
            }
            nodeInstanceMapper.updateById(new AigcWorkflowNodeInstanceDO()
                    .setId(targetNode.getId())
                    .setStatus(AigcWorkflowNodeStatusEnum.READY.getCode())
                    .setInputData(resolveInputData(instance, edges, nodeInstanceMap, edge.getTargetNodeKey())));
            logNode(instance, targetNode, "NODE_READY", targetNode.getStatus(), AigcWorkflowNodeStatusEnum.READY.getCode(),
                    "节点依赖已满足", null);
        }
    }

    private boolean allUpstreamSuccess(List<AigcWorkflowEdgeDO> edges, Map<String, AigcWorkflowNodeInstanceDO> nodeInstanceMap, String targetNodeKey) {
        return edges.stream()
                .filter(edge -> Objects.equals(edge.getTargetNodeKey(), targetNodeKey))
                .map(edge -> nodeInstanceMap.get(edge.getSourceNodeKey()))
                .allMatch(node -> node != null && AigcWorkflowNodeStatusEnum.SUCCESS.getCode().equals(node.getStatus()));
    }

    private String resolveInputData(AigcWorkflowInstanceDO instance, List<AigcWorkflowEdgeDO> edges,
                                    Map<String, AigcWorkflowNodeInstanceDO> nodeInstanceMap, String targetNodeKey) {
        List<String> upstreamOutputs = edges.stream()
                .filter(edge -> Objects.equals(edge.getTargetNodeKey(), targetNodeKey))
                .map(edge -> nodeInstanceMap.get(edge.getSourceNodeKey()))
                .filter(Objects::nonNull)
                .map(node -> node.getOutputData() != null ? node.getOutputData() : node.getInputData())
                .filter(Objects::nonNull)
                .toList();
        if (upstreamOutputs.isEmpty()) {
            return instance.getInputData();
        }
        if (upstreamOutputs.size() == 1) {
            return upstreamOutputs.get(0);
        }
        return JsonUtils.toJsonString(upstreamOutputs);
    }

    private void refreshInstanceProgress(Long instanceId) {
        AigcWorkflowInstanceDO instance = validateInstanceExists(instanceId);
        List<AigcWorkflowNodeInstanceDO> nodeInstances = nodeInstanceMapper.selectListByInstanceId(instanceId);
        int total = nodeInstances.size();
        if (total == 0) {
            return;
        }
        long finished = nodeInstances.stream().filter(item -> isFinishedNodeStatus(item.getStatus())).count();
        int progress = (int) (finished * 100 / total);
        boolean hasFailed = nodeInstances.stream().anyMatch(item -> AigcWorkflowNodeStatusEnum.FAILED.getCode().equals(item.getStatus()));
        boolean allSuccess = nodeInstances.stream().allMatch(item -> AigcWorkflowNodeStatusEnum.SUCCESS.getCode().equals(item.getStatus())
                || AigcWorkflowNodeStatusEnum.SKIPPED.getCode().equals(item.getStatus()));
        AigcWorkflowInstanceDO update = new AigcWorkflowInstanceDO().setId(instanceId).setProgress(progress);
        if (hasFailed) {
            update.setStatus(AigcWorkflowInstanceStatusEnum.FAILED.getCode()).setFinishTime(LocalDateTime.now());
        } else if (allSuccess) {
            confirmBilling(instance);
            update.setStatus(AigcWorkflowInstanceStatusEnum.SUCCESS.getCode())
                    .setProgress(100)
                    .setActualAmount(instance.getEstimateAmount())
                    .setOutputData(buildWorkflowOutput(nodeInstances))
                    .setFinishTime(LocalDateTime.now());
            logInstance(instance, "INSTANCE_SUCCESS", instance.getStatus(), AigcWorkflowInstanceStatusEnum.SUCCESS.getCode(),
                    "工作流实例执行成功", null);
        }
        instanceMapper.updateById(update);
    }

    private void failInstance(AigcWorkflowInstanceDO instance, String failReason, String failMessage) {
        instanceMapper.updateById(new AigcWorkflowInstanceDO()
                .setId(instance.getId())
                .setStatus(AigcWorkflowInstanceStatusEnum.FAILED.getCode())
                .setFailReason(failReason)
                .setFailMessage(failMessage)
                .setFinishTime(LocalDateTime.now()));
        releaseBilling(instance, failReason);
        logInstance(instance, "INSTANCE_FAILED", instance.getStatus(), AigcWorkflowInstanceStatusEnum.FAILED.getCode(),
                "工作流实例执行失败", failMessage);
    }

    private String buildWorkflowOutput(List<AigcWorkflowNodeInstanceDO> nodeInstances) {
        Map<String, Object> output = new HashMap<>();
        nodeInstances.forEach(node -> output.put(node.getNodeKey(), node.getOutputData()));
        return JsonUtils.toJsonString(output);
    }

    private boolean isFinishedNodeStatus(String status) {
        return AigcWorkflowNodeStatusEnum.SUCCESS.getCode().equals(status)
                || AigcWorkflowNodeStatusEnum.FAILED.getCode().equals(status)
                || AigcWorkflowNodeStatusEnum.SKIPPED.getCode().equals(status)
                || AigcWorkflowNodeStatusEnum.CANCELED.getCode().equals(status);
    }

    private boolean isGenerateNode(String nodeType) {
        return AigcWorkflowNodeTypeEnum.TEXT_GENERATE.getCode().equals(nodeType)
                || AigcWorkflowNodeTypeEnum.IMAGE_GENERATE.getCode().equals(nodeType)
                || AigcWorkflowNodeTypeEnum.VIDEO_GENERATE.getCode().equals(nodeType)
                || AigcWorkflowNodeTypeEnum.AUDIO_GENERATE.getCode().equals(nodeType)
                || AigcWorkflowNodeTypeEnum.DOCUMENT_GENERATE.getCode().equals(nodeType)
                || AigcWorkflowNodeTypeEnum.PPT_GENERATE.getCode().equals(nodeType)
                || AigcWorkflowNodeTypeEnum.DIGITAL_HUMAN.getCode().equals(nodeType);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long toCent(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.movePointRight(2).longValue();
    }

    private BigDecimal toAmount(Long cent) {
        if (cent == null || cent <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cent, 2);
    }

    private Long freezeBilling(AigcWorkflowInstanceDO instance, AigcWorkflowCostEstimateRespDTO estimate) {
        if (estimate.getEstimateAmount() == null || estimate.getEstimateAmount() <= 0) {
            return null;
        }
        AigcBillingFreezeRespDTO freeze = billingApi.freeze(new AigcBillingFreezeReqDTO()
                .setUserId(instance.getUserId())
                .setBizType(AigcBillingBizTypeEnum.TASK_GENERATE.getCode())
                .setBizId(instance.getInstanceNo())
                .setAmount(toAmount(estimate.getEstimateAmount()))
                .setTitle("工作流生成冻结")
                .setPriceSnapshot(estimate.getDetailData())).getCheckedData();
        logInstance(instance, "BILLING_FROZEN", null, null, "工作流预估费用已冻结", JsonUtils.toJsonString(freeze));
        return freeze == null ? null : freeze.getId();
    }

    private void confirmBilling(AigcWorkflowInstanceDO instance) {
        if (instance.getFreezeId() == null || instance.getEstimateAmount() == null || instance.getEstimateAmount() <= 0) {
            return;
        }
        billingApi.confirmFreeze(new AigcBillingConfirmReqDTO()
                .setFreezeId(instance.getFreezeId())
                .setTaskId(instance.getMainTaskId())
                .setActualAmount(toAmount(instance.getEstimateAmount()))
                .setPriceSnapshot(JsonUtils.toJsonString(Map.of("workflowInstanceId", instance.getId())))).getCheckedData();
        logInstance(instance, "BILLING_CONFIRMED", null, null, "工作流费用已确认扣除", null);
    }

    private void releaseBilling(AigcWorkflowInstanceDO instance, String reason) {
        if (instance.getFreezeId() == null) {
            return;
        }
        billingApi.releaseFreeze(new AigcBillingReleaseReqDTO()
                .setFreezeId(instance.getFreezeId())
                .setTaskId(instance.getMainTaskId())
                .setReason(reason)).getCheckedData();
        logInstance(instance, "BILLING_RELEASED", null, null, "工作流冻结费用已释放", reason);
    }

    private void logInstance(AigcWorkflowInstanceDO instance, String logType, String fromStatus, String toStatus,
                             String summary, String detailData) {
        logMapper.insert(new AigcWorkflowLogDO()
                .setWorkflowInstanceId(instance.getId())
                .setLogType(logType)
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setSummary(summary)
                .setDetailData(detailData)
                .setUserId(instance.getUserId()));
    }

    private void logNode(AigcWorkflowInstanceDO instance, AigcWorkflowNodeInstanceDO nodeInstance, String logType,
                         String fromStatus, String toStatus, String summary, String detailData) {
        logMapper.insert(new AigcWorkflowLogDO()
                .setWorkflowInstanceId(instance.getId())
                .setNodeInstanceId(nodeInstance.getId())
                .setLogType(logType)
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setSummary(summary)
                .setDetailData(detailData)
                .setUserId(instance.getUserId()));
    }

    private AigcWorkflowInstanceRespDTO buildInstanceDetail(AigcWorkflowInstanceDO instance) {
        return BeanUtils.toBean(instance, AigcWorkflowInstanceRespDTO.class)
                .setNodeInstances(BeanUtils.toBean(nodeInstanceMapper.selectListByInstanceId(instance.getId()),
                        AigcWorkflowNodeInstanceRespDTO.class));
    }

    private Long getExecuteVersionId(AigcWorkflowDefinitionDO definition, Long requestVersionId) {
        Long versionId = requestVersionId != null ? requestVersionId : definition.getCurrentVersionId();
        if (versionId == null) {
            throw exception(WORKFLOW_CURRENT_VERSION_NOT_EXISTS);
        }
        return versionId;
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
