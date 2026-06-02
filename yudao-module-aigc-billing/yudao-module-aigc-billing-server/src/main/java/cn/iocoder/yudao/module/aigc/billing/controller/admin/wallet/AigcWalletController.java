package cn.iocoder.yudao.module.aigc.billing.controller.admin.wallet;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.wallet.vo.AigcWalletAmountReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcWalletRespDTO;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.CREATE;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.UPDATE;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 钱包")
@RestController
@RequestMapping("/aigc/billing/wallet")
@Validated
public class AigcWalletController {

    @Resource
    private AigcWalletService walletService;

    @GetMapping("/get")
    @Operation(summary = "获取钱包")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:wallet:query')")
    public CommonResult<AigcWalletRespDTO> getWallet(@RequestParam("userId") Long userId) {
        AigcWalletDO wallet = walletService.getWallet(userId);
        return success(BeanUtils.toBean(wallet, AigcWalletRespDTO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取钱包分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:wallet:query')")
    public CommonResult<PageResult<AigcWalletRespDTO>> getWalletPage(@Valid PageParam reqVO) {
        PageResult<AigcWalletDO> pageResult = walletService.getWalletPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcWalletRespDTO.class));
    }

    @PutMapping("/adjust")
    @Operation(summary = "手动调整积分")
    @PreAuthorize("@ss.hasPermission('aigc:billing:wallet:update')")
    @ApiAccessLog(operateType = UPDATE)
    public CommonResult<Boolean> adjustWallet(@Valid @RequestBody AigcWalletAmountReqVO reqVO) {
        walletService.adjustWithRecord(reqVO.getUserId(), reqVO.getAmount(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/gift")
    @Operation(summary = "运营赠送积分")
    @PreAuthorize("@ss.hasPermission('aigc:billing:wallet:gift')")
    @ApiAccessLog(operateType = CREATE)
    public CommonResult<Boolean> giftWallet(@Valid @RequestBody AigcWalletAmountReqVO reqVO) {
        walletService.giftWithRecord(reqVO.getUserId(), reqVO.getAmount(), reqVO.getRemark());
        return success(true);
    }

}
