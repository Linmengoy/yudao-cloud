package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderPageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcModelProviderMapper extends BaseMapperX<AigcModelProviderDO> {

    default AigcModelProviderDO selectByCode(String code) {
        return selectOne(AigcModelProviderDO::getCode, code);
    }

    default Long selectCountByTenantId(Long tenantId) {
        return selectCount(AigcModelProviderDO::getTenantId, tenantId);
    }

    default Long selectCountByProxyId(Long proxyId) {
        return selectCount(AigcModelProviderDO::getProxyId, proxyId);
    }

    default PageResult<AigcModelProviderDO> selectPage(AigcModelProviderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelProviderDO>()
                .likeIfPresent(AigcModelProviderDO::getCode, reqVO.getCode())
                .likeIfPresent(AigcModelProviderDO::getName, reqVO.getName())
                .eqIfPresent(AigcModelProviderDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcModelProviderDO::getId));
    }

    default List<AigcModelProviderDO> selectEnabledListByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<AigcModelProviderDO>()
                .in(AigcModelProviderDO::getId, ids)
                .eq(AigcModelProviderDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }

}
