package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSyncRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSnapshotRespVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSnapshotDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasOperationLogMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasProjectMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasSnapshotMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_OPERATION_PAYLOAD_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_OPERATION_PAYLOAD_TOO_LARGE;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_OPERATION_TYPE_INVALID;

@Service
@Validated
public class AigcCanvasOperationServiceImpl implements AigcCanvasOperationService {

    private static final int MAX_OPERATION_JSON_LENGTH = 64 * 1024;
    private static final Set<String> OPERATION_TYPES = new HashSet<>(Arrays.asList(
            "NODE_CREATE", "NODE_DELETE", "NODE_MOVE", "NODE_RESIZE", "NODE_UPDATE_DATA",
            "EDGE_CREATE", "EDGE_DELETE", "ASSET_ATTACH", "ASSET_DETACH", "TASK_STATUS_PATCH", "CANVAS_CLEAR"));
    private static final Set<String> SYNCABLE_NODE_DATA_KEYS = new HashSet<>(Arrays.asList(
            "imageId", "sketchId", "videoId", "projectId", "assetId", "assetVersionId", "background", "fileName", "mimeType",
            "width", "height", "durationSec", "sizeBytes", "kind", "prompt", "content", "modelId",
            "provider", "providerModel", "modelName", "aigcModelId", "params", "inputParams", "status", "taskId",
            "errorMessage", "taskStatus", "progress", "outputAssetId", "sourceTaskId",
            "assetIds", "assetIdList", "outputs", "primaryOutputId", "outputsExpanded", "generationCount",
            "safetyStatus", "safetyReason", "generationStartedAt", "generationCompletedAt",
            "generationRunStartedAt", "elapsedMs", "upstreamStatus", "ratio", "resolution", "duration",
            "size", "generateAudio", "watermark", "firstFrameEdgeId", "lastFrameEdgeId", "referenceImageOrder",
            "createdAt", "updatedAt"));
    private static final Set<String> BLOCKED_NODE_DATA_KEYS = new HashSet<>(Arrays.asList(
            "dataUrl", "blobUrl", "objectUrl", "localUrl", "inputImages", "imageUrls"));
    private static final Set<String> RUNTIME_ASSET_URL_NODE_DATA_KEYS = new HashSet<>(Arrays.asList(
            "previewUrl", "outputPreviewUrl", "videoUrl", "assetUrlExpireTime"));

    @Resource
    private AigcCanvasProjectService projectService;
    @Resource
    private AigcCanvasProjectMapper projectMapper;
    @Resource
    private AigcCanvasOperationLogMapper operationLogMapper;
    @Resource
    private AigcCanvasSnapshotMapper snapshotMapper;
    @Resource
    private AigcCanvasSnapshotStorageService snapshotStorageService;

    private final Map<Long, CanvasGraphState> graphStateCache = new ConcurrentHashMap<>();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasOperationLogDO submitOperation(AigcCanvasOperationSubmitReqVO reqVO, Long userId) {
        projectService.validateEditableProject(reqVO.getProjectId(), userId);
        AigcCanvasProjectDO project = projectMapper.selectByIdForUpdate(reqVO.getProjectId());
        AigcCanvasOperationLogDO existed = operationLogMapper.selectByClientOperation(reqVO.getProjectId(), reqVO.getClientId(), reqVO.getOpId());
        if (existed != null) {
            return existed;
        }
        validateOperation(reqVO);
        validateAssetReferences(reqVO, userId);
        CanvasGraphState graphState = rebuildGraphState(reqVO.getProjectId());
        validateOperationAgainstGraph(reqVO, graphState);
        if ("EDGE_CREATE".equals(reqVO.getOperationType())) {
            AigcCanvasOperationLogDO semanticExisted = findExistingEdgeCreate(reqVO, graphState);
            if (semanticExisted != null) {
                return semanticExisted;
            }
        }
        long nextVersion = project.getCurrentVersion() == null ? 1L : project.getCurrentVersion() + 1;
        AigcCanvasOperationLogDO operation = BeanUtils.toBean(reqVO, AigcCanvasOperationLogDO.class);
        operation.setActorUserId(userId);
        operation.setNextVersion(nextVersion);
        operationLogMapper.insert(operation);
        applyOperationToGraphState(graphState, operation);
        graphState.version = nextVersion;
        graphStateCache.put(project.getId(), graphState.copy());

        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(project.getId());
        update.setCurrentVersion(nextVersion);
        projectMapper.updateById(update);
        if (isStatisticsAffectedOperation(reqVO.getOperationType())) {
            projectService.refreshProjectStatistics(project.getId());
        }
        return operation;
    }

