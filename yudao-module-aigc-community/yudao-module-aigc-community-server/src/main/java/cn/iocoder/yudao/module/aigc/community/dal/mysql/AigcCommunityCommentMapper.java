package cn.iocoder.yudao.module.aigc.community.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAdminCommentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityCommentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityCommentDO;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityStatusEnum;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcCommunityCommentMapper extends BaseMapperX<AigcCommunityCommentDO> {

    default PageResult<AigcCommunityCommentDO> selectPublicPage(AigcCommunityCommentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCommunityCommentDO>()
                .eq(AigcCommunityCommentDO::getPostId, reqVO.getPostId())
                .eq(AigcCommunityCommentDO::getStatus, AigcCommunityStatusEnum.NORMAL.getCode())
                .eq(AigcCommunityCommentDO::getAuditStatus, AigcCommunityAuditStatusEnum.PASS.getCode())
                .orderByDesc(AigcCommunityCommentDO::getId));
    }

    default PageResult<AigcCommunityCommentDO> selectAdminPage(AigcCommunityAdminCommentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcCommunityCommentDO>()
                .eqIfPresent(AigcCommunityCommentDO::getPostId, reqVO.getPostId())
                .eqIfPresent(AigcCommunityCommentDO::getUserId, reqVO.getUserId())
                .eqIfPresent(AigcCommunityCommentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AigcCommunityCommentDO::getAuditStatus, reqVO.getAuditStatus())
                .orderByDesc(AigcCommunityCommentDO::getId));
    }

}
