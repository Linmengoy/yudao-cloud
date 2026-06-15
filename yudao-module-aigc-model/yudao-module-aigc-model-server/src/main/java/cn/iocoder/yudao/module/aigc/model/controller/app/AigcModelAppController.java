package cn.iocoder.yudao.module.aigc.model.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelChannelMapper;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelParamTemplateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
import cn.iocoder.yudao.module.aigc.model.service.route.AigcModelRouteService;
import cn.iocoder.yudao.module.aigc.model.util.AigcModelParamUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户端 - AIGC 模型")
@RestController
@RequestMapping("/aigc/model")
@Validated
public class AigcModelAppController {

    @Resource
    private AigcModelService modelService;

    @Resource
    private AigcModelParamService paramService;

    @Resource
    private AigcModelPriceService priceService;

    @Resource
    private AigcModelChannelMapper channelMapper;

    @Resource
    private AigcModelRouteService routeService;

    @GetMapping("/get")
    @Operation(summary = "获取模型详情")
    @Parameter(name = "id", description = "模型ID", required = true)
    public CommonResult<AigcModelRespDTO> getModel(@RequestParam("id") Long id) {
        AigcModelDO model = modelService.getTenantVisibleModel(id);
        return success(BeanUtils.toBean(model, AigcModelRespDTO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获取可用模型列表")
    @Parameter(name = "type", description = "模型类型")
    @Parameter(name = "capability", description = "模型能力")
    public CommonResult<List<AigcModelRespDTO>> getModelList(@RequestParam(value = "type", required = false) Integer type,
                                                             @RequestParam(value = "capability", required = false) String capability) {
        List<AigcModelDO> models = modelService.listTenantAvailableModels(type);
        return success(models.stream()
                .filter(model -> capability == null || modelService.hasModelCapability(model.getId(), capability))
                .map(model -> BeanUtils.toBean(model, AigcModelRespDTO.class,
                        resp -> fillModelListFields(resp, model, capability)))
                .collect(Collectors.toList()));
    }

    @PostMapping("/price/calculate")
    @Operation(summary = "预估模型价格")
    public CommonResult<AigcModelPriceCalculateRespDTO> calculatePrice(@RequestBody AigcModelPriceCalculateReqDTO reqDTO) {
        return success(priceService.calculatePrice(reqDTO));
    }

    @GetMapping("/param/list")
    @Operation(summary = "获取模型参数模板")
    @Parameter(name = "modelId", description = "模型ID", required = true)
    @Parameter(name = "capability", description = "能力", required = true)
    public CommonResult<List<AigcModelParamTemplateRespDTO>> getParamTemplateList(
            @RequestParam("modelId") Long modelId,
            @RequestParam("capability") String capability) {
        List<AigcModelParamTemplateDO> templates = paramService.getParamTemplateList(modelId, capability);
        return success(templates.stream()
                .map(this::convertParamTemplate)
                .collect(Collectors.toList()));
    }

    private AigcModelParamTemplateRespDTO convertParamTemplate(AigcModelParamTemplateDO template) {
        return BeanUtils.toBean(template, AigcModelParamTemplateRespDTO.class,
                resp -> resp.setOptions(AigcModelParamUtils.parseOptions(template.getOptions())));
    }

    private void fillModelListFields(AigcModelRespDTO resp, AigcModelDO model, String capability) {
        resp.setCapabilities(modelService.getModelCapabilities(model.getId()));
        Long channelId = routeService.routeChannel(model.getId(), model.getCode(), capability);
        AigcModelChannelDO channel = channelId == null ? null : channelMapper.selectById(channelId);
        if (channel == null) {
            return;
        }
        resp.setChannelId(channel.getId());
        resp.setProviderId(channel.getProviderId());
        resp.setProviderModel(channel.getProviderModel());
        resp.setModel(channel.getProviderModel());
        if (channel.getTimeoutSeconds() != null) {
            resp.setTimeoutSeconds(channel.getTimeoutSeconds());
        }
        if (channel.getMaxConcurrent() != null) {
            resp.setMaxConcurrent(channel.getMaxConcurrent());
        }
    }

}
