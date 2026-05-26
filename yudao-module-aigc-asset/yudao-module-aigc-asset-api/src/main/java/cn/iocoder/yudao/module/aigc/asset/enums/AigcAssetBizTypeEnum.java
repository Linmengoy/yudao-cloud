package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetBizTypeEnum {

    TASK("TASK", "生成任务"),
    PROJECT("PROJECT", "创作项目"),
    COMMUNITY("COMMUNITY", "社区内容"),
    TEMPLATE("TEMPLATE", "模板"),
    WORKFLOW("WORKFLOW", "工作流"),
    PUBLISH("PUBLISH", "发布导出");

    private final String code;
    private final String name;

}
