package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_workflow_log", autoResultMap = true)
@KeySequence("aigc_workflow_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long workflowInstanceId;
    private Long nodeInstanceId;
    private String logType;
    private String fromStatus;
    private String toStatus;
    private String summary;
    private String detailData;
    private Long costMillis;
    private Long userId;

}
