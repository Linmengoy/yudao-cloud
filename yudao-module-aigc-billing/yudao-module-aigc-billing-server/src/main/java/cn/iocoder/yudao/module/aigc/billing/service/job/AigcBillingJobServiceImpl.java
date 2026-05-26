package cn.iocoder.yudao.module.aigc.billing.service.job;

import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcQuotaFreezeMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcRechargeOrderMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;

@Service
public class AigcBillingJobServiceImpl implements AigcBillingJobService {

    @Resource
    private AigcQuotaFreezeMapper quotaFreezeMapper;
    @Resource
    private AigcBillingRecordMapper billingRecordMapper;
    @Resource
    private AigcRechargeOrderMapper rechargeOrderMapper;
    @Resource
    private AigcWalletService walletService;
    @Resource
    private AigcBillingRecordService billingRecordService;

    @Override
    public int handleFreezeTimeout() {
        List<AigcQuotaFreezeDO> timeouts = quotaFreezeMapper.selectTimeoutFrozenList(LocalDateTime.now(), 100);
        int count = 0;
        for (AigcQuotaFreezeDO freeze : timeouts) {
            try {
                compensateTimeoutFreeze(freeze);
                count++;
            } catch (Exception e) {
            }
        }
        return count;
    }

    @Override
    public int reconcile() {
        int count = 0;
        count += reconcileRechargeOrders();
        count += reconcileConfirmedFreezes();
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void compensateTimeoutFreeze(AigcQuotaFreezeDO freeze) {
        AigcBillingRecordDO existingRecord = billingRecordMapper.selectByBiz(freeze.getBizType(), freeze.getBizId());
        if (existingRecord != null) {
            return;
        }
        
        walletService.recharge(freeze.getWalletId(), freeze.getAmount());
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(freeze.getWalletId());
        record.setUserId(freeze.getUserId());
        record.setBizType(freeze.getBizType());
        record.setBizId(freeze.getBizId());
        record.setRecordType(AigcBillingRecordTypeEnum.RELEASE.getCode());
        record.setTitle("AIGC 冻结超时释放-补偿");
        record.setAmount(freeze.getAmount());
        record.setFreezeId(freeze.getId());
        record.setTaskId(freeze.getTaskId());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

    private int reconcileRechargeOrders() {
        int count = 0;
        List<AigcRechargeOrderDO> orders = rechargeOrderMapper.selectList(wrapper ->
                wrapper.eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.PAID.getCode())
                        .isNotNull(AigcRechargeOrderDO::getPayTime)
                        .last("LIMIT 100"));
        
        for (AigcRechargeOrderDO order : orders) {
            AigcBillingRecordDO record = billingRecordMapper.selectByBiz(WALLET_RECHARGE.getCode(), order.getRechargeNo());
            if (record == null) {
                try {
                    compensateRechargeOrder(order);
                    count++;
                } catch (Exception e) {
                }
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void compensateRechargeOrder(AigcRechargeOrderDO order) {
        AigcBillingRecordDO existingRecord = billingRecordMapper.selectByBiz(WALLET_RECHARGE.getCode(), order.getRechargeNo());
        if (existingRecord != null) {
            return;
        }
        
        walletService.recharge(order.getWalletId(), order.getTotalPointAmount());
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(order.getWalletId());
        record.setUserId(order.getUserId());
        record.setBizType(WALLET_RECHARGE.getCode());
        record.setBizId(order.getRechargeNo());
        record.setRecordType(AigcBillingRecordTypeEnum.RECHARGE.getCode());
        record.setTitle("AIGC 钱包充值-对账补偿");
        record.setAmount(order.getTotalPointAmount());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

    private int reconcileConfirmedFreezes() {
        int count = 0;
        List<AigcQuotaFreezeDO> freezes = quotaFreezeMapper.selectConfirmedWithoutRecord(100L);
        
        for (AigcQuotaFreezeDO freeze : freezes) {
            AigcBillingRecordDO record = billingRecordMapper.selectByBiz(freeze.getBizType(), freeze.getBizId());
            if (record == null) {
                try {
                    compensateConsumeRecord(freeze);
                    count++;
                } catch (Exception e) {
                }
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void compensateConsumeRecord(AigcQuotaFreezeDO freeze) {
        AigcBillingRecordDO existingRecord = billingRecordMapper.selectByBiz(freeze.getBizType(), freeze.getBizId());
        if (existingRecord != null) {
            return;
        }
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(freeze.getWalletId());
        record.setUserId(freeze.getUserId());
        record.setBizType(freeze.getBizType());
        record.setBizId(freeze.getBizId());
        record.setRecordType(AigcBillingRecordTypeEnum.CONSUME.getCode());
        record.setTitle("AIGC 任务扣费-对账补偿");
        record.setAmount(freeze.getConfirmedAmount().negate());
        record.setFreezeId(freeze.getId());
        record.setTaskId(freeze.getTaskId());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

}
