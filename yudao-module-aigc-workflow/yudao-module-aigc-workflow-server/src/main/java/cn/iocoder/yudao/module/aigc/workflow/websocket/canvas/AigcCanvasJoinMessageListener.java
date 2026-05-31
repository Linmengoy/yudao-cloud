package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.framework.websocket.core.listener.WebSocketMessageListener;
import cn.iocoder.yudao.framework.websocket.core.util.WebSocketFrameworkUtils;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasProjectService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasJoinMessage;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasMemberMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AigcCanvasJoinMessageListener implements WebSocketMessageListener<AigcCanvasJoinMessage> {

    @Resource
    private AigcCanvasProjectService projectService;
    @Resource
    private AigcCanvasRoomService roomService;

    @Override
    public void onMessage(WebSocketSession session, AigcCanvasJoinMessage message) {
        Long userId = WebSocketFrameworkUtils.getLoginUserId(session);
        projectService.validateReadableProject(message.getProjectId(), userId);
        roomService.join(message.getProjectId(), session.getId());
        roomService.broadcastMemberEvent(message.getProjectId(), new AigcCanvasMemberMessage()
                .setProjectId(message.getProjectId())
                .setUserId(userId)
                .setUserType(WebSocketFrameworkUtils.getLoginUserType(session))
                .setClientId(message.getClientId())
                .setEvent("join"), session.getId());
    }

    @Override
    public String getType() {
        return "canvas-join";
    }

}
