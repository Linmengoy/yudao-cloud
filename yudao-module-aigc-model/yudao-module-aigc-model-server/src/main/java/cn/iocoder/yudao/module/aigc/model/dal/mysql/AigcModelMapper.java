package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcModelMapper extends BaseMapperX<AigcModelDO> {

    default AigcModelDO selectByCode(String code) {
        return selectOne(AigcModelDO::getCode, code);
    }

    default Long selectCountByProviderId(Long providerId) {
        return selectCount(AigcModelDO::getProviderId, providerId);
    }

    default PageResult<AigcModelDO> selectPage(AigcModelPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelDO>()
                .eqIfPresent(AigcModelDO::getProviderId, reqVO.getProviderId())
                .likeIfPresent(AigcModelDO::getCode, reqVO.getCode())
                .likeIfPresent(AigcModelDO::getName, reqVO.getName())
                .eqIfPresent(AigcModelDO::getType, reqVO.getType())
                .eqIfPresent(AigcModelDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcModelDO::getId));
    }

    default List<AigcModelDO> selectListByTypeAndStatus(Integer type, Integer status) {
        return selectList(new LambdaQueryWrapperX<AigcModelDO>()
                .eq(AigcModelDO::getType, type)
                .eq(AigcModelDO::getStatus, status)
                .orderByAsc(AigcModelDO::getSort)
                .orderByDesc(AigcModelDO::getId));
    }

    default List<AigcModelDO> selectListByType(Integer type) {
        if (type == null) {
            return selectList();
        }
        return selectList(AigcModelDO::getType, type);
    }

}
