package cn.iocoder.yudao.module.aigc.billing.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.aigc.billing.service.job.AigcBillingJobService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AigcBillingFreezeTimeoutJob {

    @Resource
    private AigcBillingJobService billingJobService;

    @XxlJob("aigcBillingFreezeTimeoutJob")
    @TenantJob
    public String execute(String param) {
        int count = billingJobService.handleFreezeTimeout();
        log.info("[execute][处理冻结超时 ({}) 个]", count);
        return StrUtil.format("处理冻结超时 ({}) 个", count);
    }

}
