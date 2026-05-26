package cn.iocoder.yudao.module.aigc.task.service.callback;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskCallbackDO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskCallbackMapper;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogServiceImpl;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import({AigcTaskCallbackServiceImpl.class, AigcTaskServiceImpl.class, AigcTaskLogServiceImpl.class})
public class AigcTaskCallbackServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcTaskCallbackService callbackService;
    @Resource
    private AigcTaskCallbackMapper callbackMapper;
    @Resource
    private AigcTaskService taskService;
    @Resource
    private AigcTaskMapper taskMapper;

    @Test
    public void testCreateCallbackRecord_duplicate() {
        Long callbackId = callbackService.createCallbackRecord(createCallbackReqDTO());

        Long duplicateId = callbackService.createCallbackRecord(createCallbackReqDTO());

        assertEquals(callbackId, duplicateId);
    }

    @Test
    public void testProcessCallback_success() {
        Long taskId = createRunningTask();
        Long callbackId = callbackService.createCallbackRecord(createCallbackReqDTO().setTaskId(taskId).setTaskNo(taskMapper.selectById(taskId).getTaskNo()));

        callbackService.processCallback(callbackId, new AigcTaskStatusUpdateReqDTO().setTaskId(taskId).setOutputText("ok"));

        AigcTaskDO task = taskMapper.selectById(taskId);
        AigcTaskCallbackDO callback = callbackMapper.selectById(callbackId);
        assertEquals(AigcTaskStatusEnum.SUCCESS.getCode(), task.getStatus());
        assertEquals("PROCESSED", callback.getCallbackStatus());
        assertNotNull(callback.getProcessedTime());
    }

    @Test
    public void testMarkProcessFailed() {
        Long callbackId = callbackService.createCallbackRecord(createCallbackReqDTO());

        callbackService.markProcessFailed(callbackId, "failed");

        AigcTaskCallbackDO callback = callbackMapper.selectById(callbackId);
        assertEquals("FAILED", callback.getCallbackStatus());
        assertEquals("failed", callback.getFailReason());
    }

    @Test
    public void testReplayCallback() {
        Long callbackId = callbackService.createCallbackRecord(createCallbackReqDTO());
        callbackService.markProcessFailed(callbackId, "failed");

        callbackService.replayCallback(callbackId);

        assertEquals("RECEIVED", callbackMapper.selectById(callbackId).getCallbackStatus());
    }

    private Long createRunningTask() {
        Long taskId = taskService.createTask(new AigcTaskCreateReqDTO()
                .setClientRequestId("REQ-CB")
                .setUserId(100L)
                .setTaskType("TEXT_GENERATE")
                .setCapability("TEXT_GENERATE")
                .setModelId(200L)
                .setFreezeId(1L));
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.QUEUED.getCode());
        taskService.updateTaskStatus(taskId, AigcTaskStatusEnum.RUNNING.getCode());
        return taskId;
    }

    private AigcTaskCallbackCreateReqDTO createCallbackReqDTO() {
        return new AigcTaskCallbackCreateReqDTO()
                .setProviderCode("openai")
                .setExternalTaskId("EXT-1")
                .setCallbackType("PROVIDER_TASK_SUCCESS")
                .setRawBody("{}");
    }
}
