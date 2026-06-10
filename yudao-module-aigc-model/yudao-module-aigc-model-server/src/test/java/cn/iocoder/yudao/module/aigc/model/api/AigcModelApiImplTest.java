package cn.iocoder.yudao.module.aigc.model.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelSubmitPrepareRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelValidateReqDTO;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelCapabilityEnum;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
import cn.iocoder.yudao.module.aigc.model.service.provider.AigcModelProviderService;
import cn.iocoder.yudao.module.aigc.model.service.route.AigcModelRouteService;
import cn.iocoder.yudao.module.aigc.model.service.channel.AigcModelChannelService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Map;

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
    @Mock
    private AigcModelChannelService channelService;

    @Test
    public void testValidateModel_success() {
        AigcModelDO model = randomPojo(AigcModelDO.class).setId(1L).setCode("display-model");
        AigcModelChannelDO channel = randomPojo(AigcModelChannelDO.class).setId(10L).setProviderId(20L).setProviderModel("upstream-image2");
        when(modelService.validateTenantModel(eq(1L), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()))).thenReturn(model);
        when(routeService.routeChannel(eq(1L), eq("display-model"), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()))).thenReturn(10L);
        when(channelService.validateChannelExistsAndEnable(eq(10L))).thenReturn(channel);
        when(providerService.validateProviderExistsAndEnable(eq(20L))).thenReturn(null);

        CommonResult<AigcModelRespDTO> result = modelApi.validateModel(1L, AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode());

        assertEquals(0, result.getCode());
        assertEquals(model.getId(), result.getData().getId());
        assertEquals(model.getCode(), result.getData().getCode());
        assertEquals(10L, result.getData().getChannelId());
        assertEquals(20L, result.getData().getProviderId());
        assertEquals("upstream-image2", result.getData().getProviderModel());
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

    @Test
    public void testPrepareSubmit_success() {
        AigcModelPriceCalculateReqDTO reqDTO = new AigcModelPriceCalculateReqDTO()
                .setModelId(1L).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setTaskType("IMAGE").setParams(Map.of("ratio", "1:1"));
        AigcModelDO model = randomPojo(AigcModelDO.class).setId(1L).setCode("display-model");
        AigcModelChannelDO channel = randomPojo(AigcModelChannelDO.class).setId(10L).setProviderId(20L).setProviderModel("upstream-image2");
        AigcModelProviderDO provider = randomPojo(AigcModelProviderDO.class).setId(20L).setCode("openai");
        AigcModelPriceCalculateRespDTO price = new AigcModelPriceCalculateRespDTO()
                .setModelId(1L).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setSalePrice(new BigDecimal("1.000000"));
        when(modelService.validateTenantModel(eq(1L), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()))).thenReturn(model);
        when(routeService.routeChannel(eq(1L), eq("display-model"), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()))).thenReturn(10L);
        when(channelService.validateChannelExistsAndEnable(eq(10L))).thenReturn(channel);
        when(providerService.validateProviderExistsAndEnable(eq(20L))).thenReturn(provider);
        when(priceService.calculatePrice(eq(reqDTO))).thenReturn(price);

        CommonResult<AigcModelSubmitPrepareRespDTO> result = modelApi.prepareSubmit(reqDTO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getModel().getId());
        assertEquals(20L, result.getData().getProvider().getId());
        assertEquals("openai", result.getData().getProvider().getCode());
        assertEquals(price, result.getData().getPrice());
        verify(paramService).validateParams(eq(1L), eq(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode()), eq(Map.of("ratio", "1:1")));
    }

}
