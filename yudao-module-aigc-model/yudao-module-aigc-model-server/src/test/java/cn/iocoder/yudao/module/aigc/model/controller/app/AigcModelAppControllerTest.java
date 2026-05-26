package cn.iocoder.yudao.module.aigc.model.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelCapabilityEnum;
import cn.iocoder.yudao.module.aigc.model.service.model.AigcModelService;
import cn.iocoder.yudao.module.aigc.model.service.param.AigcModelParamService;
import cn.iocoder.yudao.module.aigc.model.service.price.AigcModelPriceService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AigcModelAppControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AigcModelAppController appController;

    @Mock
    private AigcModelService modelService;
    @Mock
    private AigcModelParamService paramService;
    @Mock
    private AigcModelPriceService priceService;

    @Test
    public void testGetModel_useTenantVisibleModel() {
        AigcModelDO model = new AigcModelDO().setId(1L).setName("模型");
        when(modelService.getTenantVisibleModel(eq(1L))).thenReturn(model);

        CommonResult<AigcModelRespDTO> result = appController.getModel(1L);

        assertEquals(1L, result.getData().getId());
        verify(modelService).getTenantVisibleModel(eq(1L));
    }

    @Test
    public void testCalculatePrice_success() {
        AigcModelPriceCalculateReqDTO reqDTO = new AigcModelPriceCalculateReqDTO()
                .setModelId(1L).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode());
        AigcModelPriceCalculateRespDTO respDTO = new AigcModelPriceCalculateRespDTO()
                .setSalePrice(new BigDecimal("1.000000"));
        when(priceService.calculatePrice(eq(reqDTO))).thenReturn(respDTO);

        CommonResult<AigcModelPriceCalculateRespDTO> result = appController.calculatePrice(reqDTO);

        assertEquals(respDTO, result.getData());
    }

}
