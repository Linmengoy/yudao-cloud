package cn.iocoder.yudao.module.aigc.model.service.price;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelPriceDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelPriceMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelTenantMapper;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelBillingUnitEnum;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelCapabilityEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.MODEL_PRICE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(AigcModelPriceServiceImpl.class)
public class AigcModelPriceServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcModelPriceServiceImpl priceService;

    @Resource
    private AigcModelMapper modelMapper;
    @Resource
    private AigcModelPriceMapper priceMapper;
    @Resource
    private AigcModelTenantMapper tenantMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCalculatePrice_batchMultiplier() {
        AigcModelDO model = createModel();
        priceMapper.insert(createPrice(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(),
                AigcModelBillingUnitEnum.PER_IMAGE.getCode(), "0.500000", "1.000000", "{\"batchMultiplier\":true}"));
        AigcModelPriceCalculateReqDTO reqDTO = new AigcModelPriceCalculateReqDTO()
                .setModelId(model.getId()).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setParams(Map.of("batchSize", 4));

        AigcModelPriceCalculateRespDTO respDTO = priceService.calculatePrice(reqDTO);

        assertEquals(0, new BigDecimal("4.000000").compareTo(respDTO.getSalePrice()));
        assertEquals(0, new BigDecimal("2.000000").compareTo(respDTO.getCostPrice()));
        assertEquals("POINT", respDTO.getCurrencyType());
    }

    @Test
    public void testCalculatePrice_durationAndResolutionMultiplier() {
        AigcModelDO model = createModel();
        priceMapper.insert(createPrice(model.getId(), AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode(),
                AigcModelBillingUnitEnum.PER_5_SECONDS.getCode(), "2.000000", "4.000000",
                "{\"durationMultiplier\":true,\"resolutionExtra\":{\"720p\":0,\"1080p\":20}}"));
        AigcModelPriceCalculateReqDTO reqDTO = new AigcModelPriceCalculateReqDTO()
                .setModelId(model.getId()).setCapability(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode())
                .setParams(Map.of("duration", 12, "resolution", "1080p"));

        AigcModelPriceCalculateRespDTO respDTO = priceService.calculatePrice(reqDTO);

        assertEquals(0, new BigDecimal("14.4000000").compareTo(respDTO.getSalePrice()));
        assertEquals(0, new BigDecimal("7.2000000").compareTo(respDTO.getCostPrice()));
    }

    @Test
    public void testCalculatePrice_priceNotFound() {
        AigcModelDO model = createModel();
        AigcModelPriceCalculateReqDTO reqDTO = new AigcModelPriceCalculateReqDTO()
                .setModelId(model.getId()).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode());

        assertServiceException(() -> priceService.calculatePrice(reqDTO), MODEL_PRICE_NOT_FOUND);
    }

    private AigcModelDO createModel() {
        Long tenantId = 1L;
        TenantContextHolder.setTenantId(tenantId);
        AigcModelDO model = randomPojo(AigcModelDO.class)
                .setCode(randomString()).setStatus(1);
        model.setTenantId(tenantId);
        modelMapper.insert(model);
        AigcModelTenantDO tenant = new AigcModelTenantDO().setModelId(model.getId()).setEnabled(true);
        tenant.setTenantId(tenantId);
        tenantMapper.insert(tenant);
        return model;
    }

    private AigcModelPriceDO createPrice(Long modelId, String capability, String billingUnit,
                                         String costPrice, String salePrice, String priceConfig) {
        return new AigcModelPriceDO().setModelId(modelId).setCapability(capability).setBillingUnit(billingUnit)
                .setCostPrice(new BigDecimal(costPrice)).setSalePrice(new BigDecimal(salePrice))
                .setCurrencyType("POINT").setPriceConfig(priceConfig).setStatus(1);
    }

}
