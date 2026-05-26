package cn.iocoder.yudao.module.aigc.billing.service.record;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;

public interface AigcBillingRecordService {

    Long createBillingRecord(AigcBillingRecordCreateReqDTO reqDTO);

    AigcBillingRecordDO getBillingRecord(Long id);

    PageResult<AigcBillingRecordDO> getBillingRecordPage(PageParam reqVO);

    PageResult<AigcBillingRecordDO> getUserBillingRecordPage(PageParam reqVO, Long userId);

}
