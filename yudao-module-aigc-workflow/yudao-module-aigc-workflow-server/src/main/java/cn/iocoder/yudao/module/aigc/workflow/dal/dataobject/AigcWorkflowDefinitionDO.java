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

}
