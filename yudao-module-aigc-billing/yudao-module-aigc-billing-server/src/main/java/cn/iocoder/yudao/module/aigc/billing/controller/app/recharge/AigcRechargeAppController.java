package cn.iocoder.yudao.module.aigc.billing.controller.app.recharge;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.controller.app.recharge.vo.AppAigcRechargeOrderCreateRespVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.service.recharge.AigcRechargeOrderService;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayOrderNotifyReqDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 充值")
@RestController
@RequestMapping("/aigc/billing/recharge")
@Validated
public class AigcRechargeAppController {

    @Resource
    private AigcRechargeOrderService rechargeOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建充值订单")
    public CommonResult<AppAigcRechargeOrderCreateRespVO> createRechargeOrder(@RequestParam("amount") BigDecimal amount,
                                                                              @RequestParam(value = "payAmount", required = false) Integer payAmount) {
        return success(rechargeOrderService.createRechargeOrder(getLoginUserId(), amount, payAmount, getClientIP(), "用户充值"));
    }

    @PostMapping("/create-by-package")
    @Operation(summary = "按套餐创建充值订单")
    public CommonResult<AppAigcRechargeOrderCreateRespVO> createRechargeOrderByPackage(@RequestParam("packageId") Long packageId) {
        return success(rechargeOrderService.createRechargeOrderByPackage(getLoginUserId(), packageId, getClientIP(), "用户套餐充值"));
    }

    @GetMapping("/get")
    @Operation(summary = "获取充值订单")
    public CommonResult<AigcRechargeOrderDO> getRechargeOrder(@RequestParam("id") Long id) {
        return success(rechargeOrderService.getUserRechargeOrder(id, getLoginUserId()));
    }

    @GetMapping("/page")
    @Operation(summary = "获取当前用户充值订单分页")
    public CommonResult<PageResult<AigcRechargeOrderDO>> getRechargeOrderPage(@Valid PageParam reqVO) {
        return success(rechargeOrderService.getUserRechargeOrderPage(reqVO, getLoginUserId()));
    }

    @PostMapping("/sync-pay-status")
    @Operation(summary = "同步支付状态")
    public CommonResult<Boolean> syncPayStatus(@RequestParam("id") Long id) {
        return success(rechargeOrderService.syncPayStatus(id, getLoginUserId()));
    }

    @PostMapping("/pay-notify")
    @Operation(summary = "支付订单回调通知")
    @PermitAll
    public CommonResult<Boolean> notifyPayOrder(@Valid @RequestBody PayOrderNotifyReqDTO reqDTO) {
        return success(rechargeOrderService.notifyPayOrder(reqDTO));
    }

}
