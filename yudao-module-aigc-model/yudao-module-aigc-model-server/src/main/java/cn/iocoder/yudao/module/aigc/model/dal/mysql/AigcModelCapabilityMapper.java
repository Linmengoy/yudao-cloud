package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelCapabilityDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AigcModelCapabilityMapper extends BaseMapperX<AigcModelCapabilityDO> {

    default AigcModelCapabilityDO selectByModelIdAndCapability(Long modelId, String capability) {
        return selectOne(new LambdaQueryWrapperX<AigcModelCapabilityDO>()
                .eq(AigcModelCapabilityDO::getModelId, modelId)
                .eq(AigcModelCapabilityDO::getCapability, capability));
    }

    default List<AigcModelCapabilityDO> selectListByModelId(Long modelId) {
        return selectList(AigcModelCapabilityDO::getModelId, modelId);
    }

    default Long selectCountByCapability(String capability) {
        return selectCount(AigcModelCapabilityDO::getCapability, capability);
    }

    @Delete("DELETE FROM aigc_model_capability WHERE model_id = #{modelId}")
    void deleteByModelId(@Param("modelId") Long modelId);

    default List<AigcModelCapabilityDO> selectListByCapability(String capability) {
        return selectList(new LambdaQueryWrapperX<AigcModelCapabilityDO>()
                .eq(AigcModelCapabilityDO::getCapability, capability)
                .eq(AigcModelCapabilityDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(AigcModelCapabilityDO::getModelId));
    }

    default Long selectCountByModelIdAndCapability(Long modelId, String capability) {
        return selectCount(new LambdaQueryWrapperX<AigcModelCapabilityDO>()
                .eq(AigcModelCapabilityDO::getModelId, modelId)
                .eq(AigcModelCapabilityDO::getCapability, capability));
    }

}
