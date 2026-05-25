package cn.iocoder.yudao.module.aigc.model.controller.admin.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRoutePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRouteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelRouteDO;
import cn.iocoder.yudao.module.aigc.model.service.route.AigcModelRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型路由规则")
@RestController
@RequestMapping("/aigc/model/route")
@Validated
public class AigcModelRouteController {

    @Resource
    private AigcModelRouteService routeService;

    @PostMapping("/create")
    @Operation(summary = "创建路由规则")
    @PreAuthorize("@ss.hasPermission('aigc:model:route:create')")
    public CommonResult<Long> createRoute(@Valid @RequestBody AigcModelRouteSaveReqVO reqVO) {
        Long id = routeService.createRoute(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新路由规则")
    @PreAuthorize("@ss.hasPermission('aigc:model:route:update')")
    public CommonResult<Boolean> updateRoute(@Valid @RequestBody AigcModelRouteSaveReqVO reqVO) {
        routeService.updateRoute(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除路由规则")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:route:delete')")
    public CommonResult<Boolean> deleteRoute(@RequestParam("id") Long id) {
        routeService.deleteRoute(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取路由规则")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:route:query')")
    public CommonResult<AigcModelRouteDO> getRoute(@RequestParam("id") Long id) {
        return success(routeService.getRoute(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取路由规则列表")
    @PreAuthorize("@ss.hasPermission('aigc:model:route:query')")
    public CommonResult<PageResult<AigcModelRouteDO>> getRoutePage(@Valid AigcModelRoutePageReqVO reqVO) {
        return success(routeService.getRoutePage(reqVO));
    }

    @PutMapping("/status")
    @Operation(summary = "更新路由规则状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:route:update')")
    public CommonResult<Boolean> updateRouteStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        routeService.updateRouteStatus(id, status);
        return success(true);
    }

}
