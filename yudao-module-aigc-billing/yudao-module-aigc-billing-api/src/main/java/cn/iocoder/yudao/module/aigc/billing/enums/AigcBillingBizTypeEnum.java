package cn.iocoder.yudao.module.aigc.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcBillingBizTypeEnum {

    TASK_GENERATE("TASK_GENERATE", "生成任务"),
    TASK_REFUND("TASK_REFUND", "任务退款"),
    WALLET_RECHARGE("WALLET_RECHARGE", "钱包充值"),
    WALLET_GIFT("WALLET_GIFT", "钱包赠送"),
    MANUAL_ADJUST("MANUAL_ADJUST", "手动调整"),
    ACTIVITY_REWARD("ACTIVITY_REWARD", "活动奖励"),
    SYSTEM_COMPENSATE("SYSTEM_COMPENSATE", "系统补偿");

    private final String code;
    private final String name;

}
