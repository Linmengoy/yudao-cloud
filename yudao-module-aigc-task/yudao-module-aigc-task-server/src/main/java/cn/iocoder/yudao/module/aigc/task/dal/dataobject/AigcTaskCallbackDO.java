package cn.iocoder.yudao.module.aigc.task.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_task_callback", autoResultMap = true)
@KeySequence("aigc_task_callback_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcTaskCallbackDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String callbackNo;

    private Long taskId;

    private String taskNo;

    private Long providerId;

    private String providerCode;

    private String externalTaskId;

    private String callbackType;

    private String callbackStatus;

    private String rawBody;

    private String headers;

    private String signature;

    private String processResult;

    private String failReason;

    private LocalDateTime receivedTime;

    private LocalDateTime processedTime;

}
