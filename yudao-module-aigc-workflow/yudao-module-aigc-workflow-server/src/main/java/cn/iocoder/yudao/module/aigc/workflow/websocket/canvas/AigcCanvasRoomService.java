package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.framework.websocket.core.sender.WebSocketMessageSender;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasMemberMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AigcCanvasRoomService {

    private final Map<Long, Set<String>> projectSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionProjects = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private WebSocketMessageSender webSocketMessageSender;

    public void join(Long projectId, String sessionId) {
        projectSessions.computeIfAbsent(projectId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionProjects.put(sessionId, projectId);
    }

    public void leave(Long projectId, String sessionId) {
        Set<String> sessions = projectSessions.get(projectId);
        if (sessions != null) {
            sessions.remove(sessionId);
        }
        sessionProjects.remove(sessionId);
    }

    public Set<String> getSessionIds(Long projectId) {
        return projectSessions.getOrDefault(projectId, Collections.emptySet());
    }

    public void broadcast(Long projectId, String messageType, Object message, String excludeSessionId) {
        if (webSocketMessageSender == null) {
            return;
        }
        for (String sessionId : getSessionIds(projectId)) {
            if (sessionId.equals(excludeSessionId)) {
                continue;
            }
            webSocketMessageSender.sendObject(sessionId, messageType, message);
        }
    }

    public void send(String sessionId, String messageType, Object message) {
        if (webSocketMessageSender == null) {
            return;
        }
        webSocketMessageSender.sendObject(sessionId, messageType, message);
    }

    public void broadcastMemberEvent(Long projectId, AigcCanvasMemberMessage message, String excludeSessionId) {
        broadcast(projectId, "canvas-member-updated", message, excludeSessionId);
    }

}
