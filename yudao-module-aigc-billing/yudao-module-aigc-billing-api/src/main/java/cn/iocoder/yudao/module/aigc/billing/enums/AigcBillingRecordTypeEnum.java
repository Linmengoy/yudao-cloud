package cn.iocoder.yudao.module.aigc.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcBillingRecordTypeEnum {

    RECHARGE("RECHARGE", "充值"),
    GIFT("GIFT", "赠送"),
    FREEZE("FREEZE", "冻结"),
    CONSUME("CONSUME", "消费"),
    RELEASE("RELEASE", "释放"),
    REFUND("REFUND", "退款"),
    ADJUST_INCREASE("ADJUST_INCREASE", "手动增加"),
    ADJUST_DECREASE("ADJUST_DECREASE", "手动减少"),
    COMPENSATE("COMPENSATE", "补偿");

    private final String code;
    private final String name;

}
