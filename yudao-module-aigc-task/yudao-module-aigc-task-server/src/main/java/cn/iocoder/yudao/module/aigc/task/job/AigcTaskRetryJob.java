package cn.iocoder.yudao.module.aigc.task.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.aigc.task.service.retry.AigcTaskRetryService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AigcTaskRetryJob {

    @Resource
    private AigcTaskRetryService retryService;

    @XxlJob("aigcTaskRetryJob")
    @TenantJob
    public String execute(String param) {
        int count = retryService.scanWaitingRetries().size();
        return StrUtil.format("扫描待重试任务 ({}) 个", count);
    }
}
