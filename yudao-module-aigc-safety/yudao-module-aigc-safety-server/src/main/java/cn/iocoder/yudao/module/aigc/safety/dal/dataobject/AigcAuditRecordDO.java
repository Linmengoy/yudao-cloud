package cn.iocoder.yudao.module.aigc.safety.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_audit_record", autoResultMap = true)
@KeySequence("aigc_audit_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcAuditRecordDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String objectType;
    private Long objectId;
    private String content;
    private String scene;
    private String auditStatus;
    private String auditResult;
    private String hitWords;
    private Integer riskLevel;
    private String rejectReason;
    private Long auditorUserId;
    private LocalDateTime auditTime;

}
