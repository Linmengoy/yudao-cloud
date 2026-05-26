package cn.iocoder.yudao.module.aigc.task.service.retry;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskRetryDO;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskMapper;
import cn.iocoder.yudao.module.aigc.task.dal.mysql.AigcTaskRetryMapper;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRetryCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskRetryStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogServiceImpl;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.aigc.task.enums.ErrorCodeConstants.TASK_RETRY_EXCEED_LIMIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import({AigcTaskRetryServiceImpl.class, AigcTaskServiceImpl.class, AigcTaskLogServiceImpl.class})
public class AigcTaskRetryServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcTaskRetryService retryService;
    @Resource
    private AigcTaskRetryMapper retryMapper;
    @Resource
    private AigcTaskService taskService;
    @Resource
    private AigcTaskMapper taskMapper;

    @Test
    public void testCreateRetryRecord_success() {
        Long taskId = createTask();

        Long retryId = retryService.createRetryRecord(new AigcTaskRetryCreateReqDTO().setTaskId(taskId).setNextRetryTime(LocalDateTime.now()));

        AigcTaskRetryDO retry = retryMapper.selectById(retryId);
        assertEquals(AigcTaskRetryStatusEnum.WAITING.getCode(), retry.getRetryStatus());
        assertEquals(1, taskMapper.selectById(taskId).getRetryCount());
    }

    @Test
    public void testCreateRetryRecord_exceedLimit() {
        Long taskId = createTask();
        taskMapper.updateById(taskMapper.selectById(taskId).setRetryCount(3).setMaxRetryCount(3));

        assertServiceException(() -> retryService.createRetryRecord(new AigcTaskRetryCreateReqDTO().setTaskId(taskId)), TASK_RETRY_EXCEED_LIMIT);
    }

    @Test
    public void testScanAndMarkRetry() {
        Long retryId = retryService.createRetryRecord(new AigcTaskRetryCreateReqDTO().setTaskId(createTask()).setNextRetryTime(LocalDateTime.now().minusSeconds(1)));

        assertEquals(1, retryService.scanWaitingRetries().size());
        retryService.markRetryRunning(retryId);
        retryService.markRetryFailed(retryId, "failed");
        assertEquals(AigcTaskRetryStatusEnum.FAILED.getCode(), retryMapper.selectById(retryId).getRetryStatus());
        retryService.markRetrySuccess(retryId);
        assertEquals(AigcTaskRetryStatusEnum.SUCCESS.getCode(), retryMapper.selectById(retryId).getRetryStatus());
    }

    private Long createTask() {
        return taskService.createTask(new AigcTaskCreateReqDTO()
                .setClientRequestId("REQ-RT" + System.nanoTime())
                .setUserId(100L)
                .setTaskType("TEXT_GENERATE")
                .setCapability("TEXT_GENERATE")
                .setModelId(200L));
    }
}