    private boolean isStatisticsAffectedOperation(String operationType) {
        return "NODE_CREATE".equals(operationType)
                || "NODE_DELETE".equals(operationType)
                || "NODE_UPDATE_DATA".equals(operationType)
                || "ASSET_ATTACH".equals(operationType)
                || "ASSET_DETACH".equals(operationType)
                || "TASK_STATUS_PATCH".equals(operationType)
                || "CANVAS_CLEAR".equals(operationType);
    }

    @Override
    public List<AigcCanvasOperationLogDO> getOperationsAfterVersion(Long projectId, Long afterVersion, Long userId) {
        projectService.validateReadableProject(projectId, userId);
        return operationLogMapper.selectListAfterVersion(projectId, afterVersion == null ? 0L : afterVersion);
    }

    @Override
    public AigcCanvasOperationSyncRespVO syncOperations(Long projectId, Long afterVersion, Long userId) {
        AigcCanvasProjectDO project = projectService.validateReadableProject(projectId, userId);
        long version = afterVersion == null ? 0L : afterVersion;
        AigcCanvasSnapshotDO snapshot = snapshotStorageService.hydrateForRead(snapshotMapper.selectLatestByProjectId(projectId));
        if (snapshot != null && version < snapshot.getVersion()) {
            return buildSnapshotSync(project, snapshot);
        }
        AigcCanvasOperationLogDO minOperation = operationLogMapper.selectMinByProjectId(projectId);
        if (minOperation != null && version > 0 && version < minOperation.getNextVersion() - 1) {
            return buildSnapshotSync(project, snapshot);
        }
        List<AigcCanvasOperationLogDO> operations = operationLogMapper.selectListAfterVersion(projectId, version);
        AigcCanvasOperationSyncRespVO respVO = new AigcCanvasOperationSyncRespVO();
        respVO.setMode("delta");
        respVO.setFromVersion(version + 1);
        respVO.setToVersion(CollUtil.isEmpty(operations) ? project.getCurrentVersion() : operations.get(operations.size() - 1).getNextVersion());
        respVO.setOperations(BeanUtils.toBean(operations, AigcCanvasOperationRespVO.class));
        return respVO;
    }

    @Override
    public void invalidateGraphState(Long projectId) {
        graphStateCache.remove(projectId);
    }

    @Override
    public Long getCurrentVersion(Long projectId, Long userId) {
        AigcCanvasProjectDO project = projectService.validateReadableProject(projectId, userId);
        return project.getCurrentVersion() == null ? 0L : project.getCurrentVersion();
    }

    private AigcCanvasOperationSyncRespVO buildSnapshotSync(AigcCanvasProjectDO project, AigcCanvasSnapshotDO snapshot) {
        AigcCanvasOperationSyncRespVO respVO = new AigcCanvasOperationSyncRespVO();
        if (snapshot == null) {
            respVO.setMode("delta");
            respVO.setFromVersion(project.getCurrentVersion());
            respVO.setToVersion(project.getCurrentVersion());
            return respVO;
        }
        respVO.setMode("snapshot");
        respVO.setToVersion(project.getCurrentVersion());
        sanitizeSnapshot(snapshot);
        respVO.setSnapshot(BeanUtils.toBean(snapshot, AigcCanvasSnapshotRespVO.class));
        return respVO;
    }

    private void sanitizeSnapshot(AigcCanvasSnapshotDO snapshot) {
        if (snapshot == null || !JSONUtil.isTypeJSONArray(snapshot.getNodesJson())) {
            return;
        }
        JSONArray nodes = JSONUtil.parseArray(snapshot.getNodesJson());
        for (Object item : nodes) {
            JSONObject node = JSONUtil.parseObj(item);
            JSONObject data = node.getJSONObject("data");
            if (data != null && ("image".equals(node.getStr("type")) || "video".equals(node.getStr("type")))) {
                RUNTIME_ASSET_URL_NODE_DATA_KEYS.forEach(data::remove);
            }
        }
        snapshot.setNodesJson(nodes.toString());
    }

    private void validateOperation(AigcCanvasOperationSubmitReqVO reqVO) {
        if (!OPERATION_TYPES.contains(reqVO.getOperationType())) {
            throw exception(CANVAS_OPERATION_TYPE_INVALID);
        }
        JSONObject operationJson = validateJson(reqVO.getOperationJson());
        validateOperationPayload(reqVO.getOperationType(), operationJson);
        if (reqVO.getInverseOperationJson() != null) {
            validateJson(reqVO.getInverseOperationJson());
        }
    }

