package cn.iocoder.yudao.module.aigc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AigcModelRouteStrategyEnum {

    FIXED_MODEL("FIXED_MODEL", "固定模型"),
    LOWEST_COST("LOWEST_COST", "最低成本"),
    HIGHEST_SUCCESS_RATE("HIGHEST_SUCCESS_RATE", "最高成功率"),
    FASTEST_RESPONSE("FASTEST_RESPONSE", "最快响应"),
    ROUND_ROBIN("ROUND_ROBIN", "轮询");

    private final String code;
    private final String description;

    public static AigcModelRouteStrategyEnum fromCode(String code) {
        for (AigcModelRouteStrategyEnum strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("Unknown route strategy: " + code);
    }

    public static AigcModelRouteStrategyEnum getByValue(String code) {
        return fromCode(code);
    }

}
