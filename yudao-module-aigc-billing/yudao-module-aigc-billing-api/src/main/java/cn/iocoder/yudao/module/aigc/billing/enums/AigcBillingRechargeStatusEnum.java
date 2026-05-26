package cn.iocoder.yudao.module.aigc.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcBillingRechargeStatusEnum {

    WAIT_PAY("WAIT_PAY", "等待支付"),
    PAID("PAID", "已支付"),
    CLOSED("CLOSED", "已关闭"),
    REFUNDED("REFUNDED", "已退款"),
    MANUAL_SUCCESS("MANUAL_SUCCESS", "手工成功"),
    FAILED("FAILED", "失败");

    private final String code;
    private final String name;

}
