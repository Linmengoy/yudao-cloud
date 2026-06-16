import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class CommunitySocialModerationContractsTest(unittest.TestCase):

    def test_issue_125_like_comment_and_share_paths_are_idempotent_and_counted(self):
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/AigcCommunityServiceImpl.java"
        )
        mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityPostMapper.java"
        )
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/app/AigcCommunityAppController.java"
        )

        self.assertIn("@PutMapping(\"/post/like\")", controller)
        self.assertIn("@DeleteMapping(\"/post/like\")", controller)
        self.assertIn("@PostMapping(\"/comment/create\")", controller)
        self.assertIn("@DeleteMapping(\"/comment/delete\")", controller)
        self.assertIn("@PostMapping(\"/post/share\")", controller)

        self.assertIn("if (like != null && AigcCommunityStatusEnum.NORMAL.getCode().equals(like.getStatus()))", service)
        self.assertIn("return;", service)
        self.assertIn("likeMapper.updateStatus(like.getId(), AigcCommunityStatusEnum.NORMAL.getCode())", service)
        self.assertIn("likeMapper.updateStatus(like.getId(), AigcCommunityStatusEnum.CANCELLED.getCode())", service)
        self.assertIn("authorStatsMapper.increaseLikeReceivedCount(post.getAuthorUserId())", service)
        self.assertIn("authorStatsMapper.decreaseLikeReceivedCount(post.getAuthorUserId())", service)

        self.assertIn("String content = StrUtil.trim(reqVO.getContent())", service)
        self.assertIn("throw exception(COMMUNITY_COMMENT_CONTENT_EMPTY)", service)
        self.assertIn("requiresManualReview(content)", service)
        self.assertIn("AigcCommunityAuditStatusEnum.MANUAL_REVIEW.getCode()", service)
        self.assertIn("if (!requiresManualReview) {\n            postMapper.increaseCommentCount", service)
        self.assertIn("if (!Objects.equals(comment.getUserId(), userId))", service)
        self.assertIn("throw exception(COMMUNITY_COMMENT_NO_PERMISSION)", service)
        self.assertIn("if (isVisibleComment(comment)) {\n            postMapper.decreaseCommentCount", service)

        self.assertIn("IdUtil.fastSimpleUUID()", service)
        self.assertIn(".setShareToken(token)", service)
        self.assertIn(".setClientIp(clientIp)", service)
        self.assertIn("StrUtil.maxLength(userAgent, 512)", service)
        self.assertIn("postMapper.increaseShareCount(reqVO.getPostId())", service)
        self.assertIn('"/community/" + post.getPostNo() + "?share=" + token', service)

        for sql in [
            "like_count = like_count + 1, hot_score = hot_score + 3",
            "like_count = GREATEST(like_count - 1, 0), hot_score = GREATEST(hot_score - 3, 0)",
            "comment_count = comment_count + 1, hot_score = hot_score + 5",
            "comment_count = GREATEST(comment_count - 1, 0), hot_score = GREATEST(hot_score - 5, 0)",
            "share_count = share_count + 1, hot_score = hot_score + 2",
        ]:
            self.assertIn(sql, mapper)

    def test_issue_126_follow_relations_are_unique_reactivatable_and_exposed(self):
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/AigcCommunityServiceImpl.java"
        )
        mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityFollowMapper.java"
        )
        stats_mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityAuthorStatsMapper.java"
        )
        schema = read("yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db.sql")
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/app/AigcCommunityAppController.java"
        )

        self.assertIn("@PutMapping(\"/author/follow\")", controller)
        self.assertIn("@DeleteMapping(\"/author/follow\")", controller)
        self.assertIn("@GetMapping(\"/author/follow-status\")", controller)
        self.assertIn("@GetMapping(\"/author/following-page\")", controller)
        self.assertIn("@GetMapping(\"/author/follower-page\")", controller)

        self.assertIn("throw exception(COMMUNITY_FOLLOW_SELF)", service)
        self.assertIn("validateAuthorExists(authorUserId)", service)
        self.assertIn("followMapper.selectByPair(userId, authorUserId)", service)
        self.assertIn("AigcCommunityStatusEnum.FOLLOWING.getCode().equals(follow.getStatus())", service)
        self.assertIn("followMapper.updateStatus(follow.getId(), AigcCommunityStatusEnum.FOLLOWING.getCode())", service)
        self.assertIn("followMapper.updateStatus(follow.getId(), AigcCommunityStatusEnum.CANCELLED.getCode())", service)
        self.assertIn("authorStatsMapper.increaseFollowingCount(userId)", service)
        self.assertIn("authorStatsMapper.increaseFollowerCount(authorUserId)", service)
        self.assertIn("authorStatsMapper.decreaseFollowingCount(userId)", service)
        self.assertIn("authorStatsMapper.decreaseFollowerCount(authorUserId)", service)

        self.assertIn("UNIQUE KEY `uk_follow_pair`", schema)
        self.assertIn("selectListByFollowerAndFollowees", mapper)
        self.assertIn("cancel_time = NULL", mapper)
        self.assertIn("cancel_time = NOW()", mapper)
        self.assertIn("follower_count = GREATEST(follower_count - 1, 0)", stats_mapper)
        self.assertIn("following_count = GREATEST(following_count - 1, 0)", stats_mapper)

    def test_issue_127_author_profile_only_lists_public_posts_and_current_follow_state(self):
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/AigcCommunityServiceImpl.java"
        )
        post_mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityPostMapper.java"
        )
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/app/AigcCommunityAppController.java"
        )

        self.assertIn("@GetMapping(\"/author/get\")", controller)
        self.assertIn("@GetMapping(\"/author/post-page\")", controller)
        self.assertIn("communityService.getAuthor(authorUserId, getLoginUserId())", controller)
        self.assertIn("communityService.getAuthorPostPage(reqVO, getLoginUserId())", controller)

        self.assertIn("validateAuthorExists(authorUserId)", service)
        self.assertIn("ensureAuthorStats(authorUserId)", service)
        self.assertIn("buildAuthorRespList(List.of(authorUserId), userId)", service)
        self.assertIn("authorStatsMapper.selectListByUserIds(distinctAuthorIds)", service)
        self.assertIn(".setFollowerCount(stats == null ? 0 : stats.getFollowerCount())", service)
        self.assertIn(".setPublicPostCount(stats == null ? 0 : stats.getPublicPostCount())", service)
        self.assertIn(".setLikeReceivedCount(stats == null ? 0 : stats.getLikeReceivedCount())", service)
        self.assertIn(".setFollowedByCurrentUser(followedAuthorIds.contains(authorId))", service)
        self.assertIn("postMapper.selectAuthorPublicPage(reqVO)", service)

        author_page = re.search(
            r"default PageResult<AigcCommunityPostDO> selectAuthorPublicPage.*?\{(?P<body>.*?)\n    \}",
            post_mapper,
            re.S,
        )
        self.assertIsNotNone(author_page)
        author_page_body = author_page.group("body")
        self.assertIn("publicWrapper()", author_page_body)
        self.assertIn(".eq(AigcCommunityPostDO::getAuthorUserId, reqVO.getAuthorUserId())", author_page_body)
        self.assertIn("AigcCommunityPostStatusEnum.PUBLISHED.getCode()", post_mapper)
        self.assertIn("AigcCommunityAuditStatusEnum.PASS.getCode()", post_mapper)

    def test_issue_128_admin_moderation_controls_visibility_and_audit_logs(self):
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/AigcCommunityServiceImpl.java"
        )
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/admin/AigcCommunityAdminController.java"
        )
        post_mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityPostMapper.java"
        )
        comment_mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcCommunityCommentMapper.java"
        )

        for permission in [
            "aigc:community-post:query",
            "aigc:community-post:audit",
            "aigc:community-comment:query",
            "aigc:community-comment:audit",
        ]:
            self.assertIn(permission, controller)

        for route in [
            "/post/page",
            "/post/get",
            "/post/audit-pass",
            "/post/audit-reject",
            "/post/offline",
            "/post/restore",
            "/comment/page",
            "/comment/hide",
            "/comment/delete",
        ]:
            self.assertIn(route, controller)

        self.assertIn("post.setPublishStatus(AigcCommunityPostStatusEnum.PUBLISHED.getCode())", service)
        self.assertIn("post.setAuditStatus(AigcCommunityAuditStatusEnum.PASS.getCode())", service)
        self.assertIn("post.setPublishStatus(AigcCommunityPostStatusEnum.REJECTED.getCode())", service)
        self.assertIn("post.setAuditStatus(AigcCommunityAuditStatusEnum.REJECT.getCode())", service)
        self.assertIn("post.setPublishStatus(AigcCommunityPostStatusEnum.OFFLINE.getCode())", service)
        self.assertIn("post.setOfflineReason(reason)", service)
        self.assertIn("post.setOfflineTime(LocalDateTime.now())", service)
        self.assertIn("post.setOfflineReason(null)", service)
        self.assertIn("post.setOfflineTime(null)", service)
        self.assertIn("addAuditLog(\"POST\", id, \"PASS\"", service)
        self.assertIn("addAuditLog(\"POST\", id, \"REJECT\"", service)
        self.assertIn("addAuditLog(\"POST\", id, \"OFFLINE\"", service)
        self.assertIn("addAuditLog(\"POST\", id, \"RESTORE\"", service)

        self.assertIn("comment.setStatus(AigcCommunityStatusEnum.HIDDEN.getCode())", service)
        self.assertIn("comment.setStatus(AigcCommunityStatusEnum.DELETED.getCode())", service)
        self.assertIn("comment.setAuditReason(reason)", service)
        self.assertIn("addAuditLog(\"COMMENT\", id, \"HIDE_COMMENT\"", service)
        self.assertIn("addAuditLog(\"COMMENT\", id, \"DELETE_COMMENT\"", service)
        self.assertIn("if (visible) {\n            postMapper.decreaseCommentCount", service)
        self.assertIn("throw exception(COMMUNITY_POST_AUDIT_REASON_EMPTY)", service)

        self.assertIn("selectAdminPage", post_mapper)
        self.assertIn(".eqIfPresent(AigcCommunityPostDO::getAuditStatus, reqVO.getAuditStatus())", post_mapper)
        self.assertIn("selectAdminPage", comment_mapper)
        self.assertIn(".eqIfPresent(AigcCommunityCommentDO::getAuditStatus, reqVO.getAuditStatus())", comment_mapper)

    def test_frontend_community_cache_is_cleared_after_mutations_for_issues_125_to_128(self):
        api = read("yudao-ui/draw2video-client/src/features/community/community-api.ts")
        cache = read("yudao-ui/draw2video-client/src/lib/page-cache.ts")

        for cached_function in [
            "getCommunityPosts",
            "getCommunityPost",
            "getCommunityComments",
            "getCommunityAuthor",
            "getCommunityAuthorPosts",
        ]:
            match = re.search(rf"export function {cached_function}.*?\{{(?P<body>.*?)\n\}}", api, re.S)
            self.assertIsNotNone(match, cached_function)
            self.assertIn("cachedGet", match.group("body"))

        for mutation in [
            "likeCommunityPost",
            "unlikeCommunityPost",
            "shareCommunityPost",
            "createCommunityComment",
            "deleteCommunityComment",
            "followCommunityAuthor",
            "unfollowCommunityAuthor",
        ]:
            match = re.search(rf"export async function {mutation}.*?\{{(?P<body>.*?)\n\}}", api, re.S)
            self.assertIsNotNone(match, mutation)
            self.assertIn("clearCommunityCache()", match.group("body"))

        self.assertIn('const COMMUNITY_CACHE_PREFIX = "community:"', api)
        self.assertIn("readPageCache<T>(key, COMMUNITY_CACHE_MAX_AGE_MS)", api)
        self.assertIn("clearPageCache(COMMUNITY_CACHE_PREFIX)", api)
        self.assertIn("getCommunityPost(id: number | string)", api)
        self.assertIn("Array.from(pageCache.keys()).forEach", cache)
        self.assertIn("if (key.startsWith(prefix)) pageCache.delete(key)", cache)


if __name__ == "__main__":
    unittest.main()
