package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_workflow_version", autoResultMap = true)
@KeySequence("aigc_workflow_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowVersionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long workflowId;
    private Integer versionNo;
    private String versionName;
    private String definitionSnapshot;
    private String nodeSnapshot;
    private String edgeSnapshot;
    private String status;

    public Long getId() {
        return id;
    }

    public AigcWorkflowVersionDO setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public AigcWorkflowVersionDO setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public AigcWorkflowVersionDO setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
        return this;
    }

    public AigcWorkflowVersionDO setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public AigcWorkflowVersionDO setDefinitionSnapshot(String definitionSnapshot) {
        this.definitionSnapshot = definitionSnapshot;
        return this;
    }

    public AigcWorkflowVersionDO setNodeSnapshot(String nodeSnapshot) {
        this.nodeSnapshot = nodeSnapshot;
        return this;
    }

    public AigcWorkflowVersionDO setEdgeSnapshot(String edgeSnapshot) {
        this.edgeSnapshot = edgeSnapshot;
        return this;
    }

    public AigcWorkflowVersionDO setStatus(String status) {
        this.status = status;
        return this;
    }

}
