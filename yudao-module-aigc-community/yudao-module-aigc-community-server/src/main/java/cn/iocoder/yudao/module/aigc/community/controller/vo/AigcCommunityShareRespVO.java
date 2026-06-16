package cn.iocoder.yudao.module.aigc.community.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "Community share response")
@Data
@Accessors(chain = true)
public class AigcCommunityShareRespVO {

    private String shareUrl;
    private String shareToken;
    private Integer shareCount;

}
