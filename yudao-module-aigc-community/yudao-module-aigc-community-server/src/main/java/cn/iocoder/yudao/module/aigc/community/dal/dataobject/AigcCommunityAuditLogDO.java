package cn.iocoder.yudao.module.aigc.community.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@TableName("aigc_community_audit_log")
@KeySequence("aigc_community_audit_log_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityAuditLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String objectType;
    private Long objectId;
    private String action;
    private String reason;
    private Long operatorUserId;

}
