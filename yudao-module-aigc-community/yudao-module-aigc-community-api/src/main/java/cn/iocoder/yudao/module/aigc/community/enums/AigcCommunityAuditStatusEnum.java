package cn.iocoder.yudao.module.aigc.community.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcCommunityAuditStatusEnum {

    PENDING("PENDING", "Pending"),
    PASS("PASS", "Pass"),
    REJECT("REJECT", "Reject"),
    MANUAL_REVIEW("MANUAL_REVIEW", "Manual review");

    private final String code;
    private final String name;

}
