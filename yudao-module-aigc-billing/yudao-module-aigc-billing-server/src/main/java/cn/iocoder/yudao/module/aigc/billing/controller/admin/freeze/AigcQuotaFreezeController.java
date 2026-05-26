package cn.iocoder.yudao.module.aigc.billing.controller.admin.freeze;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.service.freeze.AigcQuotaFreezeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 冻结记录")
@RestController
@RequestMapping("/aigc/billing/freeze")
@Validated
public class AigcQuotaFreezeController {

    @Resource
    private AigcQuotaFreezeService quotaFreezeService;

    @GetMapping("/get")
    @Operation(summary = "获取冻结记录")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:freeze:query')")
    public CommonResult<AigcBillingFreezeRespDTO> getFreeze(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(quotaFreezeService.getFreeze(id), AigcBillingFreezeRespDTO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取冻结记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:freeze:query')")
    public CommonResult<PageResult<AigcBillingFreezeRespDTO>> getFreezePage(@Valid PageParam reqVO) {
        PageResult<AigcQuotaFreezeDO> pageResult = quotaFreezeService.getFreezePage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcBillingFreezeRespDTO.class));
    }

    @PutMapping("/release")
    @Operation(summary = "人工释放冻结")
    @PreAuthorize("@ss.hasPermission('aigc:billing:freeze:update')")
    public CommonResult<Boolean> releaseFreeze(@Valid @RequestBody AigcBillingReleaseReqDTO reqDTO) {
        quotaFreezeService.releaseFreeze(reqDTO);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "人工确认扣费")
    @PreAuthorize("@ss.hasPermission('aigc:billing:freeze:update')")
    public CommonResult<Boolean> confirmFreeze(@Valid @RequestBody AigcBillingConfirmReqDTO reqDTO) {
        quotaFreezeService.confirmFreeze(reqDTO);
        return success(true);
    }

}
