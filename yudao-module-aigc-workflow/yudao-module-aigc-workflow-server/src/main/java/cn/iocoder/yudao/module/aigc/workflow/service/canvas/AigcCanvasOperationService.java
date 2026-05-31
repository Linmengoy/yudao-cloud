package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSubmitReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasOperationSyncRespVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasOperationLogDO;

import java.util.List;

public interface AigcCanvasOperationService {

    AigcCanvasOperationLogDO submitOperation(AigcCanvasOperationSubmitReqVO reqVO, Long userId);

    List<AigcCanvasOperationLogDO> getOperationsAfterVersion(Long projectId, Long afterVersion, Long userId);

    AigcCanvasOperationSyncRespVO syncOperations(Long projectId, Long afterVersion, Long userId);

    Long getCurrentVersion(Long projectId, Long userId);

    void invalidateGraphState(Long projectId);

}
