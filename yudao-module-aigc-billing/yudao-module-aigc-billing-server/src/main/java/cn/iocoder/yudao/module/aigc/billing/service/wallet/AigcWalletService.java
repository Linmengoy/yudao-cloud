package cn.iocoder.yudao.module.aigc.billing.service.wallet;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;

import java.math.BigDecimal;

public interface AigcWalletService {

    AigcWalletDO getOrCreateWallet(Long userId);

    AigcWalletDO getWallet(Long userId);

    AigcWalletDO validateWalletExists(Long walletId);

    PageResult<AigcWalletDO> getWalletPage(PageParam reqVO);

    void rechargeWithRecord(Long userId, BigDecimal amount, String title);

    void giftWithRecord(Long userId, BigDecimal amount, String title);

    void adjustWithRecord(Long userId, BigDecimal amount, String title);

    void recharge(Long walletId, BigDecimal amount);

    void gift(Long walletId, BigDecimal amount);

    void adjust(Long walletId, BigDecimal amount);

    void refundWithRecord(Long userId, BigDecimal amount, String title, String bizType, String bizId);

    void compensateWithRecord(Long userId, BigDecimal amount, String title, String bizType, String bizId);

    void refund(Long walletId, BigDecimal amount);

    void compensate(Long walletId, BigDecimal amount);

}
