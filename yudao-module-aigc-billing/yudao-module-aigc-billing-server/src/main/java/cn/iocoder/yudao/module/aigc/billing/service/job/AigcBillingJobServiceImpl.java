package cn.iocoder.yudao.module.aigc.billing.service.job;

import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcQuotaFreezeMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcRechargeOrderMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.service.freeze.AigcQuotaFreezeService;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.service.freeze.AigcQuotaFreezeServiceImpl.buildRecordBizId;

@Service
@Slf4j
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
    @Resource
    private AigcQuotaFreezeService quotaFreezeService;

    @Override
    public int handleFreezeTimeout() {
        List<AigcQuotaFreezeDO> timeouts = quotaFreezeMapper.selectTimeoutFrozenList(LocalDateTime.now(), 100);
        int count = 0;
        int failCount = 0;
        for (AigcQuotaFreezeDO freeze : timeouts) {
            try {
                compensateTimeoutFreeze(freeze);
                count++;
            } catch (Exception e) {
                failCount++;
                log.error("[handleFreezeTimeout][冻结超时补偿失败，freezeId({}) freezeNo({}) userId({}) bizType({}) bizId({}) amount({})]",
                        freeze.getId(), freeze.getFreezeNo(), freeze.getUserId(), freeze.getBizType(), freeze.getBizId(), freeze.getAmount(), e);
            }
        }
        log.info("[handleFreezeTimeout][冻结超时补偿完成，待处理({}) 成功({}) 失败({})]", timeouts.size(), count, failCount);
        return count;
    }

    @Override
    public int reconcile() {
        int rechargeCount = reconcileRechargeOrders();
        int consumeCount = reconcileConfirmedFreezes();
        int count = rechargeCount + consumeCount;
        log.info("[reconcile][计费对账完成，充值补偿({}) 消费流水补偿({}) 总数({})]", rechargeCount, consumeCount, count);
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void compensateTimeoutFreeze(AigcQuotaFreezeDO freeze) {
        AigcBillingReleaseReqDTO reqDTO = new AigcBillingReleaseReqDTO();
        reqDTO.setFreezeId(freeze.getId());
        reqDTO.setTaskId(freeze.getTaskId());
        reqDTO.setTaskNo(freeze.getTaskNo());
        reqDTO.setReason("冻结超时自动释放");
        quotaFreezeService.releaseFreeze(reqDTO);
    }

    private int reconcileRechargeOrders() {
        int count = 0;
        int failCount = 0;
        List<AigcRechargeOrderDO> orders = rechargeOrderMapper.selectList(new LambdaQueryWrapperX<AigcRechargeOrderDO>()
                .eq(AigcRechargeOrderDO::getStatus, AigcBillingRechargeStatusEnum.PAID.getCode())
                        .isNotNull(AigcRechargeOrderDO::getPayTime)
                        .last("LIMIT 100"));
        
        for (AigcRechargeOrderDO order : orders) {
            AigcBillingRecordDO record = billingRecordMapper.selectByBiz(WALLET_RECHARGE.getCode(), order.getRechargeNo());
            if (record == null) {
                try {
                    compensateRechargeOrder(order);
                    count++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[reconcileRechargeOrders][充值补偿失败，rechargeOrderId({}) rechargeNo({}) userId({}) walletId({}) payOrderId({}) amount({})]",
                            order.getId(), order.getRechargeNo(), order.getUserId(), order.getWalletId(), order.getPayOrderId(), order.getTotalPointAmount(), e);
                }
            }
        }
        log.info("[reconcileRechargeOrders][充值对账完成，扫描({}) 成功补偿({}) 失败({})]", orders.size(), count, failCount);
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
        int failCount = 0;
        List<AigcQuotaFreezeDO> freezes = quotaFreezeMapper.selectConfirmedWithoutRecord(100L);
        
        for (AigcQuotaFreezeDO freeze : freezes) {
            AigcBillingRecordDO record = billingRecordMapper.selectByBiz(freeze.getBizType(),
                    buildRecordBizId(freeze.getBizId(), AigcBillingRecordTypeEnum.CONSUME.getCode()));
            if (record == null) {
                try {
                    compensateConsumeRecord(freeze);
                    count++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[reconcileConfirmedFreezes][消费流水补偿失败，freezeId({}) freezeNo({}) userId({}) bizType({}) bizId({}) confirmedAmount({})]",
                            freeze.getId(), freeze.getFreezeNo(), freeze.getUserId(), freeze.getBizType(), freeze.getBizId(), freeze.getConfirmedAmount(), e);
                }
            }
        }
        log.info("[reconcileConfirmedFreezes][消费流水对账完成，扫描({}) 成功补偿({}) 失败({})]", freezes.size(), count, failCount);
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void compensateConsumeRecord(AigcQuotaFreezeDO freeze) {
        AigcBillingRecordDO existingRecord = billingRecordMapper.selectByBiz(freeze.getBizType(),
                buildRecordBizId(freeze.getBizId(), AigcBillingRecordTypeEnum.CONSUME.getCode()));
        if (existingRecord != null) {
            return;
        }
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(freeze.getWalletId());
        record.setUserId(freeze.getUserId());
        record.setBizType(freeze.getBizType());
        record.setBizId(buildRecordBizId(freeze.getBizId(), AigcBillingRecordTypeEnum.CONSUME.getCode()));
        record.setRecordType(AigcBillingRecordTypeEnum.CONSUME.getCode());
        record.setTitle("AIGC 任务扣费-对账补偿");
        record.setAmount(freeze.getConfirmedAmount().negate());
        record.setFreezeId(freeze.getId());
        record.setTaskId(freeze.getTaskId());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

}
