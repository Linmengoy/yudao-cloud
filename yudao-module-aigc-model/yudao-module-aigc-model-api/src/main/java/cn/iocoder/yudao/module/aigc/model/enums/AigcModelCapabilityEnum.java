package cn.iocoder.yudao.module.aigc.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum AigcModelCapabilityEnum {

    TEXT_TO_IMAGE("TEXT_TO_IMAGE", "文生图", "Text to Image"),
    IMAGE_TO_IMAGE("IMAGE_TO_IMAGE", "图生图", "Image to Image"),
    TEXT_TO_VIDEO("TEXT_TO_VIDEO", "文生视频", "Text to Video"),
    IMAGE_TO_VIDEO("IMAGE_TO_VIDEO", "图生视频", "Image to Video"),
    FIRST_LAST_FRAME_VIDEO("FIRST_LAST_FRAME_VIDEO", "首尾帧视频", "First-Last Frame Video"),
    MULTI_REF_VIDEO("MULTI_REF_VIDEO", "多参考视频", "Multi-Ref Video"),
    VIDEO_EXTEND("VIDEO_EXTEND", "视频延长", "Video Extend"),
    TTS("TTS", "语音合成", "Text to Speech"),
    VOICE_CLONE("VOICE_CLONE", "声音克隆", "Voice Clone"),
    BGM_GENERATE("BGM_GENERATE", "背景音乐生成", "BGM Generate"),
    SOUND_EFFECT_GENERATE("SOUND_EFFECT_GENERATE", "音效生成", "Sound Effect"),
    TEXT_GENERATE("TEXT_GENERATE", "文本生成", "Text Generate"),
    PROMPT_OPTIMIZE("PROMPT_OPTIMIZE", "提示词优化", "Prompt Optimize"),
    SCRIPT_GENERATE("SCRIPT_GENERATE", "剧本生成", "Script Generate");

    private final String code;
    private final String description;
    private final String labelEn;

    public static List<AigcModelCapabilityEnum> getImageCapabilities() {
        return Arrays.asList(TEXT_TO_IMAGE, IMAGE_TO_IMAGE);
    }

    public static List<AigcModelCapabilityEnum> getVideoCapabilities() {
        return Arrays.asList(TEXT_TO_VIDEO, IMAGE_TO_VIDEO, FIRST_LAST_FRAME_VIDEO, MULTI_REF_VIDEO, VIDEO_EXTEND);
    }

    public static List<AigcModelCapabilityEnum> getAudioCapabilities() {
        return Arrays.asList(TTS, VOICE_CLONE, BGM_GENERATE, SOUND_EFFECT_GENERATE);
    }

    public static List<AigcModelCapabilityEnum> getTextCapabilities() {
        return Arrays.asList(TEXT_GENERATE, PROMPT_OPTIMIZE, SCRIPT_GENERATE);
    }

    public static AigcModelCapabilityEnum fromCode(String code) {
        for (AigcModelCapabilityEnum capability : values()) {
            if (capability.getCode().equals(code)) {
                return capability;
            }
        }
        throw new IllegalArgumentException("Unknown capability: " + code);
    }

    public boolean isImageCapability() {
        return getImageCapabilities().contains(this);
    }

    public boolean isVideoCapability() {
        return getVideoCapabilities().contains(this);
    }

}
