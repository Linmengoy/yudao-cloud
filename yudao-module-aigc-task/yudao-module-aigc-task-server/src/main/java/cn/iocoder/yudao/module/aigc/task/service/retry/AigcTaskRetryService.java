package cn.iocoder.yudao.module.aigc.task.service.retry;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.retry.vo.AigcTaskRetryPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskRetryDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRetryCreateReqDTO;

import java.util.List;

public interface AigcTaskRetryService {

    Long createRetryRecord(AigcTaskRetryCreateReqDTO reqDTO);

    AigcTaskRetryDO getRetry(Long id);

    PageResult<AigcTaskRetryDO> getRetryPage(AigcTaskRetryPageReqVO reqVO);

    void cancelRetry(Long id);

    List<AigcTaskRetryDO> scanWaitingRetries();

    void markRetryRunning(Long id);

    void markRetrySuccess(Long id);

    void markRetryFailed(Long id, String failReason);
}
