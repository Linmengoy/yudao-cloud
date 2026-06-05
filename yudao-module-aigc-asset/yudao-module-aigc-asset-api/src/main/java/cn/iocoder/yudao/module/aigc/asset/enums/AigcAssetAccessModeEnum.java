package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetAccessModeEnum {

    PRIVATE_SIGNED(1),
    PUBLIC(2),
    CDN_PUBLIC(3);

    private final Integer code;

}
