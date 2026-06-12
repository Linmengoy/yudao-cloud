package cn.iocoder.yudao.module.aigc.billing.service.recharge;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.billing.config.AigcBillingPayProperties;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcRechargeOrderMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.packageconfig.AigcRechargePackageService;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordServiceImpl;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletServiceImpl;
import cn.iocoder.yudao.module.pay.api.notify.PayNotifyApi;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayNotifyDiagnosticRespDTO;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayNotifyTaskRespDTO;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayOrderNotifyReqDTO;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.enums.notify.PayNotifyTypeEnum;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum.RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.RECHARGE_PAY_NOTIFY_NOT_MATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Import({AigcRechargeOrderServiceImpl.class, AigcWalletServiceImpl.class, AigcBillingRecordServiceImpl.class,
        AigcBillingNoGenerator.class})
public class AigcRechargeOrderServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcRechargeOrderService rechargeOrderService;
    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcRechargeOrderMapper rechargeOrderMapper;
    @Resource
    private AigcBillingRecordMapper billingRecordMapper;

    @MockitoBean
    private PayOrderApi payOrderApi;
    @MockitoBean
    private PayNotifyApi payNotifyApi;
    @MockitoBean
    private AigcBillingPayProperties payProperties;
    @MockitoBean
    private AigcRechargePackageService rechargePackageService;

    @Test
    public void testNotifyRechargePaid_idempotent() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        when(payOrderApi.getOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createSuccessPayOrder(order)));

        rechargeOrderService.notifyRechargePaid(createNotifyReq(order));
        rechargeOrderService.notifyRechargePaid(createNotifyReq(order));

        wallet = walletMapper.selectById(wallet.getId());
        AigcBillingRecordDO record = billingRecordMapper.selectByBiz(WALLET_RECHARGE.getCode(), order.getRechargeNo());
        assertEquals(AigcBillingRechargeStatusEnum.PAID.getCode(), rechargeOrderMapper.selectById(order.getId()).getStatus());
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getTotalRecharge()));
        assertNotNull(record);
        assertEquals(0, new BigDecimal("30.000000").compareTo(record.getAmount()));
        assertEquals(0, new BigDecimal("30.000000").compareTo(record.getBalanceAfter()));
        assertEquals(0, BigDecimal.ZERO.compareTo(record.getFrozenBalanceAfter()));
    }

    @Test
    public void testNotifyRechargePaid_paidWithExistingRecordButWalletNotUpdated_rechargeAgain() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createPaidOrder(wallet.getId());
        createBillingRecord(order, wallet);
        when(payOrderApi.getOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createSuccessPayOrder(order)));

        rechargeOrderService.notifyRechargePaid(createNotifyReq(order));

        wallet = walletMapper.selectById(wallet.getId());
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getTotalRecharge()));
    }

    @Test
    public void testNotifyRechargePaid_paidWithExistingRecordAndWalletUpdatedNotRechargeAgain() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createPaidOrder(wallet.getId());
        walletMapper.recharge(wallet.getId(), order.getTotalPointAmount());
        createBillingRecord(order, wallet);
        when(payOrderApi.getOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createSuccessPayOrder(order)));

        rechargeOrderService.notifyRechargePaid(createNotifyReq(order));

        wallet = walletMapper.selectById(wallet.getId());
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getTotalRecharge()));
    }

    @Test
    public void testNotifyPayOrder_success() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        when(payNotifyApi.getNotifyDiagnostic(eq(PayNotifyTypeEnum.ORDER.getType()), eq(order.getPayOrderId())))
                .thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createNotifyDiagnostic(order)));
        when(payOrderApi.getOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createSuccessPayOrder(order)));

        rechargeOrderService.notifyPayOrder(createPayOrderNotifyReq(order));

        wallet = walletMapper.selectById(wallet.getId());
        AigcBillingRecordDO record = billingRecordMapper.selectByBiz(WALLET_RECHARGE.getCode(), order.getRechargeNo());
        assertEquals(AigcBillingRechargeStatusEnum.PAID.getCode(), rechargeOrderMapper.selectById(order.getId()).getStatus());
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getBalance()));
        assertEquals(0, new BigDecimal("30.000000").compareTo(wallet.getTotalRecharge()));
        assertNotNull(record);
        assertEquals(0, new BigDecimal("30.000000").compareTo(record.getBalanceAfter()));
        assertEquals(0, BigDecimal.ZERO.compareTo(record.getFrozenBalanceAfter()));
    }

    @Test
    public void testNotifyPayOrder_notifyTaskNotExists() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        when(payNotifyApi.getNotifyDiagnostic(eq(PayNotifyTypeEnum.ORDER.getType()), eq(order.getPayOrderId())))
                .thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(new PayNotifyDiagnosticRespDTO()));

        assertServiceException(() -> rechargeOrderService.notifyPayOrder(createPayOrderNotifyReq(order)), RECHARGE_PAY_NOTIFY_NOT_MATCH);
    }

    @Test
    public void testNotifyPayOrder_notifyTaskMerchantOrderIdNotMatch() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        PayNotifyDiagnosticRespDTO diagnostic = createNotifyDiagnostic(order);
        diagnostic.getTask().setMerchantOrderId("R200000000000000001");
        when(payNotifyApi.getNotifyDiagnostic(eq(PayNotifyTypeEnum.ORDER.getType()), eq(order.getPayOrderId())))
                .thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(diagnostic));

        assertServiceException(() -> rechargeOrderService.notifyPayOrder(createPayOrderNotifyReq(order)), RECHARGE_PAY_NOTIFY_NOT_MATCH);
    }

    @Test
    public void testNotifyPayOrder_notifyTaskDataIdNotMatch() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        PayNotifyDiagnosticRespDTO diagnostic = createNotifyDiagnostic(order);
        diagnostic.getTask().setDataId(201L);
        when(payNotifyApi.getNotifyDiagnostic(eq(PayNotifyTypeEnum.ORDER.getType()), eq(order.getPayOrderId())))
                .thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(diagnostic));

        assertServiceException(() -> rechargeOrderService.notifyPayOrder(createPayOrderNotifyReq(order)), RECHARGE_PAY_NOTIFY_NOT_MATCH);
    }

    @Test
    public void testSyncPayStatus_payClosedCloseRechargeOrder() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        when(payOrderApi.syncOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createClosedPayOrder(order)));
        when(payOrderApi.getOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createClosedPayOrder(order)));

        boolean result = rechargeOrderService.syncPayStatus(order.getId(), order.getUserId());

        AigcRechargeOrderDO latest = rechargeOrderMapper.selectById(order.getId());
        assertEquals(false, result);
        assertEquals(AigcBillingRechargeStatusEnum.CLOSED.getCode(), latest.getStatus());
        assertNotNull(latest.getCloseTime());
    }

    @Test
    public void testSyncPayStatus_expiredCloseRechargeOrder() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO order = createWaitPayOrder(wallet.getId());
        order.setCreateTime(LocalDateTime.now().minusMinutes(31));
        rechargeOrderMapper.updateById(order);
        when(payProperties.getExpireMinutes()).thenReturn(30);
        when(payOrderApi.syncOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createWaitingPayOrder(order)));
        when(payOrderApi.getOrder(eq(order.getPayOrderId()))).thenReturn(cn.iocoder.yudao.framework.common.pojo.CommonResult.success(createWaitingPayOrder(order)));

        boolean result = rechargeOrderService.syncPayStatus(order.getId(), order.getUserId());

        AigcRechargeOrderDO latest = rechargeOrderMapper.selectById(order.getId());
        assertEquals(false, result);
        assertEquals(AigcBillingRechargeStatusEnum.CLOSED.getCode(), latest.getStatus());
        assertNotNull(latest.getCloseTime());
    }

    @Test
    public void testCloseExpiredRechargeOrders_success() {
        AigcWalletDO wallet = createWallet();
        AigcRechargeOrderDO expiredOrder = createWaitPayOrder(wallet.getId());
        expiredOrder.setCreateTime(LocalDateTime.now().minusMinutes(31));
        rechargeOrderMapper.updateById(expiredOrder);
        AigcRechargeOrderDO waitingOrder = createWaitPayOrder(wallet.getId(), "R100000000000000002", 201L);
        when(payProperties.getExpireMinutes()).thenReturn(30);

        int count = rechargeOrderService.closeExpiredRechargeOrders(100);

        assertEquals(1, count);
        assertEquals(AigcBillingRechargeStatusEnum.CLOSED.getCode(), rechargeOrderMapper.selectById(expiredOrder.getId()).getStatus());
        assertEquals(AigcBillingRechargeStatusEnum.WAIT_PAY.getCode(), rechargeOrderMapper.selectById(waitingOrder.getId()).getStatus());
    }

    private AigcWalletDO createWallet() {
        AigcWalletDO wallet = new AigcWalletDO();
        wallet.setUserId(100L);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenBalance(BigDecimal.ZERO);
        wallet.setTotalRecharge(BigDecimal.ZERO);
        wallet.setTotalGift(BigDecimal.ZERO);
        wallet.setTotalConsume(BigDecimal.ZERO);
        wallet.setTotalRefund(BigDecimal.ZERO);
        wallet.setStatus(0);
        walletMapper.insert(wallet);
        return wallet;
    }

    private AigcRechargeOrderDO createWaitPayOrder(Long walletId) {
        return createWaitPayOrder(walletId, "R100000000000000001", 200L);
    }

    private AigcRechargeOrderDO createWaitPayOrder(Long walletId, String rechargeNo, Long payOrderId) {
        AigcRechargeOrderDO order = createOrder(walletId);
        order.setRechargeNo(rechargeNo);
        order.setPayOrderId(payOrderId);
        order.setStatus(AigcBillingRechargeStatusEnum.WAIT_PAY.getCode());
        rechargeOrderMapper.insert(order);
        return order;
    }

    private AigcRechargeOrderDO createPaidOrder(Long walletId) {
        AigcRechargeOrderDO order = createOrder(walletId);
        order.setStatus(AigcBillingRechargeStatusEnum.PAID.getCode());
        order.setPayTime(LocalDateTime.now());
        rechargeOrderMapper.insert(order);
        return order;
    }

    private AigcRechargeOrderDO createOrder(Long walletId) {
        AigcRechargeOrderDO order = new AigcRechargeOrderDO();
        order.setRechargeNo("R100000000000000001");
        order.setWalletId(walletId);
        order.setUserId(100L);
        order.setRechargeType(AigcBillingRechargeTypeEnum.PACKAGE.getCode());
        order.setPayAmount(3000);
        order.setPointAmount(new BigDecimal("30.000000"));
        order.setGiftAmount(BigDecimal.ZERO);
        order.setTotalPointAmount(new BigDecimal("30.000000"));
        order.setPayOrderId(200L);
        order.setPayOrderNo("P100000000000000001");
        return order;
    }

    private PayOrderRespDTO createSuccessPayOrder(AigcRechargeOrderDO order) {
        PayOrderRespDTO payOrder = new PayOrderRespDTO();
        payOrder.setId(order.getPayOrderId());
        payOrder.setMerchantOrderId(order.getRechargeNo());
        payOrder.setPrice(order.getPayAmount());
        payOrder.setStatus(PayOrderStatusEnum.SUCCESS.getStatus());
        payOrder.setChannelCode("easypay_cashier");
        payOrder.setSuccessTime(LocalDateTime.now());
        return payOrder;
    }

    private PayOrderRespDTO createClosedPayOrder(AigcRechargeOrderDO order) {
        PayOrderRespDTO payOrder = createWaitingPayOrder(order);
        payOrder.setStatus(PayOrderStatusEnum.CLOSED.getStatus());
        return payOrder;
    }

    private PayOrderRespDTO createWaitingPayOrder(AigcRechargeOrderDO order) {
        PayOrderRespDTO payOrder = new PayOrderRespDTO();
        payOrder.setId(order.getPayOrderId());
        payOrder.setMerchantOrderId(order.getRechargeNo());
        payOrder.setPrice(order.getPayAmount());
        payOrder.setStatus(PayOrderStatusEnum.WAITING.getStatus());
        payOrder.setChannelCode("easypay_cashier");
        return payOrder;
    }

    private PayOrderNotifyReqDTO createPayOrderNotifyReq(AigcRechargeOrderDO order) {
        PayOrderNotifyReqDTO reqDTO = new PayOrderNotifyReqDTO();
        reqDTO.setMerchantOrderId(order.getRechargeNo());
        reqDTO.setPayOrderId(order.getPayOrderId());
        return reqDTO;
    }

    private PayNotifyDiagnosticRespDTO createNotifyDiagnostic(AigcRechargeOrderDO order) {
        PayNotifyTaskRespDTO task = new PayNotifyTaskRespDTO();
        task.setType(PayNotifyTypeEnum.ORDER.getType());
        task.setDataId(order.getPayOrderId());
        task.setMerchantOrderId(order.getRechargeNo());
        PayNotifyDiagnosticRespDTO diagnostic = new PayNotifyDiagnosticRespDTO();
        diagnostic.setTask(task);
        return diagnostic;
    }

    private AigcRechargeNotifyReqDTO createNotifyReq(AigcRechargeOrderDO order) {
        AigcRechargeNotifyReqDTO reqDTO = new AigcRechargeNotifyReqDTO();
        reqDTO.setRechargeNo(order.getRechargeNo());
        reqDTO.setPayOrderId(order.getPayOrderId());
        reqDTO.setPayOrderNo(order.getPayOrderNo());
        reqDTO.setPayChannelCode("easypay_cashier");
        reqDTO.setPayTime(LocalDateTime.now());
        return reqDTO;
    }

    private void createBillingRecord(AigcRechargeOrderDO order, AigcWalletDO wallet) {
        AigcBillingRecordDO record = new AigcBillingRecordDO();
        record.setRecordNo("BR100000000000000001");
        record.setWalletId(wallet.getId());
        record.setUserId(order.getUserId());
        record.setBizType(WALLET_RECHARGE.getCode());
        record.setBizId(order.getRechargeNo());
        record.setRecordType(RECHARGE.getCode());
        record.setTitle("AIGC 钱包充值");
        record.setAmount(order.getTotalPointAmount());
        record.setBalanceAfter(order.getTotalPointAmount());
        record.setFrozenBalanceAfter(BigDecimal.ZERO);
        record.setCurrencyType(POINT.getCode());
        billingRecordMapper.insert(record);
    }

}
