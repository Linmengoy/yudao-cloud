package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_workflow_definition", autoResultMap = true)
@KeySequence("aigc_workflow_definition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcWorkflowDefinitionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String name;
    private String code;
    private String description;
    private String coverUrl;
    private Long categoryId;
    private String visibility;
    private String status;
    private Long currentVersionId;
    private String inputSchema;
    private String outputSchema;
    private String config;
    private Long creatorUserId;

    public Long getId() {
        return id;
    }

    public AigcWorkflowDefinitionDO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getStatus() {
        return status;
    }

    public String getVisibility() {
        return visibility;
    }

    public Long getCurrentVersionId() {
        return currentVersionId;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public String getConfig() {
        return config;
    }

    public AigcWorkflowDefinitionDO setStatus(String status) {
        this.status = status;
        return this;
    }

    public AigcWorkflowDefinitionDO setVisibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    public AigcWorkflowDefinitionDO setCreatorUserId(Long creatorUserId) {
        this.creatorUserId = creatorUserId;
        return this;
    }

    public AigcWorkflowDefinitionDO setCurrentVersionId(Long currentVersionId) {
        this.currentVersionId = currentVersionId;
        return this;
    }

}
