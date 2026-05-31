package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.websocket.core.listener.WebSocketMessageListener;
import cn.iocoder.yudao.framework.websocket.core.util.WebSocketFrameworkUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasOperationService;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationAppliedMessage;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationMessage;
import cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message.AigcCanvasOperationRejectedMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class AigcCanvasOperationMessageListener implements WebSocketMessageListener<AigcCanvasOperationMessage> {

    @Resource
    private AigcCanvasOperationService operationService;
    @Resource
    private AigcCanvasRoomService roomService;

    @Override
    public void onMessage(WebSocketSession session, AigcCanvasOperationMessage message) {
        Long userId = WebSocketFrameworkUtils.getLoginUserId(session);
        AigcCanvasOperationSubmitReqVO reqVO = BeanUtils.toBean(message, AigcCanvasOperationSubmitReqVO.class);
        try {
            AigcCanvasOperationLogDO operation = operationService.submitOperation(reqVO, userId);
            AigcCanvasOperationAppliedMessage appliedMessage = new AigcCanvasOperationAppliedMessage()
                    .setProjectId(operation.getProjectId())
                    .setClientId(operation.getClientId())
                    .setOpId(operation.getOpId())
                    .setActorUserId(operation.getActorUserId())
                    .setBaseVersion(operation.getBaseVersion())
                    .setVersion(operation.getNextVersion())
                    .setOperationType(operation.getOperationType())
                    .setOperationJson(operation.getOperationJson())
                    .setInverseOperationJson(operation.getInverseOperationJson());
            roomService.broadcast(operation.getProjectId(), "canvas-op-applied", appliedMessage, null);
        } catch (Exception ex) {
            AigcCanvasOperationRejectedMessage rejectedMessage = new AigcCanvasOperationRejectedMessage()
                    .setProjectId(message.getProjectId())
                    .setClientId(message.getClientId())
                    .setOpId(message.getOpId())
                    .setReason(ex.getClass().getSimpleName())
                    .setMessage(ex.getMessage())
                    .setServerVersion(operationService.getCurrentVersion(message.getProjectId(), userId));
            roomService.send(session.getId(), "canvas-op-rejected", rejectedMessage);
        }
    }

    @Override
    public String getType() {
        return "canvas-op";
    }

}
