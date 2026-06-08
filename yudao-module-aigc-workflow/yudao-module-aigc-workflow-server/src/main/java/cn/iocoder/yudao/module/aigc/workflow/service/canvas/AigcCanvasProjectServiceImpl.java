package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberInviteReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberUpdateRoleReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectQuickGenerateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectQuickGenerateRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectRecycleBinPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectRecycleBinRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectUpdateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasAssetBindReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSketchSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSnapshotSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasAssetRefDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasMemberDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectRecycleBinDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSketchDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSnapshotDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasMemberMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasAssetRefMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasOperationLogMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasProjectMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasProjectRecycleBinMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasSketchMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasSnapshotMapper;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.AigcCanvasRoomService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasMemberMessage;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationAppliedMessage;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_MEMBER_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_MEMBER_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_MEMBER_ROLE_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NO_PERMISSION;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_OWNER_CAN_NOT_CHANGE;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_PROJECT_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_PROJECT_RECYCLE_BIN_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_SNAPSHOT_VERSION_CONFLICT;

@Service
@Validated
public class AigcCanvasProjectServiceImpl implements AigcCanvasProjectService {

    private static final Set<String> RUNTIME_ASSET_URL_NODE_DATA_KEYS = Set.of("previewUrl", "outputPreviewUrl", "videoUrl", "assetUrlExpireTime");

    private static final String PROJECT_STATUS_NORMAL = "NORMAL";
    private static final String MEMBER_ROLE_OWNER = "owner";
    private static final String MEMBER_ROLE_EDITOR = "editor";
    private static final String MEMBER_ROLE_VIEWER = "viewer";

