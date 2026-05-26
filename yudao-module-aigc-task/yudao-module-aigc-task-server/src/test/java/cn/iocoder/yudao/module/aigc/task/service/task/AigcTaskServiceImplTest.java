package cn.iocoder.yudao.module.aigc.task.service.task;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

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

    private AigcTaskCreateReqDTO createReqDTO() {
        return new AigcTaskCreateReqDTO()
                .setClientRequestId("REQ-1")
                .setUserId(100L)
                .setTaskType("TEXT_GENERATE")
                .setCapability("TEXT_GENERATE")
                .setModelId(200L)
                .setProviderId(300L)
                .setCurrencyType("POINT");
    }

}
