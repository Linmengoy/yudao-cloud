package cn.iocoder.yudao.module.aigc.billing.service.recharge;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcRechargeOrderMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.RECHARGE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.RECHARGE_ORDER_STATUS_INVALID;

@Service
@Validated
public class AigcRechargeOrderServiceImpl implements AigcRechargeOrderService {

    @Resource
    private AigcRechargeOrderMapper rechargeOrderMapper;
    @Resource
    private AigcBillingRecordMapper billingRecordMapper;
    @Resource
    private AigcWalletService walletService;
    @Resource
    private AigcBillingRecordService billingRecordService;
    @Resource
    private AigcBillingNoGenerator billingNoGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManualRecharge(Long userId, BigDecimal amount, String remark) {
        var wallet = walletService.getOrCreateWallet(userId);
        AigcRechargeOrderDO order = new AigcRechargeOrderDO();
        order.setRechargeNo(billingNoGenerator.generateRechargeNo());
        order.setWalletId(wallet.getId());
        order.setUserId(userId);
        order.setRechargeType("MANUAL");
        order.setPayAmount(0);
        order.setPointAmount(amount);
        order.setGiftAmount(BigDecimal.ZERO);
        order.setTotalPointAmount(amount);
        order.setStatus(AigcBillingRechargeStatusEnum.MANUAL_SUCCESS.getCode());
        order.setPayTime(LocalDateTime.now());
        order.setRemark(remark);
        rechargeOrderMapper.insert(order);
        walletService.recharge(wallet.getId(), amount);
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(wallet.getId());
        record.setUserId(userId);
        record.setBizType(WALLET_RECHARGE.getCode());
        record.setBizId(order.getRechargeNo());
        record.setRecordType(AigcBillingRecordTypeEnum.RECHARGE.getCode());
        record.setTitle("AIGC 手工充值");
        record.setAmount(amount);
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
        return order.getId();
    }

    @Override
    public Long createRechargeOrder(Long userId, BigDecimal amount, Integer payAmount, String remark) {
        var wallet = walletService.getOrCreateWallet(userId);
        AigcRechargeOrderDO order = new AigcRechargeOrderDO();
        order.setRechargeNo(billingNoGenerator.generateRechargeNo());
        order.setWalletId(wallet.getId());
        order.setUserId(userId);
        order.setRechargeType("PAY");
        order.setPayAmount(payAmount == null ? 0 : payAmount);
        order.setPointAmount(amount);
        order.setGiftAmount(BigDecimal.ZERO);
        order.setTotalPointAmount(amount);
        order.setStatus(AigcBillingRechargeStatusEnum.WAIT_PAY.getCode());
        order.setRemark(remark);
        rechargeOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    public AigcRechargeOrderDO getRechargeOrder(Long id) {
        AigcRechargeOrderDO order = rechargeOrderMapper.selectById(id);
        if (order == null) {
            throw exception(RECHARGE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    public AigcRechargeOrderDO getUserRechargeOrder(Long id, Long userId) {
        AigcRechargeOrderDO order = getRechargeOrder(id);
        if (!order.getUserId().equals(userId)) {
            throw exception(RECHARGE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyRechargePaid(AigcRechargeNotifyReqDTO reqDTO) {
        AigcRechargeOrderDO order = rechargeOrderMapper.selectByRechargeNo(reqDTO.getRechargeNo());
        if (order == null) {
            throw exception(RECHARGE_ORDER_NOT_EXISTS);
        }
        
        if (AigcBillingRechargeStatusEnum.PAID.getCode().equals(order.getStatus())) {
            compensateRechargeIfNeeded(order);
            return;
        }
        
        if (!AigcBillingRechargeStatusEnum.WAIT_PAY.getCode().equals(order.getStatus())) {
            throw exception(RECHARGE_ORDER_STATUS_INVALID);
        }
        
        if (rechargeOrderMapper.updatePaid(order.getId(), reqDTO.getPayOrderId(), reqDTO.getPayOrderNo(), reqDTO.getPayChannelCode(), reqDTO.getPayTime()) == 0) {
            order = rechargeOrderMapper.selectById(order.getId());
            if (AigcBillingRechargeStatusEnum.PAID.getCode().equals(order.getStatus())) {
                compensateRechargeIfNeeded(order);
                return;
            }
            throw exception(RECHARGE_ORDER_STATUS_INVALID);
        }
        
        walletService.recharge(order.getWalletId(), order.getTotalPointAmount());
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(order.getWalletId());
        record.setUserId(order.getUserId());
        record.setBizType(WALLET_RECHARGE.getCode());
        record.setBizId(order.getRechargeNo());
        record.setRecordType(AigcBillingRecordTypeEnum.RECHARGE.getCode());
        record.setTitle("AIGC 钱包充值");
        record.setAmount(order.getTotalPointAmount());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }
    
    private void compensateRechargeIfNeeded(AigcRechargeOrderDO order) {
        AigcBillingRecordDO existingRecord = billingRecordMapper.selectByBiz(WALLET_RECHARGE.getCode(), order.getRechargeNo());
        if (existingRecord != null) {
            return;
        }
        
        AigcWalletDO wallet = walletService.getWallet(order.getUserId());
        BigDecimal expectedBalance = order.getTotalPointAmount();
        
        walletService.recharge(order.getWalletId(), order.getTotalPointAmount());
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(order.getWalletId());
        record.setUserId(order.getUserId());
        record.setBizType(WALLET_RECHARGE.getCode());
        record.setBizId(order.getRechargeNo());
        record.setRecordType(AigcBillingRecordTypeEnum.RECHARGE.getCode());
        record.setTitle("AIGC 钱包充值-补偿入账");
        record.setAmount(order.getTotalPointAmount());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

    @Override
    public void closeRechargeOrder(Long id) {
        AigcRechargeOrderDO order = getRechargeOrder(id);
        if (!AigcBillingRechargeStatusEnum.WAIT_PAY.getCode().equals(order.getStatus())) {
            throw exception(RECHARGE_ORDER_STATUS_INVALID);
        }
        order.setStatus(AigcBillingRechargeStatusEnum.CLOSED.getCode());
        order.setCloseTime(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);
    }

    @Override
    public PageResult<AigcRechargeOrderDO> getRechargeOrderPage(PageParam reqVO) {
        return rechargeOrderMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcRechargeOrderDO> getUserRechargeOrderPage(PageParam reqVO, Long userId) {
        return rechargeOrderMapper.selectUserPage(reqVO, userId);
    }

}