    @Resource
    private AigcCanvasProjectMapper projectMapper;
    @Resource
    private AigcCanvasProjectRecycleBinMapper projectRecycleBinMapper;
    @Resource
    private AigcCanvasMemberMapper memberMapper;
    @Resource
    private AigcCanvasSnapshotMapper snapshotMapper;
    @Resource
    private AigcCanvasSketchMapper sketchMapper;
    @Resource
    private AigcCanvasAssetRefMapper assetRefMapper;
    @Resource
    private AigcCanvasOperationLogMapper operationLogMapper;
    @Resource
    private AigcCanvasRoomService roomService;
    @Resource
    @Lazy
    private AigcCanvasOperationService operationService;
    @Resource
    @Lazy
    private AigcCanvasNodeRunService nodeRunService;
    @Resource
    private AigcAssetApi assetApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(AigcCanvasProjectCreateReqVO reqVO, Long userId) {
        AigcCanvasProjectDO project = BeanUtils.toBean(reqVO, AigcCanvasProjectDO.class);
        project.setOwnerUserId(userId);
        project.setStatus(PROJECT_STATUS_NORMAL);
        project.setCurrentVersion(0L);
        project.setNodeCount(0);
        project.setAssetCount(0);
        projectMapper.insert(project);

        AigcCanvasMemberDO member = new AigcCanvasMemberDO();
        member.setProjectId(project.getId());
        member.setUserId(userId);
        member.setRole(MEMBER_ROLE_OWNER);
        member.setJoinedTime(LocalDateTime.now());
        member.setLastActiveTime(LocalDateTime.now());
        memberMapper.insert(member);
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasProjectQuickGenerateRespVO createProjectAndRunFirstNode(AigcCanvasProjectQuickGenerateReqVO reqVO, Long userId) {
        AigcCanvasProjectCreateReqVO projectReqVO = new AigcCanvasProjectCreateReqVO();
        projectReqVO.setName(reqVO.getName());
        Long projectId = createProject(projectReqVO, userId);

        String nodeType = normalizeQuickGenerateNodeType(reqVO);
        String nodeId = nodeType + "_" + IdUtil.fastSimpleUUID();
        List<QuickGenerateReferenceNode> referenceNodes = buildQuickGenerateReferenceNodes(reqVO);
        for (QuickGenerateReferenceNode referenceNode : referenceNodes) {
            submitServerOperation(projectId, userId, "server_quick_generate", "node_create_" + referenceNode.nodeId(),
                    "NODE_CREATE", new JSONObject().set("node", referenceNode.node()), projectCurrentVersion(projectId));
        }
        JSONObject node = buildQuickGenerateNode(projectId, nodeId, nodeType, reqVO);
        submitServerOperation(projectId, userId, "server_quick_generate", "node_create_" + nodeId,
                "NODE_CREATE", new JSONObject().set("node", node), projectCurrentVersion(projectId));
        for (QuickGenerateReferenceNode referenceNode : referenceNodes) {
            submitServerOperation(projectId, userId, "server_quick_generate", "edge_create_" + referenceNode.nodeId() + "_" + nodeId,
                    "EDGE_CREATE", new JSONObject().set("edge", buildQuickGenerateReferenceEdge(referenceNode.nodeId(), nodeId)),
                    projectCurrentVersion(projectId));
        }

        List<Long> referenceAssetIds = quickGenerateReferenceAssetIds(reqVO);
        if (!referenceAssetIds.isEmpty()) {
            referenceAssetIds.forEach(assetId -> bindReferenceAssetQuietly(projectId, nodeId, assetId));
            updateProjectCover(projectId, referenceAssetIds.get(0));
        }

        AigcCanvasProjectDO project = projectMapper.selectById(projectId);
        AigcCanvasNodeRunReqVO runReqVO = new AigcCanvasNodeRunReqVO();
        runReqVO.setProjectId(projectId);
        runReqVO.setNodeId(nodeId);
        runReqVO.setClientId("server_quick_generate");
        runReqVO.setBaseVersion(project.getCurrentVersion() == null ? 0L : project.getCurrentVersion());
        runReqVO.setRunId("quick_" + nodeId);
        runReqVO.setNodeType(nodeType);
        runReqVO.setGenerateType(reqVO.getGenerateType());
        runReqVO.setGenerateMode(reqVO.getGenerateMode());
        runReqVO.setModelId(reqVO.getModelId());
        runReqVO.setPrompt(reqVO.getPrompt());
        runReqVO.setInputParams(normalizeQuickGenerateInputParams(reqVO));
        runReqVO.setSync(false);
        AigcCanvasNodeRunRespVO run = nodeRunService.runNode(runReqVO, userId);
        return new AigcCanvasProjectQuickGenerateRespVO()
                .setProjectId(projectId)
                .setNodeId(nodeId)
                .setTaskId(run.getTaskId())
                .setGenerateRecordId(run.getGenerateRecordId())
                .setGenerateNo(run.getGenerateNo())
                .setStatus(run.getStatus());
    }

    @Override
    public void updateProject(AigcCanvasProjectUpdateReqVO reqVO, Long userId) {
        validateEditableProject(reqVO.getId(), userId);
        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(reqVO.getId());
        update.setName(reqVO.getName());
        update.setCoverAssetId(reqVO.getCoverAssetId());
        if (reqVO.getName() != null || reqVO.getCoverAssetId() != null) {
            projectMapper.updateById(update);
        }
        refreshProjectStatistics(reqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id, Long userId) {
        AigcCanvasProjectDO project = refreshProjectStatistics(validateOwnerProject(id, userId));
        AigcCanvasProjectRecycleBinDO recycleBin = new AigcCanvasProjectRecycleBinDO();
        recycleBin.setProjectId(project.getId());
        recycleBin.setOwnerUserId(project.getOwnerUserId());
        recycleBin.setProjectName(project.getName());
        recycleBin.setCoverAssetId(project.getCoverAssetId());
        recycleBin.setCurrentVersion(project.getCurrentVersion());
        recycleBin.setLatestSnapshotId(project.getLatestSnapshotId());
        recycleBin.setProjectStatus(project.getStatus());
        recycleBin.setNodeCount(project.getNodeCount());
        recycleBin.setAssetCount(project.getAssetCount());
        recycleBin.setDeletedBy(userId);
        recycleBin.setDeletedTime(LocalDateTime.now());
        projectRecycleBinMapper.insert(recycleBin);
        projectMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreProject(Long id, Long userId) {
        AigcCanvasProjectRecycleBinDO recycleBin = validateOwnerRecycleBin(id, userId);
        int updateCount = projectRecycleBinMapper.restoreProject(recycleBin.getProjectId());
        if (updateCount == 0) {
            throw exception(CANVAS_PROJECT_NOT_EXISTS);
        }
        projectRecycleBinMapper.deletePhysicallyById(recycleBin.getId());
    }

    @Override
    public AigcCanvasProjectDO getProject(Long id, Long userId) {
        return validateReadableProject(id, userId);
    }

    @Override
    public AigcCanvasProjectRespVO getProjectDetail(Long id, Long userId) {
        AigcCanvasProjectDO project = validateReadableProject(id, userId);
        project = refreshProjectStatistics(project);
        AigcCanvasMemberDO member = getProjectMember(project, userId);
        Map<Long, AigcAssetRespDTO> assetMap = project.getCoverAssetId() == null
                ? Collections.emptyMap()
                : getAssetMapByIds(List.of(project.getCoverAssetId()));
        return buildProjectResp(project, userId, member, assetMap);
    }

    @Override
    public AigcCanvasMemberDO getProjectMember(Long id, Long userId) {
        AigcCanvasProjectDO project = validateReadableProject(id, userId);
        return getProjectMember(project, userId);
    }

    private AigcCanvasMemberDO getProjectMember(AigcCanvasProjectDO project, Long userId) {
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndUserId(project.getId(), userId);
        if (member == null && isProjectOwner(project, userId)) {
            return buildOwnerMember(project, userId);
        }
        return member;
    }

    @Override
    public List<AigcCanvasMemberDO> getProjectMembers(Long id, Long userId) {
        validateReadableProject(id, userId);
        return memberMapper.selectListByProjectId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteProjectMember(Long id, AigcCanvasMemberInviteReqVO reqVO, Long userId) {
        validateOwnerProject(id, userId);
        validateManageableRole(reqVO.getRole());
        AigcCanvasMemberDO existed = memberMapper.selectByProjectIdAndUserId(id, reqVO.getUserId());
        if (existed != null) {
            throw exception(CANVAS_MEMBER_EXISTS);
        }
        AigcCanvasMemberDO member = new AigcCanvasMemberDO();
        member.setProjectId(id);
        member.setUserId(reqVO.getUserId());
        member.setRole(reqVO.getRole());
        member.setInviteUserId(userId);
        member.setJoinedTime(LocalDateTime.now());
        member.setLastActiveTime(LocalDateTime.now());
        memberMapper.insert(member);
        broadcastMemberUpdated(id, reqVO.getUserId(), "member-added");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProjectMemberRole(Long id, Long memberId, AigcCanvasMemberUpdateRoleReqVO reqVO, Long userId) {
        validateOwnerProject(id, userId);
        validateManageableRole(reqVO.getRole());
        AigcCanvasMemberDO member = validateProjectMember(id, memberId);
        if (MEMBER_ROLE_OWNER.equals(member.getRole())) {
            throw exception(CANVAS_OWNER_CAN_NOT_CHANGE);
        }
        AigcCanvasMemberDO update = new AigcCanvasMemberDO();
        update.setId(member.getId());
        update.setRole(reqVO.getRole());
        memberMapper.updateById(update);
        broadcastMemberUpdated(id, member.getUserId(), "role-updated");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeProjectMember(Long id, Long memberId, Long userId) {
        validateOwnerProject(id, userId);
        AigcCanvasMemberDO member = validateProjectMember(id, memberId);
        if (MEMBER_ROLE_OWNER.equals(member.getRole())) {
            throw exception(CANVAS_OWNER_CAN_NOT_CHANGE);
        }
        memberMapper.deleteById(memberId);
        broadcastMemberUpdated(id, member.getUserId(), "member-removed");
    }

    @Override
    public PageResult<AigcCanvasProjectRespVO> getProjectPage(AigcCanvasProjectPageReqVO reqVO, Long userId) {
        List<AigcCanvasMemberDO> members = memberMapper.selectListByUserId(userId);
        Set<Long> sharedProjectIds = members.stream()
                .map(AigcCanvasMemberDO::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        PageResult<AigcCanvasProjectDO> pageResult = projectMapper.selectPage(reqVO, userId, sharedProjectIds);
        if (pageResult.getList() == null || pageResult.getList().isEmpty()) {
            return PageResult.empty(pageResult.getTotal());
        }

        List<AigcCanvasProjectDO> projects = pageResult.getList();
        projects = projects.stream().map(this::refreshProjectStatistics).toList();
        Set<Long> projectIds = projects.stream().map(AigcCanvasProjectDO::getId).collect(Collectors.toSet());
        Map<Long, AigcCanvasMemberDO> memberMap = members.stream()
                .filter(member -> projectIds.contains(member.getProjectId()))
                .collect(Collectors.toMap(AigcCanvasMemberDO::getProjectId, Function.identity(), (first, second) -> first));
        Map<Long, AigcAssetRespDTO> assetMap = getAssetMap(projects);

        List<AigcCanvasProjectRespVO> list = projects.stream()
                .map(project -> buildProjectResp(project, userId, memberMap.get(project.getId()), assetMap))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public PageResult<AigcCanvasProjectRecycleBinRespVO> getProjectRecycleBinPage(AigcCanvasProjectRecycleBinPageReqVO reqVO, Long userId) {
        PageResult<AigcCanvasProjectRecycleBinDO> pageResult = projectRecycleBinMapper.selectPage(reqVO, userId);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), AigcCanvasProjectRecycleBinRespVO.class), pageResult.getTotal());
    }

    private AigcCanvasProjectRespVO buildProjectResp(AigcCanvasProjectDO project, Long userId, AigcCanvasMemberDO member,
                                                     Map<Long, AigcAssetRespDTO> assetMap) {
        AigcCanvasProjectRespVO respVO = BeanUtils.toBean(project, AigcCanvasProjectRespVO.class);
        fillProjectPermissions(respVO, project, userId, member);
        fillProjectCover(respVO, assetMap);
        return respVO;
    }

    private void fillProjectPermissions(AigcCanvasProjectRespVO project, AigcCanvasProjectDO projectDO, Long userId, AigcCanvasMemberDO member) {
        String role = isProjectOwner(projectDO, userId) ? MEMBER_ROLE_OWNER : member == null ? null : member.getRole();
        boolean canEdit = role != null && !MEMBER_ROLE_VIEWER.equals(role);
        project.setRole(role);
        project.setCanEdit(canEdit);
        project.setReadonly(!canEdit);
    }

    private Map<Long, AigcAssetRespDTO> getAssetMap(List<AigcCanvasProjectDO> projects) {
        List<Long> assetIds = projects.stream()
                .map(AigcCanvasProjectDO::getCoverAssetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<AigcAssetRespDTO> assets = assetApi.getAssets(assetIds).getCheckedData();
            if (assets == null || assets.isEmpty()) {
                return Collections.emptyMap();
            }
            return assets.stream().collect(Collectors.toMap(AigcAssetRespDTO::getId, Function.identity(), (first, second) -> first));
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private void fillProjectCover(AigcCanvasProjectRespVO project, Map<Long, AigcAssetRespDTO> assetMap) {
        if (project.getCoverAssetId() != null) {
            AigcAssetRespDTO asset = assetMap.get(project.getCoverAssetId());
            String coverUrl = getAssetPreviewUrl(asset);
            if (StrUtil.isNotBlank(coverUrl)) {
                project.setCoverUrl(coverUrl);
            }
            return;
        }
        CanvasCover fallbackCover = findFirstImageNodeCover(project.getId());
        if (fallbackCover == null) {
            return;
        }
        if (fallbackCover.assetId() != null) {
            project.setCoverAssetId(fallbackCover.assetId());
            updateProjectCoverIfAbsent(project.getId(), fallbackCover.assetId());
        }
        if (StrUtil.isNotBlank(fallbackCover.coverUrl())) {
            project.setCoverUrl(fallbackCover.coverUrl());
        }
    }

    @Override
    public AigcCanvasSnapshotDO getLatestSnapshot(Long projectId, Long userId) {
        validateReadableProject(projectId, userId);
        return snapshotMapper.selectLatestByProjectId(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasSnapshotDO saveSnapshot(AigcCanvasSnapshotSaveReqVO reqVO, Long userId) {
        validateEditableProject(reqVO.getProjectId(), userId);
        AigcCanvasProjectDO project = projectMapper.selectByIdForUpdate(reqVO.getProjectId());
        if (!Objects.equals(reqVO.getBaseVersion(), project.getCurrentVersion())) {
            throw exception(CANVAS_SNAPSHOT_VERSION_CONFLICT);
        }
        long nextVersion = project.getCurrentVersion() == null ? 1L : project.getCurrentVersion() + 1;
        reqVO.setNodesJson(sanitizeSnapshotNodesJson(reqVO.getNodesJson()));
        AigcCanvasSnapshotDO snapshot = BeanUtils.toBean(reqVO, AigcCanvasSnapshotDO.class);
        snapshot.setVersion(nextVersion);
        snapshot.setCreatedBy(userId);
        snapshotMapper.insert(snapshot);

        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(project.getId());
        update.setCurrentVersion(nextVersion);
        update.setLatestSnapshotId(snapshot.getId());
        CanvasProjectStatistics statistics = summarizeProjectNodes(project.getId(), snapshot.getNodesJson());
        update.setNodeCount(statistics.nodeCount());
        update.setAssetCount(statistics.assetCount());
        projectMapper.updateById(update);
        operationService.invalidateGraphState(project.getId());
        return snapshot;
    }

    @Override
    public AigcCanvasSketchDO getSketch(Long projectId, String nodeId, Long userId) {
        validateReadableProject(projectId, userId);
        return sketchMapper.selectByProjectIdAndNodeId(projectId, nodeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCanvasSketchDO saveSketch(AigcCanvasSketchSaveReqVO reqVO, Long userId) {
        validateEditableProject(reqVO.getProjectId(), userId);
        AigcCanvasSketchDO existed = sketchMapper.selectByProjectIdAndNodeId(reqVO.getProjectId(), reqVO.getNodeId());
        AigcCanvasSketchDO sketch = BeanUtils.toBean(reqVO, AigcCanvasSketchDO.class);
        sketch.setMimeType(StrUtil.blankToDefault(reqVO.getMimeType(), "image/png"));
        sketch.setBackground(StrUtil.blankToDefault(reqVO.getBackground(), "white"));
        if (existed == null) {
            sketchMapper.insert(sketch);
            return sketch;
        }
        sketch.setId(existed.getId());
        sketchMapper.updateById(sketch);
        return sketchMapper.selectById(existed.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bindAsset(AigcCanvasAssetBindReqVO reqVO, Long userId) {
        validateEditableProject(reqVO.getProjectId(), userId);
        AigcCanvasProjectDO project = projectMapper.selectByIdForUpdate(reqVO.getProjectId());
        String usageType = StrUtil.blankToDefault(reqVO.getUsageType(), "source");
        AigcCanvasAssetRefDO existed = assetRefMapper.selectByNodeAndAsset(reqVO.getProjectId(), reqVO.getNodeId(), reqVO.getAssetId(), usageType);
        if (existed != null) {
            updateProjectCoverIfAbsent(project, reqVO.getAssetId());
            refreshProjectStatistics(project.getId());
            return existed.getId();
        }
        AigcCanvasAssetRefDO assetRef = BeanUtils.toBean(reqVO, AigcCanvasAssetRefDO.class);
        assetRef.setUsageType(usageType);
        assetRefMapper.insert(assetRef);
        operationService.invalidateGraphState(project.getId());
        AigcCanvasOperationLogDO operation = createAssetAttachOperation(reqVO, project, usageType, userId);
        operationService.invalidateGraphState(project.getId());
        roomService.broadcast(project.getId(), "canvas-op-applied", buildAppliedMessage(operation), null);
        refreshProjectStatistics(project.getId());
        return assetRef.getId();
    }

    private AigcCanvasOperationLogDO createAssetAttachOperation(AigcCanvasAssetBindReqVO reqVO, AigcCanvasProjectDO project,
                                                               String usageType, Long userId) {
        long nextVersion = project.getCurrentVersion() == null ? 1L : project.getCurrentVersion() + 1;
        JSONObject payload = new JSONObject()
                .set("nodeId", reqVO.getNodeId())
                .set("assetId", reqVO.getAssetId())
                .set("assetVersionId", reqVO.getAssetVersionId())
                .set("usageType", usageType)
                .set("sourceTaskId", reqVO.getSourceTaskId());
        JSONObject operationJson = new JSONObject()
                .set("type", "ASSET_ATTACH")
                .set("payload", payload);
        AigcCanvasOperationLogDO operation = new AigcCanvasOperationLogDO();
        operation.setProjectId(project.getId());
        operation.setClientId("server_asset_bind");
        operation.setOpId("asset_attach_" + reqVO.getNodeId() + "_" + reqVO.getAssetId() + "_" + usageType);
        operation.setActorUserId(userId);
        operation.setBaseVersion(project.getCurrentVersion());
        operation.setNextVersion(nextVersion);
        operation.setOperationType("ASSET_ATTACH");
        operation.setOperationJson(operationJson.toString());
        operationLogMapper.insert(operation);

        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(project.getId());
        update.setCurrentVersion(nextVersion);
        if (project.getCoverAssetId() == null) {
            update.setCoverAssetId(reqVO.getAssetId());
        }
        projectMapper.updateById(update);
        return operation;
    }

    @Override
    public void refreshProjectStatistics(Long projectId) {
        if (projectId == null) {
            return;
        }
        AigcCanvasProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        refreshProjectStatistics(project);
    }

    private AigcCanvasProjectDO refreshProjectStatistics(AigcCanvasProjectDO project) {
        if (project == null || project.getId() == null) {
            return project;
        }
        CanvasProjectStatistics statistics = summarizeProject(project.getId());
        if (Objects.equals(project.getNodeCount(), statistics.nodeCount())
                && Objects.equals(project.getAssetCount(), statistics.assetCount())) {
            return project;
        }
        projectMapper.updateStatistics(project.getId(), statistics.nodeCount(), statistics.assetCount());
        project.setNodeCount(statistics.nodeCount());
        project.setAssetCount(statistics.assetCount());
        return project;
    }

    private CanvasProjectStatistics summarizeProject(Long projectId) {
        return summarizeProjectNodes(projectId, rebuildProjectNodes(projectId));
    }

    private CanvasProjectStatistics summarizeProjectNodes(Long projectId, String nodesJson) {
        Map<String, JSONObject> nodes = new LinkedHashMap<>();
        if (StrUtil.isNotBlank(nodesJson) && JSONUtil.isTypeJSONArray(nodesJson)) {
            JSONArray snapshotNodes = JSONUtil.parseArray(nodesJson);
            for (Object item : snapshotNodes) {
                JSONObject node = JSONUtil.parseObj(item);
                if (StrUtil.isNotBlank(node.getStr("id"))) {
                    nodes.put(node.getStr("id"), node);
                }
            }
        }
        return summarizeProjectNodes(projectId, nodes);
    }

    private CanvasProjectStatistics summarizeProjectNodes(Long projectId, Map<String, JSONObject> nodes) {
        Set<Long> assetIds = new HashSet<>();
        for (JSONObject node : nodes.values()) {
            collectNodeAssetIds(assetIds, node.getJSONObject("data"));
        }
        if (!nodes.isEmpty()) {
            Set<String> currentNodeIds = nodes.keySet();
            List<AigcCanvasAssetRefDO> refs = assetRefMapper.selectListByProjectId(projectId);
            for (AigcCanvasAssetRefDO ref : refs) {
                if (ref.getAssetId() != null && currentNodeIds.contains(ref.getNodeId())) {
                    assetIds.add(ref.getAssetId());
                }
            }
        }
        return new CanvasProjectStatistics(nodes.size(), assetIds.size());
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
            try {
                long assetId = Long.parseLong(text.toString());
                if (assetId > 0) {
                    assetIds.add(assetId);
                }
            } catch (NumberFormatException ignored) {
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

    private void updateProjectCoverIfAbsent(AigcCanvasProjectDO project, Long assetId) {
        if (project.getCoverAssetId() != null || assetId == null) {
            return;
        }
        updateProjectCoverIfAbsent(project.getId(), assetId);
        project.setCoverAssetId(assetId);
    }

    private void updateProjectCoverIfAbsent(Long projectId, Long assetId) {
        if (assetId == null) {
            return;
        }
        projectMapper.updateCoverAssetIfAbsent(projectId, assetId);
    }

    private void updateProjectCover(Long projectId, Long assetId) {
        if (assetId == null) {
            return;
        }
        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(projectId);
        update.setCoverAssetId(assetId);
        projectMapper.updateById(update);
    }

    private String normalizeQuickGenerateNodeType(AigcCanvasProjectQuickGenerateReqVO reqVO) {
        if ("video".equals(reqVO.getNodeType()) || "VIDEO".equals(reqVO.getGenerateType())) {
            return "video";
        }
        return "image";
    }

    private JSONObject buildQuickGenerateNode(Long projectId, String nodeId, String nodeType,
                                              AigcCanvasProjectQuickGenerateReqVO reqVO) {
        LocalDateTime now = LocalDateTime.now();
        JSONObject data = "video".equals(nodeType)
                ? buildQuickGenerateVideoNodeData(nodeId, reqVO, now)
                : buildQuickGenerateImageNodeData(nodeId, reqVO, now);
        return new JSONObject()
                .set("id", nodeId)
                .set("type", nodeType)
                .set("position", new JSONObject().set("x", 250).set("y", 200))
                .set("selected", true)
                .set("data", data.set("projectId", projectId));
    }

    private List<QuickGenerateReferenceNode> buildQuickGenerateReferenceNodes(AigcCanvasProjectQuickGenerateReqVO reqVO) {
        List<Long> referenceAssetIds = quickGenerateReferenceAssetIds(reqVO);
        if (referenceAssetIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AigcAssetRespDTO> assetMap = getAssetMapByIds(referenceAssetIds);
        LocalDateTime now = LocalDateTime.now();
        return java.util.stream.IntStream.range(0, referenceAssetIds.size())
                .mapToObj(index -> buildQuickGenerateReferenceNode(referenceAssetIds.get(index),
                        index, assetMap.get(referenceAssetIds.get(index)), now))
                .toList();
    }

    private QuickGenerateReferenceNode buildQuickGenerateReferenceNode(Long assetId, int index,
                                                                        AigcAssetRespDTO asset, LocalDateTime now) {
        String nodeId = "image_ref_" + assetId + "_" + IdUtil.fastSimpleUUID();
        JSONObject data = new JSONObject()
                .set("imageId", nodeId)
                .set("assetId", assetId)
                .set("dataUrl", "")
                .set("fileName", quickGenerateReferenceFileName(asset, index))
                .set("mimeType", StrUtil.blankToDefault(asset == null ? null : asset.getMimeType(), "image/png"))
                .set("width", asset == null ? null : asset.getWidth())
                .set("height", asset == null ? null : asset.getHeight())
                .set("createdAt", now.toString())
                .set("kind", "uploaded")
                .set("status", "idle")
                .set("taskId", null)
                .set("errorMessage", null);
        JSONObject node = new JSONObject()
                .set("id", nodeId)
                .set("type", "image")
                .set("position", new JSONObject().set("x", -190).set("y", 120 + index * 180))
                .set("selected", false)
                .set("data", data);
        return new QuickGenerateReferenceNode(nodeId, node);
    }

    private String quickGenerateReferenceFileName(AigcAssetRespDTO asset, int index) {
        if (asset != null && StrUtil.isNotBlank(asset.getTitle())) {
            return asset.getTitle();
        }
        if (asset != null && StrUtil.isNotBlank(asset.getAssetNo())) {
            return asset.getAssetNo();
        }
        return "Reference " + (index + 1);
    }

    private JSONObject buildQuickGenerateReferenceEdge(String sourceNodeId, String targetNodeId) {
        return new JSONObject()
                .set("id", "e-" + sourceNodeId + "-" + targetNodeId + "-" + IdUtil.fastSimpleUUID())
                .set("source", sourceNodeId)
                .set("target", targetNodeId)
                .set("type", "signal");
    }

    private Map<Long, AigcAssetRespDTO> getAssetMapByIds(List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<AigcAssetRespDTO> assets = assetApi.getAssets(assetIds).getCheckedData();
            if (assets == null || assets.isEmpty()) {
                return Collections.emptyMap();
            }
            return assets.stream().collect(Collectors.toMap(AigcAssetRespDTO::getId, Function.identity(), (first, second) -> first));
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private JSONObject buildQuickGenerateImageNodeData(String nodeId, AigcCanvasProjectQuickGenerateReqVO reqVO,
                                                       LocalDateTime now) {
        return new JSONObject()
                .set("imageId", nodeId)
                .set("fileName", "Image")
                .set("mimeType", "image/png")
                .set("createdAt", now.toString())
                .set("kind", "draft")
                .set("prompt", reqVO.getPrompt())
                .set("modelId", String.valueOf(reqVO.getModelId()))
                .set("providerModel", reqVO.getProviderModel())
                .set("modelName", StrUtil.blankToDefault(reqVO.getModelName(), "Image"))
                .set("aigcModelId", reqVO.getModelId())
                .set("params", parseInputParamsForNode(reqVO.getInputParams()))
                .set("status", "idle")
                .set("taskId", null)
                .set("errorMessage", null)
                .set("generationStartedAt", null)
                .set("generationCompletedAt", null)
                .set("elapsedMs", null);
    }

    private JSONObject buildQuickGenerateVideoNodeData(String nodeId, AigcCanvasProjectQuickGenerateReqVO reqVO,
                                                       LocalDateTime now) {
        JSONObject params = parseInputParamsForNode(reqVO.getInputParams());
        String providerModel = StrUtil.blankToDefault(reqVO.getProviderModel(), String.valueOf(reqVO.getModelId()));
        return new JSONObject()
                .set("videoId", nodeId)
                .set("fileName", "Video")
                .set("mimeType", "video/mp4")
                .set("prompt", reqVO.getPrompt())
                .set("provider", inferVideoProvider(providerModel, reqVO.getModelName()))
                .set("modelId", providerModel)
                .set("providerModel", providerModel)
                .set("aigcModelId", reqVO.getModelId())
                .set("modelName", StrUtil.blankToDefault(reqVO.getModelName(), "Video"))
                .set("params", params)
                .set("kind", "draft")
                .set("status", "idle")
                .set("taskId", null)
                .set("videoUrl", null)
                .set("errorMessage", null)
                .set("ratio", StrUtil.blankToDefault(params.getStr("ratio"), "16:9"))
                .set("resolution", StrUtil.blankToDefault(params.getStr("resolution"), "1080p"))
                .set("duration", params.getInt("duration", 5))
                .set("size", StrUtil.blankToDefault(params.getStr("size"), "1280*704"))
                .set("generateAudio", params.getBool("generateAudio", true))
                .set("watermark", params.getBool("watermark", false))
                .set("createdAt", now.toString())
                .set("generationStartedAt", null)
                .set("generationCompletedAt", null)
                .set("generationRunStartedAt", null)
                .set("elapsedMs", null)
                .set("upstreamStatus", null);
    }

    private JSONObject parseInputParamsForNode(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSONObject(inputParams)) {
            return new JSONObject();
        }
        JSONObject params = JSONUtil.parseObj(inputParams);
        params.remove("referenceImages");
        params.remove("referenceAssetIds");
        params.remove("referenceImageIds");
        params.remove("inputImages");
        params.remove("inputImageUrls");
        params.remove("inputImageIds");
        return params;
    }

    private String normalizeQuickGenerateInputParams(AigcCanvasProjectQuickGenerateReqVO reqVO) {
        JSONObject params = StrUtil.isNotBlank(reqVO.getInputParams()) && JSONUtil.isTypeJSONObject(reqVO.getInputParams())
                ? JSONUtil.parseObj(reqVO.getInputParams())
                : new JSONObject();
        if (StrUtil.isNotBlank(reqVO.getProviderModel())) {
            params.set("providerModel", reqVO.getProviderModel());
        }
        List<Long> referenceAssetIds = quickGenerateReferenceAssetIds(reqVO);
        if (!referenceAssetIds.isEmpty() && !hasJsonArrayValue(params, "referenceAssetIds")) {
            params.set("referenceAssetIds", referenceAssetIds);
        }
        if (!referenceAssetIds.isEmpty() && !hasJsonArrayValue(params, "referenceImageIds")) {
            params.set("referenceImageIds", referenceAssetIds.stream().map(String::valueOf).toList());
        }
        List<String> referencePreviewUrls = quickGenerateReferencePreviewUrls(reqVO);
        if (!referencePreviewUrls.isEmpty() && !hasJsonArrayValue(params, "referenceImages")) {
            params.set("referenceImages", referencePreviewUrls);
        }
        return params.toString();
    }

    private List<Long> quickGenerateReferenceAssetIds(AigcCanvasProjectQuickGenerateReqVO reqVO) {
        if (reqVO.getReferenceAssetIds() != null && !reqVO.getReferenceAssetIds().isEmpty()) {
            return reqVO.getReferenceAssetIds().stream().filter(Objects::nonNull).distinct().toList();
        }
        return reqVO.getReferenceAssetId() == null ? Collections.emptyList() : List.of(reqVO.getReferenceAssetId());
    }

    private List<String> quickGenerateReferencePreviewUrls(AigcCanvasProjectQuickGenerateReqVO reqVO) {
        if (reqVO.getReferencePreviewUrls() != null && !reqVO.getReferencePreviewUrls().isEmpty()) {
            return reqVO.getReferencePreviewUrls().stream().filter(StrUtil::isNotBlank).distinct().toList();
        }
        return StrUtil.isBlank(reqVO.getReferencePreviewUrl()) ? Collections.emptyList() : List.of(reqVO.getReferencePreviewUrl());
    }

    private boolean hasJsonArrayValue(JSONObject params, String key) {
        Object value = params.get(key);
        if (value instanceof JSONArray array) {
            return !array.isEmpty();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return false;
    }

    private String inferVideoProvider(String providerModel, String modelName) {
        String value = (StrUtil.nullToEmpty(providerModel) + " " + StrUtil.nullToEmpty(modelName)).toLowerCase();
        return value.contains("wan") ? "wan" : "seedance";
    }

    private void bindReferenceAssetQuietly(Long projectId, String nodeId, Long assetId) {
        try {
            if (assetRefMapper.selectByNodeAndAsset(projectId, nodeId, assetId, "reference") != null) {
                return;
            }
            AigcCanvasAssetRefDO assetRef = new AigcCanvasAssetRefDO();
            assetRef.setProjectId(projectId);
            assetRef.setNodeId(nodeId);
            assetRef.setAssetId(assetId);
            assetRef.setUsageType("reference");
            assetRefMapper.insert(assetRef);
        } catch (Exception ignored) {
        }
    }

    private Long projectCurrentVersion(Long projectId) {
        AigcCanvasProjectDO project = projectMapper.selectById(projectId);
        return project == null || project.getCurrentVersion() == null ? 0L : project.getCurrentVersion();
    }

    private AigcCanvasOperationLogDO submitServerOperation(Long projectId, Long userId, String clientId, String opId,
                                                          String operationType, JSONObject payload, Long baseVersion) {
        AigcCanvasOperationSubmitReqVO operationReqVO = new AigcCanvasOperationSubmitReqVO();
        operationReqVO.setProjectId(projectId);
        operationReqVO.setClientId(clientId);
        operationReqVO.setOpId(opId);
        operationReqVO.setBaseVersion(baseVersion);
        operationReqVO.setOperationType(operationType);
        operationReqVO.setOperationJson(new JSONObject().set("type", operationType).set("payload", payload).toString());
        AigcCanvasOperationLogDO operation = operationService.submitOperation(operationReqVO, userId);
        roomService.broadcast(projectId, "canvas-op-applied", buildAppliedMessage(operation), null);
        return operation;
    }

    private CanvasCover findFirstImageNodeCover(Long projectId) {
        Map<String, JSONObject> nodes = rebuildProjectNodes(projectId);
        for (JSONObject node : nodes.values()) {
            String type = node.getStr("type");
            if (!"image".equals(type) && !"sketch".equals(type)) {
                continue;
            }
            JSONObject data = node.getJSONObject("data");
            if (data == null) {
                continue;
            }
            Long assetId = firstLong(data, "assetId", "outputAssetId", "previewAssetId");
            if (assetId != null) {
                String assetUrl = getAssetPreviewUrl(assetId);
                return new CanvasCover(assetId, assetUrl);
            }
            String previewUrl = firstText(data, "previewUrl", "outputPreviewUrl");
            if (isRemotePreviewUrl(previewUrl)) {
                return new CanvasCover(null, previewUrl);
            }
        }
        return findFirstImageAssetRefCover(projectId);
    }

    private CanvasCover findFirstImageAssetRefCover(Long projectId) {
        List<AigcCanvasAssetRefDO> refs = assetRefMapper.selectListByProjectId(projectId);
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        List<Long> assetIds = refs.stream().map(AigcCanvasAssetRefDO::getAssetId).filter(Objects::nonNull).distinct().toList();
        if (assetIds.isEmpty()) {
            return null;
        }
        try {
            List<AigcAssetRespDTO> assets = assetApi.getAssets(assetIds).getCheckedData();
            if (assets == null || assets.isEmpty()) {
                return null;
            }
            Map<Long, AigcAssetRespDTO> assetMap = assets.stream()
                    .collect(Collectors.toMap(AigcAssetRespDTO::getId, Function.identity(), (first, second) -> first));
            for (AigcCanvasAssetRefDO ref : refs) {
                AigcAssetRespDTO asset = assetMap.get(ref.getAssetId());
                if (asset == null || !"IMAGE".equals(asset.getAssetType())) {
                    continue;
                }
                String previewUrl = getAssetPreviewUrl(asset);
                return new CanvasCover(asset.getId(), previewUrl);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Map<String, JSONObject> rebuildProjectNodes(Long projectId) {
        Map<String, JSONObject> nodes = new LinkedHashMap<>();
        long afterVersion = 0L;
        AigcCanvasSnapshotDO snapshot = snapshotMapper.selectLatestByProjectId(projectId);
        if (snapshot != null && StrUtil.isNotBlank(snapshot.getNodesJson()) && JSONUtil.isTypeJSONArray(snapshot.getNodesJson())) {
            JSONArray snapshotNodes = JSONUtil.parseArray(snapshot.getNodesJson());
            for (Object item : snapshotNodes) {
                JSONObject node = JSONUtil.parseObj(item);
                if (StrUtil.isNotBlank(node.getStr("id"))) {
                    nodes.put(node.getStr("id"), node);
                }
            }
            afterVersion = snapshot.getVersion() == null ? 0L : snapshot.getVersion();
        }
        for (AigcCanvasOperationLogDO operation : operationLogMapper.selectListAfterVersion(projectId, afterVersion)) {
            applyOperationToNodeMap(nodes, operation);
        }
        return nodes;
    }

    private String sanitizeSnapshotNodesJson(String nodesJson) {
        if (StrUtil.isBlank(nodesJson) || !JSONUtil.isTypeJSONArray(nodesJson)) {
            return nodesJson;
        }
        JSONArray nodes = JSONUtil.parseArray(nodesJson);
        for (Object item : nodes) {
            JSONObject node = JSONUtil.parseObj(item);
            JSONObject data = node.getJSONObject("data");
            if (data != null && ("image".equals(node.getStr("type")) || "video".equals(node.getStr("type")))) {
                RUNTIME_ASSET_URL_NODE_DATA_KEYS.forEach(data::remove);
            }
        }
        return nodes.toString();
    }

    private void applyOperationToNodeMap(Map<String, JSONObject> nodes, AigcCanvasOperationLogDO operation) {
        if (StrUtil.isBlank(operation.getOperationJson()) || !JSONUtil.isTypeJSONObject(operation.getOperationJson())) {
            return;
        }
        JSONObject operationJson = JSONUtil.parseObj(operation.getOperationJson());
        JSONObject payload = operationJson.getJSONObject("payload");
        if (payload == null) {
            return;
        }
        switch (operation.getOperationType()) {
            case "NODE_CREATE" -> {
                JSONObject node = payload.getJSONObject("node");
                if (node != null && StrUtil.isNotBlank(node.getStr("id"))) {
                    nodes.put(node.getStr("id"), node);
                }
            }
            case "NODE_DELETE" -> nodes.remove(payload.getStr("nodeId"));
            case "NODE_UPDATE_DATA", "TASK_STATUS_PATCH", "ASSET_ATTACH" -> applyNodeMapPatch(nodes, payload);
            case "CANVAS_CLEAR" -> nodes.clear();
            default -> {
            }
        }
    }

    private void applyNodeMapPatch(Map<String, JSONObject> nodes, JSONObject payload) {
        JSONObject node = nodes.get(payload.getStr("nodeId"));
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
            for (String key : List.of("assetId", "assetVersionId", "sourceTaskId")) {
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

    private Long firstLong(JSONObject data, String... keys) {
        for (String key : keys) {
            Long value = data.getLong(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JSONObject data, String... keys) {
        for (String key : keys) {
            String value = data.getStr(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isRemotePreviewUrl(String url) {
        return StrUtil.isNotBlank(url)
                && !StrUtil.startWithIgnoreCase(url, "data:")
                && !StrUtil.startWithIgnoreCase(url, "blob:");
    }

    private String getAssetPreviewUrl(Long assetId) {
        if (assetId == null) {
            return null;
        }
        try {
            return getAssetPreviewUrl(assetApi.getAsset(assetId).getCheckedData());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getAssetPreviewUrl(AigcAssetRespDTO asset) {
        if (asset == null) {
            return null;
        }
        return StrUtil.blankToDefault(asset.getThumbnailUrl(),
                StrUtil.blankToDefault(asset.getCoverUrl(), asset.getFileUrl()));
    }

    private record CanvasCover(Long assetId, String coverUrl) {
    }

    private record CanvasProjectStatistics(Integer nodeCount, Integer assetCount) {
    }

    private record QuickGenerateReferenceNode(String nodeId, JSONObject node) {
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

    @Override
    public AigcCanvasProjectDO validateEditableProject(Long projectId, Long userId) {
        AigcCanvasProjectDO project = validateReadableProject(projectId, userId);
        if (isProjectOwner(project, userId)) {
            return project;
        }
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null || "viewer".equals(member.getRole())) {
            throw exception(CANVAS_NO_PERMISSION);
        }
        return project;
    }

    @Override
    public AigcCanvasProjectDO validateReadableProject(Long projectId, Long userId) {
        AigcCanvasProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            throw exception(CANVAS_PROJECT_NOT_EXISTS);
        }
        if (isProjectOwner(project, userId)) {
            return project;
        }
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null) {
            throw exception(CANVAS_NO_PERMISSION);
        }
        return project;
    }

    private AigcCanvasProjectDO validateOwnerProject(Long projectId, Long userId) {
        AigcCanvasProjectDO project = validateReadableProject(projectId, userId);
        if (isProjectOwner(project, userId)) {
            return project;
        }
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null || !MEMBER_ROLE_OWNER.equals(member.getRole())) {
            throw exception(CANVAS_NO_PERMISSION);
        }
        return project;
    }

    private AigcCanvasProjectRecycleBinDO validateOwnerRecycleBin(Long projectId, Long userId) {
        AigcCanvasProjectRecycleBinDO recycleBin = projectRecycleBinMapper.selectByProjectId(projectId);
        if (recycleBin == null) {
            throw exception(CANVAS_PROJECT_RECYCLE_BIN_NOT_EXISTS);
        }
        if (!Objects.equals(recycleBin.getOwnerUserId(), userId)) {
            throw exception(CANVAS_NO_PERMISSION);
        }
        return recycleBin;
    }

    private boolean isProjectOwner(AigcCanvasProjectDO project, Long userId) {
        return project != null && Objects.equals(project.getOwnerUserId(), userId);
    }

    private AigcCanvasMemberDO buildOwnerMember(AigcCanvasProjectDO project, Long userId) {
        AigcCanvasMemberDO member = new AigcCanvasMemberDO();
        member.setProjectId(project.getId());
        member.setUserId(userId);
        member.setRole(MEMBER_ROLE_OWNER);
        member.setJoinedTime(project.getCreateTime());
        member.setLastActiveTime(project.getUpdateTime());
        return member;
    }

    private AigcCanvasMemberDO validateProjectMember(Long projectId, Long memberId) {
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndId(projectId, memberId);
        if (member == null) {
            throw exception(CANVAS_MEMBER_NOT_EXISTS);
        }
        return member;
    }

    private void validateManageableRole(String role) {
        if (!MEMBER_ROLE_EDITOR.equals(role) && !MEMBER_ROLE_VIEWER.equals(role)) {
            throw exception(CANVAS_MEMBER_ROLE_INVALID);
        }
    }

    private void broadcastMemberUpdated(Long projectId, Long userId, String event) {
        roomService.broadcastMemberEvent(projectId, new AigcCanvasMemberMessage()
                .setProjectId(projectId)
                .setUserId(userId)
                .setEvent(event), null);
    }

}
