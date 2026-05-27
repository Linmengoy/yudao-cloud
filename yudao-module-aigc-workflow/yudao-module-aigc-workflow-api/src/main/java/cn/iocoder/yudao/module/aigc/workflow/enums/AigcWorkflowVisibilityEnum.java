package cn.iocoder.yudao.module.aigc.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcWorkflowVisibilityEnum {

    PRIVATE("PRIVATE", "私有"),
    TENANT("TENANT", "租户内可见"),
    PUBLIC("PUBLIC", "公开");

    private final String code;
    private final String name;

}
