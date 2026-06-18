package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasGenerationRunEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AigcCanvasGenerationRunSseServiceTest {

    @Test
    void subscribe_completionCallbackRemovesUserProjectSession() throws Exception {
        AigcCanvasGenerationRunSseService service = new AigcCanvasGenerationRunSseService();
        SseEmitter emitter = service.subscribe(100L, 200L, null);

        assertEquals(1, service.getProjectConnectionCount(100L));
        assertEquals(1, service.getUserProjectConnectionCount(100L, 200L));

        triggerCompletionCallback(emitter);

        assertEquals(0, service.getProjectConnectionCount(100L));
        assertEquals(0, service.getUserProjectConnectionCount(100L, 200L));
        service.shutdown();
    }

    @Test
    void subscribe_limitsConnectionsBySameUserAndSameProjectOnly() {
        AigcCanvasGenerationRunSseService service = new AigcCanvasGenerationRunSseService();
        service.subscribe(100L, 200L, null);
        service.subscribe(100L, 200L, null);
        service.subscribe(100L, 200L, null);
        service.subscribe(100L, 201L, null);

        service.subscribe(100L, 200L, null);

        assertEquals(4, service.getProjectConnectionCount(100L));
        assertEquals(3, service.getUserProjectConnectionCount(100L, 200L));
        assertEquals(1, service.getUserProjectConnectionCount(100L, 201L));
        assertEquals(1, service.getConnectionLimitedCount());
        service.shutdown();
    }

    @Test
    void subscribe_unknownLastEventIdTriggersResyncRequired() {
        AigcCanvasGenerationRunSseService service = new AigcCanvasGenerationRunSseService();

        service.subscribe(100L, 200L, "generation-run-unknown");

        assertEquals(1, service.getResyncRequiredCount());
        service.shutdown();
    }

    @Test
    void subscribe_knownLastEventIdDoesNotForceResync() {
        AigcCanvasGenerationRunSseService service = new AigcCanvasGenerationRunSseService();
        service.subscribe(100L, 200L, null);

        service.publish("generation-run-status", new AigcCanvasGenerationRunEventMessage()
                .setEventId("generation-run-100-node-1-1")
                .setProjectId(100L)
                .setNodeId("node")
                .setTaskId(1L));
        int afterInitialResync = service.getResyncRequiredCount();
        service.subscribe(100L, 200L, "generation-run-100-node-1-1");

        assertEquals(afterInitialResync, service.getResyncRequiredCount());
        service.shutdown();
    }

    @Test
    void heartbeatFailureRemovesSession() {
        AigcCanvasGenerationRunSseService service = new AigcCanvasGenerationRunSseService();
        SseEmitter emitter = service.subscribe(100L, 200L, null);
        emitter.complete();

        service.sendHeartbeatForTest(100L, 200L);

        assertEquals(1, service.getHeartbeatFailureCount());
        assertEquals(0, service.getUserProjectConnectionCount(100L, 200L));
        service.shutdown();
    }

    private static void triggerCompletionCallback(SseEmitter emitter) throws Exception {
        Field field = emitter.getClass().getSuperclass().getDeclaredField("completionCallback");
        field.setAccessible(true);
        Object callback = field.get(emitter);
        Method run = callback.getClass().getDeclaredMethod("run");
        run.setAccessible(true);
        run.invoke(callback);
    }

}
