package cn.iocoder.yudao.module.aigc.billing.service.cost;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcCostRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcGrossProfitRespDTO;

public interface AigcCostRecordService {

    Long createCostRecord(AigcCostRecordCreateReqDTO reqDTO);

    AigcCostRecordDO getCostRecord(Long id);

    AigcGrossProfitRespDTO calculateGrossProfit(Long taskId);

    PageResult<AigcCostRecordDO> getCostRecordPage(PageParam reqVO);
}
