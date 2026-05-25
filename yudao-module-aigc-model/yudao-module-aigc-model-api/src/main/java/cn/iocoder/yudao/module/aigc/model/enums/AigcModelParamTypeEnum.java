package cn.iocoder.yudao.module.aigc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AigcModelParamTypeEnum {

    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    BOOLEAN("BOOLEAN", "布尔"),
    SELECT("SELECT", "单选"),
    MULTI_SELECT("MULTI_SELECT", "多选"),
    JSON("JSON", "JSON");

    private final String code;
    private final String description;

    public static AigcModelParamTypeEnum fromCode(String code) {
        for (AigcModelParamTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown param type: " + code);
    }

    public static AigcModelParamTypeEnum getByValue(String code) {
        return fromCode(code);
    }

}