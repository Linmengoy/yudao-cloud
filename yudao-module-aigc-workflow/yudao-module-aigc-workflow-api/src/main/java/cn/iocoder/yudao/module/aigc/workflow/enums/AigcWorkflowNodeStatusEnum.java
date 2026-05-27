package cn.iocoder.yudao.module.aigc.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcWorkflowNodeStatusEnum {

    PENDING("PENDING", "等待中"),
    READY("READY", "可执行"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    RETRYING("RETRYING", "重试中"),
    SKIPPED("SKIPPED", "已跳过"),
    WAITING_MANUAL("WAITING_MANUAL", "等待人工确认"),
    CANCELED("CANCELED", "已取消");

    private final String code;
    private final String name;

}
