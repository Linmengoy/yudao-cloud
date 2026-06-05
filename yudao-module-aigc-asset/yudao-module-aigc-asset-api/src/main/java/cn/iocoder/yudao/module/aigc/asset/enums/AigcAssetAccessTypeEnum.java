package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetAccessTypeEnum {

    PREVIEW("PREVIEW", 900),
    DOWNLOAD("DOWNLOAD", 1800),
    THUMBNAIL("THUMBNAIL", 900),
    COVER("COVER", 900);

    private final String code;
    private final Integer expireSeconds;

    public static Integer getExpireSeconds(String code) {
        for (AigcAssetAccessTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getExpireSeconds();
            }
        }
        return PREVIEW.getExpireSeconds();
    }

}
