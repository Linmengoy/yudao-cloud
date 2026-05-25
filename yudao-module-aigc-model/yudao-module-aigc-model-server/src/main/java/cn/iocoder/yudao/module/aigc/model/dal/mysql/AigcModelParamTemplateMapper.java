package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AigcModelParamTemplateMapper extends BaseMapperX<AigcModelParamTemplateDO> {

    default AigcModelParamTemplateDO selectByModelIdAndCapabilityAndParamKey(Long modelId, String capability, String paramKey) {
        return selectOne(new LambdaQueryWrapperX<AigcModelParamTemplateDO>()
                .eq(AigcModelParamTemplateDO::getModelId, modelId)
                .eq(AigcModelParamTemplateDO::getCapability, capability)
                .eq(AigcModelParamTemplateDO::getParamKey, paramKey));
    }

    default List<AigcModelParamTemplateDO> selectListByModelIdAndCapability(Long modelId, String capability) {
        return selectList(new LambdaQueryWrapperX<AigcModelParamTemplateDO>()
                .eq(AigcModelParamTemplateDO::getModelId, modelId)
                .eq(AigcModelParamTemplateDO::getCapability, capability)
                .eq(AigcModelParamTemplateDO::getStatus, 1)
                .orderByAsc(AigcModelParamTemplateDO::getSort));
    }

    default void deleteByModelId(Long modelId) {
        delete(AigcModelParamTemplateDO::getModelId, modelId);
    }

    default void deleteByModelIdAndCapability(@Param("modelId") Long modelId, @Param("capability") String capability) {
        delete(new LambdaQueryWrapperX<AigcModelParamTemplateDO>()
                .eq(AigcModelParamTemplateDO::getModelId, modelId)
                .eq(AigcModelParamTemplateDO::getCapability, capability));
    }

}
