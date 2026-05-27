package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowInstancePageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowInstanceDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcWorkflowInstanceMapper extends BaseMapperX<AigcWorkflowInstanceDO> {

    default AigcWorkflowInstanceDO selectByInstanceNo(String instanceNo) {
        return selectOne(AigcWorkflowInstanceDO::getInstanceNo, instanceNo);
    }

    default PageResult<AigcWorkflowInstanceDO> selectPage(AigcWorkflowInstancePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcWorkflowInstanceDO>()
                .eqIfPresent(AigcWorkflowInstanceDO::getWorkflowId, reqVO.getWorkflowId())
                .eqIfPresent(AigcWorkflowInstanceDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcWorkflowInstanceDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcWorkflowInstanceDO::getId));
    }

}
