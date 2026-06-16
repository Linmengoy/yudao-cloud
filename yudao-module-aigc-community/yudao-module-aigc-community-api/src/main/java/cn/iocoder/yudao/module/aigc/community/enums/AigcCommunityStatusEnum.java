package cn.iocoder.yudao.module.aigc.community.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcCommunityStatusEnum {

    NORMAL("NORMAL", "Normal"),
    HIDDEN("HIDDEN", "Hidden"),
    DELETED("DELETED", "Deleted"),
    FOLLOWING("FOLLOWING", "Following"),
    CANCELLED("CANCELLED", "Cancelled");

    private final String code;
    private final String name;

}
