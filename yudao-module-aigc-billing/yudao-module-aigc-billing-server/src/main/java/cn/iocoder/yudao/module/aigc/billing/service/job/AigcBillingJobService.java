package cn.iocoder.yudao.module.aigc.billing.service.job;

public interface AigcBillingJobService {

    int handleFreezeTimeout();

    int reconcile();

}
