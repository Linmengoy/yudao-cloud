import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require_section(source: str, pattern: str) -> str:
    match = re.search(pattern, source, re.S)
    if match is None:
        raise AssertionError(f"missing section matching {pattern!r}")
    return match.group("body")


class Issue296GenerationRunLegacyCompatContractsTest(unittest.TestCase):

    def test_legacy_task_without_generation_run_is_not_rejected_by_missing_canvas_prefix(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )

        validate = require_section(
            service,
            r"private void validateResultBelongsToCanvasNode\(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result\) \{(?P<body>.*?)\n    private ServiceException serviceException",
        )
        self.assertIn("generationRunMapper.selectByTaskId(result.getTaskId())", validate)
        self.assertIn("Objects.equals(generationRun.getProjectId(), reqVO.getProjectId())", validate)
        self.assertIn("Objects.equals(generationRun.getNodeId(), reqVO.getNodeId())", validate)
        self.assertIn("return;", validate)
        self.assertIn('String expectedPrefix = "canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_";', validate)
        self.assertIn('StrUtil.startWith(result.getClientRequestId(), "canvas_")', validate)
        self.assertIn("!StrUtil.startWith(result.getClientRequestId(), expectedPrefix)", validate)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_TASK_NOT_BELONG)", validate)
        self.assertNotIn('if (!StrUtil.startWith(result.getClientRequestId(), expectedPrefix))', validate)

    def test_legacy_generation_run_uses_stable_task_based_run_id_for_blank_or_old_request_id(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )

        update = require_section(
            service,
            r"private AigcCanvasGenerationRunDO updateGenerationRun\(AigcCanvasNodeRunSyncReqVO reqVO,\n"
            r"            AigcGenerateResultRespDTO result, AigcCanvasOperationLogDO operation\) \{(?P<body>.*?)\n    private String extractRunIdFromClientRequestId",
        )
        self.assertIn(".setRunId(extractRunIdFromClientRequestId(reqVO, result.getTaskId(), result.getClientRequestId()))", update)

        extractor = require_section(
            service,
            r"private String extractRunIdFromClientRequestId\(AigcCanvasNodeRunSyncReqVO reqVO, Long taskId, String clientRequestId\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn('String legacyRunId = "legacy_" + taskId;', extractor)
        self.assertIn("return legacyRunId;", extractor)
        self.assertIn('StrUtil.startWith(clientRequestId, "canvas_") ? clientRequestId : legacyRunId', extractor)
        self.assertNotIn("System.currentTimeMillis()", extractor)

    def test_text_node_migration_preserves_legacy_generation_state_and_asset_reference(self):
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")

        text_migration = require_section(
            page,
            r"if \(n.type === \"text\"\) \{(?P<body>.*?)\n  if \(n.type === \"sketch\"\)",
        )
        for required in [
            'taskId: typeof d.taskId === "string" ? d.taskId : null',
            'taskStatus: typeof d.taskStatus === "string" ? d.taskStatus : null',
            'progress: typeof d.progress === "number" ? d.progress : null',
            'outputAssetId: typeof d.outputAssetId === "number" ? d.outputAssetId : null',
            'outputPreviewUrl: typeof d.outputPreviewUrl === "string" ? d.outputPreviewUrl : null',
            'sourceTaskId: typeof d.sourceTaskId === "number" ? d.sourceTaskId : null',
            'generationStartedAt: typeof d.generationStartedAt === "string" ? d.generationStartedAt : null',
            'generationCompletedAt: typeof d.generationCompletedAt === "string" ? d.generationCompletedAt : null',
            'elapsedMs: typeof d.elapsedMs === "number" ? d.elapsedMs : null',
        ]:
            self.assertIn(required, text_migration)


if __name__ == "__main__":
    unittest.main()
