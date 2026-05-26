package cn.iocoder.yudao.module.aigc.task.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcTaskTypeEnum {

    TEXT_GENERATE("TEXT_GENERATE", "文本生成"),
    TEXT_CHAT("TEXT_CHAT", "文本对话"),
    IMAGE_TEXT_TO_IMAGE("IMAGE_TEXT_TO_IMAGE", "文生图"),
    IMAGE_TO_IMAGE("IMAGE_TO_IMAGE", "图生图"),
    VIDEO_TEXT_TO_VIDEO("VIDEO_TEXT_TO_VIDEO", "文生视频"),
    VIDEO_IMAGE_TO_VIDEO("VIDEO_IMAGE_TO_VIDEO", "图生视频"),
    AUDIO_TEXT_TO_SPEECH("AUDIO_TEXT_TO_SPEECH", "文本转语音"),
    CODE_GENERATE("CODE_GENERATE", "代码生成"),
    DOCUMENT_GENERATE("DOCUMENT_GENERATE", "文档生成");

    private final String code;
    private final String name;

}
