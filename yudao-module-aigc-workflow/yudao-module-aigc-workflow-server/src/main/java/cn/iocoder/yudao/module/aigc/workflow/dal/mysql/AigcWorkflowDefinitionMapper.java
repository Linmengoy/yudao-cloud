package cn.iocoder.yudao.module.aigc.workflow.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcWorkflowDefinitionMapper extends BaseMapperX<AigcWorkflowDefinitionDO> {

    default AigcWorkflowDefinitionDO selectByCode(String code) {
        return selectOne(AigcWorkflowDefinitionDO::getCode, code);
    }

    default PageResult<AigcWorkflowDefinitionDO> selectPage(AigcWorkflowDefinitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcWorkflowDefinitionDO>()
                .likeIfPresent(AigcWorkflowDefinitionDO::getName, reqVO.getName())
                .eqIfPresent(AigcWorkflowDefinitionDO::getCode, reqVO.getCode())
                .eqIfPresent(AigcWorkflowDefinitionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AigcWorkflowDefinitionDO::getVisibility, reqVO.getVisibility())
                .eqIfPresent(AigcWorkflowDefinitionDO::getCategoryId, reqVO.getCategoryId())
                .orderByDesc(AigcWorkflowDefinitionDO::getId));
    }

}
