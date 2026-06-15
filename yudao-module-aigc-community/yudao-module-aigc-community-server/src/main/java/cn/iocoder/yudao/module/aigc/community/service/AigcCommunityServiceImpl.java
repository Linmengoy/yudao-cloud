package cn.iocoder.yudao.module.aigc.community.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.api.AigcWorkflowApi;
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
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityAuditLogDO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityAuthorStatsDO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityCommentDO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityFollowDO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityPostDO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityPostLikeDO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcCommunityShareLogDO;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityAuditLogMapper;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityAuthorStatsMapper;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityCommentMapper;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityFollowMapper;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityPostLikeMapper;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityPostMapper;
import cn.iocoder.yudao.module.aigc.community.dal.mysql.AigcCommunityShareLogMapper;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityPostStatusEnum;
import cn.iocoder.yudao.module.aigc.community.enums.AigcCommunityStatusEnum;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.community.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcCommunityServiceImpl implements AigcCommunityService {

    private static final List<String> COMMENT_REVIEW_KEYWORDS = List.of(
            "广告", "色情", "暴力", "赌博", "诈骗", "spam", "illegal", "violation");

    @Resource
    private AigcCommunityPostMapper postMapper;
    @Resource
    private AigcCommunityPostLikeMapper likeMapper;
    @Resource
    private AigcCommunityCommentMapper commentMapper;
    @Resource
    private AigcCommunityShareLogMapper shareLogMapper;
    @Resource
    private AigcCommunityFollowMapper followMapper;
    @Resource
    private AigcCommunityAuthorStatsMapper authorStatsMapper;
    @Resource
    private AigcCommunityAuditLogMapper auditLogMapper;
    @Resource
    private AigcAssetApi assetApi;
    @Resource
    private AigcWorkflowApi workflowApi;
    @Resource
    private MemberUserApi memberUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishPost(AigcCommunityPostPublishReqVO reqVO, Long userId) {
        if (reqVO.getAssetId() == null && reqVO.getProjectId() == null) {
            throw exception(COMMUNITY_POST_SOURCE_EMPTY);
        }
        AigcAssetRespDTO asset = reqVO.getAssetId() == null ? null : validatePublishableAsset(reqVO.getAssetId(), userId);
        if (reqVO.getCoverAssetId() != null && !Objects.equals(reqVO.getCoverAssetId(), reqVO.getAssetId())) {
            validatePublishableAsset(reqVO.getCoverAssetId(), userId);
        }
        if (reqVO.getProjectId() != null) {
            validateReadableProject(reqVO.getProjectId(), userId);
        }
        AigcCommunityPostDO post = new AigcCommunityPostDO()
                .setPostNo("P" + IdUtil.getSnowflakeNextIdStr())
                .setAuthorUserId(userId)
                .setAssetId(reqVO.getAssetId())
                .setAssetType(asset == null ? null : asset.getAssetType())
                .setProjectId(reqVO.getProjectId())
                .setCoverAssetId(reqVO.getCoverAssetId() == null ? reqVO.getAssetId() : reqVO.getCoverAssetId())
                .setTitle(reqVO.getTitle())
                .setSummary(reqVO.getSummary())
                .setTags(reqVO.getTags())
                .setPromptSnapshot(reqVO.getPromptSnapshot())
                .setMetadata(reqVO.getMetadata())
                .setVisibility("PUBLIC")
                .setPublishStatus(AigcCommunityPostStatusEnum.PENDING.getCode())
                .setAuditStatus(AigcCommunityAuditStatusEnum.PENDING.getCode())
                .setViewCount(0)
                .setLikeCount(0)
                .setCommentCount(0)
                .setShareCount(0)
                .setDownloadCount(0)
                .setHotScore(BigDecimal.ZERO);
        postMapper.insert(post);
        ensureAuthorStats(userId);
        return post.getId();
    }

    @Override
    public PageResult<AigcCommunityPostRespVO> getPublicPostPage(AigcCommunityPostPageReqVO reqVO, Long userId) {
        PageResult<AigcCommunityPostDO> pageResult = postMapper.selectPublicPage(reqVO);
        return buildPostPage(pageResult, userId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCommunityPostRespVO getPublicPost(String idOrPostNo, Long userId) {
        AigcCommunityPostDO post = validatePublicPost(idOrPostNo);
        postMapper.increaseViewCount(post.getId());
        return buildPostResp(post, userId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long postId, Long userId) {
        AigcCommunityPostDO post = validateInteractivePost(postId);
        AigcCommunityPostLikeDO like = likeMapper.selectByPostIdAndUserId(postId, userId);
        if (like != null && AigcCommunityStatusEnum.NORMAL.getCode().equals(like.getStatus())) {
            return;
        }
        if (like == null) {
            likeMapper.insert(new AigcCommunityPostLikeDO()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setStatus(AigcCommunityStatusEnum.NORMAL.getCode()));
        } else {
            likeMapper.updateStatus(like.getId(), AigcCommunityStatusEnum.NORMAL.getCode());
        }
        postMapper.increaseLikeCount(postId);
        ensureAuthorStats(post.getAuthorUserId());
        authorStatsMapper.increaseLikeReceivedCount(post.getAuthorUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long postId, Long userId) {
        AigcCommunityPostDO post = validateInteractivePost(postId);
        AigcCommunityPostLikeDO like = likeMapper.selectByPostIdAndUserId(postId, userId);
        if (like == null || !AigcCommunityStatusEnum.NORMAL.getCode().equals(like.getStatus())) {
            return;
        }
        likeMapper.updateStatus(like.getId(), AigcCommunityStatusEnum.CANCELLED.getCode());
        postMapper.decreaseLikeCount(postId);
        ensureAuthorStats(post.getAuthorUserId());
        authorStatsMapper.decreaseLikeReceivedCount(post.getAuthorUserId());
    }

    @Override
    public PageResult<AigcCommunityCommentRespVO> getCommentPage(AigcCommunityCommentPageReqVO reqVO, Long userId) {
        validatePublicPost(reqVO.getPostId());
        PageResult<AigcCommunityCommentDO> pageResult = commentMapper.selectPublicPage(reqVO);
        return buildCommentPage(pageResult, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(AigcCommunityCommentCreateReqVO reqVO, Long userId) {
        validateInteractivePost(reqVO.getPostId());
        String content = StrUtil.trim(reqVO.getContent());
        if (StrUtil.isBlank(content)) {
            throw exception(COMMUNITY_COMMENT_CONTENT_EMPTY);
        }
        boolean requiresManualReview = requiresManualReview(content);
        AigcCommunityCommentDO comment = new AigcCommunityCommentDO()
                .setPostId(reqVO.getPostId())
                .setUserId(userId)
                .setParentId(0L)
                .setContent(content)
                .setAuditStatus(requiresManualReview ? AigcCommunityAuditStatusEnum.MANUAL_REVIEW.getCode() : AigcCommunityAuditStatusEnum.PASS.getCode())
                .setAuditReason(requiresManualReview ? "Comment matched basic moderation keywords" : null)
                .setStatus(AigcCommunityStatusEnum.NORMAL.getCode())
                .setLikeCount(0);
        commentMapper.insert(comment);
        if (!requiresManualReview) {
            postMapper.increaseCommentCount(reqVO.getPostId());
        }
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyComment(Long id, Long userId) {
        AigcCommunityCommentDO comment = validateComment(id);
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw exception(COMMUNITY_COMMENT_NO_PERMISSION);
        }
        if (AigcCommunityStatusEnum.DELETED.getCode().equals(comment.getStatus())) {
            return;
        }
        comment.setStatus(AigcCommunityStatusEnum.DELETED.getCode());
        commentMapper.updateById(comment);
        if (isVisibleComment(comment)) {
            postMapper.decreaseCommentCount(comment.getPostId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcCommunityShareRespVO sharePost(AigcCommunityShareReqVO reqVO, Long userId, String clientIp, String userAgent) {
        AigcCommunityPostDO post = validateInteractivePost(reqVO.getPostId());
        String token = IdUtil.fastSimpleUUID();
        shareLogMapper.insert(new AigcCommunityShareLogDO()
                .setPostId(reqVO.getPostId())
                .setUserId(userId)
                .setShareChannel(reqVO.getShareChannel())
                .setShareToken(token)
                .setClientIp(clientIp)
                .setUserAgent(StrUtil.maxLength(userAgent, 512)));
        postMapper.increaseShareCount(reqVO.getPostId());
        return new AigcCommunityShareRespVO()
                .setShareUrl("/community/" + post.getPostNo() + "?share=" + token)
                .setShareToken(token)
                .setShareCount((post.getShareCount() == null ? 0 : post.getShareCount()) + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followAuthor(Long authorUserId, Long userId) {
        if (Objects.equals(authorUserId, userId)) {
            throw exception(COMMUNITY_FOLLOW_SELF);
        }
        validateAuthorExists(authorUserId);
        AigcCommunityFollowDO follow = followMapper.selectByPair(userId, authorUserId);
        if (follow != null && AigcCommunityStatusEnum.FOLLOWING.getCode().equals(follow.getStatus())) {
            return;
        }
        ensureAuthorStats(userId);
        ensureAuthorStats(authorUserId);
        if (follow == null) {
            followMapper.insert(new AigcCommunityFollowDO()
                    .setFollowerUserId(userId)
                    .setFolloweeUserId(authorUserId)
                    .setStatus(AigcCommunityStatusEnum.FOLLOWING.getCode())
                    .setFollowTime(LocalDateTime.now()));
        } else {
            followMapper.updateStatus(follow.getId(), AigcCommunityStatusEnum.FOLLOWING.getCode());
        }
        authorStatsMapper.increaseFollowingCount(userId);
        authorStatsMapper.increaseFollowerCount(authorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowAuthor(Long authorUserId, Long userId) {
        AigcCommunityFollowDO follow = followMapper.selectByPair(userId, authorUserId);
        if (follow == null || !AigcCommunityStatusEnum.FOLLOWING.getCode().equals(follow.getStatus())) {
            return;
        }
        ensureAuthorStats(userId);
        ensureAuthorStats(authorUserId);
        followMapper.updateStatus(follow.getId(), AigcCommunityStatusEnum.CANCELLED.getCode());
        authorStatsMapper.decreaseFollowingCount(userId);
        authorStatsMapper.decreaseFollowerCount(authorUserId);
    }

    @Override
    public Boolean getFollowStatus(Long authorUserId, Long userId) {
        return followMapper.isFollowing(userId, authorUserId);
    }

    @Override
    public PageResult<AigcCommunityAuthorRespVO> getFollowingPage(PageParam pageParam, Long userId) {
        PageResult<AigcCommunityFollowDO> page = followMapper.selectFollowingPage(pageParam, userId);
        List<Long> authorIds = page.getList().stream().map(AigcCommunityFollowDO::getFolloweeUserId).toList();
        return new PageResult<>(buildAuthorRespList(authorIds, userId), page.getTotal());
    }

    @Override
    public PageResult<AigcCommunityAuthorRespVO> getFollowerPage(PageParam pageParam, Long authorUserId, Long userId) {
        PageResult<AigcCommunityFollowDO> page = followMapper.selectFollowerPage(pageParam, authorUserId);
        List<Long> authorIds = page.getList().stream().map(AigcCommunityFollowDO::getFollowerUserId).toList();
        return new PageResult<>(buildAuthorRespList(authorIds, userId), page.getTotal());
    }

    @Override
    public AigcCommunityAuthorRespVO getAuthor(Long authorUserId, Long userId) {
        validateAuthorExists(authorUserId);
        ensureAuthorStats(authorUserId);
        return CollUtil.getFirst(buildAuthorRespList(List.of(authorUserId), userId));
    }

    @Override
    public PageResult<AigcCommunityPostRespVO> getAuthorPostPage(AigcCommunityAuthorPostPageReqVO reqVO, Long userId) {
        validateAuthorExists(reqVO.getAuthorUserId());
        return buildPostPage(postMapper.selectAuthorPublicPage(reqVO), userId, true);
    }

    @Override
    public PageResult<AigcCommunityPostRespVO> getAdminPostPage(AigcCommunityAdminPostPageReqVO reqVO) {
        return buildPostPage(postMapper.selectAdminPage(reqVO), null, false);
    }

    @Override
    public AigcCommunityPostRespVO getAdminPost(Long id) {
        return buildPostResp(validatePost(id), null, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPassPost(Long id, Long operatorUserId) {
        AigcCommunityPostDO post = validatePost(id);
        if (post.getAssetId() != null) {
            validateAssetVisible(post.getAssetId());
        }
        LocalDateTime now = LocalDateTime.now();
        boolean firstPublish = !AigcCommunityPostStatusEnum.PUBLISHED.getCode().equals(post.getPublishStatus());
        post.setPublishStatus(AigcCommunityPostStatusEnum.PUBLISHED.getCode());
        post.setAuditStatus(AigcCommunityAuditStatusEnum.PASS.getCode());
        post.setAuditReason(null);
        post.setAuditorUserId(operatorUserId);
        post.setAuditTime(now);
        post.setPublishTime(post.getPublishTime() == null ? now : post.getPublishTime());
        postMapper.updateById(post);
        ensureAuthorStats(post.getAuthorUserId());
        if (firstPublish) {
            authorStatsMapper.increasePublicPostCount(post.getAuthorUserId(), now);
        }
        addAuditLog("POST", id, "PASS", null, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRejectPost(Long id, String reason, Long operatorUserId) {
        requireReason(reason);
        AigcCommunityPostDO post = validatePost(id);
        boolean wasPublished = AigcCommunityPostStatusEnum.PUBLISHED.getCode().equals(post.getPublishStatus());
        post.setPublishStatus(AigcCommunityPostStatusEnum.REJECTED.getCode());
        post.setAuditStatus(AigcCommunityAuditStatusEnum.REJECT.getCode());
        post.setAuditReason(reason);
        post.setAuditorUserId(operatorUserId);
        post.setAuditTime(LocalDateTime.now());
        postMapper.updateById(post);
        if (wasPublished) {
            ensureAuthorStats(post.getAuthorUserId());
            authorStatsMapper.decreasePublicPostCount(post.getAuthorUserId());
        }
        addAuditLog("POST", id, "REJECT", reason, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlinePost(Long id, String reason, Long operatorUserId) {
        requireReason(reason);
        AigcCommunityPostDO post = validatePost(id);
        boolean wasPublished = AigcCommunityPostStatusEnum.PUBLISHED.getCode().equals(post.getPublishStatus());
        post.setPublishStatus(AigcCommunityPostStatusEnum.OFFLINE.getCode());
        post.setOfflineReason(reason);
        post.setOfflineTime(LocalDateTime.now());
        postMapper.updateById(post);
        if (wasPublished) {
            ensureAuthorStats(post.getAuthorUserId());
            authorStatsMapper.decreasePublicPostCount(post.getAuthorUserId());
        }
        addAuditLog("POST", id, "OFFLINE", reason, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restorePost(Long id, Long operatorUserId) {
        AigcCommunityPostDO post = validatePost(id);
        if (post.getAssetId() != null) {
            validateAssetVisible(post.getAssetId());
        }
        boolean wasPublished = AigcCommunityPostStatusEnum.PUBLISHED.getCode().equals(post.getPublishStatus());
        post.setPublishStatus(AigcCommunityPostStatusEnum.PUBLISHED.getCode());
        post.setAuditStatus(AigcCommunityAuditStatusEnum.PASS.getCode());
        post.setOfflineReason(null);
        post.setOfflineTime(null);
        post.setPublishTime(post.getPublishTime() == null ? LocalDateTime.now() : post.getPublishTime());
        postMapper.updateById(post);
        if (!wasPublished) {
            ensureAuthorStats(post.getAuthorUserId());
            authorStatsMapper.increasePublicPostCount(post.getAuthorUserId(), LocalDateTime.now());
        }
        addAuditLog("POST", id, "RESTORE", null, operatorUserId);
    }

    @Override
    public PageResult<AigcCommunityCommentRespVO> getAdminCommentPage(AigcCommunityAdminCommentPageReqVO reqVO) {
        return buildCommentPage(commentMapper.selectAdminPage(reqVO), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hideComment(Long id, String reason, Long operatorUserId) {
        requireReason(reason);
        AigcCommunityCommentDO comment = validateComment(id);
        boolean visible = isVisibleComment(comment);
        comment.setStatus(AigcCommunityStatusEnum.HIDDEN.getCode());
        comment.setAuditReason(reason);
        commentMapper.updateById(comment);
        if (visible) {
            postMapper.decreaseCommentCount(comment.getPostId());
        }
        addAuditLog("COMMENT", id, "HIDE_COMMENT", reason, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id, String reason, Long operatorUserId) {
        requireReason(reason);
        AigcCommunityCommentDO comment = validateComment(id);
        boolean visible = isVisibleComment(comment);
        comment.setStatus(AigcCommunityStatusEnum.DELETED.getCode());
        comment.setAuditReason(reason);
        commentMapper.updateById(comment);
        if (visible) {
            postMapper.decreaseCommentCount(comment.getPostId());
        }
        addAuditLog("COMMENT", id, "DELETE_COMMENT", reason, operatorUserId);
    }

    private PageResult<AigcCommunityPostRespVO> buildPostPage(PageResult<AigcCommunityPostDO> pageResult, Long userId, boolean onlyAvailableAssets) {
        List<AigcCommunityPostRespVO> list = buildPostRespList(pageResult.getList(), userId, onlyAvailableAssets);
        return new PageResult<>(list, pageResult.getTotal());
    }

    private AigcCommunityPostRespVO buildPostResp(AigcCommunityPostDO post, Long userId, boolean checkAsset) {
        List<AigcCommunityPostRespVO> list = buildPostRespList(List.of(post), userId, checkAsset);
        if (list.isEmpty()) {
            throw exception(COMMUNITY_POST_NOT_VISIBLE);
        }
        return list.get(0);
    }

    private List<AigcCommunityPostRespVO> buildPostRespList(List<AigcCommunityPostDO> posts, Long userId, boolean onlyAvailableAssets) {
        if (CollUtil.isEmpty(posts)) {
            return Collections.emptyList();
        }
        Map<Long, AigcAssetRespDTO> assetMap = getAssetMap(posts);
        Map<Long, MemberUserRespDTO> userMap = getUserMap(posts.stream().map(AigcCommunityPostDO::getAuthorUserId).collect(Collectors.toSet()));
        Set<Long> likedPostIds = userId == null ? Collections.emptySet() : likeMapper.selectListByPostIdsAndUserId(
                posts.stream().map(AigcCommunityPostDO::getId).toList(), userId).stream()
                .filter(like -> AigcCommunityStatusEnum.NORMAL.getCode().equals(like.getStatus()))
                .map(AigcCommunityPostLikeDO::getPostId)
                .collect(Collectors.toSet());
        Set<Long> followedAuthorIds = userId == null ? Collections.emptySet() : followMapper.selectListByFollowerAndFollowees(userId,
                        posts.stream().map(AigcCommunityPostDO::getAuthorUserId).collect(Collectors.toSet())).stream()
                .map(AigcCommunityFollowDO::getFolloweeUserId)
                .collect(Collectors.toSet());
        return posts.stream()
                .filter(post -> !onlyAvailableAssets || post.getAssetId() == null || isAssetVisible(assetMap.get(post.getAssetId())))
                .map(post -> {
                    AigcAssetRespDTO asset = post.getAssetId() == null ? null : assetMap.get(post.getAssetId());
                    AigcAssetRespDTO coverAsset = post.getCoverAssetId() == null ? null : assetMap.get(post.getCoverAssetId());
                    MemberUserRespDTO author = userMap.get(post.getAuthorUserId());
                    return new AigcCommunityPostRespVO()
                            .setId(post.getId())
                            .setPostNo(post.getPostNo())
                            .setAuthorUserId(post.getAuthorUserId())
                            .setAuthorNickname(author == null ? null : author.getNickname())
                            .setAuthorAvatarUrl(author == null ? null : author.getAvatar())
                            .setAssetId(post.getAssetId())
                            .setAssetType(post.getAssetType())
                            .setProjectId(post.getProjectId())
                            .setCoverAssetId(post.getCoverAssetId())
                            .setCoverUrl(firstNotBlank(
                                    coverAsset == null ? null : coverAsset.getThumbnailUrl(),
                                    coverAsset == null ? null : coverAsset.getCoverUrl(),
                                    coverAsset == null ? null : coverAsset.getFileUrl(),
                                    asset == null ? null : asset.getThumbnailUrl(),
                                    asset == null ? null : asset.getCoverUrl(),
                                    asset == null ? null : asset.getFileUrl()))
                            .setFileUrl(asset == null ? null : asset.getFileUrl())
                            .setTitle(post.getTitle())
                            .setSummary(post.getSummary())
                            .setTags(post.getTags())
                            .setPromptSnapshot(post.getPromptSnapshot())
                            .setMetadata(post.getMetadata())
                            .setVisibility(post.getVisibility())
                            .setPublishStatus(post.getPublishStatus())
                            .setAuditStatus(post.getAuditStatus())
                            .setAuditReason(post.getAuditReason())
                            .setAuditorUserId(post.getAuditorUserId())
                            .setAuditTime(post.getAuditTime())
                            .setOfflineReason(post.getOfflineReason())
                            .setOfflineTime(post.getOfflineTime())
                            .setPublishTime(post.getPublishTime())
                            .setViewCount(post.getViewCount())
                            .setLikeCount(post.getLikeCount())
                            .setCommentCount(post.getCommentCount())
                            .setShareCount(post.getShareCount())
                            .setDownloadCount(post.getDownloadCount())
                            .setHotScore(post.getHotScore())
                            .setLikedByCurrentUser(likedPostIds.contains(post.getId()))
                            .setFollowedAuthor(followedAuthorIds.contains(post.getAuthorUserId()))
                            .setCreateTime(post.getCreateTime());
                })
                .toList();
    }

    private PageResult<AigcCommunityCommentRespVO> buildCommentPage(PageResult<AigcCommunityCommentDO> pageResult, Long userId) {
        Set<Long> userIds = pageResult.getList().stream().map(AigcCommunityCommentDO::getUserId).collect(Collectors.toSet());
        Map<Long, MemberUserRespDTO> userMap = getUserMap(userIds);
        List<AigcCommunityCommentRespVO> list = pageResult.getList().stream()
                .map(comment -> {
                    MemberUserRespDTO user = userMap.get(comment.getUserId());
                    return new AigcCommunityCommentRespVO()
                            .setId(comment.getId())
                            .setPostId(comment.getPostId())
                            .setUserId(comment.getUserId())
                            .setUserNickname(user == null ? null : user.getNickname())
                            .setUserAvatarUrl(user == null ? null : user.getAvatar())
                            .setParentId(comment.getParentId())
                            .setContent(comment.getContent())
                            .setAuditStatus(comment.getAuditStatus())
                            .setAuditReason(comment.getAuditReason())
                            .setStatus(comment.getStatus())
                            .setLikeCount(comment.getLikeCount())
                            .setMine(userId != null && Objects.equals(userId, comment.getUserId()))
                            .setCreateTime(comment.getCreateTime());
                })
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    private List<AigcCommunityAuthorRespVO> buildAuthorRespList(List<Long> authorIds, Long currentUserId) {
        if (CollUtil.isEmpty(authorIds)) {
            return Collections.emptyList();
        }
        Map<Long, MemberUserRespDTO> userMap = getUserMap(authorIds);
        List<Long> distinctAuthorIds = authorIds.stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, AigcCommunityAuthorStatsDO> statsMap = authorStatsMapper.selectListByUserIds(distinctAuthorIds).stream()
                .collect(Collectors.toMap(AigcCommunityAuthorStatsDO::getUserId, Function.identity(), (a, b) -> a));
        Set<Long> followedAuthorIds = currentUserId == null ? Collections.emptySet() : followMapper.selectListByFollowerAndFollowees(currentUserId, distinctAuthorIds).stream()
                .map(AigcCommunityFollowDO::getFolloweeUserId)
                .collect(Collectors.toSet());
        return authorIds.stream().distinct().map(authorId -> {
            AigcCommunityAuthorStatsDO stats = statsMap.get(authorId);
            if (stats == null) {
                ensureAuthorStats(authorId);
                stats = authorStatsMapper.selectByUserId(authorId);
            }
            MemberUserRespDTO user = userMap.get(authorId);
            return new AigcCommunityAuthorRespVO()
                    .setAuthorUserId(authorId)
                    .setNickname(user == null ? null : user.getNickname())
                    .setAvatarUrl(user == null ? null : user.getAvatar())
                    .setFollowerCount(stats == null ? 0 : stats.getFollowerCount())
                    .setFollowingCount(stats == null ? 0 : stats.getFollowingCount())
                    .setPublicPostCount(stats == null ? 0 : stats.getPublicPostCount())
                    .setLikeReceivedCount(stats == null ? 0 : stats.getLikeReceivedCount())
                    .setFollowedByCurrentUser(followedAuthorIds.contains(authorId));
        }).toList();
    }

    private Map<Long, AigcAssetRespDTO> getAssetMap(List<AigcCommunityPostDO> posts) {
        List<Long> assetIds = posts.stream()
                .flatMap(post -> Stream.of(post.getAssetId(), post.getCoverAssetId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(assetIds)) {
            return Collections.emptyMap();
        }
        CommonResult<List<AigcAssetRespDTO>> result = assetApi.getAssets(assetIds);
        List<AigcAssetRespDTO> assets = result.getCheckedData();
        return assets.stream().collect(Collectors.toMap(AigcAssetRespDTO::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, MemberUserRespDTO> getUserMap(Iterable<Long> ids) {
        List<Long> userIds = CollUtil.newArrayList(ids).stream().filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<MemberUserRespDTO> users = memberUserApi.getUserList(userIds).getCheckedData();
        return users.stream().collect(Collectors.toMap(MemberUserRespDTO::getId, Function.identity(), (a, b) -> a));
    }

    private AigcCommunityPostDO validatePost(Long id) {
        AigcCommunityPostDO post = postMapper.selectById(id);
        if (post == null) {
            throw exception(COMMUNITY_POST_NOT_EXISTS);
        }
        return post;
    }

    private AigcCommunityPostDO validatePublicPost(Long id) {
        AigcCommunityPostDO post = validatePost(id);
        if (!AigcCommunityPostStatusEnum.PUBLISHED.getCode().equals(post.getPublishStatus())
                || !AigcCommunityAuditStatusEnum.PASS.getCode().equals(post.getAuditStatus())) {
            throw exception(COMMUNITY_POST_NOT_VISIBLE);
        }
        if (post.getAssetId() != null) {
            validateAssetVisible(post.getAssetId());
        }
        return post;
    }

    private AigcCommunityPostDO validatePublicPost(String idOrPostNo) {
        String value = StrUtil.trim(idOrPostNo);
        if (StrUtil.isBlank(value)) {
            throw exception(COMMUNITY_POST_NOT_EXISTS);
        }
        AigcCommunityPostDO post;
        try {
            post = validatePost(Long.valueOf(value));
        } catch (NumberFormatException ignored) {
            post = postMapper.selectByPostNo(value);
            if (post == null) {
                throw exception(COMMUNITY_POST_NOT_EXISTS);
            }
        }
        if (!AigcCommunityPostStatusEnum.PUBLISHED.getCode().equals(post.getPublishStatus())
                || !AigcCommunityAuditStatusEnum.PASS.getCode().equals(post.getAuditStatus())) {
            throw exception(COMMUNITY_POST_NOT_VISIBLE);
        }
        if (post.getAssetId() != null) {
            validateAssetVisible(post.getAssetId());
        }
        return post;
    }

    private AigcCommunityPostDO validateInteractivePost(Long id) {
        AigcCommunityPostDO post = validatePublicPost(id);
        if (!"PUBLIC".equals(post.getVisibility())) {
            throw exception(COMMUNITY_POST_INTERACTION_DISABLED);
        }
        return post;
    }

    private AigcCommunityCommentDO validateComment(Long id) {
        AigcCommunityCommentDO comment = commentMapper.selectById(id);
        if (comment == null) {
            throw exception(COMMUNITY_COMMENT_NOT_EXISTS);
        }
        return comment;
    }

    private AigcAssetRespDTO validateAssetVisible(Long assetId) {
        AigcAssetRespDTO asset = assetApi.getAsset(assetId).getCheckedData();
        if (!isAssetVisible(asset)) {
            throw exception(COMMUNITY_POST_SOURCE_INVALID);
        }
        return asset;
    }

    private AigcAssetRespDTO validatePublishableAsset(Long assetId, Long userId) {
        AigcAssetRespDTO asset = validateAssetVisible(assetId);
        if (!Objects.equals(asset.getUserId(), userId)) {
            throw exception(COMMUNITY_POST_SOURCE_NO_PERMISSION);
        }
        return asset;
    }

    private void validateReadableProject(Long projectId, Long userId) {
        workflowApi.validateReadableCanvasProject(projectId, userId).getCheckedData();
    }

    private boolean isAssetVisible(AigcAssetRespDTO asset) {
        return asset != null
                && AigcAssetStatusEnum.NORMAL.getCode().equals(asset.getStatus())
                && AigcAssetAuditStatusEnum.PASS.getCode().equals(asset.getAuditStatus());
    }

    private boolean isVisibleComment(AigcCommunityCommentDO comment) {
        return AigcCommunityStatusEnum.NORMAL.getCode().equals(comment.getStatus())
                && AigcCommunityAuditStatusEnum.PASS.getCode().equals(comment.getAuditStatus());
    }

    private boolean requiresManualReview(String content) {
        String normalizedContent = StrUtil.trim(content).toLowerCase();
        return COMMENT_REVIEW_KEYWORDS.stream().anyMatch(normalizedContent::contains);
    }

    private void validateAuthorExists(Long authorUserId) {
        MemberUserRespDTO user = memberUserApi.getUser(authorUserId).getCheckedData();
        if (user == null) {
            throw exception(COMMUNITY_AUTHOR_NOT_EXISTS);
        }
    }

    private void ensureAuthorStats(Long userId) {
        if (userId == null || authorStatsMapper.selectByUserId(userId) != null) {
            return;
        }
        authorStatsMapper.insert(new AigcCommunityAuthorStatsDO()
                .setUserId(userId)
                .setFollowerCount(0)
                .setFollowingCount(0)
                .setPublicPostCount(0)
                .setLikeReceivedCount(0));
    }

    private void requireReason(String reason) {
        if (StrUtil.isBlank(reason)) {
            throw exception(COMMUNITY_POST_AUDIT_REASON_EMPTY);
        }
    }

    private void addAuditLog(String objectType, Long objectId, String action, String reason, Long operatorUserId) {
        auditLogMapper.insert(new AigcCommunityAuditLogDO()
                .setObjectType(objectType)
                .setObjectId(objectId)
                .setAction(action)
                .setReason(reason)
                .setOperatorUserId(operatorUserId));
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

}
