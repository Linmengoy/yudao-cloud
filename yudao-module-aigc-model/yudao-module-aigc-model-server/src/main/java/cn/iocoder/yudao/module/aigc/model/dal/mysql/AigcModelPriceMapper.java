package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelPriceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AigcModelPriceMapper extends BaseMapperX<AigcModelPriceDO> {

    default AigcModelPriceDO selectByModelIdAndCapability(Long modelId, String capability, Long tenantId) {
        List<AigcModelPriceDO> list = selectList(new LambdaQueryWrapperX<AigcModelPriceDO>()
                .eq(AigcModelPriceDO::getModelId, modelId)
                .eq(AigcModelPriceDO::getCapability, capability)
                .in(AigcModelPriceDO::getTenantId, tenantId, 0L)
                .eq(AigcModelPriceDO::getStatus, 1)
                .and(wrapper -> wrapper.isNull(AigcModelPriceDO::getEffectiveStartTime)
                        .or().le(AigcModelPriceDO::getEffectiveStartTime, LocalDateTime.now()))
                .and(wrapper -> wrapper.isNull(AigcModelPriceDO::getEffectiveEndTime)
                        .or().ge(AigcModelPriceDO::getEffectiveEndTime, LocalDateTime.now()))
                .orderByDesc(AigcModelPriceDO::getTenantId));
        return list.isEmpty() ? null : list.get(0);
    }

    default List<AigcModelPriceDO> selectListByModelId(Long modelId) {
        return selectList(new LambdaQueryWrapperX<AigcModelPriceDO>()
                .eq(AigcModelPriceDO::getModelId, modelId)
                .orderByDesc(AigcModelPriceDO::getTenantId)
                .orderByDesc(AigcModelPriceDO::getId));
    }

    default void deleteByModelId(Long modelId) {
        delete(AigcModelPriceDO::getModelId, modelId);
    }

    default void deleteByModelIdAndCapability(@Param("modelId") Long modelId, @Param("capability") String capability) {
        delete(new LambdaQueryWrapperX<AigcModelPriceDO>()
                .eq(AigcModelPriceDO::getModelId, modelId)
                .eq(AigcModelPriceDO::getCapability, capability));
    }

    default List<AigcModelPriceDO> selectListByModelIdAndCapability(Long modelId, String capability) {
        return selectList(new LambdaQueryWrapperX<AigcModelPriceDO>()
                .eq(AigcModelPriceDO::getModelId, modelId)
                .eq(AigcModelPriceDO::getCapability, capability)
                .eq(AigcModelPriceDO::getStatus, 1));
    }

}
