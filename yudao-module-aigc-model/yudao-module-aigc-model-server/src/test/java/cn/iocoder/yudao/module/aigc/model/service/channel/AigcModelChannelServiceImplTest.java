package cn.iocoder.yudao.module.aigc.model.service.channel;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelCloneReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelRouteDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelChannelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProviderMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelRouteMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.MODEL_CHANNEL_REFERENCED_BY_ROUTE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(AigcModelChannelServiceImpl.class)
public class AigcModelChannelServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcModelChannelServiceImpl channelService;

    @Resource
    private AigcModelMapper modelMapper;
    @Resource
    private AigcModelProviderMapper providerMapper;
    @Resource
    private AigcModelChannelMapper channelMapper;
    @Resource
    private AigcModelRouteMapper routeMapper;

    @Test
    public void testCloneChannel_success() {
        AigcModelDO model = createModel();
        AigcModelProviderDO sourceProvider = createProvider();
        AigcModelProviderDO targetProvider = createProvider();
        AigcModelChannelDO source = createChannel(model.getId(), sourceProvider.getId());

        Long cloneId = channelService.cloneChannel(new AigcModelChannelCloneReqVO()
                .setSourceChannelId(source.getId())
                .setTargetProviderId(targetProvider.getId())
                .setProviderModel("target-model")
                .setName("target-channel")
                .setWeight(50));

        AigcModelChannelDO clone = channelMapper.selectById(cloneId);
        assertEquals(model.getId(), clone.getModelId());
        assertEquals(targetProvider.getId(), clone.getProviderId());
        assertEquals("target-model", clone.getProviderModel());
        assertEquals("target-channel", clone.getName());
        assertEquals(50, clone.getWeight());
        assertEquals(source.getPriority(), clone.getPriority());
        assertEquals(source.getTimeoutSeconds(), clone.getTimeoutSeconds());
        assertEquals(source.getRateLimitConfig(), clone.getRateLimitConfig());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), clone.getStatus());
    }

    @Test
    public void testDeleteChannel_referencedByRoute() {
        AigcModelDO model = createModel();
        AigcModelProviderDO provider = createProvider();
        AigcModelChannelDO channel = createChannel(model.getId(), provider.getId());
        routeMapper.insert(new AigcModelRouteDO()
                .setName("主路由")
                .setStrategy("FIXED_MODEL")
                .setChannelIds("[" + channel.getId() + "]"));

        ServiceException exception = assertThrows(ServiceException.class, () -> channelService.deleteChannel(channel.getId()));
        assertEquals(MODEL_CHANNEL_REFERENCED_BY_ROUTE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("主路由"));
    }

    private AigcModelDO createModel() {
        AigcModelDO model = new AigcModelDO()
                .setCode(randomString())
                .setName(randomString())
                .setType(2)
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setTenantId(0L);
        modelMapper.insert(model);
        return model;
    }

    private AigcModelProviderDO createProvider() {
        AigcModelProviderDO provider = new AigcModelProviderDO()
                .setCode(randomString())
                .setName(randomString())
                .setApiBaseUrl("https://example.com")
                .setAuthType("API_KEY")
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setTenantId(0L);
        providerMapper.insert(provider);
        return provider;
    }

    private AigcModelChannelDO createChannel(Long modelId, Long providerId) {
        AigcModelChannelDO channel = new AigcModelChannelDO()
                .setModelId(modelId)
                .setProviderId(providerId)
                .setProviderModel(randomString())
                .setName("source-channel")
                .setCostPrice(new BigDecimal("0.120000"))
                .setCurrencyType("POINT")
                .setWeight(100)
                .setPriority(10)
                .setMaxConcurrent(3)
                .setTimeoutSeconds(60)
                .setRateLimitConfig("{\"qps\":1}")
                .setHealthStatus("HEALTHY")
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setTenantId(0L);
        channelMapper.insert(channel);
        assertNull(channel.getCreator());
        return channel;
    }

}
