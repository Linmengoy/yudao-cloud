package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcWorkflowVersionMapper extends BaseMapperX<AigcWorkflowVersionDO> {

    default AigcWorkflowVersionDO selectLatestByWorkflowId(Long workflowId) {
        return selectOne(new LambdaQueryWrapperX<AigcWorkflowVersionDO>()
                .eq(AigcWorkflowVersionDO::getWorkflowId, workflowId)
                .orderByDesc(AigcWorkflowVersionDO::getVersionNo)
                .last("LIMIT 1"));
    }

    default List<AigcWorkflowVersionDO> selectListByWorkflowId(Long workflowId) {
        return selectList(new LambdaQueryWrapperX<AigcWorkflowVersionDO>()
                .eq(AigcWorkflowVersionDO::getWorkflowId, workflowId)
                .orderByDesc(AigcWorkflowVersionDO::getVersionNo));
    }

}
