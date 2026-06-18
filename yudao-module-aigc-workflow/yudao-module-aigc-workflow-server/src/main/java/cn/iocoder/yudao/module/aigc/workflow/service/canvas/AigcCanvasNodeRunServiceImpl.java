package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetFileRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.gen.api.AigcGenerateApi;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateResultRespDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunBatchSyncReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunBatchSyncRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunSyncReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasAssetRefDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasGenerationRunDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasAssetRefMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasGenerationRunMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasOperationLogMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasProjectMapper;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.AigcCanvasGenerationRunSseService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.AigcCanvasRoomService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationAppliedMessage;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasGenerationRunEventMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NODE_RUN_TASK_NOT_BELONG;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NODE_RUN_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NODE_RUN_INPUT_PARAMS_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NODE_RUN_REFERENCE_IMAGE_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NODE_RUN_REFERENCE_IMAGE_REQUIRED;

@Service
@Validated
@Slf4j
public class AigcCanvasNodeRunServiceImpl implements AigcCanvasNodeRunService {

    private static final Set<String> PROVIDER_SYNC_STATUSES = Set.of(
            AigcGenerateStatusEnum.SUBMITTED.getCode(),
            AigcGenerateStatusEnum.CALLBACK_WAITING.getCode(),
            AigcGenerateStatusEnum.SYNCING.getCode());
    private static final int BATCH_SYNC_LIMIT = 20;
    private static final Set<String> NON_PARAM_NODE_DATA_KEYS = Set.of(
            "inputImages", "inputImageUrls", "referenceImages", "imageUrls");

