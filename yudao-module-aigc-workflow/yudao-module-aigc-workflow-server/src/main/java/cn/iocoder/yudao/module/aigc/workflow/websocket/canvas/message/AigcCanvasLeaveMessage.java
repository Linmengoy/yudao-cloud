package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;

@Data
public class AigcCanvasLeaveMessage {

    private Long projectId;
    private String clientId;

}
