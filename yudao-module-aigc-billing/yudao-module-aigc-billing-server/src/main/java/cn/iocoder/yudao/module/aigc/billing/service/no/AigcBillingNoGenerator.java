package cn.iocoder.yudao.module.aigc.billing.service.no;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

@Component
public class AigcBillingNoGenerator {

    public String generateFreezeNo() {
        return "F" + IdUtil.getSnowflakeNextIdStr();
    }

    public String generateBillingRecordNo() {
        return "BR" + IdUtil.getSnowflakeNextIdStr();
    }

    public String generateCostRecordNo() {
        return "C" + IdUtil.getSnowflakeNextIdStr();
    }

    public String generateRechargeNo() {
        return "R" + IdUtil.getSnowflakeNextIdStr();
    }

}
