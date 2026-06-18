package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasGenerationRunEventMessage;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AigcCanvasGenerationRunSseService {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60_000L;
    private static final int MAX_USER_PROJECT_CONNECTIONS = 3;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

    private final Map<String, SseSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger heartbeatFailureCount = new AtomicInteger();
    private final AtomicInteger connectionLimitedCount = new AtomicInteger();
    private final AtomicInteger resyncRequiredCount = new AtomicInteger();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "aigc-canvas-generation-run-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public AigcCanvasGenerationRunSseService() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeatToAll,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        heartbeatExecutor.shutdownNow();
    }

    public SseEmitter subscribe(Long projectId, Long userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        String connectionId = UUID.randomUUID().toString();
        int activeUserProjectConnections = getUserProjectConnectionCount(projectId, userId);
        if (activeUserProjectConnections >= MAX_USER_PROJECT_CONNECTIONS) {
            connectionLimitedCount.incrementAndGet();
            log.warn("[subscribe][event=sse_connection_limited projectId({}) userId({}) activeCount({}) limit({})]",
                    projectId, userId, activeUserProjectConnections, MAX_USER_PROJECT_CONNECTIONS);
            try {
                emitter.send(SseEmitter.event()
                        .name("generation-run-connection-limit")
                        .id("connection-limit-" + projectId + "-" + userId + "-" + System.currentTimeMillis())
                        .data(Map.of("projectId", projectId, "userId", userId,
                                "limit", MAX_USER_PROJECT_CONNECTIONS)));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }
        SseSession session = new SseSession(projectId, userId, connectionId, emitter, lastEventId);
        boolean canResume = canResumeFromLastEventId(projectId, lastEventId);
        sessions.put(connectionId, session);
        Runnable cleanup = () -> remove(session);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        sendHeartbeat(session);
        if (!canResume) {
            sendResyncRequired(session, StrUtil.isBlank(lastEventId)
                    ? "stream-connected"
                    : "event-gap-or-unknown-last-event-id");
        }
        return emitter;
    }

    public void publish(String eventName, AigcCanvasGenerationRunEventMessage message) {
        Collection<SseSession> projectSessions = getProjectSessions(message.getProjectId());
        if (projectSessions.isEmpty()) {
            return;
        }
        for (SseSession session : projectSessions) {
            try {
                session.emitter().send(SseEmitter.event()
                        .name(eventName)
                        .id(message.getEventId())
                        .data(message));
                session.markSent(message.getEventId());
            } catch (IOException | IllegalStateException ex) {
                log.debug("[publish][event=sse_emit_failed projectId({}) userId({}) connectionId({}) eventId({}) reason({})]",
                        session.projectId(), session.userId(), session.connectionId(), message.getEventId(), ex.getMessage(), ex);
                remove(session);
            }
        }
    }

    private void remove(SseSession session) {
        if (sessions.remove(session.connectionId(), session)) {
            long durationMs = Duration.between(session.createdAt(), LocalDateTime.now()).toMillis();
            log.info("[remove][event=sse_connection_closed projectId({}) userId({}) connectionId({}) activeCount({}) durationMs({})]",
                    session.projectId(), session.userId(), session.connectionId(),
                    getProjectConnectionCount(session.projectId()), durationMs);
        }
    }

    public int getProjectConnectionCount(Long projectId) {
        return getProjectSessions(projectId).size();
    }

    public int getUserProjectConnectionCount(Long projectId, Long userId) {
        return Math.toIntExact(sessions.values().stream()
                .filter(session -> Objects.equals(projectId, session.projectId())
                        && Objects.equals(userId, session.userId()))
                .count());
    }

    public int getHeartbeatFailureCount() {
        return heartbeatFailureCount.get();
    }

    public int getConnectionLimitedCount() {
        return connectionLimitedCount.get();
    }

    public int getResyncRequiredCount() {
        return resyncRequiredCount.get();
    }

    private void sendHeartbeatToAll() {
        for (SseSession session : sessions.values()) {
            sendHeartbeat(session);
        }
    }

    void sendHeartbeatForTest(Long projectId, Long userId) {
        getProjectSessions(projectId).stream()
                .filter(session -> Objects.equals(userId, session.userId()))
                .forEach(this::sendHeartbeat);
    }

    private void sendHeartbeat(SseSession session) {
        try {
            session.emitter().send(SseEmitter.event()
                    .name("generation-run-heartbeat")
                    .id("heartbeat-" + session.projectId() + "-" + session.userId() + "-" + System.currentTimeMillis())
                    .data(Map.of("projectId", session.projectId(), "userId", session.userId(),
                            "connectionId", session.connectionId(), "emittedAt", System.currentTimeMillis())));
            session.markHeartbeat();
        } catch (IOException | IllegalStateException ex) {
            heartbeatFailureCount.incrementAndGet();
            log.warn("[sendHeartbeat][event=sse_emit_failed projectId({}) userId({}) connectionId({}) eventId({}) reason({})]",
                    session.projectId(), session.userId(), session.connectionId(), "generation-run-heartbeat",
                    ex.getMessage(), ex);
            remove(session);
        }
    }

    private void sendResyncRequired(SseSession session, String reason) {
        try {
            resyncRequiredCount.incrementAndGet();
            session.emitter().send(SseEmitter.event()
                    .name("resync-required")
                    .id("resync-required-" + session.projectId() + "-" + session.userId() + "-"
                            + System.currentTimeMillis())
                    .data(Map.of("projectId", session.projectId(), "userId", session.userId(),
                            "connectionId", session.connectionId(), "reason", reason,
                            "emittedAt", System.currentTimeMillis())));
        } catch (IOException | IllegalStateException ex) {
            remove(session);
        }
    }

    private boolean canResumeFromLastEventId(Long projectId, String lastEventId) {
        if (StrUtil.isBlank(lastEventId)) {
            return false;
        }
        return getProjectSessions(projectId).stream()
                .map(SseSession::lastEventId)
                .filter(StrUtil::isNotBlank)
                .anyMatch(lastEventId::equals);
    }

    private Collection<SseSession> getProjectSessions(Long projectId) {
        return sessions.values().stream()
                .filter(session -> Objects.equals(projectId, session.projectId()))
                .toList();
    }

    private static final class SseSession {

        private final Long projectId;
        private final Long userId;
        private final String connectionId;
        private final SseEmitter emitter;
        private final LocalDateTime createdAt = LocalDateTime.now();
        private volatile LocalDateTime lastHeartbeatAt;
        private volatile LocalDateTime lastSendAt;
        private volatile String lastEventId;

        private SseSession(Long projectId, Long userId, String connectionId, SseEmitter emitter, String lastEventId) {
            this.projectId = projectId;
            this.userId = userId;
            this.connectionId = connectionId;
            this.emitter = emitter;
            this.lastEventId = lastEventId;
        }

        Long projectId() {
            return projectId;
        }

        Long userId() {
            return userId;
        }

        String connectionId() {
            return connectionId;
        }

        SseEmitter emitter() {
            return emitter;
        }

        LocalDateTime createdAt() {
            return createdAt;
        }

        String lastEventId() {
            return lastEventId;
        }

        void markHeartbeat() {
            this.lastHeartbeatAt = LocalDateTime.now();
            this.lastSendAt = this.lastHeartbeatAt;
        }

        void markSent(String eventId) {
            this.lastEventId = eventId;
            this.lastSendAt = LocalDateTime.now();
        }

    }

}
