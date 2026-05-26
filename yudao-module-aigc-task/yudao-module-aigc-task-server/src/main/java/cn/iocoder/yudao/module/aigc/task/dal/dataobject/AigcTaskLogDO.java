package cn.iocoder.yudao.module.aigc.task.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_task_log", autoResultMap = true)
@KeySequence("aigc_task_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcTaskLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private String taskNo;

    private String fromStatus;

    private String toStatus;

    private String action;

    private String message;

    private String operatorType;

    private Long operatorId;

    private String extraInfo;

}
