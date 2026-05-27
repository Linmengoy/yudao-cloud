package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcWorkflowLogMapper extends BaseMapperX<AigcWorkflowLogDO> {
}
