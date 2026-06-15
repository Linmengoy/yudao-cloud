package cn.iocoder.yudao.module.aigc.workflow.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas.AigcCanvasProjectRespVO;
import cn.iocoder.yudao.module.aigc.workflow.service.canvas.AigcCanvasProjectService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "管理后台 - AIGC 画布项目")
@RestController
@RequestMapping("/aigc/canvas")
@Validated
public class AigcCanvasAdminController {

    @Resource
    private AigcCanvasProjectService projectService;

    @GetMapping("/project/page")
    @Operation(summary = "查询画布项目分页")
    @PreAuthorize("@ss.hasPermission('aigc:asset:query')")
    public CommonResult<PageResult<AigcCanvasProjectRespVO>> getProjectPage(
            @Valid AigcCanvasProjectPageReqVO reqVO,
            @RequestParam(value = "ownerUserId", required = false) Long ownerUserId) {
        return success(projectService.getAdminProjectPage(reqVO, ownerUserId));
    }

}
