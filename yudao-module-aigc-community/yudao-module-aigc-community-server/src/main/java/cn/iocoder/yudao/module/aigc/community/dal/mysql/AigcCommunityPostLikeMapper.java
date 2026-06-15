package cn.iocoder.yudao.module.aigc.community.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityPostLikeDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AigcCommunityPostLikeMapper extends BaseMapperX<AigcCommunityPostLikeDO> {

    default AigcCommunityPostLikeDO selectByPostIdAndUserId(Long postId, Long userId) {
        return selectOne(AigcCommunityPostLikeDO::getPostId, postId, AigcCommunityPostLikeDO::getUserId, userId);
    }

    default List<AigcCommunityPostLikeDO> selectListByPostIdsAndUserId(Collection<Long> postIds, Long userId) {
        return selectList(new LambdaQueryWrapperX<AigcCommunityPostLikeDO>()
                .eq(AigcCommunityPostLikeDO::getUserId, userId)
                .in(AigcCommunityPostLikeDO::getPostId, postIds));
    }

    default int updateStatus(Long id, String status) {
        return update(null, new LambdaUpdateWrapper<AigcCommunityPostLikeDO>()
                .eq(AigcCommunityPostLikeDO::getId, id)
                .set(AigcCommunityPostLikeDO::getStatus, status));
    }

}
