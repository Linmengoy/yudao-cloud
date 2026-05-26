package cn.iocoder.yudao.module.aigc.billing.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("aigc_quota_freeze")
@KeySequence("aigc_quota_freeze_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcQuotaFreezeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String freezeNo;
    private Long walletId;
    private Long userId;
    private String bizType;
    private String bizId;
    private Long taskId;
    private String taskNo;
    private BigDecimal amount;
    private BigDecimal confirmedAmount;
    private BigDecimal releasedAmount;
    private String status;
    private LocalDateTime expireTime;
    private LocalDateTime confirmTime;
    private LocalDateTime releaseTime;
    private String reason;

}
