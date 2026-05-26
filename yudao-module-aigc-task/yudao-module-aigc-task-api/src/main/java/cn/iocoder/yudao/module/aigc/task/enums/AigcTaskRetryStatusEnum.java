package cn.iocoder.yudao.module.aigc.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcTaskRetryStatusEnum {

    WAITING("WAITING", "等待重试"),
    RUNNING("RUNNING", "重试中"),
    SUCCESS("SUCCESS", "重试成功"),
    FAILED("FAILED", "重试失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String name;

}
