package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_canvas_operation_log", autoResultMap = true)
@KeySequence("aigc_canvas_operation_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasOperationLogDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private String clientId;
    private String opId;
    private Long actorUserId;
    private Long baseVersion;
    private Long nextVersion;
    private String operationType;
    private String operationJson;
    private String inverseOperationJson;

}
