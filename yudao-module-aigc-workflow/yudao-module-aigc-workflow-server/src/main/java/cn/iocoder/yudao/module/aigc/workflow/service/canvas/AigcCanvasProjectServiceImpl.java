package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberInviteReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberUpdateRoleReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectUpdateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasAssetBindReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSnapshotSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasAssetRefDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasMemberDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSnapshotDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasMemberMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasAssetRefMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasOperationLogMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.canvas.AigcCanvasProjectMapper;
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
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_MEMBER_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_MEMBER_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_MEMBER_ROLE_INVALID;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_NO_PERMISSION;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_OWNER_CAN_NOT_CHANGE;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_PROJECT_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.CANVAS_SNAPSHOT_VERSION_CONFLICT;

@Service
@Validated
public class AigcCanvasProjectServiceImpl implements AigcCanvasProjectService {

    private static final String PROJECT_STATUS_NORMAL = "NORMAL";
    private static final String MEMBER_ROLE_OWNER = "owner";
    private static final String MEMBER_ROLE_EDITOR = "editor";
    private static final String MEMBER_ROLE_VIEWER = "viewer";

    @Resource
    private AigcCanvasProjectMapper projectMapper;
    @Resource
    private AigcCanvasMemberMapper memberMapper;
    @Resource
    private AigcCanvasSnapshotMapper snapshotMapper;
    @Resource
    private AigcCanvasAssetRefMapper assetRefMapper;
    @Resource
    private AigcCanvasOperationLogMapper operationLogMapper;
    @Resource
    private AigcCanvasRoomService roomService;
    @Resource
    @Lazy
    private AigcCanvasOperationService operationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(AigcCanvasProjectCreateReqVO reqVO, Long userId) {
        AigcCanvasProjectDO project = BeanUtils.toBean(reqVO, AigcCanvasProjectDO.class);
        project.setOwnerUserId(userId);
        project.setKind(StrUtil.blankToDefault(reqVO.getKind(), "image"));
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
    public void updateProject(AigcCanvasProjectUpdateReqVO reqVO, Long userId) {
        validateEditableProject(reqVO.getId(), userId);
        projectMapper.updateById(BeanUtils.toBean(reqVO, AigcCanvasProjectDO.class));
    }

    @Override
    public AigcCanvasProjectDO getProject(Long id, Long userId) {
        return validateReadableProject(id, userId);
    }

    @Override
    public AigcCanvasMemberDO getProjectMember(Long id, Long userId) {
        AigcCanvasProjectDO project = validateReadableProject(id, userId);
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndUserId(id, userId);
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
    public PageResult<AigcCanvasProjectDO> getProjectPage(AigcCanvasProjectPageReqVO reqVO, Long userId) {
        return projectMapper.selectPage(reqVO, userId);
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
        AigcCanvasSnapshotDO snapshot = BeanUtils.toBean(reqVO, AigcCanvasSnapshotDO.class);
        snapshot.setVersion(nextVersion);
        snapshot.setCreatedBy(userId);
        snapshotMapper.insert(snapshot);

        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(project.getId());
        update.setCurrentVersion(nextVersion);
        update.setLatestSnapshotId(snapshot.getId());
        update.setNodeCount(reqVO.getNodeCount());
        update.setAssetCount(reqVO.getAssetCount());
        projectMapper.updateById(update);
        operationService.invalidateGraphState(project.getId());
        return snapshot;
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
            return existed.getId();
        }
        AigcCanvasAssetRefDO assetRef = BeanUtils.toBean(reqVO, AigcCanvasAssetRefDO.class);
        assetRef.setUsageType(usageType);
        assetRefMapper.insert(assetRef);
        operationService.invalidateGraphState(project.getId());
        AigcCanvasOperationLogDO operation = createAssetAttachOperation(reqVO, project, usageType, userId);
        operationService.invalidateGraphState(project.getId());
        roomService.broadcast(project.getId(), "canvas-op-applied", buildAppliedMessage(operation), null);
        return assetRef.getId();
    }

    private AigcCanvasOperationLogDO createAssetAttachOperation(AigcCanvasAssetBindReqVO reqVO, AigcCanvasProjectDO project,
                                                               String usageType, Long userId) {
        long nextVersion = project.getCurrentVersion() == null ? 1L : project.getCurrentVersion() + 1;
        JSONObject payload = new JSONObject()
                .set("nodeId", reqVO.getNodeId())
                .set("assetId", reqVO.getAssetId())
                .set("assetVersionId", reqVO.getAssetVersionId())
                .set("previewUrl", reqVO.getPreviewUrl())
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
        update.setAssetCount(project.getAssetCount() == null ? 1 : project.getAssetCount() + 1);
        if (project.getCoverAssetId() == null) {
            update.setCoverAssetId(reqVO.getAssetId());
        }
        projectMapper.updateById(update);
        return operation;
    }

    private void updateProjectCoverIfAbsent(AigcCanvasProjectDO project, Long assetId) {
        if (project.getCoverAssetId() != null || assetId == null) {
            return;
        }
        AigcCanvasProjectDO update = new AigcCanvasProjectDO();
        update.setId(project.getId());
        update.setCoverAssetId(assetId);
        projectMapper.updateById(update);
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

    private void validateOwnerProject(Long projectId, Long userId) {
        AigcCanvasProjectDO project = validateReadableProject(projectId, userId);
        if (isProjectOwner(project, userId)) {
            return;
        }
        AigcCanvasMemberDO member = memberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (member == null || !MEMBER_ROLE_OWNER.equals(member.getRole())) {
            throw exception(CANVAS_NO_PERMISSION);
        }
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
