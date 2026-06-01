package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberInviteReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasMemberUpdateRoleReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectUpdateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasAssetBindReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasSnapshotSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasMemberDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasProjectDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSnapshotDO;

import java.util.List;

public interface AigcCanvasProjectService {

    Long createProject(AigcCanvasProjectCreateReqVO reqVO, Long userId);

    void updateProject(AigcCanvasProjectUpdateReqVO reqVO, Long userId);

    AigcCanvasProjectDO getProject(Long id, Long userId);

    AigcCanvasMemberDO getProjectMember(Long id, Long userId);

    List<AigcCanvasMemberDO> getProjectMembers(Long id, Long userId);

    void inviteProjectMember(Long id, AigcCanvasMemberInviteReqVO reqVO, Long userId);

    void updateProjectMemberRole(Long id, Long memberId, AigcCanvasMemberUpdateRoleReqVO reqVO, Long userId);

    void removeProjectMember(Long id, Long memberId, Long userId);

    PageResult<AigcCanvasProjectDO> getProjectPage(AigcCanvasProjectPageReqVO reqVO, Long userId);

    AigcCanvasSnapshotDO getLatestSnapshot(Long projectId, Long userId);

    AigcCanvasSnapshotDO saveSnapshot(AigcCanvasSnapshotSaveReqVO reqVO, Long userId);

    Long bindAsset(AigcCanvasAssetBindReqVO reqVO, Long userId);

    AigcCanvasProjectDO validateEditableProject(Long projectId, Long userId);

    AigcCanvasProjectDO validateReadableProject(Long projectId, Long userId);

}
