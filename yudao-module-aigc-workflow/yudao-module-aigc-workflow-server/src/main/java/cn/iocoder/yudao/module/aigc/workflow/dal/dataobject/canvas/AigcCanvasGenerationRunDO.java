package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_canvas_generation_run", autoResultMap = true)
@KeySequence("aigc_canvas_generation_run_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasGenerationRunDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private String nodeId;
    private String runId;
    private Long taskId;
    private Long generateRecordId;
    private String generateNo;
    private String nodeType;
    private String generateType;
    private String generateMode;
    private Long creatorUserId;
    private String status;
    private Integer progress;
    private String opId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

}
