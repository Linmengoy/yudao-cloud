package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;

@Data
public class AigcCanvasJoinMessage {

    private Long projectId;
    private String clientId;
    private Long lastAppliedVersion;

}
