package cn.iocoder.yudao.module.aigc.community.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcGuideContentDO;
import cn.iocoder.yudao.module.aigc.community.enums.AigcGuidePublishStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcGuideContentMapper extends BaseMapperX<AigcGuideContentDO> {

    default PageResult<AigcGuideContentDO> selectPage(AigcGuideContentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcGuideContentDO>()
                .likeIfPresent(AigcGuideContentDO::getTitle, reqVO.getTitle())
                .eqIfPresent(AigcGuideContentDO::getCategory, reqVO.getCategory())
                .eqIfPresent(AigcGuideContentDO::getPublishStatus, reqVO.getPublishStatus())
                .betweenIfPresent(AigcGuideContentDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(AigcGuideContentDO::getSort)
                .orderByDesc(AigcGuideContentDO::getId));
    }

    default AigcGuideContentDO selectBySlug(String slug) {
        return selectOne(AigcGuideContentDO::getSlug, slug);
    }

    default List<AigcGuideContentDO> selectPublishedList() {
        return selectList(new LambdaQueryWrapperX<AigcGuideContentDO>()
                .eq(AigcGuideContentDO::getPublishStatus, AigcGuidePublishStatusEnum.PUBLISHED.getCode())
                .orderByAsc(AigcGuideContentDO::getSort)
                .orderByAsc(AigcGuideContentDO::getId));
    }

    default AigcGuideContentDO selectPublishedBySlug(String slug) {
        return selectOne(new LambdaQueryWrapperX<AigcGuideContentDO>()
                .eq(AigcGuideContentDO::getSlug, slug)
                .eq(AigcGuideContentDO::getPublishStatus, AigcGuidePublishStatusEnum.PUBLISHED.getCode()));
    }

}
