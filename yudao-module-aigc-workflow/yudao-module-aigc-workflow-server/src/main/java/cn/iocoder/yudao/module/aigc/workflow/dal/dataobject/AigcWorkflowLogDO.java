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

    public Long getId() {
        return id;
    }

    public Long getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public Long getNodeInstanceId() {
        return nodeInstanceId;
    }

    public String getLogType() {
        return logType;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public AigcWorkflowLogDO setWorkflowInstanceId(Long workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
        return this;
    }

    public AigcWorkflowLogDO setNodeInstanceId(Long nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
        return this;
    }

    public AigcWorkflowLogDO setLogType(String logType) {
        this.logType = logType;
        return this;
    }

    public AigcWorkflowLogDO setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
        return this;
    }

    public AigcWorkflowLogDO setToStatus(String toStatus) {
        this.toStatus = toStatus;
        return this;
    }

    public AigcWorkflowLogDO setSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public AigcWorkflowLogDO setDetailData(String detailData) {
        this.detailData = detailData;
        return this;
    }

    public AigcWorkflowLogDO setCostMillis(Long costMillis) {
        this.costMillis = costMillis;
        return this;
    }

    public AigcWorkflowLogDO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

}
