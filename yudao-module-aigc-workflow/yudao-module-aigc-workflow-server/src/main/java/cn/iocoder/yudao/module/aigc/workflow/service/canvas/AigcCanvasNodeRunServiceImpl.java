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
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunSyncReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.AigcCanvasRoomService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationAppliedMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class AigcCanvasNodeRunServiceImpl implements AigcCanvasNodeRunService {

    @Resource
    private AigcCanvasProjectService projectService;
    @Resource
    private AigcCanvasOperationService operationService;
    @Resource
    private AigcCanvasRoomService roomService;
    @Resource
    private AigcGenerateApi generateApi;

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
        AigcGenerateResultRespDTO result = generateApi.getResult(reqVO.getTaskId()).getCheckedData();
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
            List<String> urls = parseStringList(result.getOutputUrls());
            if (!urls.isEmpty() && !StrUtil.startWithIgnoreCase(urls.get(0), "data:")) {
                if ("video".equals(nodeType)) {
                    patch.set("videoUrl", urls.get(0));
                } else {
                    patch.set("previewUrl", urls.get(0)).set("outputPreviewUrl", urls.get(0));
                }
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
