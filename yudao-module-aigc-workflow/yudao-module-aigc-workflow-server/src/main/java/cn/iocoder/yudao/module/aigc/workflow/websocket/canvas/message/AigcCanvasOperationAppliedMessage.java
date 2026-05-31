package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcCanvasOperationAppliedMessage {

    private Long projectId;
    private String clientId;
    private String opId;
    private Long actorUserId;
    private Long baseVersion;
    private Long version;
    private String operationType;
    private String operationJson;
    private String inverseOperationJson;

}
