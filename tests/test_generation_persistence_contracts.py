import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class GenerationPersistenceContractsTest(unittest.TestCase):

    def test_issue_263_generation_outputs_are_archived_and_checked_before_persistence(self):
        archive_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveService.java"
        )
        record_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/record/AigcGenerateRecordServiceImpl.java"
        )
        test_file = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/test/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveServiceTest.java"
        )

        self.assertIn("archiveOutputData(String outputData)", archive_service)
        self.assertIn("archiveOutputUrls(String outputUrls)", archive_service)
        self.assertIn("isInlineMediaSource(source)", archive_service)
        self.assertIn("assertNoPersistentInlineMedia(String value, String fieldName)", archive_service)
        self.assertIn("must not contain inline base64 media", archive_service)
        self.assertIn("String archivedOutputUrls = mediaArchiveService.archiveOutputUrls(reqDTO.getOutputUrls())", record_service)
        self.assertIn("String archivedOutputUrls = mediaArchiveService.archiveOutputUrls(resp.getOutputUrls())", record_service)
        self.assertIn("assertNoInlinePersistentMedia(archivedOutputData, archivedOutputUrls)", record_service)
        self.assertIn("aigc_gen_record.output_data", record_service)
        self.assertIn("aigc_gen_record.output_urls", record_service)
        self.assertIn("testArchiveOutputData_uploadsInlineMediaArrayValues", test_file)
        self.assertIn("testAssertNoPersistentInlineMedia_rejectsBase64Media", test_file)
        self.assertIn("testArchiveOutputUrls_uploadsDataUrlBeforePersistence", test_file)

    def test_issue_269_workspace_model_selection_respects_reference_count(self):
        contracts = read("yudao-ui/draw2video-client/src/app/(app)/app/workspace-page-contracts.ts")
        page = read("yudao-ui/draw2video-client/src/app/(app)/app/page.tsx")
        test_file = read("yudao-ui/draw2video-client/src/app/(app)/app/workspace-page-contracts.test.ts")

        self.assertIn("getModelMaxReferenceImages", contracts)
        self.assertIn("getCompatibleModels", contracts)
        self.assertIn("referenceImageCount", page)
        self.assertIn("getCompatibleModels(quickModels, selectedTab, quickGenerationMode, referenceImageCount)", page)
        self.assertIn("pickDefaultModelId(quickModels, selectedTab, quickGenerationMode, referenceImageCount)", page)
        self.assertIn("switches default video model away from first-frame-only models", test_file)
        self.assertIn("IMAGE_TO_VIDEO", test_file)

    def test_issue_247_video_outputs_are_stored_as_urls_and_assets(self):
        archive_service_test = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/test/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveServiceTest.java"
        )
        record_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/record/AigcGenerateRecordServiceImpl.java"
        )

        self.assertIn("testArchiveOutputUrls_uploadsVideoDataUrlBeforePersistence", archive_service_test)
        self.assertIn('eq("video/mp4")', archive_service_test)
        self.assertIn('mediaArchiveService.assertNoPersistentInlineMedia(archived, "aigc_gen_record.output_urls")', archive_service_test)
        self.assertIn('case "VIDEO" -> assetApi.createVideoAsset(reqDTO).getCheckedData();', record_service)
        self.assertIn(".setOutputUrls(JSONUtil.toJsonStr(storedUrls))", record_service)
        self.assertIn(".setAssetIds(JSONUtil.toJsonStr(assetIds))", record_service)

    def test_issue_247_remote_video_links_are_downloaded_before_database_persistence(self):
        archive_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/media/AigcMediaArchiveService.java"
        )
        record_service = read(
            "yudao-module-aigc-gen/yudao-module-aigc-gen-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/gen/service/record/AigcGenerateRecordServiceImpl.java"
        )
        evidence = read("script/docker/fullstack-dev-evidence-20260617-automation-2.md")

        self.assertIn("REMOTE_DOWNLOAD_TIMEOUT_MILLIS = 30_000", archive_service)
        self.assertIn("AigcGenerateFileSecurityUtils.isSafeRemoteUrl(trimmed)", archive_service)
        self.assertIn("media = downloadRemoteMedia(trimmed);", archive_service)
        self.assertIn('Header.ACCEPT, "image/*,video/*,audio/*,*/*;q=0.5"', archive_service)
        self.assertIn("mimeType = mimeTypeFromUrl(url)", archive_service)
        self.assertIn('case "mp4" -> "video/mp4";', archive_service)
        self.assertIn('case "webm" -> "video/webm";', archive_service)
        self.assertIn('fileApi.createFile(media.content(), archiveFileName(media.mimeType()), directory', archive_service)
        self.assertIn("String archivedOutputUrls = mediaArchiveService.archiveOutputUrls(reqDTO.getOutputUrls())", record_service)
        self.assertIn("String archivedOutputUrls = mediaArchiveService.archiveOutputUrls(resp.getOutputUrls())", record_service)
        self.assertIn("assertNoInlinePersistentMedia(archivedOutputData, archivedOutputUrls)", record_service)
        self.assertIn("#247: generation video outputs are covered by OSS/file-service archiving tests", evidence)
        self.assertIn("existing asset creation persists only URL strings plus asset IDs", evidence)

    def test_issue_275_admin_test_stable_tag_evidence_is_explicit_and_pullable(self):
        evidence = read("script/docker/fullstack-dev-evidence-20260617-automation-2.md")
        compose = read("script/docker/docker-compose.frontend.yml")
        runbook = read("script/docker/frontend-release-evidence-20260616.md")

        self.assertIn("#275: test `draw2video-admin` previous stable image tag", evidence)
        self.assertIn("127.0.0.1:3000/root/draw2video-admin:latest", evidence)
        self.assertIn("111.228.39.103:3000/root/draw2video-admin:test-96d366aa2e66", evidence)
        self.assertIn("docker pull result: Status: Image is up to date", evidence)
        self.assertIn("docker inspect draw2video-admin --format '{{.Config.Image}}'", evidence)
        self.assertIn("HTTP/1.1 200 OK", evidence)
        self.assertIn("latest` is not acceptable rollback evidence", runbook)
        self.assertIn("ssh manman \"docker inspect draw2video-admin --format '{{.Config.Image}}'\"", runbook)
        self.assertIn("ssh manman \"curl -fsS -I http://127.0.0.1:8081/\"", runbook)
        self.assertIn("image: ${FRONTEND_IMAGE_REGISTRY_PREFIX:-}draw2video-admin:${FRONTEND_IMAGE_TAG:-latest}", compose)
        self.assertIn('"${DRAW2VIDEO_ADMIN_PORT:-8081}:80"', compose)
        self.assertIn("healthcheck:", compose)

    def test_issue_248_marketing_home_reuses_www_visuals_and_public_community(self):
        page = read("yudao-ui/draw2video-client/src/app/(marketing)/page.tsx")

        self.assertIn("/www-home/assets/images/hero-cinema.webp", page)
        self.assertIn("/www-home/assets/images/inspiration-portrait.webp", page)
        self.assertIn("getCommunityPosts({ pageNo: 1, pageSize: 6, sort: \"hot\" })", page)
        self.assertIn("CommunityPostCard", page)
        self.assertIn("公开作品", page)
        self.assertIn("Seedance 2.0", page)

    def test_issues_288_to_291_canvas_generation_runs_use_project_level_sync(self):
        table_sql = read("sql/mysql/workflow/aigc_canvas_generation_run.sql")
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        controller = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/controller/app/AigcCanvasAppController.java"
        )
        sse_service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasGenerationRunSseService.java"
        )
        events_hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")
        api = read("yudao-ui/draw2video-client/src/features/canvas/canvas-node-run-api.ts")
        image_node = read("yudao-ui/draw2video-client/src/features/canvas/ImageNode.tsx")
        video_node = read("yudao-ui/draw2video-client/src/features/canvas/VideoNode.tsx")
        text_node = read("yudao-ui/draw2video-client/src/features/canvas/TextNode.tsx")

        self.assertIn("CREATE TABLE IF NOT EXISTS `aigc_canvas_generation_run`", table_sql)
        self.assertIn("UNIQUE KEY `uk_canvas_generation_run_task` (`task_id`)", table_sql)
        self.assertIn("UNIQUE KEY `uk_canvas_generation_run_project_node_task` (`project_id`, `node_id`, `task_id`)", table_sql)
        self.assertIn("UNIQUE KEY `uk_canvas_generation_run_project_node_run` (`project_id`, `node_id`, `run_id`)", table_sql)
        self.assertIn("upsertGenerationRun(reqVO, runId, submit, userId, operation)", service)
        self.assertIn("updateGenerationRun(reqVO, result, operation)", service)
        self.assertIn("projectService.validateReadableProject(projectId, userId)", service)
        self.assertIn('publishGenerationRunAfterCommit("generation-run-status"', service)
        self.assertIn("getGenerationRunEventName(result)", service)
        self.assertIn('@GetMapping("/projects/{id}/generation-runs/events")', controller)
        self.assertIn('name("generation-run-heartbeat")', sse_service)
        self.assertIn('name("resync-required")', sse_service)
        self.assertIn("MAX_PROJECT_CONNECTIONS = 6", sse_service)
        self.assertIn("syncProjectNodeRuns", api)
        self.assertIn("/nodes/run/sync", api)
        self.assertIn("useCanvasGenerationRunEvents", events_hook)
        self.assertIn("text/event-stream", events_hook)
        self.assertIn("syncProjectGenerationRuns", events_hook)
        self.assertNotIn("waitCanvasNodeRunResult", image_node)
        self.assertNotIn("waitCanvasNodeRunResult", video_node)
        self.assertNotIn("waitCanvasNodeRunResult", text_node)


if __name__ == "__main__":
    unittest.main()
