package cn.iocoder.yudao.module.aigc.model.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelValidateReqDTO;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelCapabilityEnum;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
import cn.iocoder.yudao.module.aigc.model.service.provider.AigcModelProviderService;
import cn.iocoder.yudao.module.aigc.model.service.route.AigcModelRouteService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AigcModelApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AigcModelApiImpl modelApi;

    @Mock
    private AigcModelService modelService;
    @Mock
    private AigcModelProviderService providerService;
    @Mock
    private AigcModelParamService paramService;
    @Mock
    private AigcModelPriceService priceService;
    @Mock
    private AigcModelRouteService routeService;

    @Test
    public void testValidateModel_success() {
        AigcModelDO model = randomPojo(AigcModelDO.class).setId(1L).setCode("display-model");
        when(modelService.validateTenantModel(eq(1L), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()))).thenReturn(model);
        when(routeService.route(eq("display-model"), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()))).thenReturn(null);

        CommonResult<AigcModelRespDTO> result = modelApi.validateModel(1L, AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode());

        assertEquals(0, result.getCode());
        assertPojoEquals(model, result.getData());
    }

    @Test
    public void testValidateParams_success() {
        AigcModelValidateReqDTO reqDTO = new AigcModelValidateReqDTO()
                .setModelId(1L).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setParams(Map.of("ratio", "1:1"));

        CommonResult<Boolean> result = modelApi.validateParams(reqDTO);

        assertTrue(result.getData());
        verify(modelService).validateModelCapability(eq(1L), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()));
        verify(paramService).validateParams(eq(1L), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()), eq(Map.of("ratio", "1:1")));
    }

    @Test
    public void testCalculatePrice_success() {
        AigcModelPriceCalculateReqDTO reqDTO = new AigcModelPriceCalculateReqDTO()
                .setModelId(1L).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode());
        AigcModelPriceCalculateRespDTO respDTO = new AigcModelPriceCalculateRespDTO()
                .setModelId(1L).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setSalePrice(new BigDecimal("1.000000"));
        when(priceService.calculatePrice(eq(reqDTO))).thenReturn(respDTO);

        CommonResult<AigcModelPriceCalculateRespDTO> result = modelApi.calculatePrice(reqDTO);

        assertEquals(respDTO, result.getData());
    }

}
