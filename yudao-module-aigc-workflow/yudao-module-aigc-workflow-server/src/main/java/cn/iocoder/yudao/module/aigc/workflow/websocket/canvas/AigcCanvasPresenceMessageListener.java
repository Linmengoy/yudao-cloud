package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.framework.websocket.core.listener.WebSocketMessageListener;
import cn.iocoder.yudao.framework.websocket.core.util.WebSocketFrameworkUtils;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasProjectService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasPresenceMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AigcCanvasPresenceMessageListener implements WebSocketMessageListener<AigcCanvasPresenceMessage> {

    @Resource
    private AigcCanvasProjectService projectService;
    @Resource
    private AigcCanvasRoomService roomService;

    @Override
    public void onMessage(WebSocketSession session, AigcCanvasPresenceMessage message) {
        Long userId = WebSocketFrameworkUtils.getLoginUserId(session);
        projectService.validateReadableProject(message.getProjectId(), userId);
        roomService.broadcast(message.getProjectId(), "canvas-presence", message, session.getId());
    }

    @Override
    public String getType() {
        return "canvas-presence";
    }

}
