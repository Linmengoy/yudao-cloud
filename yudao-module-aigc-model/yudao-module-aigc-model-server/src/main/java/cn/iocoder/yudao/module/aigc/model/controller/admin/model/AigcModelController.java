package cn.iocoder.yudao.module.aigc.model.controller.admin.model;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型")
@RestController
@RequestMapping("/aigc/model")
@Validated
public class AigcModelController {

    @Resource
    private AigcModelService modelService;

    @PostMapping("/create")
    @Operation(summary = "创建模型")
    @PreAuthorize("@ss.hasPermission('aigc:model:create')")
    public CommonResult<Long> createModel(@Valid @RequestBody AigcModelSaveReqVO reqVO) {
        Long id = modelService.createModel(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新模型")
    @PreAuthorize("@ss.hasPermission('aigc:model:update')")
    public CommonResult<Boolean> updateModel(@Valid @RequestBody AigcModelSaveReqVO reqVO) {
        modelService.updateModel(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除模型")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:delete')")
    public CommonResult<Boolean> deleteModel(@RequestParam("id") Long id) {
        modelService.deleteModel(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取模型")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:query')")
    public CommonResult<AigcModelRespDTO> getModel(@RequestParam("id") Long id) {
        AigcModelDO model = modelService.getModel(id);
        return success(BeanUtils.toBean(model, AigcModelRespDTO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取模型列表")
    @PreAuthorize("@ss.hasPermission('aigc:model:query')")
    public CommonResult<PageResult<AigcModelRespDTO>> getModelPage(@Valid AigcModelPageReqVO reqVO) {
        PageResult<AigcModelDO> pageResult = modelService.getModelPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcModelRespDTO.class));
    }

    @PutMapping("/status")
    @Operation(summary = "更新模型状态")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "status", description = "状态", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:update')")
    public CommonResult<Boolean> updateModelStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        modelService.updateModelStatus(id, status);
        return success(true);
    }

    @PutMapping("/visible")
    @Operation(summary = "更新模型可见性")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "publicVisible", description = "是否用户端展示", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:update')")
    public CommonResult<Boolean> updateModelVisible(@RequestParam("id") Long id, @RequestParam("publicVisible") Boolean publicVisible) {
        modelService.updateModelVisible(id, publicVisible);
        return success(true);
    }

    @PutMapping("/default")
    @Operation(summary = "更新默认模型")
    @Parameter(name = "id", description = "ID", required = true)
    @Parameter(name = "defaultModel", description = "是否默认", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:update')")
    public CommonResult<Boolean> updateModelDefault(@RequestParam("id") Long id, @RequestParam("defaultModel") Boolean defaultModel) {
        modelService.updateModelDefault(id, defaultModel);
        return success(true);
    }

}
