package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcCanvasMemberMessage {

    private Long projectId;
    private Long userId;
    private Integer userType;
    private String clientId;
    private String event;

}
