package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRoutePageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelRouteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcModelRouteMapper extends BaseMapperX<AigcModelRouteDO> {

    default PageResult<AigcModelRouteDO> selectPage(AigcModelRoutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelRouteDO>()
                .likeIfPresent(AigcModelRouteDO::getName, reqVO.getName())
                .eqIfPresent(AigcModelRouteDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(AigcModelRouteDO::getCapability, reqVO.getCapability())
                .eqIfPresent(AigcModelRouteDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcModelRouteDO::getId));
    }

    default List<AigcModelRouteDO> selectListByTaskTypeAndCapability(String taskType, String capability) {
        return selectList(new LambdaQueryWrapperX<AigcModelRouteDO>()
                .eqIfPresent(AigcModelRouteDO::getTaskType, taskType)
                .eqIfPresent(AigcModelRouteDO::getCapability, capability)
                .eq(AigcModelRouteDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

}
