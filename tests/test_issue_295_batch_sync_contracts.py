import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require_section(text: str, pattern: str) -> str:
    match = re.search(pattern, text, re.S)
    if not match:
        raise AssertionError(f"missing section: {pattern}")
    return match.group("body")


class Issue295BatchSyncContractsTest(unittest.TestCase):

    def test_generation_run_batch_sync_sends_all_pending_nodes_to_backend_limit(self):
        hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")

        sync_body = require_section(
            hook,
            r"const syncProjectGenerationRuns = useCallback\(async \(\) => \{(?P<body>.*?)\n  \}, \[projectId\]\);",
        )
        self.assertIn("getNodesRef.current().flatMap((node) => {", sync_body)
        self.assertIn("return [{ projectId, nodeId: node.id, taskId, baseVersion: lastAppliedVersionRef.current, nodeType }];", sync_body)
        self.assertIn("nodes,", sync_body)
        self.assertIn("const response = await canvasNodeRunApi.syncProjectNodeRuns(projectId, {", sync_body)
        self.assertIn("onBatchSyncRef.current?.(response)", sync_body)
        self.assertNotIn(".slice(0, 20)", sync_body)
        self.assertNotIn("nodes.slice", sync_body)

    def test_batch_sync_response_metadata_is_typed_for_ui_decisions(self):
        api = read("yudao-ui/draw2video-client/src/features/canvas/canvas-node-run-api.ts")

        response_type = require_section(
            api,
            r"export type CanvasNodeRunBatchSyncResponse = \{(?P<body>.*?)\n\};",
        )
        for field in [
            "requestedCount: number;",
            "processedCount: number;",
            "truncated: boolean;",
            "limit: number;",
            "failedCount: number;",
        ]:
            self.assertIn(field, response_type)

    def test_canvas_page_surfaces_truncated_and_partial_failure_batch_sync_status(self):
        page = read("yudao-ui/draw2video-client/src/features/canvas/CanvasFlowPage.tsx")

        handler = require_section(
            page,
            r"const handleGenerationRunBatchSync = useCallback\(\(response: CanvasNodeRunBatchSyncResponse\) => \{(?P<body>.*?)\n  \}, \[\]\);",
        )
        self.assertIn("if (response.truncated)", handler)
        self.assertIn("response.processedCount", handler)
        self.assertIn("response.requestedCount", handler)
        self.assertIn("if (response.failedCount > 0)", handler)
        self.assertIn("节点已保留可重试状态", handler)
        self.assertIn('current.includes("生成任务") ? "" : current', handler)
        self.assertRegex(
            page,
            r"useCanvasGenerationRunEvents\(\s*serverProjectId,\s*isCanvasGenerationRunSseEnabled\(\),"
            r"\s*lastAppliedVersion,\s*\(\) => getNodes\(\) as AppNode\[\],"
            r"\s*applyGenerationRunOperation,\s*handleGenerationRunBatchSync\s*\)",
        )


if __name__ == "__main__":
    unittest.main()
