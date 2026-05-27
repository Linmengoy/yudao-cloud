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

}
