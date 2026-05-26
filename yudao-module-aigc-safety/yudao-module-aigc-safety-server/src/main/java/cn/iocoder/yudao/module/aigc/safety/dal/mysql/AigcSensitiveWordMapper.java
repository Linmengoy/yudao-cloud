package cn.iocoder.yudao.module.aigc.safety.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcSensitiveWordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcSensitiveWordMapper extends BaseMapperX<AigcSensitiveWordDO> {

    default PageResult<AigcSensitiveWordDO> selectPage(AigcSensitiveWordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcSensitiveWordDO>()
                .likeIfPresent(AigcSensitiveWordDO::getWord, reqVO.getWord())
                .eqIfPresent(AigcSensitiveWordDO::getScene, reqVO.getScene())
                .eqIfPresent(AigcSensitiveWordDO::getLevel, reqVO.getLevel())
                .eqIfPresent(AigcSensitiveWordDO::getMatchType, reqVO.getMatchType())
                .eqIfPresent(AigcSensitiveWordDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AigcSensitiveWordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AigcSensitiveWordDO::getId));
    }

    default AigcSensitiveWordDO selectByWordAndScene(String word, String scene) {
        return selectOne(AigcSensitiveWordDO::getWord, word, AigcSensitiveWordDO::getScene, scene);
    }

    default List<AigcSensitiveWordDO> selectListBySceneAndStatus(String scene, String status) {
        return selectList(new LambdaQueryWrapperX<AigcSensitiveWordDO>()
                .eq(AigcSensitiveWordDO::getScene, scene)
                .eq(AigcSensitiveWordDO::getStatus, status));
    }

}
