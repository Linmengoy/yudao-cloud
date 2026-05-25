package cn.iocoder.yudao.module.aigc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AigcModelBillingUnitEnum {

    PER_TASK("PER_TASK", "按任务"),
    PER_IMAGE("PER_IMAGE", "按张"),
    PER_SECOND("PER_SECOND", "按秒"),
    PER_5_SECONDS("PER_5_SECONDS", "每5秒"),
    PER_BATCH("PER_BATCH", "按批次");

    private final String code;
    private final String description;

    public static AigcModelBillingUnitEnum fromCode(String code) {
        for (AigcModelBillingUnitEnum unit : values()) {
            if (unit.getCode().equals(code)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown billing unit: " + code);
    }

    public static AigcModelBillingUnitEnum getByValue(String code) {
        return fromCode(code);
    }

}