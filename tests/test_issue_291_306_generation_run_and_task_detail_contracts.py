import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


class GenerationRunAndTaskDetailContractsTest(unittest.TestCase):

    def test_issue_291_server_scopes_task_result_sync_to_canvas_node(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )

        guard_match = re.search(
            r"private void validateResultBelongsToCanvasNode"
            r"\(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result\) \{(?P<body>.*?)\n    \}",
            service,
            re.S,
        )
        self.assertIsNotNone(guard_match)
        guard = guard_match.group("body")

        self.assertIn("CANVAS_NODE_RUN_TASK_NOT_BELONG", guard)
        self.assertIn('"canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_"', guard)
        self.assertIn("StrUtil.startWith(result.getClientRequestId()", guard)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_TASK_NOT_BELONG)", guard)

        self.assertIn(
            '"task_result_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_" + reqVO.getTaskId()',
            service,
        )
        self.assertIn('selectByClientOperation(projectId, "server_node_run", opId)', service)

    def test_issue_291_batch_sync_reports_failed_items_without_hiding_truncation(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        resp_vo = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/controller/app/vo/canvas/"
            "AigcCanvasNodeRunBatchSyncRespVO.java"
        )
        hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")

        self.assertIn("private static final int BATCH_SYNC_LIMIT = 20", service)
        self.assertIn(".limit(BATCH_SYNC_LIMIT)", service)
        self.assertIn(".setRequestedCount(requestedCount)", service)
        self.assertIn(".setProcessedCount(results.size())", service)
        self.assertIn(".setTruncated(requestedCount > BATCH_SYNC_LIMIT)", service)
        self.assertIn(".setFailedCount(Math.toIntExact(failedCount))", service)
        self.assertIn("private Integer failedCount;", resp_vo)
        self.assertIn("if (result.success === false || !result.operation) continue;", hook)

    def test_issue_291_result_patch_adapters_cover_image_video_and_text_nodes(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")
        video_node = read("yudao-ui/draw2video-client/src/features/canvas/VideoNode.tsx")

        result_patch = re.search(
            r"private JSONObject buildResultPatch\(String nodeType, AigcGenerateResultRespDTO result\) "
            r"\{(?P<body>.*?)\n    \}",
            service,
            re.S,
        )
        self.assertIsNotNone(result_patch)
        body = result_patch.group("body")
        self.assertIn('patch.set("content", result.getOutputText())', body)
        self.assertIn('patch.set("assetId", assetIds.get(0)).set("outputAssetId", assetIds.get(0))', body)
        self.assertIn('"video".equals(nodeType) ? "complete" : "idle"', body)
        self.assertIn('if ("image".equals(nodeType))', body)
        self.assertIn("applyImageOutputsPatch(patch, assetIds)", body)

        self.assertIn("function mergeImageOutputPatch", page)
        self.assertIn("function mergeVideoOutputPatch", page)
        self.assertIn("getImageOutputIdentity", page)
        self.assertIn("getVideoOutputIdentity", page)
        self.assertIn('operationType === "TASK_STATUS_PATCH"', page)
        self.assertIn("const imageMergedPatch = mergeImageOutputPatch(node as AppNode, patch)", page)
        self.assertIn("const mergedPatch = mergeVideoOutputPatch(node as AppNode, imageMergedPatch)", page)
        self.assertIn("const assetId =", video_node)
        self.assertIn("typeof data.assetId === \"number\"", video_node)
        self.assertIn("typeof data.outputAssetId === \"number\"", video_node)
        self.assertIn("buildPrimaryVideoPatch(primaryOutput)", video_node)

    def test_issue_291_asset_hydration_failure_keeps_asset_ids_for_later_recovery(self):
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")
        image_node = read("yudao-ui/draw2video-client/src/features/canvas/ImageNode.tsx")
        video_node = read("yudao-ui/draw2video-client/src/features/canvas/VideoNode.tsx")

        self.assertIn("const fetchPersonalAssetUrlEntries = async", page)
        self.assertIn("rememberAssetUrlEntries(await fetchPersonalAssetUrlEntries(pendingRequests))", page)
        self.assertIn("const missingRequests = pendingRequests.filter", page)
        self.assertIn("if (entriesByAssetId.size === 0) return;", page)
        self.assertIn("return entry ? withFreshAssetUrl(nextNode, entry.url, entry.expireTime, assetId) : nextNode;", page)
        self.assertIn("hydrated.filter((output) => output.previewUrl || output.assetId)", image_node)
        self.assertIn("if (cancelled || hydrated.outputs.length === 0) return;", video_node)

    def test_issue_306_public_and_admin_detail_do_not_reintroduce_raw_output_data(self):
        app_controller = read(
            "yudao-module-aigc-task/yudao-module-aigc-task-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/task/controller/app/AigcTaskAppController.java"
        )
        admin_controller = read(
            "yudao-module-aigc-task/yudao-module-aigc-task-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/task/controller/admin/task/AigcTaskController.java"
        )
        admin_page = read("yudao-ui/draw2video-admin/src/views/aigc/task/detail.vue")
        client_result = read("yudao-ui/draw2video-client/src/features/tasks/components/task-result.tsx")

        self.assertIn("taskService.getUserTaskWithResult(id, getLoginUserId())", app_controller)
        self.assertIn("return success(toAppRespDTO(task));", app_controller)
        self.assertIn("return hideInternalFields(BeanUtils.toBean(task, AigcTaskRespDTO.class));", app_controller)
        self.assertIn(".setOutputData(null)", app_controller)
        self.assertIn(".setOutputData(null)", admin_controller)
        self.assertIn("displayOutput", admin_page)
        self.assertIn("task.value.outputText || task.value.outputSummary || '-'", admin_page)
        self.assertIn("task.outputSummary", client_result)
        for source in (admin_page, client_result):
            self.assertNotIn("formatJson(task.outputData)", source)
            self.assertNotIn("结构化输出", source)


if __name__ == "__main__":
    unittest.main()
