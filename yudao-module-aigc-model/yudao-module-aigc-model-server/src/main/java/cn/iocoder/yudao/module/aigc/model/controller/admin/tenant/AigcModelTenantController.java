package cn.iocoder.yudao.module.aigc.model.controller.admin.tenant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.tenant.vo.AigcModelTenantSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import cn.iocoder.yudao.module.aigc.model.service.tenant.AigcModelTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 租户模型授权")
@RestController
@RequestMapping("/aigc/model/tenant")
@Validated
public class AigcModelTenantController {

    @Resource
    private AigcModelTenantService tenantService;

    @PostMapping("/create")
    @Operation(summary = "创建租户模型授权")
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:create')")
    public CommonResult<Long> createTenantModel(@Valid @RequestBody AigcModelTenantSaveReqVO reqVO) {
        Long id = tenantService.createTenantModel(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新租户模型授权")
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:update')")
    public CommonResult<Boolean> updateTenantModel(@Valid @RequestBody AigcModelTenantSaveReqVO reqVO) {
        tenantService.updateTenantModel(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除租户模型授权")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:delete')")
    public CommonResult<Boolean> deleteTenantModel(@RequestParam("id") Long id) {
        tenantService.deleteTenantModel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取租户模型授权")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:query')")
    public CommonResult<AigcModelTenantDO> getTenantModel(@RequestParam("id") Long id) {
        return success(tenantService.getTenantModel(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取租户模型授权列表")
    @Parameter(name = "tenantId", description = "租户ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:query')")
    public CommonResult<List<AigcModelTenantDO>> getTenantModelList(@RequestParam("tenantId") Long tenantId) {
        return success(tenantService.getTenantModelList(tenantId));
    }

    @PutMapping("/status")
    @Operation(summary = "更新租户模型状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "enabled", description = "是否启用", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:update')")
    public CommonResult<Boolean> updateTenantModelStatus(@RequestParam("id") Long id, @RequestParam("enabled") Boolean enabled) {
        tenantService.updateTenantModelStatus(id, enabled);
        return success(true);
    }

    @PutMapping("/visible")
    @Operation(summary = "更新租户模型可见性")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "publicVisible", description = "是否用户端展示", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:update')")
    public CommonResult<Boolean> updateTenantModelVisible(@RequestParam("id") Long id, @RequestParam("publicVisible") Boolean publicVisible) {
        tenantService.updateTenantModelVisible(id, publicVisible);
        return success(true);
    }

    @PutMapping("/default")
    @Operation(summary = "更新租户默认模型")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "defaultModel", description = "是否默认", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:tenant:update')")
    public CommonResult<Boolean> updateTenantModelDefault(@RequestParam("id") Long id, @RequestParam("defaultModel") Boolean defaultModel) {
        tenantService.updateTenantModelDefault(id, defaultModel);
        return success(true);
    }

}
