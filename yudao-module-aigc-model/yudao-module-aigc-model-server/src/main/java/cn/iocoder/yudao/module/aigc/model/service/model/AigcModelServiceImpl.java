package cn.iocoder.yudao.module.aigc.model.service.model;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo.AigcModelSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelCapabilityDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelCapabilityMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelParamTemplateMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelPriceMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProviderMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelTenantMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;
import static java.util.stream.Collectors.toMap;

@Service
@Validated
public class AigcModelServiceImpl implements AigcModelService {

    @Resource
    private AigcModelMapper modelMapper;

    @Resource
    private AigcModelCapabilityMapper capabilityMapper;

    @Resource
    private AigcModelParamTemplateMapper paramTemplateMapper;

    @Resource
    private AigcModelPriceMapper priceMapper;

    @Resource
    private AigcModelProviderMapper providerMapper;

    @Resource
    private AigcModelTenantMapper tenantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createModel(AigcModelSaveReqVO reqVO) {
        validateModelCodeUnique(null, reqVO.getCode());

        AigcModelDO model = BeanUtils.toBean(reqVO, AigcModelDO.class);
        modelMapper.insert(model);

        if (reqVO.getCapabilities() != null && !reqVO.getCapabilities().isEmpty()) {
            for (String capability : reqVO.getCapabilities()) {
                AigcModelCapabilityDO capabilityDO = new AigcModelCapabilityDO()
                        .setModelId(model.getId())
                        .setCapability(capability);
                capabilityMapper.insert(capabilityDO);
            }
        }

        return model.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AigcModelSaveReqVO reqVO) {
        validateModelExists(reqVO.getId());
        validateModelCodeUnique(reqVO.getId(), reqVO.getCode());

        AigcModelDO updateObj = BeanUtils.toBean(reqVO, AigcModelDO.class);
        modelMapper.updateById(updateObj);

        if (reqVO.getCapabilities() != null) {
            capabilityMapper.deleteByModelId(reqVO.getId());
            for (String capability : reqVO.getCapabilities()) {
                AigcModelCapabilityDO capabilityDO = new AigcModelCapabilityDO()
                        .setModelId(reqVO.getId())
                        .setCapability(capability);
                capabilityMapper.insert(capabilityDO);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        validateModelExists(id);

        capabilityMapper.deleteByModelId(id);
        paramTemplateMapper.deleteByModelId(id);
        priceMapper.deleteByModelId(id);
        modelMapper.deleteById(id);
    }

    @Override
    public AigcModelDO getModel(Long id) {
        return modelMapper.selectById(id);
    }

    @Override
    public AigcModelDO validateModelExists(Long id) {
        AigcModelDO model = modelMapper.selectById(id);
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        return model;
    }

    @Override
    public AigcModelDO validateModelExistsAndEnable(Long id) {
        AigcModelDO model = validateModelExists(id);
        if (!CommonStatusEnum.isEnable(model.getStatus())) {
            throw exception(MODEL_DISABLED);
        }
        return model;
    }

    @Override
    public AigcModelDO validateTenantModel(Long id, String capability) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        AigcModelTenantDO tenantModel = tenantMapper.selectByTenantIdAndModelId(tenantId, id);
        if (tenantModel == null || !Boolean.TRUE.equals(tenantModel.getEnabled())) {
            throw exception(MODEL_NOT_AUTHORIZED);
        }
        AigcModelDO model = validatePlatformModelExistsAndEnable(id);
        validateProviderEnable(model.getProviderId());
        validateModelCapability(id, capability);
        return model;
    }

    @Override
    public PageResult<AigcModelDO> getModelPage(AigcModelPageReqVO reqVO) {
        return modelMapper.selectPage(reqVO);
    }

    @Override
    public List<AigcModelDO> getModelList(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return modelMapper.selectByIds(ids);
    }

    @Override
    public void updateModelStatus(Long id, Integer status) {
        validateModelExists(id);
        modelMapper.updateById(new AigcModelDO().setId(id).setStatus(status));
    }

    @Override
    public void updateModelVisible(Long id, Boolean publicVisible) {
        validateModelExists(id);
        modelMapper.updateById(new AigcModelDO().setId(id).setPublicVisible(publicVisible));
    }

    @Override
    public void updateModelDefault(Long id, Boolean defaultModel) {
        validateModelExists(id);
        modelMapper.updateById(new AigcModelDO().setId(id).setDefaultModel(defaultModel));
    }

    @Override
    public List<AigcModelDO> listModelsByType(Integer type) {
        return modelMapper.selectListByType(type);
    }

    @Override
    public List<AigcModelDO> listTenantAvailableModels(Integer type) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<AigcModelTenantDO> tenantModels = tenantMapper.selectListByEnabledTrue(tenantId);
        tenantModels = tenantModels.stream()
                .filter(tenantModel -> Boolean.TRUE.equals(tenantModel.getPublicVisible()))
                .toList();
        if (tenantModels.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AigcModelTenantDO> tenantModelMap = tenantModels.stream()
                .collect(toMap(AigcModelTenantDO::getModelId, Function.identity(), (first, second) -> first));
        List<Long> modelIds = tenantModels.stream().map(AigcModelTenantDO::getModelId).toList();
        List<AigcModelDO> models = TenantUtils.executeIgnore(() -> modelMapper.selectByIds(modelIds));
        return models.stream()
                .filter(model -> type == null || ObjectUtil.equal(model.getType(), type))
                .filter(model -> CommonStatusEnum.isEnable(model.getStatus()))
                .filter(model -> isProviderEnable(model.getProviderId()))
                .peek(model -> fillTenantModelFields(model, tenantModelMap.get(model.getId())))
                .sorted(Comparator.comparing(AigcModelDO::getSort, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Override
    public void validateModelCapability(Long modelId, String capability) {
        validateModelExists(modelId);
        Long count = capabilityMapper.selectCountByModelIdAndCapability(modelId, capability);
        if (count == 0) {
            throw exception(MODEL_CAPABILITY_NOT_SUPPORTED);
        }
    }

    @Override
    public boolean hasModelCapability(Long modelId, String capability) {
        return capabilityMapper.selectCountByModelIdAndCapability(modelId, capability) > 0;
    }

    private void validateModelCodeUnique(Long id, String code) {
        AigcModelDO model = modelMapper.selectByCode(code);
        if (model == null) {
            return;
        }
        if (!ObjectUtil.equal(model.getId(), id)) {
            throw exception(MODEL_CODE_DUPLICATE);
        }
    }

    private AigcModelDO validatePlatformModelExistsAndEnable(Long id) {
        AigcModelDO model = TenantUtils.executeIgnore(() -> modelMapper.selectById(id));
        if (model == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
        if (!CommonStatusEnum.isEnable(model.getStatus())) {
            throw exception(MODEL_DISABLED);
        }
        return model;
    }

    private void validateProviderEnable(Long providerId) {
        if (!isProviderEnable(providerId)) {
            throw exception(MODEL_PROVIDER_DISABLED);
        }
    }

    private boolean isProviderEnable(Long providerId) {
        AigcModelProviderDO provider = TenantUtils.executeIgnore(() -> providerMapper.selectById(providerId));
        return provider != null && CommonStatusEnum.isEnable(provider.getStatus());
    }

    private void fillTenantModelFields(AigcModelDO model, AigcModelTenantDO tenantModel) {
        if (tenantModel == null) {
            return;
        }
        model.setPublicVisible(tenantModel.getPublicVisible());
        model.setDefaultModel(tenantModel.getDefaultModel());
        model.setSort(tenantModel.getSort());
        if (tenantModel.getMaxConcurrent() != null) {
            model.setMaxConcurrent(tenantModel.getMaxConcurrent());
        }
    }

}
