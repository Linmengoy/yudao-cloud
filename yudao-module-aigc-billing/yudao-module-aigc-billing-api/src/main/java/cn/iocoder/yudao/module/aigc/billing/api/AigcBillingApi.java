package cn.iocoder.yudao.module.aigc.billing.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcCostRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcGrossProfitRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcWalletRespDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 计费钱包")
public interface AigcBillingApi {

    String PREFIX = ApiConstants.PREFIX;

    @GetMapping(PREFIX + "/get-or-create-wallet")
    @Operation(summary = "获取或创建钱包")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    CommonResult<AigcWalletRespDTO> getOrCreateWallet(@RequestParam("userId") Long userId);

    @GetMapping(PREFIX + "/get-wallet")
    @Operation(summary = "获取钱包")
    @Parameter(name = "userId", description = "用户编号", required = true, example = "1024")
    CommonResult<AigcWalletRespDTO> getWallet(@RequestParam("userId") Long userId);

    @PostMapping(PREFIX + "/freeze")
    @Operation(summary = "冻结积分")
    CommonResult<AigcBillingFreezeRespDTO> freeze(@RequestBody AigcBillingFreezeReqDTO reqDTO);

    @PostMapping(PREFIX + "/confirm-freeze")
    @Operation(summary = "确认冻结扣费")
    CommonResult<Boolean> confirmFreeze(@RequestBody AigcBillingConfirmReqDTO reqDTO);

    @PostMapping(PREFIX + "/release-freeze")
    @Operation(summary = "释放冻结积分")
    CommonResult<Boolean> releaseFreeze(@RequestBody AigcBillingReleaseReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-billing-record")
    @Operation(summary = "创建计费流水")
    CommonResult<Long> createBillingRecord(@RequestBody AigcBillingRecordCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-cost-record")
    @Operation(summary = "创建成本记录")
    CommonResult<Long> createCostRecord(@RequestBody AigcCostRecordCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/calculate-gross-profit")
    @Operation(summary = "计算任务毛利")
    @Parameter(name = "taskId", description = "任务编号", required = true, example = "1024")
    CommonResult<AigcGrossProfitRespDTO> calculateGrossProfit(@RequestParam("taskId") Long taskId);

    @PostMapping(PREFIX + "/notify-recharge-paid")
    @Operation(summary = "通知充值支付成功")
    CommonResult<Boolean> notifyRechargePaid(@RequestBody AigcRechargeNotifyReqDTO reqDTO);

}
