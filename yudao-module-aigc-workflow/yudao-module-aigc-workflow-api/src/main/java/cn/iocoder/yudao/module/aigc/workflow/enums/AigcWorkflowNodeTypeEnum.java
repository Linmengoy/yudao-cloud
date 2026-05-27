package cn.iocoder.yudao.module.aigc.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcWorkflowNodeTypeEnum {

    START("START", "开始节点"),
    TEXT_GENERATE("TEXT_GENERATE", "文本生成"),
    IMAGE_GENERATE("IMAGE_GENERATE", "图片生成"),
    VIDEO_GENERATE("VIDEO_GENERATE", "视频生成"),
    AUDIO_GENERATE("AUDIO_GENERATE", "音频生成"),
    DOCUMENT_GENERATE("DOCUMENT_GENERATE", "文档生成"),
    PPT_GENERATE("PPT_GENERATE", "PPT 生成"),
    DIGITAL_HUMAN("DIGITAL_HUMAN", "数字人生成"),
    ASSET_INPUT("ASSET_INPUT", "资产输入"),
    ASSET_OUTPUT("ASSET_OUTPUT", "资产输出"),
    CONDITION("CONDITION", "条件判断"),
    MANUAL_CONFIRM("MANUAL_CONFIRM", "人工确认"),
    END("END", "结束节点");

    private final String code;
    private final String name;

}
