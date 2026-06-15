package cn.iocoder.yudao.module.aigc.community.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAdminPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityAuthorPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityPostDO;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityPostStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.util.StringUtils;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCommunityPostMapper extends BaseMapperX<AigcCommunityPostDO> {

    default PageResult<AigcCommunityPostDO> selectPublicPage(AigcCommunityPostPageReqVO reqVO) {
        LambdaQueryWrapperX<AigcCommunityPostDO> wrapper = publicWrapper()
                .eqIfPresent(AigcCommunityPostDO::getAssetType, reqVO.getAssetType())
                .likeIfPresent(AigcCommunityPostDO::getTitle, reqVO.getKeyword());
        if (StringUtils.hasText(reqVO.getTag())) {
            wrapper.like(AigcCommunityPostDO::getTags, reqVO.getTag());
        }
        orderFeed(wrapper, reqVO.getSort());
        return selectPage(reqVO, wrapper);
    }

    default PageResult<AigcCommunityPostDO> selectAuthorPublicPage(AigcCommunityAuthorPostPageReqVO reqVO) {
        LambdaQueryWrapperX<AigcCommunityPostDO> wrapper = publicWrapper()
                .eq(AigcCommunityPostDO::getAuthorUserId, reqVO.getAuthorUserId());
        orderFeed(wrapper, reqVO.getSort());
        return selectPage(reqVO, wrapper);
    }

    default PageResult<AigcCommunityPostDO> selectAdminPage(AigcCommunityAdminPostPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCommunityPostDO>()
                .eqIfPresent(AigcCommunityPostDO::getAuthorUserId, reqVO.getAuthorUserId())
                .eqIfPresent(AigcCommunityPostDO::getAssetType, reqVO.getAssetType())
                .eqIfPresent(AigcCommunityPostDO::getPublishStatus, reqVO.getPublishStatus())
                .eqIfPresent(AigcCommunityPostDO::getAuditStatus, reqVO.getAuditStatus())
                .likeIfPresent(AigcCommunityPostDO::getTitle, reqVO.getTitle())
                .betweenIfPresent(AigcCommunityPostDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AigcCommunityPostDO::getId));
    }

    default LambdaQueryWrapperX<AigcCommunityPostDO> publicWrapper() {
        return new LambdaQueryWrapperX<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getPublishStatus, AigcCommunityPostStatusEnum.PUBLISHED.getCode())
                .eq(AigcCommunityPostDO::getAuditStatus, AigcCommunityAuditStatusEnum.PASS.getCode());
    }

    default void orderFeed(LambdaQueryWrapperX<AigcCommunityPostDO> wrapper, String sort) {
        if ("hot".equals(sort)) {
            wrapper.orderByDesc(AigcCommunityPostDO::getHotScore).orderByDesc(AigcCommunityPostDO::getId);
            return;
        }
        wrapper.orderByDesc(AigcCommunityPostDO::getPublishTime).orderByDesc(AigcCommunityPostDO::getId);
    }

    default int increaseViewCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getId, id)
                .setSql("view_count = view_count + 1, hot_score = hot_score + 0.2"));
    }

    default int increaseLikeCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getId, id)
                .setSql("like_count = like_count + 1, hot_score = hot_score + 3"));
    }

    default int decreaseLikeCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getId, id)
                .setSql("like_count = GREATEST(like_count - 1, 0), hot_score = GREATEST(hot_score - 3, 0)"));
    }

    default int increaseCommentCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getId, id)
                .setSql("comment_count = comment_count + 1, hot_score = hot_score + 5"));
    }

    default int decreaseCommentCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getId, id)
                .setSql("comment_count = GREATEST(comment_count - 1, 0), hot_score = GREATEST(hot_score - 5, 0)"));
    }

    default int increaseShareCount(Long id) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostDO>()
                .eq(AigcCommunityPostDO::getId, id)
                .setSql("share_count = share_count + 1, hot_score = hot_score + 2"));
    }

}
