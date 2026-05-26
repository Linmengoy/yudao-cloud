package cn.iocoder.yudao.module.aigc.safety.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAuditStatusEnum {

    PENDING("PENDING", "待审核"),
    PASS("PASS", "已通过"),
    REJECT("REJECT", "已拒绝");

    private final String code;
    private final String name;

}
