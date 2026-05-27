package cn.iocoder.yudao.module.aigc.workflow.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowDefinitionPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowInstancePageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCancelReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCostEstimateRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowDefinitionRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowExecuteRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowInstanceRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.enums.AigcWorkflowStatusEnum;
import cn.iocoder.yudao.module.aigc.workflow.service.definition.AigcWorkflowDefinitionService;
import cn.iocoder.yudao.module.aigc.workflow.service.instance.AigcWorkflowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 工作流")
@RestController
@RequestMapping("/aigc/workflow")
@Validated
public class AigcWorkflowAppController {

    @Resource
    private AigcWorkflowDefinitionService definitionService;
    @Resource
    private AigcWorkflowInstanceService instanceService;

    @GetMapping("/list")
    @Operation(summary = "可用工作流列表")
    public CommonResult<PageResult<AigcWorkflowDefinitionRespDTO>> getWorkflowList(@Valid AigcWorkflowDefinitionPageReqVO reqVO) {
        reqVO.setStatus(AigcWorkflowStatusEnum.PUBLISHED.getCode());
        return success(BeanUtils.toBean(definitionService.getDefinitionPage(reqVO), AigcWorkflowDefinitionRespDTO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "工作流详情")
    @Parameter(name = "id", description = "工作流编号", required = true)
    public CommonResult<AigcWorkflowDefinitionRespDTO> getWorkflow(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(definitionService.validateDefinitionExists(id), AigcWorkflowDefinitionRespDTO.class));
    }

    @PostMapping("/estimate")
    @Operation(summary = "费用预估")
    public CommonResult<AigcWorkflowCostEstimateRespDTO> estimate(@Valid @RequestBody AigcWorkflowCostEstimateReqDTO reqDTO) {
        return success(instanceService.estimateCost(reqDTO));
    }

    @PostMapping("/execute")
    @Operation(summary = "执行工作流")
    public CommonResult<AigcWorkflowExecuteRespDTO> execute(@Valid @RequestBody AigcWorkflowExecuteReqDTO reqDTO) {
        return success(instanceService.execute(getLoginUserId(), reqDTO));
    }

    @GetMapping("/instance/get")
    @Operation(summary = "实例详情")
    @Parameter(name = "id", description = "实例编号", required = true)
    public CommonResult<AigcWorkflowInstanceRespDTO> getInstance(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(instanceService.validateInstanceExists(id), AigcWorkflowInstanceRespDTO.class));
    }

    @GetMapping("/instance/page")
    @Operation(summary = "我的工作流实例")
    public CommonResult<PageResult<AigcWorkflowInstanceRespDTO>> getInstancePage(@Valid AigcWorkflowInstancePageReqVO reqVO) {
        return success(BeanUtils.toBean(instanceService.getUserInstancePage(reqVO, getLoginUserId()), AigcWorkflowInstanceRespDTO.class));
    }

    @PostMapping("/instance/cancel")
    @Operation(summary = "取消执行")
    public CommonResult<Boolean> cancel(@Valid @RequestBody AigcWorkflowCancelReqDTO reqDTO) {
        instanceService.cancel(reqDTO);
        return success(true);
    }

}
