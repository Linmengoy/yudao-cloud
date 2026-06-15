package cn.iocoder.yudao.module.aigc.model.controller.admin.usage;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsagePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsageTypeStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import cn.iocoder.yudao.module.aigc.model.service.usage.AigcModelUsageService;
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

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型用量日志")
@RestController
@RequestMapping("/aigc/model/usage")
@Validated
public class AigcModelUsageController {

    @Resource
    private AigcModelUsageService usageService;

    @GetMapping("/get")
    @Operation(summary = "获取用量日志")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:usage:query')")
    public CommonResult<AigcModelUsageLogDO> getUsage(@RequestParam("id") Long id) {
        return success(usageService.getUsageLog(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取用量日志分页")
    @PreAuthorize("@ss.hasPermission('aigc:model:usage:query')")
    public CommonResult<PageResult<AigcModelUsageLogDO>> getUsagePage(@Valid AigcModelUsagePageReqVO reqVO) {
        return success(usageService.getUsageLogPage(reqVO));
    }

    @GetMapping("/type-statistics")
    @Operation(summary = "按模型类型统计用量")
    @PreAuthorize("@ss.hasPermission('aigc:model:usage:query')")
    public CommonResult<List<AigcModelUsageTypeStatisticsRespVO>> getUsageTypeStatistics(
            @Valid AigcModelUsagePageReqVO reqVO) {
        return success(usageService.getUsageTypeStatistics(reqVO));
    }

}
