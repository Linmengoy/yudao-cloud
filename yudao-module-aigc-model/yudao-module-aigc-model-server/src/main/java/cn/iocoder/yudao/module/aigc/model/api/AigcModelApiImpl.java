package cn.iocoder.yudao.module.aigc.model.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelProviderRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelParamTemplateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelValidateReqDTO;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
import cn.iocoder.yudao.module.aigc.model.service.provider.AigcModelProviderService;
import cn.iocoder.yudao.module.aigc.model.service.usage.AigcModelUsageService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcModelApiImpl implements AigcModelApi {

    @Resource
    private AigcModelService modelService;

    @Resource
    private AigcModelProviderService providerService;

    @Resource
    private AigcModelParamService paramService;

    @Resource
    private AigcModelPriceService priceService;

    @Resource
    private AigcModelUsageService usageService;

    @Override
    public CommonResult<AigcModelRespDTO> validateModel(Long modelId, String capability) {
        AigcModelDO model = modelService.validateTenantModel(modelId, capability);
        return success(BeanUtils.toBean(model, AigcModelRespDTO.class));
    }

    @Override
    public CommonResult<AigcModelProviderRespDTO> getProvider(Long providerId) {
        AigcModelProviderDO provider = providerService.validateProviderExists(providerId);
        return success(BeanUtils.toBean(provider, AigcModelProviderRespDTO.class));
    }

    @Override
    public CommonResult<AigcModelRespDTO> getModel(Long modelId) {
        AigcModelDO model = modelService.validateModelExists(modelId);
        return success(BeanUtils.toBean(model, AigcModelRespDTO.class));
    }

    @Override
    public CommonResult<List<AigcModelRespDTO>> listAvailableModels(Integer type, String capability) {
        List<AigcModelDO> models = modelService.listTenantAvailableModels(type);
        return success(models.stream()
                .filter(model -> capability == null || modelService.hasModelCapability(model.getId(), capability))
                .map(model -> BeanUtils.toBean(model, AigcModelRespDTO.class))
                .collect(Collectors.toList()));
    }

    @Override
    public CommonResult<List<AigcModelParamTemplateRespDTO>> getParamTemplates(Long modelId, String capability) {
        List<AigcModelParamTemplateDO> templates = paramService.getParamTemplateList(modelId, capability);
        return success(templates.stream()
                .map(template -> BeanUtils.toBean(template, AigcModelParamTemplateRespDTO.class))
                .collect(Collectors.toList()));
    }

    @Override
    public CommonResult<Boolean> validateParams(AigcModelValidateReqDTO reqDTO) {
        modelService.validateModelCapability(reqDTO.getModelId(), reqDTO.getCapability());
        paramService.validateParams(reqDTO.getModelId(), reqDTO.getCapability(), reqDTO.getParams());
        return success(true);
    }

    @Override
    public CommonResult<AigcModelPriceCalculateRespDTO> calculatePrice(AigcModelPriceCalculateReqDTO reqDTO) {
        return success(priceService.calculatePrice(reqDTO));
    }

    @Override
    public CommonResult<Long> recordUsage(AigcModelUsageRecordReqDTO reqDTO) {
        return success(usageService.recordUsage(reqDTO));
    }

}
