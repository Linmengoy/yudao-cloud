package cn.iocoder.yudao.module.aigc.billing.service.wallet;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import({AigcWalletServiceImpl.class, AigcBillingRecordServiceImpl.class, AigcBillingNoGenerator.class})
public class AigcWalletServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcWalletService walletService;
    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcBillingRecordMapper billingRecordMapper;

    @Test
    public void testGetOrCreateWallet_success() {
        AigcWalletDO wallet = walletService.getOrCreateWallet(100L);

        assertNotNull(wallet.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getFrozenBalance()));
        assertEquals(wallet.getId(), walletService.getOrCreateWallet(100L).getId());
    }

    @Test
    public void testGiftWithRecord_success() {
        walletService.giftWithRecord(100L, new BigDecimal("10.000000"), "gift");

        AigcWalletDO wallet = walletMapper.selectByUserId(100L);
        assertEquals(0, new BigDecimal("10.000000").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(wallet.getTotalGift()));
        AigcBillingRecordDO record = billingRecordMapper.selectList().get(0);
        assertEquals(0, new BigDecimal("10.000000").compareTo(record.getBalanceAfter()));
        assertEquals(0, BigDecimal.ZERO.compareTo(record.getFrozenBalanceAfter()));
    }

}
