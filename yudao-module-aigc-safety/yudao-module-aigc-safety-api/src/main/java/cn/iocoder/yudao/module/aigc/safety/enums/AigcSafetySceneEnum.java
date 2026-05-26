package cn.iocoder.yudao.module.aigc.safety.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcSafetySceneEnum {

    PROMPT("PROMPT", "提示词审核"),
    ASSET("ASSET", "资产审核"),
    TASK("TASK", "任务审核"),
    COMMENT("COMMENT", "评论审核"),
    POST("POST", "社区内容审核");

    private final String code;
    private final String name;

}
