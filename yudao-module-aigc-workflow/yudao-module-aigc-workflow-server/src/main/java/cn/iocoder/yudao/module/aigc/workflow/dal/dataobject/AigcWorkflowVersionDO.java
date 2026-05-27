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

}
