package cn.iocoder.yudao.module.aigc.billing.service.cost;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcCostRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcCostRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import({AigcCostRecordServiceImpl.class, AigcBillingNoGenerator.class})
public class AigcCostRecordServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcCostRecordService costRecordService;
    @Resource
    private AigcCostRecordMapper costRecordMapper;

    @Test
    public void testCreateCostRecord_success() {
        AigcCostRecordCreateReqDTO reqDTO = new AigcCostRecordCreateReqDTO();
        reqDTO.setTaskId(200L);
        reqDTO.setUserId(100L);
        reqDTO.setCostAmount(new BigDecimal("5.000000"));
        reqDTO.setSaleAmount(new BigDecimal("10.000000"));

        Long id = costRecordService.createCostRecord(reqDTO);

        assertNotNull(id);
        AigcCostRecordDO record = costRecordMapper.selectById(id);
        assertEquals(new BigDecimal("5.000000"), record.getCostAmount());
        assertEquals(new BigDecimal("10.000000"), record.getSaleAmount());
        assertEquals(new BigDecimal("5.000000"), record.getGrossProfit());
    }

    @Test
    public void testCreateCostRecord_idempotent() {
        AigcCostRecordCreateReqDTO reqDTO = new AigcCostRecordCreateReqDTO();
        reqDTO.setTaskId(201L);
        reqDTO.setUserId(100L);
        reqDTO.setCostAmount(new BigDecimal("5.000000"));
        reqDTO.setSaleAmount(new BigDecimal("10.000000"));

        Long id1 = costRecordService.createCostRecord(reqDTO);
        Long id2 = costRecordService.createCostRecord(reqDTO);

        assertEquals(id1, id2);
    }

}
