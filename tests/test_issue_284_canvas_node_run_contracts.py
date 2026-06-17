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


class Issue284CanvasNodeRunContractTest(unittest.TestCase):

    def test_sync_rejects_missing_or_wrong_canvas_task_before_patching_node(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        error_codes = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-api/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/enums/ErrorCodeConstants.java"
        )

        self.assertIn("CANVAS_NODE_RUN_TASK_NOT_EXISTS", error_codes)
        self.assertIn("生成任务不存在", error_codes)
        self.assertIn("CANVAS_NODE_RUN_TASK_NOT_BELONG", error_codes)
        self.assertIn("生成任务不属于当前画布节点", error_codes)

        get_result = require_section(
            service,
            r"private AigcGenerateResultRespDTO getResultReadyForCanvas\(Long taskId\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("if (result == null)", get_result)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS)", get_result)

        refresh_result = require_section(
            service,
            r"private AigcGenerateResultRespDTO refreshResultReadyForCanvas\(AigcGenerateResultRespDTO result\) \{(?P<body>.*?)\n    private void validateResultBelongsToCanvasNode",
        )
        self.assertGreaterEqual(refresh_result.count("throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS)"), 2)

        validate_result = require_section(
            service,
            r"private void validateResultBelongsToCanvasNode\(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("if (result == null || result.getTaskId() == null)", validate_result)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_TASK_NOT_EXISTS)", validate_result)
        self.assertIn(
            'String expectedPrefix = "canvas_" + reqVO.getProjectId() + "_" + reqVO.getNodeId() + "_";',
            validate_result,
        )
        self.assertIn("StrUtil.startWith(result.getClientRequestId(), expectedPrefix)", validate_result)
        self.assertIn("throw serviceException(CANVAS_NODE_RUN_TASK_NOT_BELONG)", validate_result)

    def test_success_sync_is_idempotent_and_runs_side_effects_after_operation_commit_path(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )

        sync_one = require_section(
            service,
            r"private AigcCanvasNodeRunRespVO syncOneNodeRun\(AigcCanvasNodeRunSyncReqVO reqVO, AigcGenerateResultRespDTO result,"
            r"\n            Long userId, boolean applySideEffects\) \{(?P<body>.*?)\n    private void broadcastAfterCommit\(AigcCanvasOperationRespVO operation\)",
        )
        required_order = [
            "validateResultBelongsToCanvasNode(reqVO, result)",
            "findTaskStatusPatch(reqVO.getProjectId(), opId)",
            "if (operation == null)",
            "submitTaskStatusPatch(",
            "created = Objects.equals(operation.getActorUserId(), userId)",
            "if (applySideEffects && created)",
            "applySuccessfulAssetSideEffects(reqVO, result)",
        ]
        positions = [sync_one.index(item) for item in required_order]
        self.assertEqual(positions, sorted(positions))
        self.assertNotIn("roomService.broadcast(", sync_one)

        broadcast_after_commit = require_section(
            service,
            r"private void broadcastAfterCommit\(AigcCanvasOperationLogDO operation\) \{(?P<body>.*?)\n    private AigcCanvasOperationAppliedMessage buildAppliedMessage",
        )
        for required in [
            'roomService.broadcast(operation.getProjectId(), "canvas-op-applied"',
            "TransactionSynchronizationManager.isSynchronizationActive()",
            "TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()",
            "public void afterCommit()",
            "broadcast.run();",
        ]:
            self.assertIn(required, broadcast_after_commit)


if __name__ == "__main__":
    unittest.main()
