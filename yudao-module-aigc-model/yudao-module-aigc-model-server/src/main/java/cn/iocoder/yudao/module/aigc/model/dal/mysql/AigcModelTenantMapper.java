package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcModelTenantMapper extends BaseMapperX<AigcModelTenantDO> {

    default AigcModelTenantDO selectByModelId(Long modelId) {
        return selectOne(AigcModelTenantDO::getModelId, modelId);
    }

    default AigcModelTenantDO selectByTenantIdAndModelId(Long tenantId, Long modelId) {
        return selectOne(new LambdaQueryWrapperX<AigcModelTenantDO>()
                .eq(AigcModelTenantDO::getTenantId, tenantId)
                .eq(AigcModelTenantDO::getModelId, modelId));
    }

    default List<AigcModelTenantDO> selectListByEnabledTrue(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<AigcModelTenantDO>()
                .eq(AigcModelTenantDO::getTenantId, tenantId)
                .eq(AigcModelTenantDO::getEnabled, true)
                .orderByAsc(AigcModelTenantDO::getSort)
                .orderByDesc(AigcModelTenantDO::getId));
    }

    default List<AigcModelTenantDO> selectListByTenantId(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<AigcModelTenantDO>()
                .eq(AigcModelTenantDO::getTenantId, tenantId));
    }

    default void deleteByModelId(Long modelId) {
        delete(AigcModelTenantDO::getModelId, modelId);
    }

    default int updateDefaultModelToFalse(Long tenantId, Long excludeId, Long modelId) {
        return update(null, new LambdaUpdateWrapper<AigcModelTenantDO>()
                .set(AigcModelTenantDO::getDefaultModel, false)
                .eq(AigcModelTenantDO::getTenantId, tenantId)
                .eq(AigcModelTenantDO::getDefaultModel, true)
                .ne(AigcModelTenantDO::getId, excludeId));
    }

}
