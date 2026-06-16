package cn.iocoder.yudao.module.aigc.community.controller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Community post response")
@Data
@Accessors(chain = true)
public class AigcCommunityPostRespVO {

    private Long id;
    private String postNo;
    private Long authorUserId;
    private String authorNickname;
    private String authorAvatarUrl;
    private Long assetId;
    private Long projectId;
    private Long coverAssetId;
    private String assetType;
    private String coverUrl;
    private String fileUrl;
    private String title;
    private String summary;
    private String tags;
    private String promptSnapshot;
    private String metadata;
    private String visibility;
    private String publishStatus;
    private String auditStatus;
    private String auditReason;
    private Long auditorUserId;
    private LocalDateTime auditTime;
    private String offlineReason;
    private LocalDateTime offlineTime;
    private LocalDateTime publishTime;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer downloadCount;
    private BigDecimal hotScore;
    private Boolean likedByCurrentUser;
    private Boolean followedAuthor;
    private LocalDateTime createTime;

}
