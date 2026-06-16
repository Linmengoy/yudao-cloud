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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        validatePriceConfig(reqVO.getPriceConfig());

        AigcModelPriceDO price = BeanUtils.toBean(reqVO, AigcModelPriceDO.class);
        priceMapper.insert(price);
        return price.getId();
    }

    @Override
    public void updatePrice(AigcModelPriceSaveReqVO reqVO) {
        validatePriceExists(reqVO.getId());
        validateModelExists(reqVO.getModelId());
        validatePriceUnique(reqVO.getId(), reqVO.getModelId(), reqVO.getCapability());
        validatePriceConfig(reqVO.getPriceConfig());

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
        priceDetail.put("baseSalePrice", price.getSalePrice());
        priceDetail.put("baseCostPrice", price.getCostPrice());

        AigcModelBillingUnitEnum billingUnit = AigcModelBillingUnitEnum.getByValue(price.getBillingUnit());
        if (billingUnit != null) {
            JSONObject params = reqDTO.getParams() == null ? null : JSONUtil.parseObj(reqDTO.getParams());
            PriceCalculationResult result = calculateByBillingUnit(price.getSalePrice(), price.getCostPrice(), billingUnit,
                    params, price.getPriceConfig());
            salePrice = result.salePrice();
            costPrice = result.costPrice();
            priceDetail.put("billingUnit", billingUnit.getCode());
            priceDetail.put("priceConfig", price.getPriceConfig());
            priceDetail.put("matchedRules", result.matchedRules());
            priceDetail.put("finalSaleMultiplier", result.saleMultiplier());
            priceDetail.put("finalCostMultiplier", result.costMultiplier());
            priceDetail.put("inputParamsSummary", summarizeParams(params));
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

    private PriceCalculationResult calculateByBillingUnit(BigDecimal baseSalePrice, BigDecimal baseCostPrice,
                                                          AigcModelBillingUnitEnum billingUnit,
                                                          JSONObject params, String priceConfig) {
        BigDecimal saleMultiplier = BigDecimal.ONE;
        BigDecimal costMultiplier = BigDecimal.ONE;
        List<Map<String, Object>> matchedRules = new ArrayList<>();

        if (AigcModelBillingUnitEnum.PER_IMAGE.equals(billingUnit) && params != null) {
            BigDecimal batchSize = BigDecimal.valueOf(resolveBatchSize(params));
            saleMultiplier = saleMultiplier.multiply(batchSize);
            costMultiplier = costMultiplier.multiply(batchSize);
            Map<String, Object> ruleDetail = new HashMap<>();
            ruleDetail.put("type", "batchSize");
            ruleDetail.put("multiplier", batchSize);
            matchedRules.add(ruleDetail);
        }

        if (priceConfig != null && !priceConfig.isBlank()) {
            JSONObject config = JSONUtil.parseObj(priceConfig);

            if (!AigcModelBillingUnitEnum.PER_IMAGE.equals(billingUnit)
                    && Boolean.TRUE.equals(config.getBool("batchMultiplier")) && params != null) {
                BigDecimal batchSize = BigDecimal.valueOf(resolveBatchSize(params));
                saleMultiplier = saleMultiplier.multiply(batchSize);
                costMultiplier = costMultiplier.multiply(batchSize);
                Map<String, Object> ruleDetail = new HashMap<>();
                ruleDetail.put("type", "batchSize");
                ruleDetail.put("multiplier", batchSize);
                matchedRules.add(ruleDetail);
            }

            if (Boolean.TRUE.equals(config.getBool("durationMultiplier")) && params != null) {
                Integer duration = params.getInt("duration", 10);
                BigDecimal durationFactor = BigDecimal.valueOf(Math.ceil(duration / 5.0));
                saleMultiplier = saleMultiplier.multiply(durationFactor);
                costMultiplier = costMultiplier.multiply(durationFactor);
                Map<String, Object> ruleDetail = new HashMap<>();
                ruleDetail.put("type", "duration");
                ruleDetail.put("multiplier", durationFactor);
                ruleDetail.put("duration", duration);
                matchedRules.add(ruleDetail);
            }

            if (config.containsKey("resolutionExtra") && params != null) {
                String resolution = params.getStr("resolution", "720p");
                Integer extra = config.getJSONObject("resolutionExtra").getInt(resolution, 0);
                if (extra > 0) {
                    BigDecimal extraFactor = BigDecimal.ONE.add(BigDecimal.valueOf(extra).divide(BigDecimal.valueOf(100)));
                    saleMultiplier = saleMultiplier.multiply(extraFactor);
                    costMultiplier = costMultiplier.multiply(extraFactor);
                    Map<String, Object> ruleDetail = new HashMap<>();
                    ruleDetail.put("type", "resolutionExtra");
                    ruleDetail.put("resolution", resolution);
                    ruleDetail.put("multiplier", extraFactor);
                    matchedRules.add(ruleDetail);
                }
            }

            ParamMultiplierResult paramResult = applyParamMultipliers(config, params, saleMultiplier, costMultiplier,
                    matchedRules);
            saleMultiplier = paramResult.saleMultiplier();
            costMultiplier = paramResult.costMultiplier();
        }

        return new PriceCalculationResult(
                baseSalePrice.multiply(saleMultiplier),
                baseCostPrice.multiply(costMultiplier),
                saleMultiplier,
                costMultiplier,
                matchedRules);
    }

    private ParamMultiplierResult applyParamMultipliers(JSONObject config, JSONObject params, BigDecimal saleMultiplier,
                                                        BigDecimal costMultiplier, List<Map<String, Object>> matchedRules) {
        if (params == null || !config.containsKey("paramMultipliers")) {
            return new ParamMultiplierResult(saleMultiplier, costMultiplier);
        }
        for (Object item : config.getJSONArray("paramMultipliers")) {
            JSONObject rule = JSONUtil.parseObj(item);
            String param = rule.getStr("param");
            if (!matchesRule(params.get(param), rule)) {
                continue;
            }
            BigDecimal ruleSaleMultiplier = defaultBigDecimal(rule.getBigDecimal("saleMultiplier"), BigDecimal.ONE);
            BigDecimal ruleCostMultiplier = defaultBigDecimal(rule.getBigDecimal("costMultiplier"), ruleSaleMultiplier);
            saleMultiplier = saleMultiplier.multiply(ruleSaleMultiplier);
            costMultiplier = costMultiplier.multiply(ruleCostMultiplier);
            Map<String, Object> ruleDetail = new HashMap<>();
            ruleDetail.put("type", "paramMultiplier");
            ruleDetail.put("param", param);
            ruleDetail.put("operator", rule.getStr("operator", "eq"));
            ruleDetail.put("value", Objects.toString(rule.get("value"), ""));
            ruleDetail.put("saleMultiplier", ruleSaleMultiplier);
            ruleDetail.put("costMultiplier", ruleCostMultiplier);
            matchedRules.add(ruleDetail);
        }
        return new ParamMultiplierResult(saleMultiplier, costMultiplier);
    }

    private boolean matchesRule(Object actualValue, JSONObject rule) {
        String operator = rule.getStr("operator", "eq");
        Object expectedValue = rule.get("value");
        if ("eq".equals(operator)) {
            return Objects.equals(Objects.toString(actualValue, null), Objects.toString(expectedValue, null));
        }
        if ("in".equals(operator) && rule.getJSONArray("values") != null) {
            String actual = Objects.toString(actualValue, null);
            return rule.getJSONArray("values").stream()
                    .anyMatch(item -> Objects.equals(actual, Objects.toString(item, null)));
        }
        return false;
    }

    private Map<String, Object> summarizeParams(JSONObject params) {
        if (params == null) {
            return Map.of();
        }
        Map<String, Object> summary = new HashMap<>();
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value instanceof CharSequence text && text.length() > 64) {
                summary.put(key, text.subSequence(0, 64) + "***");
            } else {
                summary.put(key, value);
            }
        }
        return summary;
    }

    private void validatePriceConfig(String priceConfig) {
        if (priceConfig == null || priceConfig.isBlank()) {
            return;
        }
        try {
            JSONObject config = JSONUtil.parseObj(priceConfig);
            if (config.containsKey("paramMultipliers")) {
                for (Object item : config.getJSONArray("paramMultipliers")) {
                    validateParamMultiplierRule(JSONUtil.parseObj(item));
                }
            }
        } catch (Exception ex) {
            throw exception(MODEL_PRICE_INVALID);
        }
    }

    private void validateParamMultiplierRule(JSONObject rule) {
        String param = rule.getStr("param");
        String operator = rule.getStr("operator", "eq");
        BigDecimal saleMultiplier = rule.getBigDecimal("saleMultiplier");
        BigDecimal costMultiplier = defaultBigDecimal(rule.getBigDecimal("costMultiplier"), saleMultiplier);
        if (param == null || param.isBlank()
                || (!"eq".equals(operator) && !"in".equals(operator))
                || saleMultiplier == null || costMultiplier == null
                || !isValidMultiplier(saleMultiplier) || !isValidMultiplier(costMultiplier)) {
            throw exception(MODEL_PRICE_INVALID);
        }
        if ("eq".equals(operator) && rule.get("value") == null) {
            throw exception(MODEL_PRICE_INVALID);
        }
        if ("in".equals(operator) && (rule.getJSONArray("values") == null || rule.getJSONArray("values").isEmpty())) {
            throw exception(MODEL_PRICE_INVALID);
        }
    }

    private boolean isValidMultiplier(BigDecimal multiplier) {
        return multiplier.compareTo(BigDecimal.ZERO) > 0
                && multiplier.compareTo(BigDecimal.valueOf(100)) <= 0
                && multiplier.scale() <= 6;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private int resolveBatchSize(JSONObject params) {
        int batchSize = params.getInt("batchSize", params.getInt("n", 1));
        return Math.max(1, Math.min(100, batchSize));
    }

    private record PriceCalculationResult(BigDecimal salePrice, BigDecimal costPrice, BigDecimal saleMultiplier,
                                          BigDecimal costMultiplier, List<Map<String, Object>> matchedRules) {
    }

    private record ParamMultiplierResult(BigDecimal saleMultiplier, BigDecimal costMultiplier) {
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
