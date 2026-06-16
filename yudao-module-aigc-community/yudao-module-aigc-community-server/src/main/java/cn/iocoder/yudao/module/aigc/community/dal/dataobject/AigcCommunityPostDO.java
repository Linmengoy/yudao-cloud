package cn.iocoder.yudao.module.aigc.community.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("aigc_community_post")
@KeySequence("aigc_community_post_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityPostDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String postNo;
    private Long authorUserId;
    private Long assetId;
    private String assetType;
    private Long projectId;
    private Long coverAssetId;
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

}
