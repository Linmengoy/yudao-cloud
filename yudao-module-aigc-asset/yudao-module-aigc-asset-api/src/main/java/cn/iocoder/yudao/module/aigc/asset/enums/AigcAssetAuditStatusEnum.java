package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetAuditStatusEnum {

    PENDING("PENDING", "待审核"),
    PASS("PASS", "审核通过"),
    REJECT("REJECT", "审核拒绝"),
    MANUAL_REVIEW("MANUAL_REVIEW", "人工复审");

    private final String code;
    private final String name;

}
