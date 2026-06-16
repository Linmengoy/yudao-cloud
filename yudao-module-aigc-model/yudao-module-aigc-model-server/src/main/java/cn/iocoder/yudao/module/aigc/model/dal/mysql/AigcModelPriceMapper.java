package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelPriceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AigcModelPriceMapper extends BaseMapperX<AigcModelPriceDO> {

    default AigcModelPriceDO selectByModelIdAndCapability(Long modelId, String capability, Long tenantId,
                                                          LocalDateTime now) {
        List<AigcModelPriceDO> list = selectList(new LambdaQueryWrapperX<AigcModelPriceDO>()
                .eq(AigcModelPriceDO::getModelId, modelId)
                .eq(AigcModelPriceDO::getCapability, capability)
                .in(AigcModelPriceDO::getTenantId, tenantId, 0L)
                .eq(AigcModelPriceDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .and(wrapper -> wrapper.isNull(AigcModelPriceDO::getEffectiveStartTime)
                        .or().le(AigcModelPriceDO::getEffectiveStartTime, now))
                .and(wrapper -> wrapper.isNull(AigcModelPriceDO::getEffectiveEndTime)
                        .or().ge(AigcModelPriceDO::getEffectiveEndTime, now))
                .orderByDesc(AigcModelPriceDO::getTenantId)
                .orderByDesc(AigcModelPriceDO::getEffectiveStartTime)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    default Long selectCountByModelIdAndCapabilityAndTenantId(Long id, Long modelId, String capability, Long tenantId) {
        return selectCount(new LambdaQueryWrapperX<AigcModelPriceDO>()
                .eq(AigcModelPriceDO::getModelId, modelId)
                .eq(AigcModelPriceDO::getCapability, capability)
                .eq(AigcModelPriceDO::getTenantId, tenantId)
                .neIfPresent(AigcModelPriceDO::getId, id));
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
                .eqIfPresent(AigcModelPriceDO::getModelId, modelId)
                .eqIfPresent(AigcModelPriceDO::getCapability, capability)
                .eq(AigcModelPriceDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(AigcModelPriceDO::getModelId)
                .orderByAsc(AigcModelPriceDO::getCapability)
                .orderByDesc(AigcModelPriceDO::getTenantId)
                .orderByDesc(AigcModelPriceDO::getId));
    }

}
