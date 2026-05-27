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

}
