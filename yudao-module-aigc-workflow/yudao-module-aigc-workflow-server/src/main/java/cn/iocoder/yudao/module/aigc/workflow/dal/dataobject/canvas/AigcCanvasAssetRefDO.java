package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_canvas_asset_ref", autoResultMap = true)
@KeySequence("aigc_canvas_asset_ref_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasAssetRefDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private String nodeId;
    private Long assetId;
    private String usageType;
    private Long sourceTaskId;

}
