package cn.iocoder.yudao.module.aigc.task.service.compensate;

import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskFailReasonEnum;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Validated
public class AigcTaskCompensateServiceImpl implements AigcTaskCompensateService {

    @Resource
    private AigcTaskMapper taskMapper;
    @Resource
    private AigcTaskService taskService;
    @Resource
    private AigcBillingApi billingApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int compensateTimeoutTasks() {
        List<AigcTaskDO> tasks = taskMapper.selectTimeoutTasks(Set.of(AigcTaskStatusEnum.RUNNING.getCode(), AigcTaskStatusEnum.SUBMITTED.getCode(), AigcTaskStatusEnum.CALLBACK_WAITING.getCode()), LocalDateTime.now());
        tasks.forEach(task -> {
            taskService.updateTaskStatus(new AigcTaskStatusUpdateReqDTO()
                    .setTaskId(task.getId())
                    .setFailCode(AigcTaskFailReasonEnum.TIMEOUT.getCode())
                    .setFailReason(AigcTaskFailReasonEnum.TIMEOUT.getName()), AigcTaskStatusEnum.FAILED.getCode());
            compensateFailedRefunding(task.getId());
        });
        return tasks.size();
    }

    @Override
    public void compensateFailedRefunding(Long taskId) {
        AigcTaskDO task = taskService.validateTaskExists(taskId);
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.REFUNDING.getCode());
        if (task.getFreezeId() != null) {
            billingApi.releaseFreeze(new AigcBillingReleaseReqDTO()
                    .setFreezeId(task.getFreezeId())
                    .setTaskId(task.getId())
                    .setTaskNo(task.getTaskNo())
                    .setReason(task.getFailReason() == null ? "任务失败释放冻结" : task.getFailReason()))
                    .getCheckedData();
        }
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.REFUNDED.getCode());
    }
}
