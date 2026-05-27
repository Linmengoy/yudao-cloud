package cn.iocoder.yudao.module.aigc.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcWorkflowInstanceStatusEnum {

    CREATED("CREATED", "已创建"),
    ESTIMATING("ESTIMATING", "费用预估中"),
    FROZEN("FROZEN", "已冻结"),
    RUNNING("RUNNING", "运行中"),
    WAITING_MANUAL("WAITING_MANUAL", "等待人工确认"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款"),
    CANCELING("CANCELING", "取消中"),
    CANCELED("CANCELED", "已取消");

    private final String code;
    private final String name;

}
