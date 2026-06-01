package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsagePageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcModelUsageLogMapper extends BaseMapperX<AigcModelUsageLogDO> {

    default PageResult<AigcModelUsageLogDO> selectPage(AigcModelUsagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelUsageLogDO>()
                .eqIfPresent(AigcModelUsageLogDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(AigcModelUsageLogDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcModelUsageLogDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcModelUsageLogDO::getProviderId, reqVO.getProviderId())
                .eqIfPresent(AigcModelUsageLogDO::getCapability, reqVO.getCapability())
                .eqIfPresent(AigcModelUsageLogDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcModelUsageLogDO::getId));
    }

}
