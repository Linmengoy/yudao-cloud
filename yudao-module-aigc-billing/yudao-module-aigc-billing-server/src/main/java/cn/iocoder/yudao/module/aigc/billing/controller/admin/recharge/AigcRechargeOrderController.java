package cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo.AigcRechargeOrderDiagnosticRespVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo.AigcRechargeOrderPageReqVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo.AigcRechargeOrderRespVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.wallet.vo.AigcWalletAmountReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.service.recharge.AigcRechargeOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.UPDATE;
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

    @GetMapping("/diagnostic")
    @Operation(summary = "获取充值支付链路排障信息")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:query')")
    public CommonResult<AigcRechargeOrderDiagnosticRespVO> getRechargeOrderDiagnostic(@RequestParam("id") Long id) {
        return success(rechargeOrderService.getRechargeOrderDiagnostic(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取充值订单分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:query')")
    public CommonResult<PageResult<AigcRechargeOrderDO>> getRechargeOrderPage(@Valid AigcRechargeOrderPageReqVO reqVO) {
        return success(rechargeOrderService.getRechargeOrderPage(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出充值订单")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRechargeOrder(@Valid AigcRechargeOrderPageReqVO reqVO, HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AigcRechargeOrderDO> list = rechargeOrderService.getRechargeOrderPage(reqVO).getList();
        ExcelUtils.write(response, "AIGC充值订单.xls", "数据", AigcRechargeOrderRespVO.class,
                BeanUtils.toBean(list, AigcRechargeOrderRespVO.class));
    }

    @PostMapping("/manual-create")
    @Operation(summary = "手工充值")
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:create')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Long> createManualRecharge(@Valid @RequestBody AigcWalletAmountReqVO reqVO) {
        return success(rechargeOrderService.createManualRecharge(reqVO.getUserId(), reqVO.getAmount(), reqVO.getRemark()));
    }

    @PutMapping("/close")
    @Operation(summary = "关闭充值订单")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:recharge:update')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> closeRechargeOrder(@RequestParam("id") Long id) {
        rechargeOrderService.closeRechargeOrder(id);
        return success(true);
    }

}
