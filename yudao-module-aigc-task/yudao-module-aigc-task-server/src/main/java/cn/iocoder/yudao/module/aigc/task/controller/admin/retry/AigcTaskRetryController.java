package cn.iocoder.yudao.module.aigc.task.controller.admin.retry;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.retry.vo.AigcTaskRetryPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskRetryDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRetryCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.service.retry.AigcTaskRetryService;
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

@Tag(name = "管理后台 - AIGC 任务重试")
@RestController
@RequestMapping("/aigc/task/retry")
@Validated
public class AigcTaskRetryController {

    @Resource
    private AigcTaskRetryService retryService;

    @GetMapping("/page")
    @Operation(summary = "获取重试记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:task:retry:query')")
    public CommonResult<PageResult<AigcTaskRetryDO>> getRetryPage(@Valid AigcTaskRetryPageReqVO reqVO) {
        return success(retryService.getRetryPage(reqVO));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消重试")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:task:retry:update')")
    public CommonResult<Boolean> cancelRetry(@RequestParam("id") Long id) {
        retryService.cancelRetry(id);
        return success(true);
    }

    @PostMapping("/trigger")
    @Operation(summary = "手动触发重试")
    @PreAuthorize("@ss.hasPermission('aigc:task:retry')")
    public CommonResult<Long> triggerRetry(@RequestBody @Valid AigcTaskRetryCreateReqDTO reqDTO) {
        return success(retryService.createRetryRecord(reqDTO));
    }

}
