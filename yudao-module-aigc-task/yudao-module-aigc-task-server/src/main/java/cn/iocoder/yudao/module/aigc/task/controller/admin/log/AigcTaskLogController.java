package cn.iocoder.yudao.module.aigc.task.controller.admin.log;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.task.controller.admin.log.vo.AigcTaskLogPageReqVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskLogDO;
import cn.iocoder.yudao.module.aigc.task.service.log.AigcTaskLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 任务日志")
@RestController
@RequestMapping("/aigc/task/log")
@Validated
public class AigcTaskLogController {

    @Resource
    private AigcTaskLogService taskLogService;

    @GetMapping("/page")
    @Operation(summary = "获取任务日志分页")
    @PreAuthorize("@ss.hasPermission('aigc:task:log:query')")
    public CommonResult<PageResult<AigcTaskLogDO>> getTaskLogPage(@Valid AigcTaskLogPageReqVO reqVO) {
        return success(taskLogService.getTaskLogPage(reqVO));
    }

}
