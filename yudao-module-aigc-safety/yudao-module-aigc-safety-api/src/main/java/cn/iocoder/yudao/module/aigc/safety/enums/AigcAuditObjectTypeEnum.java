package cn.iocoder.yudao.module.aigc.safety.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAuditObjectTypeEnum {

    PROMPT("PROMPT", "提示词"),
    TASK("TASK", "生成任务"),
    ASSET("ASSET", "资产"),
    COMMENT("COMMENT", "评论"),
    POST("POST", "社区内容");

    private final String code;
    private final String name;

}
