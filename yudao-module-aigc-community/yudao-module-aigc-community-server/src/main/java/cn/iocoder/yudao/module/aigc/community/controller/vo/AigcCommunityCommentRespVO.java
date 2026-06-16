package cn.iocoder.yudao.module.aigc.community.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "Community comment response")
@Data
@Accessors(chain = true)
public class AigcCommunityCommentRespVO {

    private Long id;
    private Long postId;
    private Long userId;
    private String userNickname;
    private String userAvatarUrl;
    private Long parentId;
    private String content;
    private String auditStatus;
    private String auditReason;
    private String status;
    private Integer likeCount;
    private Boolean mine;
    private LocalDateTime createTime;

}
