package cn.iocoder.yudao.module.aigc.model.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AigcModelTypeEnum implements ArrayValuable<Integer> {

    TEXT(1, "文本模型"),
    IMAGE(2, "图片模型"),
    VIDEO(3, "视频模型"),
    AUDIO(4, "音频模型"),
    AUDIT(5, "审核模型");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(AigcModelTypeEnum::getValue).toArray(Integer[]::new);

    private final Integer value;
    private final String label;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static AigcModelTypeEnum valueOf(Integer value) {
        for (AigcModelTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown model type: " + value);
    }

}