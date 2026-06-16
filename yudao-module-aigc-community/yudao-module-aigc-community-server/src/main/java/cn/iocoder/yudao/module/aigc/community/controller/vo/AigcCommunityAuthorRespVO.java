package cn.iocoder.yudao.module.aigc.community.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "Community author response")
@Data
@Accessors(chain = true)
public class AigcCommunityAuthorRespVO {

    private Long authorUserId;
    private String nickname;
    private String avatarUrl;
    private Integer followerCount;
    private Integer followingCount;
    private Integer publicPostCount;
    private Integer likeReceivedCount;
    private Boolean followedByCurrentUser;

}
