package cn.iocoder.yudao.module.aigc.workflow.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowEdgeSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowLogPageReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowNodeSaveReqVO;
import cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo.AigcWorkflowVersionCreateReqVO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowEdgeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowLogDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowNodeDO;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.AigcWorkflowVersionDO;
import cn.iocoder.yudao.module.aigc.workflow.service.definition.AigcWorkflowDesignService;
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

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 工作流设计")
@RestController
@RequestMapping("/aigc/workflow/design")
@Validated
public class AigcWorkflowDesignController {

    @Resource
    private AigcWorkflowDesignService designService;

    @PostMapping("/node/create")
    @Operation(summary = "创建节点")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Long> createNode(@Valid @RequestBody AigcWorkflowNodeSaveReqVO reqVO) {
        return success(designService.createNode(reqVO));
    }

    @PutMapping("/node/update")
    @Operation(summary = "更新节点")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> updateNode(@Valid @RequestBody AigcWorkflowNodeSaveReqVO reqVO) {
        designService.updateNode(reqVO);
        return success(true);
    }

    @DeleteMapping("/node/delete")
    @Operation(summary = "删除节点")
    @Parameter(name = "id", description = "节点编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> deleteNode(@RequestParam("id") Long id) {
        designService.deleteNode(id);
        return success(true);
    }

    @GetMapping("/node/list")
    @Operation(summary = "节点列表")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<List<AigcWorkflowNodeDO>> getNodeList(@RequestParam("workflowId") Long workflowId,
                                                              @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(designService.getNodeList(workflowId, versionId));
    }

    @PostMapping("/edge/create")
    @Operation(summary = "创建连线")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Long> createEdge(@Valid @RequestBody AigcWorkflowEdgeSaveReqVO reqVO) {
        return success(designService.createEdge(reqVO));
    }

    @PutMapping("/edge/update")
    @Operation(summary = "更新连线")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> updateEdge(@Valid @RequestBody AigcWorkflowEdgeSaveReqVO reqVO) {
        designService.updateEdge(reqVO);
        return success(true);
    }

    @DeleteMapping("/edge/delete")
    @Operation(summary = "删除连线")
    @Parameter(name = "id", description = "连线编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Boolean> deleteEdge(@RequestParam("id") Long id) {
        designService.deleteEdge(id);
        return success(true);
    }

    @GetMapping("/edge/list")
    @Operation(summary = "连线列表")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<List<AigcWorkflowEdgeDO>> getEdgeList(@RequestParam("workflowId") Long workflowId,
                                                              @RequestParam(value = "versionId", required = false) Long versionId) {
        return success(designService.getEdgeList(workflowId, versionId));
    }

    @PostMapping("/version/create")
    @Operation(summary = "发布新版本")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:update')")
    public CommonResult<Long> createVersion(@Valid @RequestBody AigcWorkflowVersionCreateReqVO reqVO) {
        return success(designService.createVersion(reqVO));
    }

    @GetMapping("/version/list")
    @Operation(summary = "版本列表")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<List<AigcWorkflowVersionDO>> getVersionList(@RequestParam("workflowId") Long workflowId) {
        return success(designService.getVersionList(workflowId));
    }

    @GetMapping("/log/page")
    @Operation(summary = "执行日志分页")
    @PreAuthorize("@ss.hasPermission('aigc:workflow:query')")
    public CommonResult<PageResult<AigcWorkflowLogDO>> getLogPage(@Valid AigcWorkflowLogPageReqVO reqVO) {
        return success(designService.getLogPage(reqVO));
    }

}
