package cn.iocoder.yudao.module.aigc.billing.service.recharge;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.controller.app.recharge.vo.AppAigcRechargeOrderCreateRespVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcRechargeNotifyReqDTO;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayOrderNotifyReqDTO;

import java.math.BigDecimal;

public interface AigcRechargeOrderService {

    Long createManualRecharge(Long userId, BigDecimal amount, String remark);
    AppAigcRechargeOrderCreateRespVO createRechargeOrder(Long userId, BigDecimal amount, Integer payAmount, String userIp, String remark);
    AppAigcRechargeOrderCreateRespVO createRechargeOrderByPackage(Long userId, Long packageId, String userIp, String remark);
    AigcRechargeOrderDO getRechargeOrder(Long id);
    AigcRechargeOrderDO getUserRechargeOrder(Long id, Long userId);
    boolean syncPayStatus(Long id, Long userId);
    boolean notifyPayOrder(PayOrderNotifyReqDTO reqDTO);
    void notifyRechargePaid(AigcRechargeNotifyReqDTO reqDTO);
    void closeRechargeOrder(Long id);
    PageResult<AigcRechargeOrderDO> getRechargeOrderPage(PageParam reqVO);
    PageResult<AigcRechargeOrderDO> getUserRechargeOrderPage(PageParam reqVO, Long userId);
}
