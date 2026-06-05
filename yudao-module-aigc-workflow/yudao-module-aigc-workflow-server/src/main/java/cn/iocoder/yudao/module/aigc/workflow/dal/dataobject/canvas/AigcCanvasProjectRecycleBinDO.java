package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName(value = "aigc_canvas_project_recycle_bin", autoResultMap = true)
@KeySequence("aigc_canvas_project_recycle_bin_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasProjectRecycleBinDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private Long ownerUserId;
    private String projectName;
    private Long coverAssetId;
    private Long currentVersion;
    private Long latestSnapshotId;
    private String projectStatus;
    private Integer nodeCount;
    private Integer assetCount;
    private Long deletedBy;
    private LocalDateTime deletedTime;
    private String deleteReason;

}
