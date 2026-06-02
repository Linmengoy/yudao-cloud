package cn.iocoder.yudao.module.aigc.billing.controller.admin.recharge.vo;

import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargeOrderDO;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayNotifyDiagnosticRespDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 充值支付链路排障 Response VO")
@Data
public class AigcRechargeOrderDiagnosticRespVO {

    @Schema(description = "充值订单")
    private AigcRechargeOrderDO rechargeOrder;

    @Schema(description = "Pay 支付订单")
    private PayOrderRespDTO payOrder;

    @Schema(description = "充值入账流水")
    private AigcBillingRecordDO billingRecord;

    @Schema(description = "Pay 业务通知排障信息")
    private PayNotifyDiagnosticRespDTO payNotify;

    @Schema(description = "Pay 订单是否匹配")
    private Boolean payOrderMatched;

    @Schema(description = "金额是否匹配")
    private Boolean amountMatched;

    @Schema(description = "Pay 是否已支付成功")
    private Boolean paySuccess;

    @Schema(description = "是否已生成入账流水")
    private Boolean billingRecordExists;

    @Schema(description = "排障结论")
    private String diagnosticMessage;

}
