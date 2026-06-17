package cn.iocoder.yudao.module.aigc.task.controller.admin.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskPageReqVO;
import cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo.AigcTaskStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.task.enums.AigcTaskStatusEnum;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
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

@Tag(name = "管理后台 - AIGC 任务")
@RestController
@RequestMapping("/aigc/task")
@Validated
public class AigcTaskController {

    @Resource
    private AigcTaskService taskService;

    @GetMapping("/get")
    @Operation(summary = "获取任务")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:task:query')")
    public CommonResult<AigcTaskRespDTO> getTask(@RequestParam("id") Long id) {
        taskService.validateTaskExists(id);
        AigcTaskDO task = taskService.getTaskWithResult(id);
        AigcTaskRespDTO respDTO = BeanUtils.toBean(task, AigcTaskRespDTO.class)
                .setOutputData(null);
        return success(respDTO);
    }

    @GetMapping("/page")
    @Operation(summary = "获取任务分页")
    @PreAuthorize("@ss.hasPermission('aigc:task:query')")
    public CommonResult<PageResult<AigcTaskRespDTO>> getTaskPage(@Valid AigcTaskPageReqVO reqVO) {
        PageResult<AigcTaskDO> pageResult = taskService.getTaskPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcTaskRespDTO.class));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消任务")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:task:cancel')")
    public CommonResult<Boolean> cancelTask(@RequestParam("id") Long id) {
        taskService.cancelTask(id);
        return success(true);
    }

    @PutMapping("/mark-failed")
    @Operation(summary = "人工标记失败")
    @PreAuthorize("@ss.hasPermission('aigc:task:update')")
    public CommonResult<Boolean> markFailed(@RequestBody AigcTaskStatusUpdateReqDTO reqDTO) {
        taskService.updateTaskStatus(reqDTO, AigcTaskStatusEnum.FAILED.getCode());
        return success(true);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取任务统计")
    @PreAuthorize("@ss.hasPermission('aigc:task:query')")
    public CommonResult<AigcTaskStatisticsRespVO> getTaskStatistics() {
        return success(taskService.getTaskStatistics());
    }

}
