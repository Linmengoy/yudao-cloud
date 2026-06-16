package cn.iocoder.yudao.module.aigc.community.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcGuidePublishStatusEnum {

    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED");

    private final String code;

}
