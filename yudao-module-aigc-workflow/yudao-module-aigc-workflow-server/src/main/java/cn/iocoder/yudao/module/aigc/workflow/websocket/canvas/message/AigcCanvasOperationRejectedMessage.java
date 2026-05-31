package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcCanvasOperationRejectedMessage {

    private Long projectId;
    private String clientId;
    private String opId;
    private String reason;
    private String message;
    private Long serverVersion;

}
