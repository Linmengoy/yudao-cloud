package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetStatusEnum {

    NORMAL("NORMAL", "正常"),
    DELETED("DELETED", "已删除"),
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String name;

}
