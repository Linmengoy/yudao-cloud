package cn.iocoder.yudao.module.aigc.task.service.task;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskDurationStatisticsRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.aigc.task.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import({AigcTaskServiceImpl.class, AigcTaskLogServiceImpl.class})
public class AigcTaskServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcTaskService taskService;
    @Resource
    private AigcTaskMapper taskMapper;

    @Test
    public void testCreateTask_success() {
        AigcTaskCreateReqDTO reqDTO = createReqDTO();

        Long taskId = taskService.createTask(reqDTO);

        AigcTaskDO task = taskMapper.selectById(taskId);
        assertNotNull(task);
        assertNotNull(task.getTaskNo());
        assertEquals(AigcTaskStatusEnum.CREATED.getCode(), task.getStatus());
        assertEquals(reqDTO.getUserId(), task.getUserId());
        assertEquals(60000L, task.getEstimatedDurationMillis());
    }

    @Test
    public void testCreateTask_estimatedDurationFallbackToAverage() {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.insert(createTaskDO("TASK-SUCCESS-1", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusSeconds(20), now, null));
        taskMapper.insert(createTaskDO("TASK-SUCCESS-2", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusSeconds(40), now, null));

        Long taskId = taskService.createTask(createReqDTO().setClientRequestId("REQ-AVG").setEstimatedDurationMillis(null));

        AigcTaskDO task = taskMapper.selectById(taskId);
        assertEquals(30000L, task.getEstimatedDurationMillis());
    }

    @Test
    public void testCreateTask_estimatedDurationFallbackToDefault() {
        Long taskId = taskService.createTask(createReqDTO().setClientRequestId("REQ-DEFAULT").setEstimatedDurationMillis(null));

        AigcTaskDO task = taskMapper.selectById(taskId);
        assertEquals(60000L, task.getEstimatedDurationMillis());
    }

    @Test
    public void testCancelTask_submitted() {
        Long taskId = taskService.createTask(createReqDTO().setFreezeId(1L));
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.RUNNING.getCode());
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.SUBMITTED.getCode());

        assertServiceException(() -> taskService.cancelTask(taskId), TASK_CANCEL_NOT_ALLOWED);
    }

    @Test
    public void testCreateTask_duplicateTaskNo() {
        taskService.createTask(createReqDTO().setTaskNo("TASK-DUP"));

        assertServiceException(() -> taskService.createTask(createReqDTO().setClientRequestId("REQ-2").setTaskNo("TASK-DUP")), TASK_NO_DUPLICATE);
    }

    @Test
    public void testGetUserTask_notOwner() {
        Long taskId = taskService.createTask(createReqDTO());

        assertServiceException(() -> taskService.getUserTask(taskId, 200L), TASK_NOT_OWNER);
    }

    @Test
    public void testUpdateTaskStatus_idempotent() {
        Long taskId = taskService.createTask(createReqDTO().setFreezeId(1L));
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());

        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());

        assertEquals(AigcTaskStatusEnum.QUEUED.getCode(), taskMapper.selectById(taskId).getStatus());
    }

    @Test
    public void testUpdateTaskStatus_successProgress() {
        Long taskId = taskService.createTask(createReqDTO().setFreezeId(1L));
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.RUNNING.getCode());

        taskService.updateTaskStatus(new AigcTaskStatusUpdateReqDTO().setTaskId(taskId).setOutputText("ok"), AigcTaskStatusEnum.SUCCESS.getCode());

        AigcTaskDO task = taskMapper.selectById(taskId);
        assertEquals(AigcTaskStatusEnum.SUCCESS.getCode(), task.getStatus());
        assertEquals(100, task.getProgress());
        assertNotNull(task.getFinishTime());
    }

    @Test
    public void testUpdateTaskStatus_defaultProgress() {
        Long taskId = taskService.createTask(createReqDTO().setFreezeId(1L));

        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());
        assertEquals(5, taskMapper.selectById(taskId).getProgress());

        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.RUNNING.getCode());
        assertEquals(10, taskMapper.selectById(taskId).getProgress());

        taskService.updateTaskStatus(new AigcTaskStatusUpdateReqDTO().setTaskId(taskId).setProgress(8), AigcTaskStatusEnum.SUBMITTED.getCode());
        assertEquals(20, taskMapper.selectById(taskId).getProgress());
    }

    @Test
    public void testUpdateTaskStatus_illegal() {
        Long taskId = taskService.createTask(createReqDTO());
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.FAILED.getCode());

        assertServiceException(() -> taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.SUCCESS.getCode()), TASK_STATUS_TRANSFER_INVALID);
    }

    @Test
    public void testCancelTask_success() {
        Long taskId = taskService.createTask(createReqDTO());

        taskService.cancelTask(taskId);

        assertEquals(AigcTaskStatusEnum.CANCELLED.getCode(), taskMapper.selectById(taskId).getStatus());
    }

    @Test
    public void testGetTaskStatistics() {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.insert(createTaskDO("TASK-SUCCESS", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusMinutes(3), now.minusMinutes(1), null));
        taskMapper.insert(createTaskDO("TASK-FAILED", AigcTaskStatusEnum.FAILED.getCode(), now.minusMinutes(2), now.minusMinutes(1), null));
        taskMapper.insert(createTaskDO("TASK-QUEUED", AigcTaskStatusEnum.QUEUED.getCode(), null, null, null).setRetryCount(1));
        taskMapper.insert(createTaskDO("TASK-TIMEOUT", AigcTaskStatusEnum.RUNNING.getCode(), null, null, now.minusMinutes(1)));
        taskMapper.insert(createTaskDO("TASK-REFUNDING", AigcTaskStatusEnum.REFUNDING.getCode(), null, null, null));

        AigcTaskStatisticsRespVO statistics = taskService.getTaskStatistics();

        assertEquals(5L, statistics.getTotalCount());
        assertEquals(1L, statistics.getSuccessCount());
        assertEquals(1L, statistics.getFailedCount());
        assertEquals(1L, statistics.getRefundingCount());
        assertEquals(2L, statistics.getBacklogCount());
        assertEquals(1L, statistics.getTimeoutCount());
        assertEquals(1L, statistics.getRetryTaskCount());
        assertEquals(0.5D, statistics.getSuccessRate());
        assertEquals(0.5D, statistics.getFailedRate());
        assertEquals(90000L, statistics.getAvgDurationMillis());
        assertEquals(120000L, statistics.getP95DurationMillis());
    }

    @Test
    public void testGetSuccessDurationStatistics() {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.insert(createTaskDO("TASK-SUCCESS-1", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusSeconds(10), now, null));
        taskMapper.insert(createTaskDO("TASK-SUCCESS-2", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusSeconds(20), now, null));
        taskMapper.insert(createTaskDO("TASK-SUCCESS-3", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusSeconds(30), now, null));
        taskMapper.insert(createTaskDO("TASK-FAILED-1", AigcTaskStatusEnum.FAILED.getCode(), now.minusSeconds(60), now, null));
        taskMapper.insert(createTaskDO("TASK-OTHER-PROVIDER", AigcTaskStatusEnum.SUCCESS.getCode(), now.minusSeconds(120), now, null).setProviderId(301L));

        AigcTaskDurationStatisticsRespDTO statistics = taskService.getSuccessDurationStatistics(new AigcTaskDurationStatisticsReqDTO()
                .setProviderId(300L)
                .setModelId(200L)
                .setCapability("TEXT_GENERATE")
                .setSampleSize(50));

        assertEquals(3L, statistics.getSampleCount());
        assertEquals(20000L, statistics.getAvgDurationMillis());
        assertEquals(30000L, statistics.getP95DurationMillis());
    }

    private AigcTaskCreateReqDTO createReqDTO() {
        return new AigcTaskCreateReqDTO()
                .setClientRequestId("REQ-1")
                .setUserId(100L)
                .setTaskType("TEXT_GENERATE")
                .setCapability("TEXT_GENERATE")
                .setModelId(200L)
                .setProviderId(300L)
                .setEstimatedDurationMillis(60000L)
                .setCurrencyType("POINT");
    }

    private AigcTaskDO createTaskDO(String taskNo, String status, LocalDateTime submitTime, LocalDateTime finishTime, LocalDateTime expireTime) {
        return new AigcTaskDO()
                .setTaskNo(taskNo)
                .setUserId(100L)
                .setTaskType("TEXT_GENERATE")
                .setCapability("TEXT_GENERATE")
                .setModelId(200L)
                .setProviderId(300L)
                .setStatus(status)
                .setProgress(0)
                .setSubmitTime(submitTime)
                .setFinishTime(finishTime)
                .setExpireTime(expireTime)
                .setRetryCount(0)
                .setMaxRetryCount(3);
    }

}
