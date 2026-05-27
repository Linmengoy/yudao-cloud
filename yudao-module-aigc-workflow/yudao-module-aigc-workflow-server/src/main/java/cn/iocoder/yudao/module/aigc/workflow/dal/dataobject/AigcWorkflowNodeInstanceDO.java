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

    public Long getId() {
        return id;
    }

    public AigcWorkflowNodeInstanceDO setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public AigcWorkflowNodeInstanceDO setWorkflowInstanceId(Long workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setNodeId(Long nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public AigcWorkflowNodeInstanceDO setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setNodeType(String nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    public String getNodeType() {
        return nodeType;
    }

    public String getStatus() {
        return status;
    }

    public Long getTaskId() {
        return taskId;
    }

    public AigcWorkflowNodeInstanceDO setTaskId(Long taskId) {
        this.taskId = taskId;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setStatus(String status) {
        this.status = status;
        return this;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public AigcWorkflowNodeInstanceDO setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setGenRecordId(Long genRecordId) {
        this.genRecordId = genRecordId;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setInputData(String inputData) {
        this.inputData = inputData;
        return this;
    }

    public String getInputData() {
        return inputData;
    }

    public String getOutputData() {
        return outputData;
    }

    public String getAssetIds() {
        return assetIds;
    }

    public AigcWorkflowNodeInstanceDO setOutputData(String outputData) {
        this.outputData = outputData;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setAssetIds(String assetIds) {
        this.assetIds = assetIds;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public AigcWorkflowNodeInstanceDO setCostAmount(Long costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public AigcWorkflowNodeInstanceDO setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
        return this;
    }

}
