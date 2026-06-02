package cn.iocoder.yudao.module.aigc.billing.service.freeze;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcQuotaFreezeMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingFreezeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordServiceImpl;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import({AigcQuotaFreezeServiceImpl.class, AigcWalletServiceImpl.class, AigcBillingRecordServiceImpl.class, AigcBillingNoGenerator.class})
public class AigcQuotaFreezeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcQuotaFreezeService freezeService;
    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcQuotaFreezeMapper freezeMapper;
    @Resource
    private AigcBillingRecordMapper billingRecordMapper;

    @Test
    public void testFreezeConfirmRelease_success() {
        AigcWalletDO wallet = createWallet();
        AigcBillingFreezeReqDTO freezeReqDTO = new AigcBillingFreezeReqDTO();
        freezeReqDTO.setUserId(100L);
        freezeReqDTO.setBizType("TASK_GENERATE");
        freezeReqDTO.setBizId("task-1");
        freezeReqDTO.setAmount(new BigDecimal("10.000000"));

        AigcBillingFreezeRespDTO freezeRespDTO = freezeService.freeze(freezeReqDTO);

        wallet = walletMapper.selectById(wallet.getId());
        assertEquals(0, new BigDecimal("90.000000").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(wallet.getFrozenBalance()));
        assertEquals(freezeRespDTO.getId(), freezeService.freeze(freezeReqDTO).getId());

        AigcBillingConfirmReqDTO confirmReqDTO = new AigcBillingConfirmReqDTO();
        confirmReqDTO.setFreezeId(freezeRespDTO.getId());
        confirmReqDTO.setActualAmount(new BigDecimal("10.000000"));
        freezeService.confirmFreeze(confirmReqDTO);
        freezeService.confirmFreeze(confirmReqDTO);

        wallet = walletMapper.selectById(wallet.getId());
        AigcQuotaFreezeDO freeze = freezeMapper.selectById(freezeRespDTO.getId());
        assertEquals(AigcBillingFreezeStatusEnum.CONFIRMED.getCode(), freeze.getStatus());
        assertEquals(0, new BigDecimal("0.000000").compareTo(wallet.getFrozenBalance()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(wallet.getTotalConsume()));

        List<AigcBillingRecordDO> records = billingRecordMapper.selectList();
        assertEquals(2, records.size());
        AigcBillingRecordDO freezeRecord = records.stream()
                .filter(record -> AigcBillingRecordTypeEnum.FREEZE.getCode().equals(record.getRecordType()))
                .findFirst().orElseThrow();
        AigcBillingRecordDO consumeRecord = records.stream()
                .filter(record -> AigcBillingRecordTypeEnum.CONSUME.getCode().equals(record.getRecordType()))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("90.000000").compareTo(freezeRecord.getBalanceAfter()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(freezeRecord.getFrozenBalanceAfter()));
        assertEquals(0, new BigDecimal("90.000000").compareTo(consumeRecord.getBalanceAfter()));
        assertEquals(0, BigDecimal.ZERO.compareTo(consumeRecord.getFrozenBalanceAfter()));
    }

    @Test
    public void testRelease_success() {
        createWallet();
        AigcBillingFreezeReqDTO freezeReqDTO = new AigcBillingFreezeReqDTO();
        freezeReqDTO.setUserId(100L);
        freezeReqDTO.setBizType("TASK_GENERATE");
        freezeReqDTO.setBizId("task-2");
        freezeReqDTO.setAmount(new BigDecimal("10.000000"));
        AigcBillingFreezeRespDTO freezeRespDTO = freezeService.freeze(freezeReqDTO);

        AigcBillingReleaseReqDTO releaseReqDTO = new AigcBillingReleaseReqDTO();
        releaseReqDTO.setFreezeId(freezeRespDTO.getId());
        freezeService.releaseFreeze(releaseReqDTO);
        freezeService.releaseFreeze(releaseReqDTO);

        AigcWalletDO wallet = walletMapper.selectByUserId(100L);
        assertEquals(0, new BigDecimal("100.000000").compareTo(wallet.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet.getFrozenBalance()));

        List<AigcBillingRecordDO> records = billingRecordMapper.selectList();
        assertEquals(2, records.size());
        AigcBillingRecordDO releaseRecord = records.stream()
                .filter(record -> AigcBillingRecordTypeEnum.RELEASE.getCode().equals(record.getRecordType()))
                .findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("100.000000").compareTo(releaseRecord.getBalanceAfter()));
        assertEquals(0, BigDecimal.ZERO.compareTo(releaseRecord.getFrozenBalanceAfter()));
    }

    private AigcWalletDO createWallet() {
        AigcWalletDO wallet = new AigcWalletDO();
        wallet.setUserId(100L);
        wallet.setBalance(new BigDecimal("100.000000"));
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalRecharge(BigDecimal.ZERO);
        wallet.setTotalGift(BigDecimal.ZERO);
        wallet.setTotalConsume(BigDecimal.ZERO);
        wallet.setTotalRefund(BigDecimal.ZERO);
        wallet.setStatus(0);
        walletMapper.insert(wallet);
        return wallet;
    }

}
