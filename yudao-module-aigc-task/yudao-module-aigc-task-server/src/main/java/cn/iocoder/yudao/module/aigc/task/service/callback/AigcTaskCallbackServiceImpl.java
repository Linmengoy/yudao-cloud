package cn.iocoder.yudao.module.aigc.task.service.callback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.controller.admin.callback.vo.AigcTaskCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskCallbackDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskCallbackMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.task.enums.ErrorCodeConstants.TASK_CALLBACK_NOT_EXISTS;

@Service
@Validated
public class AigcTaskCallbackServiceImpl implements AigcTaskCallbackService {

    @Resource
    private AigcTaskCallbackMapper callbackMapper;
    @Resource
    private AigcTaskService taskService;

    @Override
    public Long createCallbackRecord(AigcTaskCallbackCreateReqDTO reqDTO) {
        AigcTaskCallbackDO exists = callbackMapper.selectByExternalCallback(reqDTO.getProviderCode(), reqDTO.getExternalTaskId(), reqDTO.getCallbackType());
        if (exists != null) {
            return exists.getId();
        }
        AigcTaskCallbackDO callback = BeanUtils.toBean(reqDTO, AigcTaskCallbackDO.class)
                .setCallbackNo("CB" + cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr())
                .setCallbackStatus("RECEIVED")
                .setReceivedTime(LocalDateTime.now());
        callbackMapper.insert(callback);
        return callback.getId();
    }

    @Override
    public AigcTaskCallbackDO getCallback(Long id) {
        AigcTaskCallbackDO callback = callbackMapper.selectById(id);
        if (callback == null) {
            throw exception(TASK_CALLBACK_NOT_EXISTS);
        }
        return callback;
    }

    @Override
    public PageResult<AigcTaskCallbackDO> getCallbackPage(AigcTaskCallbackPageReqVO reqVO) {
        return callbackMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processCallback(Long id, AigcTaskStatusUpdateReqDTO reqDTO) {
        AigcTaskCallbackDO callback = getCallback(id);
        if ("PROCESSED".equals(callback.getCallbackStatus())) {
            return;
        }
        taskService.updateTaskStatus(reqDTO, resolveTargetStatus(callback.getCallbackType()));
        callbackMapper.updateById(new AigcTaskCallbackDO().setId(id)
                .setCallbackStatus("PROCESSED")
                .setProcessResult("SUCCESS")
                .setProcessedTime(LocalDateTime.now()));
    }

    @Override
    public void markProcessFailed(Long id, String failReason) {
        getCallback(id);
        callbackMapper.updateById(new AigcTaskCallbackDO().setId(id)
                .setCallbackStatus("FAILED")
                .setFailReason(failReason)
                .setProcessedTime(LocalDateTime.now()));
    }

    @Override
    public void replayCallback(Long id) {
        AigcTaskCallbackDO callback = getCallback(id);
        callbackMapper.updateById(new AigcTaskCallbackDO().setId(id)
                .setCallbackStatus("RECEIVED")
                .setFailReason(null)
                .setProcessResult(callback.getProcessResult())
                .setProcessedTime(null));
    }

    private String resolveTargetStatus(String callbackType) {
        if ("PROVIDER_TASK_FAILED".equals(callbackType)) {
            return AigcTaskStatusEnum.FAILED.getCode();
        }
        if ("PROVIDER_TASK_CANCELLED".equals(callbackType)) {
            return AigcTaskStatusEnum.CANCELLED.getCode();
        }
        return AigcTaskStatusEnum.SUCCESS.getCode();
    }

}
