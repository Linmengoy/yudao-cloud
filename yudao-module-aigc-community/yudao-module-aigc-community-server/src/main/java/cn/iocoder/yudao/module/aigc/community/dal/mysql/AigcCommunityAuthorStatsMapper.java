package cn.iocoder.yudao.module.aigc.community.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityAuthorStatsDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface AigcCommunityAuthorStatsMapper extends BaseMapperX<AigcCommunityAuthorStatsDO> {

    default AigcCommunityAuthorStatsDO selectByUserId(Long userId) {
        return selectOne(AigcCommunityAuthorStatsDO::getUserId, userId);
    }

    default int increaseFollowerCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("follower_count = follower_count + 1"));
    }

    default int decreaseFollowerCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("follower_count = GREATEST(follower_count - 1, 0)"));
    }

    default int increaseFollowingCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("following_count = following_count + 1"));
    }

    default int decreaseFollowingCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("following_count = GREATEST(following_count - 1, 0)"));
    }

    default int increasePublicPostCount(Long userId, LocalDateTime publishTime) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .set(AigcCommunityAuthorStatsDO::getLastPublishTime, publishTime)
                .setSql("public_post_count = public_post_count + 1"));
    }

    default int decreasePublicPostCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("public_post_count = GREATEST(public_post_count - 1, 0)"));
    }

    default int increaseLikeReceivedCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("like_received_count = like_received_count + 1"));
    }

    default int decreaseLikeReceivedCount(Long userId) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityAuthorStatsDO>()
                .eq(AigcCommunityAuthorStatsDO::getUserId, userId)
                .setSql("like_received_count = GREATEST(like_received_count - 1, 0)"));
    }

}
