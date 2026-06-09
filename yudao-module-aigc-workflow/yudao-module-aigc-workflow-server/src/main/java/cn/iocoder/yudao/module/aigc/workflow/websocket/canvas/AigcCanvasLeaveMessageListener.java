package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.framework.websocket.core.listener.WebSocketMessageListener;
import cn.iocoder.yudao.framework.websocket.core.util.WebSocketFrameworkUtils;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasLeaveMessage;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasMemberMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AigcCanvasLeaveMessageListener implements WebSocketMessageListener<AigcCanvasLeaveMessage> {

    @Resource
    private AigcCanvasRoomService roomService;

    @Override
    public void onMessage(WebSocketSession session, AigcCanvasLeaveMessage message) {
        Long userId = WebSocketFrameworkUtils.getLoginUserId(session);
        if (!roomService.isJoined(message.getProjectId(), session.getId())) {
            return;
        }
        roomService.leave(message.getProjectId(), session.getId());
        roomService.broadcastMemberEvent(message.getProjectId(), new AigcCanvasMemberMessage()
                .setProjectId(message.getProjectId())
                .setUserId(userId)
                .setUserType(WebSocketFrameworkUtils.getLoginUserType(session))
                .setClientId(message.getClientId())
                .setEvent("leave"), session.getId());
    }

    @Override
    public String getType() {
        return "canvas-leave";
    }

}
