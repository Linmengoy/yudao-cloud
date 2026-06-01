package cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_canvas_sketch", autoResultMap = true)
@KeySequence("aigc_canvas_sketch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCanvasSketchDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long projectId;
    private String nodeId;
    private String sceneJson;
    private String previewUrl;
    private String previewDataUrl;
    private Long previewAssetId;
    private Long previewAssetVersionId;
    private String mimeType;
    private Integer width;
    private Integer height;
    private String background;

}
