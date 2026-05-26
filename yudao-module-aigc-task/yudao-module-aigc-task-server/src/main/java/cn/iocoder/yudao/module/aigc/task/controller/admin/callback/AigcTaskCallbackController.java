package cn.iocoder.yudao.module.aigc.task.controller.admin.callback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.task.controller.admin.callback.vo.AigcTaskCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.task.controller.admin.callback.vo.AigcTaskCallbackRespVO;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskCallbackDO;
import cn.iocoder.yudao.module.aigc.task.service.callback.AigcTaskCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 任务回调")
@RestController
@RequestMapping("/aigc/task/callback")
@Validated
public class AigcTaskCallbackController {

    @Resource
    private AigcTaskCallbackService callbackService;

    @GetMapping("/get")
    @Operation(summary = "获取回调记录")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:task:callback:query')")
    public CommonResult<AigcTaskCallbackDO> getCallback(@RequestParam("id") Long id) {
        return success(callbackService.getCallback(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取回调记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:task:callback:query')")
    public CommonResult<PageResult<AigcTaskCallbackRespVO>> getCallbackPage(@Valid AigcTaskCallbackPageReqVO reqVO) {
        return success(BeanUtils.toBean(callbackService.getCallbackPage(reqVO), AigcTaskCallbackRespVO.class));
    }

    @PostMapping("/replay")
    @Operation(summary = "回调重放")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:task:callback:replay')")
    public CommonResult<Boolean> replayCallback(@RequestParam("id") Long id) {
        callbackService.replayCallback(id);
        return success(true);
    }

}