    @Resource
    private AigcCanvasProjectService projectService;
    @Resource
    private AigcCanvasOperationService operationService;
    @Resource
    private AigcCanvasRoomService roomService;
    @Resource
    private AigcGenerateApi generateApi;
    @Resource
    private AigcCanvasAssetRefMapper assetRefMapper;
    @Resource
    private AigcCanvasProjectMapper projectMapper;
    @Resource
    private AigcCanvasOperationLogMapper operationLogMapper;
    @Resource
    private AigcCanvasGenerationRunMapper generationRunMapper;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private AigcCanvasGenerationRunSseService generationRunSseService;
    @Resource
    private AigcAssetApi assetApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasNodeRunRespVO runNode(AigcCanvasNodeRunReqVO reqVO, Long userId) {
        // 校验项目是否存在，且用户是否有编辑权限
        projectService.validateEditableProject(reqVO.getProjectId(), userId);

        Long userFlag = userId + 15;
        // 生成runId
        String runId = StrUtil.blankToDefault(reqVO.getRunId(),
                "run_" + userFlag + "_" + System.currentTimeMillis());
        reqVO.setInputParams(normalizeNodeRunInputParams(reqVO));

        // 提交请求返回
        CommonResult<AigcGenerateSubmitRespDTO> submitResult = generateApi.submit(new AigcGenerateSubmitReqDTO()
                .setUserId(userId)
                .setClientRequestId("canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_" + runId)
                .setGenerateType(reqVO.getGenerateType())
                .setGenerateMode(reqVO.getGenerateMode())
                .setModelId(reqVO.getModelId())
                .setPrompt(reqVO.getPrompt())
                .setInputParams(reqVO.getInputParams())
                .setSync(Boolean.TRUE.equals(reqVO.getSync())));

        // 返回结果
        AigcGenerateSubmitRespDTO submit = submitResult.getCheckedData();
        // 提交任务状态更新
        AigcCanvasOperationLogDO operation = submitTaskStatusPatch(reqVO.getProjectId(), reqVO.getNodeId(),
                reqVO.getBaseVersion(), userId,
                "task_status_" + reqVO.getNodeId() + "_" + runId, buildSubmitPatch(submit, reqVO));
        AigcCanvasGenerationRunDO generationRun = upsertGenerationRun(reqVO, runId, submit, userId, operation);
        publishGenerationRunAfterCommit("generation-run-status", generationRun,
                BeanUtils.toBean(operation, AigcCanvasOperationRespVO.class));
        broadcastAfterCommit(operation);
        // 返回结果
        return new AigcCanvasNodeRunRespVO()
                .setTaskId(submit.getTaskId())
                .setNodeId(reqVO.getNodeId())
                .setGenerateRecordId(submit.getId())
                .setGenerateNo(submit.getGenerateNo())
                .setStatus(submit.getStatus())
                .setSuccess(true)
                .setOperation(BeanUtils.toBean(operation, AigcCanvasOperationRespVO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasNodeRunRespVO syncNodeRun(AigcCanvasNodeRunSyncReqVO reqVO, Long userId) {
        projectService.validateEditableProject(reqVO.getProjectId(), userId);
        AigcGenerateResultRespDTO result = getResultReadyForCanvas(reqVO.getTaskId());
        AigcCanvasNodeRunRespVO respVO = syncOneNodeRun(reqVO, result, userId, true);
        return respVO;
    }

    @Override
    public AigcCanvasNodeRunBatchSyncRespVO syncNodeRuns(AigcCanvasNodeRunBatchSyncReqVO reqVO, Long userId) {
        projectService.validateEditableProject(reqVO.getProjectId(), userId);
        int requestedCount = reqVO.getNodes().size();
        List<AigcCanvasNodeRunSyncReqVO> nodes = reqVO.getNodes().stream()
                .limit(BATCH_SYNC_LIMIT)
                .toList();
        List<Long> taskIds = nodes.stream().map(AigcCanvasNodeRunSyncReqVO::getTaskId)
                .filter(Objects::nonNull).distinct().toList();
        List<AigcGenerateResultRespDTO> generateResults = generateApi.getResults(taskIds).getCheckedData();
        Map<Long, AigcGenerateResultRespDTO> resultMap = (generateResults == null ? List.<AigcGenerateResultRespDTO>of() : generateResults).stream()
                .collect(Collectors.toMap(AigcGenerateResultRespDTO::getTaskId, Function.identity(), (left, right) -> left));

        List<AigcCanvasNodeRunRespVO> results = new ArrayList<>(nodes.size());
        for (AigcCanvasNodeRunSyncReqVO node : nodes) {
            node.setProjectId(reqVO.getProjectId());
            node.setBaseVersion(reqVO.getBaseVersion());
            results.add(syncOneNodeRunTransactionally(node, resultMap.get(node.getTaskId()), userId));
        }
        long failedCount = results.stream()
                .filter(result -> !Boolean.TRUE.equals(result.getSuccess()))
                .count();
        return new AigcCanvasNodeRunBatchSyncRespVO()
                .setProjectId(reqVO.getProjectId())
                .setResults(results)
                .setRequestedCount(requestedCount)
                .setProcessedCount(results.size())
                .setTruncated(requestedCount > BATCH_SYNC_LIMIT)
                .setLimit(BATCH_SYNC_LIMIT)
                .setFailedCount(Math.toIntExact(failedCount));
    }

    @Override
    public SseEmitter subscribeGenerationRunEvents(Long projectId, Long userId, String lastEventId) {
        projectService.validateReadableProject(projectId, userId);
        return generationRunSseService.subscribe(projectId, userId, lastEventId);
    }

    private AigcCanvasNodeRunRespVO syncOneNodeRunTransactionally(AigcCanvasNodeRunSyncReqVO reqVO,
            AigcGenerateResultRespDTO result, Long userId) {
        try {
            return transactionTemplate.execute(status -> {
                AigcCanvasNodeRunRespVO respVO = syncOneNodeRun(reqVO, refreshResultReadyForCanvas(result), userId, true);
                return respVO;
            });
        } catch (ServiceException ex) {
            return buildFailedNodeRunResp(reqVO, ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.warn("[syncOneNodeRunTransactionally][projectId({}) nodeId({}) taskId({}) 同步失败]",
                    reqVO.getProjectId(), reqVO.getNodeId(), reqVO.getTaskId(), ex);
            return buildFailedNodeRunResp(reqVO, 500, StrUtil.blankToDefault(ex.getMessage(), "节点同步失败"));
        }
    }

    private AigcCanvasNodeRunRespVO syncOneNodeRun(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result,
            Long userId, boolean applySideEffects) {
        validateResultBelongsToCanvasNode(reqVO, result);
        String opId = "task_result_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_" + reqVO.getTaskId()
                + "_" + result.getStatus();
        AigcCanvasOperationLogDO existedOperation = findTaskStatusPatch(reqVO.getProjectId(), opId);
        AigcCanvasOperationLogDO operation = existedOperation;
        boolean created = false;
        if (operation == null) {
            operation = submitTaskStatusPatch(reqVO.getProjectId(), reqVO.getNodeId(), reqVO.getBaseVersion(), userId,
                    opId, buildResultPatch(reqVO.getNodeType(), result));
            created = Objects.equals(operation.getActorUserId(), userId);
        }
        AigcCanvasGenerationRunDO generationRun = updateGenerationRun(reqVO, result, operation);
        publishGenerationRunAfterCommit(getGenerationRunEventName(result),
                generationRun, BeanUtils.toBean(operation, AigcCanvasOperationRespVO.class));
        if (applySideEffects && created) {
            applySuccessfulAssetSideEffects(reqVO, result);
        }
        return new AigcCanvasNodeRunRespVO()
                .setTaskId(result.getTaskId())
                .setNodeId(reqVO.getNodeId())
                .setGenerateRecordId(result.getId())
                .setGenerateNo(result.getGenerateNo())
                .setStatus(result.getStatus())
                .setSuccess(true)
                .setOperation(BeanUtils.toBean(operation, AigcCanvasOperationRespVO.class));
    }

    private void broadcastAfterCommit(AigcCanvasOperationRespVO operation) {
        if (operation == null) {
            return;
        }
        AigcCanvasOperationLogDO logDO = BeanUtils.toBean(operation, AigcCanvasOperationLogDO.class);
        broadcastAfterCommit(logDO);
    }

    private void publishGenerationRunAfterCommit(String eventName, AigcCanvasGenerationRunDO generationRun,
            AigcCanvasOperationRespVO operation) {
        if (generationRun == null) {
            return;
        }
        AigcCanvasGenerationRunEventMessage message = new AigcCanvasGenerationRunEventMessage()
                .setEventId("generation-run-" + generationRun.getProjectId() + "-" + generationRun.getNodeId()
                        + "-" + generationRun.getTaskId() + "-" + generationRun.getStatus())
                .setProjectId(generationRun.getProjectId())
                .setNodeId(generationRun.getNodeId())
                .setRunId(generationRun.getRunId())
                .setTaskId(generationRun.getTaskId())
                .setStatus(generationRun.getStatus())
                .setProgress(generationRun.getProgress())
                .setOperation(operation)
                .setVersion(operation == null ? null : operation.getNextVersion())
                .setEmittedAt(System.currentTimeMillis());
        Runnable publish = () -> generationRunSseService.publish(eventName, message);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

    private AigcGenerateResultRespDTO getResultReadyForCanvas(Long taskId) {
        AigcGenerateResultRespDTO result = generateApi.getResult(taskId).getCheckedData();
        if (result == null) {
            throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS);
        }
        return refreshResultReadyForCanvas(result);
    }

    private AigcGenerateResultRespDTO refreshResultReadyForCanvas(AigcGenerateResultRespDTO result) {
        if (result == null) {
            throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS);
        }
        Long taskId = result.getTaskId();
        if (isProviderSyncStatus(result)) {
            syncProviderTaskQuietly(taskId);
            result = generateApi.getResult(taskId).getCheckedData();
            if (result == null) {
                throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS);
            }
        }
        if (!isSuccessWithPendingDataUrlAsset(result)) {
            return result;
        }
        for (int i = 0; i < 6; i++) {
            sleepQuietly(500);
            result = generateApi.getResult(taskId).getCheckedData();
            if (!isSuccessWithPendingDataUrlAsset(result)) {
                return result;
            }
        }
        return result.setStatus("ASSET_CREATING");
    }

    private String normalizeNodeRunInputParams(AigcCanvasNodeRunReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getInputParams())) {
            if ("IMAGE_TO_IMAGE".equals(reqVO.getGenerateMode())) {
                throw serviceException(CANVAS_NODE_RUN_REFERENCE_IMAGE_REQUIRED);
            }
            return reqVO.getInputParams();
        }
        if (!JSONUtil.isTypeJSONObject(reqVO.getInputParams())) {
            throw serviceException(CANVAS_NODE_RUN_INPUT_PARAMS_INVALID);
        }
        JSONObject params = JSONUtil.parseObj(reqVO.getInputParams());
        if (!"IMAGE_TO_IMAGE".equals(reqVO.getGenerateMode())) {
            return params.toString();
        }
        LinkedHashSet<String> references = collectStableReferenceImages(params);
        references.addAll(resolveAssetReferenceUrls(params));
        if (references.isEmpty()) {
            throw serviceException(CANVAS_NODE_RUN_REFERENCE_IMAGE_REQUIRED);
        }
        JSONArray normalizedReferences = new JSONArray();
        references.forEach(normalizedReferences::add);
        params.set("referenceImages", normalizedReferences);
        params.set("inputImageUrls", normalizedReferences);
        params.set("inputImages", normalizeInputImageSnapshots(params.getJSONArray("inputImages"), references));
        return params.toString();
    }