    private void validateAssetReferences(AigcCanvasOperationSubmitReqVO reqVO, Long userId) {
        JSONObject operationJson = JSONUtil.parseObj(reqVO.getOperationJson());
        JSONObject payload = operationJson.getJSONObject("payload");
        Set<Long> assetIds = new HashSet<>();
        if ("NODE_CREATE".equals(reqVO.getOperationType())) {
            JSONObject node = payload.getJSONObject("node");
            if (node != null) {
                collectNodeAssetIds(assetIds, node.getJSONObject("data"));
            }
        }
        if ("NODE_UPDATE_DATA".equals(reqVO.getOperationType()) || "TASK_STATUS_PATCH".equals(reqVO.getOperationType())) {
            collectNodeAssetIds(assetIds, payload.getJSONObject("patch"));
        }
        if ("ASSET_ATTACH".equals(reqVO.getOperationType())) {
            addAssetId(assetIds, payload.get("assetId"));
        }
        if (!assetIds.isEmpty()) {
            projectService.validateProjectAssetReferences(reqVO.getProjectId(), assetIds, userId);
        }
    }

    private void collectNodeAssetIds(Set<Long> assetIds, JSONObject data) {
        if (data == null) {
            return;
        }
        addAssetId(assetIds, data.get("assetId"));
        addAssetId(assetIds, data.get("outputAssetId"));
        addAssetId(assetIds, data.get("previewAssetId"));
        addAssetId(assetIds, data.get("assetIdList"));
        addAssetId(assetIds, data.get("assetIds"));
    }

