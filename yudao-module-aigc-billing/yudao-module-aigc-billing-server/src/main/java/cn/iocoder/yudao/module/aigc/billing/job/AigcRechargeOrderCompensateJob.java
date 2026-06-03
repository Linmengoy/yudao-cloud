package cn.iocoder.yudao.module.aigc.billing.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.aigc.billing.service.recharge.AigcRechargeOrderService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AigcRechargeOrderCompensateJob {

    @Resource
    private AigcRechargeOrderService rechargeOrderService;

    @XxlJob("aigcRechargeOrderCompensateJob")
    @TenantJob
    public String execute(String param) {
        int count = rechargeOrderService.compensateRechargeOrders(100);
        log.info("[execute][补偿充值入账 ({}) 个]", count);
        return StrUtil.format("补偿充值入账 ({}) 个", count);
    }

}
