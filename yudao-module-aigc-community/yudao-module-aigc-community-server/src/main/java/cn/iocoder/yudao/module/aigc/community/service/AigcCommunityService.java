package cn.iocoder.yudao.module.aigc.community.service;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAdminCommentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAdminPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityAuthorPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityCommentCreateReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityCommentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityPostPublishReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityShareReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityAuthorRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityCommentRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityPostRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityShareRespVO;

public interface AigcCommunityService {

    Long publishPost(AigcCommunityPostPublishReqVO reqVO, Long userId);

    PageResult<AigcCommunityPostRespVO> getPublicPostPage(AigcCommunityPostPageReqVO reqVO, Long userId);

    AigcCommunityPostRespVO getPublicPost(Long id, Long userId);

    void likePost(Long postId, Long userId);

    void unlikePost(Long postId, Long userId);

    PageResult<AigcCommunityCommentRespVO> getCommentPage(AigcCommunityCommentPageReqVO reqVO, Long userId);

    Long createComment(AigcCommunityCommentCreateReqVO reqVO, Long userId);

    void deleteMyComment(Long id, Long userId);

    AigcCommunityShareRespVO sharePost(AigcCommunityShareReqVO reqVO, Long userId, String clientIp, String userAgent);

    void followAuthor(Long authorUserId, Long userId);

    void unfollowAuthor(Long authorUserId, Long userId);

    Boolean getFollowStatus(Long authorUserId, Long userId);

    PageResult<AigcCommunityAuthorRespVO> getFollowingPage(PageParam pageParam, Long userId);

    PageResult<AigcCommunityAuthorRespVO> getFollowerPage(PageParam pageParam, Long authorUserId, Long userId);

    AigcCommunityAuthorRespVO getAuthor(Long authorUserId, Long userId);

    PageResult<AigcCommunityPostRespVO> getAuthorPostPage(AigcCommunityAuthorPostPageReqVO reqVO, Long userId);

    PageResult<AigcCommunityPostRespVO> getAdminPostPage(AigcCommunityAdminPostPageReqVO reqVO);

    AigcCommunityPostRespVO getAdminPost(Long id);

    void auditPassPost(Long id, Long operatorUserId);

    void auditRejectPost(Long id, String reason, Long operatorUserId);

    void offlinePost(Long id, String reason, Long operatorUserId);

    void restorePost(Long id, Long operatorUserId);

    PageResult<AigcCommunityCommentRespVO> getAdminCommentPage(AigcCommunityAdminCommentPageReqVO reqVO);

    void hideComment(Long id, String reason, Long operatorUserId);

    void deleteComment(Long id, String reason, Long operatorUserId);

}