    private void addAssetId(Set<Long> assetIds, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            long assetId = number.longValue();
            if (assetId > 0) {
                assetIds.add(assetId);
            }
            return;
        }
        if (value instanceof CharSequence text) {
            String str = text.toString();
            if (str.matches("\\d+")) {
                assetIds.add(Long.valueOf(str));
            }
            return;
        }
        if (value instanceof JSONArray array) {
            for (Object item : array) {
                addAssetId(assetIds, item);
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                addAssetId(assetIds, item);
            }
        }
    }

    private JSONObject validateJson(String json) {
        if (json.length() > MAX_OPERATION_JSON_LENGTH) {
            throw exception(CANVAS_OPERATION_PAYLOAD_TOO_LARGE);
        }
        if (!JSONUtil.isTypeJSON(json)) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        return JSONUtil.parseObj(json);
    }

    private void validateOperationPayload(String operationType, JSONObject operationJson) {
        if (!operationType.equals(operationJson.getStr("type"))) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        JSONObject payload = operationJson.getJSONObject("payload");
        if (payload == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        validateNoLocalMedia(payload);
        if ("NODE_UPDATE_DATA".equals(operationType) || "TASK_STATUS_PATCH".equals(operationType)) {
            validateNodeDataPatch(payload);
        }
        if ("EDGE_CREATE".equals(operationType)) {
            validateEdgeCreatePayload(payload);
        }
        if ("EDGE_DELETE".equals(operationType)) {
            validateEdgeDeletePayload(payload);
        }
        if ("NODE_DELETE".equals(operationType)) {
            validateNodeIdPayload(payload);
        }
        if ("NODE_CREATE".equals(operationType)) {
            validateNodeCreatePayload(payload);
        }
        if ("CANVAS_CLEAR".equals(operationType) && !payload.isEmpty()) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateNodeDataPatch(JSONObject payload) {
        if (payload.getStr("nodeId") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        JSONObject patch = payload.getJSONObject("patch");
        if (patch == null || patch.isEmpty()) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        for (String key : patch.keySet()) {
            if (!SYNCABLE_NODE_DATA_KEYS.contains(key) || BLOCKED_NODE_DATA_KEYS.contains(key)) {
                throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
            }
        }
    }

    private void validateNoLocalMedia(Object value) {
        if (value instanceof CharSequence text) {
            String str = text.toString();
            if (str.startsWith("data:") || str.startsWith("blob:")) {
                throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
            }
            return;
        }
        if (value instanceof JSONObject object) {
            for (Object item : object.values()) {
                validateNoLocalMedia(item);
            }
            return;
        }
        if (value instanceof JSONArray array) {
            for (Object item : array) {
                validateNoLocalMedia(item);
            }
        }
    }

    private void validateEdgeCreatePayload(JSONObject payload) {
        JSONObject edge = payload.getJSONObject("edge");
        if (edge == null || edge.getStr("source") == null || edge.getStr("target") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        if (edge.getStr("source").equals(edge.getStr("target"))) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateEdgeDeletePayload(JSONObject payload) {
        if (payload.getStr("edgeId") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateNodeIdPayload(JSONObject payload) {
        if (payload.getStr("nodeId") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateNodeCreatePayload(JSONObject payload) {
        JSONObject node = payload.getJSONObject("node");
        if (node == null || node.getStr("id") == null || node.getStr("type") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        JSONObject position = node.getJSONObject("position");
        if (position == null || position.get("x") == null || position.get("y") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
        if (node.getJSONObject("data") == null) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateOperationAgainstGraph(AigcCanvasOperationSubmitReqVO reqVO, CanvasGraphState graphState) {
        JSONObject payload = JSONUtil.parseObj(reqVO.getOperationJson()).getJSONObject("payload");
        switch (reqVO.getOperationType()) {
            case "NODE_CREATE" -> validateNodeCreateAgainstGraph(payload, graphState);
            case "NODE_MOVE", "NODE_RESIZE", "NODE_UPDATE_DATA", "ASSET_ATTACH", "ASSET_DETACH", "TASK_STATUS_PATCH" ->
                    validateNodeExists(payload.getStr("nodeId"), graphState);
            case "EDGE_CREATE" -> validateEdgeCreateAgainstGraph(payload.getJSONObject("edge"), graphState);
            case "EDGE_DELETE" -> validateEdgeDeleteAgainstGraph(payload.getStr("edgeId"), graphState);
            case "NODE_DELETE" -> validateNodeDeleteAgainstGraph(payload.getStr("nodeId"), graphState);
            case "CANVAS_CLEAR" -> {
            }
            default -> {
            }
        }
    }

    private void validateNodeCreateAgainstGraph(JSONObject payload, CanvasGraphState graphState) {
        JSONObject node = payload.getJSONObject("node");
        String nodeId = node.getStr("id");
        if (graphState.nodes.containsKey(nodeId)) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateNodeExists(String nodeId, CanvasGraphState graphState) {
        if (nodeId == null || !graphState.nodes.containsKey(nodeId)) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateEdgeCreateAgainstGraph(JSONObject edge, CanvasGraphState graphState) {
        validateNodeExists(edge.getStr("source"), graphState);
        validateNodeExists(edge.getStr("target"), graphState);
    }

    private void validateEdgeDeleteAgainstGraph(String edgeId, CanvasGraphState graphState) {
        if (!graphState.edges.containsKey(edgeId)) {
            throw exception(CANVAS_OPERATION_PAYLOAD_INVALID);
        }
    }

    private void validateNodeDeleteAgainstGraph(String nodeId, CanvasGraphState graphState) {
        validateNodeExists(nodeId, graphState);
    }

    private AigcCanvasOperationLogDO findExistingEdgeCreate(AigcCanvasOperationSubmitReqVO reqVO, CanvasGraphState graphState) {
        JSONObject edge = JSONUtil.parseObj(reqVO.getOperationJson()).getJSONObject("payload").getJSONObject("edge");
        String source = edge.getStr("source");
        String target = edge.getStr("target");
        String sourceHandle = edge.getStr("sourceHandle");
        String targetHandle = edge.getStr("targetHandle");
        JSONObject existedEdge = graphState.findEdge(source, target, sourceHandle, targetHandle);
        if (existedEdge == null) {
            return null;
        }
        return operationLogMapper.selectById(existedEdge.getLong("operationId"));
    }

    private boolean isSameEdge(String source, String target, String sourceHandle, String targetHandle, JSONObject edge) {
        return source.equals(edge.getStr("source"))
                && target.equals(edge.getStr("target"))
                && equalsNullable(sourceHandle, edge.getStr("sourceHandle"))
                && equalsNullable(targetHandle, edge.getStr("targetHandle"));
    }

    private boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private CanvasGraphState rebuildGraphState(Long projectId) {
        AigcCanvasProjectDO project = projectMapper.selectById(projectId);
        CanvasGraphState cached = graphStateCache.get(projectId);
        if (cached != null && project != null && cached.version == (project.getCurrentVersion() == null ? 0L : project.getCurrentVersion())) {
            return cached.copy();
        }
        CanvasGraphState state = new CanvasGraphState();
        long afterVersion = 0L;
        AigcCanvasSnapshotDO snapshot = snapshotStorageService.hydrateForRead(snapshotMapper.selectLatestByProjectId(projectId));
        if (snapshot != null) {
            hydrateGraphStateFromSnapshot(state, snapshot);
            afterVersion = snapshot.getVersion();
        }
        for (AigcCanvasOperationLogDO operation : operationLogMapper.selectListAfterVersion(projectId, afterVersion)) {
            applyOperationToGraphState(state, operation);
            state.version = operation.getNextVersion();
        }
        if (project != null) {
            state.version = project.getCurrentVersion() == null ? state.version : project.getCurrentVersion();
        }
        graphStateCache.put(projectId, state.copy());
        return state;
    }

    private void hydrateGraphStateFromSnapshot(CanvasGraphState state, AigcCanvasSnapshotDO snapshot) {
        JSONArray nodes = JSONUtil.parseArray(snapshot.getNodesJson());
        for (Object item : nodes) {
            JSONObject node = JSONUtil.parseObj(item);
            state.nodes.put(node.getStr("id"), node);
        }
        JSONArray edges = JSONUtil.parseArray(snapshot.getEdgesJson());
        for (Object item : edges) {
            JSONObject edge = JSONUtil.parseObj(item);
            state.edges.put(edge.getStr("id"), edge);
        }
    }

    private void applyOperationToGraphState(CanvasGraphState state, AigcCanvasOperationLogDO operation) {
        JSONObject operationJson = JSONUtil.parseObj(operation.getOperationJson());
        JSONObject payload = operationJson.getJSONObject("payload");
        switch (operation.getOperationType()) {
            case "NODE_CREATE" -> {
                JSONObject node = payload.getJSONObject("node");
                state.nodes.put(node.getStr("id"), node);
            }
            case "NODE_DELETE" -> {
                String nodeId = payload.getStr("nodeId");
                state.nodes.remove(nodeId);
                state.edges.entrySet().removeIf(entry -> nodeId.equals(entry.getValue().getStr("source")) || nodeId.equals(entry.getValue().getStr("target")));
            }
            case "NODE_UPDATE_DATA", "ASSET_ATTACH", "TASK_STATUS_PATCH" -> applyNodePatch(state, payload);
            case "EDGE_CREATE" -> {
                JSONObject edge = payload.getJSONObject("edge");
                edge.set("operationId", operation.getId());
                state.edges.put(edge.getStr("id"), edge);
            }
            case "EDGE_DELETE" -> state.edges.remove(payload.getStr("edgeId"));
            case "CANVAS_CLEAR" -> {
                state.nodes.clear();
                state.edges.clear();
            }
            default -> {
            }
        }
    }

    private void applyNodePatch(CanvasGraphState state, JSONObject payload) {
        JSONObject node = state.nodes.get(payload.getStr("nodeId"));
        if (node == null) {
            return;
        }
        JSONObject data = node.getJSONObject("data");
        if (data == null) {
            data = new JSONObject();
            node.set("data", data);
        }
        JSONObject patch = payload.getJSONObject("patch");
        if (patch == null) {
            patch = new JSONObject();
            for (String key : Arrays.asList("assetId", "assetVersionId", "usageType", "sourceTaskId")) {
                if (payload.containsKey(key)) {
                    patch.set(key, payload.get(key));
                }
            }
        }
        for (String key : patch.keySet()) {
            if (RUNTIME_ASSET_URL_NODE_DATA_KEYS.contains(key)) {
                continue;
            }
            data.set(key, patch.get(key));
        }
    }

    private static class CanvasGraphState {

        private long version;
        private final Map<String, JSONObject> nodes = new HashMap<>();
        private final Map<String, JSONObject> edges = new HashMap<>();

        private CanvasGraphState copy() {
            CanvasGraphState copied = new CanvasGraphState();
            copied.version = version;
            for (Map.Entry<String, JSONObject> entry : nodes.entrySet()) {
                copied.nodes.put(entry.getKey(), JSONUtil.parseObj(entry.getValue().toString()));
            }
            for (Map.Entry<String, JSONObject> entry : edges.entrySet()) {
                copied.edges.put(entry.getKey(), JSONUtil.parseObj(entry.getValue().toString()));
            }
            return copied;
        }

        private JSONObject findEdge(String source, String target, String sourceHandle, String targetHandle) {
            for (JSONObject edge : edges.values()) {
                if (source.equals(edge.getStr("source"))
                        && target.equals(edge.getStr("target"))
                        && (sourceHandle == null ? edge.getStr("sourceHandle") == null : sourceHandle.equals(edge.getStr("sourceHandle")))
                        && (targetHandle == null ? edge.getStr("targetHandle") == null : targetHandle.equals(edge.getStr("targetHandle")))) {
                    return edge;
                }
            }
            return null;
        }

    }

}
