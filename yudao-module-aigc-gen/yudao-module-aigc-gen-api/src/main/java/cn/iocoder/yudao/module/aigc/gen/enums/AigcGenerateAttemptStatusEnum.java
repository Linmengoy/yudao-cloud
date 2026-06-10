package cn.iocoder.yudao.module.aigc.gen.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AigcGenerateAttemptStatusEnum implements ArrayValuable<String> {

    CREATED("CREATED", "Created"),
    SUBMITTING("SUBMITTING", "Submitting"),
    SUBMITTED("SUBMITTED", "Submitted"),
    CALLBACK_WAITING("CALLBACK_WAITING", "Callback waiting"),
    SUCCESS("SUCCESS", "Success"),
    FAILED("FAILED", "Failed"),
    CANCELLED("CANCELLED", "Cancelled"),
    IGNORED("IGNORED", "Ignored");

    public static final String[] ARRAYS = Arrays.stream(values()).map(AigcGenerateAttemptStatusEnum::getCode).toArray(String[]::new);

    private final String code;
    private final String label;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
