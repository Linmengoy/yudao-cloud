package cn.iocoder.yudao.module.aigc.billing.service.wallet;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.MANUAL_ADJUST;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.TASK_REFUND;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_GIFT;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.WALLET_AMOUNT_INVALID;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.WALLET_BALANCE_NOT_ENOUGH;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.WALLET_NOT_EXISTS;

@Service
@Validated
public class AigcWalletServiceImpl implements AigcWalletService {

    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcBillingRecordService billingRecordService;
    @Resource
    private AigcBillingNoGenerator billingNoGenerator;

    @Override
    public AigcWalletDO getOrCreateWallet(Long userId) {
        AigcWalletDO wallet = walletMapper.selectByUserId(userId);
        if (wallet != null) {
            return wallet;
        }
        wallet = new AigcWalletDO();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalRecharge(BigDecimal.ZERO);
        wallet.setTotalGift(BigDecimal.ZERO);
        wallet.setTotalConsume(BigDecimal.ZERO);
        wallet.setTotalRefund(BigDecimal.ZERO);
        wallet.setStatus(CommonStatusEnum.ENABLE.getStatus());
        try {
            walletMapper.insert(wallet);
        } catch (DuplicateKeyException ex) {
            return walletMapper.selectByUserId(userId);
        }
        return walletMapper.selectById(wallet.getId());
    }

    @Override
    public AigcWalletDO getWallet(Long userId) {
        AigcWalletDO wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            throw exception(WALLET_NOT_EXISTS);
        }
        return wallet;
    }

    @Override
    public AigcWalletDO validateWalletExists(Long walletId) {
        AigcWalletDO wallet = walletMapper.selectById(walletId);
        if (wallet == null) {
            throw exception(WALLET_NOT_EXISTS);
        }
        return wallet;
    }

    @Override
    public PageResult<AigcWalletDO> getWalletPage(PageParam reqVO) {
        return walletMapper.selectPage(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargeWithRecord(Long userId, BigDecimal amount, String title) {
        AigcWalletDO wallet = getOrCreateWallet(userId);
        recharge(wallet.getId(), amount);
        createWalletRecord(wallet.getId(), userId, WALLET_RECHARGE.getCode(), AigcBillingRecordTypeEnum.RECHARGE.getCode(), amount, title);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void giftWithRecord(Long userId, BigDecimal amount, String title) {
        AigcWalletDO wallet = getOrCreateWallet(userId);
        gift(wallet.getId(), amount);
        createWalletRecord(wallet.getId(), userId, WALLET_GIFT.getCode(), AigcBillingRecordTypeEnum.GIFT.getCode(), amount, title);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustWithRecord(Long userId, BigDecimal amount, String title) {
        AigcWalletDO wallet = getOrCreateWallet(userId);
        adjust(wallet.getId(), amount);
        String recordType = amount.signum() > 0 ? AigcBillingRecordTypeEnum.ADJUST_INCREASE.getCode() : AigcBillingRecordTypeEnum.ADJUST_DECREASE.getCode();
        createWalletRecord(wallet.getId(), userId, MANUAL_ADJUST.getCode(), recordType, amount, title);
    }

    @Override
    public void recharge(Long walletId, BigDecimal amount) {
        validateAmount(amount);
        if (walletMapper.recharge(walletId, amount) == 0) {
            throw exception(WALLET_NOT_EXISTS);
        }
    }

    @Override
    public void gift(Long walletId, BigDecimal amount) {
        validateAmount(amount);
        if (walletMapper.gift(walletId, amount) == 0) {
            throw exception(WALLET_NOT_EXISTS);
        }
    }

    @Override
    public void adjust(Long walletId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(WALLET_AMOUNT_INVALID);
        }
        if (walletMapper.adjust(walletId, amount) == 0) {
            throw exception(amount.signum() < 0 ? WALLET_BALANCE_NOT_ENOUGH : WALLET_NOT_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundWithRecord(Long userId, BigDecimal amount, String title, String bizType, String bizId) {
        AigcWalletDO wallet = getOrCreateWallet(userId);
        refund(wallet.getId(), amount);
        createWalletRecordWithBizId(wallet.getId(), userId, bizType != null ? bizType : TASK_REFUND.getCode(), 
                AigcBillingRecordTypeEnum.REFUND.getCode(), amount, title, bizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void compensateWithRecord(Long userId, BigDecimal amount, String title, String bizType, String bizId) {
        AigcWalletDO wallet = getOrCreateWallet(userId);
        compensate(wallet.getId(), amount);
        createWalletRecordWithBizId(wallet.getId(), userId, bizType != null ? bizType : MANUAL_ADJUST.getCode(), 
                AigcBillingRecordTypeEnum.COMPENSATE.getCode(), amount, title, bizId);
    }

    @Override
    public void refund(Long walletId, BigDecimal amount) {
        validateAmount(amount);
        if (walletMapper.refund(walletId, amount) == 0) {
            throw exception(WALLET_NOT_EXISTS);
        }
    }

    @Override
    public void compensate(Long walletId, BigDecimal amount) {
        validateAmount(amount);
        if (walletMapper.compensate(walletId, amount) == 0) {
            throw exception(WALLET_NOT_EXISTS);
        }
    }

    private void createWalletRecord(Long walletId, Long userId, String bizType, String recordType, BigDecimal amount, String title) {
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(walletId);
        record.setUserId(userId);
        record.setBizType(bizType);
        record.setBizId(billingNoGenerator.generateBillingRecordNo());
        record.setRecordType(recordType);
        record.setTitle(title);
        record.setAmount(amount);
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

    private void createWalletRecordWithBizId(Long walletId, Long userId, String bizType, String recordType, 
            BigDecimal amount, String title, String bizId) {
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(walletId);
        record.setUserId(userId);
        record.setBizType(bizType);
        record.setBizId(bizId != null ? bizId : billingNoGenerator.generateBillingRecordNo());
        record.setRecordType(recordType);
        record.setTitle(title);
        record.setAmount(amount);
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(WALLET_AMOUNT_INVALID);
        }
    }

}
