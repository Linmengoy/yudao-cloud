package cn.iocoder.yudao.module.aigc.task.api;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsRespDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.callback.AigcTaskCallbackService;
import cn.iocoder.yudao.module.aigc.task.service.retry.AigcTaskRetryService;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AigcTaskApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AigcTaskApiImpl api;

    @Mock
    private AigcTaskService taskService;
    @Mock
    private AigcTaskCallbackService callbackService;
    @Mock
    private AigcTaskRetryService retryService;

    @Test
    public void testCreateTask_success() {
        AigcTaskCreateReqDTO reqDTO = new AigcTaskCreateReqDTO();
        when(taskService.createTask(reqDTO)).thenReturn(1L);

        assertEquals(1L, api.createTask(reqDTO).getData());
    }

    @Test
    public void testMarkQueued_success() {
        api.markQueued(1L);

        verify(taskService).updateTaskStatus(eq(1L), eq(AigcTaskStatusEnum.QUEUED.getCode()));
    }

    @Test
    public void testGetTask_success() {
        when(taskService.getTaskWithResult(1L)).thenReturn(new AigcTaskDO().setId(1L).setTaskNo("TASK1"));

        assertEquals("TASK1", api.getTask(1L).getData().getTaskNo());
    }

    @Test
    public void testGetSuccessDurationStatistics_success() {
        AigcTaskDurationStatisticsReqDTO reqDTO = new AigcTaskDurationStatisticsReqDTO()
                .setProviderId(10L)
                .setModelId(20L)
                .setCapability("IMAGE_GENERATE");
        when(taskService.getSuccessDurationStatistics(reqDTO)).thenReturn(new AigcTaskDurationStatisticsRespDTO().setAvgDurationMillis(60000L));

        assertEquals(60000L, api.getSuccessDurationStatistics(reqDTO).getData().getAvgDurationMillis());
    }

}
