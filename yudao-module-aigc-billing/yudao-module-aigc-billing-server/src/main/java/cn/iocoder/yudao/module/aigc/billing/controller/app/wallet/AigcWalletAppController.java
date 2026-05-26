package cn.iocoder.yudao.module.aigc.billing.controller.app.wallet;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcWalletRespDTO;
import cn.iocoder.yudao.module.aigc.billing.service.freeze.AigcQuotaFreezeService;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 钱包")
@RestController
@RequestMapping("/aigc/wallet")
@Validated
public class AigcWalletAppController {

    @Resource
    private AigcWalletService walletService;
    @Resource
    private AigcBillingRecordService billingRecordService;
    @Resource
    private AigcQuotaFreezeService quotaFreezeService;

    @GetMapping("/get")
    @Operation(summary = "获取当前用户钱包")
    public CommonResult<AigcWalletRespDTO> getWallet() {
        AigcWalletDO wallet = walletService.getOrCreateWallet(getLoginUserId());
        return success(BeanUtils.toBean(wallet, AigcWalletRespDTO.class));
    }

    @GetMapping("/record/page")
    @Operation(summary = "获取当前用户积分流水")
    public CommonResult<PageResult<AigcBillingRecordDO>> getBillingRecordPage(@Valid PageParam reqVO) {
        return success(billingRecordService.getUserBillingRecordPage(reqVO, getLoginUserId()));
    }

    @GetMapping("/freeze/page")
    @Operation(summary = "获取当前用户冻结记录")
    public CommonResult<PageResult<AigcQuotaFreezeDO>> getFreezePage(@Valid PageParam reqVO) {
        return success(quotaFreezeService.getUserFreezePage(reqVO, getLoginUserId()));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取当前用户消费统计")
    public CommonResult<Boolean> getStatistics() {
        return success(true);
    }

}
