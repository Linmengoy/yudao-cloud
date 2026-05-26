package cn.iocoder.yudao.module.aigc.safety.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcSensitiveWordMatchTypeEnum {

    CONTAINS("CONTAINS", "包含匹配"),
    EXACT("EXACT", "完全匹配"),
    REGEX("REGEX", "正则匹配");

    private final String code;
    private final String name;

}
