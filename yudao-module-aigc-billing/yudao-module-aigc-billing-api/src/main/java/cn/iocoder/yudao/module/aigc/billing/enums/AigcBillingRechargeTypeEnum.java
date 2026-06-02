package cn.iocoder.yudao.module.aigc.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcBillingRechargeTypeEnum {

    MANUAL("MANUAL", "手工充值"),
    PAY("PAY", "支付充值"),
    PACKAGE("PACKAGE", "套餐充值");

    private final String code;
    private final String name;

}