package cn.iocoder.yudao.module.aigc.task.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_task_retry", autoResultMap = true)
@KeySequence("aigc_task_retry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcTaskRetryDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String retryNo;

    private Long taskId;

    private String taskNo;

    private String retryType;

    private String retryStatus;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private String failReason;

    private Long operatorId;

}
