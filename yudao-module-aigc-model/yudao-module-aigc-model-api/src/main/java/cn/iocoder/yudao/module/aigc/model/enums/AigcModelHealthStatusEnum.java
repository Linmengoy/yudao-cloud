package cn.iocoder.yudao.module.aigc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AigcModelHealthStatusEnum {

    UNKNOWN("UNKNOWN", "未知"),
    HEALTHY("HEALTHY", "健康"),
    UNHEALTHY("UNHEALTHY", "不健康"),
    LIMITED("LIMITED", "受限"),
    BALANCE_LOW("BALANCE_LOW", "余额不足");

    private final String code;
    private final String description;

    public static AigcModelHealthStatusEnum fromCode(String code) {
        for (AigcModelHealthStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown health status: " + code);
    }

}