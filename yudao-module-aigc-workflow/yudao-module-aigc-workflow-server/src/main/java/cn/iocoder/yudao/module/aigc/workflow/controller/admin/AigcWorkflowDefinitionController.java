package cn.iocoder.yudao.module.aigc.workflow.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowDefinitionRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.service.definition.AigcWorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - AIGC 工作流定义")
@RestController
@RequestMapping("/aigc/workflow/definition")
@Validated
public class AigcWorkflowDefinitionController {

    @Resource
    private AigcWorkflowDefinitionService definitionService;

    @PostMapping("/create")
    @Operation(summary = "创建工作流")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:create')")
    public CommonResult<Long> createDefinition(@Valid @RequestBody AigcWorkflowDefinitionSaveReqVO reqVO) {
        return success(definitionService.createDefinition(reqVO, getLoginUserId()));
    }

    @PutMapping("/update")
    @Operation(summary = "修改工作流")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> updateDefinition(@Valid @RequestBody AigcWorkflowDefinitionSaveReqVO reqVO) {
        definitionService.updateDefinition(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工作流")
    @Parameter(name = "id", description = "工作流编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:delete')")
    public CommonResult<Boolean> deleteDefinition(@RequestParam("id") Long id) {
        definitionService.deleteDefinition(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "工作流详情")
    @Parameter(name = "id", description = "工作流编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<AigcWorkflowDefinitionRespDTO> getDefinition(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(definitionService.validateDefinitionExists(id), AigcWorkflowDefinitionRespDTO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "工作流分页")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<PageResult<AigcWorkflowDefinitionRespDTO>> getDefinitionPage(@Valid AigcWorkflowDefinitionPageReqVO reqVO) {
        return success(BeanUtils.toBean(definitionService.getDefinitionPage(reqVO), AigcWorkflowDefinitionRespDTO.class));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布工作流")
    @Parameter(name = "id", description = "工作流编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> publishDefinition(@RequestParam("id") Long id) {
        definitionService.publishDefinition(id);
        return success(true);
    }

    @PostMapping("/offline")
    @Operation(summary = "下线工作流")
    @Parameter(name = "id", description = "工作流编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> offlineDefinition(@RequestParam("id") Long id) {
        definitionService.offlineDefinition(id);
        return success(true);
    }

}
