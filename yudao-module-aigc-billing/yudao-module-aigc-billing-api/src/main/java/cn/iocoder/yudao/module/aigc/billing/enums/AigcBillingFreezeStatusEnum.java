package cn.iocoder.yudao.module.aigc.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcBillingFreezeStatusEnum {

    FROZEN("FROZEN", "已冻结"),
    CONFIRMED("CONFIRMED", "已扣费"),
    RELEASED("RELEASED", "已释放"),
    EXPIRED("EXPIRED", "已过期"),
    PART_CONFIRMED("PART_CONFIRMED", "部分扣费"),
    PART_RELEASED("PART_RELEASED", "部分释放");

    private final String code;
    private final String name;

}
