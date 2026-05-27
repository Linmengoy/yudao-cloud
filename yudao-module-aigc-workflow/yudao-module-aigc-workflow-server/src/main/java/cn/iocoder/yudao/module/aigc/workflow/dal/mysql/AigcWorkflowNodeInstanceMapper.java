package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeInstanceDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcWorkflowNodeInstanceMapper extends BaseMapperX<AigcWorkflowNodeInstanceDO> {

    default List<AigcWorkflowNodeInstanceDO> selectListByInstanceId(Long workflowInstanceId) {
        return selectList(new LambdaQueryWrapperX<AigcWorkflowNodeInstanceDO>()
                .eq(AigcWorkflowNodeInstanceDO::getWorkflowInstanceId, workflowInstanceId)
                .orderByAsc(AigcWorkflowNodeInstanceDO::getId));
    }

    default AigcWorkflowNodeInstanceDO selectByInstanceIdAndNodeKey(Long workflowInstanceId, String nodeKey) {
        return selectOne(AigcWorkflowNodeInstanceDO::getWorkflowInstanceId, workflowInstanceId,
                AigcWorkflowNodeInstanceDO::getNodeKey, nodeKey);
    }

}
