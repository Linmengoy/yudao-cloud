package cn.iocoder.yudao.module.aigc.billing.service.recharge;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.config.AigcBillingPayProperties;
import cn.iocoder.yudao.module.aigc.billing.controller.app.recharge.vo.AppAigcRechargeOrderCreateRespVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargePackageDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcRechargeOrderMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRechargeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.packageconfig.AigcRechargePackageService;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayOrderNotifyReqDTO;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingBizTypeEnum.WALLET_RECHARGE;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.*;

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
    @Resource
    private AigcRechargePackageService rechargePackageService;
    @Resource
    private PayOrderApi payOrderApi;
    @Resource
    private AigcBillingPayProperties payProperties;

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
    @Transactional(rollbackFor = Exception.class)
    public AppAigcRechargeOrderCreateRespVO createRechargeOrder(Long userId, BigDecimal amount, Integer payAmount, String userIp, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || payAmount == null || payAmount <= 0) {
            throw exception(RECHARGE_PAY_AMOUNT_INVALID);
        }
        var wallet = walletService.getOrCreateWallet(userId);
        AigcRechargeOrderDO order = new AigcRechargeOrderDO();
        order.setRechargeNo(billingNoGenerator.generateRechargeNo());
        order.setWalletId(wallet.getId());
        order.setUserId(userId);
        order.setRechargeType("PAY");
        order.setPayAmount(payAmount);
        order.setPointAmount(amount);
        order.setGiftAmount(BigDecimal.ZERO);
        order.setTotalPointAmount(amount);
        order.setStatus(AigcBillingRechargeStatusEnum.WAIT_PAY.getCode());
        order.setRemark(remark);
        rechargeOrderMapper.insert(order);
        createAndBindPayOrder(order, userIp, "AIGC 积分充值");
        return buildCreateResp(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppAigcRechargeOrderCreateRespVO createRechargeOrderByPackage(Long userId, Long packageId, String userIp, String remark) {
        AigcRechargePackageDO rechargePackage = rechargePackageService.getEnabledRechargePackage(packageId);
        if (rechargePackage.getPayAmount() == null || rechargePackage.getPayAmount() <= 0) {
            throw exception(RECHARGE_PAY_AMOUNT_INVALID);
        }
        var wallet = walletService.getOrCreateWallet(userId);
        AigcRechargeOrderDO order = new AigcRechargeOrderDO();
        order.setRechargeNo(billingNoGenerator.generateRechargeNo());
        order.setWalletId(wallet.getId());
        order.setUserId(userId);
        order.setRechargeType("PACKAGE");
        order.setPayAmount(rechargePackage.getPayAmount());
        order.setPointAmount(rechargePackage.getPointAmount());
        order.setGiftAmount(rechargePackage.getGiftAmount());
        order.setTotalPointAmount(rechargePackage.getTotalPointAmount());
        order.setStatus(AigcBillingRechargeStatusEnum.WAIT_PAY.getCode());
        order.setRemark(remark);
        rechargeOrderMapper.insert(order);
        createAndBindPayOrder(order, userIp, rechargePackage.getName());
        return buildCreateResp(order);
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
    public boolean syncPayStatus(Long id, Long userId) {
        AigcRechargeOrderDO order = getUserRechargeOrder(id, userId);
        if (AigcBillingRechargeStatusEnum.PAID.getCode().equals(order.getStatus())) {
            compensateRechargeIfNeeded(order);
            return true;
        }
        if (!AigcBillingRechargeStatusEnum.WAIT_PAY.getCode().equals(order.getStatus())) {
            return false;
        }
        PayOrderRespDTO payOrder = validatePayOrder(order, order.getPayOrderId());
        if (!PayOrderStatusEnum.isSuccess(payOrder.getStatus())) {
            return false;
        }
        notifyRechargePaid(buildNotifyReq(order, payOrder));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean notifyPayOrder(PayOrderNotifyReqDTO reqDTO) {
        AigcRechargeOrderDO order = rechargeOrderMapper.selectByRechargeNo(reqDTO.getMerchantOrderId());
        if (order == null) {
            throw exception(RECHARGE_ORDER_NOT_EXISTS);
        }
        PayOrderRespDTO payOrder = validatePayOrder(order, reqDTO.getPayOrderId());
        if (!PayOrderStatusEnum.isSuccess(payOrder.getStatus())) {
            throw exception(RECHARGE_PAY_ORDER_STATUS_INVALID);
        }
        notifyRechargePaid(buildNotifyReq(order, payOrder));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyRechargePaid(AigcRechargeNotifyReqDTO reqDTO) {
        AigcRechargeOrderDO order = rechargeOrderMapper.selectByRechargeNo(reqDTO.getRechargeNo());
        if (order == null) {
            throw exception(RECHARGE_ORDER_NOT_EXISTS);
        }
        validatePayOrder(order, reqDTO.getPayOrderId());
        
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

    private void createAndBindPayOrder(AigcRechargeOrderDO order, String userIp, String subject) {
        Long payOrderId = payOrderApi.createOrder(new PayOrderCreateReqDTO()
                .setAppKey(payProperties.getAppKey())
                .setUserIp(userIp)
                .setUserId(order.getUserId())
                .setUserType(UserTypeEnum.MEMBER.getValue())
                .setMerchantOrderId(order.getRechargeNo())
                .setSubject(subject)
                .setBody("AIGC 充值订单：" + order.getRechargeNo())
                .setPrice(order.getPayAmount())
                .setExpireTime(LocalDateTime.now().plusMinutes(payProperties.getExpireMinutes())))
                .getCheckedData();
        PayOrderRespDTO payOrder = payOrderApi.getOrder(payOrderId).getCheckedData();
        String payOrderNo = payOrder == null ? order.getRechargeNo() : payOrder.getMerchantOrderId();
        rechargeOrderMapper.updatePayOrder(order.getId(), payOrderId, payOrderNo);
        order.setPayOrderId(payOrderId);
        order.setPayOrderNo(payOrderNo);
    }

    private PayOrderRespDTO validatePayOrder(AigcRechargeOrderDO order, Long payOrderId) {
        if (payOrderId == null || !ObjectUtil.equals(order.getPayOrderId(), payOrderId)) {
            throw exception(RECHARGE_PAY_ORDER_NOT_MATCH);
        }
        PayOrderRespDTO payOrder = payOrderApi.getOrder(payOrderId).getCheckedData();
        if (payOrder == null) {
            throw exception(RECHARGE_PAY_ORDER_NOT_EXISTS);
        }
        if (!ObjectUtil.equals(payOrder.getMerchantOrderId(), order.getRechargeNo())) {
            throw exception(RECHARGE_PAY_ORDER_NOT_MATCH);
        }
        if (!ObjectUtil.equals(payOrder.getPrice(), order.getPayAmount())) {
            throw exception(RECHARGE_PAY_ORDER_AMOUNT_NOT_MATCH);
        }
        return payOrder;
    }

    private AigcRechargeNotifyReqDTO buildNotifyReq(AigcRechargeOrderDO order, PayOrderRespDTO payOrder) {
        AigcRechargeNotifyReqDTO reqDTO = new AigcRechargeNotifyReqDTO();
        reqDTO.setRechargeNo(order.getRechargeNo());
        reqDTO.setPayOrderId(payOrder.getId());
        reqDTO.setPayOrderNo(payOrder.getMerchantOrderId());
        reqDTO.setPayChannelCode(payOrder.getChannelCode());
        reqDTO.setPayTime(payOrder.getSuccessTime() == null ? LocalDateTime.now() : payOrder.getSuccessTime());
        return reqDTO;
    }

    private AppAigcRechargeOrderCreateRespVO buildCreateResp(AigcRechargeOrderDO order) {
        AppAigcRechargeOrderCreateRespVO respVO = new AppAigcRechargeOrderCreateRespVO();
        respVO.setRechargeOrderId(order.getId());
        respVO.setRechargeNo(order.getRechargeNo());
        respVO.setPayOrderId(order.getPayOrderId());
        respVO.setPayOrderNo(order.getPayOrderNo());
        respVO.setPayAppId(payProperties.getAppId());
        respVO.setPayAmount(order.getPayAmount());
        respVO.setPointAmount(order.getPointAmount());
        respVO.setGiftAmount(order.getGiftAmount());
        respVO.setTotalPointAmount(order.getTotalPointAmount());
        return respVO;
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
