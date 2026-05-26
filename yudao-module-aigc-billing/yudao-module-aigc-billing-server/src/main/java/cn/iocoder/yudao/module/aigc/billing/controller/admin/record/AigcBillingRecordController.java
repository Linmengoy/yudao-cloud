package cn.iocoder.yudao.module.aigc.billing.controller.admin.record;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 计费流水")
@RestController
@RequestMapping("/aigc/billing/record")
@Validated
public class AigcBillingRecordController {

    @Resource
    private AigcBillingRecordService billingRecordService;

    @GetMapping("/get")
    @Operation(summary = "获取计费流水")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:record:query')")
    public CommonResult<AigcBillingRecordDO> getBillingRecord(@RequestParam("id") Long id) {
        return success(billingRecordService.getBillingRecord(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取计费流水分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:record:query')")
    public CommonResult<PageResult<AigcBillingRecordDO>> getBillingRecordPage(@Valid PageParam reqVO) {
        return success(billingRecordService.getBillingRecordPage(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出计费流水")
    @PreAuthorize("@ss.hasPermission('aigc:billing:record:export')")
    public CommonResult<PageResult<AigcBillingRecordDO>> exportBillingRecord(@Valid PageParam reqVO) {
        return success(billingRecordService.getBillingRecordPage(reqVO));
    }

}
