package cn.iocoder.yudao.module.aigc.workflow.service.definition;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowEdgeSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowLogPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowNodeSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowVersionCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowDefinitionDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowEdgeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowVersionDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowDefinitionMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowEdgeMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowLogMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowNodeMapper;
import cn.iocoder.yudao.module.aigc.workflow.dal.mysql.AigcWorkflowVersionMapper;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_DEFINITION_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_EDGE_KEY_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_EDGE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NODE_KEY_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_NODE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.workflow.enums.ErrorCodeConstants.WORKFLOW_VERSION_NODE_EMPTY;

@Service
@Validated
public class AigcWorkflowDesignServiceImpl implements AigcWorkflowDesignService {

    @Resource
    private AigcWorkflowDefinitionMapper definitionMapper;
    @Resource
    private AigcWorkflowNodeMapper nodeMapper;
    @Resource
    private AigcWorkflowEdgeMapper edgeMapper;
    @Resource
    private AigcWorkflowVersionMapper versionMapper;
    @Resource
    private AigcWorkflowLogMapper logMapper;

    @Override
    public Long createNode(AigcWorkflowNodeSaveReqVO reqVO) {
        validateDefinitionExists(reqVO.getWorkflowId());
        validateNodeKeyUnique(reqVO.getWorkflowId(), reqVO.getVersionId(), reqVO.getNodeKey(), null);
        AigcWorkflowNodeDO node = BeanUtils.toBean(reqVO, AigcWorkflowNodeDO.class);
        nodeMapper.insert(node);
        return node.getId();
    }

    @Override
    public void updateNode(AigcWorkflowNodeSaveReqVO reqVO) {
        AigcWorkflowNodeDO node = validateNodeExists(reqVO.getId());
        validateNodeKeyUnique(reqVO.getWorkflowId(), reqVO.getVersionId(), reqVO.getNodeKey(), reqVO.getId());
        nodeMapper.updateById(BeanUtils.toBean(reqVO, AigcWorkflowNodeDO.class)
                .setWorkflowId(node.getWorkflowId())
                .setVersionId(node.getVersionId()));
    }

    @Override
    public void deleteNode(Long id) {
        validateNodeExists(id);
        nodeMapper.deleteById(id);
    }

    @Override
    public List<AigcWorkflowNodeDO> getNodeList(Long workflowId, Long versionId) {
        return versionId == null ? nodeMapper.selectDraftList(workflowId) : nodeMapper.selectListByVersionId(workflowId, versionId);
    }

    @Override
    public Long createEdge(AigcWorkflowEdgeSaveReqVO reqVO) {
        validateDefinitionExists(reqVO.getWorkflowId());
        validateEdgeKeyUnique(reqVO.getWorkflowId(), reqVO.getVersionId(), reqVO.getEdgeKey(), null);
        AigcWorkflowEdgeDO edge = BeanUtils.toBean(reqVO, AigcWorkflowEdgeDO.class);
        edgeMapper.insert(edge);
        return edge.getId();
    }

    @Override
    public void updateEdge(AigcWorkflowEdgeSaveReqVO reqVO) {
        AigcWorkflowEdgeDO edge = validateEdgeExists(reqVO.getId());
        validateEdgeKeyUnique(reqVO.getWorkflowId(), reqVO.getVersionId(), reqVO.getEdgeKey(), reqVO.getId());
        edgeMapper.updateById(BeanUtils.toBean(reqVO, AigcWorkflowEdgeDO.class)
                .setWorkflowId(edge.getWorkflowId())
                .setVersionId(edge.getVersionId()));
    }

    @Override
    public void deleteEdge(Long id) {
        validateEdgeExists(id);
        edgeMapper.deleteById(id);
    }

