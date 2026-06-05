package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetFileRoleEnum {

    ORIGINAL("ORIGINAL"),
    PREVIEW("PREVIEW"),
    THUMBNAIL("THUMBNAIL"),
    COVER("COVER"),
    WATERMARK("WATERMARK"),
    TRANSCODED("TRANSCODED");

    private final String code;

}
