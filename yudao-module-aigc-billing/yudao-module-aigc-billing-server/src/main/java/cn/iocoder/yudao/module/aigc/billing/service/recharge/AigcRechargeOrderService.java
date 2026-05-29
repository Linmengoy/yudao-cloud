package cn.iocoder.yudao.module.aigc.billing.service.recharge;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;

import java.math.BigDecimal;

public interface AigcRechargeOrderService {

    Long createManualRecharge(Long userId, BigDecimal amount, String remark);
    Long createRechargeOrder(Long userId, BigDecimal amount, Integer payAmount, String remark);
    Long createRechargeOrderByPackage(Long userId, Long packageId, String remark);
    AigcRechargeOrderDO getRechargeOrder(Long id);
    AigcRechargeOrderDO getUserRechargeOrder(Long id, Long userId);
    void notifyRechargePaid(AigcRechargeNotifyReqDTO reqDTO);
    void closeRechargeOrder(Long id);
    PageResult<AigcRechargeOrderDO> getRechargeOrderPage(PageParam reqVO);
    PageResult<AigcRechargeOrderDO> getUserRechargeOrderPage(PageParam reqVO, Long userId);
}
