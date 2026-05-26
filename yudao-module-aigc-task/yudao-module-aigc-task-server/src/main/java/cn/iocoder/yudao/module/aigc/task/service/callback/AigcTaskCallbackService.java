package cn.iocoder.yudao.module.aigc.task.service.callback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.callback.vo.AigcTaskCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskCallbackDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;

public interface AigcTaskCallbackService {

    Long createCallbackRecord(AigcTaskCallbackCreateReqDTO reqDTO);

    AigcTaskCallbackDO getCallback(Long id);

    PageResult<AigcTaskCallbackDO> getCallbackPage(AigcTaskCallbackPageReqVO reqVO);

    void processCallback(Long id, AigcTaskStatusUpdateReqDTO reqDTO);

    void markProcessFailed(Long id, String failReason);

    void replayCallback(Long id);
}
