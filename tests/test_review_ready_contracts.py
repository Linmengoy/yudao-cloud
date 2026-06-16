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
        self.assertIn("image: ${FRONTEND_IMAGE_REGISTRY_PREFIX:-}draw2video-guide:${FRONTEND_IMAGE_TAG:-latest}", compose)
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

    def test_issue_222_canvas_unload_flushes_only_when_pending_with_beacon_fallback(self):
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")
        operations = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-operations.ts")

        unload_match = re.search(
            r"useEffect\(\(\) => \{\n    if \(!serverProjectId \|\| isReadOnly\) return;(?P<body>.*?)\n  \}, "
            r"\[canvasOperations, isReadOnly, serverProjectId\]\);",
            page,
            re.S,
        )
        self.assertIsNotNone(unload_match)
        unload_body = unload_match.group("body")
        self.assertIn("if (pendingOperationCountRef.current <= 0) return;", unload_body)
        self.assertIn("canvasOperations.flushPendingOperations({ keepalive: true });", unload_body)
        self.assertIn('window.addEventListener("beforeunload", handleBeforeUnload);', unload_body)
        self.assertIn('window.addEventListener("pagehide", handlePageHide);', unload_body)
        self.assertIn("event.preventDefault();", unload_body)
        self.assertIn('event.returnValue = "";', unload_body)
        self.assertIn('window.removeEventListener("beforeunload", handleBeforeUnload);', unload_body)
        self.assertIn('window.removeEventListener("pagehide", handlePageHide);', unload_body)

        keepalive_match = re.search(
            r"function submitKeepaliveOperation\(projectId: string, clientId: string, operation: PendingCanvasOperation\) \{(?P<body>.*?)\n\}",
            operations,
            re.S,
        )
        self.assertIsNotNone(keepalive_match)
        keepalive_body = keepalive_match.group("body")
        self.assertIn("const body = JSON.stringify(buildOperationBody(projectId, clientId, operation));", keepalive_body)
        self.assertIn("!token && navigator.sendBeacon && body.length < 60_000", keepalive_body)
        self.assertIn("navigator.sendBeacon(url, blob)", keepalive_body)
        self.assertIn("keepalive: true", keepalive_body)

        flush_match = re.search(
            r"const flushPendingOperations = useCallback\(\(options\?: \{ keepalive\?: boolean \}\) => \{(?P<body>.*?)\n  \}, "
            r"\[clientId, projectId, sendRealtimeOperation, settleOperationFailure, settleOperationSuccess\]\);",
            operations,
            re.S,
        )
        self.assertIsNotNone(flush_match)
        flush_body = flush_match.group("body")
        self.assertIn("if (!projectId) return;", flush_body)
        self.assertIn("if (options?.keepalive) {", flush_body)
        self.assertIn("for (const pendingOperation of operations)", flush_body)
        self.assertIn("submitKeepaliveOperation(projectId, clientId, pendingOperation);", flush_body)
        self.assertIn("return;", flush_body)

    def test_issue_223_http_canvas_operation_broadcasts_same_applied_message_as_ws(self):
        controller = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/controller/app/AigcCanvasAppController.java"
        )
        project_service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasProjectServiceImpl.java"
        )
        ws_listener = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasOperationMessageListener.java"
        )
        realtime = read("yudao-ui/draw2video-client/src/features/canvas/canvas-realtime.ts")
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")

        self.assertIn("projectService.submitOperation(reqVO, getLoginUserId())", controller)

        http_submit_match = re.search(
            r"public AigcCanvasOperationLogDO submitOperation\(AigcCanvasOperationSubmitReqVO reqVO, Long userId\) \{(?P<body>.*?)\n    \}",
            project_service,
            re.S,
        )
        self.assertIsNotNone(http_submit_match)
        http_submit_body = http_submit_match.group("body")
        self.assertIn("operationService.submitOperation(reqVO, userId)", http_submit_body)
        self.assertIn('roomService.broadcast(operation.getProjectId(), "canvas-op-applied", buildAppliedMessage(operation), null);', http_submit_body)

        build_message_match = re.search(
            r"private AigcCanvasOperationAppliedMessage buildAppliedMessage\(AigcCanvasOperationLogDO operation\) \{(?P<body>.*?)\n    \}",
            project_service,
            re.S,
        )
        self.assertIsNotNone(build_message_match)
        build_message_body = build_message_match.group("body")
        for setter in [
            "setProjectId(operation.getProjectId())",
            "setClientId(operation.getClientId())",
            "setOpId(operation.getOpId())",
            "setActorUserId(operation.getActorUserId())",
            "setBaseVersion(operation.getBaseVersion())",
            "setVersion(operation.getNextVersion())",
            "setOperationType(operation.getOperationType())",
            "setOperationJson(operation.getOperationJson())",
            "setInverseOperationJson(operation.getInverseOperationJson())",
        ]:
            self.assertIn(setter, build_message_body)

        self.assertIn('roomService.broadcast(operation.getProjectId(), "canvas-op-applied", appliedMessage, null);', ws_listener)
        self.assertIn('type: "canvas-op-applied";', realtime)
        self.assertIn("canvasOperations.markOperationAcked(message.opId, version);", page)
        self.assertIn("applyOperationRecord({", page)

    def test_issue_219_release_runbook_records_current_and_previous_stable_sha_sources(self):
        runbook = read("script/deployment-runbook.md")
        test_workflow = read(".gitea/workflows/yudao-micro-cicd.yml")
        prod_workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")

        for required in [
            "## 发布前门禁",
            "回滚版本明确",
            "记录当前 commit SHA 和上一个稳定 commit SHA",
            "### 回滚版本从哪里取",
            "git rev-parse --short=12 HEAD",
            "workflow 页面显示的 commit 短 SHA",
            "git fetch gitea --tags",
            "prod-stable-",
            "上一条成功发布写回",
            "script/docker/community-release-evidence-index.md",
            "服务器当前运行镜像 tag",
            "不要把 `latest` 当作生产回滚版本",
            "previous_stable_image_tag",
        ]:
            self.assertIn(required, runbook)

        for workflow in [test_workflow, prod_workflow]:
            self.assertIn("previous_stable_image_tag", workflow)
            self.assertIn("PREVIOUS_STABLE_IMAGE_TAG", workflow)
            self.assertIn("Enforce rollback version gate", workflow)
            self.assertIn("bash script/docker/verify-release-evidence.sh preflight", workflow)
            self.assertIn("commit sha: $(git rev-parse HEAD)", workflow)
            self.assertIn("previous stable image tag:", workflow)
            self.assertIn("rollback command:", workflow)

    def test_issue_235_frontend_release_healthchecks_cover_admin_and_client(self):
        compose_paths = [
            "script/docker/docker-compose.frontend.yml",
            "script/docker/docker-compose-micro.yml",
            "script/docker/docker-compose-micro-prod.yml",
        ]

        for path in compose_paths:
            with self.subTest(path=path, service="draw2video-admin"):
                admin_block = self._yaml_service_block(read(path), "draw2video-admin")
                self.assertIn("healthcheck:", admin_block)
                self.assertIn("http://127.0.0.1/", admin_block)
                self.assertIn("interval: 10s", admin_block)
                self.assertIn("timeout: 5s", admin_block)
                self.assertIn("retries: 10", admin_block)

            with self.subTest(path=path, service="draw2video-client"):
                client_block = self._yaml_service_block(read(path), "draw2video-client")
                self.assertIn("healthcheck:", client_block)
                self.assertIn("http://127.0.0.1:3000/", client_block)
                self.assertIn("statusCode>=200&&r.statusCode<500", client_block)
                self.assertIn("interval: 10s", client_block)
                self.assertIn("timeout: 5s", client_block)
                self.assertIn("retries: 10", client_block)

        release_evidence = read("script/docker/frontend-release-evidence-20260616.md")
        deploy_guide = read("yudao-ui/deploy_frontend_command.md")
        runbook = read("script/deployment-runbook.md")
        for doc in [release_evidence, deploy_guide, runbook]:
            self.assertIn("docker compose", doc)
            self.assertIn("ps draw2video-client draw2video-admin", doc)
            self.assertIn("curl -fsS -I http://127.0.0.1:13000/", doc)
            self.assertIn("curl -fsS -I http://127.0.0.1:8081/", doc)
            self.assertIn("healthy", doc)
            self.assertRegex(doc, r"(失败|failure|timeout|connection error|连接失败)")

    def test_issue_236_frontend_release_evidence_assigns_uncommitted_changes(self):
        evidence = read("script/docker/frontend-release-evidence-20260616.md")

        expected_rows = {
            "script/caddy/Caddyfile": ("#236", "prod Caddy approval", "Caddy validate/reload evidence"),
            "script/d.md": ("#236", "include", "Documentation only"),
            "script/deploy-frontend-images.sh": ("#236", "include with release script review", "Bash parity"),
            "script/docker/docker-compose.frontend.yml": ("#235", "include", "container-level frontend health gate"),
            "script/docker/docker-compose-micro.yml": ("#235", "include", "micro compose behavior aligned"),
            "script/docker/docker-compose-micro-prod.yml": ("#235", "include", "prod compose health gate explicit"),
            "yudao-ui/deploy_frontend_command.md": ("#232, #233, #234, #235", "include", "Primary operator runbook"),
            "script/deployment-runbook.md": ("#234, #235", "include", "Top-level runbook cross-reference"),
            "yudao-ui/draw2video-client/pnpm-workspace.yaml": ("#232", "include", "ERR_PNPM_IGNORED_BUILDS"),
            "yudao-ui/draw2video-admin/pnpm-workspace.yaml": ("#233", "include", "ignored build scripts"),
        }

        for file_path, required_cells in expected_rows.items():
            with self.subTest(file_path=file_path):
                self.assertIn(f"`{file_path}`", evidence)
                row_match = re.search(rf"\| `{re.escape(file_path)}` \|(?P<row>.*)\|", evidence)
                self.assertIsNotNone(row_match)
                row = row_match.group("row")
                for required in required_cells:
                    self.assertIn(required, row)

        self.assertIn("No change in this table should be discarded automatically", evidence)
        self.assertIn("record the exclusion reason", evidence)
        self.assertIn("previous stable image tag", evidence)
        self.assertIn("latest` is not acceptable rollback evidence", evidence)

    def test_issue_214_admin_asset_pages_use_i18n_keys_without_chinese_literals(self):
        target_paths = [
            "yudao-ui/draw2video-admin/src/views/aigc/asset/index.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/asset/download-log/index.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/asset/prompt-template/index.vue",
            "yudao-ui/draw2video-admin/src/api/aigc/asset/index.ts",
            "yudao-ui/draw2video-admin/src/api/aigc/asset/prompt-template/index.ts",
        ]
        target_text = "\n".join(read(path) for path in target_paths)
        zh_locale = read("yudao-ui/draw2video-admin/src/locales/zh-CN.ts")
        en_locale = read("yudao-ui/draw2video-admin/src/locales/en.ts")

        self.assertNotRegex(target_text, r"[\u4e00-\u9fff]")
        for vue_path in target_paths[:3]:
            vue = read(vue_path)
            self.assertIn("const { t } = useI18n()", vue)

        for key in self._i18n_keys(target_text):
            if key.startswith("aigc.asset."):
                self.assertTrue(self._locale_has_path(zh_locale, key), key)
                self.assertTrue(self._locale_has_path(en_locale, key), key)

        for required in [
            "aigc.asset.assetTypes.image",
            "aigc.asset.sourceTypes.generate",
            "aigc.asset.auditStatus.pending",
            "aigc.asset.visibility.public",
            "aigc.asset.promptTemplate.rules.casesJsonFile",
            "aigc.asset.promptTemplate.result.totalCount",
            "aigc.asset.promptTemplate.errors.batchUploadFailed",
        ]:
            self.assertTrue(self._locale_has_path(zh_locale, required), required)
            self.assertTrue(self._locale_has_path(en_locale, required), required)

        asset_api = read("yudao-ui/draw2video-admin/src/api/aigc/asset/index.ts")
        prompt_template_api = read("yudao-ui/draw2video-admin/src/api/aigc/asset/prompt-template/index.ts")
        self.assertIn("url: '/aigc/asset/page'", asset_api)
        self.assertIn("url: '/aigc/asset/audit'", asset_api)
        self.assertIn("url: '/aigc/asset/prompt-template/import-awesome-gpt-image-files'", prompt_template_api)

        aigc_en_block = self._object_block(en_locale, "aigc")
        self.assertNotRegex(aigc_en_block, r"[\u4e00-\u9fff]")

    def test_issue_215_admin_aigc_i18n_scan_records_remaining_work_and_matching_locale_paths(self):
        scan = read("yudao-ui/draw2video-admin/src/views/aigc/i18n-hardcoded-scan-20260616.md")
        zh_locale = read("yudao-ui/draw2video-admin/src/locales/zh-CN.ts")
        en_locale = read("yudao-ui/draw2video-admin/src/locales/en.ts")

        for required in [
            "src/views/aigc/",
            "src/api/aigc/",
            "Asset pages already migrated",
            "Remaining user-visible Chinese by area:",
            "Model tenant and usage pages",
            "Billing pages",
            "Safety pages",
            "Release notes",
            'rg -n "[\\u4e00-\\u9fff]"',
        ]:
            self.assertIn(required, scan)

        zh_aigc_paths = self._locale_leaf_paths(zh_locale, "aigc")
        en_aigc_paths = self._locale_leaf_paths(en_locale, "aigc")
        self.assertEqual(zh_aigc_paths, en_aigc_paths)
        self.assertIn("aigc.asset.promptTemplate.importTip", zh_aigc_paths)
        self.assertIn("aigc.model.options.capabilities.textToImage", zh_aigc_paths)

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

    def _i18n_keys(self, text: str) -> set[str]:
        return set(re.findall(r"\bt\('([^']+)'\)", text))

    def _locale_has_path(self, locale_text: str, dotted_path: str) -> bool:
        keys = dotted_path.split(".")
        block = locale_text
        for key in keys:
            if not re.search(rf"(?:^|[\s,]){re.escape(key)}\s*:", block):
                return False
            if key != keys[-1]:
                block = self._object_block(block, key)
        return True

    def _locale_leaf_paths(self, locale_text: str, root_key: str) -> set[str]:
        block = self._object_block(locale_text, root_key)
        return {f"{root_key}.{path}" for path in self._leaf_paths(block)}

    def _yaml_service_block(self, text: str, service: str) -> str:
        match = re.search(rf"^  {re.escape(service)}:\n(?P<body>.*?)(?=^  [A-Za-z0-9_-]+:\n|\Z)", text, re.S | re.M)
        self.assertIsNotNone(match, service)
        return match.group("body")

    def _leaf_paths(self, block: str, prefix: str = "") -> set[str]:
        paths: set[str] = set()
        index = 1
        while index < len(block) - 1:
            match = re.search(r"([A-Za-z0-9_]+)\s*:", block[index:])
            if not match:
                break
            key_start = index + match.start(1)
            key = match.group(1)
            value_start = index + match.end()
            if self._inside_string(block, key_start):
                index = value_start
                continue
            path = f"{prefix}.{key}" if prefix else key
            next_value = self._skip_ws(block, value_start)
            if next_value < len(block) and block[next_value] == "{":
                child = self._balanced_block(block, next_value)
                paths.update(self._leaf_paths(child, path))
                index = next_value + len(child)
            else:
                paths.add(path)
                index = self._skip_value(block, next_value)
        return paths

    def _object_block(self, text: str, key: str) -> str:
        match = re.search(rf"(?:^|[\s,]){re.escape(key)}\s*:\s*\{{", text)
        self.assertIsNotNone(match, key)
        brace_index = text.find("{", match.start())
        return self._balanced_block(text, brace_index)

    def _balanced_block(self, text: str, brace_index: int) -> str:
        depth = 0
        quote = ""
        escaped = False
        for index in range(brace_index, len(text)):
            char = text[index]
            if quote:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == quote:
                    quote = ""
                continue
            if char in {"'", '"', "`"}:
                quote = char
            elif char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    return text[brace_index:index + 1]
        self.fail("Unbalanced locale object block")

    def _inside_string(self, text: str, position: int) -> bool:
        quote = ""
        escaped = False
        for char in text[:position]:
            if quote:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == quote:
                    quote = ""
            elif char in {"'", '"', "`"}:
                quote = char
        return bool(quote)

    def _skip_ws(self, text: str, index: int) -> int:
        while index < len(text) and text[index].isspace():
            index += 1
        return index

    def _skip_value(self, text: str, index: int) -> int:
        quote = ""
        escaped = False
        depth = 0
        while index < len(text):
            char = text[index]
            if quote:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == quote:
                    quote = ""
            elif char in {"'", '"', "`"}:
                quote = char
            elif char in "([{":
                depth += 1
            elif char in ")]}":
                if depth == 0 and char == "}":
                    return index
                depth = max(0, depth - 1)
            elif char == "," and depth == 0:
                return index + 1
            index += 1
        return index


if __name__ == "__main__":
    unittest.main()
