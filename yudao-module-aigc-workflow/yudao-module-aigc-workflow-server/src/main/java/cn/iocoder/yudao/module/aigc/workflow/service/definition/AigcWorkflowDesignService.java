package cn.iocoder.yudao.module.aigc.workflow.service.definition;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowEdgeSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowLogPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowNodeSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowVersionCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowEdgeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowVersionDO;

import java.util.List;

public interface AigcWorkflowDesignService {

    Long createNode(AigcWorkflowNodeSaveReqVO reqVO);

    void updateNode(AigcWorkflowNodeSaveReqVO reqVO);

    void deleteNode(Long id);

    List<AigcWorkflowNodeDO> getNodeList(Long workflowId, Long versionId);

    Long createEdge(AigcWorkflowEdgeSaveReqVO reqVO);

    void updateEdge(AigcWorkflowEdgeSaveReqVO reqVO);

    void deleteEdge(Long id);

    List<AigcWorkflowEdgeDO> getEdgeList(Long workflowId, Long versionId);

    Long createVersion(AigcWorkflowVersionCreateReqVO reqVO);

    List<AigcWorkflowVersionDO> getVersionList(Long workflowId);

    PageResult<AigcWorkflowLogDO> getLogPage(AigcWorkflowLogPageReqVO reqVO);

}
