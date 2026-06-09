package cn.iocoder.yudao.module.aigc.task.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRetryCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.callback.AigcTaskCallbackService;
import cn.iocoder.yudao.module.aigc.task.service.retry.AigcTaskRetryService;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcTaskApiImpl implements AigcTaskApi {

    @Resource
    private AigcTaskService taskService;
    @Resource
    private AigcTaskCallbackService callbackService;
    @Resource
    private AigcTaskRetryService retryService;

    @Override
    public CommonResult<Long> createTask(AigcTaskCreateReqDTO reqDTO) {
        return success(taskService.createTask(reqDTO));
    }

    @Override
    public CommonResult<AigcTaskRespDTO> getTask(Long taskId) {
        AigcTaskDO task = taskService.getTaskWithResult(taskId);
        return success(BeanUtils.toBean(task, AigcTaskRespDTO.class));
    }

    @Override
    public CommonResult<AigcTaskRespDTO> getTaskByTaskNo(String taskNo) {
        AigcTaskDO task = taskService.getTaskByTaskNoWithResult(taskNo);
        return success(BeanUtils.toBean(task, AigcTaskRespDTO.class));
    }

    @Override
    public CommonResult<AigcTaskDurationStatisticsRespDTO> getSuccessDurationStatistics(AigcTaskDurationStatisticsReqDTO reqDTO) {
        return success(taskService.getSuccessDurationStatistics(reqDTO));
    }

    @Override
    public CommonResult<Boolean> markQueued(Long taskId) {
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markRunning(Long taskId) {
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.RUNNING.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markSubmitted(AigcTaskStatusUpdateReqDTO reqDTO) {
        taskService.updateTaskStatus(reqDTO, AigcTaskStatusEnum.SUBMITTED.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markCallbackWaiting(AigcTaskStatusUpdateReqDTO reqDTO) {
        taskService.updateTaskStatus(reqDTO, AigcTaskStatusEnum.CALLBACK_WAITING.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markDownloading(Long taskId) {
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.DOWNLOADING.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markAssetCreating(Long taskId) {
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.ASSET_CREATING.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markSuccess(AigcTaskStatusUpdateReqDTO reqDTO) {
        taskService.updateTaskStatus(reqDTO, AigcTaskStatusEnum.SUCCESS.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markFailed(AigcTaskStatusUpdateReqDTO reqDTO) {
        taskService.updateTaskStatus(reqDTO, AigcTaskStatusEnum.FAILED.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markRefunding(Long taskId) {
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.REFUNDING.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Boolean> markRefunded(Long taskId) {
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.REFUNDED.getCode());
        return success(true);
    }

    @Override
    public CommonResult<Long> createCallbackRecord(AigcTaskCallbackCreateReqDTO reqDTO) {
        return success(callbackService.createCallbackRecord(reqDTO));
    }

    @Override
    public CommonResult<Long> createRetryRecord(AigcTaskRetryCreateReqDTO reqDTO) {
        return success(retryService.createRetryRecord(reqDTO));
    }

}
