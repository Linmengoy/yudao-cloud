package cn.iocoder.yudao.module.aigc.model.service.price;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.price.vo.AigcModelPriceSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelPriceDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelPriceMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelTenantMapper;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelBillingUnitEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelPriceServiceImpl implements AigcModelPriceService {

    @Resource
    private AigcModelPriceMapper priceMapper;

    @Resource
    private AigcModelMapper modelMapper;

    @Resource
    private AigcModelTenantMapper tenantMapper;

    @Override
    public Long createPrice(AigcModelPriceSaveReqVO reqVO) {
        validateModelExists(reqVO.getModelId());
        validatePriceUnique(null, reqVO.getModelId(), reqVO.getCapability());

        AigcModelPriceDO price = BeanUtils.toBean(reqVO, AigcModelPriceDO.class);
        priceMapper.insert(price);
        return price.getId();
    }

    @Override
    public void updatePrice(AigcModelPriceSaveReqVO reqVO) {
        validatePriceExists(reqVO.getId());
        validateModelExists(reqVO.getModelId());
        validatePriceUnique(reqVO.getId(), reqVO.getModelId(), reqVO.getCapability());

        AigcModelPriceDO updateObj = BeanUtils.toBean(reqVO, AigcModelPriceDO.class);
        priceMapper.updateById(updateObj);
    }

    @Override
    public void deletePrice(Long id) {
        validatePriceExists(id);
        priceMapper.deleteById(id);
    }

    @Override
    public AigcModelPriceDO getPrice(Long id) {
        return priceMapper.selectById(id);
    }

    @Override
    public List<AigcModelPriceDO> getPriceList(Long modelId, String capability) {
        return priceMapper.selectListByModelIdAndCapability(modelId, capability);
    }

    @Override
    public void updatePriceStatus(Long id, Integer status) {
        validatePriceExists(id);
        priceMapper.updateById(new AigcModelPriceDO().setId(id).setStatus(status));
    }

    @Override
    public AigcModelPriceCalculateRespDTO calculatePrice(AigcModelPriceCalculateReqDTO reqDTO) {
        validateTenantModel(reqDTO.getModelId());

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        AigcModelPriceDO price = TenantUtils.executeIgnore(
                () -> priceMapper.selectByModelIdAndCapability(reqDTO.getModelId(), reqDTO.getCapability(), tenantId));
        if (price == null) {
            throw exception(MODEL_PRICE_NOT_FOUND);
        }

        BigDecimal salePrice = price.getSalePrice();
        BigDecimal costPrice = price.getCostPrice();
        Map<String, Object> priceDetail = new HashMap<>();

        AigcModelBillingUnitEnum billingUnit = AigcModelBillingUnitEnum.getByValue(price.getBillingUnit());
        if (billingUnit != null) {
            JSONObject params = reqDTO.getParams() == null ? null : JSONUtil.parseObj(reqDTO.getParams());
            salePrice = calculateByBillingUnit(salePrice, billingUnit, params, price.getPriceConfig());
            costPrice = calculateByBillingUnit(costPrice, billingUnit, params, price.getPriceConfig());
            priceDetail.put("billingUnit", billingUnit.getCode());
            priceDetail.put("priceConfig", price.getPriceConfig());
        }

        return new AigcModelPriceCalculateRespDTO()
                .setModelId(reqDTO.getModelId())
                .setCapability(reqDTO.getCapability())
                .setCurrencyType(price.getCurrencyType())
                .setSalePrice(salePrice)
                .setCostPrice(costPrice)
                .setBillingUnit(price.getBillingUnit())
                .setPriceSource(ObjectUtil.equal(price.getTenantId(), tenantId) ? "TENANT" : "PLATFORM")
                .setPriceRuleId(price.getId())
                .setPriceDetail(priceDetail);
    }

    private BigDecimal calculateByBillingUnit(BigDecimal basePrice, AigcModelBillingUnitEnum billingUnit,
                                              JSONObject params, String priceConfig) {
        BigDecimal multiplier = BigDecimal.ONE;

        if (priceConfig != null) {
            JSONObject config = JSONUtil.parseObj(priceConfig);

            if (Boolean.TRUE.equals(config.getBool("batchMultiplier")) && params != null) {
                Integer batchSize = params.getInt("batchSize", 1);
                multiplier = multiplier.multiply(BigDecimal.valueOf(batchSize));
            }

            if (Boolean.TRUE.equals(config.getBool("durationMultiplier")) && params != null) {
                Integer duration = params.getInt("duration", 10);
                BigDecimal durationFactor = BigDecimal.valueOf(Math.ceil(duration / 5.0));
                multiplier = multiplier.multiply(durationFactor);
            }

            if (config.containsKey("resolutionExtra") && params != null) {
                String resolution = params.getStr("resolution", "720p");
                Integer extra = config.getJSONObject("resolutionExtra").getInt(resolution, 0);
                if (extra > 0) {
                    BigDecimal extraFactor = BigDecimal.ONE.add(BigDecimal.valueOf(extra).divide(BigDecimal.valueOf(100)));
                    multiplier = multiplier.multiply(extraFactor);
                }
            }
        }

        return basePrice.multiply(multiplier);
    }

    private void validateModelExists(Long modelId) {
        if (modelMapper.selectById(modelId) == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
    }

    private void validateTenantModel(Long modelId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        AigcModelTenantDO tenantModel = tenantMapper.selectByTenantIdAndModelId(tenantId, modelId);
        if (tenantModel == null || !Boolean.TRUE.equals(tenantModel.getEnabled())) {
            throw exception(MODEL_NOT_AUTHORIZED);
        }
        if (TenantUtils.executeIgnore(() -> modelMapper.selectById(modelId)) == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
    }

    private void validatePriceExists(Long id) {
        if (priceMapper.selectById(id) == null) {
            throw exception(MODEL_PRICE_NOT_EXISTS);
        }
    }

    private void validatePriceUnique(Long id, Long modelId, String capability) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (priceMapper.selectCountByModelIdAndCapabilityAndTenantId(id, modelId, capability, tenantId) > 0) {
            throw exception(MODEL_PRICE_DUPLICATE);
        }
    }

}
