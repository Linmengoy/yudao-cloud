package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetTypeEnum {

    IMAGE("IMAGE", "图片"),
    VIDEO("VIDEO", "视频"),
    AUDIO("AUDIO", "音频"),
    DOCUMENT("DOCUMENT", "文档"),
    PPT("PPT", "PPT"),
    SUBTITLE("SUBTITLE", "字幕"),
    COVER("COVER", "封面"),
    DIGITAL_HUMAN_VIDEO("DIGITAL_HUMAN_VIDEO", "数字人视频"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String name;

}
