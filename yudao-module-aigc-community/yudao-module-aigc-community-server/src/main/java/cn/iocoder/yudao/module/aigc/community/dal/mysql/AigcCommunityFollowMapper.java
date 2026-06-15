package cn.iocoder.yudao.module.aigc.community.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityFollowDO;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCommunityFollowMapper extends BaseMapperX<AigcCommunityFollowDO> {

    default AigcCommunityFollowDO selectByPair(Long followerUserId, Long followeeUserId) {
        return selectOne(AigcCommunityFollowDO::getFollowerUserId, followerUserId,
                AigcCommunityFollowDO::getFolloweeUserId, followeeUserId);
    }

    default boolean isFollowing(Long followerUserId, Long followeeUserId) {
        if (followerUserId == null || followeeUserId == null) {
            return false;
        }
        return selectCount(new LambdaQueryWrapperX<AigcCommunityFollowDO>()
                .eq(AigcCommunityFollowDO::getFollowerUserId, followerUserId)
                .eq(AigcCommunityFollowDO::getFolloweeUserId, followeeUserId)
                .eq(AigcCommunityFollowDO::getStatus, AigcCommunityStatusEnum.FOLLOWING.getCode())) > 0;
    }

    default int updateStatus(Long id, String status) {
        LambdaUpdateWrapper<AigcCommunityFollowDO> wrapper = new LambdaUpdateWrapper<AigcCommunityFollowDO>()
                .eq(AigcCommunityFollowDO::getId, id)
                .set(AigcCommunityFollowDO::getStatus, status);
        if (AigcCommunityStatusEnum.FOLLOWING.getCode().equals(status)) {
            wrapper.setSql("follow_time = NOW(), cancel_time = NULL");
        } else {
            wrapper.setSql("cancel_time = NOW()");
        }
        return update(null, wrapper);
    }

    default PageResult<AigcCommunityFollowDO> selectFollowingPage(PageParam pageParam, Long followerUserId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<AigcCommunityFollowDO>()
                .eq(AigcCommunityFollowDO::getFollowerUserId, followerUserId)
                .eq(AigcCommunityFollowDO::getStatus, AigcCommunityStatusEnum.FOLLOWING.getCode())
                .orderByDesc(AigcCommunityFollowDO::getFollowTime));
    }

    default PageResult<AigcCommunityFollowDO> selectFollowerPage(PageParam pageParam, Long followeeUserId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<AigcCommunityFollowDO>()
                .eq(AigcCommunityFollowDO::getFolloweeUserId, followeeUserId)
                .eq(AigcCommunityFollowDO::getStatus, AigcCommunityStatusEnum.FOLLOWING.getCode())
                .orderByDesc(AigcCommunityFollowDO::getFollowTime));
    }

}
