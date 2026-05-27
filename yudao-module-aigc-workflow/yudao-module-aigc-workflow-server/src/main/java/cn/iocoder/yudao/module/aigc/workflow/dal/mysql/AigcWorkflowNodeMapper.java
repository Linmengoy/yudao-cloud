package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcWorkflowNodeMapper extends BaseMapperX<AigcWorkflowNodeDO> {

    default List<AigcWorkflowNodeDO> selectListByVersionId(Long workflowId, Long versionId) {
        return selectList(new LambdaQueryWrapperX<AigcWorkflowNodeDO>()
                .eq(AigcWorkflowNodeDO::getWorkflowId, workflowId)
                .eq(AigcWorkflowNodeDO::getVersionId, versionId)
                .orderByAsc(AigcWorkflowNodeDO::getId));
    }

    default List<AigcWorkflowNodeDO> selectDraftList(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<AigcWorkflowNodeDO>()
                .eq(AigcWorkflowNodeDO::getWorkflowId, workflowId)
                .isNull(AigcWorkflowNodeDO::getVersionId)
                .orderByAsc(AigcWorkflowNodeDO::getId));
    }

    default AigcWorkflowNodeDO selectByNodeKey(Long workflowId, Long versionId, String nodeKey) {
        LambdaQueryWrapperX<AigcWorkflowNodeDO> wrapper = new LambdaQueryWrapperX<AigcWorkflowNodeDO>()
                .eq(AigcWorkflowNodeDO::getWorkflowId, workflowId)
                .eq(AigcWorkflowNodeDO::getNodeKey, nodeKey);
        if (versionId == null) {
            wrapper.isNull(AigcWorkflowNodeDO::getVersionId);
        } else {
            wrapper.eq(AigcWorkflowNodeDO::getVersionId, versionId);
        }
        return selectOne(wrapper);
    }

    default int deleteDraftByWorkflowId(Long workflowId) {
        return delete(new LambdaQueryWrapperX<AigcWorkflowNodeDO>()
                .eq(AigcWorkflowNodeDO::getWorkflowId, workflowId)
                .isNull(AigcWorkflowNodeDO::getVersionId));
    }

}
