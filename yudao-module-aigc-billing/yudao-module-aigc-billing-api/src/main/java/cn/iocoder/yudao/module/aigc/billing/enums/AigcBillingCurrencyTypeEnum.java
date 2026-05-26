package cn.iocoder.yudao.module.aigc.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcBillingCurrencyTypeEnum {

    POINT("POINT", "积分"),
    CNY("CNY", "人民币"),
    USD("USD", "美元");

    private final String code;
    private final String name;

}
