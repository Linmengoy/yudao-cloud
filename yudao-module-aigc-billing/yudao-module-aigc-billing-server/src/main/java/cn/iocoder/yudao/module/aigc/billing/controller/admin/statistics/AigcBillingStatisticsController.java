package cn.iocoder.yudao.module.aigc.billing.controller.admin.statistics;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.billing.service.statistics.AigcBillingStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 经营统计")
@RestController
@RequestMapping("/aigc/billing/statistics")
@Validated
public class AigcBillingStatisticsController {

    @Resource
    private AigcBillingStatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "经营总览")
    @PreAuthorize("@ss.hasPermission('aigc:billing:statistics:query')")
    public CommonResult<Map<String, Object>> getOverview(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return success(statisticsService.getOverview(startTime, endTime));
    }

    @GetMapping("/daily")
    @Operation(summary = "按日统计")
    @PreAuthorize("@ss.hasPermission('aigc:billing:statistics:query')")
    public CommonResult<List<Map<String, Object>>> getDailyStatistics(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return success(statisticsService.getDailyStatistics(startTime, endTime));
    }

    @GetMapping("/model")
    @Operation(summary = "按模型统计")
    @PreAuthorize("@ss.hasPermission('aigc:billing:statistics:query')")
    public CommonResult<List<Map<String, Object>>> getModelStatistics(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return success(statisticsService.getModelStatistics(startTime, endTime));
    }

    @GetMapping("/provider")
    @Operation(summary = "按渠道商统计")
    @PreAuthorize("@ss.hasPermission('aigc:billing:statistics:query')")
    public CommonResult<List<Map<String, Object>>> getProviderStatistics(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return success(statisticsService.getProviderStatistics(startTime, endTime));
    }

    @GetMapping("/user-rank")
    @Operation(summary = "用户消费排行")
    @PreAuthorize("@ss.hasPermission('aigc:billing:statistics:query')")
    public CommonResult<List<Map<String, Object>>> getUserRank(
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "20") Integer limit) {
        return success(statisticsService.getUserRank(startTime, endTime, limit));
    }

}
