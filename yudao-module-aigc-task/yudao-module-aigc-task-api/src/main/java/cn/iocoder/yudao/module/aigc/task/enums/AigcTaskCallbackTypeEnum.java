package cn.iocoder.yudao.module.aigc.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcTaskCallbackTypeEnum {

    PROVIDER_TASK_SUCCESS("PROVIDER_TASK_SUCCESS", "第三方任务成功"),
    PROVIDER_TASK_FAILED("PROVIDER_TASK_FAILED", "第三方任务失败"),
    PROVIDER_TASK_PROGRESS("PROVIDER_TASK_PROGRESS", "第三方任务进度"),
    PROVIDER_TASK_CANCELLED("PROVIDER_TASK_CANCELLED", "第三方任务取消"),
    MANUAL_CALLBACK("MANUAL_CALLBACK", "人工回调");

    private final String code;
    private final String name;

}
