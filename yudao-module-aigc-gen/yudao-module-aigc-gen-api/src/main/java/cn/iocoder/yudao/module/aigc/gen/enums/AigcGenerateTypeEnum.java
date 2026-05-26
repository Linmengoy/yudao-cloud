package cn.iocoder.yudao.module.aigc.gen.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AigcGenerateTypeEnum implements ArrayValuable<String> {

    TEXT("TEXT", "文本"),
    IMAGE("IMAGE", "图片"),
    VIDEO("VIDEO", "视频"),
    AUDIO("AUDIO", "音频"),
    CODE("CODE", "代码"),
    DOCUMENT("DOCUMENT", "文档");

    public static final String[] ARRAYS = Arrays.stream(values()).map(AigcGenerateTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
