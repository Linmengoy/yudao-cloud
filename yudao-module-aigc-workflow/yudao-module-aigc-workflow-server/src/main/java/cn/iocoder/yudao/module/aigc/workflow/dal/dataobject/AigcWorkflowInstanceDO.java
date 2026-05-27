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

    public Long getId() {
        return id;
    }

    public AigcWorkflowInstanceDO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getInstanceNo() {
        return instanceNo;
    }

    public AigcWorkflowInstanceDO setInstanceNo(String instanceNo) {
        this.instanceNo = instanceNo;
        return this;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public AigcWorkflowInstanceDO setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public AigcWorkflowInstanceDO setWorkflowVersionId(Long workflowVersionId) {
        this.workflowVersionId = workflowVersionId;
        return this;
    }

    public Long getWorkflowVersionId() {
        return workflowVersionId;
    }

    public AigcWorkflowInstanceDO setTemplateId(Long templateId) {
        this.templateId = templateId;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public AigcWorkflowInstanceDO setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public AigcWorkflowInstanceDO setStatus(String status) {
        this.status = status;
        return this;
    }

    public AigcWorkflowInstanceDO setInputData(String inputData) {
        this.inputData = inputData;
        return this;
    }

    public Long getMainTaskId() {
        return mainTaskId;
    }

    public AigcWorkflowInstanceDO setMainTaskId(Long mainTaskId) {
        this.mainTaskId = mainTaskId;
        return this;
    }

    public AigcWorkflowInstanceDO setProgress(Integer progress) {
        this.progress = progress;
        return this;
    }

    public Integer getProgress() {
        return progress;
    }

    public String getInputData() {
        return inputData;
    }

    public String getOutputData() {
        return outputData;
    }

    public Long getFreezeId() {
        return freezeId;
    }

    public Long getEstimateAmount() {
        return estimateAmount;
    }

    public Long getActualAmount() {
        return actualAmount;
    }

    public AigcWorkflowInstanceDO setOutputData(String outputData) {
        this.outputData = outputData;
        return this;
    }

    public AigcWorkflowInstanceDO setFreezeId(Long freezeId) {
        this.freezeId = freezeId;
        return this;
    }

    public AigcWorkflowInstanceDO setEstimateAmount(Long estimateAmount) {
        this.estimateAmount = estimateAmount;
        return this;
    }

    public AigcWorkflowInstanceDO setActualAmount(Long actualAmount) {
        this.actualAmount = actualAmount;
        return this;
    }

    public AigcWorkflowInstanceDO setFailReason(String failReason) {
        this.failReason = failReason;
        return this;
    }

    public AigcWorkflowInstanceDO setFailMessage(String failMessage) {
        this.failMessage = failMessage;
        return this;
    }

    public AigcWorkflowInstanceDO setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public AigcWorkflowInstanceDO setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
        return this;
    }

}