    @Override
    public List<AigcWorkflowEdgeDO> getEdgeList(Long workflowId, Long versionId) {
        return versionId == null ? edgeMapper.selectDraftList(workflowId) : edgeMapper.selectListByVersionId(workflowId, versionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(AigcWorkflowVersionCreateReqVO reqVO) {
        AigcWorkflowDefinitionDO definition = validateDefinitionExists(reqVO.getWorkflowId());
        List<AigcWorkflowNodeDO> draftNodes = nodeMapper.selectDraftList(reqVO.getWorkflowId());
        if (draftNodes.isEmpty()) {
            throw exception(WORKFLOW_VERSION_NODE_EMPTY);
        }
        List<AigcWorkflowEdgeDO> draftEdges = edgeMapper.selectDraftList(reqVO.getWorkflowId());
        AigcWorkflowVersionDO latestVersion = versionMapper.selectLatestByWorkflowId(reqVO.getWorkflowId());
        int nextVersionNo = latestVersion == null ? 1 : latestVersion.getVersionNo() + 1;
        AigcWorkflowVersionDO version = new AigcWorkflowVersionDO()
                .setWorkflowId(reqVO.getWorkflowId())
                .setVersionNo(nextVersionNo)
                .setVersionName(reqVO.getVersionName())
                .setDefinitionSnapshot(JsonUtils.toJsonString(definition))
                .setNodeSnapshot(JsonUtils.toJsonString(draftNodes))
                .setEdgeSnapshot(JsonUtils.toJsonString(draftEdges))
                .setStatus(AigcWorkflowStatusEnum.PUBLISHED.getCode());
        versionMapper.insert(version);
        draftNodes.forEach(node -> nodeMapper.insert(copyNodeForVersion(node, version.getId())));
        draftEdges.forEach(edge -> edgeMapper.insert(copyEdgeForVersion(edge, version.getId())));
        definitionMapper.updateById(new AigcWorkflowDefinitionDO()
                .setId(reqVO.getWorkflowId())
                .setCurrentVersionId(version.getId())
                .setStatus(AigcWorkflowStatusEnum.PUBLISHED.getCode()));
        return version.getId();
    }

    @Override
    public List<AigcWorkflowVersionDO> getVersionList(Long workflowId) {
        return versionMapper.selectListByWorkflowId(workflowId);
    }

    @Override
    public PageResult<AigcWorkflowLogDO> getLogPage(AigcWorkflowLogPageReqVO reqVO) {
        return logMapper.selectPage(reqVO);
    }

    private AigcWorkflowDefinitionDO validateDefinitionExists(Long id) {
        AigcWorkflowDefinitionDO definition = definitionMapper.selectById(id);
        if (definition == null) {
            throw exception(WORKFLOW_DEFINITION_NOT_EXISTS);
        }
        return definition;
    }

    private AigcWorkflowNodeDO validateNodeExists(Long id) {
        AigcWorkflowNodeDO node = nodeMapper.selectById(id);
        if (node == null) {
            throw exception(WORKFLOW_NODE_NOT_EXISTS);
        }
        return node;
    }

    private AigcWorkflowEdgeDO validateEdgeExists(Long id) {
        AigcWorkflowEdgeDO edge = edgeMapper.selectById(id);
        if (edge == null) {
            throw exception(WORKFLOW_EDGE_NOT_EXISTS);
        }
        return edge;
    }

    private void validateNodeKeyUnique(Long workflowId, Long versionId, String nodeKey, Long id) {
        AigcWorkflowNodeDO node = nodeMapper.selectByNodeKey(workflowId, versionId, nodeKey);
        if (node != null && (id == null || !node.getId().equals(id))) {
            throw exception(WORKFLOW_NODE_KEY_EXISTS);
        }
    }

    private void validateEdgeKeyUnique(Long workflowId, Long versionId, String edgeKey, Long id) {
        AigcWorkflowEdgeDO edge = edgeMapper.selectByEdgeKey(workflowId, versionId, edgeKey);
        if (edge != null && (id == null || !edge.getId().equals(id))) {
            throw exception(WORKFLOW_EDGE_KEY_EXISTS);
        }
    }

    private AigcWorkflowNodeDO copyNodeForVersion(AigcWorkflowNodeDO source, Long versionId) {
        return BeanUtils.toBean(source, AigcWorkflowNodeDO.class)
                .setId(null)
                .setVersionId(versionId);
    }

    private AigcWorkflowEdgeDO copyEdgeForVersion(AigcWorkflowEdgeDO source, Long versionId) {
        return BeanUtils.toBean(source, AigcWorkflowEdgeDO.class)
                .setId(null)
                .setVersionId(versionId);
    }

}
