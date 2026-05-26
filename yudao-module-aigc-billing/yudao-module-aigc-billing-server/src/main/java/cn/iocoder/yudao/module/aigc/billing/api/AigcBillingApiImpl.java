package cn.iocoder.yudao.module.aigc.billing.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcCostRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcGrossProfitRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcWalletRespDTO;
import cn.iocoder.yudao.module.aigc.billing.service.cost.AigcCostRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.freeze.AigcQuotaFreezeService;
import cn.iocoder.yudao.module.aigc.billing.service.recharge.AigcRechargeOrderService;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcBillingApiImpl implements AigcBillingApi {

    @Resource
    private AigcWalletService walletService;
    @Resource
    private AigcQuotaFreezeService quotaFreezeService;
    @Resource
    private AigcBillingRecordService billingRecordService;
    @Resource
    private AigcCostRecordService costRecordService;
    @Resource
    private AigcRechargeOrderService rechargeOrderService;

    @Override
    public CommonResult<AigcWalletRespDTO> getOrCreateWallet(Long userId) {
        AigcWalletDO wallet = walletService.getOrCreateWallet(userId);
        return success(BeanUtils.toBean(wallet, AigcWalletRespDTO.class));
    }

    @Override
    public CommonResult<AigcWalletRespDTO> getWallet(Long userId) {
        AigcWalletDO wallet = walletService.getWallet(userId);
        return success(BeanUtils.toBean(wallet, AigcWalletRespDTO.class));
    }

    @Override
    public CommonResult<AigcBillingFreezeRespDTO> freeze(AigcBillingFreezeReqDTO reqDTO) {
        return success(quotaFreezeService.freeze(reqDTO));
    }

    @Override
    public CommonResult<Boolean> confirmFreeze(AigcBillingConfirmReqDTO reqDTO) {
        quotaFreezeService.confirmFreeze(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> releaseFreeze(AigcBillingReleaseReqDTO reqDTO) {
        quotaFreezeService.releaseFreeze(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Long> createBillingRecord(AigcBillingRecordCreateReqDTO reqDTO) {
        return success(billingRecordService.createBillingRecord(reqDTO));
    }

    @Override
    public CommonResult<Long> createCostRecord(AigcCostRecordCreateReqDTO reqDTO) {
        return success(costRecordService.createCostRecord(reqDTO));
    }

    @Override
    public CommonResult<AigcGrossProfitRespDTO> calculateGrossProfit(Long taskId) {
        return success(costRecordService.calculateGrossProfit(taskId));
    }

    @Override
    public CommonResult<Boolean> notifyRechargePaid(AigcRechargeNotifyReqDTO reqDTO) {
        rechargeOrderService.notifyRechargePaid(reqDTO);
        return success(true);
    }

}
