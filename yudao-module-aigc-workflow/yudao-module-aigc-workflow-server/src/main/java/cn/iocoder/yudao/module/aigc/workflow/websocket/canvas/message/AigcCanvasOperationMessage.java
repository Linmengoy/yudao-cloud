package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;

@Data
public class AigcCanvasOperationMessage {

    private Long projectId;
    private String clientId;
    private String opId;
    private Long baseVersion;
    private String operationType;
    private String operationJson;
    private String inverseOperationJson;

}
