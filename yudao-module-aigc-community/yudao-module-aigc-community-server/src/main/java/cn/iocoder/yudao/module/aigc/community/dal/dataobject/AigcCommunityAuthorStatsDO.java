package cn.iocoder.yudao.module.aigc.community.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("aigc_community_author_stats")
@KeySequence("aigc_community_author_stats_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityAuthorStatsDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long userId;
    private Integer followerCount;
    private Integer followingCount;
    private Integer publicPostCount;
    private Integer likeReceivedCount;
    private LocalDateTime lastPublishTime;

}
