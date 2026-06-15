package cn.iocoder.yudao.module.aigc.community.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcCommunityPostStatusEnum {

    PENDING("PENDING", "Pending"),
    PUBLISHED("PUBLISHED", "Published"),
    OFFLINE("OFFLINE", "Offline"),
    REJECTED("REJECTED", "Rejected");

    private final String code;
    private final String name;

}
