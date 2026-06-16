package cn.iocoder.yudao.module.aigc.model.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelProviderRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelParamTemplateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitCandidateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitCandidateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitPrepareRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelValidateReqDTO;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelCapabilityEnum;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
import cn.iocoder.yudao.module.aigc.model.service.provider.AigcModelProviderService;
import cn.iocoder.yudao.module.aigc.model.service.route.AigcModelRouteService;
import cn.iocoder.yudao.module.aigc.model.service.channel.AigcModelChannelService;
import cn.iocoder.yudao.module.aigc.model.service.usage.AigcModelUsageService;
import cn.iocoder.yudao.module.aigc.model.util.AigcModelParamUtils;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.MODEL_CHANNEL_NOT_FOUND;

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
    private AigcModelRouteService routeService;

    @Resource
    private AigcModelChannelService channelService;

    @Resource
    private AigcModelUsageService usageService;

    @Override
    public CommonResult<AigcModelRespDTO> validateModel(Long modelId, String capability) {
        return success(validateAndRouteModel(modelId, capability).getModel());
    }

    @Override
    public CommonResult<AigcModelProviderRespDTO> getProvider(Long providerId) {
        AigcModelProviderDO provider = providerService.getProviderWithProxy(providerId);
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
                .map(model -> BeanUtils.toBean(model, AigcModelRespDTO.class, resp -> {
                    resp.setCapabilities(modelService.getModelCapabilities(model.getId()));
                    fillCapabilityLabels(resp, capability);
                }))
                .collect(Collectors.toList()));
    }

    @Override
    public CommonResult<List<AigcModelParamTemplateRespDTO>> getParamTemplates(Long modelId, String capability) {
        List<AigcModelParamTemplateDO> templates = paramService.getParamTemplateList(modelId, capability);
        return success(templates.stream()
                .map(this::convertParamTemplate)
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
    public CommonResult<AigcModelSubmitPrepareRespDTO> prepareSubmit(AigcModelPriceCalculateReqDTO reqDTO) {
        AigcModelRouteResult routeResult = validateAndRouteModel(reqDTO.getModelId(), reqDTO.getCapability());
        paramService.validateParams(reqDTO.getModelId(), reqDTO.getCapability(), reqDTO.getParams());
        AigcModelPriceCalculateRespDTO price = priceService.calculatePrice(reqDTO);
        return success(new AigcModelSubmitPrepareRespDTO()
                .setModel(routeResult.getModel())
                .setProvider(BeanUtils.toBean(routeResult.getProvider(), AigcModelProviderRespDTO.class))
                .setPrice(price));
    }

    @Override
    public CommonResult<AigcModelSubmitCandidateRespDTO> prepareSubmitCandidates(AigcModelSubmitCandidateReqDTO reqDTO) {
        AigcModelDO primaryModel = modelService.validateTenantModel(reqDTO.getModelId(), reqDTO.getCapability());
        paramService.validateParams(primaryModel.getId(), reqDTO.getCapability(), reqDTO.getParams());
        int limit = reqDTO.getMaxCandidates() == null || reqDTO.getMaxCandidates() <= 0 ? 10 : reqDTO.getMaxCandidates();
        List<AigcModelSubmitCandidateRespDTO.Candidate> candidates = buildSubmitCandidates(reqDTO, primaryModel).stream()
                .limit(limit)
                .collect(Collectors.toList());
        return success(new AigcModelSubmitCandidateRespDTO().setCandidates(candidates));
    }

    @Override
    public CommonResult<Long> recordUsage(AigcModelUsageRecordReqDTO reqDTO) {
        return success(usageService.recordUsage(reqDTO));
    }

    private AigcModelParamTemplateRespDTO convertParamTemplate(AigcModelParamTemplateDO template) {
        return BeanUtils.toBean(template, AigcModelParamTemplateRespDTO.class,
                resp -> resp.setOptions(AigcModelParamUtils.parseOptions(template.getOptions())));
    }

    private List<AigcModelSubmitCandidateRespDTO.Candidate> buildSubmitCandidates(AigcModelSubmitCandidateReqDTO reqDTO, AigcModelDO primaryModel) {
        Set<Long> excludeChannelIds = reqDTO.getExcludeChannelIds() == null ? Set.of() : reqDTO.getExcludeChannelIds();
        Set<Long> excludeProviderIds = reqDTO.getExcludeProviderIds() == null ? Set.of() : reqDTO.getExcludeProviderIds();
        List<AigcModelDO> models = listCandidateModels(reqDTO, primaryModel);
        List<AigcModelSubmitCandidateRespDTO.Candidate> candidates = new ArrayList<>();
        for (AigcModelDO model : models) {
            AigcModelPriceCalculateRespDTO price = calculateCandidatePrice(reqDTO, model);
            if (price == null) {
                continue;
            }
            List<AigcModelChannelDO> channels = channelService.listEnabledChannelsByModelId(model.getId());
            for (AigcModelChannelDO channel : channels) {
                if (excludeChannelIds.contains(channel.getId()) || excludeProviderIds.contains(channel.getProviderId())) {
                    continue;
                }
                if (isChannelRetry(reqDTO) && reqDTO.getPreferredProviderId() != null
                        && !Objects.equals(reqDTO.getPreferredProviderId(), channel.getProviderId())) {
                    continue;
                }
                AigcModelProviderDO provider = validateCandidateProvider(channel);
                if (provider == null) {
                    continue;
                }
                candidates.add(new AigcModelSubmitCandidateRespDTO.Candidate()
                        .setModel(convertModel(model, channel, reqDTO.getCapability()))
                        .setProvider(BeanUtils.toBean(provider, AigcModelProviderRespDTO.class))
                        .setPrice(price)
                        .setStrategy(reqDTO.getStrategy())
                        .setPriority(channel.getPriority())
                        .setWeight(channel.getWeight())
                        .setHealthStatus(channel.getHealthStatus()));
            }
        }
        return candidates.stream()
                .sorted(candidateComparator(primaryModel))
                .collect(Collectors.toList());
    }

    private List<AigcModelDO> listCandidateModels(AigcModelSubmitCandidateReqDTO reqDTO, AigcModelDO primaryModel) {
        Map<Long, AigcModelDO> modelMap = new LinkedHashMap<>();
        modelMap.put(primaryModel.getId(), primaryModel);
        if (!isChannelRetry(reqDTO)) {
            for (AigcModelDO model : modelService.listTenantAvailableModels(primaryModel.getType())) {
                if (!Objects.equals(model.getId(), primaryModel.getId())
                        && modelService.hasModelCapability(model.getId(), reqDTO.getCapability())) {
                    modelMap.put(model.getId(), model);
                }
            }
        }
        return new ArrayList<>(modelMap.values());
    }

    private AigcModelPriceCalculateRespDTO calculateCandidatePrice(AigcModelSubmitCandidateReqDTO reqDTO, AigcModelDO model) {
        try {
            paramService.validateParams(model.getId(), reqDTO.getCapability(), reqDTO.getParams());
            return priceService.calculatePrice(new AigcModelPriceCalculateReqDTO()
                    .setModelId(model.getId())
                    .setCapability(reqDTO.getCapability())
                    .setTaskType(reqDTO.getTaskType())
                    .setParams(reqDTO.getParams()));
        } catch (Exception ex) {
            return null;
        }
    }

    private AigcModelProviderDO validateCandidateProvider(AigcModelChannelDO channel) {
        try {
            return providerService.validateProviderExistsAndEnable(channel.getProviderId());
        } catch (Exception ex) {
            return null;
        }
    }

    private Comparator<AigcModelSubmitCandidateRespDTO.Candidate> candidateComparator(AigcModelDO primaryModel) {
        return Comparator
                .comparing((AigcModelSubmitCandidateRespDTO.Candidate candidate) -> !Objects.equals(candidate.getModel().getId(), primaryModel.getId()))
                .thenComparing(candidate -> nullLastInteger(candidate.getPriority()))
                .thenComparing(candidate -> nullLastBigDecimal(candidate.getPrice() == null ? null : candidate.getPrice().getCostPrice()))
                .thenComparing(candidate -> nullLastInteger(candidate.getWeight()));
    }

    private boolean isChannelRetry(AigcModelSubmitCandidateReqDTO reqDTO) {
        return "CHANNEL_RETRY".equalsIgnoreCase(reqDTO.getStrategy());
    }

    private Integer nullLastInteger(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private BigDecimal nullLastBigDecimal(BigDecimal value) {
        return value == null ? new BigDecimal("999999999") : value;
    }

    private AigcModelRouteResult validateAndRouteModel(Long modelId, String capability) {
        AigcModelDO model = modelService.validateTenantModel(modelId, capability);
        Long channelId = routeService.routeChannel(model.getId(), model.getCode(), capability);
        if (channelId == null) {
            throw exception(MODEL_CHANNEL_NOT_FOUND);
        }
        AigcModelChannelDO channel = channelService.validateChannelExistsAndEnable(channelId);
        AigcModelProviderDO provider = providerService.validateProviderExistsAndEnable(channel.getProviderId());
        return new AigcModelRouteResult(convertModel(model, channel, capability), provider);
    }

    private AigcModelRespDTO convertModel(AigcModelDO model, AigcModelChannelDO channel, String capability) {
        return BeanUtils.toBean(model, AigcModelRespDTO.class, resp -> {
            resp.setCapabilities(modelService.getModelCapabilities(model.getId()));
            fillCapabilityLabels(resp, capability);
            if (channel != null) {
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
        });
    }

    private void fillCapabilityLabels(AigcModelRespDTO resp, String capability) {
        if (capability == null) {
            return;
        }
        AigcModelCapabilityEnum capabilityEnum = AigcModelCapabilityEnum.fromCode(capability);
        resp.setCapabilityLabel(capabilityEnum.getDescription());
        resp.setCapabilityLabelEn(capabilityEnum.getLabelEn());
    }

    private static class AigcModelRouteResult {

        private final AigcModelRespDTO model;
        private final AigcModelProviderDO provider;

        private AigcModelRouteResult(AigcModelRespDTO model, AigcModelProviderDO provider) {
            this.model = model;
            this.provider = provider;
        }

        private AigcModelRespDTO getModel() {
            return model;
        }

        private AigcModelProviderDO getProvider() {
            return provider;
        }

    }

}
