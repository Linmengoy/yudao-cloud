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


class GenerationRunSseContractsTest(unittest.TestCase):

    def test_issue_289_project_sse_requires_read_permission_and_subscribes_by_project(self):
        controller = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/controller/app/AigcCanvasAppController.java"
        )
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )

        endpoint = require_section(
            controller,
            r"@GetMapping\(\"/projects/\{id\}/generation-runs/events\"\).*?"
            r"public SseEmitter subscribeGenerationRunEvents\(@PathVariable\(\"id\"\) Long id,\n"
            r"\s+@RequestHeader\(value = \"Last-Event-ID\", required = false\) String lastEventId\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("return nodeRunService.subscribeGenerationRunEvents(id, getLoginUserId(), lastEventId);", endpoint)

        subscription = require_section(
            service,
            r"public SseEmitter subscribeGenerationRunEvents\(Long projectId, Long userId, String lastEventId\) \{(?P<body>.*?)\n    \}",
        )
        self.assertIn("projectService.validateReadableProject(projectId, userId)", subscription)
        self.assertIn("return generationRunSseService.subscribe(projectId, userId, lastEventId);", subscription)

    def test_issue_289_generation_run_events_are_after_commit_and_versioned(self):
        service = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/service/canvas/AigcCanvasNodeRunServiceImpl.java"
        )
        message = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/message/AigcCanvasGenerationRunEventMessage.java"
        )

        publisher = require_section(
            service,
            r"private void publishGenerationRunAfterCommit\(String eventName, AigcCanvasGenerationRunDO generationRun,\n"
            r"            AigcCanvasOperationRespVO operation\) \{(?P<body>.*?)\n    private AigcGenerateResultRespDTO getResultReadyForCanvas",
        )
        for required in [
            ".setEventId(\"generation-run-\" + generationRun.getProjectId() + \"-\" + generationRun.getNodeId()",
            ".setRunId(generationRun.getRunId())",
            ".setTaskId(generationRun.getTaskId())",
            ".setOperation(operation)",
            ".setVersion(operation == null ? null : operation.getNextVersion())",
            "Runnable publish = () -> generationRunSseService.publish(eventName, message);",
            "TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()",
            "public void afterCommit()",
        ]:
            self.assertIn(required, publisher)

        for field in [
            "private String eventId;",
            "private Long projectId;",
            "private String nodeId;",
            "private String runId;",
            "private Long taskId;",
            "private AigcCanvasOperationRespVO operation;",
            "private Long version;",
        ]:
            self.assertIn(field, message)

    def test_issue_289_sse_service_limits_connections_and_forces_initial_resync(self):
        sse = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasGenerationRunSseService.java"
        )

        subscribe = require_section(
            sse,
            r"public SseEmitter subscribe\(Long projectId, Long userId, String lastEventId\) \{(?P<body>.*?)\n    public void publish",
        )
        for required in [
            "MAX_USER_PROJECT_CONNECTIONS = 3",
            "getUserProjectConnectionCount(projectId, userId)",
            ".name(\"generation-run-connection-limit\")",
            "\"limit\", MAX_USER_PROJECT_CONNECTIONS",
            "emitter.complete();",
            "emitter.onCompletion(cleanup);",
            "emitter.onTimeout(cleanup);",
            "emitter.onError(error -> cleanup.run());",
            "sendHeartbeat(session);",
            "canResumeFromLastEventId(projectId, lastEventId)",
            "sendResyncRequired(session",
        ]:
            self.assertIn(required, subscribe if required != "MAX_USER_PROJECT_CONNECTIONS = 3" else sse)

        self.assertIn("private final Map<String, SseSession> sessions = new ConcurrentHashMap<>();", sse)
        self.assertIn("private static final class SseSession", sse)

    def test_issue_297_disabled_sse_and_failures_use_project_batch_sync_only(self):
        hook = read("yudao-ui/draw2video-client/src/features/canvas/use-canvas-generation-run-events.ts")
        api = read("yudao-ui/draw2video-client/src/features/canvas/canvas-node-run-api.ts")
        runbook = read("script/deployment-runbook.md")

        disabled_branch = require_section(hook, r"if \(!enabled\) \{(?P<body>.*?)\n    \}")
        self.assertIn("void syncProjectGenerationRuns();", disabled_branch)
        self.assertNotIn("fetch(", disabled_branch)

        sync_callback = require_section(
            hook,
            r"const syncProjectGenerationRuns = useCallback\(async \(\) => \{(?P<body>.*?)\n  \}, \[projectId\]\);",
        )
        self.assertIn("canvasNodeRunApi.syncProjectNodeRuns", sync_callback)
        self.assertNotIn("slice(0, 20)", sync_callback)
        self.assertNotIn("syncNodeRun(", sync_callback)
        for field in ["requestedCount: number", "processedCount: number", "truncated: boolean", "limit: number", "failedCount: number"]:
            self.assertIn(field, api)
        self.assertIn("api.post<CanvasNodeRunBatchSyncResponse>(`/canvas/projects/${projectId}/nodes/run/sync`", api)

        connection_handler = require_section(
            hook,
            r"await readEventStream\(response, \(event\) => \{(?P<body>.*?)\n        \}\);",
        )
        self.assertIn("if (event.eventId) lastSseEventIdRef.current = event.eventId;", connection_handler)
        self.assertIn('event.type === "resync-required"', connection_handler)
        self.assertIn('event.type === "generation-run-connection-limit"', connection_handler)
        self.assertIn("void syncProjectGenerationRuns();", connection_handler)
        self.assertIn('"Last-Event-ID": lastSseEventIdRef.current', hook)
        self.assertIn("line.startsWith(\"id:\")", hook)

        error_handler = require_section(
            hook,
            r"await readEventStream\(response, \(event\) => \{.*?\n        \}\);\n      \} catch \{(?P<body>.*?)\n      \}",
        )
        self.assertIn("window.setTimeout(connect, 3_000)", error_handler)
        self.assertIn("void syncProjectGenerationRuns();", error_handler)

        for required in [
            "NEXT_PUBLIC_CANVAS_GENERATION_SSE_ENABLED=false",
            "前端不 fetch /generation-runs/events",
            "仍可看到 `/nodes/run/sync` 项目级批量同步请求",
            "禁止重复提交终态 operation",
        ]:
            self.assertIn(required, runbook)


if __name__ == "__main__":
    unittest.main()
