package cn.iocoder.yudao.module.aigc.task.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskDO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRespDTO;
import cn.iocoder.yudao.module.aigc.task.service.task.AigcTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 任务")
@RestController
@RequestMapping("/aigc/task")
@Validated
public class AigcTaskAppController {

    @Resource
    private AigcTaskService taskService;

    @GetMapping("/get")
    @Operation(summary = "获取任务详情")
    @Parameter(name = "id", description = "任务ID", required = true)
    public CommonResult<AigcTaskRespDTO> getTask(@RequestParam("id") Long id) {
        AigcTaskDO task = taskService.getUserTaskWithResult(id, getLoginUserId());
        return success(toAppRespDTO(task));
    }

    @GetMapping("/page")
    @Operation(summary = "获取任务分页")
    public CommonResult<PageResult<AigcTaskRespDTO>> getTaskPage(@Valid PageParam reqVO) {
        PageResult<AigcTaskDO> pageResult = taskService.getUserTaskPage(reqVO, getLoginUserId());
        PageResult<AigcTaskRespDTO> result = BeanUtils.toBean(pageResult, AigcTaskRespDTO.class);
        result.getList().forEach(this::hideInternalFields);
        return success(result);
    }

    @GetMapping("/progress")
    @Operation(summary = "获取任务进度")
    @Parameter(name = "id", description = "任务ID", required = true)
    public CommonResult<AigcTaskRespDTO> getTaskProgress(@RequestParam("id") Long id) {
        return success(toAppRespDTO(taskService.getUserTask(id, getLoginUserId())));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消任务")
    @Parameter(name = "id", description = "任务ID", required = true)
    public CommonResult<Boolean> cancelTask(@RequestParam("id") Long id) {
        taskService.cancelUserTask(id, getLoginUserId());
        return success(true);
    }

    private AigcTaskRespDTO toAppRespDTO(AigcTaskDO task) {
        return hideInternalFields(BeanUtils.toBean(task, AigcTaskRespDTO.class));
    }

    private AigcTaskRespDTO hideInternalFields(AigcTaskRespDTO respDTO) {
        return respDTO.setCostPrice(null)
                .setProviderId(null)
                .setExternalTaskId(null)
                .setFailCode(null)
                .setOutputData(null);
    }

}
