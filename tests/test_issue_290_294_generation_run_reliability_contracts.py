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


class GenerationRunReliabilityContractsTest(unittest.TestCase):

    def test_issue_294_connection_limit_is_reported_without_registering_extra_session(self):
        sse = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasGenerationRunSseService.java"
        )

        limit_branch = require_section(
            sse,
            r"if \(activeUserProjectConnections >= MAX_USER_PROJECT_CONNECTIONS\) \{(?P<body>.*?)\n        \}",
        )
        for required in [
            "connectionLimitedCount.incrementAndGet();",
            ".name(\"generation-run-connection-limit\")",
            ".id(\"connection-limit-\" + projectId + \"-\" + userId",
            "\"projectId\", projectId",
            "\"userId\", userId",
            "\"limit\", MAX_USER_PROJECT_CONNECTIONS",
            "emitter.complete();",
            "return emitter;",
        ]:
            self.assertIn(required, limit_branch)

        self.assertNotIn("sessions.put", limit_branch)

        subscribe = require_section(
            sse,
            r"public SseEmitter subscribe\(Long projectId, Long userId, String lastEventId\) \{(?P<body>.*?)\n    public void publish",
        )
        self.assertLess(subscribe.index("if (activeUserProjectConnections >= MAX_USER_PROJECT_CONNECTIONS)"),
                        subscribe.index("sessions.put(connectionId, session);"))

    def test_issue_294_resync_required_distinguishes_first_connect_from_unknown_last_event_id(self):
        sse = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasGenerationRunSseService.java"
        )

        subscribe = require_section(
            sse,
            r"public SseEmitter subscribe\(Long projectId, Long userId, String lastEventId\) \{(?P<body>.*?)\n    public void publish",
        )
        self.assertIn("boolean canResume = canResumeFromLastEventId(projectId, lastEventId);", subscribe)
        self.assertIn("sendResyncRequired(session, StrUtil.isBlank(lastEventId)", subscribe)
        self.assertIn("\"stream-connected\"", subscribe)
        self.assertIn("\"event-gap-or-unknown-last-event-id\"", subscribe)

        resync = require_section(
            sse,
            r"private void sendResyncRequired\(SseSession session, String reason\) \{(?P<body>.*?)\n    private boolean canResumeFromLastEventId",
        )
        for required in [
            "resyncRequiredCount.incrementAndGet();",
            ".name(\"resync-required\")",
            ".id(\"resync-required-\" + session.projectId() + \"-\" + session.userId()",
            "\"projectId\", session.projectId()",
            "\"userId\", session.userId()",
            "\"connectionId\", session.connectionId()",
            "\"reason\", reason",
            "\"emittedAt\", System.currentTimeMillis()",
        ]:
            self.assertIn(required, resync)

    def test_issue_294_resume_only_accepts_last_event_id_seen_in_same_project_session(self):
        sse = read(
            "yudao-module-aigc-workflow/yudao-module-aigc-workflow-server/src/main/java/"
            "cn/iocoder/yudao/module/aigc/workflow/websocket/canvas/AigcCanvasGenerationRunSseService.java"
        )

        can_resume = require_section(
            sse,
            r"private boolean canResumeFromLastEventId\(Long projectId, String lastEventId\) \{(?P<body>.*?)\n    private Collection<SseSession>",
        )
        for required in [
            "if (StrUtil.isBlank(lastEventId))",
            "return false;",
            "getProjectSessions(projectId).stream()",
            ".map(SseSession::lastEventId)",
            ".filter(StrUtil::isNotBlank)",
            ".anyMatch(lastEventId::equals)",
        ]:
            self.assertIn(required, can_resume)

        publish = require_section(
            sse,
            r"public void publish\(String eventName, AigcCanvasGenerationRunEventMessage message\) \{(?P<body>.*?)\n    private void remove",
        )
        self.assertIn("session.markSent(message.getEventId());", publish)


if __name__ == "__main__":
    unittest.main()
