package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcModelChannelMapper extends BaseMapperX<AigcModelChannelDO> {

    default PageResult<AigcModelChannelDO> selectPage(AigcModelChannelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelChannelDO>()
                .eqIfPresent(AigcModelChannelDO::getModelId, reqVO.getModelId())
                .eqIfPresent(AigcModelChannelDO::getProviderId, reqVO.getProviderId())
                .likeIfPresent(AigcModelChannelDO::getName, reqVO.getName())
                .eqIfPresent(AigcModelChannelDO::getStatus, reqVO.getStatus())
                .orderByAsc(AigcModelChannelDO::getPriority)
                .orderByDesc(AigcModelChannelDO::getId));
    }

    default List<AigcModelChannelDO> selectListByModelId(Long modelId) {
        return selectList(new LambdaQueryWrapperX<AigcModelChannelDO>()
                .eq(AigcModelChannelDO::getModelId, modelId)
                .orderByAsc(AigcModelChannelDO::getPriority)
                .orderByDesc(AigcModelChannelDO::getId));
    }

    default List<AigcModelChannelDO> selectEnabledListByModelId(Long modelId) {
        return selectList(new LambdaQueryWrapperX<AigcModelChannelDO>()
                .eq(AigcModelChannelDO::getModelId, modelId)
                .eq(AigcModelChannelDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(AigcModelChannelDO::getPriority)
                .orderByDesc(AigcModelChannelDO::getWeight)
                .orderByDesc(AigcModelChannelDO::getId));
    }

    default List<AigcModelChannelDO> selectEnabledListByModelIds(Collection<Long> modelIds) {
        if (modelIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<AigcModelChannelDO>()
                .in(AigcModelChannelDO::getModelId, modelIds)
                .eq(AigcModelChannelDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

    default Long selectCountByProviderId(Long providerId) {
        return selectCount(AigcModelChannelDO::getProviderId, providerId);
    }

    default Long selectCountByModelIdAndProviderModel(Long id, Long modelId, Long providerId, String providerModel) {
        return selectCount(new LambdaQueryWrapperX<AigcModelChannelDO>()
                .eq(AigcModelChannelDO::getModelId, modelId)
                .eq(AigcModelChannelDO::getProviderId, providerId)
                .eq(AigcModelChannelDO::getProviderModel, providerModel)
                .neIfPresent(AigcModelChannelDO::getId, id));
    }

    default void deleteByModelId(Long modelId) {
        delete(AigcModelChannelDO::getModelId, modelId);
    }

}
