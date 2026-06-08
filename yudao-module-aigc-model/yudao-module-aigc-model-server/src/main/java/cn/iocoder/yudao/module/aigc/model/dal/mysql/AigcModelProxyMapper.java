package cn.iocoder.yudao.module.aigc.model.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxyPageReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProxyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcModelProxyMapper extends BaseMapperX<AigcModelProxyDO> {

    default AigcModelProxyDO selectByName(String name) {
        return selectOne(AigcModelProxyDO::getName, name);
    }

    default PageResult<AigcModelProxyDO> selectPage(AigcModelProxyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcModelProxyDO>()
                .likeIfPresent(AigcModelProxyDO::getName, reqVO.getName())
                .eqIfPresent(AigcModelProxyDO::getProtocol, reqVO.getProtocol())
                .eqIfPresent(AigcModelProxyDO::getStatus, reqVO.getStatus())
                .orderByDesc(AigcModelProxyDO::getId));
    }

    default List<AigcModelProxyDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<AigcModelProxyDO>()
                .eqIfPresent(AigcModelProxyDO::getStatus, status)
                .orderByDesc(AigcModelProxyDO::getId));
    }

}
