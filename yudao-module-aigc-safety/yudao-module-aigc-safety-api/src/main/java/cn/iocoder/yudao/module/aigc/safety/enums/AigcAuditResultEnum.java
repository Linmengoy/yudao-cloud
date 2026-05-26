package cn.iocoder.yudao.module.aigc.safety.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAuditResultEnum {

    AUTO_PASS("AUTO_PASS", "自动审核通过"),
    AUTO_REJECT("AUTO_REJECT", "自动审核拒绝"),
    MANUAL_PASS("MANUAL_PASS", "人工审核通过"),
    MANUAL_REJECT("MANUAL_REJECT", "人工审核拒绝");

    private final String code;
    private final String name;

}
