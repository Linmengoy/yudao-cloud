package cn.iocoder.yudao.module.aigc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AigcModelProviderAuthTypeEnum {

    API_KEY("API_KEY", "API Key"),
    BEARER_TOKEN("BEARER_TOKEN", "Bearer Token"),
    AK_SK("AK_SK", "AK/SK"),
    CUSTOM_HEADER("CUSTOM_HEADER", "自定义头"),
    NONE("NONE", "无需鉴权");

    private final String code;
    private final String description;

    public static AigcModelProviderAuthTypeEnum fromCode(String code) {
        for (AigcModelProviderAuthTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown auth type: " + code);
    }

}