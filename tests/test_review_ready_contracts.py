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
        self.assertIn('$TestImageVersionFile = Join-Path $RootDir "script\\docker\\test-image-version"', ps1)
        self.assertIn("function Get-TestImageVersion", ps1)
        self.assertIn('$ImageTag = Get-TestImageVersion', ps1)
        self.assertIn("git -C $RootDir rev-parse --short=12 HEAD", ps1)

        self.assertIn("--target all|admin|client|guide", sh)
        self.assertIn('all|admin|client|guide)', sh)
        self.assertIn('"draw2video-guide:$IMAGE_TAG"', sh)
        self.assertIn('TEST_IMAGE_VERSION_FILE="$ROOT_DIR/script/docker/test-image-version"', sh)
        self.assertIn("read_test_image_version", sh)
        self.assertIn('IMAGE_TAG="$(read_test_image_version)"', sh)
        self.assertIn('git -C "$ROOT_DIR" rev-parse --short=12 HEAD', sh)

        self.assertIn("draw2video-guide:", compose)
        self.assertIn("image: ${FRONTEND_IMAGE_REGISTRY_PREFIX:-}draw2video-guide:${FRONTEND_IMAGE_TAG:-latest}", compose)
        self.assertIn('"${DRAW2VIDEO_GUIDE_PORT:-8082}:80"', compose)
        self.assertIn("healthcheck:", compose)
        self.assertIn("http://127.0.0.1/health", compose)

        self.assertIn("--target guide", runbook)
        self.assertIn("script/docker/test-image-version", runbook)
        self.assertIn("v0.0.1", runbook)
        self.assertIn("FRONTEND_IMAGE_TAG=<previous-test-version>", runbook)
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
            "test 记录当前/上一稳定测试镜像版本",
            "### 回滚版本从哪里取",
            "script/docker/test-image-version",
            "v0.0.1",
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

    def test_issue_258_prod_ssh_and_frontend_health_recovery_evidence_is_recorded(self):
        evidence = read("script/docker/release-gate-evidence-20260617.md")
        runbook = read("script/deployment-runbook.md")
        frontend_compose = read("script/docker/docker-compose.frontend.yml")

        for required in [
            "#258 prod SSH and frontend health",
            "tmp/manman2-prod-ssh-health-20260617-094428.log",
            "tmp/manman2-prod-compose-default-fix-20260617-094545.log",
            'ssh manman2 "docker version" -> exit 0',
            "Docker Engine 29.1.3",
            'ssh manman2 "docker compose version" -> exit 0',
            "Docker Compose 2.40.3",
            'ssh manman2 "curl -fsS -I http://127.0.0.1:8081/" -> HTTP/1.1 200 OK',
            'ssh manman2 "curl -fsS -I http://127.0.0.1:13000/" -> HTTP/1.1 200 OK',
            "Initial `docker compose ps draw2video-admin draw2video-client` failed",
            "/opt/code/compose.yml",
            "the same command then returned both frontend containers",
        ]:
            self.assertIn(required, evidence)

        for required in [
            "ssh manman2 \"cd /opt/code && docker compose --env-file .frontend-prod.env -f docker-compose.frontend.yml ps draw2video-client draw2video-admin\"",
            "ssh manman2 \"curl -fsS -I http://127.0.0.1:13000/\"",
            "ssh manman2 \"curl -fsS -I http://127.0.0.1:8081/\"",
            "draw2video-client",
            "draw2video-admin",
            "发布证据必须包含 `docker compose ps` 的 `healthy` 状态",
        ]:
            self.assertIn(required, runbook)

        for service in ["draw2video-admin", "draw2video-client"]:
            with self.subTest(service=service):
                service_block = self._yaml_service_block(frontend_compose, service)
                self.assertIn("healthcheck:", service_block)
                self.assertIn("interval: 10s", service_block)
                self.assertIn("timeout: 5s", service_block)
                self.assertIn("retries: 10", service_block)

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

    def test_issue_293_generation_run_sse_gateway_non_buffering_gate_is_documented(self):
        runbook = read("script/deployment-runbook.md")

        for required in [
            "## GenerationRun SSE 发布门禁",
            "proxy_buffering off;",
            "proxy_read_timeout 3600s;",
            "X-Accel-Buffering no",
            "flush_interval -1",
            "read_timeout 1h",
            "Content-Type: text/event-stream",
            "generation-run-heartbeat",
            "resync-required",
        ]:
            self.assertIn(required, runbook)

    def test_issue_294_generation_run_sse_limits_heartbeat_cleanup_and_resync_metrics(self):
        sse = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasGenerationRunSseService.java"
        )
        hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")

        for required in [
            "MAX_PROJECT_CONNECTIONS = 6",
            "generation-run-connection-limit",
            "HEARTBEAT_INTERVAL_SECONDS = 15L",
            "heartbeatFailureCount.incrementAndGet()",
            "log.warn(\"[sendHeartbeat]",
            "getProjectConnectionCount(Long projectId)",
            "getHeartbeatFailureCount()",
            "getResyncRequiredCount()",
            "sendResyncRequired(projectId, emitter, \"stream-connected\")",
        ]:
            self.assertIn(required, sse)

        self.assertIn('event.type === "resync-required"', hook)
        self.assertIn('event.type === "generation-run-connection-limit"', hook)
        self.assertIn("void syncProjectGenerationRuns();", hook)

    def test_issue_295_generation_run_batch_sync_degrades_with_truncation_and_partial_failures(self):
        req = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/controller/app/vo/canvas/AigcCanvasNodeRunBatchSyncReqVO.java"
        )
        resp = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/controller/app/vo/canvas/AigcCanvasNodeRunBatchSyncRespVO.java"
        )
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        api = read("yudao-ui/draw2video-client/src/features/canvas/canvas-node-run-api.ts")
        hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")

        self.assertNotIn("@Size(max = 20", req)
        for field in ["requestedCount", "processedCount", "limit", "failedCount"]:
            self.assertIn(f"private Integer {field};", resp)
        self.assertIn("private Boolean truncated;", resp)
        self.assertIn("BATCH_SYNC_LIMIT = 20", service)
        self.assertIn(".limit(BATCH_SYNC_LIMIT)", service)
        self.assertIn(".setTruncated(requestedCount > BATCH_SYNC_LIMIT)", service)
        self.assertIn(".setFailedCount(Math.toIntExact(failedCount))", service)
        self.assertIn("buildFailedNodeRunResp(reqVO, ex.getCode(), ex.getMessage())", service)
        self.assertIn("syncProjectNodeRuns", api)
        self.assertIn("slice(0, 20)", hook)
        self.assertIn("result.success === false", hook)

    def test_issue_296_generation_run_keeps_old_canvas_nodes_compatible(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")

        self.assertIn("generationRunMapper.selectByProjectNodeAndTask", service)
        self.assertIn("if (generationRun == null) {", service)
        self.assertIn(".setRunId(extractRunIdFromClientRequestId(reqVO, result.getTaskId(), result.getClientRequestId()))", service)
        self.assertIn("validateResultBelongsToCanvasNode(reqVO, result)", service)
        self.assertIn("CANVAS_NODE_RUN_TASK_NOT_EXISTS", service)
        self.assertIn("generationRunMapper.selectByTaskId(result.getTaskId())", service)
        self.assertIn('String legacyRunId = "legacy_" + taskId;', service)
        self.assertIn("return data.status === \"pending\" || (typeof data.taskId === \"string\" && data.taskId.length > 0);", page)
        self.assertIn("taskId: typeof d.taskId === \"string\" ? d.taskId : null", page)
        self.assertIn("taskStatus: typeof d.taskStatus === \"string\" ? d.taskStatus : null", page)
        self.assertIn("outputAssetId: typeof d.outputAssetId === \"number\" ? d.outputAssetId : null", page)

    def test_issue_297_generation_run_release_gate_separates_ws_sse_rollback_and_metrics(self):
        runbook = read("script/deployment-runbook.md")
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")
        hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")

        for required in [
            "WebSocket 继续只负责画布协作编辑",
            "GenerationRun SSE 只负责生成任务状态",
            "不能重复应用终态 operation",
            "单节点 `/run/sync`",
            "项目级 `/nodes/run/sync` 批量同步",
            "前端无 EventSource/连接失败时的项目级批量同步降级",
            "关闭前端 SSE 开关",
            "恢复单节点同步主路径",
            "连接泄漏",
            "心跳失败",
            "批量同步失败",
            "终态应用失败",
        ]:
            self.assertIn(required, runbook)

        self.assertIn("useCanvasGenerationRunEvents(", page)
        self.assertIn("isCanvasGenerationRunSseEnabled()", page)
        self.assertIn("applyGenerationRunOperation", page)
        self.assertIn('NEXT_PUBLIC_CANVAS_GENERATION_SSE_ENABLED !== "false"', hook)
        self.assertIn("if (!enabled) {", hook)
        self.assertIn("void syncProjectGenerationRuns();", hook)
        self.assertIn("return;", hook)
        self.assertIn("onOperationRef.current(event.operation)", hook)
        self.assertIn("await readEventStream(response", hook)
        self.assertIn('fetch(`${API_BASE_URL}/canvas/projects/${projectId}/generation-runs/events`', hook)

        disabled_branch = re.search(
            r"if \(!enabled\) \{(?P<body>.*?)\n    \}",
            hook,
            re.S,
        )
        self.assertIsNotNone(disabled_branch)
        self.assertIn("void syncProjectGenerationRuns();", disabled_branch.group("body"))
        self.assertNotIn("fetch(", disabled_branch.group("body"))

        for required in [
            "NEXT_PUBLIC_CANVAS_GENERATION_SSE_ENABLED=false",
            "前端不 fetch /generation-runs/events",
            "没有 `/generation-runs/events` 请求",
            "仍可看到 `/nodes/run/sync` 项目级批量同步请求",
        ]:
            self.assertIn(required, runbook)

    def test_issue_280_aigc_model_release_gate_uses_immutable_tags_and_health_evidence(self):
        test_compose = read("script/docker/docker-compose-micro.yml")
        prod_compose = read("script/docker/docker-compose-micro-prod.yml")
        test_workflow = read(".gitea/workflows/yudao-micro-cicd.yml")
        prod_workflow = read(".gitea/workflows/yudao-micro-cicd-prod.yml")
        gate_script = read("script/docker/verify-release-evidence.sh")
        runbook = read("script/deployment-runbook.md")

        for path, compose in [
            ("script/docker/docker-compose-micro.yml", test_compose),
            ("script/docker/docker-compose-micro-prod.yml", prod_compose),
        ]:
            with self.subTest(path=path):
                block = self._yaml_service_block(compose, "aigc-model")
                self.assertIn("image: ${MICRO_IMAGE_REGISTRY_PREFIX:-}aigc-model:${MICRO_IMAGE_TAG:-latest}", block)
                self.assertNotIn("image: aigc-model:latest", block)
                self.assertIn("healthcheck:", block)
                self.assertIn("http://127.0.0.1:48090/actuator/health", block)

        self.assertIn('service="${INPUT_SERVICE:-aigc-gen}"', test_workflow)
        self.assertIn('aigc-model) module="yudao-module-aigc-model/yudao-module-aigc-model-server" ;;', test_workflow)
        self.assertIn('image_tag="$(tr -d \'[:space:]\' < "${test_image_version_file}")"', test_workflow)
        self.assertIn("MICRO_IMAGE_TAG=${image_tag}", test_workflow)
        self.assertIn("REGISTRY_PUSH_PREFIX=127.0.0.1:3000/root/manman", test_workflow)
        self.assertIn("Push image to Gitea registry", test_workflow)
        self.assertIn("docker compose -f script/docker/docker-compose-micro.yml pull", test_workflow)
        self.assertIn("--no-build --no-deps --force-recreate", test_workflow)
        self.assertIn("bash script/docker/verify-release-evidence.sh preflight", test_workflow)
        self.assertIn("SERVICE_HEALTH_URL=\"http://127.0.0.1:48090/actuator/health\"", test_workflow)
        self.assertIn("bash script/docker/verify-release-evidence.sh verify-service-health", test_workflow)
        self.assertIn("docker compose -f script/docker/docker-compose-micro.yml logs --tail=200 aigc-model", test_workflow)

        self.assertIn('image_tag="$(git rev-parse --short=12 HEAD)"', prod_workflow)
        self.assertIn("MICRO_IMAGE_TAG=${image_tag}", prod_workflow)
        self.assertIn("REGISTRY_PUSH_PREFIX=111.228.39.103:3000/root/manman", prod_workflow)
        self.assertIn("Push image to Gitea registry", prod_workflow)
        self.assertIn("docker compose -f docker-compose-micro.yml pull", prod_workflow)
        self.assertIn("--no-build --no-deps --force-recreate", prod_workflow)
        self.assertIn("previous_stable_image_tag", prod_workflow)
        self.assertIn("bash script/docker/verify-release-evidence.sh preflight", prod_workflow)
        self.assertIn("SERVICE_HEALTH_URL=\"http://127.0.0.1:48090/actuator/health\"", prod_workflow)
        self.assertIn("verify-release-evidence.sh\" verify-service-health", prod_workflow)
        self.assertIn("MICRO_IMAGE_TAG=<previous-stable-sha>", prod_workflow)

        for required in [
            "reject_latest_tag \"$MICRO_IMAGE_TAG\" \"MICRO_IMAGE_TAG\"",
            "MICRO_IMAGE_TAG must be a semantic test image tag such as v0.0.1",
            "MICRO_IMAGE_TAG must be an immutable Git SHA tag",
            "previous_stable_image_tag is required for rollback evidence",
            "previous_stable_image_tag must be a Git SHA tag",
            "verify_service_health()",
            "docker image inspect \"${item}:${MICRO_IMAGE_TAG}\"",
            "docker pull \"$previous_ref\"",
            "rollback command: MICRO_IMAGE_TAG=${previous_tag}",
        ]:
            self.assertIn(required, gate_script)

        for required in [
            "`aigc-model` 发布证据必须包含",
            "docker image inspect aigc-model:<tag>",
            "SERVICE_HEALTH_URL=http://127.0.0.1:48090/actuator/health",
            "MICRO_IMAGE_TAG=<previous-stable-tag>",
            "不要把 `latest` 当作生产回滚版本",
        ]:
            self.assertIn(required, runbook)

    def test_issue_279_aigc_model_channel_test_gate_rebuilds_api_and_checks_error_codes(self):
        gate = read("script/aigc-model-channel-test-gate.ps1")
        constants = read(
            "yudao-module-aigc-model/yudao-module-aigc-model-api/src/main/java/"
            "cn/iocoder/yudao/module/aigc/model/enums/ErrorCodeConstants.java"
        )
        channel_test = read(
            "yudao-module-aigc-model/yudao-module-aigc-model-server/src/test/java/"
            "cn/iocoder/yudao/module/aigc/model/service/channel/AigcModelChannelServiceImplTest.java"
        )

        self.assertIn('$ErrorActionPreference = "Stop"', gate)
        self.assertIn(".m2\\repository\\cn\\iocoder\\cloud\\yudao-module-aigc-model-api", gate)
        self.assertIn("Remove-Item -LiteralPath $apiArtifact -Recurse -Force", gate)
        self.assertIn("mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-api -am install -DskipTests", gate)
        self.assertIn(
            'mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am '
            '"-Dtest=AigcModelPriceServiceImplTest,AigcModelChannelServiceImplTest" '
            '"-Dsurefire.failIfNoSpecifiedTests=false" test',
            gate,
        )

        for constant, code in [
            ("MODEL_CHANNEL_DUPLICATE", "1_041_001_102"),
            ("MODEL_CHANNEL_REFERENCED_BY_ROUTE", "1_041_001_104"),
        ]:
            with self.subTest(constant=constant):
                self.assertIn(f"ErrorCode {constant} = new ErrorCode({code}", constants)
                self.assertIn(f"import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.{constant};", channel_test)
                self.assertIn(f"assertEquals({constant}.getCode(), exception.getCode());", channel_test)

        self.assertNotIn("NoSuchFieldError", gate)

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

    def test_issue_213_model_price_route_proxy_param_pages_use_i18n_options(self):
        target_paths = [
            "yudao-ui/draw2video-admin/src/views/aigc/model/price/index.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/price/PriceForm.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/route/index.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/route/RouteForm.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/proxy/index.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/proxy/ProxyForm.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/param/index.vue",
            "yudao-ui/draw2video-admin/src/views/aigc/model/param/ParamForm.vue",
        ]
        target_text = "\n".join(read(path) for path in target_paths)
        constants = read("yudao-ui/draw2video-admin/src/views/aigc/model/constants.ts")
        zh_locale = read("yudao-ui/draw2video-admin/src/locales/zh-CN.ts")
        en_locale = read("yudao-ui/draw2video-admin/src/locales/en.ts")

        for vue_path in target_paths:
            vue = read(vue_path)
            self.assertIn("const { t } = useI18n()", vue, vue_path)

        for dynamic_option in [
            "AIGC_MODEL_CAPABILITIES",
            "AIGC_BILLING_UNITS",
            "AIGC_ROUTE_STRATEGIES",
            "AIGC_PROXY_PROTOCOLS",
            "AIGC_PARAM_TYPES",
        ]:
            self.assertRegex(target_text, rf"getOptionLabel\((?:\[item\]|{dynamic_option}), [^,\n]+, t\)")

        for label_key in [
            "aigc.model.options.capabilities.textToImage",
            "aigc.model.options.billingUnits.perTask",
            "aigc.model.options.routeStrategies.fixedModel",
            "aigc.model.options.proxyProtocols.socks5h",
            "aigc.model.options.paramTypes.string",
        ]:
            self.assertIn(f"labelKey: '{label_key}'", constants)

        zh_model_paths = self._locale_leaf_paths(zh_locale, "aigc")
        en_model_paths = self._locale_leaf_paths(en_locale, "aigc")
        for key in self._i18n_keys(target_text) | set(label_key for label_key in [
            "aigc.model.options.capabilities.textToImage",
            "aigc.model.options.billingUnits.perTask",
            "aigc.model.options.routeStrategies.fixedModel",
            "aigc.model.options.proxyProtocols.socks5h",
            "aigc.model.options.paramTypes.string",
        ]):
            if key.startswith("aigc.model."):
                self.assertIn(key, zh_model_paths)
                self.assertIn(key, en_model_paths)

        self.assertIn("t('aigc.model.fields.platformCostPrice')", target_text)
        self.assertIn("t('aigc.model.tips.platformCostPrice')", target_text)
        self.assertIn("t('aigc.model.fields.userSalePrice')", target_text)
        self.assertIn("t('aigc.model.fields.required') : t('aigc.model.fields.optional')", target_text)

    def test_issue_197_admin_model_cost_price_labels_explain_channel_vs_platform_costs(self):
        channel_form = read("yudao-ui/draw2video-admin/src/views/aigc/model/channel/ChannelForm.vue")
        price_form = read("yudao-ui/draw2video-admin/src/views/aigc/model/price/PriceForm.vue")
        zh_locale = read("yudao-ui/draw2video-admin/src/locales/zh-CN.ts")
        en_locale = read("yudao-ui/draw2video-admin/src/locales/en.ts")

        self.assertIn("t('aigc.model.fields.channelCostPrice')", channel_form)
        self.assertIn("t('aigc.model.tips.channelCostPrice')", channel_form)
        self.assertIn("v-model=\"formData.costPrice\"", channel_form)
        self.assertIn("formType !== 'clone'", channel_form)

        self.assertIn("t('aigc.model.fields.platformCostPrice')", price_form)
        self.assertIn("t('aigc.model.tips.platformCostPrice')", price_form)
        self.assertIn("t('aigc.model.fields.userSalePrice')", price_form)
        self.assertIn("v-model=\"formData.costPrice\"", price_form)
        self.assertIn("v-model=\"formData.salePrice\"", price_form)

        for key in [
            "aigc.model.fields.channelCostPrice",
            "aigc.model.fields.platformCostPrice",
            "aigc.model.fields.userSalePrice",
            "aigc.model.tips.channelCostPrice",
            "aigc.model.tips.platformCostPrice",
        ]:
            self.assertTrue(self._locale_has_path(zh_locale, key), key)
            self.assertTrue(self._locale_has_path(en_locale, key), key)

        for required_text in [
            "channelCostPrice: 'API 调用成本'",
            "platformCostPrice: '平台成本价'",
            "userSalePrice: '用户售价'",
            "填写渠道商实际收取的价格，用于平台成本核算，不影响用户计费",
            "平台成本价用于统计毛利，用户实际扣费以销售价为准",
        ]:
            self.assertIn(required_text, zh_locale)

    def test_issue_196_admin_channel_clone_uses_dedicated_dialog_payload_and_success_copy(self):
        channel_index = read("yudao-ui/draw2video-admin/src/views/aigc/model/channel/index.vue")
        channel_form = read("yudao-ui/draw2video-admin/src/views/aigc/model/channel/ChannelForm.vue")
        channel_api = read("yudao-ui/draw2video-admin/src/api/aigc/model/channel/index.ts")
        zh_locale = read("yudao-ui/draw2video-admin/src/locales/zh-CN.ts")
        en_locale = read("yudao-ui/draw2video-admin/src/locales/en.ts")

        self.assertIn("openForm('clone', scope.row.id)", channel_index)
        self.assertIn("formRef.value.open(type, id, queryParams.modelId)", channel_index)

        self.assertIn("sourceChannelId?: number", channel_form)
        self.assertIn("dialogTitle.value = type === 'clone' ? t('aigc.model.actions.clone')", channel_form)
        self.assertIn("<el-col v-if=\"formType !== 'clone'\"", channel_form)
        self.assertIn(":span=\"formType === 'clone' ? 24 : 12\"", channel_form)
        self.assertIn("<el-row v-else :gutter=\"20\">", channel_form)
        self.assertIn("sourceChannelId: id", channel_form)
        self.assertIn("status: CommonStatusEnum.DISABLE", channel_form)
        self.assertIn("name: formData.value.name ? t('aigc.model.fallbacks.cloneName'", channel_form)
        self.assertIn("AigcModelChannelApi.cloneChannel({", channel_form)
        for field in [
            "sourceChannelId: Number(formData.value.sourceChannelId)",
            "targetProviderId: Number(formData.value.providerId)",
            "providerModel: formData.value.providerModel",
            "name: formData.value.name",
            "weight: formData.value.weight",
        ]:
            self.assertIn(field, channel_form)
        self.assertIn("message.success(t('aigc.model.messages.cloneChannelSuccess'))", channel_form)

        self.assertIn("export interface AigcModelChannelCloneReqVO", channel_api)
        self.assertIn("sourceChannelId: number", channel_api)
        self.assertIn("targetProviderId: number", channel_api)
        self.assertIn("providerModel?: string", channel_api)
        self.assertIn("name?: string", channel_api)
        self.assertIn("weight?: number", channel_api)
        self.assertIn("url: '/aigc/model/channel/clone'", channel_api)
        clone_req_match = re.search(
            r"interface AigcModelChannelCloneReqVO \{(?P<body>.*?)\n\}",
            channel_api,
            re.S,
        )
        self.assertIsNotNone(clone_req_match)
        self.assertNotIn("costPrice?: number", clone_req_match.group("body"))

        self.assertIn("clone: '克隆'", zh_locale)
        self.assertIn("cloneChannelSuccess: '克隆成功，已默认禁用，请核对后启用'", zh_locale)
        self.assertIn("clone: 'Clone'", en_locale)
        self.assertIn("cloneChannelSuccess: 'Clone created and disabled by default. Review it before enabling.'", en_locale)

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
