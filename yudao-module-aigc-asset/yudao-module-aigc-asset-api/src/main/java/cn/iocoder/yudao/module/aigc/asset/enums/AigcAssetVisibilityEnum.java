package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetVisibilityEnum {

    PRIVATE("PRIVATE", "仅本人可见"),
    PUBLIC("PUBLIC", "公开可见"),
    LINK("LINK", "链接可见"),
    TENANT("TENANT", "租户内可见");

    private final String code;
    private final String name;

}
