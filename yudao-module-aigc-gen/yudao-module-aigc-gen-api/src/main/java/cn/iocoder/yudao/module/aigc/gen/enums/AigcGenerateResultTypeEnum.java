package cn.iocoder.yudao.module.aigc.gen.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AigcGenerateResultTypeEnum implements ArrayValuable<String> {

    TEXT("TEXT", "文本"),
    FILE("FILE", "文件"),
    MIXED("MIXED", "混合");

    public static final String[] ARRAYS = Arrays.stream(values()).map(AigcGenerateResultTypeEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
