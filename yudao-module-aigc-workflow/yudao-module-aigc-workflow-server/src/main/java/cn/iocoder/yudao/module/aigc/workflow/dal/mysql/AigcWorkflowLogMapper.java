package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowLogPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcWorkflowLogMapper extends BaseMapperX<AigcWorkflowLogDO> {

    default PageResult<AigcWorkflowLogDO> selectPage(AigcWorkflowLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcWorkflowLogDO>()
                .eqIfPresent(AigcWorkflowLogDO::getWorkflowInstanceId, reqVO.getWorkflowInstanceId())
                .eqIfPresent(AigcWorkflowLogDO::getNodeInstanceId, reqVO.getNodeInstanceId())
                .eqIfPresent(AigcWorkflowLogDO::getLogType, reqVO.getLogType())
                .orderByDesc(AigcWorkflowLogDO::getId));
    }

}
