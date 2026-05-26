package cn.iocoder.yudao.module.aigc.task.service.retry;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.controller.admin.retry.vo.AigcTaskRetryPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskRetryDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskRetryMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRetryCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskRetryStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.task.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcTaskRetryServiceImpl implements AigcTaskRetryService {

    @Resource
    private AigcTaskRetryMapper retryMapper;
    @Resource
    private AigcTaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRetryRecord(AigcTaskRetryCreateReqDTO reqDTO) {
        AigcTaskDO task = taskService.validateTaskExists(reqDTO.getTaskId());
        if (task.getRetryCount() != null && task.getMaxRetryCount() != null && task.getRetryCount() >= task.getMaxRetryCount()) {
            throw exception(TASK_RETRY_EXCEED_LIMIT);
        }
        AigcTaskRetryDO retry = BeanUtils.toBean(reqDTO, AigcTaskRetryDO.class)
                .setRetryNo("RT" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr())
                .setRetryStatus(AigcTaskRetryStatusEnum.WAITING.getCode())
                .setTaskNo(reqDTO.getTaskNo() == null ? task.getTaskNo() : reqDTO.getTaskNo())
                .setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        retryMapper.insert(retry);
        taskService.increaseRetryCount(reqDTO.getTaskId());
        return retry.getId();
    }

    @Override
    public AigcTaskRetryDO getRetry(Long id) {
        AigcTaskRetryDO retry = retryMapper.selectById(id);
        if (retry == null) {
            throw exception(TASK_RETRY_NOT_EXISTS);
        }
        return retry;
    }

    @Override
    public PageResult<AigcTaskRetryDO> getRetryPage(AigcTaskRetryPageReqVO reqVO) {
        return retryMapper.selectPage(reqVO);
    }

    @Override
    public void cancelRetry(Long id) {
        getRetry(id);
        retryMapper.updateById(new AigcTaskRetryDO().setId(id).setRetryStatus(AigcTaskRetryStatusEnum.CANCELLED.getCode()));
    }

    @Override
    public List<AigcTaskRetryDO> scanWaitingRetries() {
        return retryMapper.selectWaitingRetries(LocalDateTime.now());
    }

    @Override
    public void markRetryRunning(Long id) {
        getRetry(id);
        retryMapper.updateById(new AigcTaskRetryDO().setId(id)
                .setRetryStatus(AigcTaskRetryStatusEnum.RUNNING.getCode())
                .setStartTime(LocalDateTime.now()));
    }

    @Override
    public void markRetrySuccess(Long id) {
        getRetry(id);
        retryMapper.updateById(new AigcTaskRetryDO().setId(id)
                .setRetryStatus(AigcTaskRetryStatusEnum.SUCCESS.getCode())
                .setFinishTime(LocalDateTime.now()));
    }

    @Override
    public void markRetryFailed(Long id, String failReason) {
        getRetry(id);
        retryMapper.updateById(new AigcTaskRetryDO().setId(id)
                .setRetryStatus(AigcTaskRetryStatusEnum.FAILED.getCode())
                .setFailReason(failReason)
                .setFinishTime(LocalDateTime.now()));
    }

}
