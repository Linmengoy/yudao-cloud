package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationRespVO;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AigcCanvasGenerationRunEventMessage {

    private String eventId;
    private Long projectId;
    private String nodeId;
    private String runId;
    private Long taskId;
    private String status;
    private Integer progress;
    private AigcCanvasOperationRespVO operation;
    private Long version;
    private Long emittedAt;

}
