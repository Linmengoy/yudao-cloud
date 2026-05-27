package cn.iocoder.yudao.module.aigc.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcWorkflowTriggerTypeEnum {

    MANUAL("MANUAL", "手动触发"),
    TEMPLATE("TEMPLATE", "模板触发"),
    BATCH("BATCH", "批量触发"),
    API("API", "接口触发");

    private final String code;
    private final String name;

}
