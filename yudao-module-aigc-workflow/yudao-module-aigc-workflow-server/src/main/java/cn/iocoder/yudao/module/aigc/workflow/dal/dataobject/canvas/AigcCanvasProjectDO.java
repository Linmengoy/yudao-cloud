package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_canvas_project", autoResultMap = true)
@KeySequence("aigc_canvas_project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasProjectDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long ownerUserId;
    private String name;
    private String kind;
    private Long coverAssetId;
    private Long currentVersion;
    private Long latestSnapshotId;
    private String status;
    private Integer nodeCount;
    private Integer assetCount;

}
