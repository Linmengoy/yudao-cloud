package cn.iocoder.yudao.module.aigc.gen.job;

import cn.iocoder.yudao.module.aigc.gen.service.record.AigcGenerateRecordService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AigcGenerateSyncJob {

    @Resource
    private AigcGenerateRecordService generateRecordService;

    @XxlJob("aigcGenerateSyncJob")
    public void execute() {
        int count = generateRecordService.syncTimeoutTasks();
        log.info("[execute][同步 AIGC 生成任务数量({})]", count);
    }
}
