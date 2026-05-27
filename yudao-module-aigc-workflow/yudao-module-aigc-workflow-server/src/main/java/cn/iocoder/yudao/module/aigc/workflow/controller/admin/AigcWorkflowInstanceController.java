package cn.iocoder.yudao.module.aigc.workflow.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowInstancePageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowCancelReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowInstanceRespDTO;
import cn.iocoder.yudao.module.aigc.workflow.dto.AigcWorkflowRetryNodeReqDTO;
import cn.iocoder.yudao.module.aigc.workflow.service.instance.AigcWorkflowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 工作流实例")
@RestController
@RequestMapping("/aigc/workflow/instance")
@Validated
public class AigcWorkflowInstanceController {

    @Resource
    private AigcWorkflowInstanceService instanceService;

    @GetMapping("/page")
    @Operation(summary = "实例分页")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<PageResult<AigcWorkflowInstanceRespDTO>> getInstancePage(@Valid AigcWorkflowInstancePageReqVO reqVO) {
        return success(BeanUtils.toBean(instanceService.getInstancePage(reqVO), AigcWorkflowInstanceRespDTO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "实例详情")
    @Parameter(name = "id", description = "实例编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<AigcWorkflowInstanceRespDTO> getInstance(@RequestParam("id") Long id) {
        return success(instanceService.getInstanceDetail(id));
    }

    @PostMapping("/retry-node")
    @Operation(summary = "重试节点")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> retryNode(@Valid @RequestBody AigcWorkflowRetryNodeReqDTO reqDTO) {
        instanceService.retryNode(reqDTO);
        return success(true);
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消实例")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> cancel(@Valid @RequestBody AigcWorkflowCancelReqDTO reqDTO) {
        instanceService.cancel(reqDTO);
        return success(true);
    }

}
