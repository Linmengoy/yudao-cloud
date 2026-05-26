package cn.iocoder.yudao.module.aigc.model.service.model;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelTenantDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProviderMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelTenantMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(AigcModelServiceImpl.class)
public class AigcModelServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcModelServiceImpl modelService;

    @Resource
    private AigcModelMapper modelMapper;
    @Resource
    private AigcModelProviderMapper providerMapper;
    @Resource
    private AigcModelTenantMapper tenantMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testGetTenantVisibleModel_success() {
        AigcModelDO model = createVisibleModel(true, true, CommonStatusEnum.ENABLE.getStatus());

        AigcModelDO result = modelService.getTenantVisibleModel(model.getId());

        assertEquals(model.getId(), result.getId());
        assertEquals(true, result.getPublicVisible());
    }

    @Test
    public void testGetTenantVisibleModel_notPublicVisible() {
        AigcModelDO model = createVisibleModel(true, false, CommonStatusEnum.ENABLE.getStatus());

        assertServiceException(() -> modelService.getTenantVisibleModel(model.getId()), MODEL_NOT_AUTHORIZED);
    }

    @Test
    public void testGetTenantVisibleModel_providerDisabled() {
        AigcModelDO model = createVisibleModel(true, true, CommonStatusEnum.DISABLE.getStatus());

        assertServiceException(() -> modelService.getTenantVisibleModel(model.getId()), MODEL_PROVIDER_DISABLED);
    }

    private AigcModelDO createVisibleModel(Boolean enabled, Boolean publicVisible, Integer providerStatus) {
        TenantContextHolder.setTenantId(1L);
        AigcModelProviderDO provider = new AigcModelProviderDO()
                .setCode(randomString()).setName(randomString()).setApiBaseUrl("https://example.com")
                .setAuthType("API_KEY").setStatus(providerStatus).setTenantId(0L);
        providerMapper.insert(provider);
        AigcModelDO model = new AigcModelDO()
                .setProviderId(provider.getId()).setCode(randomString()).setName(randomString()).setModel(randomString())
                .setType(2).setPublicVisible(true).setStatus(CommonStatusEnum.ENABLE.getStatus()).setTenantId(0L);
        modelMapper.insert(model);
        AigcModelTenantDO tenantModel = new AigcModelTenantDO()
                .setModelId(model.getId()).setEnabled(enabled).setPublicVisible(publicVisible).setSort(1);
        tenantModel.setTenantId(1L);
        tenantMapper.insert(tenantModel);
        return model;
    }

}
