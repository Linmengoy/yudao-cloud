package cn.iocoder.yudao.module.aigc.billing.controller.admin.cost;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import cn.iocoder.yudao.module.aigc.billing.service.cost.AigcCostRecordService;
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

@Tag(name = "管理后台 - AIGC 成本记录")
@RestController
@RequestMapping("/aigc/billing/cost")
@Validated
public class AigcCostRecordController {

    @Resource
    private AigcCostRecordService costRecordService;

    @GetMapping("/get")
    @Operation(summary = "获取成本记录")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:billing:cost:query')")
    public CommonResult<AigcCostRecordDO> getCostRecord(@RequestParam("id") Long id) {
        return success(costRecordService.getCostRecord(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取成本记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:billing:cost:query')")
    public CommonResult<PageResult<AigcCostRecordDO>> getCostRecordPage(@Valid PageParam reqVO) {
        return success(costRecordService.getCostRecordPage(reqVO));
    }

    @GetMapping("/statistics")
    @Operation(summary = "成本统计")
    @PreAuthorize("@ss.hasPermission('aigc:billing:cost:query')")
    public CommonResult<PageResult<AigcCostRecordDO>> getCostStatistics(@Valid PageParam reqVO) {
        return success(costRecordService.getCostRecordPage(reqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出成本记录")
    @PreAuthorize("@ss.hasPermission('aigc:billing:cost:export')")
    public CommonResult<PageResult<AigcCostRecordDO>> exportCostRecord(@Valid PageParam reqVO) {
        return success(costRecordService.getCostRecordPage(reqVO));
    }

}
