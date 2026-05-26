package cn.iocoder.yudao.module.aigc.safety.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcSensitiveWordStatusEnum {

    ENABLE("ENABLE", "启用"),
    DISABLE("DISABLE", "禁用");

    private final String code;
    private final String name;

}
