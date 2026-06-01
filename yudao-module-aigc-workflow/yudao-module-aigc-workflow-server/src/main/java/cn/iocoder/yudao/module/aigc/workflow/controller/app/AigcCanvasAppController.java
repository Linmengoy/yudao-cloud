package cn.iocoder.yudao.module.aigc.workflow.controller.app;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberInviteReqVO;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberUpdateRoleReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasNodeRunSyncReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSyncRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectUpdateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasAssetBindReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSnapshotRespVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSnapshotSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasMemberDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasOperationService;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasNodeRunService;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 画布")
@RestController
@RequestMapping("/canvas")
@Validated
public class AigcCanvasAppController {

    @Resource
    private AigcCanvasProjectService projectService;
    @Resource
    private AigcCanvasOperationService operationService;
    @Resource
    private AigcCanvasNodeRunService nodeRunService;
    @Resource
    private AigcAssetApi assetApi;

    @GetMapping("/projects")
    @Operation(summary = "画布项目分页")
    public CommonResult<PageResult<AigcCanvasProjectRespVO>> getProjectPage(@Valid AigcCanvasProjectPageReqVO reqVO) {
        Long userId = getLoginUserId();
        PageResult<AigcCanvasProjectDO> pageResult = projectService.getProjectPage(reqVO, userId);
        PageResult<AigcCanvasProjectRespVO> respPageResult = BeanUtils.toBean(pageResult, AigcCanvasProjectRespVO.class);
        respPageResult.getList().forEach(project -> {
            fillProjectPermissions(project, projectService.getProjectMember(project.getId(), userId));
            fillProjectCover(project);
        });
        return success(respPageResult);
    }

    @PostMapping("/projects")
    @Operation(summary = "创建画布项目")
    public CommonResult<Long> createProject(@Valid @RequestBody AigcCanvasProjectCreateReqVO reqVO) {
        return success(projectService.createProject(reqVO, getLoginUserId()));
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "画布项目详情")
    @Parameter(name = "id", description = "项目编号", required = true)
    public CommonResult<AigcCanvasProjectRespVO> getProject(@PathVariable("id") Long id) {
        Long userId = getLoginUserId();
        AigcCanvasProjectRespVO project = BeanUtils.toBean(projectService.getProject(id, userId), AigcCanvasProjectRespVO.class);
        fillProjectPermissions(project, projectService.getProjectMember(id, userId));
        fillProjectCover(project);
        return success(project);
    }

    @GetMapping("/projects/{id}/members")
    @Operation(summary = "画布项目成员列表")
    public CommonResult<List<AigcCanvasMemberRespVO>> getProjectMembers(@PathVariable("id") Long id) {
        return success(BeanUtils.toBean(projectService.getProjectMembers(id, getLoginUserId()), AigcCanvasMemberRespVO.class));
    }

    @PostMapping("/projects/{id}/members")
    @Operation(summary = "邀请画布项目成员")
    public CommonResult<Boolean> inviteProjectMember(@PathVariable("id") Long id,
                                                     @Valid @RequestBody AigcCanvasMemberInviteReqVO reqVO) {
        projectService.inviteProjectMember(id, reqVO, getLoginUserId());
        return success(true);
    }

