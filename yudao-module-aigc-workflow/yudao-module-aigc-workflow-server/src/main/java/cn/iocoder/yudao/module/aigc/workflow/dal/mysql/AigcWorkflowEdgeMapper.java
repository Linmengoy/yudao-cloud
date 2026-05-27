package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowEdgeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcWorkflowEdgeMapper extends BaseMapperX<AigcWorkflowEdgeDO> {

    default List<AigcWorkflowEdgeDO> selectListByVersionId(Long workflowId, Long versionId) {
        return selectList(new LambdaQueryWrapperX<AigcWorkflowEdgeDO>()
                .eq(AigcWorkflowEdgeDO::getWorkflowId, workflowId)
                .eq(AigcWorkflowEdgeDO::getVersionId, versionId)
                .orderByAsc(AigcWorkflowEdgeDO::getId));
    }

    default List<AigcWorkflowEdgeDO> selectDraftList(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<AigcWorkflowEdgeDO>()
                .eq(AigcWorkflowEdgeDO::getWorkflowId, workflowId)
                .isNull(AigcWorkflowEdgeDO::getVersionId)
                .orderByAsc(AigcWorkflowEdgeDO::getId));
    }

    default AigcWorkflowEdgeDO selectByEdgeKey(Long workflowId, Long versionId, String edgeKey) {
        LambdaQueryWrapperX<AigcWorkflowEdgeDO> wrapper = new LambdaQueryWrapperX<AigcWorkflowEdgeDO>()
                .eq(AigcWorkflowEdgeDO::getWorkflowId, workflowId)
                .eq(AigcWorkflowEdgeDO::getEdgeKey, edgeKey);
        if (versionId == null) {
            wrapper.isNull(AigcWorkflowEdgeDO::getVersionId);
        } else {
            wrapper.eq(AigcWorkflowEdgeDO::getVersionId, versionId);
        }
        return selectOne(wrapper);
    }

    default int deleteDraftByWorkflowId(Long workflowId) {
        return delete(new LambdaQueryWrapperX<AigcWorkflowEdgeDO>()
                .eq(AigcWorkflowEdgeDO::getWorkflowId, workflowId)
                .isNull(AigcWorkflowEdgeDO::getVersionId));
    }

}
