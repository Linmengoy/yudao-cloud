package cn.iocoder.yudao.module.aigc.task.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.aigc.task.service.compensate.AigcTaskCompensateService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AigcTaskTimeoutJob {

    @Resource
    private AigcTaskCompensateService compensateService;

    @XxlJob("aigcTaskTimeoutJob")
    @TenantJob
    public String execute(String param) {
        int count = compensateService.compensateTimeoutTasks();
        return StrUtil.format("处理超时任务 ({}) 个", count);
    }
}
