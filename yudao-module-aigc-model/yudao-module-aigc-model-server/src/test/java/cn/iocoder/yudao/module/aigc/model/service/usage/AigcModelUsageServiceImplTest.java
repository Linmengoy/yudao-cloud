package cn.iocoder.yudao.module.aigc.model.service.usage;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsagePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo.AigcModelUsageTypeStatisticsRespVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelUsageLogDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelUsageLogMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(AigcModelUsageServiceImpl.class)
public class AigcModelUsageServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcModelUsageServiceImpl usageService;

    @Resource
    private AigcModelMapper modelMapper;
    @Resource
    private AigcModelUsageLogMapper usageLogMapper;

    @Test
    public void testGetUsageTypeStatistics_groupByModelType() {
        AigcModelDO imageModel = createModel(2);
        AigcModelDO videoModel = createModel(3);
        usageLogMapper.insert(createUsageLog(imageModel.getId(), 0, 100L, "2.000000", "1.000000", 1000L));
        usageLogMapper.insert(createUsageLog(imageModel.getId(), 1, 50L, "1.000000", "0.500000", 2000L));
        usageLogMapper.insert(createUsageLog(videoModel.getId(), 0, 200L, "6.000000", "4.000000", 3000L));

        List<AigcModelUsageTypeStatisticsRespVO> list = usageService.getUsageTypeStatistics(
                new AigcModelUsagePageReqVO());

        assertEquals(2, list.size());
        AigcModelUsageTypeStatisticsRespVO image = list.get(0);
        assertEquals(2, image.getModelType());
        assertEquals(2L, image.getUsageCount());
        assertEquals(1L, image.getSuccessCount());
        assertEquals(1L, image.getFailedCount());
        assertEquals(150L, image.getTotalTokens());
        assertEquals(0, new BigDecimal("3.000000").compareTo(image.getSalePrice()));
        assertEquals(0, new BigDecimal("1.500000").compareTo(image.getCostPrice()));
        assertEquals(0, new BigDecimal("1500").compareTo(image.getAvgDurationMillis()));
    }

    @Test
    public void testGetUsageTypeStatistics_filterByStatus() {
        AigcModelDO imageModel = createModel(2);
        usageLogMapper.insert(createUsageLog(imageModel.getId(), 0, 100L, "2.000000", "1.000000", 1000L));
        usageLogMapper.insert(createUsageLog(imageModel.getId(), 1, 50L, "1.000000", "0.500000", 2000L));

        List<AigcModelUsageTypeStatisticsRespVO> list = usageService.getUsageTypeStatistics(
                new AigcModelUsagePageReqVO().setStatus(0));

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getUsageCount());
        assertEquals(1L, list.get(0).getSuccessCount());
        assertEquals(0L, list.get(0).getFailedCount());
    }

    private AigcModelDO createModel(Integer type) {
        AigcModelDO model = new AigcModelDO()
                .setCode(randomString()).setName(randomString()).setModel(randomString())
                .setType(type).setStatus(0).setTenantId(0L);
        modelMapper.insert(model);
        return model;
    }

    private AigcModelUsageLogDO createUsageLog(Long modelId, Integer status, Long totalTokens,
                                               String salePrice, String costPrice, Long durationMillis) {
        AigcModelUsageLogDO usageLog = new AigcModelUsageLogDO();
        usageLog.setModelId(modelId);
        usageLog.setProviderId(1L);
        usageLog.setCapability("TEXT_TO_IMAGE");
        usageLog.setStatus(status);
        usageLog.setTotalTokens(totalTokens);
        usageLog.setSalePrice(new BigDecimal(salePrice));
        usageLog.setCostPrice(new BigDecimal(costPrice));
        usageLog.setDurationMillis(durationMillis);
        usageLog.setTenantId(1L);
        return usageLog;
    }

}
