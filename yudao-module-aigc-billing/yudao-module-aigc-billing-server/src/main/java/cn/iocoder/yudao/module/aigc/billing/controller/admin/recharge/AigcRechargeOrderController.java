package cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.wallet.vo.AigcWalletAmountReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.service.recharge.AigcRechargeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 充值订单")
@RestController
@RequestMapping("/aigc/billing/recharge")
@Validated
public class AigcRechargeOrderController {

    @Resource
    private AigcRechargeOrderService rechargeOrderService;

    @GetMapping("/get")
    @Operation(summary = "获取充值订单")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:query')")
    public CommonResult<AigcRechargeOrderDO> getRechargeOrder(@RequestParam("id") Long id) {
        return success(rechargeOrderService.getRechargeOrder(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取充值订单分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:query')")
    public CommonResult<PageResult<AigcRechargeOrderDO>> getRechargeOrderPage(@Valid PageParam reqVO) {
        return success(rechargeOrderService.getRechargeOrderPage(reqVO));
    }

    @PostMapping("/manual-create")
    @Operation(summary = "手工充值")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:create')")
    public CommonResult<Long> createManualRecharge(@Valid @RequestBody AigcWalletAmountReqVO reqVO) {
        return success(rechargeOrderService.createManualRecharge(reqVO.getUserId(), reqVO.getAmount(), reqVO.getRemark()));
    }

    @PutMapping("/close")
    @Operation(summary = "关闭充值订单")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:update')")
    public CommonResult<Boolean> closeRechargeOrder(@RequestParam("id") Long id) {
        rechargeOrderService.closeRechargeOrder(id);
        return success(true);
    }

}
