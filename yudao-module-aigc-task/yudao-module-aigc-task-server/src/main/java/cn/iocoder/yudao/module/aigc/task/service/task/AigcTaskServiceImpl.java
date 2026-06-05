package cn.iocoder.yudao.module.aigc.task.service.task;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskPageReqVO;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskResultDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskResultMapper;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskStatisticsAggregate;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.task.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcTaskServiceImpl implements AigcTaskService {

    private static final Map<String, Set<String>> STATUS_TRANSFER_MAP = Map.ofEntries(
            Map.entry(AigcTaskStatusEnum.CREATED.getCode(), Set.of(AigcTaskStatusEnum.PRICE_CALCULATED.getCode(), AigcTaskStatusEnum.FROZEN.getCode(), AigcTaskStatusEnum.FAILED.getCode(), AigcTaskStatusEnum.CANCELLED.getCode())),
            Map.entry(AigcTaskStatusEnum.PRICE_CALCULATED.getCode(), Set.of(AigcTaskStatusEnum.FROZEN.getCode(), AigcTaskStatusEnum.FAILED.getCode(), AigcTaskStatusEnum.CANCELLED.getCode())),
            Map.entry(AigcTaskStatusEnum.FROZEN.getCode(), Set.of(AigcTaskStatusEnum.QUEUED.getCode(), AigcTaskStatusEnum.FAILED.getCode(), AigcTaskStatusEnum.CANCELLED.getCode())),
            Map.entry(AigcTaskStatusEnum.QUEUED.getCode(), Set.of(AigcTaskStatusEnum.RUNNING.getCode(), AigcTaskStatusEnum.FAILED.getCode(), AigcTaskStatusEnum.CANCELLED.getCode())),
            Map.entry(AigcTaskStatusEnum.RUNNING.getCode(), Set.of(AigcTaskStatusEnum.SUBMITTED.getCode(), AigcTaskStatusEnum.CALLBACK_WAITING.getCode(), AigcTaskStatusEnum.DOWNLOADING.getCode(), AigcTaskStatusEnum.SUCCESS.getCode(), AigcTaskStatusEnum.FAILED.getCode())),
            Map.entry(AigcTaskStatusEnum.SUBMITTED.getCode(), Set.of(AigcTaskStatusEnum.CALLBACK_WAITING.getCode(), AigcTaskStatusEnum.DOWNLOADING.getCode(), AigcTaskStatusEnum.SUCCESS.getCode(), AigcTaskStatusEnum.FAILED.getCode())),
            Map.entry(AigcTaskStatusEnum.CALLBACK_WAITING.getCode(), Set.of(AigcTaskStatusEnum.DOWNLOADING.getCode(), AigcTaskStatusEnum.SUCCESS.getCode(), AigcTaskStatusEnum.FAILED.getCode())),
            Map.entry(AigcTaskStatusEnum.DOWNLOADING.getCode(), Set.of(AigcTaskStatusEnum.ASSET_CREATING.getCode(), AigcTaskStatusEnum.FAILED.getCode())),
            Map.entry(AigcTaskStatusEnum.ASSET_CREATING.getCode(), Set.of(AigcTaskStatusEnum.AUDITING.getCode(), AigcTaskStatusEnum.SUCCESS.getCode(), AigcTaskStatusEnum.FAILED.getCode())),
            Map.entry(AigcTaskStatusEnum.AUDITING.getCode(), Set.of(AigcTaskStatusEnum.SUCCESS.getCode(), AigcTaskStatusEnum.FAILED.getCode())),
            Map.entry(AigcTaskStatusEnum.FAILED.getCode(), Set.of(AigcTaskStatusEnum.REFUNDING.getCode(), AigcTaskStatusEnum.REFUNDED.getCode())),
            Map.entry(AigcTaskStatusEnum.CANCELLED.getCode(), Set.of(AigcTaskStatusEnum.REFUNDING.getCode(), AigcTaskStatusEnum.REFUNDED.getCode())),
            Map.entry(AigcTaskStatusEnum.REFUNDING.getCode(), Set.of(AigcTaskStatusEnum.REFUNDED.getCode()))
    );

    @Resource
    private AigcTaskMapper taskMapper;
    @Resource
    private AigcTaskResultMapper taskResultMapper;
    @Resource
    private AigcTaskLogService taskLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(AigcTaskCreateReqDTO reqDTO) {
        if (reqDTO.getClientRequestId() != null) {
            AigcTaskDO exists = taskMapper.selectByClientRequestId(reqDTO.getUserId(), reqDTO.getClientRequestId());
            if (exists != null) {
                return exists.getId();
            }
        }
        if (reqDTO.getTaskNo() != null && taskMapper.selectByTaskNo(reqDTO.getTaskNo()) != null) {
            throw exception(TASK_NO_DUPLICATE);
        }
        AigcTaskDO task = BeanUtils.toBean(reqDTO, AigcTaskDO.class)
                .setTaskNo(reqDTO.getTaskNo() == null ? generateTaskNo() : reqDTO.getTaskNo())
                .setStatus(reqDTO.getFreezeId() == null ? AigcTaskStatusEnum.CREATED.getCode() : AigcTaskStatusEnum.FROZEN.getCode())
                .setProgress(0)
                .setSubmitTime(LocalDateTime.now())
                .setRetryCount(0)
                .setMaxRetryCount(3);
        taskMapper.insert(task);
        taskLogService.createTaskLog(task.getId(), task.getTaskNo(), null, task.getStatus(), "CREATE", "创建任务");
        return task.getId();
    }

    @Override
    public AigcTaskDO getTask(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public AigcTaskDO getTaskByTaskNo(String taskNo) {
        return taskMapper.selectByTaskNo(taskNo);
    }

    @Override
    public AigcTaskDO getTaskWithResult(Long id) {
        return fillResult(getTask(id));
    }

    @Override
    public AigcTaskDO getTaskByTaskNoWithResult(String taskNo) {
        return fillResult(getTaskByTaskNo(taskNo));
    }

    @Override
    public AigcTaskDO validateTaskExists(Long id) {
        AigcTaskDO task = taskMapper.selectById(id);
        if (task == null) {
            throw exception(TASK_NOT_EXISTS);
        }
        return task;
    }

    @Override
    public PageResult<AigcTaskDO> getTaskPage(AigcTaskPageReqVO reqVO) {
        return taskMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcTaskDO> getUserTaskPage(PageParam reqVO, Long userId) {
        return taskMapper.selectPageByUserId(reqVO, userId);
    }

    @Override
    public AigcTaskStatisticsRespVO getTaskStatistics() {
        AigcTaskStatisticsAggregate statistics = taskMapper.selectStatistics(
                AigcTaskStatusEnum.SUCCESS.getCode(),
                AigcTaskStatusEnum.FAILED.getCode(),
                AigcTaskStatusEnum.REFUNDING.getCode(),
                Set.of(AigcTaskStatusEnum.SUCCESS.getCode(), AigcTaskStatusEnum.FAILED.getCode(), AigcTaskStatusEnum.CANCELLED.getCode(), AigcTaskStatusEnum.REFUNDED.getCode()),
                Set.of(AigcTaskStatusEnum.QUEUED.getCode(), AigcTaskStatusEnum.RUNNING.getCode(), AigcTaskStatusEnum.CALLBACK_WAITING.getCode()),
                LocalDateTime.now());
        long successCount = nullToZero(statistics.getSuccessCount());
        long failedCount = nullToZero(statistics.getFailedCount());
        long finishedCount = nullToZero(statistics.getFinishedCount());
        return new AigcTaskStatisticsRespVO()
                .setTotalCount(nullToZero(statistics.getTotalCount()))
                .setSuccessCount(successCount)
                .setFailedCount(failedCount)
                .setRefundingCount(nullToZero(statistics.getRefundingCount()))
                .setBacklogCount(nullToZero(statistics.getBacklogCount()))
                .setTimeoutCount(nullToZero(statistics.getTimeoutCount()))
                .setSuccessRate(finishedCount == 0 ? 0D : (double) successCount / finishedCount)
                .setFailedRate(finishedCount == 0 ? 0D : (double) failedCount / finishedCount)
                .setAvgDurationMillis(nullToZero(statistics.getAvgDurationMillis()));
    }

    @Override
    public AigcTaskDO getUserTask(Long id, Long userId) {
        AigcTaskDO task = validateTaskExists(id);
        if (!task.getUserId().equals(userId)) {
            throw exception(TASK_NOT_OWNER);
        }
        return task;
    }

    @Override
    public AigcTaskDO getUserTaskWithResult(Long id, Long userId) {
        return fillResult(getUserTask(id, userId));
    }

    @Override
    public void updateTaskStatus(Long taskId, String toStatus) {
        updateTaskStatus(new AigcTaskStatusUpdateReqDTO().setTaskId(taskId), toStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatus(AigcTaskStatusUpdateReqDTO reqDTO, String toStatus) {
        AigcTaskDO task = validateTaskExists(reqDTO.getTaskId());
        if (Objects.equals(task.getStatus(), toStatus)) {
            return;
        }
        validateStatusTransfer(task.getStatus(), toStatus);
        AigcTaskDO updateObj = BeanUtils.toBean(reqDTO, AigcTaskDO.class)
                .setId(reqDTO.getTaskId())
                .setStatus(toStatus);
        if (AigcTaskStatusEnum.SUCCESS.getCode().equals(toStatus)) {
            updateObj.setProgress(100);
        }
        updateObj.setOutputSummary(buildOutputSummary(reqDTO.getOutputText()));
        if (AigcTaskStatusEnum.RUNNING.getCode().equals(toStatus)) {
            updateObj.setStartTime(LocalDateTime.now());
        }
        if (AigcTaskStatusEnum.CALLBACK_WAITING.getCode().equals(toStatus)) {
            updateObj.setCallbackTime(LocalDateTime.now());
        }
        if (AigcTaskStatusEnum.SUCCESS.getCode().equals(toStatus) || AigcTaskStatusEnum.FAILED.getCode().equals(toStatus)
                || AigcTaskStatusEnum.CANCELLED.getCode().equals(toStatus) || AigcTaskStatusEnum.REFUNDED.getCode().equals(toStatus)) {
            updateObj.setFinishTime(LocalDateTime.now());
        }
        if (taskMapper.updateByIdAndStatus(updateObj, task.getStatus()) == 0) {
            AigcTaskDO current = validateTaskExists(reqDTO.getTaskId());
            if (Objects.equals(current.getStatus(), toStatus)) {
                return;
            }
            throw exception(TASK_STATUS_TRANSFER_INVALID);
        }
        if (reqDTO.getOutputText() != null || reqDTO.getOutputData() != null) {
            taskResultMapper.saveByTaskId(reqDTO.getTaskId(), reqDTO.getOutputText(), reqDTO.getOutputData());
        }
        taskLogService.createTaskLog(task.getId(), task.getTaskNo(), task.getStatus(), toStatus, "STATUS_UPDATE", "任务状态变更");
    }

    @Override
    public void cancelTask(Long taskId) {
        AigcTaskDO task = validateTaskExists(taskId);
        validateCancelAllowed(task.getStatus());
        updateTaskStatus(taskId, AigcTaskStatusEnum.CANCELLED.getCode());
    }

    @Override
    public void cancelUserTask(Long taskId, Long userId) {
        getUserTask(taskId, userId);
        cancelTask(taskId);
    }

    @Override
    public void increaseRetryCount(Long taskId) {
        validateTaskExists(taskId);
        taskMapper.incrementRetryCount(taskId);
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private AigcTaskDO fillResult(AigcTaskDO task) {
        if (task == null) {
            return null;
        }
        AigcTaskResultDO result = taskResultMapper.selectByTaskId(task.getId());
        if (result == null) {
            return task;
        }
        return task.setOutputText(result.getOutputText())
                .setOutputData(result.getOutputData());
    }

    private String buildOutputSummary(String outputText) {
        if (outputText == null) {
            return null;
        }
        return outputText.length() <= 500 ? outputText : outputText.substring(0, 500);
    }

    private void validateStatusTransfer(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw exception(TASK_STATUS_INVALID);
        }
        if (!STATUS_TRANSFER_MAP.getOrDefault(fromStatus, Set.of()).contains(toStatus)) {
            throw exception(TASK_STATUS_TRANSFER_INVALID);
        }
    }

    private void validateCancelAllowed(String status) {
        if (!Set.of(AigcTaskStatusEnum.CREATED.getCode(), AigcTaskStatusEnum.PRICE_CALCULATED.getCode(), AigcTaskStatusEnum.FROZEN.getCode(), AigcTaskStatusEnum.QUEUED.getCode()).contains(status)) {
            throw exception(TASK_CANCEL_NOT_ALLOWED);
        }
    }

    private String generateTaskNo() {
        return "TASK" + IdUtil.getSnowflakeNextIdStr();
    }

}
