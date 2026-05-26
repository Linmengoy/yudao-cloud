package cn.iocoder.yudao.module.aigc.billing.service.freeze;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;

public interface AigcQuotaFreezeService {

    AigcBillingFreezeRespDTO freeze(AigcBillingFreezeReqDTO reqDTO);

    void confirmFreeze(AigcBillingConfirmReqDTO reqDTO);

    void releaseFreeze(AigcBillingReleaseReqDTO reqDTO);

    AigcQuotaFreezeDO getFreeze(Long id);

    PageResult<AigcQuotaFreezeDO> getFreezePage(PageParam reqVO);

    PageResult<AigcQuotaFreezeDO> getUserFreezePage(PageParam reqVO, Long userId);

    int releaseTimeoutFreezes(Integer limit);

}
