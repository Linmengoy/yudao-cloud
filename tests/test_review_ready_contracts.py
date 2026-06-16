import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class ReviewReadyContractTest(unittest.TestCase):

    def test_community_publish_requires_owned_asset_or_readable_project(self):
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/AigcCommunityServiceImpl.java"
        )
        workflow_api = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-api/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/api/AigcWorkflowApi.java"
        )

        self.assertIn("COMMUNITY_POST_SOURCE_EMPTY", service)
        self.assertIn("validatePublishableAsset(reqVO.getAssetId(), userId)", service)
        self.assertIn("validatePublishableAsset(reqVO.getCoverAssetId(), userId)", service)
        self.assertIn("validateReadableProject(reqVO.getProjectId(), userId)", service)
        self.assertIn("workflowApi.validateReadableCanvasProject(projectId, userId).getCheckedData()", service)
        self.assertIn("validateReadableCanvasProject", workflow_api)

        publish_match = re.search(
            r"public Long publishPost\(AigcCommunityPostPublishReqVO reqVO, Long userId\) \{(?P<body>.*?)\n    \}",
            service,
            re.S,
        )
        self.assertIsNotNone(publish_match)
        publish_body = publish_match.group("body")
        self.assertIn(".setPublishStatus(AigcCommunityPostStatusEnum.PENDING.getCode())", publish_body)
        self.assertIn(".setAuditStatus(AigcCommunityAuditStatusEnum.PENDING.getCode())", publish_body)
        self.assertIn(".setCoverAssetId(reqVO.getCoverAssetId() == null ? reqVO.getAssetId() : reqVO.getCoverAssetId())", publish_body)

    def test_community_publish_never_exposes_private_storage_urls_in_schema(self):
        sql = read("yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/resources/schema/community_db.sql")
        post_table = re.search(
            r"CREATE TABLE IF NOT EXISTS `aigc_community_post` \((?P<body>.*?)\n\) ENGINE=",
            sql,
            re.S,
        )

        self.assertIsNotNone(post_table)
        post_body = post_table.group("body")
        self.assertIn("`asset_id` bigint DEFAULT NULL", post_body)
        self.assertIn("`project_id` bigint DEFAULT NULL", post_body)
        self.assertIn("`cover_asset_id` bigint DEFAULT NULL", post_body)
        self.assertNotRegex(post_body, r"`(?:file|source|origin|private)_url`")
        self.assertNotRegex(post_body, r"`(?:oss|s3)_[^`]*url`")

    def test_guide_admin_content_management_has_permissions_and_public_snapshot(self):
        controller = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/controller/guide/AigcGuideContentController.java"
        )
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/guide/AigcGuideContentServiceImpl.java"
        )
        mapper = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/dal/mysql/AigcGuideContentMapper.java"
        )

        for permission in [
            "aigc:guide:create",
            "aigc:guide:update",
            "aigc:guide:delete",
            "aigc:guide:query",
            "aigc:guide:publish",
        ]:
            self.assertIn(permission, controller)

        for route in [
            '/aigc/guide/content/create',
            '/aigc/guide/content/update',
            '/aigc/guide/content/delete',
            '/aigc/guide/content/page',
            '/aigc/guide/content/publish',
            '/aigc/guide/content/unpublish',
            '/aigc/guide/content/list',
            '/aigc/guide/content/public-get',
        ]:
            self.assertIn(route, controller)

        self.assertIn("validateSlugUnique(null, reqVO.getSlug())", service)
        self.assertIn(".setPublishStatus(AigcGuidePublishStatusEnum.DRAFT.getCode())", service)
        self.assertIn(".setPublishStatus(AigcGuidePublishStatusEnum.PUBLISHED.getCode())", service)
        self.assertIn(".setPublisherUserId(publisherUserId)", service)
        self.assertIn("selectPublishedList", mapper)
        self.assertIn("selectPublishedBySlug", mapper)
        self.assertIn(".eq(AigcGuideContentDO::getPublishStatus, AigcGuidePublishStatusEnum.PUBLISHED.getCode())", mapper)

    def test_frontend_guide_release_scripts_are_sha_tagged_and_rollbackable(self):
        ps1 = read("script/deploy-frontend-images.ps1")
        sh = read("script/deploy-frontend-images.sh")
        compose = read("script/docker/docker-compose.frontend.yml")
        runbook = read("script/docker/frontend-guide-release-runbook.md")

        self.assertIn('[ValidateSet("all", "admin", "client", "guide")]', ps1)
        self.assertIn('"draw2video-guide:$ImageTag"', ps1)
        self.assertIn('$GuideDir = Join-Path $RootDir "yudao-ui\\draw2video-guide"', ps1)
        self.assertIn("git -C $RootDir rev-parse --short=12 HEAD", ps1)

        self.assertIn("--target all|admin|client|guide", sh)
        self.assertIn('all|admin|client|guide)', sh)
        self.assertIn('"draw2video-guide:$IMAGE_TAG"', sh)
        self.assertIn('git -C "$ROOT_DIR" rev-parse --short=12 HEAD', sh)

        self.assertIn("draw2video-guide:", compose)
        self.assertIn("image: draw2video-guide:${FRONTEND_IMAGE_TAG:-latest}", compose)
        self.assertIn('"8082:80"', compose)
        self.assertIn("healthcheck:", compose)
        self.assertIn("http://127.0.0.1/health", compose)

        self.assertIn("--target guide", runbook)
        self.assertIn("git rev-parse --short=12 HEAD", runbook)
        self.assertIn("FRONTEND_IMAGE_TAG=<previous-stable-sha>", runbook)
        self.assertIn("curl -fsS http://127.0.0.1:8082/guide/", runbook)

    def test_issue_72_asset_direct_upload_thumbnail_contract_is_end_to_end(self):
        service = read(
            "yudao-module-aigc-asset/yudao-module-aigc-asset-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/asset/service/asset/AigcAssetServiceImpl.java"
        )
        token = read(
            "yudao-module-aigc-asset/yudao-module-aigc-asset-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/asset/dal/redis/AigcAssetUploadTokenRedisDAO.java"
        )
        prepare_dto = read(
            "yudao-module-aigc-asset/yudao-module-aigc-asset-api/src/main/java/"
            "cn/iocoder/yudao/module/aigc/asset/dto/AigcAssetDirectUploadPrepareRespDTO.java"
        )
        complete_dto = read(
            "yudao-module-aigc-asset/yudao-module-aigc-asset-api/src/main/java/"
            "cn/iocoder/yudao/module/aigc/asset/dto/AigcAssetDirectUploadCompleteReqDTO.java"
        )

        self.assertIn("IMAGE_THUMBNAIL_MAX_SIDE = 512", service)
        self.assertIn('IMAGE_THUMBNAIL_MIME_TYPE = "image/jpeg"', service)
        self.assertIn("shouldCreateImageThumbnail(reqDTO.getAssetType(), reqDTO.getMimeType())", service)
        self.assertIn('fileApi.presignPutUrlV2(thumbnailStorageFileName(reqDTO.getFileName()), "aigc/asset/thumbnail")', service)
        self.assertIn("AigcAssetUploadTokenRedisDAO.UploadToken.of(userId, reqDTO, presign, thumbnailPresign)", service)

        for field in [
            "thumbnailUploadUrl",
            "thumbnailUrl",
            "thumbnailConfigId",
            "thumbnailStorageType",
            "thumbnailBucket",
            "thumbnailObjectKey",
            "thumbnailPath",
            "thumbnailPublicAccess",
        ]:
            self.assertIn(f"private {self._java_type_for_thumbnail_prepare_field(field)} {field};", prepare_dto)
            self.assertIn(self._setter_for(field, "thumbnailPresign"), service)

        for field in [
            "thumbnailUrl",
            "thumbnailConfigId",
            "thumbnailStorageType",
            "thumbnailBucket",
            "thumbnailObjectKey",
            "thumbnailPath",
            "thumbnailFileName",
            "thumbnailMimeType",
            "thumbnailFileSize",
            "thumbnailWidth",
            "thumbnailHeight",
            "thumbnailPublicAccess",
        ]:
            self.assertIn(f"private {self._java_type_for_thumbnail_complete_field(field)} {field};", complete_dto)

        self.assertIn("private String thumbnailPath;", token)
        self.assertIn("private String thumbnailUrl;", token)
        self.assertIn("private Boolean thumbnailPublicAccess;", token)
        self.assertIn("thumbnailPresign == null ? null : thumbnailPresign.getPath()", token)
        self.assertIn("insertDirectUploadThumbnail(assetId, token, reqDTO)", service)
        self.assertIn("StrUtil.isBlank(token.getThumbnailPath())", service)
        self.assertIn("reqDTO.getThumbnailFileSize() == null", service)
        self.assertIn("fileApi.createFileRecordV2(new FileCreateRespDTO()", service)
        self.assertIn(".setPath(token.getThumbnailPath())", service)
        self.assertIn(".setType(StrUtil.blankToDefault(reqDTO.getThumbnailMimeType(), IMAGE_THUMBNAIL_MIME_TYPE))", service)
        self.assertIn("AigcAssetFileRoleEnum.THUMBNAIL.getCode()", service)

    def test_issue_104_author_stats_missing_row_returns_zero_snapshot_without_second_select(self):
        service = read(
            "yudao-module-aigc-community/yudao-module-aigc-community-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/community/service/AigcCommunityServiceImpl.java"
        )

        author_list = re.search(
            r"private List<AigcCommunityAuthorRespVO> buildAuthorRespList\(List<Long> authorIds, Long currentUserId\) \{(?P<body>.*?)\n    \}",
            service,
            re.S,
        )
        self.assertIsNotNone(author_list)
        body = author_list.group("body")

        self.assertIn("Map<Long, AigcCommunityAuthorStatsDO> statsMap = authorStatsMapper.selectListByUserIds(distinctAuthorIds)", body)
        self.assertIn("ensureAuthorStats(authorId)", body)
        self.assertIn("stats = new AigcCommunityAuthorStatsDO()", body)
        self.assertIn(".setFollowerCount(0)", body)
        self.assertIn(".setFollowingCount(0)", body)
        self.assertIn(".setPublicPostCount(0)", body)
        self.assertIn(".setLikeReceivedCount(0)", body)
        self.assertNotIn("stats = authorStatsMapper.selectByUserId(authorId)", body)
        self.assertIn(".setFollowerCount(stats == null ? 0 : stats.getFollowerCount())", body)
        self.assertIn(".setFollowingCount(stats == null ? 0 : stats.getFollowingCount())", body)
        self.assertIn(".setPublicPostCount(stats == null ? 0 : stats.getPublicPostCount())", body)
        self.assertIn(".setLikeReceivedCount(stats == null ? 0 : stats.getLikeReceivedCount())", body)

    def _setter_for(self, field: str, source: str) -> str:
        suffix = field[0].upper() + field[1:]
        source_getters = {
            "thumbnailUploadUrl": "getUploadUrl",
            "thumbnailUrl": "getUrl",
            "thumbnailConfigId": "getConfigId",
            "thumbnailStorageType": "getStorageType",
            "thumbnailBucket": "getBucket",
            "thumbnailObjectKey": "getObjectKey",
            "thumbnailPath": "getPath",
            "thumbnailPublicAccess": "getPublicAccess",
        }
        return f".set{suffix}({source} == null ? null : {source}.{source_getters[field]}())"

    def _java_type_for_thumbnail_prepare_field(self, field: str) -> str:
        if field == "thumbnailConfigId":
            return "Long"
        if field == "thumbnailPublicAccess":
            return "Boolean"
        return "String"

    def _java_type_for_thumbnail_complete_field(self, field: str) -> str:
        if field in {"thumbnailConfigId", "thumbnailFileSize"}:
            return "Long"
        if field in {"thumbnailWidth", "thumbnailHeight"}:
            return "Integer"
        if field == "thumbnailPublicAccess":
            return "Boolean"
        return "String"


if __name__ == "__main__":
    unittest.main()
