package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.gen.api.AigcGenerateApi;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateResultRespDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunSyncReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasAssetRefDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasAssetRefMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasProjectMapper;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.AigcCanvasRoomService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationAppliedMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Validated
@Slf4j
public class AigcCanvasNodeRunServiceImpl implements AigcCanvasNodeRunService {

    private static final Set<String> PROVIDER_SYNC_STATUSES = Set.of(
            AigcGenerateStatusEnum.SUBMITTED.getCode(),
            AigcGenerateStatusEnum.CALLBACK_WAITING.getCode(),
            AigcGenerateStatusEnum.SYNCING.getCode()
    );

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasNodeRunRespVO runNode(AigcCanvasNodeRunReqVO reqVO, Long userId) {
        projectService.validateEditableProject(reqVO.getProjectId(), userId);
        String runId = StrUtil.blankToDefault(reqVO.getRunId(), "run_" + System.currentTimeMillis());
        CommonResult<AigcGenerateSubmitRespDTO> submitResult = generateApi.submit(new AigcGenerateSubmitReqDTO()
                .setUserId(userId)
                .setClientRequestId("canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_" + runId)
                .setGenerateType(reqVO.getGenerateType())
                .setGenerateMode(reqVO.getGenerateMode())
                .setModelId(reqVO.getModelId())
                .setPrompt(reqVO.getPrompt())
                .setInputParams(reqVO.getInputParams())
                .setSync(Boolean.TRUE.equals(reqVO.getSync())));
        AigcGenerateSubmitRespDTO submit = submitResult.getCheckedData();
        AigcCanvasOperationLogDO operation = submitTaskStatusPatch(reqVO.getProjectId(), reqVO.getNodeId(), reqVO.getBaseVersion(), userId,
                "task_status_" + reqVO.getNodeId() + "_" + runId, buildSubmitPatch(submit));
        roomService.broadcast(operation.getProjectId(), "canvas-op-applied", buildAppliedMessage(operation), null);
        return new AigcCanvasNodeRunRespVO()
                .setTaskId(submit.getTaskId())
                .setGenerateRecordId(submit.getId())
                .setGenerateNo(submit.getGenerateNo())
                .setStatus(submit.getStatus())
                .setOperation(BeanUtils.toBean(operation, AigcCanvasOperationRespVO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasNodeRunRespVO syncNodeRun(AigcCanvasNodeRunSyncReqVO reqVO, Long userId) {
        projectService.validateEditableProject(reqVO.getProjectId(), userId);
        AigcGenerateResultRespDTO result = getResultReadyForCanvas(reqVO.getTaskId());
        applySuccessfulAssetSideEffects(reqVO, result);
        AigcCanvasOperationLogDO operation = submitTaskStatusPatch(reqVO.getProjectId(), reqVO.getNodeId(), reqVO.getBaseVersion(), userId,
                "task_result_" + reqVO.getNodeId() + "_" + reqVO.getTaskId() + "_" + result.getStatus(), buildResultPatch(reqVO.getNodeType(), result));
        roomService.broadcast(operation.getProjectId(), "canvas-op-applied", buildAppliedMessage(operation), null);
        return new AigcCanvasNodeRunRespVO()
                .setTaskId(result.getTaskId())
                .setGenerateRecordId(result.getId())
                .setGenerateNo(result.getGenerateNo())
                .setStatus(result.getStatus())
                .setOperation(BeanUtils.toBean(operation, AigcCanvasOperationRespVO.class));
    }

    private AigcGenerateResultRespDTO getResultReadyForCanvas(Long taskId) {
        AigcGenerateResultRespDTO result = generateApi.getResult(taskId).getCheckedData();
        if (isProviderSyncStatus(result)) {
            syncProviderTaskQuietly(taskId);
            result = generateApi.getResult(taskId).getCheckedData();
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

    private JSONObject buildSubmitPatch(AigcGenerateSubmitRespDTO submit) {
        return new JSONObject()
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
    }

    private JSONObject buildResultPatch(String nodeType, AigcGenerateResultRespDTO result) {
        boolean success = "SUCCESS".equals(result.getStatus());
        boolean failed = "FAILED".equals(result.getStatus()) || "CANCELED".equals(result.getStatus()) || "CANCELLED".equals(result.getStatus());
        JSONObject patch = new JSONObject()
                .set("taskId", String.valueOf(result.getTaskId()))
                .set("taskStatus", result.getStatus())
                .set("upstreamStatus", result.getStatus())
                .set("updatedAt", LocalDateTime.now().toString())
                .set("progress", success || failed ? 100 : 40);
        if (success) {
            patch.set("status", "video".equals(nodeType) ? "complete" : "idle")
                    .set("errorMessage", null)
                    .set("generationCompletedAt", result.getFinishTime() == null ? LocalDateTime.now().toString() : result.getFinishTime().toString())
                    .set("elapsedMs", result.getCreateTime() == null || result.getFinishTime() == null ? null : java.time.Duration.between(result.getCreateTime(), result.getFinishTime()).toMillis());
            if (StrUtil.isNotBlank(result.getOutputText())) {
                patch.set("content", result.getOutputText());
            }
            List<Long> assetIds = parseLongList(result.getAssetIds());
            if (!assetIds.isEmpty()) {
                patch.set("assetId", assetIds.get(0)).set("outputAssetId", assetIds.get(0));
            }
            patch.set("kind", "generated");
            return patch;
        }
        if (failed) {
            patch.set("status", "failed")
                    .set("errorMessage", StrUtil.blankToDefault(result.getFailMessage(), "生成失败，请稍后重试。"))
                    .set("generationCompletedAt", result.getFinishTime() == null ? LocalDateTime.now().toString() : result.getFinishTime().toString());
            return patch;
        }
        patch.set("status", "pending");
        return patch;
    }

    private void applySuccessfulAssetSideEffects(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result) {
        if (result == null || !"SUCCESS".equals(result.getStatus())) {
            return;
        }
        List<Long> assetIds = parseLongList(result.getAssetIds());
        if (assetIds.isEmpty()) {
            return;
        }
        Long assetId = assetIds.get(0);
        bindOutputAsset(reqVO, assetId, result.getTaskId());
        if ("image".equals(reqVO.getNodeType())) {
            AigcCanvasProjectDO update = new AigcCanvasProjectDO();
            update.setId(reqVO.getProjectId());
            update.setCoverAssetId(assetId);
            projectMapper.updateById(update);
        }
    }

    private void bindOutputAsset(AigcCanvasNodeRunSyncReqVO reqVO, Long assetId, Long taskId) {
        if (assetRefMapper.selectByNodeAndAsset(reqVO.getProjectId(), reqVO.getNodeId(), assetId, "output") != null) {
            return;
        }
        AigcCanvasAssetRefDO assetRef = new AigcCanvasAssetRefDO();
        assetRef.setProjectId(reqVO.getProjectId());
        assetRef.setNodeId(reqVO.getNodeId());
        assetRef.setAssetId(assetId);
        assetRef.setUsageType("output");
        assetRef.setSourceTaskId(taskId);
        assetRefMapper.insert(assetRef);
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

    private AigcCanvasOperationLogDO submitTaskStatusPatch(Long projectId, String nodeId, Long baseVersion, Long userId, String opId, JSONObject patch) {
        JSONObject payload = new JSONObject()
                .set("nodeId", nodeId)
                .set("patch", patch);
        AigcCanvasOperationSubmitReqVO operationReq = new AigcCanvasOperationSubmitReqVO();
        operationReq.setProjectId(projectId);
        operationReq.setClientId("server_node_run");
        operationReq.setOpId(opId);
        operationReq.setBaseVersion(baseVersion);
        operationReq.setOperationType("TASK_STATUS_PATCH");
        operationReq.setOperationJson(new JSONObject().set("type", "TASK_STATUS_PATCH").set("payload", payload).toString());
        return operationService.submitOperation(operationReq, userId);
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
