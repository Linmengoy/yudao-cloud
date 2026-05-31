package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_canvas_member", autoResultMap = true)
@KeySequence("aigc_canvas_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasMemberDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private Long userId;
    private String role;
    private Long inviteUserId;
    private LocalDateTime joinedTime;
    private LocalDateTime lastActiveTime;

}
