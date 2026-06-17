package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasGenerationRunEventMessage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AigcCanvasGenerationRunSseService {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60_000L;
    private static final int MAX_PROJECT_CONNECTIONS = 6;
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15L;

    private final Map<Long, Set<SseEmitter>> projectEmitters = new ConcurrentHashMap<>();
    private final AtomicInteger heartbeatFailureCount = new AtomicInteger();
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

    public SseEmitter subscribe(Long projectId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        Set<SseEmitter> emitters = projectEmitters.computeIfAbsent(projectId, key -> ConcurrentHashMap.newKeySet());
        if (emitters.size() >= MAX_PROJECT_CONNECTIONS) {
            try {
                emitter.send(SseEmitter.event()
                        .name("generation-run-connection-limit")
                        .id("connection-limit-" + projectId + "-" + System.currentTimeMillis())
                        .data(Map.of("projectId", projectId, "limit", MAX_PROJECT_CONNECTIONS)));
            } catch (IOException ignored) {
            }
            emitter.complete();
            return emitter;
        }
        emitters.add(emitter);
        Runnable cleanup = () -> remove(projectId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        sendHeartbeat(projectId, emitter);
        sendResyncRequired(projectId, emitter, "stream-connected");
        return emitter;
    }

    public void publish(String eventName, AigcCanvasGenerationRunEventMessage message) {
        Set<SseEmitter> emitters = projectEmitters.get(message.getProjectId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .id(message.getEventId())
                        .data(message));
            } catch (IOException | IllegalStateException ex) {
                log.debug("[publish][projectId({}) eventName({}) SSE emitter closed]",
                        message.getProjectId(), eventName, ex);
                remove(message.getProjectId(), emitter);
            }
        }
    }

    private void remove(Long projectId, SseEmitter emitter) {
        Set<SseEmitter> emitters = projectEmitters.get(projectId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            projectEmitters.remove(projectId, emitters);
        }
    }

    public int getProjectConnectionCount(Long projectId) {
        Set<SseEmitter> emitters = projectEmitters.get(projectId);
        return emitters == null ? 0 : emitters.size();
    }

    public int getHeartbeatFailureCount() {
        return heartbeatFailureCount.get();
    }

    public int getResyncRequiredCount() {
        return resyncRequiredCount.get();
    }

    private void sendHeartbeatToAll() {
        for (Map.Entry<Long, Set<SseEmitter>> entry : projectEmitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                sendHeartbeat(entry.getKey(), emitter);
            }
        }
    }

    private void sendHeartbeat(Long projectId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("generation-run-heartbeat")
                    .id("heartbeat-" + projectId + "-" + System.currentTimeMillis())
                    .data(Map.of("projectId", projectId, "emittedAt", System.currentTimeMillis())));
        } catch (IOException | IllegalStateException ex) {
            heartbeatFailureCount.incrementAndGet();
            log.warn("[sendHeartbeat][projectId({}) heartbeat failed, closing SSE emitter]", projectId, ex);
            remove(projectId, emitter);
        }
    }

    private void sendResyncRequired(Long projectId, SseEmitter emitter, String reason) {
        try {
            resyncRequiredCount.incrementAndGet();
            emitter.send(SseEmitter.event()
                    .name("resync-required")
                    .id("resync-required-" + projectId + "-" + System.currentTimeMillis())
                    .data(Map.of("projectId", projectId, "reason", reason, "emittedAt", System.currentTimeMillis())));
        } catch (IOException | IllegalStateException ex) {
            remove(projectId, emitter);
        }
    }

}