    private LinkedHashSet<String> collectStableReferenceImages(JSONObject params) {
        LinkedHashSet<String> references = new LinkedHashSet<>();
        addStableReferences(references, params.getJSONArray("referenceImages"));
        addStableReferences(references, params.getJSONArray("inputImageUrls"));
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages != null) {
            for (Object item : inputImages) {
                JSONObject image = JSONUtil.parseObj(item);
                addStableReference(references, StrUtil.blankToDefault(image.getStr("url"), image.getStr("dataUrl")));
            }
        }
        return references;
    }

    private void addStableReferences(Set<String> references, JSONArray values) {
        if (values == null) {
            return;
        }
        for (Object item : values) {
            addStableReference(references, Objects.toString(item, null));
        }
    }

    private void addStableReference(Set<String> references, String source) {
        if (isStableImageReference(source)) {
            references.add(source.trim());
        }
    }

    private boolean isStableImageReference(String source) {
        String value = StrUtil.trim(source);
        return StrUtil.startWithAnyIgnoreCase(value, "http://", "https://", "data:image/");
    }

    private List<String> resolveAssetReferenceUrls(JSONObject params) {
        LinkedHashSet<Long> assetIds = new LinkedHashSet<>();
        collectAssetIds(assetIds, params.getJSONArray("referenceAssetIds"));
        collectAssetIds(assetIds, params.getJSONArray("inputAssetIds"));
        collectAssetIds(assetIds, params.getJSONArray("inputImageIds"));
        if (assetIds.isEmpty()) {
            return List.of();
        }
        List<AigcAssetRespDTO> assets = assetApi.getAssets(new ArrayList<>(assetIds)).getCheckedData();
        if (assets == null || assets.isEmpty()) {
            throw serviceException(CANVAS_NODE_RUN_REFERENCE_IMAGE_INVALID);
        }
        List<String> urls = assets.stream()
                .map(this::resolveAssetAccessUrl)
                .filter(this::isStableImageReference)
                .distinct()
                .toList();
        if (urls.isEmpty()) {
            throw serviceException(CANVAS_NODE_RUN_REFERENCE_IMAGE_INVALID);
        }
        return urls;
    }

    private void collectAssetIds(Set<Long> assetIds, JSONArray values) {
        if (values == null) {
            return;
        }
        for (Object item : values) {
            Long assetId = parseLongQuietly(item);
            if (assetId != null) {
                assetIds.add(assetId);
            }
        }
    }

    private Long parseLongQuietly(Object value) {
        try {
            String text = Objects.toString(value, "").trim();
            return text.isEmpty() ? null : Long.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveAssetAccessUrl(AigcAssetRespDTO asset) {
        if (asset == null) {
            return null;
        }
        if (StrUtil.isNotBlank(asset.getFileUrl())) {
            return asset.getFileUrl();
        }
        if (asset.getFiles() == null) {
            return null;
        }
        return asset.getFiles().stream()
                .map(AigcAssetFileRespDTO::getAccessUrl)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private JSONArray normalizeInputImageSnapshots(JSONArray inputImages, Set<String> references) {
        JSONArray normalized = new JSONArray();
        if (inputImages != null) {
            for (Object item : inputImages) {
                JSONObject image = JSONUtil.parseObj(item);
                String source = StrUtil.blankToDefault(image.getStr("url"), image.getStr("dataUrl"));
                if (!isStableImageReference(source)) {
                    continue;
                }
                image.set("url", source.trim());
                image.remove("dataUrl");
                normalized.add(image);
            }
        }
        for (String reference : references) {
            boolean exists = normalized.stream()
                    .map(JSONUtil::parseObj)
                    .anyMatch(image -> reference.equals(image.getStr("url")));
            if (!exists) {
                normalized.add(new JSONObject().set("url", reference));
            }
        }
        return normalized;
    }

    private void validateResultBelongsToCanvasNode(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result) {
        if (result == null || result.getTaskId() == null) {
            throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS);
        }
        AigcCanvasGenerationRunDO generationRun = generationRunMapper.selectByTaskId(result.getTaskId());
        if (generationRun != null) {
            if (!Objects.equals(generationRun.getProjectId(), reqVO.getProjectId())
                    || !Objects.equals(generationRun.getNodeId(), reqVO.getNodeId())) {
                throw serviceException(CANVAS_NODE_RUN_TASK_NOT_BELONG);
            }
            return;
        }
        String expectedPrefix = "canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_";
        if (StrUtil.startWith(result.getClientRequestId(), "canvas_")
                && !StrUtil.startWith(result.getClientRequestId(), expectedPrefix)) {
            throw serviceException(CANVAS_NODE_RUN_TASK_NOT_BELONG);
        }
    }

    private ServiceException serviceException(ErrorCode errorCode) {
        return new ServiceException(errorCode);
    }

    private AigcCanvasOperationLogDO findTaskStatusPatch(Long projectId, String opId) {
        return operationLogMapper.selectByClientOperation(projectId, "server_node_run", opId);
    }

    private AigcCanvasNodeRunRespVO buildFailedNodeRunResp(AigcCanvasNodeRunSyncReqVO reqVO, Integer errorCode,
            String errorMessage) {
        return new AigcCanvasNodeRunRespVO()
                .setTaskId(reqVO.getTaskId())
                .setNodeId(reqVO.getNodeId())
                .setSuccess(false)
                .setErrorCode(String.valueOf(errorCode))
                .setErrorMessage(errorMessage);
    }

    private AigcCanvasGenerationRunDO upsertGenerationRun(AigcCanvasNodeRunReqVO reqVO, String runId,
            AigcGenerateSubmitRespDTO submit, Long userId, AigcCanvasOperationLogDO operation) {
        AigcCanvasGenerationRunDO generationRun = generationRunMapper.selectByProjectNodeAndRun(
                reqVO.getProjectId(), reqVO.getNodeId(), runId);
        if (generationRun == null && submit.getTaskId() != null) {
            generationRun = generationRunMapper.selectByTaskId(submit.getTaskId());
        }
        boolean insert = generationRun == null;
        if (insert) {
            generationRun = new AigcCanvasGenerationRunDO()
                    .setStartedAt(LocalDateTime.now());
        }
        generationRun.setProjectId(reqVO.getProjectId());
        generationRun.setNodeId(reqVO.getNodeId());
        generationRun.setRunId(runId);
        generationRun.setTaskId(submit.getTaskId());
        generationRun.setGenerateRecordId(submit.getId());
        generationRun.setGenerateNo(submit.getGenerateNo());
        generationRun.setNodeType(reqVO.getNodeType());
        generationRun.setGenerateType(reqVO.getGenerateType());
        generationRun.setGenerateMode(reqVO.getGenerateMode());
        generationRun.setCreatorUserId(userId);
        generationRun.setStatus(submit.getStatus());
        generationRun.setProgress(5);
        generationRun.setOpId(operation == null ? null : operation.getOpId());
        if (insert) {
            generationRunMapper.insert(generationRun);
        } else {
            generationRunMapper.updateById(generationRun);
        }
        return generationRun;
    }

    private AigcCanvasGenerationRunDO updateGenerationRun(AigcCanvasNodeRunSyncReqVO reqVO,
            AigcGenerateResultRespDTO result, AigcCanvasOperationLogDO operation) {
        AigcCanvasGenerationRunDO generationRun = generationRunMapper.selectByProjectNodeAndTask(
                reqVO.getProjectId(), reqVO.getNodeId(), result.getTaskId());
        if (generationRun == null) {
            generationRun = new AigcCanvasGenerationRunDO()
                    .setProjectId(reqVO.getProjectId())
                    .setNodeId(reqVO.getNodeId())
                    .setRunId(extractRunIdFromClientRequestId(reqVO, result.getTaskId(), result.getClientRequestId()))
                    .setTaskId(result.getTaskId())
                    .setNodeType(reqVO.getNodeType())
                    .setGenerateType(StrUtil.blankToDefault(result.getGenerateType(), "UNKNOWN"))
                    .setGenerateMode(StrUtil.blankToDefault(result.getGenerateMode(), "UNKNOWN"))
                    .setCreatorUserId(operation == null ? 0L : operation.getActorUserId())
                    .setStartedAt(result.getCreateTime() == null ? LocalDateTime.now() : result.getCreateTime());
        }
        generationRun.setGenerateRecordId(result.getId());
        generationRun.setGenerateNo(result.getGenerateNo());
        generationRun.setStatus(result.getStatus());
        generationRun.setProgress(isTerminalStatus(result.getStatus()) ? 100 : 40);
        generationRun.setOpId(operation == null ? null : operation.getOpId());
        if (isTerminalStatus(result.getStatus())) {
            generationRun.setFinishedAt(result.getFinishTime() == null ? LocalDateTime.now() : result.getFinishTime());
        }
        if (generationRun.getId() == null) {
            generationRunMapper.insert(generationRun);
        } else {
            generationRunMapper.updateById(generationRun);
        }
        return generationRun;
    }

    private String extractRunIdFromClientRequestId(AigcCanvasNodeRunSyncReqVO reqVO, Long taskId, String clientRequestId) {
        String legacyRunId = "legacy_" + taskId;
        if (StrUtil.isBlank(clientRequestId)) {
            return legacyRunId;
        }
        String expectedPrefix = "canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_";
        if (StrUtil.startWith(clientRequestId, expectedPrefix) && clientRequestId.length() > expectedPrefix.length()) {
            return clientRequestId.substring(expectedPrefix.length());
        }
        return StrUtil.startWith(clientRequestId, "canvas_") ? clientRequestId : legacyRunId;
    }

    private boolean isTerminalStatus(String status) {
        return "SUCCESS".equals(status) || "FAILED".equals(status)
                || "CANCELED".equals(status) || "CANCELLED".equals(status);
    }

    private String getGenerationRunEventName(AigcGenerateResultRespDTO result) {
        if ("SUCCESS".equals(result.getStatus())) {
            return "generation-run-applied";
        }
        if ("FAILED".equals(result.getStatus()) || "CANCELED".equals(result.getStatus())
                || "CANCELLED".equals(result.getStatus())) {
            return "generation-run-failed";
        }
        return "generation-run-status";
    }

    private boolean isProviderSyncStatus(AigcGenerateResultRespDTO result) {
        return result != null && PROVIDER_SYNC_STATUSES.contains(result.getStatus());
    }

    private void syncProviderTaskQuietly(Long taskId) {
        try {
            generateApi.syncTask(taskId).getCheckedData();
        } catch (Exception ex) {
            log.warn("[syncProviderTaskQuietly][taskId({}) 同步第三方任务失败，保留当前画布轮询状态]", taskId, ex);
        }
    }

    private boolean isSuccessWithPendingDataUrlAsset(AigcGenerateResultRespDTO result) {
        if (result == null || !"SUCCESS".equals(result.getStatus()) || StrUtil.isNotBlank(result.getAssetIds())) {
            return false;
        }
        List<String> urls = parseStringList(result.getOutputUrls());
        return !urls.isEmpty() && StrUtil.startWithIgnoreCase(urls.get(0), "data:");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private JSONObject buildSubmitPatch(AigcGenerateSubmitRespDTO submit, AigcCanvasNodeRunReqVO reqVO) {
        JSONObject patch = new JSONObject()
                .set("status", "pending")
                .set("taskId", String.valueOf(submit.getTaskId()))
                .set("errorMessage", null)
                .set("taskStatus", submit.getStatus())
                .set("progress", 5)
                .set("generationStartedAt", LocalDateTime.now().toString())
                .set("generationCompletedAt", null)
                .set("elapsedMs", null)
                .set("upstreamStatus", submit.getStatus())
                .set("updatedAt", LocalDateTime.now().toString());
        if (StrUtil.isNotBlank(reqVO.getPrompt())) {
            patch.set("prompt", reqVO.getPrompt());
        }
        if (reqVO.getModelId() != null) {
            patch.set("aigcModelId", reqVO.getModelId())
                    .set("modelId", String.valueOf(reqVO.getModelId()));
        }
        if (StrUtil.isNotBlank(reqVO.getInputParams())) {
            applyInputParamsPatch(patch, reqVO.getInputParams());
        }
        return patch;
    }

    private void applyInputParamsPatch(JSONObject patch, String inputParams) {
        if (!JSONUtil.isTypeJSONObject(inputParams)) {
            return;
        }
        JSONObject params = JSONUtil.parseObj(inputParams);
        NON_PARAM_NODE_DATA_KEYS.forEach(params::remove);
        patch.set("inputParams", params.toString());
        if (!params.isEmpty() && !patch.containsKey("params")) {
            patch.set("params", params);
        }
    }

    private JSONObject buildResultPatch(String nodeType, AigcGenerateResultRespDTO result) {
        boolean success = "SUCCESS".equals(result.getStatus());
        boolean failed = "FAILED".equals(result.getStatus()) || "CANCELED".equals(result.getStatus())
                || "CANCELLED".equals(result.getStatus());
        JSONObject patch = new JSONObject()
                .set("taskId", String.valueOf(result.getTaskId()))
                .set("taskStatus", result.getStatus())
                .set("upstreamStatus", result.getStatus())
                .set("updatedAt", LocalDateTime.now().toString())
                .set("progress", success || failed ? 100 : 40);
        if (success) {
            patch.set("status", "video".equals(nodeType) ? "complete" : "idle")
                    .set("errorMessage", null)
                    .set("generationCompletedAt",
                            result.getFinishTime() == null ? LocalDateTime.now().toString()
                                    : result.getFinishTime().toString())
                    .set("elapsedMs", result.getCreateTime() == null || result.getFinishTime() == null ? null
                            : java.time.Duration.between(result.getCreateTime(), result.getFinishTime()).toMillis());
            if (StrUtil.isNotBlank(result.getOutputText())) {
                patch.set("content", result.getOutputText());
            }
            List<Long> assetIds = parseLongList(result.getAssetIds());
            if (!assetIds.isEmpty()) {
                patch.set("assetId", assetIds.get(0)).set("outputAssetId", assetIds.get(0));
            }
            if ("image".equals(nodeType)) {
                applyImageOutputsPatch(patch, assetIds);
            }
            patch.set("kind", "generated");
            return patch;
        }
        if (failed) {
            patch.set("status", "failed")
                    .set("errorMessage", formatGenerateFailureMessage(result.getFailReason(), result.getFailMessage()))
                    .set("generationCompletedAt", result.getFinishTime() == null ? LocalDateTime.now().toString()
                            : result.getFinishTime().toString());
            return patch;
        }
        patch.set("status", "pending");
        return patch;
    }

    private String formatGenerateFailureMessage(String failReason, String failMessage) {
        if ("MEDIA_URL_INVALID".equalsIgnoreCase(failReason)) {
            return "生成参数未通过模型校验，请检查参考图是否可访问，并确认视频模式、比例、分辨率和时长后重试。";
        }
        if ("PARAM_REJECTED".equalsIgnoreCase(failReason)) {
            return "生成参数未通过模型校验，请检查提示词、参考图和视频参数后重试。";
        }
        return StrUtil.blankToDefault(failMessage, "生成失败，请稍后重试。");
    }

    private void applySuccessfulAssetSideEffects(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result) {
        if (result == null || !"SUCCESS".equals(result.getStatus())) {
            return;
        }
        List<Long> assetIds = parseLongList(result.getAssetIds());
        if (assetIds.isEmpty()) {
            return;
        }
        boolean assetBound = bindOutputAssets(reqVO, assetIds, result.getTaskId());
        if (!assetBound) {
            return;
        }
        Long assetId = assetIds.get(0);
        projectService.refreshProjectStatistics(reqVO.getProjectId());
        if ("image".equals(reqVO.getNodeType())) {
            AigcCanvasProjectDO update = new AigcCanvasProjectDO();
            update.setId(reqVO.getProjectId());
            update.setCoverAssetId(assetId);
            projectMapper.updateById(update);
        }
    }

    private void applyImageOutputsPatch(JSONObject patch, List<Long> assetIds) {
        if (assetIds.isEmpty()) {
            return;
        }
        cn.hutool.json.JSONArray outputs = new cn.hutool.json.JSONArray();
        for (int i = 0; i < assetIds.size(); i++) {
            Long assetId = assetIds.get(i);
            outputs.add(new JSONObject()
                    .set("id", "asset-" + assetId)
                    .set("assetId", assetId));
        }
        patch.set("assetIds", assetIds)
                .set("outputs", outputs)
                .set("primaryOutputId", outputs.getJSONObject(0).getStr("id"))
                .set("outputsExpanded", outputs.size() > 1);
    }

    private boolean bindOutputAssets(AigcCanvasNodeRunSyncReqVO reqVO, List<Long> assetIds, Long taskId) {
        boolean inserted = false;
        for (Long assetId : assetIds) {
            inserted = bindOutputAsset(reqVO, assetId, taskId) || inserted;
        }
        return inserted;
    }

    private boolean bindOutputAsset(AigcCanvasNodeRunSyncReqVO reqVO, Long assetId, Long taskId) {
        if (assetRefMapper.selectByNodeAndAsset(reqVO.getProjectId(), reqVO.getNodeId(), assetId, "output") != null) {
            return false;
        }
        AigcCanvasAssetRefDO assetRef = new AigcCanvasAssetRefDO();
        assetRef.setProjectId(reqVO.getProjectId());
        assetRef.setNodeId(reqVO.getNodeId());
        assetRef.setAssetId(assetId);
        assetRef.setUsageType("output");
        assetRef.setSourceTaskId(taskId);
        assetRefMapper.insert(assetRef);
        return true;
    }

    private List<String> parseStringList(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return JSONUtil.parseArray(value).toList(String.class);
    }

    private List<Long> parseLongList(String value) {
        if (StrUtil.isBlank(value)) {
            return List.of();
        }
        return JSONUtil.parseArray(value).toList(Long.class);
    }

    private AigcCanvasOperationLogDO submitTaskStatusPatch(Long projectId, String nodeId, Long baseVersion, Long userId,
            String opId, JSONObject patch) {
        JSONObject payload = new JSONObject()
                .set("nodeId", nodeId)
                .set("patch", patch);
        AigcCanvasOperationSubmitReqVO operationReq = new AigcCanvasOperationSubmitReqVO();
        operationReq.setProjectId(projectId);
        operationReq.setClientId("server_node_run");
        operationReq.setOpId(opId);
        operationReq.setBaseVersion(baseVersion);
        operationReq.setOperationType("TASK_STATUS_PATCH");
        operationReq
                .setOperationJson(new JSONObject().set("type", "TASK_STATUS_PATCH").set("payload", payload).toString());
        return operationService.submitOperation(operationReq, userId);
    }

    private void broadcastAfterCommit(AigcCanvasOperationLogDO operation) {
        if (operation == null) {
            return;
        }
        Runnable broadcast = () -> roomService.broadcast(operation.getProjectId(), "canvas-op-applied",
                buildAppliedMessage(operation), null);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            broadcast.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcast.run();
            }
        });
    }

    private AigcCanvasOperationAppliedMessage buildAppliedMessage(AigcCanvasOperationLogDO operation) {
        return new AigcCanvasOperationAppliedMessage()
                .setProjectId(operation.getProjectId())
                .setClientId(operation.getClientId())
                .setOpId(operation.getOpId())
                .setActorUserId(operation.getActorUserId())
                .setBaseVersion(operation.getBaseVersion())
                .setVersion(operation.getNextVersion())
                .setOperationType(operation.getOperationType())
                .setOperationJson(operation.getOperationJson())
                .setInverseOperationJson(operation.getInverseOperationJson());
    }

}
