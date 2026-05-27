package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_workflow_node_instance", autoResultMap = true)
@KeySequence("aigc_workflow_node_instance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowNodeInstanceDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long workflowInstanceId;
    private Long nodeId;
    private String nodeKey;
    private String nodeType;
    private String status;
    private Long taskId;
    private Long genRecordId;
    private String inputData;
    private String outputData;
    private String assetIds;
    private Integer retryCount;
    private Integer maxRetryCount;
    private Long costAmount;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

}
