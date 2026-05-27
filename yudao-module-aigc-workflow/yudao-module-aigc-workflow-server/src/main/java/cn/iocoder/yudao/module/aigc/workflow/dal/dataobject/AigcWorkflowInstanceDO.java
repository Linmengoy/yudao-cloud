package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_workflow_instance", autoResultMap = true)
@KeySequence("aigc_workflow_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowInstanceDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String instanceNo;
    private Long workflowId;
    private Long workflowVersionId;
    private Long templateId;
    private Long userId;
    private String status;
    private String inputData;
    private String outputData;
    private Long mainTaskId;
    private Long freezeId;
    private Long estimateAmount;
    private Long actualAmount;
    private Integer progress;
    private String failReason;
    private String failMessage;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

}
