package cn.iocoder.yudao.module.aigc.workflow.websocket.canvas.message;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AigcCanvasPresenceMessage {

    private Long projectId;
    private String clientId;
    private Map<String, Object> cursor;
    private List<String> selectedNodeIds;
    private String editingNodeId;
    private Map<String, Object> viewport;

}
