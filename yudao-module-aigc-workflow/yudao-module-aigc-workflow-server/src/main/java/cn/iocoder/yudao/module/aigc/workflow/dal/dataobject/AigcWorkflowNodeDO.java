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

}
