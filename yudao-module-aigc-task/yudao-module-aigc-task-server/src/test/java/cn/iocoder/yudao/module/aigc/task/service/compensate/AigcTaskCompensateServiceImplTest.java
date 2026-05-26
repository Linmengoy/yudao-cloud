package cn.iocoder.yudao.module.aigc.task.service.compensate;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogServiceImpl;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({AigcTaskCompensateServiceImpl.class, AigcTaskServiceImpl.class, AigcTaskLogServiceImpl.class})
public class AigcTaskCompensateServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcTaskCompensateService compensateService;
    @Resource
    private AigcTaskService taskService;
    @Resource
    private AigcTaskMapper taskMapper;
    @MockitoBean
    private AigcBillingApi billingApi;

    @Test
    public void testCompensateTimeoutTasks_running() {
        Long taskId = createRunningTask(LocalDateTime.now().minusMinutes(1));
        when(billingApi.releaseFreeze(any())).thenReturn(CommonResult.success(true));

        int count = compensateService.compensateTimeoutTasks();

        ArgumentCaptor<AigcBillingReleaseReqDTO> captor = ArgumentCaptor.forClass(AigcBillingReleaseReqDTO.class);
        verify(billingApi).releaseFreeze(captor.capture());
        assertEquals(1, count);
        assertEquals(1L, captor.getValue().getFreezeId());
        assertEquals(taskId, captor.getValue().getTaskId());
        assertEquals(AigcTaskStatusEnum.REFUNDED.getCode(), taskMapper.selectById(taskId).getStatus());
    }

    @Test
    public void testCompensateTimeoutTasks_notTimeout() {
        Long taskId = createRunningTask(LocalDateTime.now().plusMinutes(1));

        int count = compensateService.compensateTimeoutTasks();

        assertEquals(0, count);
        assertEquals(AigcTaskStatusEnum.RUNNING.getCode(), taskMapper.selectById(taskId).getStatus());
    }

    private Long createRunningTask(LocalDateTime expireTime) {
        Long taskId = taskService.createTask(new AigcTaskCreateReqDTO()
                .setClientRequestId("REQ-CP" + System.nanoTime())
                .setUserId(100L)
                .setTaskType("TEXT_GENERATE")
                .setCapability("TEXT_GENERATE")
                .setModelId(200L)
                .setFreezeId(1L));
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.RUNNING.getCode());
        taskMapper.updateById(taskMapper.selectById(taskId).setExpireTime(expireTime));
        return taskId;
    }
}
