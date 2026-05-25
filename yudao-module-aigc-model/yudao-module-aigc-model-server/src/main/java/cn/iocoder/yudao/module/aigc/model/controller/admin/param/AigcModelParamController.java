package cn.iocoder.yudao.module.aigc.model.controller.admin.param;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelParamTemplateRespDTO;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 模型参数模板")
@RestController
@RequestMapping("/aigc/model/param")
@Validated
public class AigcModelParamController {

    @Resource
    private AigcModelParamService paramService;

    @PostMapping("/create")
    @Operation(summary = "创建参数模板")
    @PreAuthorize("@ss.hasPermission('aigc:model:param:create')")
    public CommonResult<Long> createParamTemplate(@Valid @RequestBody AigcModelParamTemplateSaveReqVO reqVO) {
        Long id = paramService.createParamTemplate(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新参数模板")
    @PreAuthorize("@ss.hasPermission('aigc:model:param:update')")
    public CommonResult<Boolean> updateParamTemplate(@Valid @RequestBody AigcModelParamTemplateSaveReqVO reqVO) {
        paramService.updateParamTemplate(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除参数模板")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:param:delete')")
    public CommonResult<Boolean> deleteParamTemplate(@RequestParam("id") Long id) {
        paramService.deleteParamTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取参数模板")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:param:query')")
    public CommonResult<AigcModelParamTemplateRespDTO> getParamTemplate(@RequestParam("id") Long id) {
        AigcModelParamTemplateDO template = paramService.getParamTemplate(id);
        return success(BeanUtils.toBean(template, AigcModelParamTemplateRespDTO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取参数模板列表")
    @Parameter(name = "modelId", description = "模型ID", required = true)
    @Parameter(name = "capability", description = "能力", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:model:param:query')")
    public CommonResult<List<AigcModelParamTemplateRespDTO>> getParamTemplateList(
            @RequestParam("modelId") Long modelId,
            @RequestParam("capability") String capability) {
        List<AigcModelParamTemplateDO> templates = paramService.getParamTemplateList(modelId, capability);
        return success(templates.stream()
                .map(template -> BeanUtils.toBean(template, AigcModelParamTemplateRespDTO.class))
                .collect(Collectors.toList()));
    }

}
