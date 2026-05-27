package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_workflow_node", autoResultMap = true)
@KeySequence("aigc_workflow_node_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowNodeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long workflowId;
    private Long versionId;
    private String nodeKey;
    private String nodeName;
    private String nodeType;
    private String generateType;
    private String generateMode;
    private Long modelId;
    private String inputMapping;
    private String outputMapping;
    private String paramConfig;
    private String retryConfig;
    private Integer timeoutSeconds;
    private String position;

    public Long getId() {
        return id;
    }

    public AigcWorkflowNodeDO setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public AigcWorkflowNodeDO setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public Long getVersionId() {
        return versionId;
    }

    public AigcWorkflowNodeDO setVersionId(Long versionId) {
        this.versionId = versionId;
        return this;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public AigcWorkflowNodeDO setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
        return this;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public AigcWorkflowNodeDO setNodeType(String nodeType) {
        this.nodeType = nodeType;
        return this;
    }

    public String getGenerateType() {
        return generateType;
    }

    public String getGenerateMode() {
        return generateMode;
    }

    public Long getModelId() {
        return modelId;
    }

    public String getInputMapping() {
        return inputMapping;
    }

    public String getOutputMapping() {
        return outputMapping;
    }

    public String getParamConfig() {
        return paramConfig;
    }

    public String getRetryConfig() {
        return retryConfig;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getPosition() {
        return position;
    }

}
