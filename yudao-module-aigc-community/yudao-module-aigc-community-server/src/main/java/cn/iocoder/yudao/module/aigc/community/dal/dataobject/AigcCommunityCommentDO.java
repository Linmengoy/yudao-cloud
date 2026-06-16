package cn.iocoder.yudao.module.aigc.community.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@TableName("aigc_community_comment")
@KeySequence("aigc_community_comment_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityCommentDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long postId;
    private Long userId;
    private Long parentId;
    private String content;
    private String auditStatus;
    private String auditReason;
    private String status;
    private Integer likeCount;

}
