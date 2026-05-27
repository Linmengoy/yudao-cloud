package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_workflow_edge", autoResultMap = true)
@KeySequence("aigc_workflow_edge_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowEdgeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long workflowId;
    private Long versionId;
    private String edgeKey;
    private String sourceNodeKey;
    private String targetNodeKey;
    private String conditionConfig;
    private String inputMapping;

    public Long getId() {
        return id;
    }

    public AigcWorkflowEdgeDO setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public AigcWorkflowEdgeDO setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public Long getVersionId() {
        return versionId;
    }

    public AigcWorkflowEdgeDO setVersionId(Long versionId) {
        this.versionId = versionId;
        return this;
    }

    public String getEdgeKey() {
        return edgeKey;
    }

    public String getSourceNodeKey() {
        return sourceNodeKey;
    }

    public String getTargetNodeKey() {
        return targetNodeKey;
    }

    public String getConditionConfig() {
        return conditionConfig;
    }

    public String getInputMapping() {
        return inputMapping;
    }

}