    @PutMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "更新画布项目成员角色")
    public CommonResult<Boolean> updateProjectMemberRole(@PathVariable("id") Long id,
                                                         @PathVariable("memberId") Long memberId,
                                                         @Valid @RequestBody AigcCanvasMemberUpdateRoleReqVO reqVO) {
        projectService.updateProjectMemberRole(id, memberId, reqVO, getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/projects/{id}/members/{memberId}")
    @Operation(summary = "移除画布项目成员")
    public CommonResult<Boolean> removeProjectMember(@PathVariable("id") Long id,
                                                     @PathVariable("memberId") Long memberId) {
        projectService.removeProjectMember(id, memberId, getLoginUserId());
        return success(true);
    }

    @PutMapping("/projects/{id}")
    @Operation(summary = "更新画布项目")
    public CommonResult<Boolean> updateProject(@PathVariable("id") Long id, @Valid @RequestBody AigcCanvasProjectUpdateReqVO reqVO) {
        reqVO.setId(id);
        projectService.updateProject(reqVO, getLoginUserId());
        return success(true);
    }

    @GetMapping("/projects/{id}/snapshot")
    @Operation(summary = "获取最新画布快照")
    public CommonResult<AigcCanvasSnapshotRespVO> getLatestSnapshot(@PathVariable("id") Long id) {
        return success(BeanUtils.toBean(projectService.getLatestSnapshot(id, getLoginUserId()), AigcCanvasSnapshotRespVO.class));
    }

    @PostMapping("/projects/{id}/snapshot")
    @Operation(summary = "保存画布快照")
    public CommonResult<AigcCanvasSnapshotRespVO> saveSnapshot(@PathVariable("id") Long id, @Valid @RequestBody AigcCanvasSnapshotSaveReqVO reqVO) {
        reqVO.setProjectId(id);
        return success(BeanUtils.toBean(projectService.saveSnapshot(reqVO, getLoginUserId()), AigcCanvasSnapshotRespVO.class));
    }

    @GetMapping("/projects/{id}/operations")
    @Operation(summary = "获取指定版本后的画布操作")
    public CommonResult<List<AigcCanvasOperationRespVO>> getOperations(@PathVariable("id") Long id,
                                                                       @RequestParam(value = "afterVersion", required = false) Long afterVersion) {
        return success(BeanUtils.toBean(operationService.getOperationsAfterVersion(id, afterVersion, getLoginUserId()), AigcCanvasOperationRespVO.class));
    }

    @GetMapping("/projects/{id}/operations/sync")
    @Operation(summary = "同步画布操作或快照")
    public CommonResult<AigcCanvasOperationSyncRespVO> syncOperations(@PathVariable("id") Long id,
                                                                      @RequestParam(value = "afterVersion", required = false) Long afterVersion) {
        return success(operationService.syncOperations(id, afterVersion, getLoginUserId()));
    }

    @PostMapping("/projects/{id}/operations")
    @Operation(summary = "提交画布操作")
    public CommonResult<AigcCanvasOperationRespVO> submitOperation(@PathVariable("id") Long id,
                                                                   @Valid @RequestBody AigcCanvasOperationSubmitReqVO reqVO) {
        reqVO.setProjectId(id);
        return success(BeanUtils.toBean(operationService.submitOperation(reqVO, getLoginUserId()), AigcCanvasOperationRespVO.class));
    }

    @PostMapping("/projects/{id}/nodes/{nodeId}/assets")
    @Operation(summary = "绑定画布节点资产")
    public CommonResult<Long> bindNodeAsset(@PathVariable("id") Long id,
                                            @PathVariable("nodeId") String nodeId,
                                            @Valid @RequestBody AigcCanvasAssetBindReqVO reqVO) {
        reqVO.setProjectId(id);
        reqVO.setNodeId(nodeId);
        return success(projectService.bindAsset(reqVO, getLoginUserId()));
    }

    @PostMapping("/projects/{id}/nodes/{nodeId}/run")
    @Operation(summary = "运行画布节点")
    public CommonResult<AigcCanvasNodeRunRespVO> runNode(@PathVariable("id") Long id,
                                                        @PathVariable("nodeId") String nodeId,
                                                        @Valid @RequestBody AigcCanvasNodeRunReqVO reqVO) {
        reqVO.setProjectId(id);
        reqVO.setNodeId(nodeId);
        return success(nodeRunService.runNode(reqVO, getLoginUserId()));
    }

    @PostMapping("/projects/{id}/nodes/{nodeId}/run/sync")
    @Operation(summary = "同步画布节点运行结果")
    public CommonResult<AigcCanvasNodeRunRespVO> syncNodeRun(@PathVariable("id") Long id,
                                                            @PathVariable("nodeId") String nodeId,
                                                            @Valid @RequestBody AigcCanvasNodeRunSyncReqVO reqVO) {
        reqVO.setProjectId(id);
        reqVO.setNodeId(nodeId);
        return success(nodeRunService.syncNodeRun(reqVO, getLoginUserId()));
    }

    private void fillProjectPermissions(AigcCanvasProjectRespVO project, AigcCanvasMemberDO member) {
        String role = member == null ? null : member.getRole();
        boolean canEdit = role != null && !"viewer".equals(role);
        project.setRole(role);
        project.setCanEdit(canEdit);
        project.setReadonly(!canEdit);
    }

    private void fillProjectCover(AigcCanvasProjectRespVO project) {
        if (project.getCoverAssetId() == null) {
            return;
        }
        try {
            AigcAssetRespDTO asset = assetApi.getAsset(project.getCoverAssetId()).getCheckedData();
            project.setCoverUrl(StrUtil.blankToDefault(asset.getThumbnailUrl(),
                    StrUtil.blankToDefault(asset.getCoverUrl(), asset.getFileUrl())));
        } catch (Exception ignored) {
        }
    }

}
